package websockets

import (
	"context"
	"encoding/json"
	"sync"

	"github.com/nteshxx/chatwolf2/socket/internal/kafka"
	"github.com/nteshxx/chatwolf2/socket/internal/utils"
	"github.com/redis/go-redis/v9"
)

type RedisHub struct {
	client   *redis.Client
	ctx      context.Context
	cancel   context.CancelFunc
	server   *Server
	pubsub   *redis.PubSub
	wg       sync.WaitGroup
	shutdown chan struct{}
}

func NewRedisHub(addr, pass string, db int) (*RedisHub, error) {
	r := redis.NewClient(&redis.Options{
		Addr:     addr,
		Password: pass,
		DB:       db,
	})

	if err := r.Ping(context.Background()).Err(); err != nil {
		return nil, err
	}

	ctx, cancel := context.WithCancel(context.Background())
	return &RedisHub{
		client:   r,
		ctx:      ctx,
		cancel:   cancel,
		shutdown: make(chan struct{}),
	}, nil
}

func (h *RedisHub) SetServer(s *Server) {
	h.server = s
}

func (h *RedisHub) StartSubscriber() {
	if h.server == nil {
		utils.Log.Fatal().Msg("server not set before starting subscriber")
		return
	}

	h.pubsub = h.client.Subscribe(h.ctx, "message-deliver", "presence-events")
	ch := h.pubsub.Channel()

	h.wg.Add(1)
	go func() {
		defer h.wg.Done()
		utils.Log.Info().Msg("redis subscriber started")

		for {
			select {
			case <-h.shutdown:
				utils.Log.Info().Msg("redis subscriber shutting down")
				return
			case msg, ok := <-ch:
				if !ok {
					utils.Log.Warn().Msg("redis channel closed")
					return
				}
				h.handleRedisMessage(msg)
			}
		}
	}()
}

func (h *RedisHub) handleRedisMessage(msg *redis.Message) {
	switch msg.Channel {
	case "message-deliver":
		var ev kafka.MessageEvent
		if err := json.Unmarshal([]byte(msg.Payload), &ev); err != nil {
			utils.Log.Error().Err(err).Msg("invalid message-deliver payload")
			return
		}

		// Deliver to local clients if they exist
		// This handles cross-instance message delivery
		h.server.deliverToUser(ev.To, ev)

		utils.Log.Debug().
			Str("from", ev.From).
			Str("to", ev.To).
			Str("msgId", ev.EventID).
			Msg("message delivered via redis")

	case "presence-events":
		// Handle presence events if needed
		var presenceEvent map[string]interface{}
		if err := json.Unmarshal([]byte(msg.Payload), &presenceEvent); err != nil {
			utils.Log.Error().Err(err).Msg("invalid presence-events payload")
			return
		}

		utils.Log.Debug().
			Str("userId", presenceEvent["userId"].(string)).
			Str("status", presenceEvent["status"].(string)).
			Msg("presence event received")

	default:
		utils.Log.Warn().Str("channel", msg.Channel).Msg("unknown redis channel")
	}
}

func (h *RedisHub) PublishDeliver(ev kafka.MessageEvent) error {
	b, err := json.Marshal(ev)
	if err != nil {
		return err
	}
	return h.client.Publish(h.ctx, "message-deliver", b).Err()
}

func (h *RedisHub) PublishPresence(ev map[string]interface{}) error {
	b, err := json.Marshal(ev)
	if err != nil {
		return err
	}
	return h.client.Publish(h.ctx, "presence-events", b).Err()
}

func (h *RedisHub) Close() error {
	utils.Log.Info().Msg("closing redis hub")
	close(h.shutdown)

	// Close pubsub first
	if h.pubsub != nil {
		if err := h.pubsub.Close(); err != nil {
			utils.Log.Error().Err(err).Msg("error closing pubsub")
		}
	}

	// Wait for subscriber goroutine
	h.wg.Wait()

	// Cancel context and close client
	h.cancel()
	return h.client.Close()
}
