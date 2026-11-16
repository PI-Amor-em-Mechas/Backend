$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$outputsDir = Join-Path $root 'outputs'
$runtimePath = Join-Path $outputsDir 'demo-runtime.json'

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
    if ($processId -le 0) { return }
    $childIds = @(Get-TrackedProcessIds $processId)
    [Array]::Reverse($childIds)
    foreach ($childId in $childIds) {
        Stop-Process -Id $childId -Force -ErrorAction SilentlyContinue
    }
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

if (-not (Test-Path $runtimePath)) {
    Write-Host 'DEMO_NAO_ESTA_ATIVA'
    exit 0
}

try {
    $runtime = Get-Content -Raw -Path $runtimePath | ConvertFrom-Json
    foreach ($property in @('FlaskPid', 'SpringPid')) {
        if ($runtime.$property) {
            Stop-TrackedPid ([int]$runtime.$property)
        }
    }
    if ($runtime.TunnelOwned -and $runtime.TunnelPid) {
        Stop-TrackedPid ([int]$runtime.TunnelPid)
    }
    Remove-Item -LiteralPath $runtimePath -Force
    Write-Host 'DEMO_PARADA'
} catch {
    Write-Host ('STOP_FAILED: ' + $_.Exception.Message)
    exit 1
}
