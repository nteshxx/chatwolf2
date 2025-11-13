package middleware

import (
	"net/http"
	"strings"
	"time"

	"github.com/nteshxx/chatwolf2/socket/internal/observability/logger"
)

type LoggingConfig struct {
	ExcludeHealthEndpoint  bool
	ExcludeMetricsEndpoint bool
}

func Logging(log *logger.Logger, cfg LoggingConfig) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if shouldSkipLogging(r.URL.Path, cfg) {
				next.ServeHTTP(w, r)
				return
			}

			start := time.Now()

			log.Info(r.Context(), "incoming request", map[string]interface{}{
				"method":      r.Method,
				"path":        r.URL.Path,
				"remote_addr": r.RemoteAddr,
			})

			next.ServeHTTP(w, r)

			log.Info(r.Context(), "request completed", map[string]interface{}{
				"method":   r.Method,
				"path":     r.URL.Path,
				"duration": time.Since(start).String(),
			})
		})
	}
}

func shouldSkipLogging(path string, cfg LoggingConfig) bool {
	if cfg.ExcludeHealthEndpoint && strings.HasPrefix(path, "/health") {
		return true
	}
	if cfg.ExcludeMetricsEndpoint && (strings.HasPrefix(path, "/prometheus/metrics")) {
		return true
	}
	return false
}
