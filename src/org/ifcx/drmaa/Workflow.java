package org.ifcx.drmaa;

import org.ggf.drmaa.Session;

import java.io.File;
import java.util.Set;

public interface Workflow extends Session {
    String getWorkflowName();
    void setWorkflowName(String name);

    String getTemporaryFilesPath();
    void setTemporaryFilesPath(String path);

//    Set<String> getParentJobIds(String childJobId);
    void addToParentJobIds(String childJobId, String parentJobId);

    void createDAGFile(File dagFile);

    void addWarning(String message);
}
