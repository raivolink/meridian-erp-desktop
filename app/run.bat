@echo off
rem Starts Meridian ERP. Arguments are passed through, e.g. run.bat --hard --suffix=Test1
setlocal
cd /d "%~dp0"

if not exist dist\MeridianErp.jar call build.bat || exit /b 1

java -jar dist\MeridianErp.jar %*
