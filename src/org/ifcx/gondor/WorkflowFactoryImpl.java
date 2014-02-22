package org.ifcx.gondor;

import org.ifcx.drmaa.Workflow;
import org.ifcx.drmaa.WorkflowFactory;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 2/21/14
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class WorkflowFactoryImpl extends WorkflowFactory {
    @Override
    public Workflow getWorkflow() {
        return new WorkflowImpl();
    }
}
