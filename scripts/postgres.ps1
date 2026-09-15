. "$PSScriptRoot\common.ps1"
$PostgresSettings = Join-Path $ProjectRoot 'backend\data\postgresql.json'
function Resolve-PostgresBin {
    if ($env:POSTGRES_BIN -and (Test-Path -LiteralPath "$env:POSTGRES_BIN\psql.exe")) { return $env:POSTGRES_BIN }
    $installed = @(Get-ChildItem -LiteralPath 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending)
    foreach ($version in $installed) { if (Test-Path -LiteralPath "$($version.FullName)\bin\psql.exe") { return "$($version.FullName)\bin" } }
    throw 'Set POSTGRES_BIN to the installed PostgreSQL bin directory.'
}
function Use-ProjectPostgres {
    if (-not $env:DATABASE_URL) {
        if (-not (Test-Path -LiteralPath $PostgresSettings)) { throw 'Configure DATABASE_URL, DATABASE_USER and DATABASE_PASSWORD for a dedicated PostgreSQL database; see README.md.' }
        $settings = Get-Content -LiteralPath $PostgresSettings -Raw | ConvertFrom-Json
        $env:DATABASE_URL="jdbc:postgresql://$($settings.host):$($settings.port)/$($settings.database)"
        $env:DATABASE_USER=$settings.username
        $env:DATABASE_PASSWORD=$settings.password
    }
    if (-not $env:DATABASE_URL.StartsWith('jdbc:postgresql://') -or -not $env:DATABASE_USER -or -not $env:DATABASE_PASSWORD) { throw 'A PostgreSQL URL, username and password are required.' }
}
