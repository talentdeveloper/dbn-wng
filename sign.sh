#!/bin/sh
java -cp "classes:lib/*:conf" dbn.tools.SignTransactionJSON $@
exit $?
