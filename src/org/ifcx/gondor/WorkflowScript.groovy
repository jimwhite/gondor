package org.ifcx.gondor

import org.ifcx.drmaa.Workflow
import org.ifcx.drmaa.WorkflowFactory

public abstract class WorkflowScript extends GondorScript {
    Workflow workflow

    abstract Object buildWorkflow();

    Object run() {
        workflow = WorkflowFactory.getFactory().getWorkflow()

        workflow.setWorkflowName(this.getClass().name)

        workflow.setTemporaryFilesPath(workflow.workflowName + '.jobs')

        workflow.init("")

        buildWorkflow()

        workflow.createDAGFile(new File(getWorkflow().workflowName + '.dag'))
    }

}
