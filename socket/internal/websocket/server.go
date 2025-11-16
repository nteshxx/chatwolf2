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
	apperrors "github.com/nteshxx/chatwolf2/socket/pkg/errors"
)

const (
	// WebSocket configuration
	readBufferSize  = 1024
	writeBufferSize = 1024

	// Presence configuration
	heartbeatInterval = 30 * time.Second
	shutdownTimeout   = 10 * time.Second
)

var upgrader = websocket.Upgrader{
	CheckOrigin:     func(r *http.Request) bool { return true }, // TODO: Implement proper CORS in production
	ReadBufferSize:  readBufferSize,
	WriteBufferSize: writeBufferSize,
}

// MessageHandler handles incoming messages from clients
type MessageHandler interface {
	HandleMessage(ctx context.Context, from string, msg domain.Message) (domain.MessageEvent, error)
}

// PresencePublisher publishes user presence events
type PresencePublisher interface {
	PublishPresence(ctx context.Context, event domain.PresenceEvent) error
}

// Server manages WebSocket connections and message routing
type Server struct {
	auth              *middleware.AuthService
	messageHandler    MessageHandler
	presencePublisher PresencePublisher
	log               *logger.Logger
	metrics           *metrics.Metrics

	clientsMu sync.RWMutex
	clients   map[string]map[string]*Client // userID -> connectionID -> Client

	wg     sync.WaitGroup
	ctx    context.Context
	cancel context.CancelFunc
}

// NewServer creates a new WebSocket server instance
func NewServer(
	auth *middleware.AuthService,
	messageHandler MessageHandler,
	presencePublisher PresencePublisher,
	log *logger.Logger,
	metrics *metrics.Metrics,
) *Server {
	ctx, cancel := context.WithCancel(context.Background())

	return &Server{
		auth:              auth,
		messageHandler:    messageHandler,
		presencePublisher: presencePublisher,
		log:               log,
		metrics:           metrics,
		clients:           make(map[string]map[string]*Client),
		ctx:               ctx,
		cancel:            cancel,
	}
}

// HandleConnection handles incoming WebSocket connection requests
func (s *Server) HandleConnection(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	// Extract and validate token
	token := s.extractToken(r)
	if token == "" {
		s.log.Warn(ctx, "missing authentication token", nil)
		http.Error(w, "unauthorized: missing token", http.StatusUnauthorized)
		return
	}

	userID, err := s.auth.ValidateToken(ctx, token)
	if err != nil {
		s.log.Warn(ctx, "unauthorized ws connection attempt", map[string]interface{}{
			"error": err.Error(),
		})
		http.Error(w, "unauthorized: invalid token", http.StatusUnauthorized)
		return
	}

	// Upgrade HTTP connection to WebSocket
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		s.log.Error(ctx, "failed to upgrade connection", err)
		return
	}

	// Create new client
	connectionID := uuid.NewString()
	client := newClient(connectionID, userID, conn, s, s.log)

	// Register client
	s.registerClient(ctx, client)

	s.log.Info(ctx, "client connected", map[string]interface{}{
		"user_id":       userID,
		"connection_id": connectionID,
	})

	// Publish online presence
	if err := s.publishPresence(ctx, userID, connectionID, "ONLINE"); err != nil {
		s.log.Error(ctx, "failed to publish online presence", err)
	}

	// Start heartbeat for this connection
	s.wg.Add(3)
	go func() {
		defer s.wg.Done()
		s.heartbeatLoop(ctx, client)
	}()
	go func() {
		defer s.wg.Done()
		client.writePump(s.ctx)
	}()
	go func() {
		defer s.wg.Done()
		client.readPump(s.ctx)
	}()
}

// extractToken extracts authentication token from request
func (s *Server) extractToken(r *http.Request) string {
	// Try query parameter first
	if token := r.URL.Query().Get("token"); token != "" {
		return token
	}

	// Try Authorization header
	if auth := r.Header.Get("Authorization"); len(auth) > 7 && auth[:7] == "Bearer " {
		return auth[7:]
	}

	return ""
}

// registerClient adds a client to the server's connection pool
func (s *Server) registerClient(ctx context.Context, c *Client) {
	s.clientsMu.Lock()
	defer s.clientsMu.Unlock()

	if s.clients[c.userID] == nil {
		s.clients[c.userID] = make(map[string]*Client)
	}
	s.clients[c.userID][c.id] = c

	s.metrics.ActiveConnections.Inc()

	s.log.Debug(ctx, "client registered", map[string]interface{}{
		"user_id":       c.userID,
		"connection_id": c.id,
		"total_conns":   len(s.clients[c.userID]),
	})
}

// unregisterClient removes a client from the server's connection pool
func (s *Server) unregisterClient(ctx context.Context, c *Client) {
	s.clientsMu.Lock()
	defer s.clientsMu.Unlock()

	conns, ok := s.clients[c.userID]
	if !ok {
		return
	}

	if _, exists := conns[c.id]; !exists {
		return
	}

	delete(conns, c.id)
	close(c.send)
	s.metrics.ActiveConnections.Dec()

	// If this was the last connection for the user, publish offline presence
	if len(conns) == 0 {
		delete(s.clients, c.userID)

		if err := s.publishPresence(ctx, c.userID, c.id, "OFFLINE"); err != nil {
			s.log.Error(ctx, "failed to publish offline presence", err)
		}
	}

	s.log.Info(ctx, "client unregistered", map[string]interface{}{
		"user_id":       c.userID,
		"connection_id": c.id,
		"remaining":     len(conns),
	})
}

// getUserClients returns all active connections for a user
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

// handleMessage processes incoming messages from clients
func (s *Server) handleMessage(ctx context.Context, c *Client, msg domain.Message) {
	switch msg.Type {
	case "message.send":
		s.handleSendMessage(ctx, c, msg)
	case "ping":
		s.handlePing(ctx, c)
	default:
		s.log.Warn(ctx, "unknown message type", map[string]interface{}{
			"type":    msg.Type,
			"user_id": c.userID,
		})
		c.sendError(ctx, fmt.Sprintf("unknown message type: %s", msg.Type))
	}
}

// handleSendMessage processes message.send events
func (s *Server) handleSendMessage(ctx context.Context, c *Client, msg domain.Message) {
	event, err := s.messageHandler.HandleMessage(ctx, c.userID, msg)
	if err != nil {
		s.log.Error(ctx, "failed to handle message", err)
		c.sendError(ctx, "failed to send message")
		s.metrics.MessagesFailed.Inc()
		return
	}

	// Send acknowledgment
	s.sendAck(ctx, c, msg.ClientMsgID, event.EventID, event.Conversation, event.SentAt)
	s.metrics.MessagesProcessed.Inc()
}

// handlePing responds to ping messages
func (s *Server) handlePing(ctx context.Context, c *Client) {
	pong, err := json.Marshal(map[string]interface{}{
		"type": "pong",
		"data": map[string]interface{}{
			"timestamp": time.Now().Unix(),
		},
	})
	if err != nil {
		s.log.Error(ctx, "failed to marshal pong", err)
		return
	}

	if err := c.Send(ctx, pong); err != nil {
		s.log.Warn(ctx, "failed to send pong", map[string]interface{}{
			"error": err.Error(),
		})
	}
}

// DeliverToUser delivers a message to all connections of a user
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
		s.log.Debug(ctx, "user not connected", map[string]interface{}{
			"user_id": userID,
		})
		return apperrors.NewAppError(nil, "user not connected", "WS_002")
	}

	delivered := false
	var lastErr error

	for _, client := range clients {
		if err := client.Send(ctx, payload); err != nil {
			s.log.Warn(ctx, "failed to send to client", map[string]interface{}{
				"user_id":       userID,
				"connection_id": client.id,
				"error":         err.Error(),
			})
			lastErr = err
		} else {
			delivered = true
		}
	}

	if delivered {
		s.metrics.MessagesDeliveredLocal.Inc()
		return nil
	}

	return apperrors.NewAppError(lastErr, "failed to deliver to any client", "WS_003")
}

// sendAck sends message acknowledgment to client
func (s *Server) sendAck(ctx context.Context, c *Client, clientMsgID, serverMsgID, conversationID string, sentAt time.Time) {
	ack, err := json.Marshal(map[string]interface{}{
		"type": "message.ack",
		"data": map[string]interface{}{
			"clientMsgId":    clientMsgID,
			"serverMsgId":    serverMsgID,
			"conversationId": conversationID,
			"sentAt":         sentAt.Unix(),
		},
	})
	if err != nil {
		s.log.Error(ctx, "failed to marshal ack", err)
		return
	}

	if err := c.Send(ctx, ack); err != nil {
		s.log.Warn(ctx, "failed to send ack", map[string]interface{}{
			"client_msg_id": clientMsgID,
			"error":         err.Error(),
		})
	}
}

// publishPresence publishes user presence event to Redis
func (s *Server) publishPresence(ctx context.Context, userID, connectionID, status string) error {
	event := domain.PresenceEvent{
		EventID:      uuid.NewString(),
		UserID:       userID,
		Status:       status,
		Timestamp:    time.Now(),
		ConnectionID: connectionID,
	}

	if err := s.presencePublisher.PublishPresence(ctx, event); err != nil {
		s.metrics.PresencePublishFailed.Inc()
		return fmt.Errorf("failed to publish presence: %w", err)
	}

	s.metrics.PresencePublished.Inc()
	return nil
}

// heartbeatLoop periodically sends heartbeat presence events
func (s *Server) heartbeatLoop(ctx context.Context, c *Client) {
	ticker := time.NewTicker(heartbeatInterval)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-s.ctx.Done():
			return
		case <-ticker.C:
			// Only send heartbeat if client still exists
			if !s.isClientConnected(c.userID, c.id) {
				return
			}

			if err := s.publishPresence(ctx, c.userID, c.id, "HEARTBEAT"); err != nil {
				s.log.Error(ctx, "failed to publish heartbeat", err)
			}
		}
	}
}

// isClientConnected checks if a client is still connected
func (s *Server) isClientConnected(userID, connectionID string) bool {
	s.clientsMu.RLock()
	defer s.clientsMu.RUnlock()

	if conns, ok := s.clients[userID]; ok {
		_, exists := conns[connectionID]
		return exists
	}
	return false
}

// Shutdown gracefully shuts down the WebSocket server
func (s *Server) Shutdown(ctx context.Context) error {
	s.log.Info(ctx, "shutting down websocket server", nil)

	// Signal all goroutines to stop
	s.cancel()

	// Close all client connections
	s.clientsMu.Lock()
	for userID, conns := range s.clients {
		for connID, client := range conns {
			s.log.Debug(ctx, "closing client connection", map[string]interface{}{
				"user_id":       userID,
				"connection_id": connID,
			})

			// Send close message
			_ = client.conn.WriteControl(
				websocket.CloseMessage,
				websocket.FormatCloseMessage(websocket.CloseGoingAway, "server shutting down"),
				time.Now().Add(time.Second),
			)

			_ = client.conn.Close()
		}
	}
	s.clientsMu.Unlock()

	// Wait for all goroutines with timeout
	done := make(chan struct{})
	go func() {
		s.wg.Wait()
		close(done)
	}()

	shutdownCtx, shutdownCancel := context.WithTimeout(ctx, shutdownTimeout)
	defer shutdownCancel()

	select {
	case <-done:
		s.log.Info(ctx, "all client connections closed gracefully", nil)
		return nil
	case <-shutdownCtx.Done():
		s.log.Warn(ctx, "shutdown timeout exceeded, some connections may not have closed gracefully", nil)
		return fmt.Errorf("shutdown timeout exceeded")
	}
}

// GetStats returns current server statistics
func (s *Server) GetStats() map[string]interface{} {
	s.clientsMu.RLock()
	defer s.clientsMu.RUnlock()

	totalConnections := 0
	for _, conns := range s.clients {
		totalConnections += len(conns)
	}

	return map[string]interface{}{
		"total_users":       len(s.clients),
		"total_connections": totalConnections,
	}
}
