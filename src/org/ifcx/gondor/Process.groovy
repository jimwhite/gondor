package org.ifcx.gondor

import org.ggf.drmaa.JobTemplate

class Process
{
    String jobId

    JobTemplate jobTemplate

    Command command

    Map<String, Object> params

    List<File> infiles = []
    List<File> outfiles = []
}
