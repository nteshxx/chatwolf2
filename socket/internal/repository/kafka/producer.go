package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/IBM/sarama"
	"github.com/nteshxx/chatwolf2/socket/internal/domain"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/logger"
	apperrors "github.com/nteshxx/chatwolf2/socket/pkg/errors"
)

type Producer struct {
	producer sarama.AsyncProducer
	topic    string
	log      *logger.Logger
	errChan  chan error
}

func NewProducer(ctx context.Context, brokers []string, topic string, log *logger.Logger) (*Producer, error) {
	config := sarama.NewConfig()
	config.Producer.Return.Successes = false
	config.Producer.Return.Errors = true
	config.Producer.RequiredAcks = sarama.WaitForLocal
	config.Producer.Retry.Max = 5
	config.Producer.Retry.Backoff = 100 * time.Millisecond
	config.Producer.Compression = sarama.CompressionGZIP

	p, err := sarama.NewAsyncProducer(brokers, config)
	if err != nil {
		return nil, fmt.Errorf("failed to create kafka producer: %w", err)
	}

	producer := &Producer{
		producer: p,
		topic:    topic,
		log:      log,
		errChan:  make(chan error, 100),
	}

	// Start error handler
	go producer.handleErrors(ctx)

	log.Info(ctx, "kafka producer initialized", map[string]interface{}{
		"brokers": brokers,
		"topic":   topic,
	})

	return producer, nil
}

func (p *Producer) handleErrors(ctx context.Context) {
	for {
		select {
		case <-ctx.Done():
			return
		case err := <-p.producer.Errors():
			if err != nil {
				p.log.Error(ctx, "kafka produce error", err.Err, map[string]interface{}{
					"topic": err.Msg.Topic,
					"key":   string(err.Msg.Key.(sarama.StringEncoder)),
				})
				select {
				case p.errChan <- err.Err:
				default:
				}
			}
		}
	}
}

func (p *Producer) PublishMessage(ctx context.Context, event domain.MessageEvent) error {
	data, err := json.Marshal(event)
	if err != nil {
		return apperrors.NewAppError(err, "failed to marshal message", "KAFKA_001")
	}

	msg := &sarama.ProducerMessage{
		Topic: p.topic,
		Key:   sarama.StringEncoder(event.Conversation),
		Value: sarama.ByteEncoder(data),
	}

	select {
	case p.producer.Input() <- msg:
		return nil
	case <-ctx.Done():
		return ctx.Err()
	case <-time.After(5 * time.Second):
		return apperrors.ErrKafkaPublish
	}
}

func (p *Producer) Close(ctx context.Context) error {
	p.log.Info(ctx, "closing kafka producer")
	return p.producer.Close()
}
