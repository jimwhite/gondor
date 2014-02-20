import groovy.transform.BaseScript

class FooScript extends Script {
    def run() {
        println "yo"
    }
}

@BaseScript FooScript thisScript

println "yip"
super.run()
println "yay"