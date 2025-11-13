package errors

import (
	"errors"
	"fmt"
)

var (
	ErrInvalidToken     = errors.New("invalid or expired token")
	ErrUnauthorized     = errors.New("unauthorized")
	ErrInvalidMessage   = errors.New("invalid message format")
	ErrMessageTooLarge  = errors.New("message exceeds maximum size")
	ErrConnectionClosed = errors.New("connection closed")
	ErrKafkaPublish     = errors.New("failed to publish to kafka")
	ErrRedisPublish     = errors.New("failed to publish to redis")
	ErrClientBufferFull = errors.New("client buffer full")
)

type AppError struct {
	Err     error
	Message string
	Code    string
	Fields  map[string]interface{}
}

func (e *AppError) Error() string {
	if e.Message != "" {
		return fmt.Sprintf("%s: %v", e.Message, e.Err)
	}
	return e.Err.Error()
}

func (e *AppError) Unwrap() error {
	return e.Err
}

func NewAppError(err error, message, code string) *AppError {
	return &AppError{
		Err:     err,
		Message: message,
		Code:    code,
		Fields:  make(map[string]interface{}),
	}
}

func (e *AppError) WithField(key string, value interface{}) *AppError {
	e.Fields[key] = value
	return e
}
