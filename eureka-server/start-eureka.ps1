# PowerShell script to start Eureka Server

Write-Host "🚀 Starting Eureka Server..." -ForegroundColor Cyan
Write-Host ""

# Check if Java is available
$javaVersion = $null
try {
    $javaOutput = java -version 2>&1
    $javaVersion = ($javaOutput | Select-String -Pattern 'version "(\d+)' | ForEach-Object { $_.Matches.Groups[1].Value })
} catch {
    Write-Host "❌ Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Java 21 or higher" -ForegroundColor Yellow
    exit 1
}

if ([int]$javaVersion -lt 21) {
    Write-Host "⚠️  Warning: Java version is $javaVersion, but Java 21 or higher is recommended" -ForegroundColor Yellow
}

# Navigate to eureka-server directory
Set-Location $PSScriptRoot

# Check if Maven wrapper exists, otherwise use system Maven
$mavenCmd = $null
if (Test-Path "../mvnw.cmd") {
    Write-Host "Using Maven wrapper..." -ForegroundColor Green
    $mavenCmd = "../mvnw.cmd"
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "Using system Maven..." -ForegroundColor Green
    $mavenCmd = "mvn"
} else {
    Write-Host "❌ Maven is not installed and mvnw not found" -ForegroundColor Red
    Write-Host "Please install Maven or run from project root" -ForegroundColor Yellow
    exit 1
}

# Build if target doesn't exist
if (-not (Test-Path "target")) {
    Write-Host "Building Eureka Server..." -ForegroundColor Yellow
    & $mavenCmd clean package -DskipTests
}

# Start Eureka Server
Write-Host ""
Write-Host "✅ Starting Eureka Server on port 8761..." -ForegroundColor Green
Write-Host "📊 Dashboard: http://localhost:8761" -ForegroundColor Cyan
Write-Host "💚 Health: http://localhost:8761/actuator/health" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

& $mavenCmd spring-boot:run
