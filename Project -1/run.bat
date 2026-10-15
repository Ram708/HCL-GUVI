@echo off
REM Compiles and runs the Student Management System.
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out -sourcepath src src\com\sms\Main.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
java -cp out com.sms.Main %*
