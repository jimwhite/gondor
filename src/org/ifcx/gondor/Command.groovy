package org.ifcx.gondor

import org.ggf.drmaa.JobTemplate


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
    String commandPath

    private Closure<JobTemplate> jobTemplateCustomizer // = { it }
    private WorkflowScript workflowScript

    Command(WorkflowScript workflowScript, String path, @DelegatesTo(Command) Closure desc) {
        super(desc.owner)
        this.workflowScript = workflowScript
        this.commandPath = path
        desc.delegate = this
        desc.call(this)
    }

    final static def REQUIRED = new Object()
    final static def OPTIONAL = new Object()

    Map<String, Object> argumentDefaultValues = [:]
    List<Closure> args = []

    def flag(String lit, Closure pat = { it }) {
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            switch (pat.maximumNumberOfParameters) {
                case 1 : addArguments(a, pat(lit)) ; break
                case 2 : addArguments(a, pat(m, lit)) ; break
                default : addArguments(a, pat(p, m, lit))
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

    def arg(Map m) {
        if (m.containsKey('format')) {
            arg((String) m.name, m.containsKey('value') ? m.value : REQUIRED, (Closure) m.format)
        } else {
            arg((String) m.name, m.containsKey('value') ? m.value : REQUIRED)
        }
    }

    def infile(String name, File val = null, Closure pat = { File f -> f.path }) {
        addArgumentName(name, val)
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            resolveFileArgument(m, name).each { File f ->
                if (val != null) {
                    System.err.println "Warning: File argument $name in command $commandPath must have value $val but is given $f"
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
            File f = resolveFileArgument(m, name)
            if (val != null && val != f) {
                System.err.println "Warning: File argument $name in command $commandPath must have value $val but is given $f"
            }
            if (f != null) {
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
        if (argumentDefaultValues.containsKey(name)) {
            System.err.println "Warning: Duplicated argument name '$name' in command $commandPath"
        }
        argumentDefaultValues[name] = val
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
        f instanceof Collection ? f : [f]
    }

    static void addArguments(List<String> a, def v) {
        if (v instanceof Collection) {
            v.flatten().each { a << (it as String) }
        }  else {
            a << (v as String)
        }
    }

//    static String stringifyFile(File file) { file.path }

    WorkflowScript getWorkflowScript() { workflowScript }

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
    Process call(Map params) {
        workflowScript.process(this, params)
    }

    JobTemplate createJobTemplate(Process process) {
        JobTemplate jt = workflowScript.createJobTemplate()

        jt.remoteCommand = process.command.getCommandPath()

        List<String> jobArgs = []
        args.each { Closure ac ->
            ac(jobArgs, getWorkflowScript(), process, process.params)
        }
        jt.args = jobArgs

        if (process._stdin != null) jt.setInputPath(process._stdin.path)
        if (process._stdout != null) jt.setOutputPath(process._stdout.path)
        if (process._stderr != null) jt.setErrorPath(process._stderr.path)

        if (jobTemplateCustomizer) jobTemplateCustomizer(jt)

        jt
    }

    File newTemporaryFile(String s) {
        getWorkflowScript().newTemporaryFile(commandPath.replaceAll(/\W/, /_/), s)
    }
}
