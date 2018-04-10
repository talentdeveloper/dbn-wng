#!/bin/bash
VERSION=$1
if [ -x ${VERSION} ];
then
	echo VERSION not defined
	exit 1
fi
PACKAGE=dbn-client-${VERSION}
echo PACKAGE="${PACKAGE}"
CHANGELOG=dbn-client-${VERSION}.changelog.txt
OBFUSCATE=$2

FILES="changelogs conf html lib resource contrib logs"
FILES="${FILES} dbn.exe dbnservice.exe"
FILES="${FILES} 3RD-PARTY-LICENSES.txt AUTHORS.txt LICENSE.txt"
FILES="${FILES} DEVELOPERS-GUIDE.md OPERATORS-GUIDE.md README.md README.txt USERS-GUIDE.md"
FILES="${FILES} mint.bat mint.sh run.bat run.sh run-tor.sh run-desktop.sh start.sh stop.sh compact.sh compact.bat sign.sh"
FILES="${FILES} dbn.policy dbndesktop.policy DBN_Wallet.url Dockerfile"

# unix2dos *.bat
echo compile
./win-compile.sh
rm -rf html/doc/*
rm -rf dbn
rm -rf ${PACKAGE}.jar
rm -rf ${PACKAGE}.exe
rm -rf ${PACKAGE}.zip
mkdir -p dbn/
mkdir -p dbn/logs
mkdir -p dbn/addons/src

if [ "${OBFUSCATE}" == "obfuscate" ];
then
echo obfuscate
proguard.bat @dbn.pro
mv ../dbn.map ../dbn.map.${VERSION}
mkdir -p dbn/src/
else
FILES="${FILES} classes src COPYING.txt"
FILES="${FILES} compile.sh javadoc.sh jar.sh package.sh"
FILES="${FILES} win-compile.sh win-javadoc.sh win-package.sh"
echo javadoc
./win-javadoc.sh
fi
echo copy resources
cp installer/lib/JavaExe.exe dbn.exe
cp installer/lib/JavaExe.exe dbnservice.exe
cp -a ${FILES} dbn
cp -a logs/placeholder.txt dbn/logs
echo gzip
for f in `find dbn/html -name *.gz`
do
	rm -f "$f"
done
for f in `find dbn/html -name *.html -o -name *.js -o -name *.css -o -name *.json  -o -name *.ttf -o -name *.svg -o -name *.otf`
do
	gzip -9c "$f" > "$f".gz
done
cd dbn
echo generate jar files
../jar.sh
echo package installer Jar
../installer/build-installer.sh ../${PACKAGE}
echo create installer exe
../installer/build-exe.bat ${PACKAGE}
echo create installer zip
cd -
zip -q -X -r ${PACKAGE}.zip dbn -x \*/.idea/\* \*/.gitignore \*/.git/\* \*.iml dbn/conf/dbn.properties dbn/conf/logging.properties dbn/conf/localstorage/\*
rm -rf dbn

echo creating change log ${CHANGELOG}
echo -e "Release $1\n" > ${CHANGELOG}
echo -e "https://bitbucket.org/JeanLucPicard/dbn/downloads/${PACKAGE}.exe\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.exe >> ${CHANGELOG}

echo -e "https://bitbucket.org/JeanLucPicard/dbn/downloads/${PACKAGE}.jar\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.jar >> ${CHANGELOG}

if [ "${OBFUSCATE}" == "obfuscate" ];
then
echo -e "\n\nThis is an experimental release for testing only. Source code is not provided." >> ${CHANGELOG}
fi
echo -e "\n\nChange log:\n" >> ${CHANGELOG}

cat changelogs/${CHANGELOG} >> ${CHANGELOG}
echo >> ${CHANGELOG}
