$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080"

Write-Host "1. Login estudiante"
$student = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" -ContentType "application/json" -Body (@{
    email = "student@edubio.local"
    password = "Student123!"
} | ConvertTo-Json)
$studentHeaders = @{ Authorization = "Bearer $($student.token)" }

Write-Host "2. Consultando ofertas"
$ofertas = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/ofertas" -Headers $studentHeaders
if (-not $ofertas -or $ofertas.Count -eq 0) { throw "No se encontraron ofertas" }
$ofertaId = $ofertas[0].id
Write-Host "   Oferta seleccionada: $ofertaId - $($ofertas[0].carrera)"

Write-Host "3. Creando solicitud"
$fecha = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
$solicitud = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/solicitudes" -Headers $studentHeaders -ContentType "application/json" -Body (@{
    ofertaId = $ofertaId
    motivo = "Quiero orientación para comparar alternativas académicas"
    fechaHora = $fecha
} | ConvertTo-Json)
Write-Host "   Solicitud creada: $($solicitud.id), estado $($solicitud.estado)"

Write-Host "4. Login orientador"
$orientador = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" -ContentType "application/json" -Body (@{
    email = "orientador@edubio.local"
    password = "Orientador123!"
} | ConvertTo-Json)
$orientadorHeaders = @{ Authorization = "Bearer $($orientador.token)" }

Write-Host "5. Confirmando solicitud"
$confirmada = Invoke-RestMethod -Method Put -Uri "$baseUrl/api/solicitudes/$($solicitud.id)/confirmar" -Headers $orientadorHeaders
Write-Host "   Estado final: $($confirmada.estado)"

Start-Sleep -Seconds 2
Write-Host "6. Revisando eventos consumidos por Notification Service"
$notificaciones = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/notificaciones/mias" -Headers $studentHeaders
$notificaciones | ConvertTo-Json -Depth 5

Write-Host "Flujo mínimo completado correctamente."
