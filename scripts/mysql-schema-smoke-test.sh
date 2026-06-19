#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEMA_FILE="$ROOT_DIR/backend/src/main/resources/db/schema.sql"
MYSQL_IMAGE="${MYSQL_SCHEMA_SMOKE_IMAGE:-mysql:8.4}"
MYSQL_ROOT_PASSWORD="${MYSQL_SCHEMA_SMOKE_ROOT_PASSWORD:-codex-root}"
MYSQL_DATABASE="${MYSQL_SCHEMA_SMOKE_DATABASE:-wuxing_persona}"
CONTAINER_NAME="wuxing-schema-check-$$"

cleanup() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

if [ ! -f "$SCHEMA_FILE" ]; then
  echo "schema.sql not found: $SCHEMA_FILE" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker CLI is required for fresh MySQL schema smoke test." >&2
  exit 1
fi

docker info >/dev/null

if ! docker image inspect "$MYSQL_IMAGE" >/dev/null 2>&1; then
  echo "Docker image $MYSQL_IMAGE is required for schema smoke test. Pull it before running quality-check." >&2
  exit 1
fi

docker run \
  --name "$CONTAINER_NAME" \
  -e "MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD" \
  -e "MYSQL_DATABASE=$MYSQL_DATABASE" \
  -v "$SCHEMA_FILE:/docker-entrypoint-initdb.d/01-schema.sql:ro" \
  -d "$MYSQL_IMAGE" >/dev/null

ready="false"
for attempt in $(seq 1 60); do
  if docker exec -e "MYSQL_PWD=$MYSQL_ROOT_PASSWORD" "$CONTAINER_NAME" mysqladmin ping -h 127.0.0.1 -uroot --silent >/dev/null 2>&1; then
    ready="true"
    break
  fi
  sleep 1
done

if [ "$ready" != "true" ]; then
  docker logs "$CONTAINER_NAME" >&2 || true
  echo "MySQL did not become ready after schema initialization." >&2
  exit 1
fi

mysql_query() {
  docker exec -e "MYSQL_PWD=$MYSQL_ROOT_PASSWORD" "$CONTAINER_NAME" mysql -uroot -N -B "$MYSQL_DATABASE" -e "$1"
}

table_count="$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$MYSQL_DATABASE';")"
if [ "$table_count" -lt 5 ]; then
  mysql_query "SHOW TABLES;"
  echo "Expected at least 5 schema tables, got $table_count." >&2
  exit 1
fi

require_table() {
  local table="$1"
  local found
  found="$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$MYSQL_DATABASE' AND table_name = '$table';")"
  if [ "$found" != "1" ]; then
    echo "Missing required table: $table" >&2
    exit 1
  fi
}

require_column() {
  local table="$1"
  local column="$2"
  local found
  found="$(mysql_query "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = '$MYSQL_DATABASE' AND table_name = '$table' AND column_name = '$column';")"
  if [ "$found" != "1" ]; then
    echo "Missing required column: $table.$column" >&2
    exit 1
  fi
}

for table in short_link short_link_daily_metric site_daily_metric user_result visit_event; do
  require_table "$table"
done

require_column visit_event event_type
require_column visit_event channel
require_column visit_event campaign
require_column visit_event short_code
require_column visit_event client_id_hash
require_column visit_event ip_hash
require_column visit_event user_agent_hash
require_column visit_event device_type
require_column visit_event referer
require_column visit_event event_date
require_column short_link short_code
require_column short_link result_id
require_column short_link original_path
require_column short_link short_url
require_column short_link pv_count
require_column short_link uv_count
require_column short_link uip_count
require_column short_link last_visit_at
require_column user_result result_id
require_column user_result primary_element
require_column user_result secondary_element
require_column user_result star_officer_name

mysql_query "
  INSERT INTO user_result (
    result_id, birth_year, birth_month, birth_day, birth_time_range,
    answer_json, primary_element, secondary_element, primary_percent, secondary_percent,
    all_element_scores_json, star_officer_code, star_officer_name, keywords_json,
    layout_explanation, strength_text, relationship_text, status, created_at, updated_at
  ) VALUES (
    'R-SCHEMA-SMOKE', 2002, 8, NULL, NULL,
    '[]', 'METAL', 'EARTH', 58, 42,
    '{}', 'schema-smoke', 'Schema Smoke', '[]',
    'schema smoke layout', 'schema smoke strength', 'schema smoke relationship',
    1, NOW(), NOW()
  );
"

mysql_query "
  INSERT INTO short_link (
    short_code, result_id, original_path, short_url,
    pv_count, uv_count, uip_count, last_visit_at, status, created_at, updated_at
  ) VALUES (
    'SCSMOK', 'R-SCHEMA-SMOKE', '/result/R-SCHEMA-SMOKE',
    'http://127.0.0.1:5175/s/SCSMOK', 0, 0, 0, NULL, 1, NOW(), NOW()
  );
"

mysql_query "
  INSERT INTO visit_event (
    event_type, page_path, result_id, short_code, client_id_hash,
    session_id_hash, ip_hash, user_agent_hash, channel, campaign,
    device_type, referer, event_date, created_at
  ) VALUES (
    'SHORT_LINK_VISIT', '/s/SCSMOK', 'R-SCHEMA-SMOKE', 'SCSMOK', 'client-hash',
    'session-hash', 'ip-hash', 'ua-hash', 'perf-test', 'schema-smoke',
    'mobile', 'http://127.0.0.1:5175/', CURRENT_DATE(), NOW()
  );
"

smoke_count="$(mysql_query "
  SELECT COUNT(*)
  FROM short_link sl
  JOIN visit_event ve ON ve.short_code = sl.short_code
  WHERE sl.short_code = 'SCSMOK'
    AND sl.result_id = 'R-SCHEMA-SMOKE'
    AND ve.event_type = 'SHORT_LINK_VISIT'
    AND ve.channel = 'perf-test';
")"
if [ "$smoke_count" != "1" ]; then
  echo "Fresh schema smoke insert/query did not return the expected linked short-link visit." >&2
  exit 1
fi

echo "Fresh MySQL schema smoke test passed: image=$MYSQL_IMAGE tables=$table_count"
