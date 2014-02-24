import org.ggf.drmaa.JobTemplate

@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript thisScript

//thisScript.workflow.init("jimwhite@dryas")

def jt = thisScript.workflow.createJobTemplate()

jt.remoteCommand = "/bin/ls"
jt.jobName = "list"
jt.outputPath = "output_${JobTemplate.PARAMETRIC_INDEX}.txt"
[1, 2, 8, 9, 10, 11, 19, 20, 21, 98, 99, 100, 101, 102, 998, 999, 1000, 1001].each {
    def jid = thisScript.workflow.runBulkJobs(jt, 1, it, 3)

    println "1 $it ${jid.size()} $jid"
}

println "25 50 4 ${thisScript.workflow.runBulkJobs(jt, 25, 50, 4)}"
println "25 51 4 ${thisScript.workflow.runBulkJobs(jt, 25, 51, 4)}"
println "25 52 4 ${thisScript.workflow.runBulkJobs(jt, 25, 52, 4)}"
println "25 53 4 ${thisScript.workflow.runBulkJobs(jt, 25, 53, 4)}"
println "25 54 4 ${thisScript.workflow.runBulkJobs(jt, 25, 54, 4)}"
println "25 55 4 ${thisScript.workflow.runBulkJobs(jt, 25, 55, 4)}"

def jt2 = thisScript.workflow.createJobTemplate()

jt2.remoteCommand = "/bin/ls"
jt2.args = ["-la", "/"]
jt2.errorPath = "errors.txt"
jt2.outputPath = "list_home.txt"
jt2.jobName = "list_home"

assert jt != jt2

def jid2 = thisScript.workflow.runJob(jt2)

println jid2

def jt3 = thisScript.workflow.createJobTemplate()

jt3.remoteCommand = "/bin/ls"
jt3.args = ["-la", "/"]
jt3.errorPath = "errors.txt"

assert jt != jt3

println thisScript.workflow.runJob(jt3)

def jt4 = thisScript.workflow.createJobTemplate()

jt4.remoteCommand = "/bin/ls"
jt4.args = ["-la", "/"]
jt4.errorPath = "errors.txt"

assert jt3 == jt4

println thisScript.workflow.runJob(jt4)

def jt5 = thisScript.workflow.createJobTemplate()

jt5.remoteCommand = "/bin/ls"
jt5.args = ["-la", "/"]
jt5.outputPath = "list5.txt"
jt5.errorPath = "errors.txt"

assert jt3 != jt5

println thisScript.workflow.runJob(jt4)
