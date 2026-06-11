#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8088}"
ADMIN_TOKEN="${ADMIN_TOKEN:-dev-token}"
CLIENT_ID_PREFIX="${CLIENT_ID_PREFIX:-wuxing-perf-smoke}"
SHORTLINK_HITS="${SHORTLINK_HITS:-30}"
ADMIN_HITS="${ADMIN_HITS:-2}"
MAX_SHORTLINK_AVG_MS="${MAX_SHORTLINK_AVG_MS:-0}"
MAX_ADMIN_AVG_MS="${MAX_ADMIN_AVG_MS:-0}"

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

now_ms() {
  python3 - <<'PY'
import time
print(int(time.time() * 1000))
PY
}

measure() {
  local label="$1"
  shift
  local start
  local end
  start="$(now_ms)"
  "$@"
  end="$(now_ms)"
  echo "${label}_ms=$((end - start))"
}

command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v python3 >/dev/null 2>&1 || fail "python3 is required"
[[ "$SHORTLINK_HITS" =~ ^[0-9]+$ && "$SHORTLINK_HITS" -gt 0 ]] || fail "SHORTLINK_HITS must be a positive integer"
[[ "$ADMIN_HITS" =~ ^[0-9]+$ && "$ADMIN_HITS" -gt 0 ]] || fail "ADMIN_HITS must be a positive integer"
[[ "$MAX_SHORTLINK_AVG_MS" =~ ^[0-9]+$ ]] || fail "MAX_SHORTLINK_AVG_MS must be a non-negative integer"
[[ "$MAX_ADMIN_AVG_MS" =~ ^[0-9]+$ ]] || fail "MAX_ADMIN_AVG_MS must be a non-negative integer"

body_file="$(mktemp)"
create_response="$(mktemp)"
result_response="$(mktemp)"
headers_file="$(mktemp)"
overview_response="$(mktemp)"
trap 'rm -f "$body_file" "$create_response" "$result_response" "$headers_file" "$overview_response"' EXIT

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

curl -fsS "$BASE_URL/api/health" >/dev/null
curl -fsS "$BASE_URL/api/questions" >/dev/null

measure "create_result" curl -fsS \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: ${CLIENT_ID_PREFIX}-creator" \
  -d @"$body_file" \
  "$BASE_URL/api/results" \
  -o "$create_response"

result_id="$(json_get "$create_response" data.resultId)"
short_code="$(json_get "$create_response" data.shortCode)"
[[ -n "$result_id" ]] || fail "resultId missing"
[[ -n "$short_code" ]] || fail "shortCode missing"

measure "read_result" curl -fsS \
  -H "X-Client-Id: ${CLIENT_ID_PREFIX}-reader" \
  "$BASE_URL/api/results/${result_id}" \
  -o "$result_response"

short_start="$(now_ms)"
for i in $(seq 1 "$SHORTLINK_HITS"); do
  redirect_code="$(curl -sS -o /dev/null -D "$headers_file" -w '%{http_code}' \
    -H "X-Client-Id: ${CLIENT_ID_PREFIX}-share-${i}" \
    "$BASE_URL/s/$short_code")"
  [[ "$redirect_code" == "301" || "$redirect_code" == "302" ]] || fail "shortlink redirect failed on hit ${i}: ${redirect_code}"

  location="$(awk 'tolower($1) == "location:" {print $2}' "$headers_file" | tr -d '\r' | tail -1)"
  [[ "$location" == *"/result/${result_id}"* ]] || fail "unexpected redirect location on hit ${i}: ${location}"
done
short_end="$(now_ms)"
short_elapsed_ms=$((short_end - short_start))
short_avg_ms=$((short_elapsed_ms / SHORTLINK_HITS))

admin_start="$(now_ms)"
for i in $(seq 1 "$ADMIN_HITS"); do
  curl -fsS \
    -H "X-Admin-Token: ${ADMIN_TOKEN}" \
    "$BASE_URL/api/admin/overview" \
    -o "$overview_response"
done
admin_end="$(now_ms)"
admin_elapsed_ms=$((admin_end - admin_start))
admin_avg_ms=$((admin_elapsed_ms / ADMIN_HITS))

result_created="$(json_get "$overview_response" data.resultCreated)"
[[ "$result_created" -ge 1 ]] || fail "admin overview did not record result creation"
if [[ "$MAX_SHORTLINK_AVG_MS" -gt 0 && "$short_avg_ms" -gt "$MAX_SHORTLINK_AVG_MS" ]]; then
  fail "shortlink average ${short_avg_ms}ms exceeded MAX_SHORTLINK_AVG_MS=${MAX_SHORTLINK_AVG_MS}"
fi
if [[ "$MAX_ADMIN_AVG_MS" -gt 0 && "$admin_avg_ms" -gt "$MAX_ADMIN_AVG_MS" ]]; then
  fail "admin average ${admin_avg_ms}ms exceeded MAX_ADMIN_AVG_MS=${MAX_ADMIN_AVG_MS}"
fi

echo "Performance smoke test passed"
echo "baseUrl=${BASE_URL}"
echo "resultId=${result_id}"
echo "shortCode=${short_code}"
echo "shortlinkHits=${SHORTLINK_HITS}"
echo "shortlinkTotalMs=${short_elapsed_ms}"
echo "shortlinkAvgMs=${short_avg_ms}"
echo "maxShortlinkAvgMs=${MAX_SHORTLINK_AVG_MS}"
echo "adminHits=${ADMIN_HITS}"
echo "adminTotalMs=${admin_elapsed_ms}"
echo "adminAvgMs=${admin_avg_ms}"
echo "maxAdminAvgMs=${MAX_ADMIN_AVG_MS}"
