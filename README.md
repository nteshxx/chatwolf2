High Level Design:

### Architecture Overview

```text
   ┌──────────────────┐
   │ React Frontend   │
   └───────┬──────────┘
           │                   ┌───────────────────────┐
   ┌───────▼──────────┐        │ Observability Stack   │
   │ API Gateway      │        │ Prometheus + Grafana  │
   │ (Spring Cloud)   │        │ Zipkin + Micrometer   │
   └───────┬──────────┘        └───────────────────────┘
           │
  ┌────────┼──────────────────┬─────────┐
  │        │                  │         │
┌─▼────┐ ┌─▼────────┐ ┌───────▼────┐ ┌──▼────────┐
│Auth  │ │   API    │ │ Socket Svc │ │ Presence  │
│(JWT  │ │ (REST +  │ │ (Go + WS)  │ │ (Redis +  │
│+User)│ │   DB)    │ │            │ │ Kafka)    │
└─┬────┘ └─┬────────┘ └───┬────────┘ └────────┬──┘
  │        │              │___________________│
  │        │              │                   │
  │        │    ┌─────────▼────────────┐      │
  │        │    │   Message Consumer   │______│
  │        │    │ (Kafka → PostgreSQL) │      │
  │        │    └──────┬───────────────┘      │
  │        │           │                      │
  │        │ ┌─────────▼────────┐             │
  │        │ │ Storage Service  │_____________│
  │        │ │ (MinIO + REST)   │             │
  │        │ └──────┬───────────┘             │
  │        │        │                         │
  │        │ ┌──────▼────────┐                │
  │        │ │ Notification  │                │
  │        │ │ (Mail/SMS via │________________│
  │        │ │   Kafka)      │                │
  │        │ └───────────────┘                │
  │        │                                  │
  │        │ ┌─────────────────┐              │
  │        │ │ Search Service  │              │
  │        │ │ (Elasticsearch) │              │
  │        │ └─────────────────┘              │
  │        │         │________________________│
┌─▼────────▼─────────▼─┐
│ Eureka Server        │
│ (Service Discovery)  │
└──────────────────────┘

```

Services (brief):
- Auth Service — JWT, user management
- API Service — Core REST APIs, DB
- Socket Service — Real-time messaging (Go, WebSocket)
- Presence Service — Online status (Redis + Kafka)
- Message Consumer — Persist messages from Kafka to Postgres
- Storage Service — File uploads (MinIO + REST)
- Notification Service — Email/SMS (via Kafka)
- Search Service — Elasticsearch for message search
- Eureka — Service discovery
- Observability — Prometheus, Grafana, Zipkin, Micrometer


Microservices:

-----------------------------------------------------------------------------------------
| #    | Service                      | Purpose                      | Status           |
|------| ---------------------------- | ---------------------------- | ------------------
| 1️    | **Eureka Server**            | Service Discovery            | ✅ Done         |
| 2️    | **API Gateway**              | Entry point                  | ✅ Done         |
| 3    | **Auth Service**             | Authentication & JWT         | ✅ Done         |
| 4️    | **API Service**              | Core REST APIs               | ⏳ Pending      |
| 5️    | **Socket Service (Go)**      | Real-time messaging          | ⏳ Pending      |
| 6️    | **Message Consumer Service** | Persist messages from Kafka  | ⏳ Pending      |
| 7️    | **Presence Service**         | Manage online status         | ⏳ Pending      |
| 8️    | **Storage Service**          | File uploads to MinIO        | ⏳ Pending      |
| 9️    | **Notification Service**     | Email/SMS notifications      | ⏳ Pending      |
| 10   | **Search Service**           | Elasticsearch message search | ⏳ Pending      |
-----------------------------------------------------------------------------------------


Docker Containers

--------------------------------------------------------------------------------------------
| Container            | Docker Image             | Ports     | Role                       |
| -------------------- | ------------------------ | --------- | -------------------------- |
| Eureka               | `eureka:1.0.0`           | 8761      | Service Discovery          |
| Gateway              | `gateway:1.0.0`          | 7000      | API Gateway                |
| Auth                 | `auth:1.0.0`             | 7100      | Auth Service               |
| Loki                 | `grafana/loki:3.5`       | 3100      | Logs                       |
| Promtail             | `grafana/promtail:3.5`   | 9092      | Log Shipper to Loki        |
| Prometheus           | `prom/prometheus:v3.7.3` | 9090      | Metrics                    |
| Grafana              | `grafana/grafana:12.2.1` | 3000      | Logs + Metrics Dashboards  |
| Redis                | `redis:8.2-alpine`       | 6379      | Rate Limit & Caching       |
| Zipkin               | `openzipkin/zipkin:3.5`  | 9411      | Distributed Tracing        |
| Elasticsearch        | `elasticsearch:8.11.4`   | 9200      | Zipkin Storage             |
| Postgres             | `postgres:18-alpine`     | 5432      | Database Storage           |
--------------------------------------------------------------------------------------------


Deliverables Remaining:

-------------------------------------------------------------------------------------------------------------
| Order | Step                                                                  | Deliverable               |
| ----- | --------------------------------------------------------------------- | ------------------------- |
| 1     | Build **Socket Service (Go)** with Kafka producers                    | Real-time layer           |
| 2     | Implement **Presence Service** (Redis + Kafka)                        | Online/offline            |
| 3     | Setup **Kafka + Zookeeper** in Docker Compose                         | Local infra backbone      |
| 4     | Implement **Message Consumer Service**                                | Persist messages          |
| 5     | Implement **API Service** (core REST + JWT validation)                | User/chats API            |
| 6     | Implement **Storage Service + MinIO**                                 | File handling             |
| 7     | Implement **Notification Service**                                    | Email/SMS async           |
| 8     | Implement **Search Service**                                          | Full-text message search  |
| 9     | Add **Prometheus + Grafana + Zipkin**                                 | Observability stack       |
-------------------------------------------------------------------------------------------------------------
