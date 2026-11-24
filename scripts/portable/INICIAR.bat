@echo off
setlocal
cd /d "%~dp0"
docker info --format "{{.OSType}}" 2>nul | findstr /x linux >nul
if errorlevel 1 (
  echo Inicia Docker Desktop en modo contenedores Linux.
  pause
  exit /b 1
)
docker compose version >nul 2>&1
if errorlevel 1 goto error
set "NEED_LOAD="
for /f "delims=" %%I in ('docker compose -f compose.json config --images') do docker image inspect "%%I" >nul 2>&1 || set "NEED_LOAD=1"
if defined NEED_LOAD (
  echo Verificando integridad de las imagenes...
  powershell -NoProfile -Command "$expected=(Get-Content 'imagenes-docker/SHA256.txt').Split(' ')[0]; if ((Get-FileHash 'imagenes-docker/edubio360.tar.gz' -Algorithm SHA256).Hash -ne $expected) {exit 1}"
  if errorlevel 1 goto error
  docker load -i imagenes-docker\edubio360.tar.gz
  if errorlevel 1 goto error
)
docker compose -f compose.json up -d --no-build --pull never --wait --wait-timeout 900
if errorlevel 1 goto error
call VERIFICAR.bat --quiet
if errorlevel 1 goto error
start "" http://localhost:8088
echo EduBio360 listo.
pause
exit /b 0
:error
echo No se pudo iniciar EduBio360. Revisa los mensajes y ejecuta VERIFICAR.bat.
docker compose -f compose.json ps
pause
exit /b 1
