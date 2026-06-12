#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${DOMAIN:-}"
EXPECTED_IP="${EXPECTED_IP:-}"
BASE_URL="${BASE_URL:-}"
ADMIN_TOKEN="${ADMIN_TOKEN:-}"
ALLOW_HTTP="${ALLOW_HTTP:-false}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

info() {
  echo "==> $*"
}

json_get() {
  local file="$1"
  local path="$2"
  python3 - "$file" "$path" <<'PY'
import json
import sys

file_path, value_path = sys.argv[1], sys.argv[2]
with open(file_path, "r", encoding="utf-8") as fp:
    value = json.load(fp)
for part in value_path.split("."):
    value = value[int(part)] if isinstance(value, list) else value.get(part)
    if value is None:
        break
print("" if value is None else value)
PY
}

resolve_domain() {
  python3 - "$DOMAIN" <<'PY'
import socket
import sys

domain = sys.argv[1]
addresses = sorted({
    item[4][0]
    for item in socket.getaddrinfo(domain, None, proto=socket.IPPROTO_TCP)
})
print("\n".join(addresses))
PY
}

[[ -n "$DOMAIN" ]] || fail "DOMAIN is required, for example DOMAIN=wuxing.example.com"
[[ "$DOMAIN" != http://* && "$DOMAIN" != https://* && "$DOMAIN" != */* ]] || fail "DOMAIN must be a host name only, without scheme or path"
command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v python3 >/dev/null 2>&1 || fail "python3 is required"

BASE_URL="${BASE_URL:-https://${DOMAIN}}"
if [[ "$BASE_URL" == http://* && "$ALLOW_HTTP" != "true" ]]; then
  fail "BASE_URL uses http. Set ALLOW_HTTP=true only for temporary first binding without TLS."
fi

info "Resolve ${DOMAIN}"
resolved_ips="$(resolve_domain || true)"
[[ -n "$resolved_ips" ]] || fail "DOMAIN did not resolve: ${DOMAIN}"
echo "$resolved_ips"

if [[ -n "$EXPECTED_IP" ]] && ! grep -Fx "$EXPECTED_IP" <<<"$resolved_ips" >/dev/null; then
  fail "DOMAIN does not resolve to EXPECTED_IP=${EXPECTED_IP}"
fi

info "Check health endpoints on ${BASE_URL}"
health_response="$(mktemp)"
questions_response="$(mktemp)"
overview_response="$(mktemp)"
trap 'rm -f "$health_response" "$questions_response" "$overview_response"' EXIT

curl -fsS "${BASE_URL}/api/health" -o "$health_response"
service_status="$(json_get "$health_response" data.status)"
[[ "$service_status" == "UP" ]] || fail "health status is not UP: ${service_status}"

curl -fsS "${BASE_URL}/api/questions" -o "$questions_response"
question_count="$(python3 - "$questions_response" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as fp:
    payload = json.load(fp)
print(len(payload.get("data") or []))
PY
)"
[[ "$question_count" -ge 5 ]] || fail "expected at least 5 questions, got ${question_count}"

if [[ -n "$ADMIN_TOKEN" ]]; then
  info "Check admin overview"
  curl -fsS -H "X-Admin-Token: ${ADMIN_TOKEN}" "${BASE_URL}/api/admin/overview" -o "$overview_response"
  overview_code="$(json_get "$overview_response" code)"
  [[ "$overview_code" == "0" ]] || fail "admin overview returned code ${overview_code}"
else
  info "Skip admin overview because ADMIN_TOKEN is empty"
fi

echo "Domain bind preflight passed"
echo "domain=${DOMAIN}"
echo "baseUrl=${BASE_URL}"
