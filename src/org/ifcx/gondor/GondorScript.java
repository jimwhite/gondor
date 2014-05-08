package org.ifcx.gondor;

//import groovy.lang.Closure;
//import groovy.lang.GroovyInterceptable;
//import groovy.lang.MissingMethodException;
//import groovy.lang.MissingPropertyException;
//import org.apache.ivy.util.StringUtils;
//import org.codehaus.groovy.runtime.MethodClosure;
//import org.ggf.drmaa.JobTemplate;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;

import groovyx.cli.JCommanderScript;

public abstract class GondorScript extends JCommanderScript {
//    abstract public Process process(Command command, Map<String, Object> params);

//    @Override
//    public Object getProperty(String property) {
//        try {
//            return super.getProperty(property);
//        } catch (MissingPropertyException e) {
////            if ("ls".equals(property)) return "/bin/ls";
//            if ("ls".equals(property)) {
//                // TODO: Don't do this, implement a suitable Binding class.
//                // invokeMethod looks at binding.getVariable not getProperty.
//                // Fixed by GROOVY-6582 but released when?  Can patch our invokeMethod.
////                setProperty(property, new MethodClosure(this, "doLs"));
////                return super.getProperty(property);
//                return new MethodClosure(lsCommand, "run");
//            }
//            if ("wget".equals(property)) return "/usr/bin/wget";
//            throw e;
//        }
//    }

//    public GondorScript()
//    {
//        super(new CommandBindings());
//    }
//
//    @Override
//    public Object invokeMethod(String name, Object args) {
//        try {
//            System.out.println("invoking " + name);
//            return super.invokeMethod(name, args);
//        }
//        // if the method was not found in the current scope (the script's methods)
//        // let's try to see if there's a method closure with the same name in the binding
//        catch (MissingMethodException mme) {
//            try {
//                if (name.equals(mme.getMethod())) {
//                    // We do this again here because there is a bug in Script.invokeMethod.
//                    // See GROOVY-6582.  It will be fixed but has been broken a long time,
//                    // so make sure we get it right by doing the right thing here.
//                    Object boundClosure = getProperty(name);
//                    if (boundClosure != null && boundClosure instanceof Closure) {
//                        return ((Closure) boundClosure).call((Object[])args);
//                    } else {
//                        throw mme;
//                    }
//                } else {
//                    throw mme;
//                }
//            } catch (MissingPropertyException mpe) {
//                throw mme;
//            }
//        }
//    }

//    LsCommand lsCommand = new LsCommand();
//
//    static class LsCommand {
//        public LsCommand() {
//        }
//
//        public String run() {
//            return "/bin/ls0";
//        }
//
//        public String run(Object dir, Object pattern) {
//            List a = new ArrayList();
//            a.add("/bin/ls1");
//            a.add(dir.toString());
//            a.add(pattern.toString());
//            return StringUtils.join(a.toArray(), " ");
//        }
//
//        public String run(Object... args) {
//            List a = new ArrayList();
//            a.add("/bin/ls2");
//            a.addAll(Arrays.asList(args));
//            return StringUtils.join(a.toArray(), " ");
//        }
//    }
}
