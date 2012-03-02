
##--------------------------------------------------------------------------------------------
## Environment variables set from zbnfjax:
## JAVA_JDK: Directory where bin/javac is found. This java version will taken for compilation
## The java-copiler may be located at a user-specified position.
## Set the environment variable JAVA_HOME, where bin/javac will be found.
if test "$JAVA_JDK" = "";  then export JAVA_JDK="/usr/share/JDK"; fi


export DST="../../docuSrcJava_vishiaBase"
export DST_priv="../../docuSrcJava_vishiaBase_priv"

export SRC="-subpackages org.vishia"

rm -f -r $DST
rm -f -r $DST_priv

mkdir $DST
mkdir $DST_priv

echo generate docu: $SRC
echo javadoc -d $DST -linksource -notimestamp -sourcepath ..
$JAVA_JDK/bin/javadoc -d $DST -linksource -notimestamp -sourcepath .. $SRC   1>$DST/javadoc.rpt 2>$DST/javadoc.err
echo javadoc -d $DST_priv -private -linksource -notimestamp -sourcepath ..
$JAVA_JDK/bin/javadoc -d $DST_priv -private -linksource -notimestamp -sourcepath .. $SRC   1>$DST_priv/javadoc.rpt 2>$DST_priv/javadoc.err

echo copy img
mkdir $DST/img
cp -r ../img $DST

mkdir $DST_priv/img
cp -r ../img $DST_priv

cp stylesheet_javadoc.css $DST/stylesheet.css
cp stylesheet_javadoc.css $DST_priv/stylesheet.css

echo successfull generated $DST
