#!/bin/bash

# Start Eureka Server

echo "🚀 Starting Eureka Server..."
echo ""

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed or not in PATH"
    echo "Please install Java 21 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "⚠️  Warning: Java version is $JAVA_VERSION, but Java 21 or higher is recommended"
fi

# Navigate to eureka-server directory
cd "$(dirname "$0")"

# Check if Maven wrapper exists, otherwise use system Maven
if [ -f "../mvnw" ]; then
    echo "Using Maven wrapper..."
    MAVEN_CMD="../mvnw"
elif command -v mvn &> /dev/null; then
    echo "Using system Maven..."
    MAVEN_CMD="mvn"
else
    echo "❌ Maven is not installed and mvnw not found"
    echo "Please install Maven or run from project root"
    exit 1
fi

# Build if target doesn't exist
if [ ! -d "target" ]; then
    echo "Building Eureka Server..."
    $MAVEN_CMD clean package -DskipTests
fi

# Start Eureka Server
echo ""
echo "✅ Starting Eureka Server on port 8761..."
echo "📊 Dashboard: http://localhost:8761"
echo "💚 Health: http://localhost:8761/actuator/health"
echo ""
echo "Press Ctrl+C to stop"
echo ""

$MAVEN_CMD spring-boot:run
