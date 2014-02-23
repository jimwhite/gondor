package org.ifcx.gondor

import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import org.ggf.drmaa.DrmaaException
import org.ggf.drmaa.JobInfo

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 2/22/14
 * Time: 9:36 PM
 */
@Immutable
@EqualsAndHashCode
class JobInfoImpl implements JobInfo {
    final String jobId
    final Map resourceUsage
    final boolean hasExited
    final int exitStatus
    final boolean hasSignaled
    final String terminatingSignal
    final boolean hasCoreDump
    final boolean wasAborted

//    @Override
//    String getJobId() { this.jobId }
//
//    @Override
//    Map getResourceUsage() { this.resourceUsage }

    @Override
    boolean hasExited() { this.hasExited }

//    @Override
//    int getExitStatus() { this.exitStatus }

    @Override
    boolean hasSignaled() { this.hasSignaled }

//    @Override
//    String getTerminatingSignal() { this.terminatingSignal }

    @Override
    boolean hasCoreDump() { this.hasCoreDump }

    @Override
    boolean wasAborted() { this.wasAborted }
}
