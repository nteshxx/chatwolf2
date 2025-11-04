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

+---------------------------------------------------------------------------------------+
| #    | Service                      | Purpose                      | Status           |
|------| ---------------------------- | ---------------------------- | -----------------+
| 1️    | **Eureka Server**            | Service Discovery            | ✅ Done         |
| 2️    | **API Gateway**              | Entry point                  | 🚧 In Progress  |
| 3    | **Auth Service**             | Authentication & JWT         | 🚧 In Progress  |
| 4️    | **API Service**              | Core REST APIs               | ⏳ Pending      |
| 5️    | **Socket Service (Go)**      | Real-time messaging          | ⏳ Pending      |
| 6️    | **Message Consumer Service** | Persist messages from Kafka  | ⏳ Pending      |
| 7️    | **Presence Service**         | Manage online status         | ⏳ Pending      |
| 8️    | **Storage Service**          | File uploads to MinIO        | ⏳ Pending      |
| 9️    | **Notification Service**     | Email/SMS notifications      | ⏳ Pending      |
| 10   | **Search Service**           | Elasticsearch message search | ⏳ Pending      |
| 1️1   | **PostgreSQL DB**            | Persistent store             | ⏳ Pending      |
| 1️2   | **Kafka**                    | Event backbone               | ⏳ Pending      |
| 1️1   | **Zipkin**                   | Distributed tracing          | ⏳ Pending      |
| 1️3   | **Grafana**                  | Monitoring + tracing         | ✅ Done         |
+---------------------------------------------------------------------------------------+


Docker Components

---------------------------------------------------------------------------------------
| Component            | Docker Image        | Ports     | Role                       |
| -------------------- | ------------------- | --------- | -------------------------- |
| Eureka               | `eureka:1.0.0`      | 8761      | Service Discovery          |
| Eureka               | `gateway:1.0.0`     | 8080      | API Gateway                |
| Promtail             | `apache/kafka`      | 9092      | Log Shipper to Loki        |
| Loki                 | `grafana/loki`      | 2181      | Logs                       |
| Prometheus           | `prom/prometheus`   | 9090      | Metrics                    |
| Grafana              | `grafana/grafana`   | 3000      | Logs + Metrics Dashboards  | 
---------------------------------------------------------------------------------------


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


