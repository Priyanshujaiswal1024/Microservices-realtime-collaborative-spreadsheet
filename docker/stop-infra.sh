#!/bin/bash

# Stop all infrastructure services

echo "🛑 Stopping Collaborative Spreadsheet Infrastructure..."

cd "$(dirname "$0")/.."

docker-compose down

echo ""
echo "✅ All services stopped."
echo ""
echo "To remove volumes (⚠️  data will be lost): docker-compose down -v"
echo "To restart: ./docker/start-infra.sh"
