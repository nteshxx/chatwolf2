package middleware

import (
	"context"
	"fmt"

	"github.com/MicahParks/keyfunc/v3"
	"github.com/golang-jwt/jwt/v5"
	apperrors "github.com/nteshxx/chatwolf2/socket/pkg/errors"
)

type AuthService struct {
	jwks keyfunc.Keyfunc
}

func NewAuthService(ctx context.Context, jwksURL string) (*AuthService, error) {
	jwks, err := keyfunc.NewDefaultCtx(ctx, []string{jwksURL})
	if err != nil {
		return nil, fmt.Errorf("failed to create JWKS: %w", err)
	}

	return &AuthService{jwks: jwks}, nil
}

func (a *AuthService) ValidateToken(ctx context.Context, tokenStr string) (string, error) {
	if tokenStr == "" {
		return "", apperrors.ErrInvalidToken
	}

	parser := jwt.NewParser()
	token, err := parser.Parse(tokenStr, a.jwks.Keyfunc)
	if err != nil {
		return "", apperrors.NewAppError(err, "token parsing failed", "AUTH_001")
	}

	if !token.Valid {
		return "", apperrors.ErrInvalidToken
	}

	claims, ok := token.Claims.(jwt.MapClaims)
	if !ok {
		return "", apperrors.NewAppError(nil, "invalid claims format", "AUTH_002")
	}

	sub, ok := claims["sub"].(string)
	if !ok || sub == "" {
		return "", apperrors.NewAppError(nil, "missing sub claim", "AUTH_003")
	}

	return sub, nil
}

func (a *AuthService) Close(ctx context.Context) error {
	// JWKS cleanup if needed
	return nil
}
