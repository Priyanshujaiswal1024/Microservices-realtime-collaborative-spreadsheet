# System Design Document — Real-Time Collaborative Spreadsheet Platform

## 1. High-Level Architecture

The platform follows an event-driven, decoupled microservices architecture designed to scale to thousands of concurrent users editing workbooks simultaneously.

```
                                      +------------------------------------+
                                      |     React Frontend (Vite)          |
                                      +-----------------+------------------+
                                                        |
                                            HTTP / REST | WebSocket (STOMP)
                                                        v
                                      +------------------------------------+
                                      |       API Gateway (Port 8080)      |
                                      |     (JWT Validation & Routing)     |
                                      +----+-----------+---------------+---+
                                           |           |               |
               +---------------------------+           |               +---------------------------+
               |                                       |                                           |
               v                                       v                                           v
+-----------------------------+         +-----------------------------+         +-----------------------------+
|    User Service (8081)      |         |   Collab Service (8083)     |         |    Sheet Service (8082)     |
|   (Auth, JWT, RBAC Roles)   |         |   (CRDT, Presence, HLC)     |         |  (Workbook/Sheet/Cell CRUD) |
+--------------+--------------+         +--------------+--------------+         +--------------+--------------+
               |                                       |                                       |
               |                                       | Redis Pub/Sub & Hash                  | Postgres (Flyway)
               |                                       v                                       |
               |                        +-----------------------------+                        |
               |                        |        Redis Cluster        |                        |
               |                        |  (Hash, Streams, Pub/Sub)   |                        |
               |                        +-----------------------------+                        |
               |                                       |                                       |
               +-----------------------------------+   | Kafka Producer                        |
                                                   |   v                                       |
                                            +-----------------------------+                    |
                                            |       Apache Kafka          |                    |
                                            | (cell-edits, sheet-lifecycle|                    |
                                            |  comments, notifications)   |                    |
                                            +----+-------------------+----+                    |
                                                 |                   |                         |
                                                 v                   v                         v
                                  +--------------------+     +--------------------+     +---------------+
                                  |Comment Service(8084|     |Audit Service (8086)|     |Notification(85|
                                  | (Threads, Mentions)|     |  (Event Sourcing)  |     | (Email, Push) |
                                  +--------------------+     +--------------------+     +---------------+
```

## 2. Microservice Responsibilities

1. **API Gateway (`api-gateway`, 8080)**: Validates incoming JWT access tokens, injects `X-User-Id` / `X-User-Role` headers, enforces token-bucket rate limits, and routes WebSocket traffic.
2. **User Service (`user-service`, 8081)**: Handles user registration, BCrypt password hashing, JWT issue, and SHA-256 hashed refresh token rotation in Redis.
3. **Sheet Service (`sheet-service`, 8082)**: Manages workbook and sheet hierarchies, cell state in PostgreSQL, protected range definitions, and Apache POI XLSX/CSV import/export.
4. **Collab Service (`collab-service`, 8083)**: Handles active WebSocket sessions, executes LWW-CRDT merge strategy using Hybrid Logical Clocks, caches cell state in Redis Hashes, buffers ops in Redis Streams, and broadcasts across pods via Redis Pub/Sub.
5. **Comment Service (`comment-service`, 8084)**: Manages cell-level discussion threads, replies, and extracts `@mentions` via regex.
6. **Notification Service (`notification-service`, 8085)**: Listens to Kafka topics asynchronously and stores in-app notifications and dispatches emails.
7. **Audit Service (`audit-service`, 8086)**: Consumes all state mutation events idempotently to maintain an immutable append-only event log with snapshot and restore capabilities.
