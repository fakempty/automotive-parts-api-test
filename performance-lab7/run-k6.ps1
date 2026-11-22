param(
    [ValidateSet("baseline", "load", "stress", "all")]
    [string]$Scenario = "all",
    [string]$BaseUrl = "http://localhost:8080"
)

if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    Write-Error "k6 is not installed or not in PATH. Install via: winget install Grafana.k6"
    exit 1
}

$scenarios = if ($Scenario -eq "all") { @("baseline", "load", "stress") } else { @($Scenario) }

foreach ($s in $scenarios) {
    $outDir = Join-Path $PSScriptRoot "results\$s"
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $summaryFile = Join-Path $outDir "k6-summary.json"
    $rawFile = Join-Path $outDir "k6-raw.json"

    Write-Host "=== Running k6 [$s] against $BaseUrl ===" -ForegroundColor Cyan
    $env:BASE_URL = $BaseUrl
    $env:SCENARIO = $s

    k6 run --out "json=$rawFile" --summary-export="$summaryFile" (Join-Path $PSScriptRoot "k6\parts-load-test.js")
}
