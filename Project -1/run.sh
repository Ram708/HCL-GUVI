#!/usr/bin/env sh
# Compiles and runs the Student Management System (Git Bash / Linux / macOS).
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java") || exit 1
java -cp out com.sms.Main "$@"
