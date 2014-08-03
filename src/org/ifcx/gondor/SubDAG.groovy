package org.ifcx.gondor

import org.ifcx.drmaa.GondorJobTemplate

/**
 * Created by jim on 5/16/14.
 */
class SubDAG extends Job {

    Job init(File dagFile) {
        def jobId = nextJobId(defaultJobTemplateName(dagFile.path))
        String jobComment = "Workflow ${dagFile.path}"
        comment = jobComment
        /*procId: -1,*/
        templateFile = dagFile
        parentIds.addAll(workflow.parentJobIds)
        this
    }

    @Override
    void printToDAG(PrintWriter printer) {
        printer.println '# ' + comment
        printer.println "SUBDAG EXTERNAL ${id} ${templateFile.path}" + (workingDir ? ' DIR ' + workingDir.path : '')
    }
}
