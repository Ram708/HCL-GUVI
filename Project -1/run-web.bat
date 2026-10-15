@echo off
REM Compiles the project and starts the web UI at http://localhost:8080
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out -sourcepath src src\com\sms\Main.java src\com\sms\web\WebServer.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
start "" http://localhost:8080
java -cp out com.sms.web.WebServer %*
