package org.ifcx.gondor

import groovy.text.markup.MarkupTemplateEngine
import groovy.xml.StreamingMarkupBuilder
import org.ggf.drmaa.JobTemplate

class Process
{
    final WorkflowScript workflow
    Command command
    final Map<String, Object> params

    File jobDirectory

    List<File> infiles = []
    List<File> outfiles = []

    Map<String, Object> attributes = [:]

    Set<String> psuedo_io = []

    public final static String INPUT = ":input";
    public final static String OUTPUT = ":output";
    public final static String ERROR = ":error";

    public Process(WorkflowScript workflow, Command command, Map<String, Object> params) {
        this.workflow = workflow
        this.command = command
        this.params = command.getArgumentDefaultValues().collectEntries {
            k, v -> [k, params.containsKey(k) ? params[k] : v]
        }

        Set<String> undefinedParams = params.keySet() - this.params.keySet()
        if (undefinedParams) {
            throw new IllegalArgumentException("Command ${command.getCommandPath()} called with undefined parameters: $undefinedParams")
        }

        initializeAttributes()
    }

    void initializeAttributes() {
        [INPUT, OUTPUT, ERROR].each {
            if (params.containsKey(it)) {
                def value = command.getArgumentDefaultValues()[it]
                if (value != null) {
                    attributes[it] = value
                    setPsuedoStdioFile(it)
                }
            }
        }
    }

    public boolean isStdioFileUsed(String name) { attributes.containsKey(name) }
    public boolean isPsuedoStdioFile(String name) { psuedo_io.contains(name) }
    public void setPsuedoStdioFile(String name) { psuedo_io.add(name) }

    @Override
    public Object getProperty(String property) {
//        command.argumentDefaultValues.contains(property) ? params[property] : super.getProperty(property)
//        params.containsKey(property) ? params[property] : super.getProperty(property)
        try {
            // super.getProperty(property)
            getMetaClass().getProperty(this, property)
        } catch (MissingPropertyException e) {
            if (!params.containsKey(property)) throw e
            params[property]
        }
    }

    JobTemplate setUpJob(JobTemplate jt) {
        Closure<JobTemplate> jobTemplateCustomizer = command.getJobTemplateCustomizer()
        if (jobTemplateCustomizer != null) { jobTemplateCustomizer(jt) }

        jt.remoteCommand = this.command.getCommandPath()

        List<String> jobArgs = []
        command.getArgs().each { Closure ac ->
            ac(jobArgs, workflow, this, params)
        }
        jt.args = jobArgs

        // Don't redirect these for files that the executable will create itself.
        if (isStdioFileUsed(Process.INPUT) && !isPsuedoStdioFile(Process.INPUT)) jt.setInputPath(input.path)
        if (isStdioFileUsed(Process.OUTPUT) && !isPsuedoStdioFile(Process.OUTPUT)) jt.setOutputPath(output.path)
        if (isStdioFileUsed(Process.ERROR) && !isPsuedoStdioFile(Process.ERROR)) jt.setErrorPath(error.path)

        jt
    }

    File createPreScript(String jobId) {
        File scriptFile = newJobFile("prescript.sh")
        File metadataFile = newJobFile("index.html")

        def html = new StreamingMarkupBuilder().bind {
            html {
                head { title("Job " + jobId) }
                body {
                    h1 "Job $jobId"
                    h2 { mkp.yieldUnescaped 'JOB ${CONDOR_JOB}' }
                    h2 "Workflow ${workflow.workflowName}"
                    h3 command.getCommandPath()
                    if (params) {
                        table(border:1) {
                            caption "params"
                            params.each { k, v -> tr { td(k as String) ; td(v as String) } }
                        }
                        br()
                    }
                    if (attributes) {
                        table(border:1) {
                            caption "attrs"
                            attributes.each { k, v -> tr { td(k as String) ; td(v as String) } }
                        }
                        br()
                    }
                    if (infiles) {
                        table(border:1) {
                            caption "input files"
                            infiles.eachWithIndex { f, x ->
                                tr {
                                    td(f.path);
                                    def t = f.isFile() ? 'f' : f.isDirectory() ? 'd' : '?'
                                    td(t) ; td { mkp.yieldUnescaped("\${shasum_f$x}") }
                                }
                            }
                        }
                        br()
                    }
                    if (outfiles) {
                        table(border:1) {
                            caption "output files"
                            outfiles.each { f -> tr { td(f.path) } }
                        }
                        br()
                    }
                }
            }
        }.toString()

        def bash = """#!/bin/sh
# Pre-script for job $jobId in workflow ${workflow.workflowName}
#
script_type=\$1 # pre
CONDOR_JOB=\$2
${ def sb=new StringBuilder() ; infiles.eachWithIndex { f, x -> sb.append("shasum_f$x=`shasum -p '${f.path}'`\n") } ; sb }
cat > ${metadataFile.path} << EOL
$html
EOL
# End of script
"""

        scriptFile.withPrintWriter { it.print(bash.toString()) }

        // This permission probably works as owner-only, but I don't care to find out right now.
        scriptFile.setExecutable(true, false)
        scriptFile
    }

    File newJobFile(String s) {
        if (jobDirectory == null) {
            jobDirectory = workflow.newDirectory(FileType.JOB_DIR, command.getCommandPath().replaceAll(/\W/, /_/))
            if (!(jobDirectory.exists() || jobDirectory.mkdirs())) {
                throw new FailedFileSystemOperation("File.mkdirs failed in Process.newJobFile for " + jobDirectory)
            }
        }
        new File(jobDirectory, s)
    }

    Process or(Process sink) { toProcess(sink) }

    Process toProcess(Process sink) {
        sink.fromFile(output)
    }

    Process leftShift(File source) { fromFile(source) }

    Process fromFile(File source) {
        if (attributes[INPUT]) throw new IllegalStateException("stdin already set")
        attributes[INPUT] = source
        infiles << source
        this
    }

    File getInput() {
        if (attributes[INPUT] == null) {
            fromFile(newJobFile("job.in"))
        }
        (File) attributes[INPUT]
    }

    Process rightShift(File sink) { toFile(sink) }

    Process toFile(File sink) {
        if (attributes[OUTPUT] != null) throw new IllegalStateException("stdout already set")
        attributes[OUTPUT] = sink
        outfiles << sink
        this
    }

    File getOutput() {
        if (attributes[OUTPUT] == null) {
            toFile(newJobFile("job.out"))
        }
        (File) attributes[OUTPUT]
    }

    Process rightShiftUnsigned(File sink) { errorFile(sink) }

    Process errorFile(File sink) {
        if (attributes[ERROR] != null) throw new IllegalStateException("stderr already set")
        attributes[ERROR] = sink
        outfiles << sink
        this
    }

    File getError() {
        if (attributes[ERROR] == null) {
            errorFile(newJobFile("job.err"))
        }
        (File) attributes[ERROR]
    }

}
