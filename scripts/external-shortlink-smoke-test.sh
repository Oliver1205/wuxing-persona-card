#!/usr/bin/env bash
set -euo pipefail

WUXING_BASE_URL="${WUXING_BASE_URL:-http://127.0.0.1:8088}"
CLIENT_ID="${CLIENT_ID:-wuxing-v11-smoke-client}"
EXPECTED_STAT_SOURCE="${EXPECTED_STAT_SOURCE:-}"
ADMIN_TOKEN="${ADMIN_TOKEN:-}"

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
    if isinstance(value, list):
        value = value[int(part)]
    else:
        value = value.get(part)
    if value is None:
        break
if value is None:
    print("")
elif isinstance(value, (dict, list)):
    print(json.dumps(value, ensure_ascii=False))
else:
    print(value)
PY
}

find_stat_source() {
  local file="$1"
  local short_code="$2"
  python3 - "$file" "$short_code" <<'PY'
import json
import sys

file_path, short_code = sys.argv[1], sys.argv[2]
with open(file_path, "r", encoding="utf-8") as fp:
    payload = json.load(fp)
records = payload.get("data", {}).get("records", [])
for record in records:
    if record.get("shortCode") == short_code:
        print(record.get("statSource", ""))
        break
PY
}

require_command curl
require_command python3

health_file="$(mktemp)"
create_body="$(mktemp)"
create_response="$(mktemp)"
headers_file="$(mktemp)"
admin_response="$(mktemp)"
trap 'rm -f "$health_file" "$create_body" "$create_response" "$headers_file" "$admin_response"' EXIT

curl -fsS "$WUXING_BASE_URL/api/health" -o "$health_file"

cat >"$create_body" <<'JSON'
{
  "birthYear": 2002,
  "birthMonth": 8,
  "birthDay": null,
  "birthTimeRange": "UNKNOWN",
  "answers": [
    { "questionCode": "Q1", "optionCode": "METAL" },
    { "questionCode": "Q2", "optionCode": "WOOD" },
    { "questionCode": "Q3", "optionCode": "WATER" },
    { "questionCode": "Q4", "optionCode": "FIRE" },
    { "questionCode": "Q5", "optionCode": "EARTH" }
  ]
}
JSON

curl -fsS \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: ${CLIENT_ID}" \
  -d @"$create_body" \
  "$WUXING_BASE_URL/api/results" \
  -o "$create_response"

result_id="$(json_get "$create_response" data.resultId)"
short_code="$(json_get "$create_response" data.shortCode)"
short_url="$(json_get "$create_response" data.shortUrl)"

[[ -n "$result_id" ]] || fail "Missing resultId in create response"
[[ -n "$short_code" ]] || fail "Missing shortCode in create response"
[[ -n "$short_url" ]] || fail "Missing shortUrl in create response"

redirect_code="$(curl -sS -o /dev/null -D "$headers_file" -w '%{http_code}' "$WUXING_BASE_URL/s/$short_code")"
if [[ "$redirect_code" != "301" && "$redirect_code" != "302" ]]; then
  fail "Expected /s/${short_code} to redirect, got HTTP ${redirect_code}"
fi

location="$(awk 'tolower($1) == "location:" {print $2}' "$headers_file" | tr -d '\r' | tail -1)"
if [[ "$location" != *"/result/${result_id}"* ]]; then
  fail "Redirect location does not point to result ${result_id}: ${location}"
fi

if [[ -n "$ADMIN_TOKEN" ]]; then
  curl -fsS \
    -H "X-Admin-Token: ${ADMIN_TOKEN}" \
    "$WUXING_BASE_URL/api/admin/short-links?page=1&pageSize=20" \
    -o "$admin_response"
  stat_source="$(find_stat_source "$admin_response" "$short_code")"
  if [[ -n "$EXPECTED_STAT_SOURCE" && "$stat_source" != "$EXPECTED_STAT_SOURCE" ]]; then
    fail "Expected statSource=${EXPECTED_STAT_SOURCE}, got ${stat_source:-empty}"
  fi
  echo "admin statSource=${stat_source:-not-found}"
elif [[ -n "$EXPECTED_STAT_SOURCE" ]]; then
  fail "ADMIN_TOKEN is required when EXPECTED_STAT_SOURCE is set"
fi

echo "External shortlink smoke test passed"
echo "resultId=${result_id}"
echo "shortCode=${short_code}"
echo "shortUrl=${short_url}"
echo "redirect=${location}"
