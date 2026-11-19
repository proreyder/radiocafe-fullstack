$ErrorActionPreference = 'Stop'
$projectDirectory = $PSScriptRoot
$settings = @{}
Get-Content -LiteralPath (Join-Path $projectDirectory '.env') | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') { $settings[$matches[1]] = $matches[2].Trim() }
}
if (-not $settings['DB_PASSWORD']) { throw 'Falta DB_PASSWORD en .env.' }
$databasePort = if ($settings['DB_PORT']) { $settings['DB_PORT'] } else { '5432' }
if ($databasePort -notmatch '^\d{1,5}$') { throw 'DB_PORT debe ser numérico.' }
$previousEnvironment = @{}
foreach ($variable in @('SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','SPRING_JPA_HIBERNATE_DDL_AUTO')) {
    $previousEnvironment[$variable] = [Environment]::GetEnvironmentVariable($variable, 'Process')
}
Push-Location -LiteralPath $projectDirectory
try {
    docker compose up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar PostgreSQL.' }
    $exists = docker compose exec -T postgres psql -U radiocafe -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='radiocafe_test'"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar PostgreSQL.' }
    if ($exists -ne '1') {
        docker compose exec -T postgres createdb -U radiocafe radiocafe_test
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base de pruebas.' }
    }
    # Only radiocafe_test is disposable; never run these schema tests against radiocafe.
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:$databasePort/radiocafe_test"
    $env:SPRING_DATASOURCE_USERNAME = 'radiocafe'
    $env:SPRING_DATASOURCE_PASSWORD = $settings['DB_PASSWORD']
    $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'create-drop'
    Set-Location -LiteralPath (Join-Path $projectDirectory 'radiocafe-backend')
    .\mvnw.cmd verify -q
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas en PostgreSQL.' }
} finally {
    foreach ($variable in $previousEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($variable, $previousEnvironment[$variable], 'Process')
    }
    Pop-Location
}
