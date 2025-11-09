package tracing

import (
	"context"
	"net/http"

	"go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/zipkin"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	"go.opentelemetry.io/otel/trace"
)

func InitTracer(zipkinURL, serviceName string) (func(context.Context) error, error) {
	if zipkinURL == "" {
		tp := sdktrace.NewTracerProvider()
		otel.SetTracerProvider(tp)
		return func(ctx context.Context) error { return nil }, nil
	}
	exp, err := zipkin.New(zipkinURL)
	if err != nil {
		return nil, err
	}
	tp := sdktrace.NewTracerProvider(sdktrace.WithBatcher(exp))
	otel.SetTracerProvider(tp)
	return tp.Shutdown, nil
}

func WrapHandler(h func(http.ResponseWriter, *http.Request)) http.Handler {
	return otelhttp.NewHandler(http.HandlerFunc(h), "socket-http")
}

func Tracer(name string) trace.Tracer {
	return otel.Tracer(name)
}
