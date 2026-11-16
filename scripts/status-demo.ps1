$ErrorActionPreference = 'SilentlyContinue'

function Get-HttpStatus([string]$url) {
    try {
        $response = Invoke-WebRequest -Uri $url -Method Get -UseBasicParsing -TimeoutSec 5
        return [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        return 0
    }
}

$root = Split-Path -Parent $PSScriptRoot
$runtimePath = Join-Path (Join-Path $root 'outputs') 'demo-runtime.json'
$tunnel = Test-NetConnection 127.0.0.1 -Port 13306 -WarningAction SilentlyContinue
$springStatus = Get-HttpStatus 'http://127.0.0.1:8080/actuator/health'
$flaskStatus = Get-HttpStatus 'http://127.0.0.1:5000/lgpd/privacy-notice'

Write-Host 'AMOR EM MECHAS - STATUS'
Write-Host ('Tunel:  ' + $(if ($tunnel.TcpTestSucceeded) { 'UP' } else { 'DOWN' }))
Write-Host ('Spring: ' + $(if ($springStatus -gt 0) { 'UP' } else { 'DOWN' }) + ' HTTP ' + $springStatus)
Write-Host ('Flask:  ' + $(if ($flaskStatus -gt 0) { 'UP' } else { 'DOWN' }) + ' HTTP ' + $flaskStatus)
Write-Host ('PID metadata: ' + $(if (Test-Path $runtimePath) { 'PRESENTE' } else { 'AUSENTE' }))
