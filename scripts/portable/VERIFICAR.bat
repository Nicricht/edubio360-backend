@echo off
setlocal
cd /d "%~dp0"
docker compose -f compose.json ps
if errorlevel 1 exit /b 1
powershell -NoProfile -Command "$ErrorActionPreference='Stop'; $ports=8761,8081,8082,8083,8084,8085,8086,8080; foreach($port in $ports){$r=Invoke-RestMethod ('http://127.0.0.1:'+ $port +'/actuator/health') -TimeoutSec 10; if($r.status -ne 'UP'){throw ('Servicio no disponible: '+$port)}; Write-Host ('OK '+$port)}; $ready=$false; for($i=0;$i -lt 36;$i++){try {$r=Invoke-RestMethod 'http://127.0.0.1:8088/api/ofertas' -TimeoutSec 10; if(@($r).Count -gt 0){$ready=$true; break}}catch{}; Start-Sleep -Seconds 5}; if(-not $ready){throw 'Catalogo no disponible'}; $r=Invoke-WebRequest 'http://127.0.0.1:8088/healthz' -UseBasicParsing -TimeoutSec 10; if($r.StatusCode -ne 200){throw 'Frontend no disponible'}; Write-Host 'OK frontend, Gateway y catalogo'"
set "RESULT=%ERRORLEVEL%"
if /i not "%~1"=="--quiet" pause
exit /b %RESULT%
