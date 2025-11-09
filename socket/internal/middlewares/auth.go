package auth

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/MicahParks/keyfunc/v3"
	"github.com/golang-jwt/jwt/v5"
	"github.com/nteshxx/chatwolf2/socket/internal/utils"
)

type JwtValidator struct {
	jwks keyfunc.Keyfunc
}

func NewJwtValidator(ctx context.Context, jwksURL string) (*JwtValidator, error) {
	override := keyfunc.Override{
		RefreshInterval:  24 * time.Hour,
		RateLimitWaitMax: 0, // No rate limit
		RefreshErrorHandlerFunc: func(u string) func(ctx context.Context, err error) {
			return func(ctx context.Context, err error) {
				utils.Log.Info().Msgf("JWKS error for %s: %v", u, err)
			}
		},
	}

	// Create JWKS instance using NewDefaultOverrideCtx
	jwks, err := keyfunc.NewDefaultOverrideCtx(ctx, []string{jwksURL}, override)
	if err != nil {
		return nil, fmt.Errorf("failed to create JWKS: %w", err)
	}
	return &JwtValidator{jwks: jwks}, nil
}

func (j *JwtValidator) ValidateToken(ctx context.Context, tokenStr string) (string, error) {
	if tokenStr == "" {
		return "", errors.New("empty token")
	}
	parser := jwt.NewParser()
	token, err := parser.Parse(tokenStr, j.jwks.Keyfunc)
	if err != nil {
		return "", err
	}
	if !token.Valid {
		return "", errors.New("invalid token")
	}
	claims := token.Claims.(jwt.MapClaims)
	sub, ok := claims["sub"].(string)
	if !ok || sub == "" {
		return "", errors.New("missing sub claim")
	}
	return sub, nil
}
