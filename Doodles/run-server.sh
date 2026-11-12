#!/bin/bash
# Starts the Doodles SERVER (start this first). Compiles the project, then starts it. The MySQL driver is taken from ./lib
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -d out -sourcepath src src/server/GameServer.java src/ui/WelcomeScreen.java || exit 1
java -cp "out:lib/*" server.GameServer
