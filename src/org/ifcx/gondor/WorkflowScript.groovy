package org.ifcx.gondor

import com.beust.jcommander.Parameter
import org.ggf.drmaa.JobTemplate
import org.ifcx.drmaa.Workflow
import org.ifcx.drmaa.WorkflowFactory
import org.ifcx.gondor.api.Initializer
import org.ifcx.gondor.api.OutputFile

public abstract class WorkflowScript extends GondorScript implements Workflow {
    /**
     * The workflow script has delegated access (and can override) to all Workflow methods.
     * Note that this field isn't initialized until the <code>run</code> method is called.
     * That means script class (and member) initializers can't use any of them.
     */
    @Delegate
    Workflow workflow

    @Parameter(names=['workflowName'])
//    @Initializer({ getClass().name })
    String workflowName = getClass().name

    @Parameter(names=Process.OUTPUT)
//    @Initializer({ new File(it.workflowName + '.dag') })
//    @Initializer({ -> dagFile })
    @Initializer({ -> new File(workflowName + '.dag') })
//    @OutputFile(name=Process.OUTPUT) File dagFile = new File(workflowName + '.dag')
    @OutputFile File dagFile = new File(workflowName + '.dag')

    List<Process> processes = []
    Map<String, Process> processForJobId = [:]
    Map<File, String> jobIdForOutputFile = [:]

    protected abstract Object runWorkflowScriptBody();

    public Object runScriptBody()
    {
        // Waiting until we're run means that script class initializers can't use any of the delegated methods.
        setWorkflow(WorkflowFactory.getFactory().getWorkflow())

        workflow.setWorkflowName(workflowName)

//        setTemporaryFilesPath(workflowName + '.jobs')

        init("")

        try {
            runWorkflowScriptBody()

            runJobsForProcesses()

            addWorkflowDependencies()

            createDAGFile(dagFile)
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

    public WorkflowCommand workflow(Command command) {
        new WorkflowCommand(command)
    }

    public Process process(Command command, Map<String, Object> params) {
        Process process = new Process(this, command, params)
        processes.add(process)
        process
    }

    void runJobsForProcesses() { processes.each { runJobForProcess(it) } }

    void runJobForProcess(Process process) {
        String jobId = process.runJob()
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

    Integer temporaryFileNumber = 0

    File newTemporaryFile(String prefix, String suffix) {
//        File.createTempFile(prefix, suffix, new File(getTemporaryFilesPath()))
//        new File(getTemporaryFilesPath(), "${prefix}_${++temporaryFileNumber}$suffix")
        new File(getTemporaryFilesPath(), String.format("%s_%04d_%s", prefix, ++temporaryFileNumber, suffix))
    }

    @Override
    String getWorkflowName() {
        workflowName
    }

    @Override
    void setWorkflowName(String name) {
        // Can't change workflow name after class initialization.
        // Specify a different name on the command line if you want a different name than the default (class name).
        // The reason is that the value for dagFile, the name of the output must be determined prior to run time.
        throw new IllegalStateException("Error: Attepmt to change workflow name from $workflowName to $name after initialization.")
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
