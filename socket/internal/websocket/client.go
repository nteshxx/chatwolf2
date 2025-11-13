package websocket

import (
	"context"
	"encoding/json"
	"time"

	"github.com/gorilla/websocket"
	"github.com/nteshxx/chatwolf2/socket/internal/domain"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/logger"
	apperrors "github.com/nteshxx/chatwolf2/socket/pkg/errors"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10
	maxMessageSize = 1024 * 1024
)

type Client struct {
	id     string
	userID string
	conn   *websocket.Conn
	send   chan []byte
	server *Server
	log    *logger.Logger
}

func newClient(id, userID string, conn *websocket.Conn, server *Server, log *logger.Logger) *Client {
	return &Client{
		id:     id,
		userID: userID,
		conn:   conn,
		send:   make(chan []byte, 256),
		server: server,
		log:    log,
	}
}

func (c *Client) readPump(ctx context.Context) {
	defer func() {
		c.server.unregisterClient(ctx, c)
		c.conn.Close()
	}()

	c.conn.SetReadLimit(maxMessageSize)
	if err := c.conn.SetReadDeadline(time.Now().Add(pongWait)); err != nil {
		c.log.Error(ctx, "failed to set read deadline", err)
		return
	}

	c.conn.SetPongHandler(func(string) error {
		return c.conn.SetReadDeadline(time.Now().Add(pongWait))
	})

	for {
		select {
		case <-ctx.Done():
			return
		default:
		}

		_, msgBytes, err := c.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				c.log.Warn(ctx, "unexpected close error", map[string]interface{}{
					"error": err.Error(),
				})
			}
			return
		}

		var msg domain.Message
		if err := json.Unmarshal(msgBytes, &msg); err != nil {
			c.log.Warn(ctx, "invalid message format", map[string]interface{}{
				"error": err.Error(),
			})
			c.sendError(ctx, "invalid message format")
			continue
		}

		c.server.handleMessage(ctx, c, msg)
	}
}

func (c *Client) writePump(ctx context.Context) {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.conn.Close()
	}()

	for {
		select {
		case message, ok := <-c.send:
			if err := c.conn.SetWriteDeadline(time.Now().Add(writeWait)); err != nil {
				c.log.Error(ctx, "failed to set write deadline", err)
				return
			}

			if !ok {
				c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			if err := c.conn.WriteMessage(websocket.TextMessage, message); err != nil {
				c.log.Error(ctx, "failed to write message", err)
				return
			}

		case <-ticker.C:
			if err := c.conn.SetWriteDeadline(time.Now().Add(writeWait)); err != nil {
				c.log.Error(ctx, "failed to set write deadline", err)
				return
			}
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}

		case <-ctx.Done():
			return
		}
	}
}

func (c *Client) sendError(ctx context.Context, message string) {
	payload, err := json.Marshal(map[string]interface{}{
		"type": "error",
		"data": map[string]string{
			"message": message,
		},
	})
	if err != nil {
		c.log.Error(ctx, "failed to marshal error message", err)
		return
	}

	select {
	case c.send <- payload:
	default:
		c.log.Warn(ctx, "client send buffer full, dropping error message")
	}
}

func (c *Client) Send(ctx context.Context, payload []byte) error {
	select {
	case c.send <- payload:
		return nil
	case <-time.After(5 * time.Second):
		return apperrors.ErrClientBufferFull
	case <-ctx.Done():
		return ctx.Err()
	}
}
