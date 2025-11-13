package logger

import (
	"context"
	"fmt"
	"io"
	"os"
	"strings"
	"time"

	"github.com/openzipkin/zipkin-go"
	"github.com/rs/zerolog"
)

type Logger struct {
	zlog zerolog.Logger
}

func New(output io.Writer, level string) *Logger {
	writer := zerolog.ConsoleWriter{
		Out:        output,
		TimeFormat: time.RFC3339,
		FormatLevel: func(i interface{}) string {
			return fmt.Sprintf("%-6s", strings.ToUpper(fmt.Sprint(i)))
		},
	}

	logLevel := parseLogLevel(level)
	zlog := zerolog.New(writer).
		Level(logLevel).
		With().
		Timestamp().
		Caller().
		Logger()

	return &Logger{zlog: zlog}
}

func parseLogLevel(level string) zerolog.Level {
	switch strings.ToLower(level) {
	case "debug":
		return zerolog.DebugLevel
	case "info":
		return zerolog.InfoLevel
	case "warn", "warning":
		return zerolog.WarnLevel
	case "error":
		return zerolog.ErrorLevel
	default:
		return zerolog.InfoLevel
	}
}

// WithTracing adds trace context to logger
func (l *Logger) WithTracing(ctx context.Context) *Logger {
	span := zipkin.SpanFromContext(ctx)
	if span == nil {
		return l
	}

	spanCtx := span.Context()
	newLogger := l.zlog.With().
		Str("trace_id", spanCtx.TraceID.String()).
		Str("span_id", spanCtx.ID.String()).
		Logger()

	return &Logger{zlog: newLogger}
}

func (l *Logger) WithFields(fields map[string]interface{}) *Logger {
	event := l.zlog.With()
	for k, v := range fields {
		event = event.Interface(k, v)
	}
	return &Logger{zlog: event.Logger()}
}

func (l *Logger) Debug(ctx context.Context, msg string, fields ...map[string]interface{}) {
	l.log(ctx, l.zlog.Debug(), msg, fields...)
}

func (l *Logger) Info(ctx context.Context, msg string, fields ...map[string]interface{}) {
	l.log(ctx, l.zlog.Info(), msg, fields...)
}

func (l *Logger) Warn(ctx context.Context, msg string, fields ...map[string]interface{}) {
	l.log(ctx, l.zlog.Warn(), msg, fields...)
}

func (l *Logger) Error(ctx context.Context, msg string, err error, fields ...map[string]interface{}) {
	event := l.zlog.Error()
	if err != nil {
		event = event.Err(err)
	}
	l.log(ctx, event, msg, fields...)
}

func (l *Logger) Fatal(ctx context.Context, msg string, err error, fields ...map[string]interface{}) {
	event := l.zlog.Fatal()
	if err != nil {
		event = event.Err(err)
	}
	l.log(ctx, event, msg, fields...)
}

func (l *Logger) log(ctx context.Context, event *zerolog.Event, msg string, fields ...map[string]interface{}) {
	// Add trace context
	span := zipkin.SpanFromContext(ctx)
	if span != nil {
		spanCtx := span.Context()
		event = event.
			Str("trace_id", spanCtx.TraceID.String()).
			Str("span_id", spanCtx.ID.String())
	}

	// Add fields
	if len(fields) > 0 {
		for k, v := range fields[0] {
			event = event.Interface(k, v)
		}
	}

	event.Msg(msg)
}

// Global logger instance
var global *Logger

func Init(level string) {
	global = New(os.Stdout, level)
	global.Info(context.Background(), "logger initialized")
}

func Global() *Logger {
	if global == nil {
		Init("info")
	}
	return global
}
