package tracing

import (
	"context"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/openzipkin/zipkin-go"
	zipkinhttp "github.com/openzipkin/zipkin-go/middleware/http"
	"github.com/openzipkin/zipkin-go/reporter"
	reporterhttp "github.com/openzipkin/zipkin-go/reporter/http"
)

type Config struct {
	ServiceName            string
	ServiceAddr            string
	ZipkinURL              string
	SampleRate             float64
	BatchSize              int
	BatchTimeout           time.Duration
	ExcludeHealthEndpoint  bool
	ExcludeMetricsEndpoint bool
}

type Tracer struct {
	tracer     *zipkin.Tracer
	reporter   reporter.Reporter
	middleware func(http.Handler) http.Handler
	config     Config
}

func New(cfg Config) (*Tracer, error) {
	reporter := reporterhttp.NewReporter(
		cfg.ZipkinURL,
		reporterhttp.BatchSize(cfg.BatchSize),
		reporterhttp.BatchInterval(cfg.BatchTimeout),
		reporterhttp.MaxBacklog(1000),
		reporterhttp.Timeout(5*time.Second),
	)

	endpoint, err := zipkin.NewEndpoint(cfg.ServiceName, cfg.ServiceAddr)
	if err != nil {
		reporter.Close()
		return nil, fmt.Errorf("failed to create endpoint: %w", err)
	}

	sampler, err := zipkin.NewBoundarySampler(cfg.SampleRate, time.Now().Unix())
	if err != nil {
		reporter.Close()
		return nil, fmt.Errorf("failed to create sampler: %w", err)
	}

	tracer, err := zipkin.NewTracer(
		reporter,
		zipkin.WithLocalEndpoint(endpoint),
		zipkin.WithSampler(sampler),
		zipkin.WithSharedSpans(false),
		zipkin.WithTraceID128Bit(true),
	)
	if err != nil {
		reporter.Close()
		return nil, fmt.Errorf("failed to create tracer: %w", err)
	}

	middleware := zipkinhttp.NewServerMiddleware(
		tracer,
		zipkinhttp.TagResponseSize(true),
	)

	return &Tracer{
		tracer:     tracer,
		reporter:   reporter,
		middleware: middleware,
		config:     cfg,
	}, nil
}

func (t *Tracer) Middleware() func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// skip tracing for excluded endpoints
			if t.shouldSkipTracing(r.URL.Path) {
				next.ServeHTTP(w, r)
				return
			}

			// Apply Zipkin middleware for other endpoints
			t.middleware(next).ServeHTTP(w, r)
		})
	}
}

func (t *Tracer) shouldSkipTracing(path string) bool {
	if t.config.ExcludeHealthEndpoint && strings.HasPrefix(path, "/health") {
		return true
	}
	if t.config.ExcludeMetricsEndpoint && (strings.HasPrefix(path, "/prometheus/metrics")) {
		return true
	}
	return false
}

func (t *Tracer) Tracer() *zipkin.Tracer {
	return t.tracer
}

func (t *Tracer) StartSpan(ctx context.Context, name string) (zipkin.Span, context.Context) {
	span, ctx := t.tracer.StartSpanFromContext(ctx, name)
	return span, ctx
}

func (t *Tracer) Close(ctx context.Context) error {
	return t.reporter.Close()
}
