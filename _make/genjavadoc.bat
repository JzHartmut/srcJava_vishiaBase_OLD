

set DSTDIR=..\..\
set DST=docuSrcJava_vishiaBase
set DST_priv=docuSrcJava_vishiaBase_priv

set SRC=-subpackages org.vishia
set SRCPATH=..

echo set linkpath
set LINKPATH=
#export LINKPATH="$LINKPATH -link ..\docuSrcJava_Zbnf"


..\..\srcJava_vishiaBase\_make\+genjavadocbase.bat

