#!/bin/sh
if [ -e ~/.dbn/dbn.pid ]; then
    PID=`cat ~/.dbn/dbn.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    echo "stopping"
    while [ $STATUS -eq 0 ]; do
        kill `cat ~/.dbn/dbn.pid` > /dev/null
        sleep 5
        ps -p $PID > /dev/null
        STATUS=$?
    done
    rm -f ~/.dbn/dbn.pid
    echo "Dbn server stopped"
fi

