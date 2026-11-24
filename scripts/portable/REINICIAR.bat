@echo off
cd /d "%~dp0"
docker compose -f compose.json down
if errorlevel 1 exit /b 1
call INICIAR.bat
