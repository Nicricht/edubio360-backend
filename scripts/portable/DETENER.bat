@echo off
cd /d "%~dp0"
if exist compose-monitoring.json (docker compose -f compose-monitoring.json down --remove-orphans) else (docker compose -f compose.json down)
pause
