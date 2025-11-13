.PHONY: build run test clean docker-build docker-run

build:
	go build -o bin/socket ./cmd/server

run:
	go run ./cmd/server

test:
	go test -v -race -cover ./...

clean:
	rm -rf bin/

docker-build:
	docker build -t socket-service .

docker-run:
	docker run -p 7200:7200 socket-service

lint:
	golangci-lint run

fmt:
	go fmt ./...

vet:
	go vet ./...
	