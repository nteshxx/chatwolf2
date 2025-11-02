package main

import (
    "github.com/prometheus/client_golang/prometheus"
)

var (
    messagesReceivedCounter = prometheus.NewCounter(prometheus.CounterOpts{
        Name: "socket_messages_received_total",
        Help: "Total incoming messages received by socket server",
    })
    messagesKafkaPublishedCounter = prometheus.NewCounter(prometheus.CounterOpts{
        Name: "socket_messages_kafka_published_total",
        Help: "Total messages published to kafka",
    })
    messagesDeliveredLocalCounter = prometheus.NewCounter(prometheus.CounterOpts{
        Name: "socket_messages_delivered_local_total",
        Help: "Total messages delivered to local connected clients",
    })
)

func initMetrics() {
    prometheus.MustRegister(messagesReceivedCounter)
    prometheus.MustRegister(messagesKafkaPublishedCounter)
    prometheus.MustRegister(messagesDeliveredLocalCounter)
}
