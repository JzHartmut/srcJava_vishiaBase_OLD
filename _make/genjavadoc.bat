@echo off
set DSTDIR=D:\vishia\Java\
if exist %DSTDIR% goto :okdstdir
set DSTDIR=..\..\
:okdstdir
echo %DSTDIR%
set DST=docuSrcJava_vishiaBase
set DST_priv=docuSrcJavaPriv_vishiaBase

set SRC=-subpackages org.vishia
set SRCPATH=..
set CLASSPATH=xxxxx
set LINKPATH=


..\..\srcJava_vishiaBase\_make\+genjavadocbase.bat

