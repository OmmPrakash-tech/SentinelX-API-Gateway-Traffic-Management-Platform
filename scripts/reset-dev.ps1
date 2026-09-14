param([switch]$ConfirmReset)
. "$PSScriptRoot\common.ps1"
if (-not $ConfirmReset) { throw 'This removes local demo database and reset mailbox. Re-run with -ConfirmReset only if intended.' }
& "$PSScriptRoot\stop-dev.ps1"
$target = [IO.Path]::GetFullPath((Join-Path $ProjectRoot 'backend\data'))
$expected = [IO.Path]::GetFullPath($ProjectRoot).TrimEnd('\') + '\backend\data'
if ($target -ne $expected) { throw 'Refusing unexpected deletion target.' }
if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
Write-Host 'Local demo data reset. Start development to seed new accounts.'
