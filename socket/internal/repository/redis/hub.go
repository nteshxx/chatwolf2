package redis

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/nteshxx/chatwolf2/socket/internal/domain"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/logger"
	"github.com/redis/go-redis/v9"
)

const (
	messageChannel = "message:events"
)

// MessageHandler handles incoming messages from Redis
type MessageHandler func(ctx context.Context, event domain.MessageEvent)

// PresenceHandler handles incoming presence events from Redis
type PresenceHandler func(ctx context.Context, event domain.PresenceEvent)

// Hub manages Redis pub/sub for cross-instance communication
type Hub struct {
	client          *redis.Client
	log             *logger.Logger
	messageHandler  MessageHandler
	presenceHandler PresenceHandler
	pubsub          *redis.PubSub
	done            chan struct{}
}

// NewHub creates a new Redis hub
func NewHub(ctx context.Context, addr, password string, db int, log *logger.Logger) (*Hub, error) {
	client := redis.NewClient(&redis.Options{
		Addr:         addr,
		Password:     password,
		DB:           db,
		DialTimeout:  5 * time.Second,
		ReadTimeout:  3 * time.Second,
		WriteTimeout: 3 * time.Second,
		PoolSize:     20,
		MinIdleConns: 5,
		MaxRetries:   3,
	})

	// Test connection
	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("failed to connect to Redis: %w", err)
	}

	log.Info(ctx, "redis hub connected", map[string]interface{}{
		"addr": addr,
		"db":   db,
	})

	return &Hub{
		client: client,
		log:    log,
		done:   make(chan struct{}),
	}, nil
}

// GetClient returns the underlying Redis client
func (h *Hub) GetClient() *redis.Client {
	return h.client
}

// SetHandlers sets the message and presence handlers
func (h *Hub) SetHandlers(messageHandler MessageHandler, presenceHandler PresenceHandler) {
	h.messageHandler = messageHandler
	h.presenceHandler = presenceHandler
}

// PublishMessage publishes a message event to Redis for cross-instance delivery
func (h *Hub) PublishMessage(ctx context.Context, event domain.MessageEvent) error {
	data, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to marshal message event: %w", err)
	}

	publishCtx, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()

	if err := h.client.Publish(publishCtx, messageChannel, data).Err(); err != nil {
		return fmt.Errorf("failed to publish message to Redis: %w", err)
	}

	h.log.Debug(ctx, "message published to redis", map[string]interface{}{
		"event_id": event.EventID,
		"to":       event.To,
		"channel":  messageChannel,
	})

	return nil
}

// StartSubscriber starts listening to Redis pub/sub channels
func (h *Hub) StartSubscriber(ctx context.Context) error {
	// Subscribe to message channel
	h.pubsub = h.client.Subscribe(ctx, messageChannel)

	// Wait for subscription confirmation
	if _, err := h.pubsub.Receive(ctx); err != nil {
		return fmt.Errorf("failed to subscribe to Redis channels: %w", err)
	}

	h.log.Info(ctx, "redis subscriber started", map[string]interface{}{
		"channel": messageChannel,
	})

	// Start message processing goroutine
	go h.processMessages(ctx)

	return nil
}

// processMessages processes incoming messages from Redis pub/sub
func (h *Hub) processMessages(ctx context.Context) {
	ch := h.pubsub.Channel()

	for {
		select {
		case <-h.done:
			h.log.Info(ctx, "redis subscriber stopped", nil)
			return
		case msg, ok := <-ch:
			if !ok {
				h.log.Warn(ctx, "redis pubsub channel closed", nil)
				return
			}

			h.handleMessage(ctx, msg)
		}
	}
}

// handleMessage handles a single message from Redis pub/sub
func (h *Hub) handleMessage(ctx context.Context, msg *redis.Message) {
	switch msg.Channel {
	case messageChannel:
		var event domain.MessageEvent
		if err := json.Unmarshal([]byte(msg.Payload), &event); err != nil {
			h.log.Error(ctx, "failed to unmarshal message event", err)
			return
		}

		if h.messageHandler != nil {
			h.messageHandler(ctx, event)
		}

	default:
		h.log.Warn(ctx, "received message from unknown channel", map[string]interface{}{
			"channel": msg.Channel,
		})
	}
}

// Close closes the Redis hub and stops the subscriber
func (h *Hub) Close(ctx context.Context) error {
	h.log.Info(ctx, "closing redis hub", nil)

	// Signal subscriber to stop
	close(h.done)

	// Close pubsub
	if h.pubsub != nil {
		if err := h.pubsub.Close(); err != nil {
			h.log.Error(ctx, "failed to close pubsub", err)
		}
	}

	// Close Redis client
	if err := h.client.Close(); err != nil {
		return fmt.Errorf("failed to close redis client: %w", err)
	}

	h.log.Info(ctx, "redis hub closed", nil)
	return nil
}

// Ping checks if Redis is healthy
func (h *Hub) Ping(ctx context.Context) error {
	return h.client.Ping(ctx).Err()
}
