package service

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/nteshxx/chatwolf2/socket/internal/domain"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/logger"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/metrics"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/tracing"
)

type KafkaPublisher interface {
	PublishMessage(ctx context.Context, event domain.MessageEvent) error
}

type RedisPublisher interface {
	PublishMessage(ctx context.Context, event domain.MessageEvent) error
}

type MessageDeliverer interface {
	DeliverToUser(ctx context.Context, userID string, event domain.MessageEvent) error
}

type MessageService struct {
	kafka     KafkaPublisher
	redis     RedisPublisher
	deliverer MessageDeliverer
	log       *logger.Logger
	metrics   *metrics.Metrics
	tracer    *tracing.Tracer
}

func NewMessageService(
	kafka KafkaPublisher,
	redis RedisPublisher,
	deliverer MessageDeliverer,
	log *logger.Logger,
	metrics *metrics.Metrics,
	tracer *tracing.Tracer,
) *MessageService {
	return &MessageService{
		kafka:     kafka,
		redis:     redis,
		deliverer: deliverer,
		log:       log,
		metrics:   metrics,
		tracer:    tracer,
	}
}

func (s *MessageService) HandleMessage(ctx context.Context, from string, msg domain.Message) (domain.MessageEvent, error) {
	start := time.Now()
	defer func() {
		s.metrics.MessageProcessingDuration.Observe(time.Since(start).Seconds())
	}()

	s.metrics.MessagesReceived.Inc()

	span, ctx := s.tracer.StartSpan(ctx, "MessageService.HandleMessage")
	defer span.Finish()

	span.Tag("from", from)
	span.Tag("to", msg.To)
	span.Tag("client_msg_id", msg.ClientMsgID)

	event := domain.MessageEvent{
		EventID:      uuid.NewString(),
		ClientMsgID:  msg.ClientMsgID,
		From:         from,
		To:           msg.To,
		Conversation: msg.Conversation,
		Content:      msg.Content,
		Attachment:   msg.Attachment,
		SentAt:       time.Now(),
	}

	// Publish to Kafka for persistence
	if err := s.kafka.PublishMessage(ctx, event); err != nil {
		s.log.Error(ctx, "kafka publish failed", err, map[string]interface{}{
			"event_id": event.EventID,
		})
		span.Tag("error", "true")
		span.Tag("error.type", "kafka_error")
		return event, err
	}
	s.metrics.MessagesKafkaPublished.Inc()

	// Publish to Redis for cross-instance delivery
	if err := s.redis.PublishMessage(ctx, event); err != nil {
		s.log.Error(ctx, "redis publish failed", err, map[string]interface{}{
			"event_id": event.EventID,
		})
		span.Tag("error", "true")
		span.Tag("error.type", "redis_error")
		// Don't fail the request, local delivery might still work
	}

	// Try local delivery
	if err := s.deliverer.DeliverToUser(ctx, msg.To, event); err != nil {
		s.log.Debug(ctx, "local delivery not available", map[string]interface{}{
			"to":       msg.To,
			"event_id": event.EventID,
		})
	}

	span.Tag("server_msg_id", event.EventID)
	return event, nil
}
