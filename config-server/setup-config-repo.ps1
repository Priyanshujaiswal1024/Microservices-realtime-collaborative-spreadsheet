# PowerShell script to initialize Git-backed configuration repository for Config Server

$CONFIG_REPO_DIR = "$env:USERPROFILE\collab-spreadsheet-config"

Write-Host "🔧 Setting up Config Server Git repository..." -ForegroundColor Cyan
Write-Host ""

# Check if directory already exists
if (Test-Path $CONFIG_REPO_DIR) {
    Write-Host "⚠️  Directory $CONFIG_REPO_DIR already exists." -ForegroundColor Yellow
    $confirm = Read-Host "Do you want to recreate it? (yes/no)"
    if ($confirm -eq "yes") {
        Remove-Item -Recurse -Force $CONFIG_REPO_DIR
        Write-Host "Removed existing directory." -ForegroundColor Green
    } else {
        Write-Host "Keeping existing directory. Exiting." -ForegroundColor Yellow
        exit 0
    }
}

# Create directory
New-Item -ItemType Directory -Path $CONFIG_REPO_DIR -Force | Out-Null
Set-Location $CONFIG_REPO_DIR

# Initialize Git repository
git init
Write-Host "✅ Initialized Git repository at $CONFIG_REPO_DIR" -ForegroundColor Green

# Copy configuration files from config-server resources
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$CONFIG_SOURCE_DIR = Join-Path $SCRIPT_DIR "src\main\resources\config"

if (Test-Path $CONFIG_SOURCE_DIR) {
    Copy-Item "$CONFIG_SOURCE_DIR\*.yml" -Destination $CONFIG_REPO_DIR
    Write-Host "✅ Copied configuration files" -ForegroundColor Green
} else {
    Write-Host "⚠️  Warning: Config source directory not found at $CONFIG_SOURCE_DIR" -ForegroundColor Yellow
    Write-Host "Creating sample configuration files..." -ForegroundColor Yellow
    
    # Create minimal application.yml
    @"
# Global configuration for all services
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: UTC

logging:
  level:
    com.collab.spreadsheet: INFO
"@ | Out-File -FilePath "application.yml" -Encoding UTF8
}

# Create .gitignore
@"
# Ignore sensitive files
*-secrets.yml
*-local.yml
*.key
*.pem

# OS files
.DS_Store
Thumbs.db
"@ | Out-File -FilePath ".gitignore" -Encoding UTF8

# Git add and commit
git add .
git commit -m "Initial configuration commit"

# Create main branch explicitly (in case default is master)
git branch -M main

Write-Host ""
Write-Host "✅ Config repository setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📁 Repository location: $CONFIG_REPO_DIR" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 Next steps:" -ForegroundColor Cyan
Write-Host "   1. Review and customize the configuration files"
Write-Host "   2. Start Config Server: cd config-server; mvn spring-boot:run"
Write-Host "   3. Test: curl http://localhost:8888/user-service/default"
Write-Host ""
Write-Host "🔐 Important: Change JWT_SECRET in production!" -ForegroundColor Yellow
Write-Host "   Set environment variable: `$env:JWT_SECRET='your-secret-here'"
Write-Host ""
