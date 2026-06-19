#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/deploy/.env}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_key() {
  local key="$1"
  local value
  value="$(grep -E "^${key}=" "$ENV_FILE" | tail -1 | cut -d= -f2- || true)"
  if [[ -z "$value" ]]; then
    fail "Missing required env key: ${key}"
  fi
  if [[ "$value" == "change-me" || "$value" == replace-with-* || "$value" == "https://your-domain.com" ]]; then
    fail "Replace placeholder value for ${key}"
  fi
}

require_secret() {
  local key="$1"
  local min_length="$2"
  local value
  value="$(grep -E "^${key}=" "$ENV_FILE" | tail -1 | cut -d= -f2- || true)"
  require_key "$key"
  if [[ "$value" == "dev-token" || "$value" == "dev-salt" || "$value" == "local-token" || "$value" == "local-salt" ]]; then
    fail "Replace local development value for ${key}"
  fi
  if ((${#value} < min_length)); then
    fail "${key} must be at least ${min_length} characters"
  fi
}

[[ -f "$ENV_FILE" ]] || fail "Env file not found: ${ENV_FILE}"

require_key APP_BASE_URL
require_key MYSQL_PASSWORD
require_key MYSQL_ROOT_PASSWORD
require_secret ADMIN_TOKEN 24
require_secret HASH_SALT 32
require_key SHORT_LINK_MODE

mode="$(grep -E '^SHORT_LINK_MODE=' "$ENV_FILE" | tail -1 | cut -d= -f2-)"
if [[ "$mode" != "internal" && "$mode" != "external" ]]; then
  fail "SHORT_LINK_MODE must be internal or external"
fi

if [[ "$mode" == "external" ]]; then
  require_key SHORT_LINK_EXTERNAL_BASE_URL
  require_key SHORT_LINK_EXTERNAL_GROUP_ID
  require_key SHORT_LINK_EXTERNAL_DOMAIN
fi

if grep -E '(^|=)(changeme|password|123456|dev-token|dev-salt)$' "$ENV_FILE" >/dev/null; then
  fail "Weak placeholder-like secret detected in ${ENV_FILE}"
fi

echo "Deploy preflight passed for ${ENV_FILE}"
