#!/bin/bash

# Start all infrastructure services for local development

echo "🚀 Starting Collaborative Spreadsheet Infrastructure..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Navigate to project root
cd "$(dirname "$0")/.."

# Start services
echo "📦 Starting PostgreSQL, Redis, Kafka, and supporting services..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
echo ""

# Wait for PostgreSQL
echo -n "Waiting for PostgreSQL... "
until docker exec collab-postgres pg_isready -U collabuser -d collabdb > /dev/null 2>&1; do
    sleep 1
    echo -n "."
done
echo " ✅"

# Wait for Redis
echo -n "Waiting for Redis... "
until docker exec collab-redis redis-cli ping > /dev/null 2>&1; do
    sleep 1
    echo -n "."
done
echo " ✅"

# Wait for Kafka
echo -n "Waiting for Kafka... "
sleep 15
until docker exec collab-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    sleep 2
    echo -n "."
done
echo " ✅"

echo ""
echo "✅ All infrastructure services are ready!"
echo ""
echo "📊 Service URLs:"
echo "   PostgreSQL:      localhost:5432 (user: collabuser, password: collabpass)"
echo "   Redis:           localhost:6379"
echo "   Kafka:           localhost:29092 (internal: kafka:9092)"
echo "   Kafka UI:        http://localhost:8090"
echo "   MailHog UI:      http://localhost:8025"
echo "   Adminer (DB UI): http://localhost:8091"
echo ""
echo "📝 Kafka Topics:"
docker exec collab-kafka kafka-topics --list --bootstrap-server localhost:9092
echo ""
echo "To stop: ./docker/stop-infra.sh"
echo "To view logs: docker-compose logs -f [service-name]"
