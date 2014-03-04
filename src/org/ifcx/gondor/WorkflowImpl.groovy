package org.ifcx.gondor

import org.ggf.drmaa.AlreadyActiveSessionException
import org.ggf.drmaa.DrmaaException
import org.ggf.drmaa.InternalException
import org.ggf.drmaa.InvalidJobException
import org.ggf.drmaa.JobInfo
import org.ggf.drmaa.JobTemplate
import org.ggf.drmaa.Version

import org.ifcx.drmaa.Workflow

public class WorkflowImpl implements Workflow {
    private static final Version VERSION = new Version(0, 1)

    private boolean hasInitialized = false;

    private String _contact = ""
    private String _workflowName = "gondor_default_workflow"
    private String _temporaryFilesPath

    private File temporaryFilesDir

    private File workingDir = new File('.')
    private File logFile

    private int job_number = 0
    private int job_template_number = 0

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

        logFile = new File(temporaryFilesDir, workflowName + ".log")

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

    String nextJobId(String jobName) {
        jobName + String.format("_%04d", ++job_number)
    }

    String nextJobTemplateName(String jobTemplateName) {
        String.format("${jobTemplateName}_%03d", ++job_template_number)
    }

    def getJobTemplateFile(JobTemplate jt0) {
//        if (jobTemplateMap.containsKey(jt0)) {
//            return jobTemplateFiles[jobTemplateMap[jt0]]
//        }
//
//        if (jobTemplateMap.values().find { it.jobName.equalsIgnoreCase(jt0.jobName) }) {
//            throw new InvalidJobTemplateException("Job name ${jt0.jobName} used in more than one job template but they are not equivalent.")
//        }
//
//        jt0 = (JobTemplateImpl) jt0.clone()
//        JobTemplate jt1 = (JobTemplateImpl) jt0.clone()

        def jt1 = jt0

        def jobTemplateName = nextJobTemplateName(jt1.jobName ?: defaultJobTemplateName(jt1))

        File jobTemplateFile = new File(temporaryFilesDir, jobTemplateName + ".job")

        writeJobTemplateFile(jt1, jobTemplateFile)

//        jobTemplateMap[jt0] = jt1
//        jobTemplateFiles[jt1] = jobTemplateFile

        def jobComment = "${jt1.workingDirectory ? 'cd ' + replacePathPlaceholders(jt1.workingDirectory) + ' ' : ''}" +
                "${jt1.remoteCommand} ${jt1.args.join(' ')}" +
                "${jt1.inputPath ? ' <' + replacePathPlaceholders(jt1.inputPath) : ''}" +
                "${jt1.outputPath ? ' >' + replacePathPlaceholders(jt1.outputPath) : ''}" +
                "${jt1.errorPath ? ' 2>' + replacePathPlaceholders(jt1.errorPath) : ''}"

        [jobTemplateName, jobComment, jobTemplateFile]
    }

    private String defaultJobTemplateName(JobTemplateImpl jt1) {
        def name = jt1.remoteCommand.replaceAll(/[^A-Za-z_]/, '_')
        if (!name.startsWith('_')) name = '_' + name
        name
    }

    @Override
    String runJob(JobTemplate jt) throws DrmaaException {
        def (jobTemplateName, jobComment, jobTemplateFile) = getJobTemplateFile(jt)
        def jobId = nextJobId(jobTemplateName)
        def job = new Job(id: jobId, comment: jobComment, /*procId: -1,*/ templateFile: jobTemplateFile)
        job.parentIds.addAll(parentJobIds)
        jobs[jobId] = job
        jobId
    }

    @Override
    List runBulkJobs(JobTemplate jt, int start, int end, int incr) throws DrmaaException {
        def (jobTemplateName, jobComment, jobTemplateFile) = getJobTemplateFile(jt)
        if (incr > end - start) incr = Math.max(end - start, 1)
        def jobIds = (start..end).step(incr).collect { procId ->
            def jobId = nextJobId(jobTemplateName) + String.sprintf('_%03d', procId)
            def job = new Job(id: jobId, comment: jobComment, procId: procId, templateFile: jobTemplateFile)
            job.parentIds.addAll(parentJobIds)
            jobs[jobId] = job
            jobId
        }
        assert jobIds.size() == Math.floor(((end - start) / incr) + 1)
        jobIds
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

    /**
     * Create a Condor submit file for the job template.
     *
     * @param jt a {@link JobTemplate}
     * @param number the number of times to execute the job
     * @see "condor_submit man page"
     * @return a {@link File} for the submit file created
     * @throws Exception
     */
    void writeJobTemplateFile(JobTemplate jt, File jobTemplateFile) throws Exception {
        jobTemplateFile.withPrintWriter { printer ->
            printer.println """### BEGIN Condor Job Template File ###
# Generated by $drmSystem version $version using $drmaaImplementation on ${new Date()}
#
Universe=vanilla
Executable=${jt.remoteCommand}
Log=${logFile}
"""
            // Handle the case of the user/caller setting the environment for the job.
            if (jt.jobEnvironment) {
                // This is the Condor directive for setting the job environment.
                def envArgsValue = jt.jobEnvironment.collect { String k, String v -> k + '=' + v.replaceAll('"', '""') }
                printer.println "Environment = \"${envArgsValue.join(' ')}\""
            }

            // Here we handle the job arguments, if any have been supplied.
            // We try to adhere to the "new" way of specifying the arguments
            // as explained in the 'condor_submit' man page.
            if (jt.args) {
                def args = jt.args.collect { String arg ->
                    if (arg.contains("\"")) {
                        arg = arg.replace("\"", "\"\"");
                    }
                    // Replace ticks with double ticks
                    if (arg.contains("\'")) {
                        arg = arg.replace("\'", "\'\'");
                    }
                    if (arg.contains(" ")) {
                        arg = "'" + arg + "'"
                    }
                    arg
                }
                printer.println "Arguments=\"${args.join(' ')}\""
            }

            // If the working directory has been set, configure it.
            if (jt.workingDirectory != null) {
                printer.println "InitialDir=" + replacePathPlaceholders(jt.workingDirectory)
            }

            // Handle any native specifications that have been set
            if (jt.getNativeSpecification() != null) {
                printer.println(jt.getNativeSpecification())
            }

            // Handle the job category.
            //TODO: Could use priority or rank for this.
            if (jt.getJobCategory() != null) {
                printer.println("# Category=" + jt.getJobCategory())
            }

            // If the caller has specified a start time, then we add special
            // Condor settings into the submit file. Otherwise, don't do anything
            // special...
            if (jt.getStartTime() != null) {
                long time = (jt.getStartTime().getTimeInMillis() + 500) / 1000;
                printer.println("PeriodicRelease=(CurrentTime > " + time + ")");

//                // TODO: Is this correct?  If we submit with a hold will release happen?
//                if (jt.getJobSubmissionState() != JobTemplate.HOLD_STATE) {
//                    writer.println "Hold=true"
//                }
            }

            // Handle the naming of the job.
            if (jt.jobName) {
                // TODO: The C implementation has a "+" character in front of the
                // directive. We add it here as well. Find out why (or if) this is necessary.
                printer.println("+JobName=" + jt.jobName);
            }

            // Handle the job input path. Care must be taken to replace DRMAA tokens
            // with tokens that Condor understands.
            if (jt.getInputPath() != null) {
                String input = replacePathPlaceholders(jt.inputPath)
                printer.println("Input=" + input)

                // Check whether to transfer the input files
                if (jt.transferFiles?.inputStream) {
                    printer.println("transfer_input_files=" + input);
                }
            }

            // Handle the job output path. Care must be taken to replace DRMAA tokens
            // with tokens that Condor understands.
            if (jt.outputPath) {
                String output = replacePathPlaceholders(jt.outputPath)
                printer.println("Output=" + output);

                // Check if we need to join input and output files
                if (jt.joinFiles) {
                    printer.println("# Joining Input and Output");
                    printer.println("Error=" + output);
                }
            }

            // Handle the error path if specified. Do token replacement if necessary.
            if (! jt.joinFiles && jt.errorPath) {
                String errorPath = replacePathPlaceholders(jt.errorPath)
                printer.println("Error=" + errorPath)
            }

            if (jt.transferFiles?.outputStream) {
                printer.println("should_transfer_files=IF_NEEDED");
                printer.println("when_to_transfer_output=ON_EXIT");
            }

            // Send email notifications?
            if (jt.getBlockEmail()) {
                printer.println("Notification=Never");
            }

            // Documentation is a bit thin, but it seems Condor will accept multiple
            // email addresses separated by a comma.
            Set<String> emails = []
            if (contact) emails.add(contact)
            if (jt.email) emails.addAll(jt.email)
            if (emails) {
                printer.println("Notify_user=" + emails.join(","))
            }

            // Should jobs be submitted into a holding pattern
            // (don't immediately start running them)?
            if (jt.getJobSubmissionState() == JobTemplate.HOLD_STATE) {
                printer.println "Hold=true"
            }

            // Every Condor submit file needs a Queue directive to make the job go.
            // Array jobs will have a Queue count greater than 1.
            printer.println "Queue"
            printer.println "#"
            printer.println "### END Condor Job Template File ###"
        }
    }

    private String replacePathPlaceholders(String path) {
        path = path.replace(JobTemplate.PARAMETRIC_INDEX, '$(_GONDOR_PROCID)')
        path = path.replace(JobTemplate.HOME_DIRECTORY, '$ENV(HOME)')
        path = path.replace(JobTemplate.WORKING_DIRECTORY, workingDir.path)
        if (path.startsWith(":")) {
            path = path.substring(1);
        }
        path
    }

    @Override
    void addWarning(String message) {
        warnings << message
    }

    def writeDAGFile(dag_file)
    {
        dag_file.withPrintWriter { printer ->
            Map<Set<String>, Set<String>> dependencies = [:].withDefault { [] as Set<String> }

            printer.println """### BEGIN Condor DAGman DAG File ###
# Generated by $drmSystem version $version using $drmaaImplementation on ${new Date()}
#
"""
            jobs.each { String jobId, Job job ->
                printer.println '# ' + job.comment
                printer.println "JOB ${job.id} ${job.templateFile.path}" +
                        "${job.workingDir ? ' DIR ' + job.workingDir.path : ''}"

                def vars = [:]

                if (job.procId != null) { vars._GONDOR_PROCID = job.procId }

                if (vars) {
                    printer.println "VARS ${job.id} ${vars.collect { k, v -> "$k=\"$v\""}.join(' ')}"
                }

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
