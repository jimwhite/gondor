#!/usr/env groovy

import com.beust.jcommander.Parameter
import groovy.transform.Field
import groovyx.cli.JCommanderScript
import org.ifcx.gondor.Command

@groovy.transform.BaseScript JCommanderScript thisScript

@Parameter @Field File path

@Parameter @Field String pat

def ls = command(path:'/bin/ls') { infile 'path' }

def grep = command(path:'/usr/bin/grep') { arg 'pat', { it }, Command.REQUIRED }

// (ls(path:'.') | grep(pat:/Work/)) >> new File('grep_out.txt')
(ls(path:path) | grep(pat:pat)) >> new File('grep_out.txt')

