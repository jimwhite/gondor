import org.ifcx.gondor.Command

@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript thisScript

def ls = command(path:'/bin/ls') { infile 'path' }

def grep = command(path:'/usr/bin/grep') { arg 'pat', { it }, Command.REQUIRED }

// ls('.')
(ls(path:new File('.')) | grep(pat:/Work/)) >> new File('grep_out.txt')

