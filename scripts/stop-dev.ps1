. "$PSScriptRoot\common.ps1"
$stateFile = Join-Path $ProjectRoot 'work\processes.json'
if (-not (Test-Path -LiteralPath $stateFile)) { Write-Host 'No tracked development processes.'; return }
$entries = @(Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json)
foreach ($entry in $entries) {
    $process = Get-Process -Id $entry.id -ErrorAction SilentlyContinue
    if ($process -and $process.StartTime.ToUniversalTime().Ticks -eq ([datetime]$entry.startTime).ToUniversalTime().Ticks) { Stop-Process -Id $entry.id; $process.WaitForExit(10000) | Out-Null }
}
Remove-Item -LiteralPath $stateFile
Write-Host 'Tracked SentinelX processes stopped. Data retained.'
