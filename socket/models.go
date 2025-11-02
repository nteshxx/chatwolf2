package main

import "time"

type IncomingMessage struct {
    Type         string `json:"type"`         // "message.send"
    ClientMsgID  string `json:"clientMsgId"`  // client-side id
    To           string `json:"to"`           // recipient userId
    Conversation string `json:"conversationId"`
    Content      string `json:"content"`
    Attachment   string `json:"attachmentUrl,omitempty"`
}

type ChatMessageEvent struct {
    EventID      string    `json:"eventId"`
    ClientMsgID  string    `json:"clientMsgId"`
    From         string    `json:"from"`
    To           string    `json:"to"`
    Conversation string    `json:"conversationId"`
    Content      string    `json:"content"`
    Attachment   string    `json:"attachmentUrl,omitempty"`
    SentAt       time.Time `json:"sentAt"`
}
