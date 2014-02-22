package org.ifcx.gondor

import org.ifcx.drmaa.Workflow
import org.ifcx.drmaa.WorkflowFactory

public abstract class WorkflowScript extends GondorScript {
    Workflow workflow = WorkflowFactory.getFactory().getWorkflow()

}
