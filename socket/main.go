package main

import (
    "context"
    "fmt"
    "github.com/prometheus/client_golang/prometheus/promhttp"
    "net/http"
    "os"
    "os/signal"
    "syscall"
    "time"
)

func main() {
    jwksURL := envOrDefault("JWKS_URL", "http://auth-service:8081/.well-known/jwks.json")
    kafkaBrokers := envOrDefault("KAFKA_BROKERS", "kafka:9092")
    kafkaTopic := envOrDefault("KAFKA_TOPIC", "chat-messages")
    addr := envOrDefault("SOCKET_ADDR", ":3000")
    redisAddr := envOrDefault("REDIS_ADDR", "redis:6379")
    zipkinURL := envOrDefault("ZIPKIN_URL", "http://zipkin:9411/api/v2/spans")
    eurekaURL := envOrDefault("EUREKA_URL", "http://eureka:8761/eureka")
    appName := envOrDefault("APP_NAME", "socket-service")
    host := envOrDefault("HOST_NAME", "socket")
    port := 3000

    initMetrics()

    shutdownTracer, err := InitTracer(zipkinURL, appName)
    if err != nil {
        fmt.Println("zipkin init error:", err)
    }
    defer shutdownTracer(context.Background())

    validator, err := NewJwtValidator(jwksURL)
    if err != nil {
        logger.Fatal().Err(err).Msg("failed to init jwks validator")
    }

    producers, err := NewKafkaProducer([]string{kafkaBrokers}, kafkaTopic)
    if err != nil {
        logger.Fatal().Err(err).Msg("failed to create kafka producer")
    }

    redisHub, err := NewRedisHub(redisAddr, "", 0, nil)
    if err != nil {
        logger.Fatal().Err(err).Msg("failed to connect redis")
    }

    tracer := Tracer("socket-service")
    server := NewServer(validator, producers, redisHub, tracer)
    redisHub.server = server
    redisHub.StartSubscriber()

    deregister, err := RegisterWithEureka(eurekaURL, appName, host, port)
    if err != nil {
        logger.Warn().Err(err).Msg("eureka register failed")
    } else {
        defer deregister()
    }

    mux := http.NewServeMux()
    mux.HandleFunc("/ws", server.handleWS)
    mux.Handle("/metrics", promhttp.Handler())
    mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
        w.WriteHeader(http.StatusOK)
        _, _ = w.Write([]byte("ok"))
    })

    srv := &http.Server{
        Addr:    addr,
        Handler: mux,
    }

    go func() {
        logger.Info().Str("addr", addr).Msg("starting socket service")
        if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
            logger.Fatal().Err(err).Msg("server error")
        }
    }()

    stop := make(chan os.Signal, 1)
    signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
    <-stop
    ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
    defer cancel()
    logger.Info().Msg("shutting down")
    _ = srv.Shutdown(ctx)
    _ = producers.Close()
    _ = redisHub.client.Close()
}
