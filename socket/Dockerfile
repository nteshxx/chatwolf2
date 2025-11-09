FROM golang:1.25-alpine AS build

WORKDIR /app

COPY go.mod go.sum ./

RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o /socket-service ./cmd

FROM alpine:3.18

RUN apk add --no-cache ca-certificates

COPY --from=build /socket /socket

EXPOSE 3000

ENTRYPOINT ["/socket"]
