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
	// Redis channel for presence events
	presenceChannel = "presence:channel"

	// Timeouts
	publishTimeout = 2 * time.Second
)

// PresencePublisher publishes presence events to Redis
type PresencePublisher struct {
	client  *redis.Client
	log     *logger.Logger
	channel string
}

// NewPresencePublisher creates a new Redis-based presence publisher
func NewPresencePublisher(client *redis.Client, log *logger.Logger) *PresencePublisher {
	return &PresencePublisher{
		client:  client,
		log:     log,
		channel: presenceChannel,
	}
}

// PublishPresence publishes a presence event to Redis pub/sub
func (p *PresencePublisher) PublishPresence(ctx context.Context, event domain.PresenceEvent) error {
	// Create a timeout context for the publish operation
	publishCtx, cancel := context.WithTimeout(ctx, publishTimeout)
	defer cancel()

	// Validate event
	if err := p.validateEvent(event); err != nil {
		return fmt.Errorf("invalid presence event: %w", err)
	}

	// Marshal event to JSON
	data, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to marshal presence event: %w", err)
	}

	// Publish to Redis channel
	if err := p.client.Publish(publishCtx, p.channel, data).Err(); err != nil {
		p.log.Error(ctx, "failed to publish presence event to Redis", err)
		return fmt.Errorf("failed to publish to Redis: %w", err)
	}

	p.log.Debug(ctx, "presence event published", map[string]interface{}{
		"event_id": event.EventID,
		"user_id":  event.UserID,
		"status":   event.Status,
		"channel":  p.channel,
	})

	return nil
}

// validateEvent validates presence event fields
func (p *PresencePublisher) validateEvent(event domain.PresenceEvent) error {
	if event.EventID == "" {
		return fmt.Errorf("event_id is required")
	}
	if event.UserID == "" {
		return fmt.Errorf("user_id is required")
	}
	if event.Status == "" {
		return fmt.Errorf("status is required")
	}
	if event.Timestamp.IsZero() {
		return fmt.Errorf("timestamp is required")
	}
	return nil
}

// Ping checks if Redis connection is healthy
func (p *PresencePublisher) Ping(ctx context.Context) error {
	pingCtx, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()

	if err := p.client.Ping(pingCtx).Err(); err != nil {
		return fmt.Errorf("redis ping failed: %w", err)
	}
	return nil
}

// Close closes the Redis client connection
func (p *PresencePublisher) Close() error {
	return p.client.Close()
}
