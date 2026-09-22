#!/bin/bash

# View logs from infrastructure services

SERVICE=$1

cd "$(dirname "$0")/.."

if [ -z "$SERVICE" ]; then
    echo "📋 Available services:"
    echo "   - postgres"
    echo "   - redis"
    echo "   - kafka"
    echo "   - zookeeper"
    echo "   - kafka-ui"
    echo "   - mailhog"
    echo "   - adminer"
    echo ""
    echo "Usage: ./docker/logs-infra.sh [service-name]"
    echo "Example: ./docker/logs-infra.sh kafka"
    echo ""
    echo "Or view all logs:"
    docker-compose logs --tail=50
else
    echo "📋 Logs for $SERVICE:"
    docker-compose logs -f --tail=100 "$SERVICE"
fi
