#!/bin/sh
CP="lib/*;classes"
SP=src/java/

/bin/rm -f dbn.jar
/bin/rm -f dbnservice.jar
/bin/rm -rf classes
/bin/mkdir -p classes/
/bin/rm -rf addons/classes
/bin/mkdir -p addons/classes/

javac -encoding utf8 -sourcepath "${SP}" -classpath "${CP}" -d classes/ src/java/dbn/*.java src/java/dbn/*/*.java src/java/dbn/*/*/*.java src/java/dbndesktop/*.java || exit 1

echo "dbn class files compiled successfully"

ls addons/src/*.java > /dev/null 2>&1 || exit 0
javac -encoding utf8 -sourcepath "${SP}" -classpath "${CP}" -d addons/classes addons/src/*.java || exit 1

echo "addon class files compiled successfully"
