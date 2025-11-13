FROM golang:1.25-alpine AS builder

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o socket ./cmd/server

FROM alpine:3.22

RUN apk add --no-cache ca-certificates

WORKDIR /root/

COPY --from=builder /app/socket .

EXPOSE 7200

CMD ["./socket"]
