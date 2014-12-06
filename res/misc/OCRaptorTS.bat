@ECHO ON
CHCP 65001

REM  ------------------------------------------------------------

SET SEARCH_STRING="Würzburg"
SET DATABASE_FOLDER="C:\Users\foo\Desktop\Dropbox\windows-db"
SET FOLDER_TO_INDEX="C:\Users\foo\Desktop\Dropbox\ocraptor-tmp"

call cmd /k OCRaptorCL ^
-d "%DATABASE_FOLDER%" ^
-i "%FOLDER_TO_INDEX%" ^
-l "%SEARCH_STRING%" ^
-p -r -s -g -u

REM  ------------------------------------------------------------
