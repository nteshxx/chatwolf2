package redis

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"

	"github.com/nteshxx/chatwolf2/socket/internal/domain"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/logger"
	apperrors "github.com/nteshxx/chatwolf2/socket/pkg/errors"
	"github.com/redis/go-redis/v9"
)

const (
	MessageDeliverChannel = "message-deliver"
	PresenceEventsChannel = "presence-events"
)

type MessageHandler func(ctx context.Context, event domain.MessageEvent)
type PresenceHandler func(ctx context.Context, event domain.PresenceEvent)

type Hub struct {
	client          *redis.Client
	log             *logger.Logger
	pubsub          *redis.PubSub
	messageHandler  MessageHandler
	presenceHandler PresenceHandler
	wg              sync.WaitGroup
	cancel          context.CancelFunc
}

func NewHub(ctx context.Context, addr, password string, db int, log *logger.Logger) (*Hub, error) {
	client := redis.NewClient(&redis.Options{
		Addr:         addr,
		Password:     password,
		DB:           db,
		PoolSize:     10,
		MinIdleConns: 5,
	})

	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("failed to connect to redis: %w", err)
	}

	ctx, cancel := context.WithCancel(ctx)

	hub := &Hub{
		client: client,
		log:    log,
		cancel: cancel,
	}

	log.Info(ctx, "redis hub initialized", map[string]interface{}{
		"addr": addr,
		"db":   db,
	})

	return hub, nil
}

func (h *Hub) SetHandlers(messageHandler MessageHandler, presenceHandler PresenceHandler) {
	h.messageHandler = messageHandler
	h.presenceHandler = presenceHandler
}

func (h *Hub) StartSubscriber(ctx context.Context) error {
	if h.messageHandler == nil {
		return fmt.Errorf("message handler not set")
	}

	h.pubsub = h.client.Subscribe(ctx, MessageDeliverChannel, PresenceEventsChannel)

	// Wait for subscription confirmation
	if _, err := h.pubsub.Receive(ctx); err != nil {
		return fmt.Errorf("failed to confirm subscription: %w", err)
	}

	ch := h.pubsub.Channel()

	h.wg.Add(1)
	go func() {
		defer h.wg.Done()
		h.log.Info(ctx, "redis subscriber started")

		for {
			select {
			case <-ctx.Done():
				h.log.Info(ctx, "redis subscriber shutting down")
				return
			case msg, ok := <-ch:
				if !ok {
					h.log.Warn(ctx, "redis channel closed")
					return
				}
				h.handleMessage(ctx, msg)
			}
		}
	}()

	return nil
}

func (h *Hub) handleMessage(ctx context.Context, msg *redis.Message) {
	switch msg.Channel {
	case MessageDeliverChannel:
		var event domain.MessageEvent
		if err := json.Unmarshal([]byte(msg.Payload), &event); err != nil {
			h.log.Error(ctx, "invalid message-deliver payload", err)
			return
		}

		if h.messageHandler != nil {
			h.messageHandler(ctx, event)
		}

		h.log.Debug(ctx, "message delivered via redis", map[string]interface{}{
			"from":   event.From,
			"to":     event.To,
			"msg_id": event.EventID,
		})

	case PresenceEventsChannel:
		var event domain.PresenceEvent
		if err := json.Unmarshal([]byte(msg.Payload), &event); err != nil {
			h.log.Error(ctx, "invalid presence-events payload", err)
			return
		}

		if h.presenceHandler != nil {
			h.presenceHandler(ctx, event)
		}

		h.log.Debug(ctx, "presence event received", map[string]interface{}{
			"user_id": event.UserID,
			"status":  event.Status,
		})

	default:
		h.log.Warn(ctx, "unknown redis channel", map[string]interface{}{
			"channel": msg.Channel,
		})
	}
}

func (h *Hub) PublishMessage(ctx context.Context, event domain.MessageEvent) error {
	data, err := json.Marshal(event)
	if err != nil {
		return apperrors.NewAppError(err, "failed to marshal message", "REDIS_001")
	}

	if err := h.client.Publish(ctx, MessageDeliverChannel, data).Err(); err != nil {
		return apperrors.NewAppError(err, "failed to publish message", "REDIS_002")
	}

	return nil
}

func (h *Hub) PublishPresence(ctx context.Context, event domain.PresenceEvent) error {
	data, err := json.Marshal(event)
	if err != nil {
		return apperrors.NewAppError(err, "failed to marshal presence", "REDIS_003")
	}

	if err := h.client.Publish(ctx, PresenceEventsChannel, data).Err(); err != nil {
		return apperrors.NewAppError(err, "failed to publish presence", "REDIS_004")
	}

	return nil
}

func (h *Hub) Close(ctx context.Context) error {
	h.log.Info(ctx, "closing redis hub")

	h.cancel()

	if h.pubsub != nil {
		if err := h.pubsub.Close(); err != nil {
			h.log.Error(ctx, "error closing pubsub", err)
		}
	}

	h.wg.Wait()

	return h.client.Close()
}
