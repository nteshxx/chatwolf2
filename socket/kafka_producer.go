package main

import (
    "encoding/json"
    "github.com/Shopify/sarama"
    "time"
)

type KafkaProducer struct {
    asyncProducer sarama.AsyncProducer
    topic         string
}

func NewKafkaProducer(brokers []string, topic string) (*KafkaProducer, error) {
    config := sarama.NewConfig()
    config.Producer.Return.Successes = false
    config.Producer.Return.Errors = true
    config.Producer.RequiredAcks = sarama.WaitForLocal
    config.Producer.Retry.Max = 5
    config.Producer.Retry.Backoff = 100 * time.Millisecond
    p, err := sarama.NewAsyncProducer(brokers, config)
    if err != nil {
        return nil, err
    }

    kp := &KafkaProducer{asyncProducer: p, topic: topic}
    go func() {
        for err := range p.Errors() {
            logger.Error().Err(err.Err).Msg("kafka produce error")
        }
    }()
    return kp, nil
}

func (kp *KafkaProducer) PublishMessage(event ChatMessageEvent) error {
    b, err := json.Marshal(event)
    if err != nil {
        return err
    }
    kp.asyncProducer.Input() <- &sarama.ProducerMessage{
        Topic: kp.topic,
        Key:   sarama.StringEncoder(event.Conversation),
        Value: sarama.ByteEncoder(b),
    }
    return nil
}

func (kp *KafkaProducer) Close() error {
    return kp.asyncProducer.Close()
}
