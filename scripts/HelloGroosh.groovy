//@groovy.lang.GrabConfig(systemClassLoader = true)
@Grab(group='org.codehaus.groovy.modules.groosh', module='groovy-groosh', version='0.3.5')

//@groovy.transform.BaseScript org.ifcx.gondor.SecuredScript thisScript

import groosh.Groosh
import org.codehaus.groovy.groosh.GrooshProcess
import org.codehaus.groovy.groosh.stream.StringStreams

Groosh.withGroosh(this)

//def zz = new File('bar.txt')
//zz <<= "ONE LINE\n"
tr('a-z', 'A-Z') | sort() << new File('../README') >> new File('bar.txt')
//zz <<= "\nONE LAST LINE\n"

//return 0

echo("hello world") >> stdout
echo("hello world") >> new File('baz.txt')

println (new File(System.getProperty("user.home")).listFiles())

cat('HelloGroosh.groovy') >> new File('foo.txt')

(tr('a-z', 'A-Z') << new File('../README') | sort()) >> new File('bar.txt')
//(ls() | tr('a-z', 'A-Z')) >> new File('zax.txt')
//println new File('zax.txt').text

//def sink = StringStreams.stringSink()
(ls() | tr('a-z', 'A-Z')).eachLine { println it }
//println sink.toString().length()
//println sink

(date() | tr('A-Z', 'a-z')) >> stdout
which('java') >> stdout
//cat('HelloGroosh.groovy') >> stdout

(tr('a-z', 'A-Z') << "four score and sven").eachLine { println it }

(tr('a-z', 'A-Z') << '''Disable LibreOffice's Spotlight importer:

/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Versions/A/Support/lsregister -u /Applications/LibreOffice.app

To make python 2.7 the default (i.e. the version you get when you run 'python'), please run:

sudo port select --set python python27
sudo port install texlive texlive-pictures texlive-humanities

port install evince hevea

############################################################################
# Startup items have been generated that will aid in
# starting dbus with launchd. They are disabled
# by default. Execute the following command to start them,
# and to cause them to launch at startup:
#
# sudo launchctl load -w /Library/LaunchDaemons/org.freedesktop.dbus-system.plist
# launchctl load -w /Library/LaunchAgents/org.freedesktop.dbus-session.plist
############################################################################

''').toList().eachWithIndex { l, i -> println "$i: $l" }
