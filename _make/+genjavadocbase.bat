
if "%JAVA_JDK%"==""  then(
  if exist D:\Progs\JAVA\jdk1.6.0_21 set JAVA_JDK=D:\Progs\JAVA\jdk1.6.0_21
  if exist C:\Progs\JAVA\jdk1.6.0_21 set JAVA_JDK=C:\Progs\JAVA\jdk1.6.0_21
)
echo JAVA_JDK=JAVA_JDK

if exist %DST%\* del /F /Q /S %DST%\*
if exist %DST%_priv\* del /F /Q /S %DST%_priv\*


echo generate docu: $SRC

echo javadoc -d %DST% -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH%
%JAVA_JDK%\bin\javadoc -d %DST% -protected -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH% %SRC% 1>%DST%\javadoc.rpt 2>%DST%\javadoc.err

echo javadoc -d %DST%_priv -private -linksource -notimestamp $LINKPATH -sourcepath $SRCPATH
%JAVA_JDK%\bin\javadoc -d %DST%_priv -private -linksource -notimestamp %LINKPATH% -sourcepath %SRCPATH% %SRC% 1>%DST%_priv\javadoc.rpt 2>%DST%_priv\javadoc.err

if exist ..\img(
	mkdir %DST%\img
	copy ..\img %DST%
	mkdir %DST%_priv\img
	copy ..\img %DST%_priv
)


copy ..\..\srcJava_vishiaBase\_make\stylesheet_javadoc.css %DST%\stylesheet.css
copy ..\..\srcJava_vishiaBase\_make\stylesheet_javadoc.css %DST%_priv\stylesheet.css

echo successfull generated %DST%
