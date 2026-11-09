@echo off
rem Builds dist\MeridianErp.jar. Needs a JDK 9+ on PATH; the jar runs on Java 8+.
setlocal
cd /d "%~dp0"

if exist out rmdir /s /q out
if not exist dist mkdir dist

javac --release 8 -Xlint:-options -encoding UTF-8 -d out -sourcepath src src\meridian\MeridianApp.java
if errorlevel 1 exit /b 1

jar cfe dist\MeridianErp.jar meridian.MeridianApp -C out .
if errorlevel 1 exit /b 1

echo Built dist\MeridianErp.jar
