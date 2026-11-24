@echo off
cd /d "%~dp0"
docker compose -f compose.json down
pause
