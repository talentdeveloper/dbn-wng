#!/bin/sh
java -cp classes dbn.tools.ManifestGenerator
/bin/rm -f dbn.jar
jar cfm dbn.jar resource/dbn.manifest.mf -C classes . || exit 1
/bin/rm -f dbnservice.jar
jar cfm dbnservice.jar resource/dbnservice.manifest.mf -C classes . || exit 1

echo "jar files generated successfully"