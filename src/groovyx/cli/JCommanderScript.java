/*
 * Copyright 2014-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovyx.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;

import groovy.lang.Binding;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.join;

/**
 * Base script that provides JCommander declarative (annotation-based) argument processing for scripts.
 *
 * @author Jim White
 */

abstract public class JCommanderScript extends Script {
    public JCommanderScript() { this(new Binding()); }

    public JCommanderScript(Binding binding) {
        super(binding);
//        initializeJCommanderScript();
    }

    @Override
    public void setBinding(Binding binding) {
        super.setBinding(binding);
//        initializeJCommanderScript();
    }

    /**
     * Name of the property that holds the JCommander for this script (i.e. 'scriptJCommander').
     */
    public final static String SCRIPT_JCOMMANDER = "scriptJCommander";

    private Binding initializedBinding;
    private String[] initializedArgs;

    /**
     * The script body
     * @return The result of the script evaluation.
     */
    public abstract Object runScriptBody();

    @Override
    public Object run() {
        String[] args = getScriptArguments();
        if (args != initializedArgs || getBinding() != initializedBinding) {
            initializeJCommanderScript();
        }
        JCommander jc = (JCommander) getProperty(SCRIPT_JCOMMANDER);
        try {
            for (ParameterDescription pd : jc.getParameters()) {
                if (pd.isHelp() && pd.isAssigned()) return exitCode(printHelpMessage(jc, args));
            }
            runScriptCommand(jc);
            return exitCode(runScriptBody());
        } catch (ParameterException pe) {
            return exitCode(handleParameterException(jc, args, pe));
        }
    }

    public void initializeJCommanderScript() {
        String[] args = getScriptArguments();
        if (args != null) {
            initializedBinding = getBinding();
            initializedArgs = args;
            JCommander jc = getScriptJCommanderWithInit();
            parseScriptArguments(jc, args);
            initializeScriptDefaults(jc, getAssignedParameterNames(jc));
        } else {
            System.err.println("Bad script caller (no arg binding).");
        }
    }

    private Collection<String> getAssignedParameterNames(JCommander jc) {
        List<ParameterDescription> parameters = jc.getParameters();
        List<String> assignedNames = new ArrayList<String>(parameters.size());
        for (ParameterDescription pd : parameters) {
            if (pd.isAssigned()) assignedNames.add(pd.getLongestName());
        }
        return assignedNames;
    }

    public void initializeScriptDefaults(JCommander jc, Collection<String> assignedParameters) {
        Class cls = this.getClass();
        while (cls != null) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                Annotation annotation = field.getAnnotation(Default.class);
                if (annotation != null) {
                    String longestParameterName = getLongestParameterName(jc, field);
                    if (!assignedParameters.contains(longestParameterName)) {
                        initializeScriptField(field, (Default) annotation);
                    }
                }
            }

            cls = cls.getSuperclass();
        }
    }

    public void initializeScriptField(Field field, Default annotation) {
        try {
            field.setAccessible(true);
            Class<Callable> klazz = ((Default) annotation).value();
            Constructor<Callable> constructor = klazz.getConstructor(Object.class, Object.class);
            Callable defaultCalculator = constructor.newInstance(this, this);
            Object value = defaultCalculator.call();
            field.set(this, value);
        } catch (Exception e) {
            printErrorMessage("Trying to run GondorScript initializers but got exception '" + e
                    + "' when getting value of field " + field.getName());
        }
    }

    private String getLongestParameterName(JCommander jc, Field field) {
        Annotation annotation = field.getAnnotation(Parameter.class);
        if (annotation != null) {
            String name = "";
            String[] names = ((Parameter) annotation).names();
            for (String n : names) {
                if (n.length() > name.length()) name = n;
            }
            return name;
        }
        return null;
    }

    /**
     * If the given numeric code is non-zero, then return it from this process using System.exit.
     * A null code is taken to be zero, non-Integer values have their toString representation parsed as an integer.
     *
     * @param code
     * @return the given code as an Integer
     */
    public Object exitCode(Object code) {
        if (code == null) code = 0;
        Integer codeValue = code instanceof Integer ? (Integer) code : Integer.parseInt(code.toString());
        if (codeValue != 0) System.exit(codeValue);
        return codeValue;
    }

    /**
     * Return the script arguments as an array of strings.
     * The default implementation is to get the "args" property.
     * If there is no "args" property (or binding) then return null.
     *
     * @return the script arguments as an array of strings.
     */
    public String[] getScriptArguments() {
        try {
            return (String[]) getProperty("args");
        } catch (MissingPropertyException e) {
            return null;
        }
    }

    public Object getScriptParameter(String name) {
        JCommander jc = (JCommander) getProperty(SCRIPT_JCOMMANDER);
        for (ParameterDescription p : jc.getParameters()) {
            for (String n : p.getParameter().names()) {
                if (n.equals(name)) {
                    return p.getObject();
                }
            }
        }

        throw new IllegalArgumentException("No parameter named '" + name + "' found.");
    }

    /**
     * Return the JCommander for this script.
     * If there isn't one already, then create it using createScriptJCommander.
     *
     * @return the JCommander for this script.
     */
    public JCommander getScriptJCommanderWithInit() {
        try {
            JCommander jc = (JCommander) getProperty(SCRIPT_JCOMMANDER);
            if (jc == null) {
                jc = createScriptJCommander();
                // The script has a real property (a field or getter) but if we let Script.setProperty handle
                // this then it just gets stuffed into a binding that shadows the property.
                // This is somewhat related to other bugged behavior in Script wrt properties and bindings.
                // See http://jira.codehaus.org/browse/GROOVY-6582 for example.
                // The correct behavior for Script.setProperty would be to check whether
                // the property has a setter before creating a new script binding.
                this.getMetaClass().setProperty(this, SCRIPT_JCOMMANDER, jc);
            }
            return jc;
        } catch (MissingPropertyException mpe) {
            JCommander jc = createScriptJCommander();
            // Since no property or binding already exists, we can use plain old setProperty here.
            setProperty(SCRIPT_JCOMMANDER, jc);
            return jc;
        }
    }

    /**
     * Create a new (hopefully just once for this script!) JCommander instance.
     * The default name for the command name in usage is the script's class simple name.
     * This is the time to load it up with command objects, which is done by initializeJCommanderCommands.
     *
     * @return A JCommander instance with the commands (if any) initialized.
     */
    public JCommander createScriptJCommander() {
        JCommander jc = new JCommander(this);
        jc.setProgramName(this.getClass().getSimpleName());

        initializeJCommanderCommands(jc);

        return jc;
    }

    /**
     * Add command objects to the given JCommander.
     * The default behavior is to look for JCommanderCommand annotations.
     *
     * @param jc The JCommander instance to add the commands (if any) to.
     */
    public void initializeJCommanderCommands(JCommander jc) {
        Class cls = this.getClass();
        while (cls != null) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                Annotation annotation = field.getAnnotation(Subcommand.class);
                if (annotation != null) {
                    try {
                        field.setAccessible(true);
                        jc.addCommand(field.get(this));
                    } catch (IllegalAccessException e) {
                        printErrorMessage("Trying to add JCommanderCommand but got error '" + e.getMessage()
                                + "' when getting value of field " + field.getName());
                    }
                }
            }

            cls = cls.getSuperclass();
        }
    }

    /**
     * Do JCommander.parse using the given arguments.
     * If you want to do any special checking before the Runnable commands get run,
     * this is the place to do it by overriding.
     *
     * @param jc The JCommander instance for this script instance.
     * @param args  The argument array.
     */
    public void parseScriptArguments(JCommander jc, String[] args) {
        jc.parse(args);
    }

    /**
     * If there are any objects implementing Runnable that are part of this command script,
     * then run them.  If there is a parsed command, then run those objects after the main command objects.
     * Note that this will not run the main script though, we leave that for run to do (which will happen
     * normally since groovy.lang.Script doesn't implement java.lang.Runnable).
     *
     * @param jc
     */
    public void runScriptCommand(JCommander jc) {
        List<Object> objects = jc.getObjects();

        String parsedCommand = jc.getParsedCommand();
        if (parsedCommand != null) {
            JCommander commandCommander = jc.getCommands().get(parsedCommand);
            objects.addAll(commandCommander.getObjects());
        }

        for (Object commandObject : objects) {
            if (commandObject instanceof Runnable) {
                Runnable runnableCommand = (Runnable) commandObject;
                if ((Object) runnableCommand != (Object) this) {
                    runnableCommand.run();
                }
            }
        }
    }

    /**
     * Error messages that arise from command line processing call this.
     * The default is to use the Script's println method (which will go to the
     * 'out' binding, if any, and System.out otherwise).
     * If you want to use System.err, a logger, or something, this is the thing to override.
     *
     * @param message
     */
    public void printErrorMessage(String message) {
        println(message);
    }

    /**
     * If a ParameterException occurs during parseScriptArguments, runScriptCommand, or runScriptBody
     * then this gets called to report the problem.
     * The default behavior is to show the exception message using printErrorMessage, then call printHelpMessage.
     * The return value becomes the return value for the Script.run which will be the exit code
     * if we've been called from the command line.
     *
     * @param jc The JCommander instance
     * @param args The argument array
     * @param pe The ParameterException that occurred
     * @return The value that Script.run should return (2 by default).
     */
    public Object handleParameterException(JCommander jc, String[] args, ParameterException pe) {
        StringBuilder sb = new StringBuilder();

        sb.append("args: [");
        sb.append(join(args, ", "));
        sb.append("]");
        sb.append("\n");

        sb.append(pe.getMessage());

        printErrorMessage(sb.toString());

        printHelpMessage(jc, args);

        return 3;
    }

    /**
     * If a @Parameter whose help attribute is annotated as true appears in the arguments.
     * then the script body is not run and this printHelpMessage method is called instead.
     * The default behavior is to show the arguments and the JCommander.usage using printErrorMessage.
     * The return value becomes the return value for the Script.run which will be the exit code
     * if we've been called from the command line.
     *
     * @param jc The JCommander instance
     * @param args The argument array
     * @return The value that Script.run should return (1 by default).
     */
    public Object printHelpMessage(JCommander jc, String[] args) {
        StringBuilder sb = new StringBuilder();

        jc.usage(sb);

        printErrorMessage(sb.toString());

        return 2;
    }

}
