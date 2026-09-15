. "$PSScriptRoot\common.ps1"
Use-Java
Push-Location "$ProjectRoot\backend"
try { & .\mvnw.cmd test; if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed.' } } finally { Pop-Location }
& node --check "$ProjectRoot\frontend\app.js"
if ($LASTEXITCODE -ne 0) { throw 'Frontend syntax check failed.' }
& node --check "$ProjectRoot\services\server.mjs"
if ($LASTEXITCODE -ne 0) { throw 'Demo service syntax check failed.' }

& node --test "$ProjectRoot\tests\frontend\auth-redirect.test.mjs"
if ($LASTEXITCODE -ne 0) { throw 'Authentication redirect regression tests failed.' }
