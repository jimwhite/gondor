package org.ifcx.gondor

import org.ggf.drmaa.DrmaaException
import org.ggf.drmaa.JobInfo
import org.ggf.drmaa.JobTemplate
import org.ggf.drmaa.Version
import org.ifcx.drmaa.Workflow

public class WorkflowImpl implements Workflow {
    private static final Version VERSION = new Version(0, 1)

    String contact = ""

    Map<JobTemplate, JobTemplate> jobTemplateMap = [:].withDefault { it.clone() }

    @Override
    void init(String contact) throws DrmaaException {
        this.contact = contact
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

    }

    @Override
    String runJob(JobTemplate jt) throws DrmaaException {
        String jobName = jt.jobName
        JobTemplate jobTemplate = jobTemplateMap[jt] = jobTemplateMap[jt]
        jobName
    }

    @Override
    List runBulkJobs(JobTemplate jt, int start, int end, int incr) throws DrmaaException {
        String jobName = jt.jobName
        JobTemplate jobTemplate = jobTemplateMap[jt] = jobTemplateMap[jt]
        if (incr > end - start) incr = Math.max(end - start, 1)
        def jobIds = (start..end).step(incr).collect { jobName + String.format("%0${Math.round(Math.log10(end)) + 1}d", it) }
        assert jobIds.size() == Math.floor(((end - start) / incr) + 1)
        jobIds
    }

    @Override
    void control(String jobId, int action) throws DrmaaException {
       
    }

    @Override
    void synchronize(List jobIds, long timeout, boolean dispose) throws DrmaaException {
       
    }

    @Override
    JobInfo wait(String jobId, long timeout) throws DrmaaException {
        return null 
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
}
