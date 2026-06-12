#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/deploy/.env}"
COMPOSE_FILE="${COMPOSE_FILE:-$ROOT_DIR/deploy/docker-compose.yml}"
AUTHORIZED_KEYS="${AUTHORIZED_KEYS:-$HOME/.ssh/authorized_keys}"
TEMP_CODEX_KEY_COMMENT="${TEMP_CODEX_KEY_COMMENT:-codex-wuxingcard-domain-20260612}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

warn() {
  echo "WARN: $*" >&2
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required"
}

[[ -f "$ENV_FILE" ]] || fail "env file not found: $ENV_FILE"
require_command docker

echo "== Environment file"
env_perm="$(stat -c '%a' "$ENV_FILE" 2>/dev/null || stat -f '%Lp' "$ENV_FILE")"
echo "envFile=$ENV_FILE"
echo "envPerm=$env_perm"
if [[ "$env_perm" != "600" && "$env_perm" != "640" ]]; then
  warn "$ENV_FILE should usually be chmod 600 or 640"
fi

echo "== Public entry"
grep -E '^(APP_BASE_URL|NGINX_HTTP_PORT|SHORT_LINK_MODE|SHORT_LINK_EXTERNAL_DOMAIN)=' "$ENV_FILE" || true

echo "== Docker compose"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

echo "== Listening ports"
if command -v ss >/dev/null 2>&1; then
  ss -ltnp 2>/dev/null | grep -E ':(22|80|443|8088|3306|6379)\b' || true
else
  netstat -ltnp 2>/dev/null | grep -E ':(22|80|443|8088|3306|6379)\b' || true
fi

echo "== UFW"
if command -v ufw >/dev/null 2>&1; then
  sudo ufw status numbered || true
else
  echo "ufw=not-installed"
fi

echo "== Temporary Codex SSH key"
if [[ -f "$AUTHORIZED_KEYS" ]] && grep -F "$TEMP_CODEX_KEY_COMMENT" "$AUTHORIZED_KEYS" >/dev/null; then
  warn "temporary Codex SSH key is still present: $TEMP_CODEX_KEY_COMMENT"
else
  echo "temporaryCodexKey=absent"
fi

echo "Server security audit completed"
