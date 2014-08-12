package org.ifcx.gondor

import org.ggf.drmaa.JobTemplate

/**
 * Created by jim on 5/16/14.
 */
class WorkflowCommand extends Command {
    // Alas recursive delegation to our own class using @Delegate is not supported.
    Command dagProducerCommand

//    Command(WorkflowScript workflow, String path, @DelegatesTo(Command) Closure desc) {
    WorkflowCommand(WorkflowScript workflow, Command command) {
        super(workflow, null, { _workflow(command) } )
        assert workflow == command.getWorkflowScript()
    }

    def _workflow(Command command) {
        dagProducerCommand = command
    }

//    @Override
//    Process call(Map params) {
//        Process process = dagProducerCommand.call(params)
//        process.setCommand(this)
//        process
//    }

//    @Override
//    String runJob(Process process) {
//        String dagFileJobId = dagProducerCommand.runJob(process)
//        File dagFile = process.output
//        String subDAGJobId = getWorkflow().runWorkflow(dagFile)
//        getWorkflow().addToParentJobIds(subDAGJobId, dagFileJobId)
//        subDAGJobId
//    }

    @Override
    def flag(String lit, Closure pat) {
        return dagProducerCommand.flag(lit, pat)
    }

    @Override
    def flag(String lit) {
        return dagProducerCommand.flag(lit)
    }

    @Override
    def arg(String name, Object val, Closure pat) {
        return dagProducerCommand.arg(name, val, pat)
    }

    @Override
    def arg(String name, Object val) {
        return dagProducerCommand.arg(name, val)
    }

    @Override
    def arg(String name) {
        return dagProducerCommand.arg(name)
    }

    @Override
    def infile(String name, File val, Closure pat) {
        return dagProducerCommand.infile(name, val, pat)
    }

    @Override
    def infile(String name, File val) {
        return dagProducerCommand.infile(name, val)
    }

    @Override
    def infile(String name) {
        return dagProducerCommand.infile(name)
    }

    @Override
    def outfile(String name, File val, Closure pat) {
        return dagProducerCommand.outfile(name, val, pat)
    }

    @Override
    def outfile(String name, File val) {
        return dagProducerCommand.outfile(name, val)
    }

    @Override
    def outfile(String name) {
        return dagProducerCommand.outfile(name)
    }

    @Override
    Object getType() {
        return dagProducerCommand.getType()
    }

    @Override
    void setType(Object type) {
        dagProducerCommand.setType(type)
    }

    @Override
    Map<String, Object> get_argumentDefaultValues() {
        return dagProducerCommand.get_argumentDefaultValues()
    }

    @Override
    void set_argumentDefaultValues(Map<String, Object> _argumentDefaultValues) {
        dagProducerCommand.set_argumentDefaultValues(_argumentDefaultValues)
    }

    @Override
    List<Closure> getArgs() {
        return dagProducerCommand.getArgs()
    }

    @Override
    void setArgs(List<Closure> args) {
        dagProducerCommand.setArgs(args)
    }

    @Override
    WorkflowScript getWorkflowScript() {
        return dagProducerCommand.getWorkflowScript()
    }

    @Override
    String getCommandPath() {
        return dagProducerCommand.getCommandPath()
    }

    @Override
    Map<String, Object> getArgumentDefaultValues() {
        return dagProducerCommand.getArgumentDefaultValues()
    }

    @Override
    def flag(Map m) {
        return dagProducerCommand.flag(m)
    }

    @Override
    def arg(Map m) {
        return dagProducerCommand.arg(m)
    }

    @Override
    def infile(String name, Closure<List<String>> pat) {
        return dagProducerCommand.infile(name, pat)
    }

    @Override
    def infile(Map m) {
        return dagProducerCommand.infile(m)
    }

    @Override
    def outfile(String name, Closure<List<String>> pat) {
        return dagProducerCommand.outfile(name, pat)
    }

    @Override
    def outfile(Map m) {
        return dagProducerCommand.outfile(m)
    }

    @Override
    @Override
    def jobTemplate(@DelegatesTo(JobTemplate) Closure setupJT) {
        return dagProducerCommand.jobTemplate(setupJT)
    }

    @Override
    void addArgumentName(String name, Object val) {
        dagProducerCommand.addArgumentName(name, val)
    }

    @Override
    def _groovy() {
        return dagProducerCommand._groovy()
    }

    @Override
    def getParameterAnnotations(Class cls) {
        return dagProducerCommand.getParameterAnnotations(cls)
    }

}
