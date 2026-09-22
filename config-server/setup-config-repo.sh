#!/bin/bash

# Script to initialize Git-backed configuration repository for Config Server

CONFIG_REPO_DIR="$HOME/collab-spreadsheet-config"

echo "🔧 Setting up Config Server Git repository..."
echo ""

# Check if directory already exists
if [ -d "$CONFIG_REPO_DIR" ]; then
    echo "⚠️  Directory $CONFIG_REPO_DIR already exists."
    read -p "Do you want to recreate it? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        rm -rf "$CONFIG_REPO_DIR"
        echo "Removed existing directory."
    else
        echo "Keeping existing directory. Exiting."
        exit 0
    fi
fi

# Create directory
mkdir -p "$CONFIG_REPO_DIR"
cd "$CONFIG_REPO_DIR"

# Initialize Git repository
git init
echo "✅ Initialized Git repository at $CONFIG_REPO_DIR"

# Copy configuration files from config-server resources
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_SOURCE_DIR="$SCRIPT_DIR/src/main/resources/config"

if [ -d "$CONFIG_SOURCE_DIR" ]; then
    cp "$CONFIG_SOURCE_DIR"/*.yml .
    echo "✅ Copied configuration files"
else
    echo "⚠️  Warning: Config source directory not found at $CONFIG_SOURCE_DIR"
    echo "Creating sample configuration files..."
    
    # Create minimal application.yml
    cat > application.yml <<'EOF'
# Global configuration for all services
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: UTC

logging:
  level:
    com.collab.spreadsheet: INFO
EOF
fi

# Create .gitignore
cat > .gitignore <<'EOF'
# Ignore sensitive files
*-secrets.yml
*-local.yml
*.key
*.pem

# OS files
.DS_Store
Thumbs.db
EOF

# Git add and commit
git add .
git commit -m "Initial configuration commit"

# Create main branch explicitly (in case default is master)
git branch -M main

echo ""
echo "✅ Config repository setup complete!"
echo ""
echo "📁 Repository location: $CONFIG_REPO_DIR"
echo ""
echo "📝 Next steps:"
echo "   1. Review and customize the configuration files"
echo "   2. Start Config Server: cd config-server && mvn spring-boot:run"
echo "   3. Test: curl http://localhost:8888/user-service/default"
echo ""
echo "🔐 Important: Change JWT_SECRET in production!"
echo "   Set environment variable: export JWT_SECRET=your-secret-here"
echo ""
