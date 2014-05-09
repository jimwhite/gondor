import org.ifcx.gondor.Command

@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript thisScript

// Define a command for /bin/ls.  It takes an optional file path argument.
def ls = command(path:'/bin/ls') { infile 'path' }

def grep = command(path:'/usr/bin/grep') {
    // An optional argument for whether the output should have numbered lines.
    // The third argument here is a closure that takes the given parameter value
    // and returns the string that will appear as the command line argument.
    arg 'lineNumbers', Command.OPTIONAL, { it ? '--line-number' : [] }

    // Arguments are currently optional by default.
    // Here that is overridden as required, but the default formatter is used.
    // The default is { it } which simply uses the string representation of the object.
    arg 'pat', Command.REQUIRED
}

// ls('.')
// (ls(path:'.') | grep(pat:/Work/)) >> new File('grep_out.txt')

(ls(path:new File('.')) | grep(pat:/Work/)) >> new File('grep_out.txt')

(ls() | grep(pat:/est/, lineNumbers:true)) >> new File('grep_with_numbers.txt')
