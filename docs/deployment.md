# Production Deployment & AWS Infrastructure Guide

## 1. AWS Managed Services

| Component | AWS Service | Configuration |
|---|---|---|
| **PostgreSQL** | AWS RDS PostgreSQL 16 | Multi-AZ, db.r6g.xlarge, gp3 storage |
| **Redis** | AWS ElastiCache for Redis | Cluster Mode Enabled, 3 shards x 2 replicas |
| **Kafka** | AWS MSK (Managed Streaming for Kafka) | 3 brokers, kafka.m5.large, TLS authentication |
| **Container Engine** | AWS EKS (Kubernetes 1.29+) | Managed node groups (m5.xlarge), Karpenter autoscaling |
| **Object Storage** | AWS S3 | Encrypted with KMS for workbook exports |
| **Telemetry** | CloudWatch + OpenTelemetry | Distributed tracing across microservices |

---

## 2. Local Development Deployment

```bash
# 1. Start Postgres, Redis, Kafka, MailHog
docker-compose up -d

# 2. Build backend
mvn clean install -DskipTests

# 3. Launch Config Server & Eureka
cd config-server && mvn spring-boot:run
cd eureka-server && mvn spring-boot:run

# 4. Launch domain services in separate terminals
cd user-service && mvn spring-boot:run
cd sheet-service && mvn spring-boot:run
cd collab-service && mvn spring-boot:run
cd comment-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd audit-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run

# 5. Start Frontend
cd frontend
npm install
npm run dev
```

---

## 3. Full Containerized Local Deployment

```bash
# Builds and runs all 9 services + Frontend + Infra in Docker
docker-compose -f docker-compose.full.yml up --build -d
```
