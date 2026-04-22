@ECHO OFF
SETLOCAL

SET DIRNAME=%~dp0
IF "%DIRNAME%"=="" SET DIRNAME=.
SET APP_HOME=%DIRNAME%

SET CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

"%JAVA_HOME%\bin\java.exe" -version >NUL 2>&1
IF %ERRORLEVEL% EQU 0 (
  "%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
  EXIT /B %ERRORLEVEL%
)

java -version >NUL 2>&1
IF %ERRORLEVEL% EQU 0 (
  java %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
  EXIT /B %ERRORLEVEL%
)

ECHO ERROR: JAVA_HOME is not set and java was not found on PATH.
EXIT /B 1
