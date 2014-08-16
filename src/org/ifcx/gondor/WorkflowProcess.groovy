package org.ifcx.gondor

import groovy.transform.InheritConstructors

@InheritConstructors
class WorkflowProcess extends Process
{
//    public WorkflowProcess(WorkflowScript workflow, WorkflowCommand command, Map<String, Object> params) {
//        super(workflow, command, params)
//    }

    /**
     * A workflow process runs the dag-generating command as usual, but there is an additional job for
     * the subdag which is necessarily a child of the dag-generation JOB.  We then use the subdag's
     * job id as that of the workflow so that our children (if any) will wait for the subdag to complete.
     *
     * Note that WorkflowScript.runJobForProcess is going to use the SUBDAG job id for all outputs of
     * this command, including the dag file itself.  This is alright as long as no one tries to analyze
     * the SUBDAG "job" like other jobs wrt to its input files.  Not really to weird since since
     * those two kinds of jobs are pretty different (no pre-/post- scripts etc).
     *
     * @return
     */
    String runJob() {
        String dagFileJobId = super.runJob()
        File dagFile = params.workflowDAGFile
        String subDAGJobId = getWorkflow().runWorkflow(dagFile)
        getWorkflow().addToParentJobIds(subDAGJobId, dagFileJobId)
        subDAGJobId
    }
}
