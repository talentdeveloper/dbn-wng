#!/bin/sh
if [ -e ~/.dbn/dbn.pid ]; then
    PID=`cat ~/.dbn/dbn.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    if [ $STATUS -eq 0 ]; then
        echo "Dbn server already running"
        exit 1
    fi
fi
mkdir -p ~/.dbn/
DIR=`dirname "$0"`
cd "${DIR}"
if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi
nohup ${JAVA} -cp classes:lib/*:conf:addons/classes:addons/lib/* -Ddbn.runtime.mode=desktop dbn.Dbn > /dev/null 2>&1 &
echo $! > ~/.dbn/dbn.pid
cd - > /dev/null
