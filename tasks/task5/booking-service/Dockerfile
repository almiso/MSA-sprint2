FROM golang:1.21-alpine AS builder

WORKDIR /app
COPY go.mod ./
COPY main.go ./

# Build the Go application securely and with smallest footprint
RUN CGO_ENABLED=0 GOOS=linux go build -o /booking-service

FROM alpine:3.19

WORKDIR /
COPY --from=builder /booking-service /booking-service

EXPOSE 8080

CMD ["/booking-service"]
