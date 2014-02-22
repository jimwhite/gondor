
@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript thisScript

def jt = thisScript.workflow.createJobTemplate()

jt.remoteCommand = "/bin/ls"

[1, 2, 8, 9, 10, 11, 19, 20, 21, 98, 99, 100, 101, 102, 998, 999, 1000, 1001].each {
    def jid = thisScript.workflow.runBulkJobs(jt, 1, it, 3)

    println "1 $it ${jid.size()} $jid"

}
