param([switch]$ConfirmReset)
. "$PSScriptRoot\postgres.ps1"
if (-not $ConfirmReset) { throw 'This backs up and clears the configured SentinelX PostgreSQL schema. Re-run with -ConfirmReset only if intended.' }
Use-ProjectPostgres
$connection = [uri]$env:DATABASE_URL.Substring(5)
if ($connection.AbsolutePath -ne '/sentinelx' -or $connection.Query) { throw 'Reset is restricted to the dedicated sentinelx database without URL options.' }
& "$PSScriptRoot\stop-dev.ps1"
$bin = Resolve-PostgresBin
$backupDirectory=Join-Path $ProjectRoot 'backend\data\backups'
New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
$backup=Join-Path $backupDirectory ("sentinelx-" + (Get-Date -Format 'yyyyMMdd-HHmmss-ffff') + '.dump')
$previousPassword=$env:PGPASSWORD
try {
    $env:PGPASSWORD=$env:DATABASE_PASSWORD
    & "$bin\pg_dump.exe" -w -h $connection.Host -p $connection.Port -U $env:DATABASE_USER -d sentinelx -Fc -f $backup
    if ($LASTEXITCODE -ne 0) { throw 'Backup failed; database was not reset.' }
    & "$bin\psql.exe" -w -h $connection.Host -p $connection.Port -U $env:DATABASE_USER -d sentinelx -v ON_ERROR_STOP=1 -c 'BEGIN; DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION CURRENT_USER; COMMIT;'
    if ($LASTEXITCODE -ne 0) { throw 'Database reset failed.' }
} finally { $env:PGPASSWORD=$previousPassword }
Write-Host 'PostgreSQL schema reset after backup. Start development to apply migrations and seed demo accounts. Connection settings and older H2 files retained.'
