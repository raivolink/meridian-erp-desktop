@echo off
rem Runs the recordable robot demo. Arguments are passed through, e.g. demo.bat --speed 0.7 --start-delay 10
setlocal
cd /d "%~dp0"

if not exist dist\MeridianErp.jar call build.bat || exit /b 1

if exist .venv\Scripts\python.exe goto run
echo Creating .venv with the Java Access Bridge wrapper ...
rem "call" because python may be a .bat shim (pyenv-win), which would not return otherwise.
call python -m venv .venv || exit /b 1
.venv\Scripts\python.exe -m pip install -q --disable-pip-version-check -r demo\requirements.txt || exit /b 1

:run
.venv\Scripts\python.exe -u demo\meridian_demo.py %*
