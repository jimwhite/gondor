#!/usr/bin/env CLASSPATH=build/libs/Gondor-0.1.jar groovy

@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript thisScript

copyEnvironment('PATH')

//this.class.getDeclaredConstructors().each { println it }

def fileListGrepWorkflow = workflow(groovy(path:"scripts/FileListGrepWorkflow.groovy"))

// scripts/FileListGrepWorkflow.groovy --path ../scripts --regex Go --result ls_grep_res.txt ../src/org/ifcx/gondor ..
def p = fileListGrepWorkflow(path:new File('scripts'), pattern:'Go'
        , new File('src/org/ifcx/gondor'), new File('doc')
        , result:new File('tl_ls_grep_res.txt'))

p >>> new File('tl_error.txt')

// assert p.output == new File('FileListGrepWorkflow.dag')
println p.output.path

// fileListGrepWorkflow('--path':'scripts', '--pattern':'Go',
//        'src/org/ifcx/gondor', '.', '--result':'tl_ls_grep_res.txt') >>> 'tl_flsgrep-err.txt'
