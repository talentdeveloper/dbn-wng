#!/bin/sh
if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi
${JAVA} -cp classes:lib/*:conf:addons/classes:addons/lib/* -Ddbn.runtime.mode=desktop -Ddbn.runtime.dirProvider=dbn.env.DefaultDirProvider dbn.Dbn
