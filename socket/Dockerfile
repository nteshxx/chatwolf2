FROM golang:1.25-alpine AS builder

WORKDIR /build

COPY go.* ./

RUN go mod download

COPY . .

RUN go build -o /build/bin/socket ./cmd/main

FROM alpine:3.22

RUN apk add --no-cache ca-certificates

COPY --from=builder /build/bin/socket /bin/socket

EXPOSE 7200

ENTRYPOINT ["/bin/socket"]
