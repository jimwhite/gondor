#!/usr/bin/perl

# This script is used by the JoinOutputErrorTest unit test, which
# tests if the merging of STDOUT and STDERR works properly in the
# DRMAA implementation. This script generates both STDOUT and STDERR.

use strict;

if (scalar(@ARGV) != 2) {
    print "Usage: $0: <STDOUT> <STDERR>\n";
    exit 2;
}
print STDOUT $ARGV[0] . "\n"; 
print STDERR $ARGV[1] . "\n";
exit 0;
