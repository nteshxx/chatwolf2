package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

func getEnv(key, defaultVal string) string {
	v := os.Getenv(key)
	if v == "" {
		return defaultVal
	}
	return v
}

func getEnvAsInt(key string, defaultVal int) int {
	v := os.Getenv(key)
	if v == "" {
		return defaultVal
	}

	val, err := strconv.Atoi(v)
	if err != nil {
		return defaultVal
	}

	return val
}

func getEnvAsFloat(key string, defaultVal float64) float64 {
	v := os.Getenv(key)
	if v == "" {
		return defaultVal
	}

	val, err := strconv.ParseFloat(v, 64)
	if err != nil {
		return defaultVal
	}

	return val
}

type Config struct {
	Server   ServerConfig
	Auth     AuthConfig
	Kafka    KafkaConfig
	Redis    RedisConfig
	Zipkin   ZipkinConfig
	Eureka   EurekaConfig
	Logging  LoggingConfig
	Presence PresenceConfig
}

type ServerConfig struct {
	AppName      string
	Port         int
	Host         string
	ReadTimeout  time.Duration
	WriteTimeout time.Duration
	IdleTimeout  time.Duration
}

type AuthConfig struct {
	JWKSURL string
}

type KafkaConfig struct {
	Brokers []string
	Topic   string
}

type RedisConfig struct {
	Addr     string
	Password string
	DB       int
}

type ZipkinConfig struct {
	URL          string
	SampleRate   float64
	BatchSize    int
	BatchTimeout time.Duration
}

type EurekaConfig struct {
	URL string
}

type LoggingConfig struct {
	Level                  string
	ExcludeHealthEndpoint  bool
	ExcludeMetricsEndpoint bool
}

type PresenceConfig struct {
	HeartbeatInterval string
	Channel           string
}

func Load() (*Config, error) {
	cfg := &Config{
		Server: ServerConfig{
			AppName:      "socket",
			Port:         getEnvAsInt("SOCKET_SERVICE_PORT", 7200),
			Host:         getEnv("HOST_NAME", "localhost"),
			ReadTimeout:  15 * time.Second,
			WriteTimeout: 15 * time.Second,
			IdleTimeout:  60 * time.Second,
		},
		Auth: AuthConfig{
			JWKSURL: getEnv("JWKS_URL", "http://localhost:7100/api/auth/.well-known/jwks.json"),
		},
		Kafka: KafkaConfig{
			Brokers: strings.Split(getEnv("KAFKA_BROKERS", "localhost:9092"), ","),
			Topic:   getEnv("KAFKA_TOPIC", "chat-messages"),
		},
		Redis: RedisConfig{
			Addr:     fmt.Sprintf("%s:%s", getEnv("REDIS_HOST", "localhost"), getEnv("REDIS_PORT", "6379")),
			Password: getEnv("REDIS_PASSWORD", "strongredispassword"),
			DB:       getEnvAsInt("REDIS_DB", 0),
		},
		Zipkin: ZipkinConfig{
			URL:          getEnv("ZIPKIN_URL", "http://localhost:9411/api/v2/spans"),
			SampleRate:   getEnvAsFloat("ZIPKIN_SAMPLING_RATE", 1.0),
			BatchSize:    100,
			BatchTimeout: 5 * time.Second,
		},
		Eureka: EurekaConfig{
			URL: getEnv("EUREKA_URL", "http://localhost:8761/eureka"),
		},
		Logging: LoggingConfig{
			Level:                  getEnv("LOG_LEVEL", "info"),
			ExcludeHealthEndpoint:  true,
			ExcludeMetricsEndpoint: true,
		},
		Presence: PresenceConfig{
			HeartbeatInterval: "30s",
			Channel:           "presence:channel",
		},
	}

	if err := cfg.Validate(); err != nil {
		return nil, fmt.Errorf("invalid configuration: %w", err)
	}

	return cfg, nil
}

func (c *Config) Validate() error {
	if c.Server.Port < 1 || c.Server.Port > 65535 {
		return fmt.Errorf("invalid port: %d", c.Server.Port)
	}
	if c.Auth.JWKSURL == "" {
		return fmt.Errorf("JWKS URL is required")
	}
	if len(c.Kafka.Brokers) == 0 {
		return fmt.Errorf("at least one Kafka broker is required")
	}
	if c.Redis.Addr == "" {
		return fmt.Errorf("redis address is required")
	}
	return nil
}
