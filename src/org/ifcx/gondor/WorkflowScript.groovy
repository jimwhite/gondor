package org.ifcx.gondor

import org.ifcx.drmaa.Workflow
import org.ifcx.drmaa.WorkflowFactory

public abstract class WorkflowScript extends GondorScript {
    Workflow workflow

    abstract Object buildWorkflow();

    Object run() {
        workflow = WorkflowFactory.getFactory().getWorkflow()

        workflow.init("", this.getClass().name)

        buildWorkflow()
    }

}
