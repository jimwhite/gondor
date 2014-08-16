package org.ifcx.gondor

import com.beust.jcommander.Parameter

import org.ggf.drmaa.JobTemplate
import org.ifcx.gondor.api.Initializer
import org.ifcx.gondor.api.InputDirectory
import org.ifcx.gondor.api.InputFile
import org.ifcx.gondor.api.OutputDirectory
import org.ifcx.gondor.api.OutputFile

import java.lang.annotation.Annotation
import java.lang.reflect.Field

// first-stage/PARSE/parseIt -l399 -N50 first-stage/DATA/EN/ $*
// parse_nbest = gondor.condor_command(new File(bllip_dir, 'first-stage/PARSE/parseIt'), ['-K.flag', '-l400.flag', '-N50.flag', 'model.in', 'input.in'])
//
// second-stage/programs/features/best-parses" -l "$MODELDIR/features.gz" "$MODELDIR/$ESTIMATORNICKNAME-weights.gz"
// rerank_parses = gondor.condor_command(new File(bllip_dir, 'second-stage/programs/features/best-parses'), ['-l.flag', 'features.in', 'weights.in', 'infile.in'])
//
// parse_nbest(model:PARSER_MODEL, input:charniak_input, stdout:nbest_output)
// rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS, stdin:nbest_output, stdout:reranker_output)

class Command extends Closure<Process>
{
    final WorkflowScript _workflow
    final private String _commandPath

    Closure<JobTemplate> jobTemplateCustomizer // = { it }

    Command(WorkflowScript workflow, String path, @DelegatesTo(Command) Closure desc) {
        super(desc.owner)
        this._workflow = workflow
        this._commandPath = path
        desc.delegate = this
        desc.call(this)
    }

    final static def VARARGS_PARAMETER_NAME = ':args'

    public static boolean isProcessParameter(String name) { name.startsWith(':') }

    /** TODO: Convert these to enums. **/

    final static def EXECUTABLE = new Object()
    final static def GROOVY = new Object()

    final static def REQUIRED = new Object()
    final static def OPTIONAL = new Object()

    def type = EXECUTABLE

    Map<String, Object> _argumentDefaultValues = [:]
    List<Closure> args = []

    WorkflowScript getWorkflowScript() { _workflow }

    String getCommandPath() { _commandPath }

    Map<String, Object> getArgumentDefaultValues() {
        _argumentDefaultValues
    }

    def flag(String lit, Closure pat = { it }) {
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            switch (pat.maximumNumberOfParameters) {
                case 0 : addArguments(a, pat()) ; break
                case 1 : addArguments(a, pat(lit)) ; break
                case 2 : addArguments(a, pat(lit, m)) ; break
                default : addArguments(a, pat(lit, m, p))
            }
        }
    }

    def flag(Map m) {
        if (m.containsKey('format')) {
            flag(m.value, (Closure) m.format)
        } else {
            flag((String) m.value)
        }
    }

    def arg(String name, Object val = REQUIRED, Closure pat = { it }) {
        addArgumentName(name, val)
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            def v = m[name]
            if (v.is(REQUIRED)) {
                System.err.println "Warning: Missing argument value for '$name' in command ${getCommandPath()}"
            } else if (!v.is(OPTIONAL)) {
                (v instanceof Collection ? v.flatten() : [v]).each { addArguments(a, pat(it)) }
//                    def vs = pat(it)
//                    (vs instanceof Collection ? vs.flatten() : [vs]).each { addArguments(a, it) }
//                }
            }
        }
    }

    def arg(String name, Object val = REQUIRED, Map mappings) {
        addArgumentName(name, val)
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            def v = m[name]
            if (v.is(REQUIRED)) {
                System.err.println "Warning: Missing argument value for '$name' in command ${getCommandPath()}"
            } else if (!v.is(OPTIONAL)) {
                (v instanceof Collection ? v.flatten() : [v]).each { addArguments(a, mapArgument(name, mappings, it)) }
//                    def vs = pat(it)
//                    (vs instanceof Collection ? vs.flatten() : [vs]).each { addArguments(a, it) }
//                }
            }
        }
    }

    def arg(Map m) {
        if (m.containsKey('format')) {
            arg((String) m.name, m.containsKey('value') ? m.value : REQUIRED, m.format)
        } else {
            arg((String) m.name, m.containsKey('value') ? m.value : REQUIRED)
        }
    }

    def infile(String name, File val = null, Closure pat = { File f -> f != null ? f.path : [] }) {
        addArgumentName(name, val)
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            resolveFileArgument(m, name).each { File f ->
                if (val != null) {
                    System.err.println "Warning: File argument $name in command $_commandPath must have value $val but is given $f"
                }
                p.infiles << f
                addArguments(a, pat(f))
            }
        }
    }

    def infile(String name, Closure<List<String>> pat) { infile(name, null, pat) }

    def infile(Map m) {
        if (m.containsKey('format')) {
            infile((String) m.name, m.containsKey('value') ? (File) m.value : null, (Closure) m.format)
        } else {
            infile((String) m.name, m.containsKey('value') ? (File) m.value : null)
        }
    }

    def outfile(String name, File val = null, Closure pat = { File f -> f.path }) {
        addArgumentName(name, val)
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            resolveFileArgument(m, name).each { File f ->
                if (val != null && val != f) {
                    System.err.println "Warning: File argument ${name} in command ${getCommandPath()} must have value $val but is given $f"
                }
                p.outfiles << f
                addArguments(a, pat(f))
            }
        }
    }

    def outfile(String name, Closure<List<String>> pat) { outfile(name, null, pat) }

    def outfile(Map m) {
        if (m.containsKey('format')) {
            outfile((String) m.name, m.containsKey('value') ? (File) m.value : null, (Closure) m.format)
        } else {
            outfile((String) m.name, m.containsKey('value') ? (File) m.value : null)
        }
    }

    def jobTemplate(@DelegatesTo(JobTemplate) Closure setupJT) {
        jobTemplateCustomizer = setupJT
    }

    void addArgumentName(String name, Object val) {
        if (getArgumentDefaultValues().containsKey(name)) {
            System.err.println "Warning: Duplicated argument name '$name' in command $_commandPath"
        }
        getArgumentDefaultValues()[name] = val
    }

    /**
     * Get the value(s) supplied for a file argument.
     * If the given value is a single File it is boxed up as a List<File>.
     *
     * @param map
     * @param s
     * @return
     */
    static def resolveFileArgument(Map map, String s) {
        def f = map[s]
        // Don't do automatic coercion of strings to files for now to cut down on silent bugs.
        // (f instanceof String) ? new File(f) : ((f instanceof GString) ? new File(f.toString()) : f)
        f instanceof Collection ? f.flatten() : (f != null ? [f] : null)
    }

    public static List<String> stringify(v) {
        v instanceof Collection ? v.flatten().collect { it as String } : [v as String]
    }

    static void addArguments(List<String> a, def v) {
        a.addAll(stringify(v))
    }

    static mapArgument(String name, Map mappings, def v) {
        if (!mappings.containsKey(v)) {
            throw new IllegalArgumentException("Argument '$name' should have a value in ${mappings.keySet()} but got '$v'.")
        }
        def mapping = mappings[v]
        (mapping instanceof Closure) ? mapping(v) : mapping
    }

//    static String stringifyFile(File file) { file.path }

    // This is a Gondor command in Groovy.  Get the metadata from the annotations.
    def _groovy() {
        println "Inspecting Groovy command $_commandPath"

        GroovyShell shell = new GroovyShell()
        Script script = shell.parse(new File(_commandPath))
        Class scriptClass = script.getClass();

        def parameters = getParameterAnnotations(scriptClass)

        if (parameters) {
            parameters.each {
                println it
                Class initializer = it.initializer?.value()
                if (initializer) {
                    def val = ((Closure) (initializer.newInstance(script, script))).call()
                    println(val)
                    it.value = val
                }
            }
            println "${parameters.size()} parameters"

            parameters.each { parameter ->
                def name = parameter.name ?: VARARGS_PARAMETER_NAME
                if (parameter.infile) {
                    if (parameter.outfile) {
                        System.err.println "Error: Parameter ${it.name} in $_commandPath is marked as both an infile and an outfile."
                    }
                    def value = null
                    if (parameter.initializer) value = parameter.value
                    parameter.name ? infile(name:name, value:value, format: isProcessParameter(name) ? {[]} : {[name, it]}) : infile(name:name)
                } else if (parameter.outfile) {
                    def value = null
                    if (parameter.initializer) value = parameter.value
                    parameter.name ? outfile(name:name, value:value, format: isProcessParameter(name) ? {[]} : {[name, it]}) : outfile(name:name)
                } else {
                    if (parameter.name) {
                        def value = parameter.required ? REQUIRED : OPTIONAL
                        if (parameter.initializer) value = parameter.value
                        arg(name: name, value: value, format:{ [name, it] })
                    } else {
                        arg(name:VARARGS_PARAMETER_NAME, value:parameter.required ? REQUIRED : OPTIONAL)
                    }
                }
            }
        }
    }

    def getParameterAnnotations(Class cls) {
        def parameters = []
        while (cls != null) {
            Field[] fields = cls.declaredFields;
            for (Field field : fields) {
                Annotation annotation = field.getAnnotation(Parameter.class);
                if (annotation != null) {
//                    println annotation
//                    def name = annotation.names().first()
//                    def required = annotation.required()
                    def name = annotation.names()?.size() ? annotation.names().first() : null
                    def infile = (field.getAnnotation(InputFile.class) != null || field.getAnnotation(InputDirectory.class) != null )
                    def outfile = (field.getAnnotation(OutputFile.class) != null || field.getAnnotation(OutputDirectory.class) != null )
                    def initializer = field.getAnnotation(Initializer.class)
                    parameters << [name: name, required:annotation.required(), initializer:initializer, infile:infile, outfile:outfile]
                }
            }

            cls = cls.getSuperclass();
        }

//        // Sort by the first name is the names list, if any.
//        // The "no name" parameter (empty array for names()), if there is one, will be first with standard sort.
//        // parameters.sort { it.name }
//        // But if there is an varargs (unnamed) parameter it should be at the end.
//        // Use a comparator that says false (null or empty strings) is greater than anything.
//        parameters.sort { a, b -> a.name ? (b.name ? a.name <=> b.name : 0) : 1 }

        parameters
    }

    /**
     * Since we allow optional arguments, we might get called with none, so handle that case
     * and treat it as though we'd been called with an empty map.
     */
    Process call() {
        call([:])
    }

    /**
     * This is the method called when this closure is called to run this command
     * in the workflow with the given parameter values.
     */
    Process call(Map params, Object... args) {
        Map<String, Object> p = (Map) params.clone()
        p[VARARGS_PARAMETER_NAME] = args as List
        call(p)
    }

    /**
     * This is the method called when this closure is called to run this command
     * in the workflow with the given parameter values.
     */
    Process call(Map params) {
        getWorkflowScript().process(this, params)
    }

}
