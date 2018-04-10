#!/bin/sh
CP=conf/:classes/:lib/*:testlib/*
SP=src/java/:test/java/

if [ $# -eq 0 ]; then
TESTS="dbn.crypto.Curve25519Test dbn.crypto.ReedSolomonTest dbn.peer.HallmarkTest dbn.TokenTest dbn.FakeForgingTest
dbn.FastForgingTest dbn.ManualForgingTest"
else
TESTS=$@
fi

/bin/rm -f dbn.jar
/bin/rm -rf classes
/bin/mkdir -p classes/

javac -encoding utf8 -sourcepath ${SP} -classpath ${CP} -d classes/ src/java/dbn/*.java src/java/dbn/*/*.java test/java/dbn/*.java test/java/dbn/*/*.java || exit 1

for TEST in ${TESTS} ; do
java -classpath ${CP} org.junit.runner.JUnitCore ${TEST} ;
done



