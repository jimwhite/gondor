package org.ifcx.gondor

/**
 * Created by jim on 5/16/14.
 */
class SubDAG extends Job {
    @Override
    void printToDAG(PrintWriter printer) {
        printer.println '# ' + comment
        printer.println "SUBDAG EXTERNAL ${id} ${templateFile.path}" + (workingDir ? ' DIR ' + workingDir.path : '')
    }
}
