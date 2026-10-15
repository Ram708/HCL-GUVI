#!/usr/bin/env sh
# Compiles the project and starts the web UI at http://localhost:8080
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -encoding UTF-8 -d out -sourcepath src src/com/sms/Main.java src/com/sms/web/WebServer.java || exit 1
java -cp out com.sms.web.WebServer "$@"
