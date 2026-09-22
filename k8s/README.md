# Kubernetes Deployment Guide (AWS EKS)

This directory contains the production-grade Kubernetes deployment manifests for the **Real-Time Collaborative Spreadsheet Platform**.

## Architecture on EKS

- **Namespace**: `collab-spreadsheet`
- **Managed AWS Services**:
  - AWS RDS PostgreSQL (Multi-AZ)
  - AWS ElastiCache for Redis (Cluster Mode Enabled)
  - AWS MSK (Managed Streaming for Apache Kafka)
  - AWS S3 (Excel/CSV attachments & backups)
- **Ingress**: NGINX Ingress Controller with WebSocket upgrade routing for STOMP over SockJS
- **Autoscaling**: Horizontal Pod Autoscalers (HPA) configured for all stateless services

## Deployment Steps

```bash
# 1. Create namespace
kubectl apply -f 00-namespace.yaml

# 2. Apply configuration & secrets
kubectl apply -f 01-configmap.yaml
kubectl apply -f 02-secrets.yaml

# 3. Deploy infrastructure services
kubectl apply -f 10-eureka-server.yaml
kubectl apply -f 11-config-server.yaml
kubectl apply -f 12-api-gateway.yaml

# 4. Deploy domain microservices
kubectl apply -f 20-user-service.yaml
kubectl apply -f 21-sheet-service.yaml
kubectl apply -f 22-collab-service.yaml
kubectl apply -f 23-comment-service.yaml
kubectl apply -f 24-notification-service.yaml
kubectl apply -f 25-audit-service.yaml

# 5. Deploy Frontend & Ingress
kubectl apply -f 30-frontend.yaml
kubectl apply -f 40-ingress.yaml
```

## Verify Deployment

```bash
kubectl get pods -n collab-spreadsheet
kubectl get hpa -n collab-spreadsheet
kubectl get ingress -n collab-spreadsheet
```
