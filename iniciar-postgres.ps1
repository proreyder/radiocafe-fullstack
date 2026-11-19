$ErrorActionPreference = 'Stop'
$projectDirectory = $PSScriptRoot
$settingsFile = Join-Path $projectDirectory '.env'
if (-not (Test-Path -LiteralPath $settingsFile)) {
    throw 'Falta .env. Consulta el README para configurar DB_PASSWORD y DB_PORT.'
}
$settings = @{}
Get-Content -LiteralPath $settingsFile | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') { $settings[$matches[1]] = $matches[2].Trim() }
}
if (-not $settings['DB_PASSWORD']) { throw 'Falta DB_PASSWORD en .env.' }
$databasePort = if ($settings['DB_PORT']) { $settings['DB_PORT'] } else { '5432' }
if ($databasePort -notmatch '^\d{1,5}$') { throw 'DB_PORT debe ser un número de puerto.' }
$previousEnvironment = @{}
foreach ($variable in @('SPRING_PROFILES_ACTIVE','DB_URL','DB_USER','DB_PASSWORD','DEMO_ENABLED','SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','SPRING_JPA_HIBERNATE_DDL_AUTO')) {
    $previousEnvironment[$variable] = [Environment]::GetEnvironmentVariable($variable, 'Process')
}
Push-Location -LiteralPath $projectDirectory
try {
    docker compose up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar PostgreSQL en Docker.' }
    $env:SPRING_PROFILES_ACTIVE = 'postgres'
    $env:DB_URL = "jdbc:postgresql://127.0.0.1:$databasePort/radiocafe"
    $env:DB_USER = 'radiocafe'
    $env:DB_PASSWORD = $settings['DB_PASSWORD']
    $env:DEMO_ENABLED = 'true'
    $env:SPRING_DATASOURCE_URL = $env:DB_URL
    $env:SPRING_DATASOURCE_USERNAME = $env:DB_USER
    $env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
    $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'update'
    Set-Location -LiteralPath (Join-Path $projectDirectory 'radiocafe-backend')
    .\mvnw.cmd spring-boot:run
} finally {
    foreach ($variable in $previousEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($variable, $previousEnvironment[$variable], 'Process')
    }
    Pop-Location
}
