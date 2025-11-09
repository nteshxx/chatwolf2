package utils

import (
	"os"

	"github.com/rs/zerolog"
)

var Log zerolog.Logger

func InitLogger(debug bool) {
	level := zerolog.InfoLevel
	if debug {
		level = zerolog.DebugLevel
	}

	Log = zerolog.New(os.Stdout).
		Level(level).
		With().
		Timestamp().
		Caller().
		Logger()

	Log.Info().Msg("Logger initialized")
}
