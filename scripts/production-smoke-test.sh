#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-}"
ADMIN_TOKEN="${ADMIN_TOKEN:-}"
CLIENT_ID="${CLIENT_ID:-wuxing-production-smoke-client}"
CHANNEL="${CHANNEL:-production-smoke}"
CAMPAIGN="${CAMPAIGN:-post-deploy}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
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

[[ -n "$BASE_URL" ]] || fail "BASE_URL is required, for example BASE_URL=https://example.com"
[[ -n "$ADMIN_TOKEN" ]] || fail "ADMIN_TOKEN is required"
command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v python3 >/dev/null 2>&1 || fail "python3 is required"

body_file="$(mktemp)"
create_response="$(mktemp)"
headers_file="$(mktemp)"
overview_response="$(mktemp)"
trap 'rm -f "$body_file" "$create_response" "$headers_file" "$overview_response"' EXIT

curl -fsS "$BASE_URL/api/health" >/dev/null
curl -fsS "$BASE_URL/api/questions" >/dev/null

cat >"$body_file" <<'JSON'
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
  -H "X-Session-Id: ${CLIENT_ID}-session" \
  -H "X-Channel: ${CHANNEL}" \
  -H "X-Campaign: ${CAMPAIGN}" \
  -d @"$body_file" \
  "$BASE_URL/api/results" \
  -o "$create_response"

result_id="$(json_get "$create_response" data.resultId)"
short_code="$(json_get "$create_response" data.shortCode)"
[[ -n "$result_id" ]] || fail "resultId missing"
[[ -n "$short_code" ]] || fail "shortCode missing"

redirect_code="$(curl -sS -o /dev/null -D "$headers_file" -w '%{http_code}' \
  "$BASE_URL/s/$short_code?channel=${CHANNEL}&campaign=${CAMPAIGN}")"
[[ "$redirect_code" == "301" || "$redirect_code" == "302" ]] || fail "shortlink redirect failed: ${redirect_code}"

location="$(awk 'tolower($1) == "location:" {print $2}' "$headers_file" | tr -d '\r' | tail -1)"
[[ "$location" == *"/result/${result_id}"* ]] || fail "unexpected redirect location: ${location}"
[[ "$location" == *"channel=${CHANNEL}"* ]] || fail "redirect did not preserve channel: ${location}"

curl -fsS \
  -H "X-Admin-Token: ${ADMIN_TOKEN}" \
  "$BASE_URL/api/admin/overview" \
  -o "$overview_response"

result_created="$(json_get "$overview_response" data.resultCreated)"
short_link_visits="$(json_get "$overview_response" data.shortLinkVisits)"
[[ "$result_created" -ge 1 ]] || fail "admin overview did not record result creation"
[[ "$short_link_visits" -ge 1 ]] || fail "admin overview did not record short link visit"

echo "Production smoke test passed"
echo "resultId=${result_id}"
echo "shortCode=${short_code}"
