package org.ifcx.gondor

import org.ggf.drmaa.AlreadyActiveSessionException
import org.ggf.drmaa.DrmaaException
import org.ggf.drmaa.InternalException
import org.ggf.drmaa.InvalidJobException
import org.ggf.drmaa.JobInfo
import org.ggf.drmaa.JobTemplate
import org.ggf.drmaa.Version
import org.ifcx.drmaa.GondorJobTemplate
import org.ifcx.drmaa.Workflow

public class WorkflowImpl implements Workflow {
    private static final Version VERSION = new Version(0, 1)

    private boolean hasInitialized = false;

    private String _contact = ""
    private String _workflowName = "gondor_default_workflow"
    private String _temporaryFilesPath
    private String _logFilePath

    private File temporaryFilesDir

    // FIXME: This is here cuz Job looks at it but this isn't really configured to the right place yet.
    File workingDir = new File('.')
//    private File logFile

    private int job_number = 0
    private int job_template_number = 0

    int nextJobNumber() { ++job_number }
    int nextJobTemplateNumber() { ++job_template_number }

//    Map<JobTemplate, JobTemplate> jobTemplateMap = [:]
//    Map<JobTemplate, File> jobTemplateFiles = [:]

    Map<String, Job> jobs = [:]
    Set<String> parentJobIds = []

    List<String> warnings = []

    @Override
    void init(String contact) throws DrmaaException {
        if (hasInitialized) throw new AlreadyActiveSessionException("Can only initialize a workflow once.")

        this._contact = contact

        if (!temporaryFilesPath) setTemporaryFilesPath(workflowName + ".jobs")

        temporaryFilesDir = new File(temporaryFilesPath)

        if (!temporaryFilesDir.exists() && !temporaryFilesDir.mkdirs()) {
            throw new InternalException("Can't create directory $temporaryFilesDir for job templates.")
        }

        if (!temporaryFilesDir.isDirectory()) {
            throw new InternalException("The file $temporaryFilesDir exists where we want to put the job templates dir.")
        }

        if (!logFilePath) setLogFilePath(new File(temporaryFilesDir, workflowName + ".log").path)

//        logFile = new File(logFilePath)

        hasInitialized = true;
    }


    @Override
    String getWorkflowName() {
        this._workflowName
    }

    @Override
    void setWorkflowName(String name) {
        if (hasInitialized) throw new AlreadyActiveSessionException("Can't change workflowName after initialization.")
        this._workflowName = name
    }

    @Override
    String getTemporaryFilesPath() {
        _temporaryFilesPath
    }

    @Override
    void setTemporaryFilesPath(String path) {
        if (hasInitialized) throw new AlreadyActiveSessionException("Can't change temporaryFilesPath after initialization.")
        _temporaryFilesPath = path
    }

    @Override
    String getLogFilePath() {
        _logFilePath
    }

    @Override
    void setLogFilePath(String path) {
        if (hasInitialized) throw new AlreadyActiveSessionException("Can't change logFilePath after initialization.")
        _logFilePath = path
    }

    @Override
    void createDAGFile(File dagFile) {
        writeDAGFile(dagFile)
    }

    @Override
    void exit() throws DrmaaException {

    }

    @Override
    JobTemplate createJobTemplate() throws DrmaaException {
        new JobTemplateImpl()
    }

    @Override
    void deleteJobTemplate(JobTemplate jt) throws DrmaaException {
//        jobTemplateMap.remove(jt)
    }

    @Override
    String runJob(JobTemplate jt) throws DrmaaException {
        Job job = new Job(workflow:this)
        job.init((GondorJobTemplate) jt)
        String jobId = job.id
        jobs[jobId] = job
        return jobId
    }

    @Override
    List runBulkJobs(JobTemplate jt, int start, int end, int incr) throws DrmaaException {
        if (incr > end - start) incr = Math.max(end - start, 1)
        def jobIds = (start..end).step(incr).collect { procId ->
            Job job = new Job(workflow:this)
            job.init((GondorJobTemplate) jt)
            def jobId = job.id + String.sprintf('_%03d', procId)
            job.id = jobId
            job.procId = procId
            jobs[jobId] = job
            jobId
        }
        assert jobIds.size() == Math.floor(((end - start) / incr) + 1)
        jobIds
    }

    String runWorkflow(File dagFile) throws DrmaaException {
        SubDAG job = new SubDAG(workflow:this)
        job.init(dagFile)
        def jobId = job.id
        jobs[jobId] = job
        jobId
    }

    @Override
    void control(String jobId, int action) throws DrmaaException {
        throw new IllegalWorkflowOperation()
    }

    @Override
    void synchronize(List jobIds, long timeout, boolean dispose) throws DrmaaException {
        jobIds.each { wait(it as String, timeout) }
    }

    @Override
    JobInfo wait(String jobId, long timeout) throws DrmaaException {
        if (!jobs.containsKey(jobId)) throw new InvalidJobException("Unknown job id '$jobId'")

        parentJobIds += jobId

        new JobInfoImpl(jobId: jobId)
    }

    private Set<String> _getParentJobIds(String childJobId) {
        jobs[childJobId].parentIds
    }

    @Override
    void addToParentJobIds(String childJobId, String parentJobId) {
        _getParentJobIds(childJobId).add(parentJobId)
    }

    void setJobPreScript(String jobId, String scriptPath, String args, Set<Integer> preSkipCodes = []) {
        Job job = jobs[jobId]
        if (job == null) throw new IllegalWorkflowOperation("Invalid job id $jobId")
        if (job.preScript) throw new IllegalStateException("Job PRE Script already set!")
        job.preScript = scriptPath
        job.preScriptArgs = args
        job.preSkipCodes = preSkipCodes
    }

    void setJobPostScript(String jobId, String scriptPath, String args) {
        Job job = jobs[jobId]
        if (job == null) throw new IllegalWorkflowOperation("Invalid job id $jobId")
        if (job.postScript) throw new IllegalStateException("Job POST Script already set!")
        job.postScript = scriptPath
        job.postScriptArgs = args
    }

    @Override
    int getJobProgramStatus(String jobId) throws DrmaaException {
        throw new IllegalWorkflowOperation()
    }

    @Override
    String getContact() {
       _contact
    }

    @Override
    Version getVersion() {
        VERSION
    }

    @Override
    String getDrmSystem() {
        "Gondor"
    }

    @Override
    String getDrmaaImplementation() {
        "Gondor DRMAA 1.0 + Workflow"
    }

    @Override
    void addWarning(String message) {
        warnings << message
    }

    def writeDAGFile(dag_file)
    {
        dag_file.withPrintWriter { PrintWriter printer ->
            Map<Set<String>, Set<String>> dependencies = [:].withDefault { [] as Set<String> }

            printer.println """### BEGIN Condor DAGman DAG File ###
# Generated by $drmSystem version $version using $drmaaImplementation.
#
"""
            jobs.each { String jobId, Job job ->
                job.printToDAG(printer)

                if (job.parentIds) {
                    if (!dependencies.containsKey(job.parentIds)) {
                        Set<String> parents = new HashSet<String>(job.parentIds)
                        dependencies[parents] = new HashSet<String>([job.id])
                    } else {
                        dependencies[job.parentIds].add(job.id)
                    }
                }

                printer.println()
            }

            dependencies.each { Set<String> parents, Set<String> children ->
                // N.B. If breaking long lines DAGman files can use "\" but be sure to have a space before it.
                // https://lists.cs.wisc.edu/archive/htcondor-users/2014-February/msg00010.shtml

                printer.println "PARENT ${parents.sort().join(' ')} CHILD ${children.sort().join(' ')}"
            }

            warnings.each { printer.println "\n# $it" }

            printer.println()

            printer.println """#
### END Condor DAGman DAG File ###"""
        }

        println "Generated ${jobs.size()} jobs for Condor DAG ${dag_file}"
        if (warnings) println "WARNING: ${warnings.size()} warnings generated! See DAG file for details."
    }

}
