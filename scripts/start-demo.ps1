$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$springDir = Join-Path $root 'formulario_kit_amor\api-para-formulario'
$flaskDir = Join-Path $root 'reconhecimento_facial'
$outputsDir = Join-Path $root 'outputs'
$runtimePath = Join-Path $outputsDir 'demo-runtime.json'
$secretsPath = Join-Path $outputsDir 'demo-secrets.json'
$sshLog = Join-Path $outputsDir 'demo-ssh.log'
$sshErrorLog = Join-Path $outputsDir 'demo-ssh-error.log'
$springLog = Join-Path $outputsDir 'demo-spring.log'
$flaskLog = Join-Path $outputsDir 'demo-flask.log'
$sshKey = Join-Path $env:USERPROFILE '.ssh\amor_mariadb'
$sshUser = 'artur@20.88.10.66'

function Write-Step([string]$message) {
    Write-Host $message
}

function Test-TcpPort([int]$port) {
    $result = Test-NetConnection 127.0.0.1 -Port $port -WarningAction SilentlyContinue
    return [bool]$result.TcpTestSucceeded
}

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

function Get-LogTail([string]$path) {
    if (-not (Test-Path $path)) {
        return '(log indisponivel)'
    }

    $lines = @(Get-Content -Path $path -Tail 30 -ErrorAction SilentlyContinue)
    if ($lines.Count -eq 0) {
        return '(log vazio)'
    }

    $text = $lines -join [Environment]::NewLine
    foreach ($secretName in @('DB_PASSWORD', 'JWT_SECRET', 'PHI_ENCRYPTION_KEY')) {
        $secret = [Environment]::GetEnvironmentVariable($secretName, 'Process')
        if (-not [string]::IsNullOrWhiteSpace($secret)) {
            $text = $text.Replace($secret, '[REDACTED]')
        }
    }
    return $text
}

function Get-TrackedProcessIds([int]$rootPid) {
    $all = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    $ids = New-Object System.Collections.Generic.List[int]
    $pending = New-Object System.Collections.Generic.Queue[int]
    $pending.Enqueue($rootPid)

    while ($pending.Count -gt 0) {
        $parentId = $pending.Dequeue()
        foreach ($process in $all) {
            if ([int]$process.ParentProcessId -eq $parentId -and -not $ids.Contains([int]$process.ProcessId)) {
                $ids.Add([int]$process.ProcessId)
                $pending.Enqueue([int]$process.ProcessId)
            }
        }
    }
    return @($ids)
}

function Stop-TrackedPid([int]$processId) {
    if ($processId -le 0) {
        return
    }

    $childIds = @(Get-TrackedProcessIds $processId)
    [Array]::Reverse($childIds)
    foreach ($childId in $childIds) {
        Stop-Process -Id $childId -Force -ErrorAction SilentlyContinue
    }
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

function Read-Runtime {
    if (-not (Test-Path $runtimePath)) {
        return $null
    }
    try {
        return Get-Content -Raw -Path $runtimePath | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Read-OrCreateSecrets([string]$pythonPath) {
    if (Test-Path -LiteralPath $secretsPath) {
        try {
            $secrets = Get-Content -Raw -LiteralPath $secretsPath | ConvertFrom-Json
            $jwtBytes = [Convert]::FromBase64String($secrets.JWT_SECRET)
            $phiBytes = [Convert]::FromBase64String($secrets.PHI_ENCRYPTION_KEY)
            if ($jwtBytes.Length -lt 32 -or $phiBytes.Length -ne 32) {
                throw 'comprimentos invalidos'
            }
            return $secrets
        } catch {
            throw 'SPRING_START_FAILED: demo-secrets.json invalido ou corrompido.'
        }
    }

    $secrets = [ordered]@{
        JWT_SECRET = (& $pythonPath -c "import base64,secrets; print(base64.b64encode(secrets.token_bytes(32)).decode(), end='')")
        PHI_ENCRYPTION_KEY = (& $pythonPath -c "import base64,secrets; print(base64.b64encode(secrets.token_bytes(32)).decode(), end='')")
    }
    if ([string]::IsNullOrWhiteSpace($secrets.JWT_SECRET) -or [string]::IsNullOrWhiteSpace($secrets.PHI_ENCRYPTION_KEY)) {
        throw 'SPRING_START_FAILED: segredos temporarios nao puderam ser gerados.'
    }
    $secrets | ConvertTo-Json | Set-Content -LiteralPath $secretsPath -Encoding ASCII -NoNewline
    return [pscustomobject]$secrets
}

function Test-ExistingDemo {
    $runtime = Read-Runtime
    if ($null -eq $runtime) {
        return $false
    }

    $tunnelReady = Test-TcpPort 13306
    $springReady = (Get-HttpStatus 'http://127.0.0.1:8080/actuator/health') -eq 200
    $flaskReady = (Get-HttpStatus 'http://127.0.0.1:5000/lgpd/privacy-notice') -eq 200
    if ($tunnelReady -and $springReady -and $flaskReady) {
        Write-Host 'DEMO_JA_ESTA_ATIVA'
        Write-Host 'Spring: http://localhost:8080'
        Write-Host 'Flask:  http://localhost:5000'
        return $true
    }

    foreach ($property in @('FlaskPid', 'SpringPid', 'TunnelPid')) {
        if ($runtime.$property) {
            Stop-TrackedPid ([int]$runtime.$property)
        }
    }
    Remove-Item -LiteralPath $runtimePath -Force -ErrorAction SilentlyContinue
    return $false
}

function Invoke-Ssh([string[]]$arguments) {
    & ssh @arguments
    return $LASTEXITCODE
}

try {
    New-Item -ItemType Directory -Force -Path $outputsDir | Out-Null

    if (Test-ExistingDemo) {
        exit 0
    }

    if (-not (Get-Command ssh.exe -ErrorAction SilentlyContinue)) {
        throw 'SSH_KEY_NOT_FOUND: ssh.exe nao foi encontrado.'
    }
    if (-not (Test-Path $sshKey)) {
        throw 'SSH_KEY_NOT_FOUND: chave amor_mariadb nao encontrada.'
    }

    $sshCheckArgs = @('-i', $sshKey, '-o', 'BatchMode=yes', '-o', 'ConnectTimeout=10',
        '-o', 'StrictHostKeyChecking=no', $sshUser, 'exit')
    $sshCheckOutput = & ssh @sshCheckArgs 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw 'SSH_CONNECTION_FAILED: acesso BatchMode recusado ou indisponivel.'
    }

    $tunnelPid = 0
    $tunnelOwned = $false
    if (Test-TcpPort 13306) {
        Write-Step 'Tunel SSH existente detectado; validacao sera feita pelo banco.'
    } else {
        $tunnelArguments = @('-i', $sshKey, '-o', 'BatchMode=yes',
            '-o', 'StrictHostKeyChecking=no', '-N', '-L', '13306:127.0.0.1:3306', $sshUser)
        $tunnelProcess = Start-Process -FilePath 'ssh.exe' -ArgumentList $tunnelArguments `
            -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput $sshLog `
            -RedirectStandardError $sshErrorLog -PassThru
        $tunnelPid = $tunnelProcess.Id
        $tunnelOwned = $true

        $tunnelReady = $false
        for ($attempt = 0; $attempt -lt 15; $attempt++) {
            Start-Sleep -Seconds 1
            if (Test-TcpPort 13306) {
                $tunnelReady = $true
                break
            }
        }
        if (-not $tunnelReady) {
            Stop-TrackedPid $tunnelPid
            throw 'TUNNEL_FAILED: 127.0.0.1:13306 nao abriu.'
        }
    }

    $dbPasswordArgs = @('-i', $sshKey, '-o', 'BatchMode=yes',
        '-o', 'StrictHostKeyChecking=no', $sshUser,
        'sudo cat /opt/amor-mariadb/secrets/app_password')
    $env:DB_PASSWORD = ((& ssh @dbPasswordArgs 2>$null) | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        throw 'DATABASE_UNAVAILABLE: DB_PASSWORD nao foi recuperada.'
    }

    $env:DB_HOST = '127.0.0.1'
    $env:DB_PORT = '13306'
    $env:DB_NAME = 'amor_em_mechas'
    $env:DB_USER = 'amor_app'
    $env:DB_BACKEND = 'mysql'

    if (-not (Test-TcpPort 13306)) {
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        throw 'DATABASE_UNAVAILABLE: porta do banco indisponivel.'
    }

    $pythonPath = Join-Path $flaskDir 'venv\Scripts\python.exe'
    if (-not (Test-Path $pythonPath)) {
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        throw 'FLASK_VENV_NOT_FOUND: venv\Scripts\python.exe nao encontrado.'
    }
    $mvnwPath = Join-Path $springDir 'mvnw.cmd'
    if (-not (Test-Path $springDir)) {
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        throw 'SPRING_DIRECTORY_NOT_FOUND: diretorio Spring nao encontrado.'
    }
    if (-not (Test-Path $mvnwPath)) {
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        throw 'MAVEN_WRAPPER_NOT_FOUND: mvnw.cmd nao encontrado.'
    }

    $dbProbe = & $pythonPath -c "import os,pymysql; c=pymysql.connect(host=os.environ['DB_HOST'],port=int(os.environ['DB_PORT']),user=os.environ['DB_USER'],password=os.environ['DB_PASSWORD'],database=os.environ['DB_NAME'],connect_timeout=5); x=c.cursor(); x.execute('SELECT 1'); x.fetchone(); x.execute('SELECT VERSION()'); x.fetchone(); c.close()" 2>$null
    if ($LASTEXITCODE -ne 0) {
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        throw 'DATABASE_UNAVAILABLE: SELECT 1 nao foi confirmado.'
    }

    $demoSecrets = Read-OrCreateSecrets $pythonPath
    $env:JWT_SECRET = $demoSecrets.JWT_SECRET
    $env:PHI_ENCRYPTION_KEY = $demoSecrets.PHI_ENCRYPTION_KEY

    if (Test-TcpPort 8080) {
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        throw 'SPRING_START_FAILED: porta 8080 ja esta em uso.'
    }
    if (Test-TcpPort 5000) {
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        throw 'FLASK_START_FAILED: porta 5000 ja esta em uso.'
    }

    Remove-Item -LiteralPath $springLog -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $flaskLog -Force -ErrorAction SilentlyContinue

    $springCommand = '/d /c ""' + $mvnwPath + '" spring-boot:run >> "' + $springLog + '" 2>&1"'
    $springProcess = Start-Process -FilePath $env:ComSpec -ArgumentList $springCommand `
        -WorkingDirectory $springDir -WindowStyle Hidden -PassThru
    $springPid = $springProcess.Id

    $springReady = $false
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        Start-Sleep -Seconds 2
        if ((Get-HttpStatus 'http://127.0.0.1:8080/actuator/health') -eq 200) {
            $springReady = $true
            break
        }
    }
    if (-not $springReady) {
        Stop-TrackedPid $springPid
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        Write-Host 'SPRING_START_FAILED'
        Write-Host (Get-LogTail $springLog)
        exit 1
    }

    $flaskCommand = '/d /c ""' + $pythonPath + '" -m src.web_app >> "' + $flaskLog + '" 2>&1"'
    $flaskProcess = Start-Process -FilePath $env:ComSpec -ArgumentList $flaskCommand `
        -WorkingDirectory $flaskDir -WindowStyle Hidden -PassThru
    $flaskPid = $flaskProcess.Id

    $flaskReady = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        Start-Sleep -Seconds 2
        if ((Get-HttpStatus 'http://127.0.0.1:5000/lgpd/privacy-notice') -eq 200) {
            $flaskReady = $true
            break
        }
    }
    if (-not $flaskReady) {
        Stop-TrackedPid $flaskPid
        Stop-TrackedPid $springPid
        if ($tunnelOwned) { Stop-TrackedPid $tunnelPid }
        Write-Host 'FLASK_START_FAILED'
        Write-Host (Get-LogTail $flaskLog)
        exit 1
    }

    $runtime = [ordered]@{
        StartedAt = (Get-Date).ToUniversalTime().ToString('o')
        TunnelPid = if ($tunnelOwned) { $tunnelPid } else { $null }
        TunnelOwned = $tunnelOwned
        SpringPid = $springPid
        FlaskPid = $flaskPid
        SpringPort = 8080
        FlaskPort = 5000
        TunnelPort = 13306
    }
    $runtime | ConvertTo-Json | Set-Content -Path $runtimePath -Encoding ASCII

    Write-Host ''
    Write-Host '============================================='
    Write-Host ' AMOR EM MECHAS - DEMONSTRACAO PRONTA'
    Write-Host '============================================='
    Write-Host 'MariaDB Azure        OK'
    Write-Host 'Tunel SSH            OK  localhost:13306'
    Write-Host 'Spring Boot          OK  http://localhost:8080'
    Write-Host 'Flask / Face         OK  http://localhost:5000'
    Write-Host 'Spring Health        OK'
    Write-Host 'Flask Health         OK'
    Write-Host ''
    Write-Host 'Backend principal:'
    Write-Host 'http://localhost:8080'
    Write-Host ''
    Write-Host 'Backend facial:'
    Write-Host 'http://localhost:5000'
    Write-Host ''
    Write-Host '============================================='
    Write-Host 'Secrets: CONFIGURADO_EM_MEMORIA'
    exit 0
} catch {
    Write-Host $_.Exception.Message
    exit 1
} finally {
    foreach ($name in @('DB_PASSWORD', 'JWT_SECRET', 'PHI_ENCRYPTION_KEY')) {
        Remove-Item "Env:$name" -ErrorAction SilentlyContinue
    }
}
