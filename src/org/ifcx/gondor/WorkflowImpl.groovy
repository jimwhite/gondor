package org.ifcx.gondor

import org.ggf.drmaa.DrmaaException
import org.ggf.drmaa.InternalException
import org.ggf.drmaa.InvalidJobException
import org.ggf.drmaa.InvalidJobTemplateException
import org.ggf.drmaa.JobInfo
import org.ggf.drmaa.JobTemplate
import org.ggf.drmaa.Version

import org.ifcx.drmaa.Workflow

public class WorkflowImpl implements Workflow {
    private static final Version VERSION = new Version(0, 1)

    String contact = ""
    String workflowName = "gondor_default_workflow"

    File jobTemplatesDir
    Map<JobTemplate, JobTemplate> jobTemplateMap = [:]
    Map<JobTemplate, File> jobTemplateFiles = [:]

    File logFile

    int job_number = 0
    int job_template_number = 0

    Map<String, Job> jobs = [:]
    Set<String> parentJobIds = []

    @Override
    void init(String contact) throws DrmaaException {
        this.contact = contact

        jobTemplatesDir = new File(workflowName + ".jobs")

        if (!jobTemplatesDir.exists() && !jobTemplatesDir.mkdirs()) {
            throw new InternalException("Can't create directory $jobTemplatesDir for job templates.")
        }

        if (!jobTemplatesDir.isDirectory()) {
            throw new InternalException("The file $jobTemplatesDir exists where we want to put the job templates dir.")
        }

        logFile = new File(workflowName + ".log")
    }


    @Override
    String getWorkflowName() {
        this.workflowName
    }

    @Override
    void init(String contact, String workflowName) {
        this.workflowName = workflowName
        init(contact)
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
        String.format("${jobTemplateName}_%04d", ++job_template_number)
    }

    File getJobTemplateFile(JobTemplate jt0) {
        if (jobTemplateMap.containsKey(jt0)) {
            return jobTemplateFiles[jobTemplateMap[jt0]]
        }

        if (jobTemplateMap.values().find { it.jobName.equalsIgnoreCase(jt0.jobName) }) {
            throw new InvalidJobTemplateException("Job name ${jt0.jobName} used in more than one job template but they are not equivalent.")
        }

        jt0 = (JobTemplateImpl) jt0.clone()
        JobTemplate jt1 = (JobTemplateImpl) jt0.clone()

        if (!jt1.jobName) {
            jt1.setGeneratedJobName(nextJobTemplateName(jt1.remoteCommand.replaceAll(/[^A-Za-z_]/, '_')))
        }

        File jobTemplateFile = new File(jobTemplatesDir, jt1.jobName + ".job")

        writeJobTemplateFile(jt1, jobTemplateFile)

        jobTemplateMap[jt0] = jt1
        jobTemplateFiles[jt1] = jobTemplateFile
    }

    @Override
    String runJob(JobTemplate jt) throws DrmaaException {
        File jobTemplateFile = getJobTemplateFile(jt)
        jt = jobTemplateMap[jt]
        def jobId = nextJobId(jt.jobName)
        jobs[jobId] = new Job(jobId: jobId, jobTemplate: jt)
        jobId
    }

    @Override
    List runBulkJobs(JobTemplate jt, int start, int end, int incr) throws DrmaaException {
        File jobTemplateFile = getJobTemplateFile(jt)
        jt = jobTemplateMap[jt]
        String jobName = jt.jobName
        if (incr > end - start) incr = Math.max(end - start, 1)
        def jobIds = (start..end).step(incr).collect {
            String jobId = nextJobId(jobName)
            jobs[jobId] = new Job(jobId: jobId, jobTemplate: jt)
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

    @Override
    int getJobProgramStatus(String jobId) throws DrmaaException {
        throw new IllegalWorkflowOperation()
    }

    @Override
    String getContact() {
       contact
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
        jobTemplateFile.withPrintWriter { writer ->
            writer.println """### BEGIN Condor Job Template File ###
# Generated by $drmSystem version $drmaaImplementation on ${new Date()}
#
Log=${logFile}
Universe=vanilla
Executable=${jt.remoteCommand}
"""

            // Should jobs be submitted into a holding pattern
            // (don't immediately start running them)?
            if (jt.getJobSubmissionState() == JobTemplate.HOLD_STATE) {
                writer.println "Hold=true"
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
                writer.println "Arguments=\"${args.join(' ')}\""
            }

            // If the working directory has been set, configure it.
            if (jt.getWorkingDirectory() != null) {
                writer.println "InitialDir=" + jt.getWorkingDirectory()
            }

            // Handle any native specifications that have been set
            if (jt.getNativeSpecification() != null) {
                writer.println(jt.getNativeSpecification())
            }

            // Handle the job category.
            //TODO: Could use priority or rank for this.
            if (jt.getJobCategory() != null) {
                writer.println("# Category=" + jt.getJobCategory())
            }

            // Send email notifications?
            if (jt.getBlockEmail()) {
                writer.println("Notification=Never");
            }

            // If the caller has specified a start time, then we add special
            // Condor settings into the submit file. Otherwise, don't do anything
            // special...
            if (jt.getStartTime() != null) {
                long time = (jt.getStartTime().getTimeInMillis() + 500) / 1000;
                writer.println("PeriodicRelease=(CurrentTime > " + time + ")");

                // TODO: Is this correct?  If we submit with a hold will release happen?
                if (jt.getJobSubmissionState() != JobTemplate.HOLD_STATE) {
                    writer.println "Hold=true"
                }
            }

            // Handle the naming of the job.
            if (jt.jobName) {
                // TODO: The C implementation has a "+" character in front of the
                // directive. We add it here as well. Find out why (or if) this is necessary.
                writer.println("+JobName=" + jt.jobName);
            }

            // Handle the job input path. Care must be taken to replace DRMAA tokens
            // with tokens that Condor understands.
            if (jt.getInputPath() != null) {
                String input = drmaaProcessPath(jt.inputPath)
                writer.println("Input=" + input)

                // Check whether to transfer the input files
                if (jt.transferFiles?.inputStream) {
                    writer.println("transfer_input_files=i");
                }
            }

            // Handle the job output path. Care must be taken to replace DRMAA tokens
            // with tokens that Condor understands.
            if (jt.outputPath) {
                String output = drmaaProcessPath(jt.outputPath)
                writer.println("Output=" + output);

                // Check if we need to join input and output files
                if (jt.joinFiles) {
                    writer.println("# Joining Input and Output");
                    writer.println("Error=" + output);
                }
            }

            // Handle the error path if specified. Do token replacement if necessary.
            if (jt.errorPath && ! jt.joinFiles) {
                String errorPath = drmaaProcessPath(jt.errorPath)
                writer.println("Error=" + errorPath)
            }

            if (jt.transferFiles?.outputStream) {
                writer.println("should_transfer_files=IF_NEEDED");
                writer.println("when_to_transfer_output=ON_EXIT");
            }

            // Handle the case of the user/caller setting the environment for the job.
            if (jt.jobEnvironment) {
                // This is the Condor directive for setting the job environment.
                def envArgsValue = jt.jobEnvironment.collect { String k, String v -> k + '=' + v.replaceAll('"', '""') }
                writer.println "Environment = \"${envArgsValue.join(' ')}\""
            }

            // It appears that Condor can only handle 1 email address for notifications
            // while the DRMAA returns a set of them. If we have emails specified, then
            // just use the first one...
            if (jt.email) {
                writer.println("Notify_user=" + jt.email.join(";"))
            }

            // Every Condor submit file needs a Queue directive to make the job go.
            // Array jobs will have a Queue count greater than 1.
            writer.println "Queue"
            writer.println "### End of Condor job template file ###"
        }
    }

    private String drmaaProcessPath(String path) {
        path = path.replace(JobTemplate.PARAMETRIC_INDEX, '$(_GONDOR_JOBID)')
        path = path.replace(JobTemplate.HOME_DIRECTORY, '$ENV(HOME)')
        if (path.startsWith(":")) {
            path = path.substring(1);
        }
        path
    }

    def generate_dag(dag_file)
    {
        def warnings = 0

        dag_file.withPrintWriter { printer ->
            def dependencies = []
            jobs.each { Job job ->
                // println job
                printer.println "JOB ${job.jobId} ${job.jobTemplate.jobName}"

                // Variables that begin with a dash have a default value of themselves.
                job.vars.grep { it.startsWith('-') }.each { if (!job.args.containsKey(it)) job.args[it] = it }

                if (!job.args.keySet().containsAll(job.vars)) {
                    printer.println "### WARNING: Missing arguments: ${job.vars - job.args.keySet()}"
                    ++warnings
                }

                if (!job.vars.containsAll(job.args.keySet())) {
                    printer.println "### WARNING: Extra arguments: ${job.args.keySet() - job.vars}"
                    ++warnings
                }

                printer.println "VARS ${job.id} " + ((job.vars.collect { var ->
                    if (job.invars.contains(var)) {
                        def input_files = job.args[var]
                        if (input_files instanceof File) input_files = [input_files]

                        input_files.each { File input_file ->
                            def parentJob = output_files[input_file]

                            if (!parentJob) {
                                if (!input_file.exists()) {
                                    printer.println ("### WARNING: Didn't find input file for generating parent dependency for '$var'!")
                                    printer.println ("### Assuming file exists: ${input_file.canonicalPath}")
                                    ++warnings
                                }
                            } else if (!parentJob.data) {
                                job.parents.add(parentJob)
                            }
                        }
                    }

                    '_' + ((var == 'infile') ? 'MyJobInput' : var) + '=\"' + argument_to_string(job.args[var]) + '\"'
                }) + ['_MyJobOutput=\"'+job.outfile.canonicalPath+'\"', '_MyJobError=\"'+job.errfile.canonicalPath+'\"']).join(' ')

                if (job.parents) dependencies.add("PARENT ${job.parents.id.unique().join(' ')} CHILD ${job.id}".toString())

                printer.println()
            }

            dependencies.each { printer.println it }
        }

        println "Generated ${all_jobs.size()} jobs for Condor DAG ${dag_file}"
        if (warnings) println "WARNING: ${warnings} warnings generated! See DAG file for details."
    }
}
