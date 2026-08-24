[CmdletBinding()]
param(
    [int]$Port = 8080,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java nao foi encontrado no PATH. Instale o JDK 21 ou superior."
}

$javaVersion = (java -version 2>&1 | Select-Object -First 1)
Write-Host "Java detectado: $javaVersion" -ForegroundColor DarkGray

if (-not (Test-Path ".\mvnw.cmd")) {
    throw "mvnw.cmd nao foi encontrado em $projectRoot."
}

if (-not $SkipTests) {
    Write-Host "Executando testes antes de subir a API..." -ForegroundColor Cyan
    & .\mvnw.cmd test
    if ($LASTEXITCODE -ne 0) {
        throw "Os testes falharam. A API nao sera iniciada."
    }
}

Write-Host "Subindo API com perfil dev (H2 em memoria) na porta $Port..." -ForegroundColor Green
Write-Host "Swagger: http://localhost:$Port/swagger-ui.html" -ForegroundColor Yellow
Write-Host "OpenAPI: http://localhost:$Port/v3/api-docs" -ForegroundColor Yellow
Write-Host "Health:  http://localhost:$Port/actuator/health" -ForegroundColor Yellow

& .\mvnw.cmd spring-boot:run `
    "-Dspring-boot.run.profiles=dev" `
    "-Dspring-boot.run.arguments=--server.port=$Port"

exit $LASTEXITCODE
