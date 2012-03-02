#/bin/bash

#$JAVA_JDK/bin/javadoc --help >javadoc.help

if test "$JAVA_JDK" = "";  then export JAVA_JDK="/usr/share/JDK"; fi

rm -f -r $DST
rm -f -r $DST_priv

mkdir $DST
mkdir $DST_priv

echo generate docu: $SRC
echo javadoc -d $DST $LINKPATH -sourcepath ..:$SRCPATH
$JAVA_JDK/bin/javadoc -d $DST $LINKPATH -sourcepath $SRCPATH $SRC 1>$DST/javadoc.rpt 2>$DST/javadoc.err
echo javadoc -d $DST_priv $LINKPATH -sourcepath $SRCPATH

$JAVA_JDK/bin/javadoc -d $DST_priv -private $LINKPATH -sourcepath ..:$SRCPATH $SRC 1>$DST_priv/javadoc.rpt 2>$DST_priv/javadoc.err

mkdir $DST/img
cp -r ../img $DST

mkdir $DST_priv/img
cp -r ../img $DST_priv

cp stylesheet_javadoc.css $DST/stylesheet.css
cp stylesheet_javadoc.css $DST_priv/stylesheet.css

echo successfull generated $DST
