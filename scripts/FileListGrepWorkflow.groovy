#!/usr/bin/env CLASSPATH=build/libs/Gondor-0.1.jar groovy

import com.beust.jcommander.Parameter
import groovy.transform.Field
import org.ifcx.gondor.Command
import org.ifcx.gondor.api.InputDirectory
import org.ifcx.gondor.api.OutputFile

@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript thisScript

@Parameter(names = ['--path', 'path'], description = 'Path to directory for the first file list.')
@InputDirectory @Field File path

@Parameter(names = ['--pattern', '--regex', 'pattern', 'regex'], description = 'Regular expression to filter with.')
@Field String pattern

@Parameter(names = ['--result', 'result'], description = 'Output file.')
@OutputFile @Field File result

@Parameter(description = 'A list of directory paths for additional file lists')
@InputDirectory @Field List<File> paths

copyEnvironment('PATH')

environment.FOO = 'hey there! what\'s up "doc"?'
environment.BAR = "/yo /   3 spaces'path /"
// environment.'X=Y' = 'Z'  // Check that we can catch bad names.

// Define a command for /bin/ls.  It takes an optional file path argument.
def ls = command(path:'/bin/ls') { infile 'path' }

// Note that the exit status for grep is 1 if there are no matched lines.
// Gondor does not yet have an option for treating non-zero exit status values as an indication of success.
def grep = command(path:'/usr/bin/grep') {
    // An optional argument for whether the output should have numbered lines.
    // The third argument here is a closure that takes the given parameter value
    // and returns the string that will appear as the command line argument.
    arg 'lineNumbers', Command.OPTIONAL, { it ? '--line-number' : [] }

    arg 'ignoreCase', Command.OPTIONAL, { it ? '--ignore-case' : [] }

    // Arguments are currently optional by default, but ere that is overridden as required.
    // The formatting closure may return a list of strings rather than just one.
    // That is used here to provide the '-e' flag and it's associated pattern value.
    // Note that grep permits multiple patterns, so we return a list of those pairs.
    // The stringify(v) helper returns its argument as a flattened list of string values.
    arg 'pat', Command.REQUIRED, { stringify(it).collect { ['-e', it] } }
}

def cat = command(path:'/bin/cat') { infile 'paths' }

// (ls(path:path) | grep(pat:pattern)) >> result

// (ls() | grep(pat:/est/, lineNumbers:true)) >> new File('grep_with_numbers.txt')

cat(paths:[path, *paths].collect { (ls(path:it) |
        grep(pat:pattern, ignoreCase:true, lineNumbers:true)).output }) >> new File("bin-${result.name}")

def fileListGrep = groovy(path:"scripts/FileListGrepCommand.groovy")

fileListGrep(path:path, pattern:pattern, result:result, *paths) >>> new File('flsgrep-err.txt')

groovy(path:"scripts/EchoEnvironment.groovy").call() >> new File(new File("."), "env-dump.txt")
// That can also be done this way, but it isn't very obvious that we're calling the closure,
// especially when there are no parameters being passed in the call.
// groovy(path:"scripts/EchoEnvironment.groovy")() >> new File(new File("."), "env-dump.txt")

// Can't do any of these because spaces are not allowed in paths (and don't use commas either).
//groovy(path:"scripts/EchoEnvironment.groovy")() >> new File(new File("this dir name has spaces"), "env_dump1.txt")
//groovy(path:"scripts/EchoEnvironment.groovy")() >> new File(new File(" this dir name has spaces"), " env_dump2.txt")
//groovy(path:"scripts/EchoEnvironment.groovy")() >> new File(new File(new File("."), "this dir name has spaces"), "env_dump3.txt")
