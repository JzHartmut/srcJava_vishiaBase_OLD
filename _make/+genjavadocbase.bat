@echo off
call +findJAVA_JDK.bat
echo genJavadoc: %DSTDIR%%DST%

echo JAVA_JDK=%JAVA_JDK%

::goto :zip
if exist %DSTDIR%%DST% rmdir /Q /S %DSTDIR%%DST% >NUL
if exist %DSTDIR%%DST%_priv rmdir /Q /S %DSTDIR%%DST%_priv >NUL

if not exist %DSTDIR%%DST% mkdir %DSTDIR%%DST%
if not exist %DSTDIR%%DST%_priv mkdir %DSTDIR%%DST%_priv

echo javadoc -d %DSTDIR%%DST% -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH%
%JAVA_JDK%\bin\javadoc -d %DSTDIR%%DST% -protected -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH% %SRC% 1>%DSTDIR%%DST%\javadoc.rpt 2>%DSTDIR%%DST%\javadoc.err
if errorlevel 1 goto :error
copy ..\..\srcJava_vishiaBase\_make\stylesheet_javadoc.css %DSTDIR%%DST%\stylesheet.css >NUL

echo javadoc -d %DSTDIR%%DST%_priv -private -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH%
%JAVA_JDK%\bin\javadoc -d %DSTDIR%%DST%_priv -private -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH% %SRC% 1>%DSTDIR%%DST%_priv\javadoc.rpt 2>%DSTDIR%%DST%_priv\javadoc.err
if errorlevel 1 goto :error
copy ..\..\srcJava_vishiaBase\_make\stylesheet_javadoc.css %DSTDIR%%DST%_priv\stylesheet.css >NUL

if "%NOPAUSE%"=="" pause

if not exist ..\img goto :noImg
  echo copy %DSTDIR%%DST%\img
	if not exist %DSTDIR%%DST%\img mkdir %DSTDIR%%DST%\img
	copy ..\img %DSTDIR%%DST% >NUL
	if not exist %DSTDIR%%DST%_priv\img mkdir %DSTDIR%%DST%_priv\img
	copy ..\img %DSTDIR%%DST%_priv >NUL
:noImg


:zip
if "%DSTDIR%" == "" goto :nozip
::pause
::echo on
set PWD1=%CD%
cd %DSTDIR%
if exist %DST%.zip del %DST%.zip >NUL
echo zip %CD%\%DST%.zip
pkzipc.exe -add -Directories %DST%.zip %DST%\* %DST%_priv\* >NUL
cd %PWD1%
echo off
if errorlevel 1 goto :error
:nozip

echo successfull generated %DSTDIR%%DST%
if "%NOPAUSE%"=="" pause
goto :ende

:error
pause
:ende



