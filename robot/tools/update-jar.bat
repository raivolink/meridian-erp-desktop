@echo off
rem Compiles Meridian ERP desktop from ..\app\src into meridian\MeridianErp.jar.
rem Needs a JDK 9+ on PATH (or run it with: rcc task script -- tools\update-jar.bat).
setlocal
cd /d "%~dp0.."
set CLASSES=%TEMP%\meridian-erp-java-classes

if exist "%CLASSES%" rmdir /s /q "%CLASSES%"
javac --release 8 -Xlint:-options -encoding UTF-8 -d "%CLASSES%" -sourcepath ..\app\src ..\app\src\meridian\MeridianApp.java || exit /b 1
jar cfe meridian\MeridianErp.jar meridian.MeridianApp -C "%CLASSES%" . || exit /b 1
rmdir /s /q "%CLASSES%"
echo Updated meridian\MeridianErp.jar
