## Gondor ("Groovy Condor")
### Gondor : High Throughput Computing in High Style

Gondor is a tool that aims to make programming HTCondor DAGman workflows simple, streamlined, and (eventually) more
powerful.  Planned features include workflow reduction (job output memoization) and provenance metadata that supports
reproducible computation.

There are some sample scripts in the scripts directory.  `TestWorkflow.groovy` is a functioning workflow that runs a
couple jobs using `/bin/ls` and `/usr/bin/grep`.  `HelloGondor.groovy` and `GoodGondor.groovy` illustrate some of the API.

* NEW: `FileListGrepCommand.groovy` is a Groovy script using JCommander CLI and Gradle Task annotations.  `FileListGrepWorkflow.groovy` is a workflow that calls that command as well as the binary commands as in `TestWorkflow.groovy`.  The API will be changing significantly soon though, primarily the addition of Gondor CLI annotations which will be preferred over the JCommander + Gradle annotations.

Gondor depends on features [Groovy](http://groovy.codehaus.org/Download) that are new in the 2.3.0 release,
so you will need at least that version installed in the usual way.
I recommend the full "sdk" distribution, but any should work fine.

At present the easiest way to run the script is inside Intellij IDEA.
You should be able to use `Open Project` on the `Gondor` directory and fix up the Groovy framework library reference to where you have 2.3.0 installed.

An item for `TestWorkflow` should appear in the run configurations menu.  It contains these settings:

```
Script path: [parent]/Gondor/scripts/TestWorkflow.groovy
Module: gondor
VM options: -Dorg.ggf.drmaa.SessionFactory=net.sf.igs.SessionFactoryImpl -Dorg.ifcx.drmaa.WorkflowFactory=org.ifcx.gondor.WorkflowFactoryImpl
Working dir: [parent]/Gondor/scripts
```

The reason for running inside IntelliJ is setting the classpath, but that can be done from the command line as well.  
There is a Gradle build, so in the `Gondor` dir you can do:

```
$ gradle jar
:compileJava UP-TO-DATE
:compileGroovy
warning: [options] bootstrap class path not set in conjunction with -source 1.6
Note: /Users/jim/Downloads/xxx/Gondor/src/org/ifcx/gondor/NullSecurityManager.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
1 warning
:processResources
:classes
:jar

BUILD SUCCESSFUL

Total time: 9.901 secs

$ CLASSPATH=build/libs/Gondor-0.1.jar groovy scripts/TestWorkflow.groovy

Generated 2 jobs for Condor DAG TestWorkflow.dag

That should generate a file TestWorkflow.dag and a directory TestWorkflow.jobs.

The DAGman workflow can then be submitted in the usual way:

$ condor_submit_dag TestWorkflow.dag
```
