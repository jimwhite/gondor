//Read a text file and write it to stdout
@Grab(group='org.codehaus.groovy.modules.groosh', module='groovy-groosh', version='0.3.5')
@groovy.lang.GrabConfig(systemClassLoader = true)

import groosh.Groosh
Groosh.withGroosh(this)

echo("hello world") >> new File('baz.txt')
cat('HelloGroosh.groovy') >> new File('foo.txt')
(tr('a-z', 'A-Z') << new File('../README') | sort()) >> new File('bar.txt')
(ls() | tr('a-z', 'A-Z')) >> new File('zax.txt')
println new File('zax.txt').text
(date() | tr('A-Z', 'a-z')) >> stdout
which('java') >> stdout
cat('HelloGroosh.groovy') >> stdout
echo("hello world") >> stdout
