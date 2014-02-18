import groovy.transform.Field

@Field
PrintWriter printer = new PrintWriter(System.out)

// Must use this after any transformations that check ClassNode.isScriptBody
// if you are changing the name of the run script method in the BaseScript annotation.
@groovy.transform.BaseScript org.ifcx.gondor.SessionScript thisScript



//
//import groovy.transform.BaseScript
//import org.ifcx.gondor.GondorScript
//@BaseScript GondorScript thisScript

def a = 123

if (this instanceof GroovyInterceptable) println("We want to GI");

println "yo"

println thisScript
println this
println session
println a

PrintWriter getOut() {  printer }

printer.flush()
