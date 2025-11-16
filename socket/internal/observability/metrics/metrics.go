package metrics

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

// Metrics holds all Prometheus metrics for the WebSocket server
type Metrics struct {
	// Connection metrics
	ActiveConnections prometheus.Gauge

	// Message metrics
	MessagesProcessed      prometheus.Counter
	MessagesReceived       prometheus.Counter
	MessagesFailed         prometheus.Counter
	MessagesDeliveredLocal prometheus.Counter
	MessagesKafkaPublished prometheus.Counter

	// Presence metrics
	PresencePublished     prometheus.Counter
	PresencePublishFailed prometheus.Counter

	// Performance metrics
	MessageProcessingDuration   prometheus.Histogram
	WebSocketConnectionDuration prometheus.Histogram
}

// NewMetrics creates and registers all Prometheus metrics
func NewMetrics() *Metrics {
	return &Metrics{
		ActiveConnections: promauto.NewGauge(prometheus.GaugeOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "active_connections",
			Help:      "Number of active WebSocket connections",
		}),

		MessagesProcessed: promauto.NewCounter(prometheus.CounterOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "messages_processed_total",
			Help:      "Total number of messages processed",
		}),

		MessagesReceived: promauto.NewCounter(prometheus.CounterOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "messages_received",
			Help:      "Total number of messages received",
		}),

		MessagesKafkaPublished: promauto.NewCounter(prometheus.CounterOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "messages_kafka_published",
			Help:      "Total number of messages published to kafka",
		}),

		MessagesFailed: promauto.NewCounter(prometheus.CounterOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "messages_failed_total",
			Help:      "Total number of failed messages",
		}),

		MessagesDeliveredLocal: promauto.NewCounter(prometheus.CounterOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "messages_delivered_local_total",
			Help:      "Total number of messages delivered locally",
		}),

		PresencePublished: promauto.NewCounter(prometheus.CounterOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "presence_published_total",
			Help:      "Total number of presence events published",
		}),

		PresencePublishFailed: promauto.NewCounter(prometheus.CounterOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "presence_publish_failed_total",
			Help:      "Total number of failed presence publishes",
		}),

		MessageProcessingDuration: promauto.NewHistogram(prometheus.HistogramOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "message_processing_duration_seconds",
			Help:      "Duration of message processing",
			Buckets:   prometheus.DefBuckets,
		}),

		WebSocketConnectionDuration: promauto.NewHistogram(prometheus.HistogramOpts{
			Namespace: "chatwolf",
			Subsystem: "websocket",
			Name:      "connection_duration_seconds",
			Help:      "Duration of WebSocket connections",
			Buckets:   []float64{1, 5, 10, 30, 60, 300, 600, 1800, 3600},
		}),
	}
}
