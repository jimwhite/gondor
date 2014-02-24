
@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript workflowScript

void init(String contact) {
    setWorkflowName(getWorkflowName() + 'Workflow')
    super.init("jim")
}

workflowScript.with {
    println workflowName

    def jt = createJobTemplate()

    jt.remoteCommand = "/bin/ls"
    jt.outputPath = "good_out.txt"
    runJob(jt)

    jt.remoteCommand = "ls_here"
    jt.outputPath = "good_here.txt"
    runJob(jt)

    jt.remoteCommand = "../scripts/ls_here"
    jt.outputPath = "good_up_then_here.txt"
    runJob(jt)
}
