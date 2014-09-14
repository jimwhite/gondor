#!/usr/bin/env groovy
//#!/usr/bin/env CLASSPATH=build/libs/Gondor-0.1.jar groovy

@groovy.transform.BaseScript(groovyx.cli.JCommanderScript)
import groovyx.cli.JCommanderScript

System.getenv().each { k, v -> println "$k=$v" }

// The value of the last expression is the result of this script and will be our process' exit code.
0
