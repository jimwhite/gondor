package org.ifcx.gondor

import com.beust.jcommander.Parameter
import org.ggf.drmaa.DrmaaException
import org.ggf.drmaa.JobTemplate
import org.ifcx.drmaa.Workflow
import org.ifcx.drmaa.WorkflowFactory
import groovyx.cli.Default
import org.ifcx.gondor.api.OutputDirectory
import org.ifcx.gondor.api.OutputFile

// Can't use this AST transformation because it happens after ModuleNode generates looks for them.
// @InheritConstructors
public abstract class WorkflowScript extends GondorScript implements Workflow {
    // Can just use @InheritConstructors for brevity and general future-proofing.

    public WorkflowScript() { this(new Binding()) }
    public WorkflowScript(Binding context) { super(context) }

    /**
     * The workflow script has delegated access (and can override) to all Workflow methods.
     * Note that this field isn't initialized until the <code>run</code> method is called.
     * That means script class (and member) initializers can't use any of them.
     */
    @Delegate
    Workflow workflow

    @Parameter(names=['--workflowName', 'workflowName'])
    @Default({ getOwner().getClass().name })
    String workflowName

    @Parameter(names=[ '--workflowDirectory', 'workflowDirectory'])
    @Default({ -> new File(workflowName) })
    @OutputDirectory File workflowDirectory

//    @Parameter(names=['--force', 'force', '-f'], converter = BooleanConverter.class)
//    @Default({ -> false })
//    boolean overwriteDirectories

    // This can't/shouldn't be specified in command line.
    // It is computed from the workflowDirectory parameter.
    @Parameter(names=['--workflowDAGFile', 'workflowDAGFile'])
    @Default({ -> new File(workflowDirectory, DAG_FILE_NAME) })
    @OutputFile File workflowDAGFile

    static final String DAG_FILE_NAME = 'workflow.dag'

    Map<FileType, File> directories = [:]

    static final Map<FileType, String> directoryNames =
            [(FileType.FILE_DIR):'files', (FileType.GIT_DIR):'.git', (FileType.LOG_DIR):'logs',
             (FileType.JOB_DIR):'jobs',(FileType.TMP_DIR):'tmp' ]

    static final def PRE_SCRIPT_USED_JOB_MEMO = 142
//    static final def PRE_SCRIPT_DAG_ABORT = 146

    static final def POST_SCRIPT_DUPLICATED_JOB = 151
    static final def POST_SCRIPT_NO_JOB_RESULT = 152


    // This fails (Groovy 2.4.0): static final Set<Integer> PRE_SCRIPT_SKIP_CODES = [PRE_SCRIPT_USED_JOB_MEMO]
    final Set<Integer> PRE_SCRIPT_SKIP_CODES = [PRE_SCRIPT_USED_JOB_MEMO /*, PRE_SCRIPT_DAG_ABORT*/]

//    static final String PRE_SCRIPT_ARGS = '0 $JOB $RETRY $MAX_RETRIES $DAG_STATUS $FAILED_COUNT '
//
//    static final String POST_SCRIPT_ARGS = '1 $JOB $RETRY $MAX_RETRIES $DAG_STATUS $FAILED_COUNT ' +
//            '$JOBID $RETURN $PRE_SCRIPT_RETURN '
    static final String PRE_SCRIPT_ARGS = '0 $JOB $DAG_STATUS '

    static final String POST_SCRIPT_ARGS = '1 $JOB $DAG_STATUS $RETURN $PRE_SCRIPT_RETURN '


    List<Process> processes = []
    Map<String, Process> processForJobId = [:]
    Map<File, String> jobIdForOutputFile = [:]

    Map<String, String> environment = [:]

    protected abstract Object runWorkflowScriptBody();

    public Object runScriptBody()
    {
        // Waiting until we're run means that script class initializers can't use any of the delegated methods.
        setWorkflow(WorkflowFactory.getFactory().getWorkflow())

        workflow.setWorkflowName(workflowName)

        try {
            // The DAGman workflow implementation uses the temp path for job command files.
            setTemporaryFilesPath(getDirectory(FileType.JOB_DIR).path)
            setLogFilePath(new File(getDirectory(FileType.LOG_DIR), workflowName + '.log').path)

            init("")

            runWorkflowScriptBody()

            runJobsForProcesses()

            addWorkflowDependencies()

            createDAGFile(workflowDAGFile)
        } finally {
            exit()
        }
    }

    @Override
    public void init(String contact) throws DrmaaException {
        // The workflow implementation uses this directory for the generated job command files.
        setTemporaryFilesPath(getDirectory(FileType.JOB_DIR).path)
        workflow.init(contact)
    }

    public Command command(Map params, @DelegatesTo(Command) Closure description) {
        new Command(this, params.path, description)
    }

    public Command groovy(Map params) {
        new Command(this, params.path, { _groovy() })
    }

    public WorkflowCommand workflow(Command command) {
        new WorkflowCommand(this, command)
    }

    public Process process(Command command, Map<String, Object> params) {
        Process process = new Process(this, command, params)
        processes.add(process)
        process
    }

    public Process process(WorkflowCommand command, Map<String, Object> params) {
        String subworkflowName = new File(command.getCommandPath()).name - ~/\.groovy$/
        params.workflowName = subworkflowName
        //FIXME: Default argument values for commands only work for the first/primary name.
        // Probably need to canonicalize parameter names.
        def subworkflowDir = new File(getDirectory(FileType.WORKFLOW_DIR), subworkflowName)
        params.workflowDirectory = subworkflowDir
        //FIXME: Idea is to use the script introspection to give us the computed properties.
        params.workflowDAGFile = new File(subworkflowDir, DAG_FILE_NAME)
//        params.overwriteDirectories = overwriteDirectories as String

        Process process = new WorkflowProcess(this, command, params)
        processes.add(process)
        process
    }

    void runJobsForProcesses() { processes.each { runJobForProcess(it) } }

//    /**
//     * A workflow process runs the dag-generating command as usual, but there is an additional job for
//     * the subdag which is necessarily a child of the dag-generation JOB.  We then use the subdag's
//     * job id as that of the workflow so that our children (if any) will wait for the subdag to complete.
//     *
//     * Note that WorkflowScript.runJobForProcess is going to use the SUBDAG job id for all outputs of
//     * this command, including the dag file itself.  This is alright as long as no one tries to analyze
//     * the SUBDAG "job" like other jobs wrt to its input files.  Not really to weird since since
//     * those two kinds of jobs are pretty different (no pre-/post- scripts etc).
//     *
//     * @return
//     */
//    String runJobForWorkflow(WorkflowProcess process) {
//        String dagFileJobId = runJob(process.setUpJob(createJobTemplate()))
//        File dagFile = process.params.workflowDAGFile
//        String subDAGJobId = runWorkflow(dagFile)
//        addToParentJobIds(subDAGJobId, dagFileJobId)
//        subDAGJobId
//    }

    String runJobForProcess(Process process) {
        String jobId = runJob(process.setUpJob(createJobTemplate()))
        setJobPreScript(jobId, process.createPreScript(jobId).path, PRE_SCRIPT_ARGS, PRE_SCRIPT_SKIP_CODES)
        setJobPostScript(jobId, process.createPostScript(jobId).path, POST_SCRIPT_ARGS)

        // If this is a workflow process then we need to add the SUBDAG node too.
        if (process instanceof WorkflowProcess) {
            def dagFile = process.params.workflowDAGFile
            String subDAGJobId = runWorkflow(dagFile)
            // The parent of the SUBDAG job is the DAG file generation job.
            addToParentJobIds(subDAGJobId, jobId)
            // Children of the DAG file generator use the SUBDAG as their parent.
            // That is because we assume that only the DAG file is really ready
            // and that the other outputs could actually be computed by the subworkflow.
            jobId = subDAGJobId
        }

        processForJobId[jobId] = process
        process.outfiles.each { jobIdForOutputFile[it] = jobId }
        jobId
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

    /**
     * Note that paths in the environment are expected to be absolute since we may not
     * necessarily be run in the directory that was current when the workflow script was run.
     */
    Map<String, String> copyEnvironment(Object... vars)
    {
        // This is probably not a great idea, but it's here is you want it.
        // Note that this is just if the parameter list is empty as in clone_environment().
        // clone_environment([]) is a no-op (one parameter - no variable names).
        if (!vars) { vars = System.getenv().keySet() }

        vars.flatten().collectEntries {
            String var -> def value = System.getenv(var); if (value) environment[var] = value ; [var, value]
        }
    }

    // FIXME: These paths should be relativized like the others.
    def prependClasspath(Object... args)
    {
        def classpaths = normalize_file_argument(args)*.canonicalPath

        if (environment.CLASSPATH) classpaths << environment.CLASSPATH

        environment.CLASSPATH = classpaths.join(File.pathSeparator)
    }

    // FIXME: This should be using the same logic in Command.
    List<File> normalize_file_argument(arg) {
        if (arg instanceof String) {
            new File(arg as String)
        } else {
            arg.collect { it instanceof File ? [it] : new File(it as String) }
        }
    }


//    Integer temporaryFileNumber = 0
//
//    File newTemporaryFile(String prefix, String suffix) {
////        File.createTempFile(prefix, suffix, new File(getTemporaryFilesPath()))
////        new File(getTemporaryFilesPath(), "${prefix}_${++temporaryFileNumber}$suffix")
//        new File(getTemporaryFilesPath(), String.format("%s_%04d_%s", prefix, ++temporaryFileNumber, suffix))
//    }

    File getDirectory(FileType type) {
        File dir = directories[type]

        if (dir == null) {
            dir = (type == FileType.WORKFLOW_DIR) ? workflowDirectory : new File(workflowDirectory, directoryNames[type])
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new FailedFileSystemOperation("Failed to create new $type directory: $dir")
                }
            }
            directories[type] = dir
        }

        dir
    }

//    File initializeDirectory(FileType t, File d) {
//
//    }

    Integer directoryNumber = 0

    File newDirectory(FileType type, String prefix) {
        new File(getDirectory(type), String.format("%s_%04d", prefix, ++directoryNumber))
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

    @Override
    JobTemplate createJobTemplate() throws DrmaaException {
        JobTemplate jt = workflow.createJobTemplate()
        jt.setJobEnvironment(environment)
        jt
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
