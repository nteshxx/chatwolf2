package websockets

type Message struct {
	Type         string `json:"type"`        // "message.send"
	ClientMsgID  string `json:"clientMsgId"` // client-side id
	To           string `json:"to"`          // recipient userId
	Conversation string `json:"conversationId"`
	Content      string `json:"content"`
	Attachment   string `json:"attachmentUrl,omitempty"`
}
