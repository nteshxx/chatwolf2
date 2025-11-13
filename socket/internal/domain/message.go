package domain

import (
	"time"
)

type Message struct {
	Type         string `json:"type"`
	ClientMsgID  string `json:"clientMsgId"`
	To           string `json:"to"`
	Conversation string `json:"conversationId"`
	Content      string `json:"content"`
	Attachment   string `json:"attachmentUrl,omitempty"`
}

type MessageEvent struct {
	EventID      string    `json:"eventId"`
	ClientMsgID  string    `json:"clientMsgId"`
	From         string    `json:"from"`
	To           string    `json:"to"`
	Conversation string    `json:"conversationId"`
	Content      string    `json:"content"`
	Attachment   string    `json:"attachmentUrl,omitempty"`
	SentAt       time.Time `json:"sentAt"`
}
