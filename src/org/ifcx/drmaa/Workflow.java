package org.ifcx.drmaa;

import org.ggf.drmaa.Session;

public interface Workflow extends Session {
    String getWorkflowName();

    void init(String contact, String workflowName);
}
