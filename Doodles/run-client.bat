@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -d out -sourcepath src src\server\GameServer.java src\ui\WelcomeScreen.java || exit /b 1
java -cp "out;lib\*" ui.WelcomeScreen
pause
