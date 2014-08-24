package org.ifcx.gondor

import groovy.text.markup.MarkupTemplateEngine
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
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

    File metadataFile

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
        def commandPath = command.getCommandPath()
        String commandId = command.getIdentifier()
        File scriptFile = newJobFile("prescript.sh")
        metadataFile = newJobFile("index.html")

        Writable html = new StreamingMarkupBuilder().bind {
            html {
                def name = new File(commandPath).name
                head {
                    title("Command " + name) }
                body {
                    h1 "Command " + name
                    h2 "Id " + commandId
                    h3 "Path " + commandPath
//                    h1 "Job $jobId"
//                    h2 { mkp.yieldUnescaped 'JOB ${CONDOR_JOB}' }
//                    h2 "Workflow ${workflow.workflowName}"
                    if (params) {
                        table(border:1) {
                            caption "params"
                            params.each { k, v -> tr(valign:'top') { td(k as String) ; td(v as String) } }
                        }
                        br()
                    }
                    if (attributes) {
                        table(border:1) {
                            caption "attrs"
                            attributes.each { k, v -> tr(valign:'top') { td(k as String) ; td(v as String) } }
                        }
                        br()
                    }
                    if (infiles) {
                        table(border:1) {
                            caption "input files"
                            infiles.eachWithIndex { f, x ->
                                tr(valign:'top') {
                                    td(f.path)
                                    def t = f.isFile() ? 'f' : f.isDirectory() ? 'd' : '?'
                                    td(t)
                                    td { pre { mkp.yieldUnescaped("\${hashes_i$x}") } }
                                    td { pre { mkp.yieldUnescaped("\$(htmlencode \"\${paths_i$x}\")") } }
                                }
                            }
                        }
                        br()
                    }
                    if (outfiles) {
                        table(border:1) {
                            caption "output files"
                            outfiles.each { f -> tr(valign:'top') { td(f.path) } }
                        }
                        br()
                    }
                }
            }
        }

        // An extra step to pretty print the html.
        def html_text = XmlUtil.serialize(html)

        // Order of the script arguments is determined by the DAG file generator.
        // See o.i.gondor.Job.printToDAG.  These might live closer together (this probably goes back to WorkflowScript).

        def bash = """#!/bin/sh
# Pre-script for job $jobId in workflow ${workflow.workflowName}
#
script_type=\$1 # pre or post
CONDOR_JOB=\$2

${
    def sb=new StringBuilder()
    infiles.eachWithIndex { f, x ->
        sb.append("paths_i$x=`find '${f.path}' -type f | sort`\n")
        sb.append("hashes_i$x=`git hash-object --stdin-paths <<<\"\$paths_i$x\"`\n")
    }
    sb
}
htmlencode() {
  local string="\${1}"
  local encoded1=\${string//&/&amp;} # Must do this first!
  local encoded2=\${encoded1//</&lt;}
  local encoded3=\${encoded2//>/&gt;}
  echo "\${encoded3}"
}

cat > ${metadataFile.path} << END_OF_METADATA_HTML_SCRIPT_HEREDOC_9827312838923u78673
$html_text
END_OF_METADATA_HTML_SCRIPT_HEREDOC_9827312838923u78673
# End of script
"""

//        This snippet is escaped for a gstring
//        # From http://stackoverflow.com/a/10660730
//        rawurlencode() {
//            local string="\${1}"
//            local strlen=\${#string}
//            local encoded=""
//
//            for (( pos=0 ; pos<strlen ; pos++ )); do
//            c=\${string:\$pos:1}
//            case "\$c" in
//                    [-_.~a-zA-Z0-9] ) o="\${c}" ;;
//            * )               printf -v o '%%%02x' "'\$c"
//            esac
//            encoded+="\${o}"
//            done
//            echo "\${encoded}"    # You can either set a return variable (FASTER)
//            REPLY="\${encoded}"   #+or echo the result (EASIER)... or both... :p
//        }

        scriptFile.withPrintWriter { it.print(bash.toString()) }

        // This permission probably works as owner-only, but I don't care to find out right now.
        scriptFile.setExecutable(true, false)
        scriptFile
    }

    File createPostScript(String jobId) {
        File scriptFile = newJobFile("postscript.sh")
        File resultFile = newJobFile("results.txt")

        // Order of the script arguments is determined by the DAG file generator.
        // See o.i.gondor.Job.printToDAG.  These might live closer together (this probably goes back to WorkflowScript).

        def bash = """#!/bin/sh
# Post-script for job $jobId in workflow ${workflow.workflowName}
#
script_type=\$1 # pre or post
CONDOR_JOB=\$2
RETURN=\$8
PRE_SCRIPT_RETURN=\${9}
cat > ${resultFile.path} << END_OF_METADATA_HTML_SCRIPT_HEREDOC_9827312838923u78673
CONDOR_JOB : \${CONDOR_JOB}
RETURN : \${RETURN}
PRE_SCRIPT_RETURN : \${PRE_SCRIPT_RETURN}
END_OF_METADATA_HTML_SCRIPT_HEREDOC_9827312838923u78673
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
