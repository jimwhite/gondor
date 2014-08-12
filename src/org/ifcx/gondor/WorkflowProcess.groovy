package org.ifcx.gondor

class WorkflowProcess extends Process
{
    public WorkflowProcess(WorkflowScript workflow, WorkflowCommand command, Map<String, Object> params) {
        super(workflow, command, params)
    }

    String runJob() {
        String dagFileJobId = super.runJob()
        File dagFile = getOutput()
        String subDAGJobId = getWorkflow().runWorkflow(dagFile)
        getWorkflow().addToParentJobIds(subDAGJobId, dagFileJobId)
        subDAGJobId
    }
}
