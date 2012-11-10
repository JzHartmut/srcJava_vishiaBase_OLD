
if "%JAVA_JDK%"==""  then(
  if exist D:\Progs\JAVA\jdk1.6.0_21 set JAVA_JDK=D:\Progs\JAVA\jdk1.6.0_21
  if exist C:\Progs\JAVA\jdk1.6.0_21 set JAVA_JDK=C:\Progs\JAVA\jdk1.6.0_21
)
echo JAVA_JDK=JAVA_JDK

::goto :zip
if exist %DST%\* del /F /Q /S %DST%\*
if exist %DSTDIR%%DST%_priv\* del /F /Q /S %DSTDIR%%DST%_priv\*

if not exist %DSTDIR%%DST% mkdir %DSTDIR%%DST%
if not exist %DSTDIR%%DST%_priv mkdir %DSTDIR%%DST%_priv

echo generate docu: $SRC

echo javadoc -d %DSTDIR%%DST% -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH%
%JAVA_JDK%\bin\javadoc -d %DSTDIR%%DST% -protected -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH% %SRC% 1>%DST%\javadoc.rpt 2>%DST%\javadoc.err
if errorlevel 1 goto :error

echo javadoc -d %DSTDIR%%DST%_priv -private -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH
%JAVA_JDK%\bin\javadoc -d %DSTDIR%%DST%_priv -private -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH% %SRC% 1>%DST%_priv\javadoc.rpt 2>%DST%_priv\javadoc.err
if errorlevel 1 goto :error

if not exist ..\img goto :noImg
	mkdir %DST%\img
	copy ..\img %DSTDIR%%DST%
	mkdir %DSTDIR%%DST%_priv\img
	copy ..\img %DSTDIR%%DST%_priv
:noImg


copy ..\..\srcJava_vishiaBase\_make\stylesheet_javadoc.css %DST%\stylesheet.css
copy ..\..\srcJava_vishiaBase\_make\stylesheet_javadoc.css %DSTDIR%%DST%_priv\stylesheet.css

:zip
if "%DSTDIR%" == "" goto :nozip
echo %DSTDIR%%DST%
::pause
set PWD1=%CD%
cd %DSTDIR%
if exist %DST%.zip del %DST%.zip
pkzipc.exe -add -Directories %DST%.zip %DST%\* %DST%_priv\*
cd %PWD1%
if errorlevel 1 goto :error
:nozip

echo successfull generated %DSTDIR%%DST%
goto :ende

:error
pause
:ende



