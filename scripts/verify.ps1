$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

java -version
mvn -version

docker info | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop debe estar iniciado para ejecutar las pruebas de integración con Testcontainers.' }

mvn -B clean verify
if ($LASTEXITCODE -ne 0) { throw 'Falló la verificación del proyecto con Maven.' }

Write-Host 'Se completó la ejecución de pruebas unitarias y de integración.'
Write-Host 'Reportes: */target/surefire-reports, */target/failsafe-reports, */target/site/jacoco'
