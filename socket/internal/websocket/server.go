package websocket

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"sync"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/websocket"
	"github.com/nteshxx/chatwolf2/socket/internal/domain"
	"github.com/nteshxx/chatwolf2/socket/internal/middleware"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/logger"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/metrics"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/tracing"
	apperrors "github.com/nteshxx/chatwolf2/socket/pkg/errors"
)

var upgrader = websocket.Upgrader{
	CheckOrigin:     func(r *http.Request) bool { return true },
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

type MessageHandler interface {
	HandleMessage(ctx context.Context, from string, msg domain.Message) (domain.MessageEvent, error)
}

type PresencePublisher interface {
	PublishPresence(ctx context.Context, event domain.PresenceEvent) error
}

type Server struct {
	auth            *middleware.AuthService
	messageHandler  MessageHandler
	presenceHandler PresencePublisher
	log             *logger.Logger
	metrics         *metrics.Metrics
	clientsMu       sync.RWMutex
	clients         map[string]map[string]*Client // userID -> connectionID -> Client
	wg              sync.WaitGroup
	ctx             context.Context
	cancel          context.CancelFunc
}

func NewServer(
	auth *middleware.AuthService,
	messageHandler MessageHandler,
	presenceHandler PresencePublisher,
	log *logger.Logger,
	metrics *metrics.Metrics,
	tracer *tracing.Tracer,
) *Server {
	ctx, cancel := context.WithCancel(context.Background())

	return &Server{
		auth:            auth,
		messageHandler:  messageHandler,
		presenceHandler: presenceHandler,
		log:             log,
		metrics:         metrics,
		clients:         make(map[string]map[string]*Client),
		ctx:             ctx,
		cancel:          cancel,
	}
}

func (s *Server) HandleConnection(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	token := r.URL.Query().Get("token")
	if token == "" {
		if ah := r.Header.Get("Authorization"); len(ah) > 7 && ah[:7] == "Bearer " {
			token = ah[7:]
		}
	}

	userID, err := s.auth.ValidateToken(ctx, token)
	if err != nil {
		s.log.Warn(ctx, "unauthorized ws connection attempt", map[string]interface{}{
			"error": err.Error(),
		})

		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		s.log.Error(ctx, "upgrade failed", err)
		return
	}

	client := newClient(uuid.NewString(), userID, conn, s, s.log)

	s.registerClient(ctx, client)

	s.log.Info(ctx, "client connected", map[string]interface{}{
		"user_id":       userID,
		"connection_id": client.id,
	})

	// Publish online presence
	presenceEvent := domain.PresenceEvent{
		EventID:   uuid.NewString(),
		UserID:    userID,
		Status:    "online",
		Timestamp: time.Now(),
	}

	if err := s.presenceHandler.PublishPresence(ctx, presenceEvent); err != nil {
		s.log.Error(ctx, "failed to publish presence", err)
	}

	// Start pumps
	s.wg.Add(2)
	go func() {
		defer s.wg.Done()
		client.writePump(s.ctx)
	}()
	go func() {
		defer s.wg.Done()
		client.readPump(s.ctx)
	}()
}

func (s *Server) registerClient(ctx context.Context, c *Client) {
	s.clientsMu.Lock()
	defer s.clientsMu.Unlock()

	if s.clients[c.userID] == nil {
		s.clients[c.userID] = make(map[string]*Client)
	}
	s.clients[c.userID][c.id] = c

	s.metrics.ActiveConnections.Inc()

	s.log.Info(ctx, "client registered", map[string]interface{}{
		"user_id":       c.userID,
		"connection_id": c.id,
	})
}

func (s *Server) unregisterClient(ctx context.Context, c *Client) {
	s.clientsMu.Lock()
	defer s.clientsMu.Unlock()

	if conns, ok := s.clients[c.userID]; ok {
		if _, exists := conns[c.id]; exists {
			delete(conns, c.id)
			close(c.send)
			s.metrics.ActiveConnections.Dec()

			if len(conns) == 0 {
				delete(s.clients, c.userID)

				// Publish offline presence
				presenceEvent := domain.PresenceEvent{
					EventID:   uuid.NewString(),
					UserID:    c.userID,
					Status:    "offline",
					Timestamp: time.Now(),
				}

				if err := s.presenceHandler.PublishPresence(ctx, presenceEvent); err != nil {
					s.log.Error(ctx, "failed to publish presence", err)
				}
			}
		}
	}

	s.log.Info(ctx, "client disconnected", map[string]interface{}{
		"user_id":       c.userID,
		"connection_id": c.id,
	})
}

func (s *Server) getUserClients(userID string) []*Client {
	s.clientsMu.RLock()
	defer s.clientsMu.RUnlock()

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

func (s *Server) handleMessage(ctx context.Context, c *Client, msg domain.Message) {
	switch msg.Type {
	case "message.send":
		event, err := s.messageHandler.HandleMessage(ctx, c.userID, msg)
		if err != nil {
			s.log.Error(ctx, "failed to handle message", err)
			c.sendError(ctx, "failed to send message")
			return
		}

		// Send acknowledgment
		s.sendAck(ctx, c, msg.ClientMsgID, event.EventID, event.Conversation, event.SentAt)

	default:
		s.log.Warn(ctx, "unknown message type", map[string]interface{}{
			"type": msg.Type,
		})
		c.sendError(ctx, fmt.Sprintf("unknown message type: %s", msg.Type))
	}
}

func (s *Server) DeliverToUser(ctx context.Context, userID string, event domain.MessageEvent) error {
	payload, err := json.Marshal(map[string]interface{}{
		"type": "message.create",
		"data": event,
	})
	if err != nil {
		return apperrors.NewAppError(err, "failed to marshal message", "WS_001")
	}

	clients := s.getUserClients(userID)
	if len(clients) == 0 {
		return apperrors.NewAppError(nil, "user not connected", "WS_002")
	}

	delivered := false
	for _, client := range clients {
		if err := client.Send(ctx, payload); err != nil {
			s.log.Warn(ctx, "failed to send to client", map[string]interface{}{
				"user_id":       userID,
				"connection_id": client.id,
				"error":         err.Error(),
			})
		} else {
			delivered = true
		}
	}

	if delivered {
		s.metrics.MessagesDeliveredLocal.Inc()
	}

	return nil
}

func (s *Server) sendAck(ctx context.Context, c *Client, clientMsgID, serverMsgID, conversationID string, sentAt time.Time) {
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
		s.log.Error(ctx, "failed to marshal ack", err)
		return
	}

	if err := c.Send(ctx, ack); err != nil {
		s.log.Warn(ctx, "failed to send ack", map[string]interface{}{
			"error": err.Error(),
		})
	}
}

func (s *Server) Shutdown(ctx context.Context) error {
	s.log.Info(ctx, "shutting down websocket server")

	s.cancel()

	// Close all connections
	s.clientsMu.Lock()
	for _, conns := range s.clients {
		for _, client := range conns {
			client.conn.Close()
		}
	}
	s.clientsMu.Unlock()

	// Wait for goroutines with timeout
	done := make(chan struct{})
	go func() {
		s.wg.Wait()
		close(done)
	}()

	select {
	case <-done:
		s.log.Info(ctx, "all client connections closed gracefully")
	case <-ctx.Done():
		s.log.Warn(ctx, "shutdown timeout, forcing close")
	}

	return nil
}
