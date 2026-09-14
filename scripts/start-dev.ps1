param([switch]$SkipBuild)
. "$PSScriptRoot\common.ps1"
Use-Java
$stateDir = Join-Path $ProjectRoot 'work'
New-Item -ItemType Directory -Path $stateDir -Force | Out-Null
$stateFile = Join-Path $stateDir 'processes.json'
if (Test-Path -LiteralPath $stateFile) { throw 'Development process state already exists. Run stop-dev.ps1 first.' }
foreach ($requiredPort in @(8080,9101,9102,9103,9104)) {
    $probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $requiredPort)
    try { $probe.Start() } catch { throw "Port $requiredPort is already in use. Stop the other stack before starting native development." } finally { $probe.Stop() }
}
if (-not $SkipBuild) {
    Push-Location "$ProjectRoot\backend"
    try { & .\mvnw.cmd package -DskipTests; if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' } } finally { Pop-Location }
}
$controlFile = Join-Path $stateDir 'control-token.txt'
if (-not (Test-Path -LiteralPath $controlFile)) { [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)) | Set-Content -LiteralPath $controlFile }
$env:DEMO_CONTROL_TOKEN = (Get-Content -LiteralPath $controlFile -Raw).Trim()
$env:NODE_ENV = 'development'
$started = @()
try {
    $node = (Get-Command node.exe).Source
    foreach ($spec in @(@('user',9101),@('product',9102),@('order',9103),@('user',9104))) {
        $port = $spec[1]
        $process = Start-Process -FilePath $node -ArgumentList @('services/server.mjs',$spec[0],$port) -WorkingDirectory $ProjectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput "$stateDir\service-$port.log" -RedirectStandardError "$stateDir\service-$port.error.log"
        $started += @{id=$process.Id;name="service-$port";startTime=$process.StartTime.ToUniversalTime().ToString('o')}
    }
    $process = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" -ArgumentList @('-jar','target/backend-0.0.1-SNAPSHOT.jar','--spring.profiles.active=dev','--server.port=8080') -WorkingDirectory "$ProjectRoot\backend" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$stateDir\gateway.log" -RedirectStandardError "$stateDir\gateway.error.log"
    $started += @{id=$process.Id;name='gateway';startTime=$process.StartTime.ToUniversalTime().ToString('o')}
    $started | ConvertTo-Json | Set-Content -LiteralPath $stateFile
    $ready = $false
    for ($attempt=0; $attempt -lt 60; $attempt++) {
        Start-Sleep -Seconds 1
        if ($process.HasExited) { throw 'Gateway exited. Inspect work/gateway.log.' }
        try { $response = Invoke-RestMethod 'http://127.0.0.1:8080/health' -NoProxy -TimeoutSec 2; if ($response.status -eq 'UP') { $ready=$true; break } } catch { $readinessError = $_.Exception.Message }
    }
    if (-not $ready) { throw "Gateway did not become ready within 60 seconds. Last check: $readinessError. Inspect work/gateway.log." }
    Write-Host 'SentinelX is running at http://localhost:8080'
    Write-Host 'Demo credentials: docs/development-credentials.md'
} catch {
    $started | ConvertTo-Json | Set-Content -LiteralPath $stateFile
    & "$PSScriptRoot\stop-dev.ps1"
    throw
} finally { Remove-Item Env:DEMO_CONTROL_TOKEN -ErrorAction SilentlyContinue }
