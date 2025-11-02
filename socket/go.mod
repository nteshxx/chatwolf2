module github.com/yourorg/socket-service

go 1.20

require (
    github.com/Shopify/sarama v1.34.0
    github.com/gorilla/websocket v1.5.0
    github.com/MicahParks/keyfunc v0.9.0
    github.com/golang-jwt/jwt/v5 v5.0.0
    github.com/rs/zerolog v1.30.0
    github.com/redis/go-redis/v9 v9.3.2
    go.opentelemetry.io/otel v1.28.0
    go.opentelemetry.io/otel/exporters/zipkin v1.7.0
    go.opentelemetry.io/otel/sdk v1.28.0
    go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp v0.14.0
    github.com/prometheus/client_golang v1.16.0
)
