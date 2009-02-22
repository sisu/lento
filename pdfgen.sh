#!/bin/sh

mkdir -p tex
cd tex
CLASSPATH="$CLASSPATH:tmp" javadoc -private -author -version -sourcepath ../src/ -docletpath texdoclet.jar -doclet org.wonderly.doclets.TexDoclet -title "Luolalentely verkossa" -author "Mikko Sysikaski" lento.gamestate lento.gameui lento.net lento.menu
head -1 docs.tex > lento.tex
echo "\usepackage[utf8]{inputenc}" >> lento.tex
tail +2 docs.tex >> lento.tex
latex lento.tex
dvipdf lento.dvi
okular lento.pdf &
cd -
