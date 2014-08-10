#!/usr/bin/env groovy

import com.beust.jcommander.Parameter
import com.beust.jcommander.converters.BaseConverter
import groovy.transform.Field
import groovyx.cli.JCommanderScript
import groovyx.cli.PatternConverter

import java.util.regex.Pattern

@groovy.transform.BaseScript JCommanderScript thisScript

@Parameter(names='--path') @Field File path

//@Parameter(names='--pattern', converter = { java.util.regex.Pattern.compile(it) } ) @Field Pattern pat
@Parameter(names='--pattern', converter = groovyx.cli.PatternConverter.class ) @Field pat

@Parameter @Field List<File> paths = []

@Parameter(names=['--help', '-h'], help=true, arity=-1) @Field String foo

[path, *paths].each { File dir -> dir.listFiles()*.name.findAll { pat.matcher(it).matches() }.each { println it } }

0
