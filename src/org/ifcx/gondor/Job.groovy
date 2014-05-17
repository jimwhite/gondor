package org.ifcx.gondor

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 2/22/14
 * Time: 9:48 PM
 * To change this template use File | Settings | File Templates.
 */

class Job {
    String id
    String comment
    File templateFile
    File workingDir
    Integer procId
    Set<String> parentIds = []

    void printToDAG(PrintWriter printer) {
        printer.println '# ' + comment
        printer.println "JOB ${id} ${templateFile.path}" + (workingDir ? ' DIR ' + workingDir.path : '')

        def vars = [:]

        if (procId != null) {
            vars._GONDOR_PROCID = procId
        }

        if (vars) {
            printer.println "VARS ${id} ${vars.collect { k, v -> "$k=\"$v\"" }.join(' ')}"
        }
    }
}
