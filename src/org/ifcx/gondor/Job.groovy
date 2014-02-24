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
}
