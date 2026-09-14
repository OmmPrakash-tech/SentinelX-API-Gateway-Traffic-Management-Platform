$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath "$env:JAVA_HOME\bin\javac.exe")) { return $env:JAVA_HOME }
    $candidates = @(Get-ChildItem -LiteralPath "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue | Where-Object Name -Like '*25*' | Sort-Object Name -Descending)
    foreach ($candidate in $candidates) { if (Test-Path -LiteralPath "$($candidate.FullName)\bin\javac.exe") { return $candidate.FullName } }
    throw 'Set JAVA_HOME to an installed JDK 25 directory.'
}
function Use-Java {
    $env:JAVA_HOME = Resolve-JavaHome
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}
