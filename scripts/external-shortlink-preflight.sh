#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/deploy/.env.external.example}"
PROBE="${2:-${PROBE_EXTERNAL_SHORTLINK:-0}}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

warn() {
  echo "WARN: $*" >&2
}

env_value() {
  local key="$1"
  grep -E "^${key}=" "$ENV_FILE" | tail -1 | cut -d= -f2- || true
}

require_value() {
  local key="$1"
  local value
  value="$(env_value "$key")"
  if [[ -z "$value" ]]; then
    fail "Missing required env key: ${key}"
  fi
  if [[ "$value" == "change-me" || "$value" == replace-with-* || "$value" == "https://your-domain.com" ]]; then
    fail "Replace placeholder value for ${key}"
  fi
  printf '%s' "$value"
}

require_boolean() {
  local key="$1"
  local value="$2"
  if [[ "$value" != "true" && "$value" != "false" ]]; then
    fail "${key} must be true or false"
  fi
}

[[ -f "$ENV_FILE" ]] || fail "Env file not found: ${ENV_FILE}"

mode="$(require_value SHORT_LINK_MODE)"
[[ "$mode" == "external" ]] || fail "SHORT_LINK_MODE must be external for this preflight"

base_url="$(require_value SHORT_LINK_EXTERNAL_BASE_URL)"
group_id="$(require_value SHORT_LINK_EXTERNAL_GROUP_ID)"
domain="$(require_value SHORT_LINK_EXTERNAL_DOMAIN)"
fallback="$(require_value SHORT_LINK_EXTERNAL_FALLBACK_TO_INTERNAL)"
stats_enabled="$(require_value SHORT_LINK_EXTERNAL_STATS_ENABLED)"
username="$(require_value SHORT_LINK_EXTERNAL_SYSTEM_USERNAME)"
user_id="$(require_value SHORT_LINK_EXTERNAL_SYSTEM_USER_ID)"
real_name="$(require_value SHORT_LINK_EXTERNAL_SYSTEM_REAL_NAME)"

require_boolean SHORT_LINK_EXTERNAL_FALLBACK_TO_INTERNAL "$fallback"
require_boolean SHORT_LINK_EXTERNAL_STATS_ENABLED "$stats_enabled"

[[ "$base_url" == http://* || "$base_url" == https://* ]] || fail "SHORT_LINK_EXTERNAL_BASE_URL must start with http:// or https://"
if [[ "$domain" == http://* || "$domain" == https://* ]]; then
  fail "SHORT_LINK_EXTERNAL_DOMAIN must be host[:port] without scheme"
fi

if [[ "$group_id" == "tSUBMP" ]]; then
  warn "SHORT_LINK_EXTERNAL_GROUP_ID is the upstream demo group. Use a dedicated wuxing_persona group before production."
fi

if [[ "$username" == "admin" || "$user_id" == "admin" || "$real_name" == "admin" ]]; then
  warn "External shortlink system headers still use admin identity. Prefer a dedicated wuxing_system identity."
fi

project_dir="$(env_value EXTERNAL_SHORTLINK_PROJECT_DIR)"
if [[ -n "$project_dir" ]]; then
  [[ -d "$project_dir" ]] || fail "EXTERNAL_SHORTLINK_PROJECT_DIR does not exist: ${project_dir}"
  [[ -f "$project_dir/aggregation/src/main/resources/application.yaml" ]] \
    || fail "External aggregation application.yaml not found under ${project_dir}"
fi

if [[ "$PROBE" == "--probe" || "$PROBE" == "1" || "$PROBE" == "true" ]]; then
  command -v curl >/dev/null 2>&1 || fail "curl is required for --probe"
  http_code="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 3 "$base_url" || true)"
  if [[ "$http_code" == "000" ]]; then
    fail "Cannot reach external shortlink service at ${base_url}"
  fi
  echo "External shortlink service responded with HTTP ${http_code} at ${base_url}"
else
  echo "Probe skipped. Pass --probe or PROBE_EXTERNAL_SHORTLINK=1 after the external service is running."
fi

echo "External shortlink preflight passed for ${ENV_FILE}"
echo "groupId=${group_id}"
echo "domain=${domain}"
echo "statsEnabled=${stats_enabled}"
