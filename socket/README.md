# Socket Service (Go) - with Redis broadcasting, Zipkin tracing, Prometheus metrics, and Eureka registration

This service:
- Accepts WebSocket connections at /ws
- Validates JWTs using JWKS URL from Auth Service
- Publishes messages to Kafka topic `chat-messages`
- Publishes deliver events to Redis channel `message-deliver` for multi-node delivery
- Publishes presence events to Redis channel `presence-events`
- Exposes Prometheus metrics at /metrics
- Registers with Eureka for service discovery
- Sends traces to Zipkin via OpenTelemetry

## Build
```
docker build -t socket-service:local .
```

## Run (example)
Use docker-compose with Kafka, Zookeeper, Redis, Zipkin, and Eureka. Example env vars:
- JWKS_URL
- KAFKA_BROKERS
- KAFKA_TOPIC
- REDIS_ADDR
- ZIPKIN_URL
- EUREKA_URL
- APP_NAME
- HOST_NAME
- SOCKET_ADDR
