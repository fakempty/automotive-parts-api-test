param(
    [ValidateSet("smoke", "regression", "random", "stability", "single")]
    [string]$Mode = "regression",
    [string]$BaseUrl = "http://localhost:8080",
    [string]$SingleTest = "PartsRegressionSuiteTest#smokeCreateAndGetPartById"
)

$ErrorActionPreference = "Stop"

$javaCandidates = @(
    $env:JAVA_HOME,
    "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\jbr",
    "C:\Users\kolia\.jdks"
)

$validJavaHome = $null

foreach ($cand in $javaCandidates) {
    if ([string]::IsNullOrWhiteSpace($cand) -or -not (Test-Path $cand)) { continue }
    if (Test-Path (Join-Path $cand "bin\java.exe")) {
        $validJavaHome = (Resolve-Path $cand).Path
        break
    }
    $foundJdk = Get-ChildItem -Path $cand -Filter "java.exe" -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -like "*\bin\java.exe" } |
        Select-Object -First 1
    if ($foundJdk) {
        $validJavaHome = Split-Path (Split-Path $foundJdk.FullName -Parent) -Parent
        break
    }
}

if (-not $validJavaHome) {
    foreach ($root in @("C:\Program Files\Java", "C:\Program Files\Eclipse Adoptium", "C:\Program Files\JetBrains")) {
        if (-not (Test-Path $root)) { continue }
        $foundJdk = Get-ChildItem -Path $root -Filter "java.exe" -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -like "*\bin\java.exe" } |
            Select-Object -First 1
        if ($foundJdk) {
            $validJavaHome = Split-Path (Split-Path $foundJdk.FullName -Parent) -Parent
            break
        }
    }
}

if (-not $validJavaHome) {
    throw "Не знайдено валідний JDK (bin\java.exe). Вкажи шлях до JDK у змінній JAVA_HOME."
}

$env:JAVA_HOME = $validJavaHome
Write-Host "Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor DarkGray

$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -First 1
if (-not $mvnCmd) {
    $ideaMaven = Get-ChildItem -Path "C:\Program Files\JetBrains" -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName -First 1
    if ($ideaMaven) {
        $mvnCmd = $ideaMaven
    }
}

if (-not $mvnCmd) {
    throw "Maven (mvn) не знайдено в PATH або в C:\Program Files\JetBrains."
}

Write-Host "Using Maven: $mvnCmd" -ForegroundColor DarkGray

switch ($Mode) {
    "smoke" {
        Write-Host "=== Running SMOKE subset ===" -ForegroundColor Cyan
        & $mvnCmd test -Dgroups="smoke" "-Dsut.baseUrl=$BaseUrl"
    }
    "regression" {
        Write-Host "=== Running FULL REGRESSION suite (includes smoke) ===" -ForegroundColor Green
        & $mvnCmd test -Dgroups="regression" "-Dsut.baseUrl=$BaseUrl"
    }
    "random" {
        Write-Host "=== Running REGRESSION suite in RANDOM order ===" -ForegroundColor Yellow
        & $mvnCmd test -Dgroups="regression" "-Dsurefire.runOrder=random" "-Dsut.baseUrl=$BaseUrl"
    }
    "single" {
        Write-Host "=== Running SINGLE isolated test: $SingleTest ===" -ForegroundColor Magenta
        & $mvnCmd test "-Dtest=$SingleTest" "-Dsut.baseUrl=$BaseUrl"
    }
    "stability" {
        Write-Host "=== Running 5 consecutive REGRESSION runs to verify stability ===" -ForegroundColor Cyan
        for ($i = 1; $i -le 5; $i++) {
            Write-Host "--- Stability Run #$i / 5 ---" -ForegroundColor Cyan
            & $mvnCmd test -Dgroups="regression" "-Dsurefire.runOrder=random" "-Dsut.baseUrl=$BaseUrl"
            if ($LASTEXITCODE -ne 0) {
                throw "Stability check failed on run #$i"
            }
        }
        Write-Host "All 5 consecutive runs PASSED!" -ForegroundColor Green
    }
}

