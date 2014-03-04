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
//        desc.resolveStrategy = Closure.DELEGATE_FIRST
        desc.delegate = this
        desc.call(this)
    }

    List<Closure> args = []

    def flag(String lit) { args << { List<String> a, WorkflowScript w, Process p, Map m ->
        a << lit
    } }

    def infile(String name) { args << { List<String> a, WorkflowScript w, Process p, Map m ->
        File f = resolveFileArgument(m, name)
        if (f != null) {
            p.infiles << f
            a << stringifyFile(f)
        }
    }}

    def outfile(String name) { args << { List<String> a, WorkflowScript w, Process p, Map m ->
        File f = resolveFileArgument(m, name)
        if (f != null) {
            p.outfiles << f
            a << stringifyFile(f)
        }
    }}

    def jobTemplate(@DelegatesTo(JobTemplate) Closure setupJT) {
        jobTemplateCustomizer = setupJT
    }

    static File resolveFileArgument(Map map, String s) { map[s] }

    static String stringifyFile(File file) { file.path }

    WorkflowScript getWorkflowScript() { workflowScript }

    @Override
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
