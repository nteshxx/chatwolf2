High Level Design:


                     ┌──────────────────┐
                     │  React Frontend  │
                     └───────┬──────────┘
                             │
                     ┌───────▼──────────┐
                     │  API Gateway     │
                     │ (Spring Cloud)   │
                     └───────┬──────────┘
                             │
        ┌────────────────────┼──────────────────────────────────────────────┐
        │                    │                      │                       │
 ┌──────▼──────┐    ┌────────▼────────┐     ┌───────▼────────┐      ┌───────▼────────┐
 │Auth Service │    │ API Service     │     │ Socket Service │      │ Presence Svc   │
 │ (JWT + User)│    │ (REST + DB)     │     │ (Go + WS)      │      │ (Redis + Kafka)│
 └──────┬──────┘    └────────┬────────┘     └────────┬───────┘      └───────┬────────┘
        │                    │                       │                      │
        │                    │                       │                      │
        │                    │                       │                      │
        │                    │                       │                      │
        │                    │                       │                      │
        │                    ▼                       ▼                      ▼
        │        ┌────────────────────────────┐      │                      │
        │        │  Message Consumer Svc      │◄─────┘                      │
        │        │  (Kafka → PostgreSQL)      │                             │
        │        └─────────┬──────────────────┘                             │
        │                  │                                                │
        │          ┌───────▼────────┐                                       │
        │          │ Storage Svc    │                                       │
        │          │ (MinIO + REST) │                                       │
        │          └───────┬────────┘                                       │
        │                  │                                                │
        │          ┌───────▼────────┐                                       │
        │          │ Notification   │                                       │
        │          │ Service (Mail, │                                       │
        │          │ SMS via Kafka) │                                       │
        │          └───────┬────────┘                                       │
        │                  │                                                │
        │          ┌───────▼────────┐                                       │
        │          │ Search Service │                                       │
        │          │ (Elasticsearch)│                                       │
        │          └────────────────┘                                       │
        │                                                                   │
        ▼                                                                   │
 ┌───────────────────────────┐                                              │
 │ Eureka Server (Discovery) │◄─────────────────────────────────────────────┘ 
 └────────────┬──────────────┘
              ▼
 ┌────────────────────────┐
 │ Observability Stack    │
 │ Prometheus + Grafana + │
 │ Zipkin + Micrometer    │
 └────────────────────────┘



Services

| #    | Service                      | Purpose                      | Status          |
|------| ---------------------------- | ---------------------------- | --------------- |
| 1️    | **Eureka Server**            | Service Discovery            | ✅ Done         |
| 2️    | **API Gateway**              | Entry point                  | ✅ Done         |
| 3    | **Auth Service**             | Authentication & JWT         | ✅ Done         |
| 4️    | **API Service**              | Core REST APIs               | ✅ Done         |
| 5️    | **Socket Service (Go)**      | Real-time messaging          | ✅ Done         |
| 6️    | **Message Consumer Service** | Persist messages from Kafka  | 🚧 In Progress  |
| 7️    | **Presence Service**         | Manage online status         | ⏳ Pending      |
| 8️    | **Storage Service**          | File uploads to MinIO        | 🆕 Pending      |
| 9️    | **Notification Service**     | Email/SMS notifications      | 🆕 Pending      |
| 10   | **Search Service**           | Elasticsearch message search | 🆕 Pending      |
| 1️1   | **PostgreSQL DB**            | Persistent store             | ✅ Done         |
| 1️2   | **Kafka**                    | Event backbone               | ✅ Done         |
| 1️3   | **Grafana**                  | Monitoring + tracing         | 🧩 To Integrate |


Docker

---------------------------------------------------------------------------------------
| Component            | Docker Image        | Ports     | Role                       |
| -------------------- | ------------------- | --------- | -------------------------- |
| PostgreSQL           | `postgres:16`       | 5432      | Persistent data            | done
| Redis                | `redis:7`           | 6379      | Cache, pub/sub, rate limit | done
| MinIO                | `minio/minio`       | 9000/9001 | Attachment storage         | done
| Kafka (optional)     | `bitnami/kafka`     | 9092      | Message broker             | done
| Zookeeper (if Kafka) | `bitnami/zookeeper` | 2181      | Kafka dependency           | done
| Prometheus           | `prom/prometheus`   | 9090      | Metrics                    | done
| Grafana              | `grafana/grafana`   | 3000      | Dashboards                 | done



Deliverables:

| Order | Step                                                                  | Deliverable               |
| ----- | --------------------------------------------------------------------- | ------------------------- |
| 1️     | Finalize **Auth Service** (JWT issue + refresh)                       | Fully working auth + JWKS |
| 2️     | Implement **API Service** (core REST + JWT validation)                | User/chats API            |
| 3     | Setup **Kafka + Zookeeper + MinIO + Elasticsearch** in Docker Compose | Local infra backbone      |
| 4️     | Build **Socket Service (Go)** with Kafka producers                    | Real-time layer           |
| 5️     | Implement **Message Consumer Service**                                | Persist messages          |
| 6️     | Implement **Presence Service** (Redis + Kafka)                        | Online/offline            |
| 7️     | Implement **Storage Service**                                         | File handling             |
| 8️     | Implement **Notification Service**                                    | Email/SMS async           |
| 9️     | Implement **Search Service**                                          | Full-text message search  |
| 10    | Add **Prometheus + Grafana + Zipkin**                                 | Observability stack       |


