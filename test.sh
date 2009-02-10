#!/bin/sh

ant test-compile
cd test
for i in *.java; do
	x=`basename "$i" .java`
	java -classpath .:/usr/share/java/junit.jar -ea org.junit.runner.JUnitCore "$x"
done
cd -
