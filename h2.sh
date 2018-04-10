#!/bin/sh
java -cp lib/h2*.jar org.h2.tools.Shell -url jdbc:h2:./dbn_db/dbn -user sa -password sa
