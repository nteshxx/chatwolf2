package main

import (
    "context"
    "errors"
    "github.com/MicahParks/keyfunc"
    "github.com/golang-jwt/jwt/v5"
    "time"
)

type JwtValidator struct {
    jwks *keyfunc.JWKS
}

func NewJwtValidator(jwksURL string) (*JwtValidator, error) {
    options := keyfunc.Options{
        RefreshErrorHandler: func(err error) {
            logger.Error().Err(err).Msg("jwks refresh error")
        },
        RefreshInterval:   time.Minute * 15,
        RefreshRateLimit:  time.Minute,
        RefreshTimeout:    10 * time.Second,
        RefreshUnknownKID: true,
    }
    jwks, err := keyfunc.Get(jwksURL, options)
    if err != nil {
        return nil, err
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
