[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$SkipOpenApi
)

$ErrorActionPreference = "Stop"
$passed = 0
$failed = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [int]$ExpectedStatus,
        [hashtable]$Headers = @{},
        [string]$Body,
        [string]$ContentType = "application/json"
    )

    try {
        $request = @{
            Uri = "$BaseUrl$Path"
            Method = $Method
            Headers = $Headers
            UseBasicParsing = $true
            ErrorAction = "Stop"
        }
        if ($null -ne $Body) {
            $request.Body = $Body
            $request.ContentType = $ContentType
        }

        $response = Invoke-WebRequest @request
        $status = [int]$response.StatusCode
    } catch {
        if ($null -ne $_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        } else {
            $status = 0
        }
    }

    if ($status -eq $ExpectedStatus) {
        $script:passed++
        Write-Host ("[PASS] {0} -> {1} {2}" -f $Name, $status, $Path) -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host ("[FAIL] {0} -> esperado {1}, recebido {2} {3}" -f $Name, $ExpectedStatus, $status, $Path) -ForegroundColor Red
    }
}

Write-Host "Verificando API em $BaseUrl" -ForegroundColor Cyan
Test-Endpoint -Name "Health publico" -Method GET -Path "/actuator/health" -ExpectedStatus 200
Test-Endpoint -Name "Swagger publico" -Method GET -Path "/swagger-ui.html" -ExpectedStatus 200
Test-Endpoint -Name "OpenAPI publico" -Method GET -Path "/v3/api-docs" -ExpectedStatus 200

$invalidLogin = '{"username":"usuario-inexistente","password":"senha-invalida"}'
Test-Endpoint -Name "Login invalido" -Method POST -Path "/auth/login" -ExpectedStatus 401 -Body $invalidLogin

$devTokenBody = @{ username = "smoke.test"; role = "ROLE_ATENDENTE" } | ConvertTo-Json
try {
    $tokenResponse = Invoke-RestMethod "$BaseUrl/auth/dev-token" -Method POST -ContentType "application/json" -Body $devTokenBody
    $token = $tokenResponse.accessToken
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "A resposta nao contem accessToken."
    }
    $passed++
    Write-Host "[PASS] Token de desenvolvimento gerado" -ForegroundColor Green
} catch {
    $failed++
    Write-Host "[FAIL] Token de desenvolvimento: $($_.Exception.Message)" -ForegroundColor Red
    $token = $null
}

$authHeaders = @{}
if ($token) {
    $authHeaders.Authorization = "Bearer $token"
}

Test-Endpoint -Name "Formulario publico sem token" -Method POST -Path "/formulario-solicitacao-peruca" -ExpectedStatus 400 -Body "{}"
Test-Endpoint -Name "Pacientes sem token" -Method GET -Path "/pacientes" -ExpectedStatus 401
Test-Endpoint -Name "Pacientes com token" -Method GET -Path "/pacientes" -ExpectedStatus 200 -Headers $authHeaders
Test-Endpoint -Name "Dados medicos sem token" -Method GET -Path "/dados-medicos" -ExpectedStatus 401
Test-Endpoint -Name "Dados medicos ATENDENTE" -Method GET -Path "/dados-medicos" -ExpectedStatus 403 -Headers $authHeaders
Test-Endpoint -Name "Kits com token" -Method GET -Path "/kits" -ExpectedStatus 200 -Headers $authHeaders
Test-Endpoint -Name "Enderecos com token" -Method GET -Path "/enderecos" -ExpectedStatus 200 -Headers $authHeaders
Test-Endpoint -Name "Arquivos com token" -Method GET -Path "/arquivos" -ExpectedStatus 200 -Headers $authHeaders
Test-Endpoint -Name "Solicitantes com token" -Method GET -Path "/solicitantes" -ExpectedStatus 200 -Headers $authHeaders
Test-Endpoint -Name "Filhos com token" -Method GET -Path "/filhos" -ExpectedStatus 200 -Headers $authHeaders
Test-Endpoint -Name "Madrinhas com token" -Method GET -Path "/madrinhas" -ExpectedStatus 200 -Headers $authHeaders

if (-not $SkipOpenApi) {
    try {
        $openApi = Invoke-RestMethod "$BaseUrl/v3/api-docs"
        Write-Host "`nEndpoints publicados no OpenAPI:" -ForegroundColor Cyan
        $openApi.paths.PSObject.Properties.Name | Sort-Object | ForEach-Object {
            Write-Host ("  {0}" -f $_)
        }
    } catch {
        Write-Host "[WARN] Nao foi possivel ler os paths do OpenAPI: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host "`nResumo: $passed passaram; $failed falharam." -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
if ($failed -gt 0) {
    exit 1
}
exit 0
