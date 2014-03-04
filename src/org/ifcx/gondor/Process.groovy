package org.ifcx.gondor

import org.ggf.drmaa.JobTemplate

class Process
{
    String jobId

    Command command

    Map<String, Object> params

    List<File> infiles = []
    List<File> outfiles = []

    File _stdin
    File _stdout
    File _stderr

    Process or(Process sink) { toProcess(sink) }

    Process toProcess(Process sink) {
        sink.fromFile(stdout)
    }

    Process leftShift(File source) { fromFile(source) }

    Process fromFile(File source) {
        if (_stdin) throw new IllegalStateException("stdin already set")
        _stdin = source
        infiles << _stdin
        this
    }

    File getStdin() {
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

    File getStdout() {
        if (!_stdout) {
            toFile(command.newTemporaryFile(".out"))
        }
        _stdout
    }

    Process rightShiftUnsigned(File sink) { errFile(sink) }

    Process errFile(File sink) {
        if (_stderr) throw new IllegalStateException("stderr already set")
        _stderr = sink
        outfiles << _stderr
        this
    }

    File getStderr() {
        if (!_stderr) {
            errFile(command.newTemporaryFile(".err"))
        }
        _stderr
    }

}
