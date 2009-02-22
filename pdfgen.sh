#!/bin/sh

mkdir -p tex
cd tex
CLASSPATH="$CLASSPATH:tmp" javadoc -private -author -version -sourcepath ../src/ -docletpath texdoclet.jar -doclet org.wonderly.doclets.TexDoclet -title "Liite A: Ohjelman API-kuvaus" -author "Mikko Sysikaski" lento.gamestate lento.gameui lento.net lento.menu
head -1 docs.tex > lento.tex
echo "\usepackage[utf8]{inputenc}" >> lento.tex
tail +2 docs.tex >> lento.tex
latex lento.tex
dvipdf lento.dvi
#okular lento.pdf &

CLASSPATH="$CLASSPATH:tmp:../classes/:/usr/lib/junit.jar" javadoc -private -author -version -sourcepath ../test/ -docletpath texdoclet.jar -doclet org.wonderly.doclets.TexDoclet -title "Liite C: Testien API-kuvaus" -author "Mikko Sysikaski" lento.gamestate lento.gameui lento.net
head -1 docs.tex > test.tex
echo "\usepackage[utf8]{inputenc}" >> test.tex
tail +2 docs.tex >> test.tex
latex test.tex
dvipdf test.dvi

cd -
