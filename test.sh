#!/bin/sh

ant test-compile || exit 1
cd tclasses
for i in `find ../test/ |grep java`; do
	echo $i
	x=`echo "$i" | awk -F . '{print $1}' | replace / .`
#	x=test.`basename $i .class`
	echo "testing class $x"
	java -classpath .:../classes/:/usr/share/java/junit.jar -ea org.junit.runner.JUnitCore "$x" || break
done
cd -
