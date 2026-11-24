@echo off
setlocal
cd /d "%~dp0"
docker info --format "{{.OSType}}" 2>nul | findstr /x linux >nul
if errorlevel 1 goto error
powershell -NoProfile -Command "$ErrorActionPreference='Stop'; foreach($pair in @(@('SHA256.txt','edubio360.tar.gz'),@('SHA256-monitoring.txt','monitoring.tar.gz'))){$expected=(Get-Content ('imagenes-docker/'+$pair[0])).Split(' ')[0];if((Get-FileHash ('imagenes-docker/'+$pair[1]) -Algorithm SHA256).Hash -ne $expected){throw 'Hash incorrecto'}}"
if errorlevel 1 goto error
docker load -i imagenes-docker\edubio360.tar.gz
if errorlevel 1 goto error
docker load -i imagenes-docker\monitoring.tar.gz
if errorlevel 1 goto error
powershell -NoProfile -Command "$ErrorActionPreference='Stop';$sw=[Diagnostics.Stopwatch]::StartNew(); docker compose -f compose-monitoring.json up -d --no-build --pull never --wait --wait-timeout 900; if($LASTEXITCODE -ne 0){exit 1}; $sw.Stop(); $data=(Get-Content ci-metrics.prom -Raw)+[Environment]::NewLine+'edubio_ci_deploy_duration_seconds '+[int]$sw.Elapsed.TotalSeconds+[Environment]::NewLine; $data | docker compose -f compose-monitoring.json exec -T api-gateway curl -fsS --retry 10 --retry-all-errors -X PUT --data-binary '@-' http://pushgateway:9091/metrics/job/edubio_ci; exit $LASTEXITCODE"
if errorlevel 1 goto error
call VERIFICAR.bat --quiet
if errorlevel 1 goto error
echo Grafana: admin / edubio-local-demo
start "" http://localhost:3000
start "" http://localhost:8088
pause
exit /b 0
:error
echo No se pudo iniciar el modo monitoreo. El modo basico permanece disponible.
docker compose -f compose-monitoring.json ps
pause
exit /b 1
