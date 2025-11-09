package metrics

import (
	"github.com/prometheus/client_golang/prometheus"
)

var (
	MessagesReceived       = prometheus.NewCounter(prometheus.CounterOpts{Name: "socket_messages_received_total", Help: "Total incoming messages"})
	MessagesKafkaPublished = prometheus.NewCounter(prometheus.CounterOpts{Name: "socket_messages_kafka_published_total", Help: "Total messages published to kafka"})
	MessagesDeliveredLocal = prometheus.NewCounter(prometheus.CounterOpts{Name: "socket_messages_delivered_local_total", Help: "Total messages delivered locally"})
)

func InitMetrics() {
	prometheus.MustRegister(MessagesReceived, MessagesKafkaPublished, MessagesDeliveredLocal)
}
