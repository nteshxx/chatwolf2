package domain

import "time"

type PresenceEvent struct {
	EventID      string    `json:"eventId"`
	UserID       string    `json:"userId"`
	Status       string    `json:"status"` // ONLINE, OFFLINE, AWAY, HEARTBEAT
	Timestamp    time.Time `json:"timestamp"`
	DeviceID     string    `json:"deviceId,omitempty"`
	ConnectionID string    `json:"connectionId,omitempty"`
}
