package org.ifcx.drmaa;

import org.ggf.drmaa.Session;

import java.io.File;

public interface Workflow extends Session {
    String getWorkflowName();
    void setWorkflowName(String name);

    String getTemporaryFilesPath();
    void setTemporaryFilesPath(String path);

    void createDAGFile(File dagFile);
}
