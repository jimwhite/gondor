package org.ifcx.drmaa;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.Session;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface Workflow extends Session {
    String getWorkflowName();
    void setWorkflowName(String name);

    String getTemporaryFilesPath();
    void setTemporaryFilesPath(String path);

    String getLogFilePath();
    void setLogFilePath(String path);

    //    Set<String> getParentJobIds(String childJobId);
    void addToParentJobIds(String childJobId, String parentJobId);

    void setJobPreScript(String jobId, String scriptPath, String args, Set<Integer> preSkipCodes);

    void setJobPostScript(String jobId, String scriptPath, String args);

    String runWorkflow(File dagFile) throws DrmaaException;

    void createDAGFile(File dagFile);

    void addWarning(String message);
}
