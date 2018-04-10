#!/bin/sh
CP="lib/*:classes"
SP=src/java/

/bin/rm -rf html/doc/*

javadoc -quiet -sourcepath ${SP} -classpath "${CP}" -protected -splitindex -subpackages dbn -d html/doc/
