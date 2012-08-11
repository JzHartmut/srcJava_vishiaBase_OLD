#/bin/bash

#$JAVA_JDK/bin/javadoc --help >javadoc.help

if test -z "$JAVA_JDK";  then
  if test -d /usr/share/JDK; then export JAVA_JDK="/usr/share/JDK"; fi
  if test -d /d/Progs/JAVA/jdk1.6.0_21; then export JAVA_JDK="/d/Progs/JAVA/jdk1.6.0_21"; fi
fi
echo JAVA_JDK=$JAVA_JDK
echo 2012-06-10
rm -f -r $DST/*
rm -f -r $DST_priv/*

#mkdir $DST
#mkdir $DST_priv

echo generate docu: $SRC

echo javadoc -d $DST -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH
$JAVA_JDK/bin/javadoc -d $DST -protected -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH $SRC 1>$DST/javadoc.rpt 2>$DST/javadoc.err

echo javadoc -d $DST_priv -private -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH
$JAVA_JDK/bin/javadoc -d $DST_priv -private -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH $SRC 1>$DST_priv/javadoc.rpt 2>$DST_priv/javadoc.err

mkdir $DST/img
cp -r ../img $DST

mkdir $DST_priv/img
cp -r ../img $DST_priv

cp ../../srcJava_vishiaBase/_make/stylesheet_javadoc.css $DST/stylesheet.css
cp ../../srcJava_vishiaBase/_make/stylesheet_javadoc.css $DST_priv/stylesheet.css

echo successfull generated $DST
