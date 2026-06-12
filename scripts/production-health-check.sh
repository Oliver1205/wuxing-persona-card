#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-}"
ADMIN_TOKEN="${ADMIN_TOKEN:-}"
EXPECT_HTTP_REDIRECT_TO_HTTPS="${EXPECT_HTTP_REDIRECT_TO_HTTPS:-false}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required"
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

[[ -n "$BASE_URL" ]] || fail "BASE_URL is required, for example BASE_URL=http://82.157.137.36"
require_command curl
require_command python3

health_response="$(mktemp)"
questions_response="$(mktemp)"
admin_response="$(mktemp)"
trap 'rm -f "$health_response" "$questions_response" "$admin_response"' EXIT

home_status="$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL/")"
[[ "$home_status" == "200" ]] || fail "home returned HTTP $home_status"

curl -fsS "$BASE_URL/api/health" -o "$health_response"
service_status="$(json_get "$health_response" data.status)"
[[ "$service_status" == "UP" ]] || fail "health status is not UP: $service_status"

curl -fsS "$BASE_URL/api/questions" -o "$questions_response"
question_count="$(python3 - "$questions_response" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as fp:
    payload = json.load(fp)
print(len(payload.get("data") or []))
PY
)"
[[ "$question_count" -ge 5 ]] || fail "expected at least 5 questions, got $question_count"

if [[ -n "$ADMIN_TOKEN" ]]; then
  curl -fsS -H "X-Admin-Token: ${ADMIN_TOKEN}" "$BASE_URL/api/admin/overview" -o "$admin_response"
  admin_code="$(json_get "$admin_response" code)"
  [[ "$admin_code" == "0" ]] || fail "admin overview returned code $admin_code"
fi

if [[ "$EXPECT_HTTP_REDIRECT_TO_HTTPS" == "true" ]]; then
  [[ "$BASE_URL" == https://* ]] || fail "EXPECT_HTTP_REDIRECT_TO_HTTPS requires BASE_URL=https://..."
  http_url="http://${BASE_URL#https://}"
  redirect_status="$(curl -sS -o /dev/null -w '%{http_code}' "$http_url/")"
  redirect_target="$(curl -sS -o /dev/null -w '%{redirect_url}' "$http_url/")"
  [[ "$redirect_status" == "301" || "$redirect_status" == "302" ]] || fail "HTTP did not redirect to HTTPS: $redirect_status"
  [[ "$redirect_target" == "$BASE_URL/" ]] || fail "unexpected HTTP redirect target: $redirect_target"
fi

echo "Production health check passed"
echo "baseUrl=$BASE_URL"
echo "homeStatus=$home_status"
echo "healthStatus=$service_status"
echo "questionCount=$question_count"
if [[ -n "$ADMIN_TOKEN" ]]; then
  echo "adminOverview=checked"
else
  echo "adminOverview=skipped"
fi
