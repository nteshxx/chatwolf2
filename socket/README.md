# WebSocket Service

Production-ready WebSocket service for real-time messaging.

## Features

- Real-time bidirectional communication via WebSocket
- JWT-based authentication
- Distributed tracing with Zipkin
- Prometheus metrics
- Service discovery with Eureka
- Redis pub/sub for cross-instance messaging
- Kafka integration for message persistence
- Graceful shutdown
- Structured logging with trace correlation

## Architecture

- **Domain Layer**: Business entities and models
- **Service Layer**: Business logic
- **Repository Layer**: Data access (Kafka, Redis)
- **Transport Layer**: WebSocket and HTTP handlers
- **Middleware**: Auth, logging, tracing
- **Observability**: Logging, metrics, tracing

## Getting Started

### Prerequisites

- Go 1.21+
- Redis
- Kafka
- Zipkin (optional)
- Eureka (optional)

### Installation

```bash
go mod download
```

### Configuration

Copy `.env.example` to `.env` and adjust values.

### Running

```bash
make run
```

### Building

```bash
make build
```

### Docker

```bash
make docker-build
make docker-run
```

## API Endpoints

### WebSocket

- `ws://localhost:7200/socket/connect?token=<JWT>`

### HTTP

- `GET /health` - Health check
- `GET /prometheus/metrics` - Prometheus metrics

## WebSocket Protocol

### Client → Server

```json
{
  "type": "message.send",
  "clientMsgId": "uuid",
  "to": "userId",
  "conversationId": "uuid",
  "content": "message text",
  "attachmentUrl": "https://..."
}
```

### Server → Client

#### Message Acknowledgment
```json
{
  "type": "message.ack",
  "data": {
    "clientMsgId": "uuid",
    "serverMsgId": "uuid",
    "conversationId": "uuid",
    "sentAt": "2024-01-01T00:00:00Z"
  }
}
```

#### Incoming Message
```json
{
  "type": "message.create",
  "data": {
    "eventId": "uuid",
    "clientMsgId": "uuid",
    "from": "userId",
    "to": "userId",
    "conversationId": "uuid",
    "content": "message text",
    "attachmentUrl": "https://...",
    "sentAt": "2024-01-01T00:00:00Z"
  }
}
```

## Best Practices Implemented

1. **Context Propagation**: Context passed as first parameter in all functions
2. **Structured Logging**: Unified logger with trace correlation
3. **Error Handling**: Custom error types with context
4. **Dependency Injection**: Loose coupling, easy testing
5. **Graceful Shutdown**: Proper cleanup on SIGTERM/SIGINT
6. **Configuration**: Environment-based with validation
7. **Observability**: Metrics, logging, distributed tracing
8. **Thread Safety**: Proper mutex usage
9. **Resource Management**: Defer cleanup, context cancellation
10. **Code Organization**: Clear separation of concerns

## Testing

```bash
make test
```

## License

MIT
