REM  The java-copiler may be located at a user-specified position.
REM  Set the environment variable JAVA_JDK, where bin/javac will be found.
set JAVA_JDK=D:\Progs\JAVA\jdk1.6.0_21
if exist "%JAVA_JDK%" goto :JavaOk
set JAVA_JDK=D:\Programme\JAVA\jdk1.6.0_21
if exist "%JAVA_JDK%" goto :JavaOk
set JAVA_JDK=C:\Progs\JAVA\jdk1.6.0_21
if exist "%JAVA_JDK%" goto :JavaOk
set JAVA_JDK=D:\Progs\JAVA\jdk1.6.0_21
if exist "%JAVA_JDK%" goto :JavaOk
set JAVA_JDK=D:\Progs\JAVA\jdk1.6.0_21
if exist "%JAVA_JDK%" goto :JavaOk
set JAVA_JDK=D:\Progs\JAVA\jdk1.6.0_21
if exist "%JAVA_JDK%" goto :JavaOk
echo JAVA_JDK never found, please check content of srcJava_vishiaBase/+findJAVA_JDK.bat
pause
exit
:JavaOk
exit /B