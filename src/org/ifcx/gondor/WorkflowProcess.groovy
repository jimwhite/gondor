package org.ifcx.gondor

import groovy.transform.InheritConstructors

@InheritConstructors
class WorkflowProcess extends Process
{
    public WorkflowProcess(WorkflowScript workflow, WorkflowCommand command, Map<String, Object> params) {
        super(workflow, command, params)
        System.out.println params
    }

}
