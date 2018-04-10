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
MACVERSION=$3
if [ -x ${MACVERSION} ];
then
MACVERSION=${VERSION}
fi
echo MACVERSION="${MACVERSION}"

FILES="changelogs conf html lib resource contrib"
FILES="${FILES} dbn.exe dbnservice.exe"
FILES="${FILES} 3RD-PARTY-LICENSES.txt AUTHORS.txt LICENSE.txt"
FILES="${FILES} DEVELOPERS-GUIDE.md OPERATORS-GUIDE.md README.md README.txt USERS-GUIDE.md"
FILES="${FILES} mint.bat mint.sh run.bat run.sh run-tor.sh run-desktop.sh start.sh stop.sh compact.sh compact.bat sign.sh"
FILES="${FILES} dbn.policy dbndesktop.policy DBN_Wallet.url Dockerfile"

echo compile
./compile.sh
rm -rf html/doc/*
rm -rf dbn
rm -rf ${PACKAGE}.jar
rm -rf ${PACKAGE}.exe
rm -rf ${PACKAGE}.zip
mkdir -p dbn/
mkdir -p dbn/logs
mkdir -p dbn/addons/src

if [ "${OBFUSCATE}" = "obfuscate" ]; 
then
echo obfuscate
~/proguard/proguard5.2.1/bin/proguard.sh @dbn.pro
mv ../dbn.map ../dbn.map.${VERSION}
else
FILES="${FILES} classes src COPYING.txt"
FILES="${FILES} compile.sh javadoc.sh jar.sh package.sh"
FILES="${FILES} win-compile.sh win-javadoc.sh win-package.sh"
echo javadoc
./javadoc.sh
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
cd -
rm -rf dbn

echo bundle a dmg file	
/Library/Java/JavaVirtualMachines/jdk1.8.0_66.jdk/Contents/Home/bin/javapackager -deploy -outdir . -outfile dbn-client -name dbn-installer -width 34 -height 43 -native dmg -srcfiles ${PACKAGE}.jar -appclass com.izforge.izpack.installer.bootstrap.Installer -v -Bmac.category=Business -Bmac.CFBundleIdentifier=org.dbn.client.installer -Bmac.CFBundleName=Dbn-Installer -Bmac.CFBundleVersion=${MACVERSION} -BappVersion=${MACVERSION} -Bicon=installer/AppIcon.icns -Bmac.signing-key-developer-id-app="Developer ID Application: Stichting DBN (YU63QW5EFW)" > installer/javapackager.log 2>&1
