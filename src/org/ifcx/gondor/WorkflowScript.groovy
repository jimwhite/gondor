package org.ifcx.gondor

import org.ggf.drmaa.JobTemplate
import org.ifcx.drmaa.Workflow
import org.ifcx.drmaa.WorkflowFactory

public abstract class WorkflowScript extends GondorScript implements Workflow {
    /**
     * The workflow script has delegated access (and can override) to all Workflow methods.
     * Note that this field isn't initialized until the <code>run</code> method is called.
     * That means script class (and member) initializers can't use any of them.
     */
    @Delegate
    Workflow workflow

    List<Process> processes = []
    Map<String, Process> processForJobId = [:]
    Map<File, String> jobIdForOutputFile = [:]

    protected abstract Object runWorkflowScriptBody();

    public Object runScriptBody()
    {
        // Waiting until we're run means that script class initializers can't use any of the delegated methods.
        setWorkflow(WorkflowFactory.getFactory().getWorkflow())

        setWorkflowName(this.getClass().name)

//        setTemporaryFilesPath(workflowName + '.jobs')

        init("")

        try {
            runWorkflowScriptBody()

            runJobsForProcesses()

            addWorkflowDependencies()

            createDAGFile(new File(workflowName + '.dag'))
        } finally {
            exit()
        }
    }

    public Command command(Map params, @DelegatesTo(Command) Closure description) {
        new Command(this, params.path, description)
    }

    public Command groovy(Map params) {
        new Command(this, params.path, { _groovy() })
    }

    public Process process(Command command, Map<String, Object> params) {
        def p = command.getArgumentDefaultValues().collectEntries { k, v -> [k, params.containsKey(k) ? params[k] : v]}
        Process process = new Process(command:command, params:p)
        processes.add(process)
        process
    }

    void runJobsForProcesses() { processes.each { runJobForProcess(it) } }

    void runJobForProcess(Process process) {
        JobTemplate jt = process.command.createJobTemplate(process)
        String jobId = runJob(jt)
        processForJobId[jobId] = process
        process.outfiles.each { jobIdForOutputFile[it] = jobId }
    }

    void addWorkflowDependencies() {
        processForJobId.each { String childJobId, Process process ->
            process.infiles.each { File infile ->
                String parentJobId = jobIdForOutputFile[infile]
                if (parentJobId) {
                    addToParentJobIds(childJobId, parentJobId)
                } else {
                    if (!infile.exists()) addWarning("Job $childJobId has input file $infile which does not exist and no job says it will output it.")
                }
            }
        }
    }

    File newTemporaryFile(String prefix, String suffix) {
        File.createTempFile(prefix, suffix, new File(getTemporaryFilesPath()))
    }

/*
    @Override
    void addToParentJobIds(String childJobId, String parentJobId) {
        workflow.addToParentJobIds(childJobId, parentJobId)
    }

    @Override
    void addWarning(String message) {
        workflow.addWarning(message)
    }

    @Override
    void createDAGFile(File dagFile)  throws DrmaaException {
        workflow.createDAGFile(dagFile)
    }

    @Override
    void init(String contact) throws DrmaaException
    {
        workflow.init(contact)
    }

    @Override
    String getWorkflowName() {
        workflow.getWorkflowName()
    }

    @Override
    void setWorkflowName(String name) {
        workflow.setWorkflowName(name)
    }

    @Override
    String getTemporaryFilesPath() {
        workflow.getTemporaryFilesPath()
    }

    @Override
    void setTemporaryFilesPath(String path) {
        workflow.setTemporaryFilesPath(path)
    }

    @Override
    void exit() throws DrmaaException {
        workflow.exit()
    }

    @Override
    JobTemplate createJobTemplate() throws DrmaaException {
        workflow.createJobTemplate()
    }

    @Override
    void deleteJobTemplate(JobTemplate jt) throws DrmaaException {
        workflow.getWorkflowName()
    }

    @Override
    String runJob(JobTemplate jt) throws DrmaaException {
        workflow.runJob(jt)
    }

    @Override
    List runBulkJobs(JobTemplate jt, int start, int end, int incr) throws DrmaaException {
        workflow.runBulkJobs(jt, start, end, incr)
    }

    @Override
    void control(String jobId, int action) throws DrmaaException {
        workflow.control(jobId, action)
    }

    @Override
    void synchronize(List jobIds, long timeout, boolean dispose) throws DrmaaException {
        workflow.synchronize(jobIds, timeout, dispose)
    }

    @Override
    JobInfo wait(String jobId, long timeout) throws DrmaaException {
        workflow.wait(jobId, timeout)
    }

    @Override
    int getJobProgramStatus(String jobId) throws DrmaaException {
        workflow.getJobProgramStatus(jobId)
    }

    @Override
    String getContact() {
        workflow.getContact()
    }

    @Override
    Version getVersion() {
        workflow.getVersion()
    }

    @Override
    String getDrmSystem() {
        workflow.getDrmSystem()
    }

    @Override
    String getDrmaaImplementation() {
        workflow.getDrmaaImplementation()
    }
*/

}
