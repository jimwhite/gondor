package org.ifcx.gondor

import com.beust.jcommander.Parameter
import com.beust.jcommander.converters.BooleanConverter
import groovy.transform.InheritConstructors
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

    @Parameter(names=['--force', 'force', '-f'], converter = BooleanConverter.class)
    @Default({ -> false })
    boolean overwriteDirectories

    // This can't/shouldn't be specified in command line.
    // It is computed from the workflowDirectory parameter.
    @Parameter(names=['--workflowDAGFile', 'workflowDAGFile'])
    @Default({ -> new File(workflowDirectory, DAG_FILE_NAME) })
    @OutputFile File workflowDAGFile

    static final String DAG_FILE_NAME = 'workflow.dag'
    static final String FILE_DIR_NAME = 'files'
    static final String LOG_DIR_NAME = 'logs'
    static final String TMP_DIR_NAME = 'templates'

    List<Process> processes = []
    Map<String, Process> processForJobId = [:]
    Map<File, String> jobIdForOutputFile = [:]

    Map<String, String> environment = [:]

    Map<FileType, File> directories

    protected abstract Object runWorkflowScriptBody();

    public Object runScriptBody()
    {
        // Waiting until we're run means that script class initializers can't use any of the delegated methods.
        setWorkflow(WorkflowFactory.getFactory().getWorkflow())

        workflow.setWorkflowName(workflowName)

        try {
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
        setTemporaryFilesPath(getDirectory(FileType.TMP_DIR).path)
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
        params.overwriteDirectories = overwriteDirectories as String

        Process process = new WorkflowProcess(this, command, params)
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
        if (directories == null) {
            directories = [
                    (FileType.FILE_DIR)  : new File(workflowDirectory, FILE_DIR_NAME)
                    , (FileType.LOG_DIR) : new File(workflowDirectory, LOG_DIR_NAME)
                    , (FileType.TMP_DIR) : new File(workflowDirectory, TMP_DIR_NAME)
                    , (FileType.WORKFLOW_DIR) : workflowDirectory
            ]

            directories.each { FileType t, File d ->
                if (d.exists()) {
                    if (overwriteDirectories) {
                        if (!d.deleteDir()) {
                            throw new FailedFileSystemOperation("Failed to delete existing $t directory: $d")
                        }
                    } else {
                        throw new IllegalWorkflowOperation("$t directory exists but we don't overwrite without --force: $d")
                    }
                }

                if (!d.mkdirs()) {
                    throw new FailedFileSystemOperation("Failed to create new $t directory: $d")
                }
            }
        }

        directories[type]
    }

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
