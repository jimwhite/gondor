#!/usr/bin/env CLASSPATH=../build/libs/Gondor-0.1.jar /Users/jim/Projects/Groovy/groovy-2.3.0/bin/groovy

import com.beust.jcommander.Parameter
import groovy.transform.Field
import groovyx.cli.JCommanderScript
import org.ifcx.gondor.api.InputDirectory
import org.ifcx.gondor.api.OutputFile

import java.util.regex.Pattern

@groovy.transform.BaseScript JCommanderScript thisScript

@Parameter(names = '--path', description = 'Path to directory for the first file list.')
@InputDirectory @Field File path

//@Parameter(names = ['--pattern', '--regex'], converter = { ~it } as IStringConverter<Pattern>, description = 'Regular expression to filter with.')
@Parameter(names = ['--pattern', '--regex']
        , converter = groovyx.cli.PatternConverter.class
        , description = 'Regular expression to filter with.')
@Field Pattern pattern

@Parameter(names = '--result', description = 'Output file.')
@OutputFile @Field File result

@Parameter(description = 'A list of directory paths for additional file lists')
@InputDirectory @Field List<File> paths

// cat(paths:[path, *paths].collect { (ls(path:it) | grep(pat:pattern, lineNumbers:true)).output }) >> result

result.withPrintWriter {
    out = it
//    [path, *paths].each { File dir -> dir.listFiles().name.findAll { pattern.matcher(it).find() }.each { println it } }
    [path, *paths].each { File dir -> dir.listFiles().name.findAll { it =~ pattern  }.each { println it } }
}

0
