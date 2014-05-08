import groovy.transform.BaseScript

@BaseScript FooScript thisScript

println "yip"
super.run()
println "yay"

class FooScript extends Script {
    def run() {
        println "yo"
    }
}
