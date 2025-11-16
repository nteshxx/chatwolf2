package main

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gorilla/mux"
	"github.com/nteshxx/chatwolf2/socket/internal/config"
	"github.com/nteshxx/chatwolf2/socket/internal/domain"
	"github.com/nteshxx/chatwolf2/socket/internal/middleware"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/logger"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/metrics"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/tracing"
	"github.com/nteshxx/chatwolf2/socket/internal/registry/eureka"
	"github.com/nteshxx/chatwolf2/socket/internal/repository/kafka"
	"github.com/nteshxx/chatwolf2/socket/internal/repository/redis"
	"github.com/nteshxx/chatwolf2/socket/internal/service"
	"github.com/nteshxx/chatwolf2/socket/internal/websocket"
	"github.com/op/go-logging"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

func main() {
	disableFargoLogging()
	ctx := context.Background()

	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		fmt.Fprintf(os.Stderr, "failed to load config: %v\n", err)
		os.Exit(1)
	}

	// Initialize logger
	logger.Init(cfg.Logging.Level)
	log := logger.Global()
	log.Info(ctx, "starting socket service", map[string]interface{}{
		"app":       cfg.Server.AppName,
		"port":      cfg.Server.Port,
		"log_level": cfg.Logging.Level,
	})

	// Initialize metrics
	appMetrics := metrics.NewMetrics()

	// Initialize tracer
	tracerCfg := tracing.Config{
		ServiceName:            cfg.Server.AppName,
		ServiceAddr:            fmt.Sprintf("%s:%d", cfg.Server.Host, cfg.Server.Port),
		ZipkinURL:              cfg.Zipkin.URL,
		SampleRate:             cfg.Zipkin.SampleRate,
		BatchSize:              cfg.Zipkin.BatchSize,
		BatchTimeout:           cfg.Zipkin.BatchTimeout,
		ExcludeHealthEndpoint:  true,
		ExcludeMetricsEndpoint: true,
	}
	tracer, err := tracing.New(tracerCfg)
	if err != nil {
		log.Fatal(ctx, "failed to init tracer", err)
	}
	defer tracer.Close(ctx)

	// Initialize auth service
	authService, err := middleware.NewAuthService(ctx, cfg.Auth.JWKSURL)
	if err != nil {
		log.Fatal(ctx, "failed to init auth service", err)
	}
	defer authService.Close(ctx)

	// Initialize Kafka producer
	kafkaProducer, err := kafka.NewProducer(ctx, cfg.Kafka.Brokers, cfg.Kafka.Topic, log)
	if err != nil {
		log.Fatal(ctx, "failed to create kafka producer", err)
	}
	defer kafkaProducer.Close(ctx)

	// Initialize Redis hub
	redisHub, err := redis.NewHub(ctx, cfg.Redis.Addr, cfg.Redis.Password, cfg.Redis.DB, log)
	if err != nil {
		log.Fatal(ctx, "failed to create redis hub", err)
	}
	defer redisHub.Close(ctx)

	// Initialize presence publisher (uses Redis hub internally)
	presencePublisher := redis.NewPresencePublisher(redisHub.GetClient(), log)

	// Verify Redis connection for presence
	if err := presencePublisher.Ping(ctx); err != nil {
		log.Fatal(ctx, "failed to connect to Redis for presence", err)
	}
	log.Info(ctx, "presence publisher initialized", nil)

	// Initialize WebSocket server
	wsServer := websocket.NewServer(authService, nil, presencePublisher, log, appMetrics)

	// Initialize message service
	messageService := service.NewMessageService(kafkaProducer, redisHub, wsServer, log, appMetrics, tracer)

	// Set message handler on WebSocket server (circular dependency resolved)
	wsServer = websocket.NewServer(authService, messageService, presencePublisher, log, appMetrics)

	// Set Redis handlers
	redisHub.SetHandlers(
		func(ctx context.Context, event domain.MessageEvent) {
			wsServer.DeliverToUser(ctx, event.To, event)
		},
		nil, // presence handler not needed (using direct publisher)
	)

	// Start Redis subscriber
	if err := redisHub.StartSubscriber(ctx); err != nil {
		log.Fatal(ctx, "failed to start redis subscriber", err)
	}

	// Register with Eureka
	eurekaClient, err := eureka.NewClient(ctx, cfg.Eureka.URL, cfg.Server.AppName, cfg.Server.Host, cfg.Server.Port, log)
	if err != nil {
		log.Fatal(ctx, "failed to register with eureka", err)
	}

	// Setup HTTP routes
	router := mux.NewRouter()

	// Apply middleware
	router.Use(tracer.Middleware())

	loggingCfg := middleware.LoggingConfig{
		ExcludeHealthEndpoint:  cfg.Logging.ExcludeHealthEndpoint,
		ExcludeMetricsEndpoint: cfg.Logging.ExcludeMetricsEndpoint,
	}
	router.Use(middleware.Logging(log, loggingCfg))

	// WebSocket endpoint
	router.HandleFunc("/api/socket/connect", wsServer.HandleConnection)

	// Stats endpoint
	router.HandleFunc("/api/socket/stats", func(w http.ResponseWriter, r *http.Request) {
		stats := wsServer.GetStats()
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, `{"status":"ok","total_users":%d,"total_connections":%d}`,
			stats["total_users"], stats["total_connections"])
	}).Methods("GET")

	// Metrics endpoint
	router.Handle("/prometheus/metrics", promhttp.Handler()).Methods("GET")

	// Health check endpoint
	router.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		// Check Redis connection
		if err := presencePublisher.Ping(r.Context()); err != nil {
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusServiceUnavailable)
			w.Write([]byte(`{"status":"DOWN","redis":"disconnected"}`))
			return
		}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"UP","redis":"connected"}`))
	}).Methods("GET")

	// Create HTTP server
	httpServer := &http.Server{
		Addr:         fmt.Sprintf("%s:%d", cfg.Server.Host, cfg.Server.Port),
		Handler:      router,
		ReadTimeout:  cfg.Server.ReadTimeout,
		WriteTimeout: cfg.Server.WriteTimeout,
		IdleTimeout:  cfg.Server.IdleTimeout,
	}

	// Start HTTP server
	serverErrors := make(chan error, 1)
	go func() {
		log.Info(ctx, "http server listening", map[string]interface{}{
			"port": cfg.Server.Port,
		})
		serverErrors <- httpServer.ListenAndServe()
	}()

	// Wait for interrupt signal
	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)

	select {
	case err := <-serverErrors:
		if err != nil && err != http.ErrServerClosed {
			log.Fatal(ctx, "http server failed", err)
		}
	case sig := <-stop:
		log.Info(ctx, "shutdown signal received", map[string]interface{}{
			"signal": sig.String(),
		})
	}

	// Graceful shutdown
	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer shutdownCancel()

	log.Info(shutdownCtx, "initiating graceful shutdown")

	// Deregister from Eureka
	if err := eurekaClient.Deregister(shutdownCtx); err != nil {
		log.Error(shutdownCtx, "failed to deregister from eureka", err)
	}

	// Shutdown HTTP server
	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		log.Error(shutdownCtx, "http server shutdown failed", err)
		httpServer.Close()
	}

	// Shutdown WebSocket server
	if err := wsServer.Shutdown(shutdownCtx); err != nil {
		log.Error(shutdownCtx, "websocket server shutdown failed", err)
	}

	// Close remaining resources
	kafkaProducer.Close(shutdownCtx)
	redisHub.Close(shutdownCtx)
	authService.Close(shutdownCtx)
	tracer.Close(shutdownCtx)

	log.Info(shutdownCtx, "server shutdown complete")
}

func disableFargoLogging() {
	backend := logging.NewLogBackend(io.Discard, "", 0)
	logging.SetBackend(backend)
}
