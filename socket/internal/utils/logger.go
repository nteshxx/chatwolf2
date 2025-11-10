package utils

import (
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/rs/zerolog"
)

var Log zerolog.Logger

func InitLogger() {
	writer := zerolog.ConsoleWriter{
		Out:        os.Stdout,
		TimeFormat: time.RFC3339,
		FormatLevel: func(i interface{}) string {
			return fmt.Sprintf("%-6s", strings.ToUpper(fmt.Sprint(i)))
		},
		FormatTimestamp: func(i interface{}) string {
			return fmt.Sprint(i)
		},
	}

	zerolog.SetGlobalLevel(zerolog.InfoLevel)
	Log = zerolog.New(writer).
		Level(zerolog.InfoLevel).
		With().
		Timestamp().
		Caller().
		Logger()

	Log.Info().Msg("logger initialized")
}
