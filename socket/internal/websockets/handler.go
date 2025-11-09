package websockets

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"sync"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/websocket"
	"github.com/nteshxx/chatwolf2/socket/internal/kafka"
	"github.com/nteshxx/chatwolf2/socket/internal/metrics"
	auth "github.com/nteshxx/chatwolf2/socket/internal/middlewares"
	"github.com/nteshxx/chatwolf2/socket/internal/utils"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/trace"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10 // Must be less than pongWait
	maxMessageSize = 1024 * 1024
)

type Client struct {
	ID     string // Unique connection ID
	UserID string
	Conn   *websocket.Conn
	Send   chan []byte
	Server *Server
}

type Server struct {
	validator    *auth.JwtValidator
	producer     *kafka.KafkaProducer
	redisHub     *RedisHub
	clientsMutex sync.RWMutex
	clients      map[string]map[string]*Client // userID -> connectionID -> Client
	tracer       trace.Tracer
	shutdown     chan struct{}
	wg           sync.WaitGroup
}

var upgrader = websocket.Upgrader{
	CheckOrigin:     func(r *http.Request) bool { return true },
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

func NewServer(validator *auth.JwtValidator, producer *kafka.KafkaProducer, redisHub *RedisHub, tracer trace.Tracer) *Server {
	s := &Server{
		validator: validator,
		producer:  producer,
		redisHub:  redisHub,
		clients:   make(map[string]map[string]*Client),
		tracer:    tracer,
		shutdown:  make(chan struct{}),
	}
	redisHub.SetServer(s)
	return s
}

func (s *Server) HandleWS(w http.ResponseWriter, r *http.Request) {
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
		utils.Log.Info().Err(err).Msg("unauthorized ws connection attempt")
		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		utils.Log.Error().Err(err).Msg("upgrade failed")
		return
	}

	client := &Client{
		ID:     uuid.NewString(),
		UserID: userID,
		Conn:   conn,
		Send:   make(chan []byte, 256),
		Server: s,
	}

	s.registerClient(client)
	utils.Log.Info().
		Str("user", userID).
		Str("connection", client.ID).
		Msg("client connected")

	// Publish online presence
	if err := s.redisHub.PublishPresence(map[string]interface{}{
		"eventId":   uuid.NewString(),
		"userId":    userID,
		"status":    "online",
		"timestamp": time.Now(),
	}); err != nil {
		utils.Log.Error().Err(err).Msg("failed to publish presence")
	}

	// Start read and write pumps
	s.wg.Add(2)
	go client.writePump()
	go client.readPump()
}

func (s *Server) registerClient(c *Client) {
	s.clientsMutex.Lock()
	defer s.clientsMutex.Unlock()

	if s.clients[c.UserID] == nil {
		s.clients[c.UserID] = make(map[string]*Client)
	}
	s.clients[c.UserID][c.ID] = c
}

func (s *Server) unregisterClient(c *Client) {
	s.clientsMutex.Lock()
	defer s.clientsMutex.Unlock()

	if conns, ok := s.clients[c.UserID]; ok {
		if _, exists := conns[c.ID]; exists {
			delete(conns, c.ID)
			close(c.Send)

			// If no more connections for this user, clean up the map
			if len(conns) == 0 {
				delete(s.clients, c.UserID)
			}
		}
	}
}

func (s *Server) getUserClients(userID string) []*Client {
	s.clientsMutex.RLock()
	defer s.clientsMutex.RUnlock()

	conns, ok := s.clients[userID]
	if !ok {
		return nil
	}

	clients := make([]*Client, 0, len(conns))
	for _, c := range conns {
		clients = append(clients, c)
	}
	return clients
}

func (c *Client) readPump() {
	defer func() {
		c.Server.wg.Done()
		c.Server.unregisterClient(c)
		c.Conn.Close()

		// Publish offline presence only if user has no more connections
		if len(c.Server.getUserClients(c.UserID)) == 0 {
			if err := c.Server.redisHub.PublishPresence(map[string]interface{}{
				"eventId":   uuid.NewString(),
				"userId":    c.UserID,
				"status":    "offline",
				"timestamp": time.Now(),
			}); err != nil {
				utils.Log.Error().Err(err).Msg("failed to publish presence")
			}
		}

		utils.Log.Info().
			Str("user", c.UserID).
			Str("connection", c.ID).
			Msg("client disconnected")
	}()

	c.Conn.SetReadLimit(maxMessageSize)
	if err := c.Conn.SetReadDeadline(time.Now().Add(pongWait)); err != nil {
		utils.Log.Error().Err(err).Msg("failed to set read deadline")
		return
	}

	c.Conn.SetPongHandler(func(string) error {
		return c.Conn.SetReadDeadline(time.Now().Add(pongWait))
	})

	for {
		select {
		case <-c.Server.shutdown:
			return
		default:
		}

		_, msgBytes, err := c.Conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				utils.Log.Warn().Err(err).Msg("unexpected close error")
			}
			return
		}

		var in Message
		if err := json.Unmarshal(msgBytes, &in); err != nil {
			utils.Log.Warn().Err(err).Msg("invalid message format")
			c.sendError("invalid message format")
			continue
		}

		switch in.Type {
		case "message.send":
			c.Server.handleSendMessage(c, in)
		default:
			utils.Log.Warn().Str("type", in.Type).Msg("unknown message type")
			c.sendError(fmt.Sprintf("unknown message type: %s", in.Type))
		}
	}
}

func (c *Client) writePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		c.Server.wg.Done()
		ticker.Stop()
		c.Conn.Close()
	}()

	for {
		select {
		case message, ok := <-c.Send:
			if err := c.Conn.SetWriteDeadline(time.Now().Add(writeWait)); err != nil {
				utils.Log.Error().Err(err).Msg("failed to set write deadline")
				return
			}

			if !ok {
				// Channel closed
				c.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			if err := c.Conn.WriteMessage(websocket.TextMessage, message); err != nil {
				utils.Log.Error().Err(err).Msg("failed to write message")
				return
			}

		case <-ticker.C:
			if err := c.Conn.SetWriteDeadline(time.Now().Add(writeWait)); err != nil {
				utils.Log.Error().Err(err).Msg("failed to set write deadline")
				return
			}
			if err := c.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				utils.Log.Debug().Err(err).Msg("failed to send ping")
				return
			}

		case <-c.Server.shutdown:
			return
		}
	}
}

func (c *Client) sendError(message string) {
	payload, err := json.Marshal(map[string]interface{}{
		"type": "error",
		"data": map[string]string{
			"message": message,
		},
	})
	if err != nil {
		utils.Log.Error().Err(err).Msg("failed to marshal error message")
		return
	}

	select {
	case c.Send <- payload:
	default:
		utils.Log.Warn().Msg("client send buffer full, dropping error message")
	}
}

func (s *Server) handleSendMessage(c *Client, in Message) {
	metrics.MessagesReceived.Inc()

	_, span := s.tracer.Start(context.Background(), "handleSendMessage",
		trace.WithAttributes(
			attribute.String("from", c.UserID),
			attribute.String("to", in.To),
			attribute.String("clientMsgId", in.ClientMsgID),
		))
	defer span.End()

	event := kafka.MessageEvent{
		EventID:      uuid.NewString(),
		ClientMsgID:  in.ClientMsgID,
		From:         c.UserID,
		To:           in.To,
		Conversation: in.Conversation,
		Content:      in.Content,
		Attachment:   in.Attachment,
		SentAt:       time.Now(),
	}

	// Publish to Kafka for persistence
	if err := s.producer.PublishMessage(event); err != nil {
		utils.Log.Error().Err(err).Msg("kafka publish failed")
		span.RecordError(err)
		c.sendError("failed to send message")
		return
	}
	metrics.MessagesKafkaPublished.Inc()

	// Publish to Redis for cross-instance delivery
	if err := s.redisHub.PublishDeliver(event); err != nil {
		utils.Log.Error().Err(err).Msg("redis publish failed")
		span.RecordError(err)
	}

	// Try local delivery to all recipient connections
	s.deliverToUser(in.To, event)

	// Send acknowledgment to sender
	s.sendAck(c, in.ClientMsgID, event.EventID, in.Conversation, event.SentAt)

	span.SetAttributes(attribute.String("serverMsgId", event.EventID))
}

func (s *Server) deliverToUser(userID string, event kafka.MessageEvent) {
	payload, err := json.Marshal(map[string]interface{}{
		"type": "message.create",
		"data": event,
	})
	if err != nil {
		utils.Log.Error().Err(err).Msg("failed to marshal message")
		return
	}

	clients := s.getUserClients(userID)
	if len(clients) == 0 {
		return
	}

	delivered := false
	for _, client := range clients {
		select {
		case client.Send <- payload:
			delivered = true
		default:
			utils.Log.Warn().
				Str("user", userID).
				Str("connection", client.ID).
				Msg("client send buffer full, dropping message")
		}
	}

	if delivered {
		metrics.MessagesDeliveredLocal.Inc()
	}
}

func (s *Server) sendAck(c *Client, clientMsgID, serverMsgID, conversationID string, sentAt time.Time) {
	ack, err := json.Marshal(map[string]interface{}{
		"type": "message.ack",
		"data": map[string]interface{}{
			"clientMsgId":    clientMsgID,
			"serverMsgId":    serverMsgID,
			"conversationId": conversationID,
			"sentAt":         sentAt,
		},
	})
	if err != nil {
		utils.Log.Error().Err(err).Msg("failed to marshal ack")
		return
	}

	select {
	case c.Send <- ack:
	default:
		utils.Log.Warn().Msg("client send buffer full, dropping ack")
	}
}

func (s *Server) Shutdown(ctx context.Context) error {
	utils.Log.Info().Msg("shutting down websocket server")
	close(s.shutdown)

	// Close all client connections
	s.clientsMutex.Lock()
	for _, conns := range s.clients {
		for _, client := range conns {
			client.Conn.Close()
		}
	}
	s.clientsMutex.Unlock()

	// Wait for all goroutines to finish with timeout
	done := make(chan struct{})
	go func() {
		s.wg.Wait()
		close(done)
	}()

	select {
	case <-done:
		utils.Log.Info().Msg("all client connections closed gracefully")
	case <-ctx.Done():
		utils.Log.Warn().Msg("shutdown timeout, forcing close")
	}

	return nil
}
