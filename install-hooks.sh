#!/bin/bash
# install-hooks.sh

HOOK_SOURCE="./git-hooks/pre-commit"
HOOK_TARGET=".git/hooks/pre-commit"

echo "Installing pre-commit hook..."

cp "$HOOK_SOURCE" "$HOOK_TARGET"
chmod +x "$HOOK_TARGET"

echo "✅ Pre-commit hook installed successfully!"
