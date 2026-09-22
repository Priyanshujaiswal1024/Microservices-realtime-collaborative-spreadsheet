#!/bin/bash

# Stop services and remove all volumes (DELETES ALL DATA!)

echo "⚠️  WARNING: This will delete ALL data (databases, Kafka topics, Redis data)"
echo ""
read -p "Are you sure you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Aborted."
    exit 0
fi

echo ""
echo "🗑️  Stopping services and removing volumes..."

cd "$(dirname "$0")/.."

docker-compose down -v

echo ""
echo "✅ All services stopped and data volumes removed."
echo ""
echo "To restart with fresh data: ./docker/start-infra.sh"
