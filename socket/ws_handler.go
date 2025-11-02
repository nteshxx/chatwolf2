package main

import (
    "context"
    "encoding/json"
    "github.com/gorilla/websocket"
    "github.com/google/uuid"
    "go.opentelemetry.io/otel/attribute"
    "go.opentelemetry.io/otel/trace"
    "net/http"
    "sync"
    "time"
)

var upgrader = websocket.Upgrader{
    CheckOrigin: func(r *http.Request) bool { return true },
}

type Client struct {
    userID string
    conn   *websocket.Conn
    sendMu sync.Mutex
}

type Server struct {
    validator    *JwtValidator
    producer     *KafkaProducer
    redisHub     *RedisHub
    clientsMutex sync.RWMutex
    clients      map[string]*Client
    tracer       trace.Tracer
}

func NewServer(validator *JwtValidator, producer *KafkaProducer, redisHub *RedisHub, tracer trace.Tracer) *Server {
    return &Server{
        validator: validator,
        producer:  producer,
        redisHub:  redisHub,
        clients:   make(map[string]*Client),
        tracer:    tracer,
    }
}

func (s *Server) handleWS(w http.ResponseWriter, r *http.Request) {
    token := r.URL.Query().Get("token")
    if token == "" {
        ah := r.Header.Get("Authorization")
        if len(ah) > 7 && ah[:7] == "Bearer " {
            token = ah[7:]
        }
    }

    ctx := r.Context()
    userID, err := s.validator.ValidateToken(ctx, token)
    if err != nil {
        logger.Info().Err(err).Msg("unauthorized ws connection attempt")
        http.Error(w, "unauthorized", http.StatusUnauthorized)
        return
    }

    conn, err := upgrader.Upgrade(w, r, nil)
    if err != nil {
        logger.Error().Err(err).Msg("upgrade failed")
        return
    }

    client := &Client{userID: userID, conn: conn}
    s.registerClient(client)
    logger.Info().Str("user", userID).Msg("client connected")

    s.redisHub.PublishPresence(map[string]interface{}{
        "eventId": uuid.NewString(),
        "userId": userID,
        "status": "online",
        "timestamp": time.Now(),
    })

    go s.readLoop(client)
}

func (s *Server) registerClient(c *Client) {
    s.clientsMutex.Lock()
    s.clients[c.userID] = c
    s.clientsMutex.Unlock()
}

func (s *Server) unregisterClient(c *Client) {
    s.clientsMutex.Lock()
    existing, ok := s.clients[c.userID]
    if ok && existing == c {
        delete(s.clients, c.userID)
    }
    s.clientsMutex.Unlock()
}

func (s *Server) readLoop(c *Client) {
    defer func() {
        s.unregisterClient(c)
        c.conn.Close()
        s.redisHub.PublishPresence(map[string]interface{}{
            "eventId": uuid.NewString(),
            "userId": c.userID,
            "status": "offline",
            "timestamp": time.Now(),
        })
        logger.Info().Str("user", c.userID).Msg("client disconnected")
    }()

    c.conn.SetReadLimit(1024 * 1024)
    _ = c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
    c.conn.SetPongHandler(func(string) error {
        _ = c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
        return nil
    })

    for {
        _, msgBytes, err := c.conn.ReadMessage()
        if err != nil {
            logger.Debug().Err(err).Msg("read error")
            return
        }

        var in IncomingMessage
        if err := json.Unmarshal(msgBytes, &in); err != nil {
            logger.Warn().Err(err).Msg("invalid message format")
            continue
        }

        switch in.Type {
        case "message.send":
            s.handleSendMessage(c, in)
        default:
            logger.Warn().Str("type", in.Type).Msg("unknown message type")
        }
    }
}

func (s *Server) handleSendMessage(c *Client, in IncomingMessage) {
    messagesReceivedCounter.Inc()

    ctx, span := s.tracer.Start(context.Background(), "handleSendMessage",
        trace.WithAttributes(attribute.String("from", c.userID), attribute.String("to", in.To)))
    defer span.End()

    event := ChatMessageEvent{
        EventID:      uuid.NewString(),
        ClientMsgID:  in.ClientMsgID,
        From:         c.userID,
        To:           in.To,
        Conversation: in.Conversation,
        Content:      in.Content,
        Attachment:   in.Attachment,
        SentAt:       time.Now(),
    }

    if err := s.producer.PublishMessage(event); err != nil {
        logger.Error().Err(err).Msg("kafka publish failed")
        span.RecordError(err)
    } else {
        messagesKafkaPublishedCounter.Inc()
    }

    if err := s.redisHub.PublishDeliver(event); err != nil {
        logger.Error().Err(err).Msg("redis publish failed")
        span.RecordError(err)
    }

    s.clientsMutex.RLock()
    recipient, ok := s.clients[in.To]
    s.clientsMutex.RUnlock()
    if ok {
        payload, _ := json.Marshal(map[string]interface{}{
            "type": "message.create",
            "data": event,
        })
        recipient.sendMu.Lock()
        _ = recipient.conn.WriteMessage(websocket.TextMessage, payload)
        recipient.sendMu.Unlock()
        messagesDeliveredLocalCounter.Inc()
    }

    ack, _ := json.Marshal(map[string]interface{}{
        "type": "message.ack",
        "data": map[string]interface{}{
            "clientMsgId":   in.ClientMsgID,
            "serverMsgId":   event.EventID,
            "conversationId": in.Conversation,
            "sentAt":        event.SentAt,
        },
    })
    c.sendMu.Lock()
    _ = c.conn.WriteMessage(websocket.TextMessage, ack)
    c.sendMu.Unlock()
}
