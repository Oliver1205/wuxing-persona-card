#!/usr/bin/env bash
set -euo pipefail

FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1:5175}"
BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:48081}"
ADMIN_TOKEN="${ADMIN_TOKEN:-dev-token}"
CLIENT_ID="${CLIENT_ID:-wuxing-local-preview-smoke-$(date +%s)}"
SYNTHETIC_CHANNEL="${SYNTHETIC_CHANNEL:-perf-test}"
SYNTHETIC_CAMPAIGN="${SYNTHETIC_CAMPAIGN:-local-preview-smoke}"
ALLOW_PUBLIC_PREVIEW_SMOKE="${ALLOW_PUBLIC_PREVIEW_SMOKE:-0}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

[[ "$SYNTHETIC_CHANNEL" == "perf-test" ]] || fail "local preview smoke synthetic isolation requires SYNTHETIC_CHANNEL=perf-test"

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
if value is None:
    print("")
else:
    print(value)
PY
}

assert_csv_header() {
  local file="$1"
  local expected_header="$2"
  python3 - "$file" "$expected_header" <<'PY'
import sys

file_path, expected_header = sys.argv[1], sys.argv[2]
with open(file_path, "rb") as fp:
    raw = fp.read()
if not raw.startswith(b"\xef\xbb\xbf"):
    raise SystemExit("ERROR: admin export CSV is missing UTF-8 BOM")
lines = raw.decode("utf-8-sig").splitlines()
if not lines:
    raise SystemExit("ERROR: admin export CSV is empty")
if lines[0] != expected_header:
    raise SystemExit(f"ERROR: admin export CSV header mismatch: {lines[0]!r}")
PY
}

command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v python3 >/dev/null 2>&1 || fail "python3 is required"

FRONTEND_URL="${FRONTEND_URL%/}"
BACKEND_URL="${BACKEND_URL%/}"

python3 - "$FRONTEND_URL" "$BACKEND_URL" "$ALLOW_PUBLIC_PREVIEW_SMOKE" <<'PY'
import ipaddress
import sys
from urllib.parse import urlparse

frontend_url, backend_url, allow_public = sys.argv[1:4]

def is_local_or_private(url):
    host = (urlparse(url).hostname or "").lower()
    if host in {"localhost", "0.0.0.0", "::1"} or host.startswith("127."):
        return True
    try:
        ip = ipaddress.ip_address(host)
    except ValueError:
        return False
    return ip.is_loopback or ip.is_private

if allow_public != "1" and (not is_local_or_private(frontend_url) or not is_local_or_private(backend_url)):
    raise SystemExit(
        "ERROR: refusing to run local preview smoke against public URLs. "
        "Set ALLOW_PUBLIC_PREVIEW_SMOKE=1 only for an authorized test window."
    )
PY

body_file="$(mktemp)"
create_response="$(mktemp)"
match_body_file="$(mktemp)"
match_candidate_response="$(mktemp)"
match_create_response="$(mktemp)"
match_reload_response="$(mktemp)"
headers_file="$(mktemp)"
admin_html="$(mktemp)"
admin_export="$(mktemp)"
admin_export_default="$(mktemp)"
admin_export_headers="$(mktemp)"
frontend_admin_export="$(mktemp)"
frontend_admin_export_headers="$(mktemp)"
runtime_response="$(mktemp)"
readiness_response="$(mktemp)"
frontend_readiness_response="$(mktemp)"
trap 'rm -f "$body_file" "$create_response" "$match_body_file" "$match_candidate_response" "$match_create_response" "$match_reload_response" "$headers_file" "$admin_html" "$admin_export" "$admin_export_default" "$admin_export_headers" "$frontend_admin_export" "$frontend_admin_export_headers" "$runtime_response" "$readiness_response" "$frontend_readiness_response"' EXIT

cat >"$body_file" <<'JSON'
{
  "birthYear": 2005,
  "birthMonth": 12,
  "birthDay": null,
  "birthTimeRange": null,
  "answers": [
    { "questionCode": "Q1", "optionCode": "FIRE" },
    { "questionCode": "Q2", "optionCode": "EARTH" },
    { "questionCode": "Q3", "optionCode": "FIRE" },
    { "questionCode": "Q4", "optionCode": "EARTH" },
    { "questionCode": "Q5", "optionCode": "FIRE" }
  ]
}
JSON

curl -fsS "$BACKEND_URL/api/health" >/dev/null
curl -fsS "$BACKEND_URL/api/readiness" -o "$readiness_response"
readiness_status="$(json_get "$readiness_response" data.status)"
[[ "$readiness_status" == "UP" ]] || fail "backend readiness is not UP: $readiness_status"
curl -fsS "$FRONTEND_URL/api/readiness" -o "$frontend_readiness_response"
frontend_readiness_status="$(json_get "$frontend_readiness_response" data.status)"
[[ "$frontend_readiness_status" == "UP" ]] || fail "frontend /api/readiness proxy is not UP: $frontend_readiness_status"
curl -fsS "$BACKEND_URL/api/questions" >/dev/null

curl -fsS \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: $CLIENT_ID" \
  -H "X-Channel: $SYNTHETIC_CHANNEL" \
  -H "X-Campaign: $SYNTHETIC_CAMPAIGN" \
  -d @"$body_file" \
  "$BACKEND_URL/api/results" \
  -o "$create_response"

result_id="$(json_get "$create_response" data.resultId)"
short_code="$(json_get "$create_response" data.shortCode)"
short_url="$(json_get "$create_response" data.shortUrl)"

[[ -n "$result_id" ]] || fail "resultId missing from create result response"
[[ -n "$short_code" ]] || fail "shortCode missing from create result response"
[[ "$short_url" == "$FRONTEND_URL/s/$short_code" ]] || fail "shortUrl mismatch: got '$short_url', expected '$FRONTEND_URL/s/$short_code'. Check APP_BASE_URL."

curl -fsS \
  -H "X-Client-Id: ${CLIENT_ID}-match-candidate" \
  -H "X-Channel: $SYNTHETIC_CHANNEL" \
  -H "X-Campaign: $SYNTHETIC_CAMPAIGN" \
  "$BACKEND_URL/api/matches/candidates/$short_code" \
  -o "$match_candidate_response"
candidate_short_code="$(json_get "$match_candidate_response" data.shortCode)"
[[ "$candidate_short_code" == "$short_code" ]] || fail "match candidate shortCode mismatch: '$candidate_short_code'"

cat >"$match_body_file" <<JSON
{
  "partnerShortCode": "$short_code",
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
  -H "X-Client-Id: ${CLIENT_ID}-match-create" \
  -H "X-Channel: $SYNTHETIC_CHANNEL" \
  -H "X-Campaign: $SYNTHETIC_CAMPAIGN" \
  -d @"$match_body_file" \
  "$BACKEND_URL/api/matches" \
  -o "$match_create_response"
match_id="$(json_get "$match_create_response" data.matchId)"
current_short_code="$(json_get "$match_create_response" data.currentShortCode)"
partner_short_code="$(json_get "$match_create_response" data.partnerShortCode)"
compatibility_score="$(json_get "$match_create_response" data.compatibilityScore)"
[[ -n "$match_id" ]] || fail "matchId missing from create match response"
[[ "$partner_short_code" == "$short_code" ]] || fail "partnerShortCode mismatch: '$partner_short_code'"
[[ -n "$current_short_code" ]] || fail "currentShortCode missing from create match response"
[[ -n "$compatibility_score" ]] || fail "compatibilityScore missing from create match response"

curl -fsS \
  -H "X-Client-Id: ${CLIENT_ID}-match-reload" \
  -H "X-Channel: $SYNTHETIC_CHANNEL" \
  -H "X-Campaign: $SYNTHETIC_CAMPAIGN" \
  "$BACKEND_URL/api/matches/$short_code/$current_short_code" \
  -o "$match_reload_response"
reloaded_match_id="$(json_get "$match_reload_response" data.matchId)"
[[ "$reloaded_match_id" == "$match_id" ]] || fail "reloaded matchId mismatch: '$reloaded_match_id'"

redirect_code="$(curl -sS -o /dev/null -D "$headers_file" -w '%{http_code}' \
  -H "X-Client-Id: ${CLIENT_ID}-shortlink" \
  -H "X-Channel: $SYNTHETIC_CHANNEL" \
  -H "X-Campaign: $SYNTHETIC_CAMPAIGN" \
  "$FRONTEND_URL/s/$short_code")"
[[ "$redirect_code" == "301" || "$redirect_code" == "302" ]] || fail "frontend shortlink route did not redirect: HTTP $redirect_code. Check BACKEND_PROXY_TARGET."

location="$(awk 'tolower($1) == "location:" {print $2}' "$headers_file" | tr -d '\r' | tail -1)"
[[ "$location" == *"/result/${result_id}"* ]] || fail "unexpected shortlink Location: '$location'"
[[ "$location" == *"sc=${short_code}"* ]] || fail "shortlink Location missing sc query: '$location'"

redirect_code="$(curl -sS -o /dev/null -D "$headers_file" -w '%{http_code}' \
  -H "X-Client-Id: ${CLIENT_ID}-share-query" \
  "$FRONTEND_URL/s/$short_code?channel=$SYNTHETIC_CHANNEL&campaign=result-card")"
[[ "$redirect_code" == "301" || "$redirect_code" == "302" ]] || fail "frontend shared shortlink route did not redirect: HTTP $redirect_code. Check BACKEND_PROXY_TARGET."

shared_location="$(awk 'tolower($1) == "location:" {print $2}' "$headers_file" | tr -d '\r' | tail -1)"
[[ "$shared_location" == *"/result/${result_id}"* ]] || fail "unexpected shared shortlink Location: '$shared_location'"
[[ "$shared_location" == *"sc=${short_code}"* ]] || fail "shared shortlink Location missing sc query: '$shared_location'"
[[ "$shared_location" == *"channel=$SYNTHETIC_CHANNEL"* ]] || fail "shared shortlink Location missing channel attribution: '$shared_location'"
[[ "$shared_location" == *"campaign=result-card"* ]] || fail "shared shortlink Location missing campaign attribution: '$shared_location'"

curl -fsS "$FRONTEND_URL/admin" -o "$admin_html"
grep -q '<div id="app">' "$admin_html" || fail "frontend /admin did not return the SPA shell"

curl -fsS \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  "$BACKEND_URL/api/admin/visit-events/runtime" \
  -o "$runtime_response"
runtime_health="$(json_get "$runtime_response" data.healthStatus)"
[[ "$runtime_health" != "danger" ]] || fail "visit event runtime is danger"

curl -fsS \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  "$BACKEND_URL/api/admin/short-links/export?keyword=$short_code&includeSynthetic=true" \
  -D "$admin_export_headers" \
  -o "$admin_export"
grep -qi '^content-type: text/csv' "$admin_export_headers" || fail "admin export Content-Type is not text/csv"
grep -qi '^content-disposition: .*wuxing-short-links' "$admin_export_headers" || fail "admin export Content-Disposition missing filename"
csv_header="shortCode,resultId,shortUrl,elementCombo,starOfficerName,pv,uv,uip,statSource,metricSource,createdAt,lastVisitAt"
assert_csv_header "$admin_export" "$csv_header"
grep -q "$short_code" "$admin_export" || fail "admin export missing shortCode $short_code"
grep -q "$result_id" "$admin_export" || fail "admin export missing resultId $result_id"

curl -fsS \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  "$FRONTEND_URL/api/admin/short-links/export?keyword=$short_code&includeSynthetic=true" \
  -D "$frontend_admin_export_headers" \
  -o "$frontend_admin_export"
grep -qi '^content-type: text/csv' "$frontend_admin_export_headers" || fail "frontend /api admin export Content-Type is not text/csv"
grep -qi '^content-disposition: .*wuxing-short-links' "$frontend_admin_export_headers" || fail "frontend /api admin export Content-Disposition missing filename"
assert_csv_header "$frontend_admin_export" "$csv_header"
grep -q "$short_code" "$frontend_admin_export" || fail "frontend /api admin export missing shortCode $short_code"
grep -q "$result_id" "$frontend_admin_export" || fail "frontend /api admin export missing resultId $result_id"

curl -fsS \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  "$BACKEND_URL/api/admin/short-links/export?keyword=$short_code" \
  -o "$admin_export_default"
if grep -q "$short_code" "$admin_export_default"; then
  fail "default admin export should exclude synthetic shortCode $short_code"
fi

echo "Local preview smoke test passed"
echo "frontendUrl=$FRONTEND_URL"
echo "backendUrl=$BACKEND_URL"
echo "resultId=$result_id"
echo "shortCode=$short_code"
echo "shortUrl=$short_url"
echo "matchId=$match_id"
echo "currentShortCode=$current_short_code"
echo "compatibilityScore=$compatibility_score"
echo "redirectLocation=$location"
echo "sharedRedirectLocation=$shared_location"
echo "runtimeHealth=$runtime_health"
echo "readinessStatus=$readiness_status"
echo "frontendReadinessStatus=$frontend_readiness_status"
echo "syntheticChannel=$SYNTHETIC_CHANNEL"
echo "syntheticCampaign=$SYNTHETIC_CAMPAIGN"
