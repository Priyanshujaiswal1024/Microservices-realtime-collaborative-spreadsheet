# Docker Infrastructure Guide

This directory contains Docker Compose configuration and helper scripts for running the infrastructure services locally.

## Services

| Service | Port | Purpose | UI |
|---------|------|---------|-----|
| **PostgreSQL** | 5432 | Primary database (5 separate DBs) | http://localhost:8091 (Adminer) |
| **Redis** | 6379 | CRDT state, Pub/Sub, Streams, cache | - |
| **Kafka** | 29092 | Event streaming | http://localhost:8090 (Kafka UI) |
| **Zookeeper** | 2181 | Kafka coordination | - |
| **MailHog** | 1025 (SMTP), 8025 (UI) | Email testing | http://localhost:8025 |

## Quick Start

### 1. Start Infrastructure

```bash
./docker/start-infra.sh
```

This will:
- Start all services
- Wait for health checks
- Create Kafka topics
- Display service URLs

### 2. Stop Infrastructure

```bash
./docker/stop-infra.sh
```

Stops all services but **preserves data volumes**.

### 3. Clean Everything (Delete All Data)

```bash
./docker/clean-infra.sh
```

⚠️ **Warning**: This deletes all databases, Kafka topics, and Redis data!

### 4. View Logs

```bash
# View specific service logs
./docker/logs-infra.sh kafka

# View all logs
./docker/logs-infra.sh
```

## Manual Commands

### PostgreSQL

```bash
# Connect to PostgreSQL
docker exec -it collab-postgres psql -U collabuser -d collabdb

# List databases
docker exec collab-postgres psql -U collabuser -l

# Connect to specific service database
docker exec -it collab-postgres psql -U collabuser -d sheetdb
```

### Redis

```bash
# Connect to Redis CLI
docker exec -it collab-redis redis-cli

# Check Redis info
docker exec collab-redis redis-cli INFO

# Monitor Redis commands in real-time
docker exec collab-redis redis-cli MONITOR
```

### Kafka

```bash
# List topics
docker exec collab-kafka kafka-topics --list --bootstrap-server localhost:9092

# Describe a topic
docker exec collab-kafka kafka-topics --describe --topic cell-edits --bootstrap-server localhost:9092

# Consume messages from a topic (from beginning)
docker exec collab-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic cell-edits \
  --from-beginning

# Produce test message
docker exec -it collab-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic cell-edits
```

## Kafka Topics

Created automatically on startup:

| Topic | Partitions | Retention | Purpose |
|-------|------------|-----------|---------|
| `cell-edits` | 10 | 7 days | Real-time cell updates from collab-service |
| `sheet-lifecycle` | 5 | 30 days | Workbook/sheet create/delete/update events |
| `comments` | 5 | 30 days | Comment and @mention events |
| `notifications` | 10 | 7 days | User notification events |

## Database Structure

PostgreSQL creates 5 separate databases:

- `userdb` - User service (users, roles, refresh tokens)
- `sheetdb` - Sheet service (workbooks, sheets, cells, permissions)
- `commentdb` - Comment service (comments, mentions)
- `notificationdb` - Notification service (notification queue)
- `auditdb` - Audit service (event-sourced audit log)

## Web UIs

### Kafka UI (http://localhost:8090)
- Browse topics
- View messages
- Monitor consumer groups
- Topic configuration

### MailHog UI (http://localhost:8025)
- View all emails sent by the application
- Test email notifications

### Adminer (http://localhost:8091)
- Connect: Server=`postgres`, Username=`collabuser`, Password=`collabpass`
- Browse all databases
- Execute SQL queries
- Import/export data

## Troubleshooting

### Kafka not starting

```bash
# Check Zookeeper health
docker exec collab-zookeeper zkServer.sh status

# Restart Kafka
docker-compose restart kafka
```

### PostgreSQL connection refused

```bash
# Check if PostgreSQL is ready
docker exec collab-postgres pg_isready -U collabuser

# View PostgreSQL logs
docker-compose logs postgres
```

### Redis connection errors

```bash
# Test Redis connection
docker exec collab-redis redis-cli ping

# Check Redis memory
docker exec collab-redis redis-cli INFO memory
```

### Out of disk space

```bash
# Check Docker disk usage
docker system df

# Clean up unused resources
docker system prune -a --volumes
```

### Reset everything

```bash
# Stop and remove all containers and volumes
./docker/clean-infra.sh

# Rebuild and restart
./docker/start-infra.sh
```

## Production Notes

⚠️ **This setup is for LOCAL DEVELOPMENT ONLY!**

For production:
- Use managed services (AWS RDS, ElastiCache, MSK)
- Enable authentication on all services
- Use SSL/TLS for all connections
- Configure proper replication factors
- Enable monitoring and alerting
- Set up proper backup strategies

See [../k8s/README.md](../k8s/README.md) for production Kubernetes deployment.

## Environment Variables

Services use these defaults (override in `docker-compose.yml` if needed):

```bash
# PostgreSQL
POSTGRES_USER=collabuser
POSTGRES_PASSWORD=collabpass
POSTGRES_DB=collabdb

# Kafka
KAFKA_BROKER_ID=1
KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181

# Redis
# No auth by default (add requirepass for production)
```

## Network

All services run on bridge network `collab-network` (172.25.0.0/16).

Service-to-service communication uses container names:
- `postgres:5432`
- `redis:6379`
- `kafka:9092`
- `zookeeper:2181`

External access uses `localhost` with mapped ports.
