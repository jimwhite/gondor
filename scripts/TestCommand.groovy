#!/usr/bin/env groovy

import com.beust.jcommander.Parameter
import groovy.transform.Field
import groovyx.cli.JCommanderScript
import org.ifcx.gondor.Command

@groovy.transform.BaseScript JCommanderScript thisScript

@Parameter(names='--path') @Field File path

@Parameter(names='--pattern') @Field String pat

@Parameter @Field List<File> paths

@Parameter(names=['--help', '-h'], help=true, arity=-1) @Field String foo

[path, *paths].each { File dir -> dir.listFiles().name.findAll { it =~ pattern  }.each { println it } }

0
