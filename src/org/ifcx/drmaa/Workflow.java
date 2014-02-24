package org.ifcx.drmaa;

import org.ggf.drmaa.Session;

import java.io.File;

public interface Workflow extends Session {
    void setWorkflowName(String name);
    String getWorkflowName();

    void createDAGFile(File dagFile);
}
