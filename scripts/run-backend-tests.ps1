[CmdletBinding()]
param(
    [switch]$OpenCoverage
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host 'Running all SWT301 backend automated tests...' -ForegroundColor Cyan
& .\mvnw.cmd clean test
if ($LASTEXITCODE -ne 0) {
    throw "Backend tests failed with exit code $LASTEXITCODE"
}

$report = Join-Path $projectRoot 'target\site\jacoco\index.html'
Write-Host 'All backend automated tests passed.' -ForegroundColor Green
Write-Host "Surefire reports: $projectRoot\target\surefire-reports"
Write-Host "Coverage report:  $report"

if ($OpenCoverage -and (Test-Path $report)) {
    Start-Process $report
}
