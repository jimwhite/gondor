import org.ggf.drmaa.JobTemplate
import org.ifcx.drmaa.Workflow

@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript thisScript

void init(String contact) {
    super.init("jim")
}

def jt = thisScript.createJobTemplate()

jt.remoteCommand = "/bin/ls"
jt.jobName = "list"
jt.outputPath = "list_output_${JobTemplate.PARAMETRIC_INDEX}.txt"
[1, 2, 8 /*, 9, 10, 11, 19, 20, 21, 98, 99, 100, 101, 102, 998, 999, 1000, 1001*/].each {
    def jid = thisScript.runBulkJobs(jt, 1, it, 3)

    thisScript.synchronize(jid, Workflow.TIMEOUT_WAIT_FOREVER, false)

    println "1 $it ${jid.size()} $jid"
}

println "25 50 4 ${thisScript.runBulkJobs(jt, 25, 50, 4)}"

//println "25 51 4 ${thisScript.workflow.runBulkJobs(jt, 25, 51, 4)}"
println "25 52 4 ${thisScript.runBulkJobs(jt, 25, 52, 4)}"
//println "25 53 4 ${thisScript.workflow.runBulkJobs(jt, 25, 53, 4)}"
println "25 54 4 ${thisScript.runBulkJobs(jt, 25, 54, 4)}"
//println "25 55 4 ${thisScript.workflow.runBulkJobs(jt, 25, 55, 4)}"

def jt2 = thisScript.createJobTemplate()

jt2.remoteCommand = "/bin/ls"
jt2.args = ["-la", "/"]
jt2.errorPath = "errors.txt"
jt2.outputPath = "list_home.txt"
jt2.jobName = "list_home"

assert jt != jt2

def jid2 = thisScript.runJob(jt2)

println jid2

def jt3 = thisScript.createJobTemplate()

jt3.remoteCommand = "/bin/ls"
jt3.args = ["-la", "/"]
jt3.outputPath = "${JobTemplate.WORKING_DIRECTORY}/list-la.txt"
jt3.joinFiles = true

assert jt != jt3

def id3 = thisScript.runJob(jt3)
println id3

thisScript.wait(id3, Workflow.TIMEOUT_WAIT_FOREVER)

def jt4 = thisScript.createJobTemplate()

jt4.remoteCommand = "/bin/ls"
jt4.args = ["-la", "/"]
jt4.outputPath = "${JobTemplate.WORKING_DIRECTORY}/list-la.txt"
jt4.joinFiles = true

assert jt3 == jt4

println thisScript.runJob(jt4)

def jt5 = thisScript.createJobTemplate()

jt5.remoteCommand = "/bin/ls"
jt5.args = ["-zlkjd893^la", "/"]
jt5.outputPath = "list5.txt"
jt5.joinFiles = true

assert jt3 != jt5

println thisScript.runJob(jt5)
