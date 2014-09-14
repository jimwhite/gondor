#!/usr/bin/env groovy
//#!/usr/bin/env CLASSPATH=gondor/build/libs/gondor-0.1-SNAPSHOT.jar groovy

@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript thisScript

copyEnvironment('PATH', 'CLASSPATH')

def outputDir = new File("output")
outputDir.mkdirs()

//this.class.getDeclaredConstructors().each { println it }

def fileListGrepWorkflow = workflow(groovy(path:"gondor/scripts/FileListGrepWorkflow.groovy"))

// scripts/FileListGrepWorkflow.groovy --path ../scripts --regex Go --result ls_grep_res.txt ../src/org/ifcx/gondor ..
def p = fileListGrepWorkflow(path:new File('gondor/scripts'), pattern:'Go'
        , new File('gondor/src/org/ifcx/gondor'), new File('gondor/doc')
        , result:new File(outputDir, 'tl_ls_grep_res.txt'))

p >>> new File(outputDir, 'tl_error.txt')

// assert p.output == new File('FileListGrepWorkflow.dag')
println p.output.path

// fileListGrepWorkflow('--path':'scripts', '--pattern':'Go',
//        'src/org/ifcx/gondor', '.', '--result':'tl_ls_grep_res.txt') >>> 'tl_flsgrep-err.txt'
