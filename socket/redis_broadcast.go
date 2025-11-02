package main

import (
    "context"
    "encoding/json"
    "github.com/redis/go-redis/v9"
    "time"
)

type RedisHub struct {
    client *redis.Client
    ctx    context.Context
    server *Server
}

func NewRedisHub(addr, password string, db int, server *Server) (*RedisHub, error) {
    r := redis.NewClient(&redis.Options{
        Addr:     addr,
        Password: password,
        DB:       db,
    })
    ctx := context.Background()
    if err := r.Ping(ctx).Err(); err != nil {
        return nil, err
    }
    hub := &RedisHub{client: r, ctx: ctx, server: server}
    return hub, nil
}

func (h *RedisHub) StartSubscriber() {
    pubsub := h.client.Subscribe(h.ctx, "message-deliver", "presence-events")
    go func() {
        ch := pubsub.Channel()
        for msg := range ch {
            switch msg.Channel {
            case "message-deliver":
                var ev ChatMessageEvent
                if err := json.Unmarshal([]byte(msg.Payload), &ev); err != nil {
                    logger.Error().Err(err).Msg("invalid deliver payload")
                    continue
                }
                h.server.clientsMutex.RLock()
                recipient, ok := h.server.clients[ev.To]
                h.server.clientsMutex.RUnlock()
                if ok {
                    payload, _ := json.Marshal(map[string]interface{}{
                        "type": "message.create",
                        "data": ev,
                    })
                    recipient.sendMu.Lock()
                    _ = recipient.conn.WriteMessage(websocket.TextMessage, payload)
                    recipient.sendMu.Unlock()
                    messagesDeliveredLocal.Inc()
                }
            case "presence-events":
            }
        }
    }()
}

func (h *RedisHub) PublishDeliver(ev ChatMessageEvent) error {
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
