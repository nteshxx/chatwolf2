package domain

import "time"

type PresenceEvent struct {
	EventID   string                 `json:"eventId"`
	UserID    string                 `json:"userId"`
	Status    string                 `json:"status"`
	Timestamp time.Time              `json:"timestamp"`
	Metadata  map[string]interface{} `json:"metadata,omitempty"`
}
