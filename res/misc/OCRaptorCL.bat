@ECHO OFF

REM  ------------------------------------------------------------
REM  SET ENCODING TO UTF-8
REM  ------------------------------------------------------------

CHCP 65001
CLS

REM  ------------------------------------------------------------
REM  CHECK INSTALLED JAVA-VERSION
REM  ------------------------------------------------------------

SET JV="res\bins\portable-java\win-x86-64\bin\java"
%JV% -cp "res/bins/jre-version-checker.jar;lib/*" ^
CommandLineInterpreter "1.8" "res/cnfg/default.resources" -gui

if errorlevel 1 (
   exit /b %errorlevel%
)

REM  ------------------------------------------------------------
REM  START MAIN APPLICATION
REM  ------------------------------------------------------------

CLS
SET PARAMETER=%*

%JV% -Dfile.encoding=UTF-8 "-Xms128m" "-Xmx2048m" -cp ^
"res/bins/ocraptor.jar;lib/*" "mj.Main" %PARAMETER%

REM  ------------------------------------------------------------
REM  REMOVE TEMP FILES
REM  ------------------------------------------------------------


REM  IF "%PARAMETER%"=="%PARAMETER:-u=%" (
REM    ECHO '...'
REM    SET LIB="gsdll32.dll"
REM    if exist "%LIB%" del "%LIB%"

REM    SET LIB="gsdll64.dll"
REM    if exist "%LIB%" del "%LIB%"

REM    SET LIB="libtesseract302.dll"
REM    if exist "%LIB%" del "%LIB%"

REM    SET LIB="liblept168.dll"
REM    if exist "%LIB%" del "%LIB%"
REM  )

REM  ------------------------------------------------------------
