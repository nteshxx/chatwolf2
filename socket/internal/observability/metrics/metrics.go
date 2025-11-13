package metrics

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

type Metrics struct {
	MessagesReceived       prometheus.Counter
	MessagesKafkaPublished prometheus.Counter
	MessagesDeliveredLocal prometheus.Counter
	MessagesDeliveredRedis prometheus.Counter
	ActiveConnections      prometheus.Gauge
	MessageProcessingTime  prometheus.Histogram
}

func New() *Metrics {
	return &Metrics{
		MessagesReceived: promauto.NewCounter(prometheus.CounterOpts{
			Name: "socket_messages_received_total",
			Help: "Total number of incoming messages",
		}),
		MessagesKafkaPublished: promauto.NewCounter(prometheus.CounterOpts{
			Name: "socket_messages_kafka_published_total",
			Help: "Total messages published to Kafka",
		}),
		MessagesDeliveredLocal: promauto.NewCounter(prometheus.CounterOpts{
			Name: "socket_messages_delivered_local_total",
			Help: "Total messages delivered to local clients",
		}),
		MessagesDeliveredRedis: promauto.NewCounter(prometheus.CounterOpts{
			Name: "socket_messages_delivered_redis_total",
			Help: "Total messages delivered via Redis pub/sub",
		}),
		ActiveConnections: promauto.NewGauge(prometheus.GaugeOpts{
			Name: "socket_active_connections",
			Help: "Number of active WebSocket connections",
		}),
		MessageProcessingTime: promauto.NewHistogram(prometheus.HistogramOpts{
			Name:    "socket_message_processing_seconds",
			Help:    "Time taken to process messages",
			Buckets: prometheus.DefBuckets,
		}),
	}
}
