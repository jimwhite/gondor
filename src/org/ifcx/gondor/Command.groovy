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

    def flag(String lit) {
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            a << lit
        }
    }

    def arg(String name, Closure pat = { it }, Object val = REQUIRED) {
        addArgumentName(name, val)
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            def v = m[name]
            if (v.is(REQUIRED)) {
                System.err.println "Warning: Missing argument value for '$name' in command ${getCommandPath()}"
            } else if (!v.is(OPTIONAL)) {
                a << pat(v).toString()
            }
        }
    }

    def arg(Map m) {
        arg(m.name, m.pat, m.val)
    }

    def infile(String name, File val = null) {
        addArgumentName(name, val)
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            File f = resolveFileArgument(m, name)
            if (f != null) {
                p.infiles << f
                a << stringifyFile(f)
            }
        }
    }

    def outfile(String name, File val = null) {
        addArgumentName(name, val)
        args << { List<String> a, WorkflowScript w, Process p, Map m ->
            File f = resolveFileArgument(m, name)
            if (f != null) {
                p.outfiles << f
                a << stringifyFile(f)
            }
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

    static File resolveFileArgument(Map map, String s) {
        def f = map[s]
//        (f instanceof String) ? new File(f) : ((f instanceof GString) ? new File(f.toString()) : f)
        f
    }

    static String stringifyFile(File file) { file.path }

    WorkflowScript getWorkflowScript() { workflowScript }

//    @Override
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
