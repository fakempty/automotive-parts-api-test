param(
    [string]$HostName = "localhost",
    [string]$Port = "8080"
)

if (-not (Get-Command jmeter -ErrorAction SilentlyContinue)) {
    Write-Warning "JMeter CLI ('jmeter') is not in PATH. You can open performance-lab7\jmeter\parts-test-plan.jmx directly in Apache JMeter GUI."
    exit 0
}

$jmxPath = Join-Path $PSScriptRoot "jmeter\parts-test-plan.jmx"
$profiles = @(
    @{ Name = "baseline"; Threads = 2;  RampUp = 3;  Loops = 5 },
    @{ Name = "load";     Threads = 15; RampUp = 10; Loops = 15 },
    @{ Name = "stress";   Threads = 60; RampUp = 10; Loops = 20 }
)

foreach ($p in $profiles) {
    $outDir = Join-Path $PSScriptRoot "results\$($p.Name)\jmeter"
    if (Test-Path $outDir) { Remove-Item -Recurse -Force $outDir }
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null

    $jtlFile = Join-Path $outDir "results.jtl"
    $reportDir = Join-Path $outDir "html-report"

    Write-Host "=== Running JMeter [$($p.Name)] ===" -ForegroundColor Green
    jmeter -n -t $jmxPath `
        -Jhost=$HostName `
        -Jport=$Port `
        -Jthreads=$($p.Threads) `
        -Jrampup=$($p.RampUp) `
        -Jloops=$($p.Loops) `
        -l $jtlFile -e -o $reportDir
}
