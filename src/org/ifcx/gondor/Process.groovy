package org.ifcx.gondor

class Process
{
    String jobId

    Command command

    Map<String, Object> params

    List<File> infiles = []
    List<File> outfiles = []

    Map<String, Object> attributes = [:]

    Set<String> psuedo_io = []

    public final static String INPUT = ":input";
    public final static String OUTPUT = ":output";
    public final static String ERROR = ":error";

    public Process(Command command, Map<String, Object> params) {
        this.command = command
        this.params = params
        initializeAttributes()
    }

    void initializeAttributes() {
        [INPUT, OUTPUT, ERROR].each {
            if (params.containsKey(it)) {
                def value = command.getArgumentDefaultValues()[it]
                if (value != null) {
                    attributes[it] = value
                    psuedo_io << it
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
        if (!attributes[INPUT] != null) {
            fromFile(command.newTemporaryFile(".in"))
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
            toFile(command.newTemporaryFile(".out"))
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
            errorFile(command.newTemporaryFile(".err"))
        }
        (File) attributes[ERROR]
    }

}
