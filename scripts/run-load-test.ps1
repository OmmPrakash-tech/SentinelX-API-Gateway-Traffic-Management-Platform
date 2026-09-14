param([switch]$Unthrottled)
. "$PSScriptRoot\common.ps1"
Push-Location $ProjectRoot
try {
    if ($Unthrottled) { & node tests/load/run.mjs --unthrottled } else { & node tests/load/run.mjs }
    if ($LASTEXITCODE -ne 0) { throw 'Load test failed. Review output.' }
} finally { Pop-Location }
