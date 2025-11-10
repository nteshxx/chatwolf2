package main

import (
	"context"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	"github.com/nteshxx/chatwolf2/socket/config"
	"github.com/nteshxx/chatwolf2/socket/eureka"
	"github.com/nteshxx/chatwolf2/socket/internal/kafka"
	"github.com/nteshxx/chatwolf2/socket/internal/metrics"
	auth "github.com/nteshxx/chatwolf2/socket/internal/middlewares"
	"github.com/nteshxx/chatwolf2/socket/internal/tracing"
	"github.com/nteshxx/chatwolf2/socket/internal/utils"
	"github.com/nteshxx/chatwolf2/socket/internal/websockets"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

func main() {
	// Load configuration variables
	cfg := config.LoadFromEnv()

	// Init logger
	utils.InitLogger()
	utils.Log.Info().Msg("starting socket service")

	// Init zipkin tracer
	shutdown, err := tracing.InitTracer(cfg.ZipkinURL, cfg.AppName)
	if err != nil {
		utils.Log.Fatal().Err(err).Msg("failed to init zipkin tracer")
	}
	defer shutdown(context.Background())

	// Init Prometheus metrics
	metrics.InitMetrics()

	// Init JWT validator
	validator, err := auth.NewJwtValidator(context.Background(), cfg.JWKSURL)
	if err != nil {
		utils.Log.Fatal().Err(err).Msg("failed to init jwt validator")
	}

	// Kafka producer
	kafkaProducer, err := kafka.NewKafkaProducer(cfg.KafkaBrokers, cfg.KafkaTopic, cfg.Debug)
	if err != nil {
		utils.Log.Fatal().Err(err).Msg("failed to create kafka producer")
	}
	defer kafkaProducer.Close()

	// Redis hub
	redisHub, err := websockets.NewRedisHub(cfg.RedisAddr, cfg.RedisPassword, cfg.RedisDB)
	if err != nil {
		utils.Log.Fatal().Err(err).Msg("failed to create redis hub")
	}
	defer redisHub.Close()

	// WebSocket server
	tracer := tracing.Tracer("socket-service")
	wsServer := websockets.NewServer(validator, kafkaProducer, redisHub, tracer)

	// Start Redis subscriber (must be after server is set)
	redisHub.StartSubscriber()

	// Register with Eureka
	deregister, err := eureka.RegisterWithEureka(cfg.EurekaURL, cfg.AppName, cfg.HostName, cfg.Port)
	if err != nil {
		utils.Log.Fatal().Err(err).Msg("failed to register with eureka")
	}

	// Setup HTTP routes
	mux := http.NewServeMux()
	mux.HandleFunc("/socket/connect", wsServer.HandleWS)
	mux.Handle("/socket/metrics", promhttp.Handler())
	mux.HandleFunc("/socket/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("{ \"status\": \"UP\" }"))
	})

	// Create HTTP server
	httpServer := &http.Server{
		Addr:         cfg.HostName + ":" + strconv.Itoa(cfg.Port),
		Handler:      mux,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Start HTTP server in goroutine
	serverErrors := make(chan error, 1)
	go func() {
		utils.Log.Info().Msg("http server listening on port " + strconv.Itoa(cfg.Port))
		serverErrors <- httpServer.ListenAndServe()
	}()

	// Wait for interrupt signal or server error
	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)

	select {
	case err := <-serverErrors:
		if err != nil && err != http.ErrServerClosed {
			utils.Log.Fatal().Err(err).Msg("http server failed")
		}
	case sig := <-stop:
		utils.Log.Info().Str("signal", sig.String()).Msg("shutdown signal received")
	}

	// Graceful shutdown
	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer shutdownCancel()

	utils.Log.Info().Msg("initiating graceful shutdown")

	// Deregister from Eureka
	deregister()

	// Shutdown HTTP server (stops accepting new connections)
	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		utils.Log.Error().Err(err).Msg("http server shutdown failed")
		httpServer.Close()
	}

	// Shutdown WebSocket server (closes all active connections)
	if err := wsServer.Shutdown(shutdownCtx); err != nil {
		utils.Log.Error().Err(err).Msg("websocket server shutdown failed")
	}

	kafkaProducer.Close()
	redisHub.Close()
	utils.Log.Info().Msg("server shutdown complete")
}
