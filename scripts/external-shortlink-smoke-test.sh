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

assert_visit_page() {
  local file="$1"
  local label="$2"
  local expected_source="$3"
  python3 - "$file" "$label" "$expected_source" <<'PY'
import json
import sys

file_path, label, expected_source = sys.argv[1], sys.argv[2], sys.argv[3]
with open(file_path, "r", encoding="utf-8") as fp:
    payload = json.load(fp)
data = payload.get("data", {})
total = data.get("total")
records = data.get("records", [])
if not isinstance(total, int):
    raise SystemExit(f"{label} total is not numeric: {total!r}")
if total < 0:
    raise SystemExit(f"{label} total is negative: {total}")
if total > 0 and not records:
    raise SystemExit(f"{label} total is {total} but records are empty")
allowed_sources = {source.strip() for source in expected_source.split(",") if source.strip()}
if allowed_sources:
    for index, record in enumerate(records):
        source = record.get("statSource", "")
        if source not in allowed_sources:
            raise SystemExit(
                f"{label} record[{index}].statSource expected one of "
                f"{sorted(allowed_sources)}, got {source!r}"
            )
print(records[0].get("statSource", "empty") if records else "empty")
PY
}

require_command curl
require_command python3

health_file="$(mktemp)"
create_body="$(mktemp)"
create_response="$(mktemp)"
headers_file="$(mktemp)"
shared_headers_file="$(mktemp)"
synthetic_headers_file="$(mktemp)"
admin_response="$(mktemp)"
visits_default_response="$(mktemp)"
visits_source_response="$(mktemp)"
visits_all_response="$(mktemp)"
trap 'rm -f "$health_file" "$create_body" "$create_response" "$headers_file" "$shared_headers_file" "$synthetic_headers_file" "$admin_response" "$visits_default_response" "$visits_source_response" "$visits_all_response"' EXIT

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
if [[ "$location" != *"sc=${short_code}"* ]]; then
  fail "Redirect location does not preserve short code attribution: ${location}"
fi

shared_redirect_code="$(curl -sS -o /dev/null -D "$shared_headers_file" -w '%{http_code}' "$WUXING_BASE_URL/s/$short_code?channel=share&campaign=result-card")"
if [[ "$shared_redirect_code" != "301" && "$shared_redirect_code" != "302" ]]; then
  fail "Expected attributed /s/${short_code} to redirect, got HTTP ${shared_redirect_code}"
fi

shared_location="$(awk 'tolower($1) == "location:" {print $2}' "$shared_headers_file" | tr -d '\r' | tail -1)"
if [[ "$shared_location" != *"/result/${result_id}"* ]]; then
  fail "Attributed redirect location does not point to result ${result_id}: ${shared_location}"
fi
if [[ "$shared_location" != *"sc=${short_code}"* ]]; then
  fail "Attributed redirect location does not preserve short code attribution: ${shared_location}"
fi
if [[ "$shared_location" != *"channel=share"* || "$shared_location" != *"campaign=result-card"* ]]; then
  fail "Attributed redirect location does not preserve share landing attribution: ${shared_location}"
fi

synthetic_redirect_code="$(curl -sS -o /dev/null -D "$synthetic_headers_file" -w '%{http_code}' "$WUXING_BASE_URL/s/$short_code?channel=perf-test&campaign=external-shortlink-smoke")"
if [[ "$synthetic_redirect_code" != "301" && "$synthetic_redirect_code" != "302" ]]; then
  fail "Expected synthetic /s/${short_code} to redirect, got HTTP ${synthetic_redirect_code}"
fi

synthetic_location="$(awk 'tolower($1) == "location:" {print $2}' "$synthetic_headers_file" | tr -d '\r' | tail -1)"
if [[ "$synthetic_location" != *"/result/${result_id}"* ]]; then
  fail "Synthetic redirect location does not point to result ${result_id}: ${synthetic_location}"
fi
if [[ "$synthetic_location" != *"sc=${short_code}"* ]]; then
  fail "Synthetic redirect location does not preserve short code attribution: ${synthetic_location}"
fi
if [[ "$synthetic_location" != *"channel=perf-test"* || "$synthetic_location" != *"campaign=external-shortlink-smoke"* ]]; then
  fail "Synthetic redirect location does not preserve perf-test attribution: ${synthetic_location}"
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

  curl -fsS \
    -H "X-Admin-Token: ${ADMIN_TOKEN}" \
    "$WUXING_BASE_URL/api/admin/short-links/${short_code}/visits?page=1&pageSize=20" \
    -o "$visits_default_response"

  detail_source="${stat_source:-local}"
  if [[ "$detail_source" != "local" && "$detail_source" != "external" ]]; then
    detail_source="local"
  fi

  curl -fsS \
    -H "X-Admin-Token: ${ADMIN_TOKEN}" \
    "$WUXING_BASE_URL/api/admin/short-links/${short_code}/visits?page=1&pageSize=20&statSource=${detail_source}" \
    -o "$visits_source_response"

  curl -fsS \
    -H "X-Admin-Token: ${ADMIN_TOKEN}" \
    "$WUXING_BASE_URL/api/admin/short-links/${short_code}/visits?page=1&pageSize=20&includeSynthetic=true" \
    -o "$visits_all_response"

  default_visit_source="$(assert_visit_page "$visits_default_response" "default visits" "local")"
  detail_visit_source="$(assert_visit_page "$visits_source_response" "${detail_source} visits" "$detail_source")"
  include_synthetic_source="$(assert_visit_page "$visits_all_response" "includeSynthetic visits" "local,external")"
  default_total="$(json_get "$visits_default_response" data.total)"
  include_synthetic_total="$(json_get "$visits_all_response" data.total)"

  if [[ "$detail_source" == "local" ]]; then
    for _ in 1 2 3 4 5; do
      if [[ "$include_synthetic_total" =~ ^[0-9]+$ && "$default_total" =~ ^[0-9]+$ && "$include_synthetic_total" -gt "$default_total" ]]; then
        break
      fi
      sleep 1
      curl -fsS \
        -H "X-Admin-Token: ${ADMIN_TOKEN}" \
        "$WUXING_BASE_URL/api/admin/short-links/${short_code}/visits?page=1&pageSize=20" \
        -o "$visits_default_response"
      curl -fsS \
        -H "X-Admin-Token: ${ADMIN_TOKEN}" \
        "$WUXING_BASE_URL/api/admin/short-links/${short_code}/visits?page=1&pageSize=20&includeSynthetic=true" \
        -o "$visits_all_response"
      default_visit_source="$(assert_visit_page "$visits_default_response" "default visits" "local")"
      include_synthetic_source="$(assert_visit_page "$visits_all_response" "includeSynthetic visits" "local,external")"
      default_total="$(json_get "$visits_default_response" data.total)"
      include_synthetic_total="$(json_get "$visits_all_response" data.total)"
    done
    if [[ ! "$default_total" =~ ^[0-9]+$ || ! "$include_synthetic_total" =~ ^[0-9]+$ || "$include_synthetic_total" -le "$default_total" ]]; then
      fail "includeSynthetic=true should include the synthetic perf-test visit: default=${default_total}, includeSynthetic=${include_synthetic_total}"
    fi
  fi

  echo "admin visits default total=${default_total}"
  echo "admin visits default firstSource=${default_visit_source}"
  echo "admin visits ${detail_source} total=$(json_get "$visits_source_response" data.total)"
  echo "admin visits ${detail_source} firstSource=${detail_visit_source}"
  echo "admin visits includeSynthetic total=${include_synthetic_total}"
  echo "admin visits includeSynthetic firstSource=${include_synthetic_source}"
elif [[ -n "$EXPECTED_STAT_SOURCE" ]]; then
  fail "ADMIN_TOKEN is required when EXPECTED_STAT_SOURCE is set"
fi

echo "External shortlink smoke test passed"
echo "resultId=${result_id}"
echo "shortCode=${short_code}"
echo "shortUrl=${short_url}"
echo "redirect=${location}"
echo "sharedRedirect=${shared_location}"
echo "syntheticRedirect=${synthetic_location}"
