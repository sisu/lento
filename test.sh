#!/bin/sh

ant test-compile || exit 1
cd classes
for i in `find lento -name "Test*.class"`; do
#	echo $i
	x=`echo "$i" | awk -F . '{print $1}' | replace / .`
	echo "testing class $x"
	java -classpath .:/usr/share/java/junit.jar -ea org.junit.runner.JUnitCore "$x"
done
cd -
