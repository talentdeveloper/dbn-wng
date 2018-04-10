#!/bin/sh
CP="conf/;classes/;lib/*;testlib/*"
SP="src/java/;test/java/"
TESTS="dbn.crypto.Curve25519Test dbn.crypto.ReedSolomonTest"

/bin/rm -f dbn.jar
/bin/rm -rf classes
/bin/mkdir -p classes/

javac -encoding utf8 -sourcepath $SP -classpath $CP -d classes/ src/java/dbn/*.java src/java/dbn/*/*.java test/java/dbn/*/*.java || exit 1

java -classpath $CP org.junit.runner.JUnitCore $TESTS

