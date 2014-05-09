package org.ifcx.gondor

class Process
{
    String jobId

    Command command

    Map<String, Object> params

    List<File> infiles = []
    List<File> outfiles = []

    private File _stdin
    private File _stdout
    private File _stderr

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
        if (_stdin) throw new IllegalStateException("stdin already set")
        _stdin = source
        infiles << _stdin
        this
    }

    File getInput() {
        if (!_stdin) {
            fromFile(command.newTemporaryFile(".in"))
        }
        _stdin
    }

    Process rightShift(File sink) { toFile(sink) }

    Process toFile(File sink) {
        if (_stdout) throw new IllegalStateException("stdout already set")
        _stdout = sink
        outfiles << _stdout
        this
    }

    File getOutput() {
        if (!_stdout) {
            toFile(command.newTemporaryFile(".out"))
        }
        _stdout
    }

    Process rightShiftUnsigned(File sink) { errorFile(sink) }

    Process errorFile(File sink) {
        if (_stderr) throw new IllegalStateException("stderr already set")
        _stderr = sink
        outfiles << _stderr
        this
    }

    File getError() {
        if (!_stderr) {
            errorFile(command.newTemporaryFile(".err"))
        }
        _stderr
    }

}
