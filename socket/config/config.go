package config

import (
	"os"
	"strconv"
	"strings"
)

type Config struct {
	AppName       string
	Port          int
	HostName      string
	JWKSURL       string
	KafkaBrokers  []string
	KafkaTopic    string
	RedisAddr     string
	RedisPassword string
	RedisDB       int
	ZipkinURL     string
	EurekaURL     string
	Debug         bool
}

func envOrDefault(key, def string) string {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	return v
}

func LoadFromEnv() *Config {
	port, _ := strconv.Atoi(envOrDefault("SOCKET_PORT", "7200"))
	db, _ := strconv.Atoi(envOrDefault("REDIS_DB", "0"))
	debug := envOrDefault("DEBUG", "false") == "true"
	kafkaBrokersEnv := envOrDefault("KAFKA_BROKERS", "localhost:9092")
	kafkaBrokers := strings.Split(kafkaBrokersEnv, ",")

	return &Config{
		AppName:       envOrDefault("APP_NAME", "socket-service"),
		Port:          port,
		HostName:      envOrDefault("HOST_NAME", "localhost"),
		JWKSURL:       envOrDefault("JWKS_URL", "http://localhost:7100/auth/.well-known/jwks.json"),
		KafkaBrokers:  kafkaBrokers,
		KafkaTopic:    envOrDefault("KAFKA_TOPIC", "chat-messages"),
		RedisAddr:     envOrDefault("REDIS_ADDR", "localhost:6379"),
		RedisPassword: envOrDefault("REDIS_PASSWORD", "strongredispassword"),
		RedisDB:       db,
		ZipkinURL:     envOrDefault("ZIPKIN_URL", "http://localhost:9411/api/v2/spans"),
		EurekaURL:     envOrDefault("EUREKA_URL", "http://localhost:8761/eureka"),
		Debug:         debug,
	}
}
