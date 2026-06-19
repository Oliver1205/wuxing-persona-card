#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8088}"
ADMIN_TOKEN="${ADMIN_TOKEN:-dev-token}"
CLIENT_ID_PREFIX="${CLIENT_ID_PREFIX:-wuxing-perf-smoke}"
SYNTHETIC_CHANNEL="${SYNTHETIC_CHANNEL:-perf-test}"
RUN_ID="${RUN_ID:-smoke-$(date +%Y%m%d%H%M%S)}"
SYNTHETIC_CAMPAIGN="${SYNTHETIC_CAMPAIGN:-performance-smoke:${RUN_ID}}"
SHORTLINK_HITS="${SHORTLINK_HITS:-30}"
ADMIN_HITS="${ADMIN_HITS:-2}"
MAX_SHORTLINK_AVG_MS="${MAX_SHORTLINK_AVG_MS:-0}"
MAX_ADMIN_AVG_MS="${MAX_ADMIN_AVG_MS:-0}"
MAX_SHORTLINK_P95_MS="${MAX_SHORTLINK_P95_MS:-0}"
MAX_ADMIN_P95_MS="${MAX_ADMIN_P95_MS:-0}"
MAX_ASYNC_QUEUE_SIZE="${MAX_ASYNC_QUEUE_SIZE:-}"
MAX_ASYNC_DROPPED_EVENTS="${MAX_ASYNC_DROPPED_EVENTS:-}"
MAX_ASYNC_BATCH_FAILURES="${MAX_ASYNC_BATCH_FAILURES:-}"
SMOKE_OUT_DIR="${SMOKE_OUT_DIR:-}"

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
if value is None:
    print("")
elif isinstance(value, bool):
    print(str(value).lower())
else:
    print(value)
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

p95_ms() {
  local file="$1"
  python3 - "$file" <<'PY'
import math
import sys

with open(sys.argv[1], "r", encoding="utf-8") as fp:
    values = sorted(int(line.strip()) for line in fp if line.strip())
if not values:
    print(0)
else:
    index = max(0, math.ceil(len(values) * 0.95) - 1)
    print(values[index])
PY
}

write_smoke_artifacts() {
  [[ -n "$SMOKE_OUT_DIR" ]] || return 0

  mkdir -p "$SMOKE_OUT_DIR"
  local metrics_file="$SMOKE_OUT_DIR/smoke-output.txt"
  local summary_file="$SMOKE_OUT_DIR/summary.json"

  cat >"$metrics_file" <<EOF
Performance smoke test passed
baseUrl=${BASE_URL}
resultId=${result_id}
shortCode=${short_code}
syntheticChannel=${SYNTHETIC_CHANNEL}
syntheticCampaign=${SYNTHETIC_CAMPAIGN}
runId=${RUN_ID}
shortlinkHits=${SHORTLINK_HITS}
shortlinkTotalMs=${short_elapsed_ms}
shortlinkAvgMs=${short_avg_ms}
shortlinkP95Ms=${short_p95_ms}
maxShortlinkAvgMs=${MAX_SHORTLINK_AVG_MS}
maxShortlinkP95Ms=${MAX_SHORTLINK_P95_MS}
adminHits=${ADMIN_HITS}
adminTotalMs=${admin_elapsed_ms}
adminAvgMs=${admin_avg_ms}
adminP95Ms=${admin_p95_ms}
maxAdminAvgMs=${MAX_ADMIN_AVG_MS}
maxAdminP95Ms=${MAX_ADMIN_P95_MS}
asyncQueueSize=${async_queue_size}
asyncQueueCapacity=${async_queue_capacity}
asyncDrainLimit=${async_drain_limit}
asyncDroppedEvents=${async_dropped_events}
asyncTotalFlushedEvents=${async_total_flushed_events}
asyncLastFlushAt=${async_last_flush_at}
asyncLastBatchSize=${async_last_batch_size}
asyncBatchWriteFailures=${async_batch_write_failures}
asyncWorkerAlive=${async_worker_alive}
asyncMode=${async_mode}
rocketMqAvailable=${rocketmq_available}
rocketMqConsumerEnabled=${rocketmq_consumer_enabled}
rocketMqConsumerPersistenceReady=${rocketmq_consumer_persistence_ready}
rocketMqFallbackToLocal=${rocketmq_fallback_to_local}
rocketMqTopic=${rocketmq_topic}
rocketMqPublishedEvents=${rocketmq_published_events}
rocketMqPublishFailures=${rocketmq_publish_failures}
rocketMqFallbackEvents=${rocketmq_fallback_events}
rocketMqShadowLocalEvents=${rocketmq_shadow_local_events}
runtimeHealthStatus=${runtime_health_status}
runtimeHealthMessage=${runtime_health_message}
readinessStatus=${readiness_status}
maxAsyncQueueSize=${MAX_ASYNC_QUEUE_SIZE}
maxAsyncDroppedEvents=${MAX_ASYNC_DROPPED_EVENTS}
maxAsyncBatchFailures=${MAX_ASYNC_BATCH_FAILURES}
EOF

  python3 - "$metrics_file" "$summary_file" <<'PY'
import json
import sys

metrics = {}
with open(sys.argv[1], "r", encoding="utf-8") as fp:
    for line in fp:
        line = line.strip()
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        metrics[key] = value

integer_keys = {
    "shortlinkHits",
    "shortlinkTotalMs",
    "shortlinkAvgMs",
    "shortlinkP95Ms",
    "maxShortlinkAvgMs",
    "maxShortlinkP95Ms",
    "adminHits",
    "adminTotalMs",
    "adminAvgMs",
    "adminP95Ms",
    "maxAdminAvgMs",
    "maxAdminP95Ms",
    "asyncQueueSize",
    "asyncQueueCapacity",
    "asyncDrainLimit",
    "asyncDroppedEvents",
    "asyncTotalFlushedEvents",
    "asyncLastBatchSize",
    "asyncBatchWriteFailures",
    "rocketMqPublishedEvents",
    "rocketMqPublishFailures",
    "rocketMqFallbackEvents",
    "rocketMqShadowLocalEvents",
    "maxAsyncQueueSize",
    "maxAsyncDroppedEvents",
    "maxAsyncBatchFailures",
}
boolean_keys = {
    "asyncWorkerAlive",
    "rocketMqAvailable",
    "rocketMqConsumerEnabled",
    "rocketMqConsumerPersistenceReady",
    "rocketMqFallbackToLocal",
}

summary = {
    "status": "passed",
    "baseUrl": metrics.get("baseUrl", ""),
    "resultId": metrics.get("resultId", ""),
    "shortCode": metrics.get("shortCode", ""),
    "syntheticChannel": metrics.get("syntheticChannel", ""),
    "syntheticCampaign": metrics.get("syntheticCampaign", ""),
    "runId": metrics.get("runId", ""),
}

for key, value in metrics.items():
    if key in summary:
        continue
    if key in integer_keys:
        summary[key] = int(value) if value else None
    elif key in boolean_keys:
        summary[key] = value.lower() == "true"
    else:
        summary[key] = value

with open(sys.argv[2], "w", encoding="utf-8") as fp:
    json.dump(summary, fp, ensure_ascii=False, indent=2)
    fp.write("\n")
PY
}

command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v python3 >/dev/null 2>&1 || fail "python3 is required"
[[ "$SHORTLINK_HITS" =~ ^[0-9]+$ && "$SHORTLINK_HITS" -gt 0 ]] || fail "SHORTLINK_HITS must be a positive integer"
[[ "$ADMIN_HITS" =~ ^[0-9]+$ && "$ADMIN_HITS" -gt 0 ]] || fail "ADMIN_HITS must be a positive integer"
[[ "$MAX_SHORTLINK_AVG_MS" =~ ^[0-9]+$ ]] || fail "MAX_SHORTLINK_AVG_MS must be a non-negative integer"
[[ "$MAX_ADMIN_AVG_MS" =~ ^[0-9]+$ ]] || fail "MAX_ADMIN_AVG_MS must be a non-negative integer"
[[ "$MAX_SHORTLINK_P95_MS" =~ ^[0-9]+$ ]] || fail "MAX_SHORTLINK_P95_MS must be a non-negative integer"
[[ "$MAX_ADMIN_P95_MS" =~ ^[0-9]+$ ]] || fail "MAX_ADMIN_P95_MS must be a non-negative integer"
[[ -z "$MAX_ASYNC_QUEUE_SIZE" || "$MAX_ASYNC_QUEUE_SIZE" =~ ^[0-9]+$ ]] || fail "MAX_ASYNC_QUEUE_SIZE must be empty or a non-negative integer"
[[ -z "$MAX_ASYNC_DROPPED_EVENTS" || "$MAX_ASYNC_DROPPED_EVENTS" =~ ^[0-9]+$ ]] || fail "MAX_ASYNC_DROPPED_EVENTS must be empty or a non-negative integer"
[[ -z "$MAX_ASYNC_BATCH_FAILURES" || "$MAX_ASYNC_BATCH_FAILURES" =~ ^[0-9]+$ ]] || fail "MAX_ASYNC_BATCH_FAILURES must be empty or a non-negative integer"

body_file="$(mktemp)"
create_response="$(mktemp)"
result_response="$(mktemp)"
headers_file="$(mktemp)"
overview_response="$(mktemp)"
runtime_response="$(mktemp)"
short_timings_file="$(mktemp)"
admin_timings_file="$(mktemp)"
readiness_response="$(mktemp)"
trap 'rm -f "$body_file" "$create_response" "$result_response" "$headers_file" "$overview_response" "$runtime_response" "$short_timings_file" "$admin_timings_file" "$readiness_response"' EXIT

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
curl -fsS "$BASE_URL/api/readiness" -o "$readiness_response"
readiness_status="$(json_get "$readiness_response" data.status)"
[[ "$readiness_status" == "UP" ]] || fail "readiness status is not UP: ${readiness_status}"
curl -fsS "$BASE_URL/api/questions" >/dev/null

measure "create_result" curl -fsS \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: ${CLIENT_ID_PREFIX}-creator" \
  -H "X-Channel: ${SYNTHETIC_CHANNEL}" \
  -H "X-Campaign: ${SYNTHETIC_CAMPAIGN}" \
  -d @"$body_file" \
  "$BASE_URL/api/results" \
  -o "$create_response"

result_id="$(json_get "$create_response" data.resultId)"
short_code="$(json_get "$create_response" data.shortCode)"
[[ -n "$result_id" ]] || fail "resultId missing"
[[ -n "$short_code" ]] || fail "shortCode missing"

measure "read_result" curl -fsS \
  -H "X-Client-Id: ${CLIENT_ID_PREFIX}-reader" \
  -H "X-Channel: ${SYNTHETIC_CHANNEL}" \
  -H "X-Campaign: ${SYNTHETIC_CAMPAIGN}" \
  "$BASE_URL/api/results/${result_id}" \
  -o "$result_response"

short_start="$(now_ms)"
for i in $(seq 1 "$SHORTLINK_HITS"); do
  hit_start="$(now_ms)"
  redirect_code="$(curl -sS -o /dev/null -D "$headers_file" -w '%{http_code}' \
    -H "X-Client-Id: ${CLIENT_ID_PREFIX}-share-${i}" \
    -H "X-Channel: ${SYNTHETIC_CHANNEL}" \
    -H "X-Campaign: ${SYNTHETIC_CAMPAIGN}" \
    "$BASE_URL/s/$short_code")"
  hit_end="$(now_ms)"
  echo "$((hit_end - hit_start))" >>"$short_timings_file"
  [[ "$redirect_code" == "301" || "$redirect_code" == "302" ]] || fail "shortlink redirect failed on hit ${i}: ${redirect_code}"

  location="$(awk 'tolower($1) == "location:" {print $2}' "$headers_file" | tr -d '\r' | tail -1)"
  [[ "$location" == *"/result/${result_id}"* ]] || fail "unexpected redirect location on hit ${i}: ${location}"
done
short_end="$(now_ms)"
short_elapsed_ms=$((short_end - short_start))
short_avg_ms=$((short_elapsed_ms / SHORTLINK_HITS))
short_p95_ms="$(p95_ms "$short_timings_file")"

admin_start="$(now_ms)"
for i in $(seq 1 "$ADMIN_HITS"); do
  hit_start="$(now_ms)"
  curl -fsS \
    -H "X-Admin-Token: ${ADMIN_TOKEN}" \
    "$BASE_URL/api/admin/overview?includeSynthetic=true" \
    -o "$overview_response"
  hit_end="$(now_ms)"
  echo "$((hit_end - hit_start))" >>"$admin_timings_file"
done
admin_end="$(now_ms)"
admin_elapsed_ms=$((admin_end - admin_start))
admin_avg_ms=$((admin_elapsed_ms / ADMIN_HITS))
admin_p95_ms="$(p95_ms "$admin_timings_file")"

curl -fsS \
  -H "X-Admin-Token: ${ADMIN_TOKEN}" \
  "$BASE_URL/api/admin/overview?includeSynthetic=true&forceRefresh=true" \
  -o "$overview_response"
result_created="$(json_get "$overview_response" data.resultCreated)"
[[ "$result_created" -ge 1 ]] || fail "admin overview did not record result creation"
curl -fsS \
  -H "X-Admin-Token: ${ADMIN_TOKEN}" \
  "$BASE_URL/api/admin/visit-events/runtime" \
  -o "$runtime_response"
async_queue_size="$(json_get "$runtime_response" data.queueSize)"
async_queue_capacity="$(json_get "$runtime_response" data.queueCapacity)"
async_drain_limit="$(json_get "$runtime_response" data.drainLimit)"
async_dropped_events="$(json_get "$runtime_response" data.droppedAsyncEvents)"
async_total_flushed_events="$(json_get "$runtime_response" data.totalFlushedEvents)"
async_last_flush_at="$(json_get "$runtime_response" data.lastFlushAt)"
async_last_batch_size="$(json_get "$runtime_response" data.lastBatchSize)"
async_batch_write_failures="$(json_get "$runtime_response" data.batchWriteFailures)"
async_worker_alive="$(json_get "$runtime_response" data.workerAlive)"
async_mode="$(json_get "$runtime_response" data.asyncMode)"
rocketmq_available="$(json_get "$runtime_response" data.rocketMqAvailable)"
rocketmq_consumer_enabled="$(json_get "$runtime_response" data.rocketMqConsumerEnabled)"
rocketmq_fallback_to_local="$(json_get "$runtime_response" data.rocketMqFallbackToLocal)"
rocketmq_topic="$(json_get "$runtime_response" data.rocketMqTopic)"
rocketmq_published_events="$(json_get "$runtime_response" data.rocketMqPublishedEvents)"
rocketmq_publish_failures="$(json_get "$runtime_response" data.rocketMqPublishFailures)"
rocketmq_fallback_events="$(json_get "$runtime_response" data.rocketMqFallbackEvents)"
rocketmq_shadow_local_events="$(json_get "$runtime_response" data.rocketMqShadowLocalEvents)"
rocketmq_consumer_persistence_ready="$(json_get "$runtime_response" data.rocketMqConsumerPersistenceReady)"
runtime_health_status="$(json_get "$runtime_response" data.healthStatus)"
runtime_health_message="$(json_get "$runtime_response" data.healthMessage)"
[[ -n "$async_queue_size" ]] || fail "async queue size missing"
[[ -n "$async_dropped_events" ]] || fail "async dropped events missing"
[[ -n "$async_total_flushed_events" ]] || fail "async total flushed events missing"
[[ -n "$async_last_batch_size" ]] || fail "async last batch size missing"
[[ -n "$async_batch_write_failures" ]] || fail "async batch write failures missing"
[[ -n "$runtime_health_status" ]] || fail "runtime health status missing"
[[ "$async_worker_alive" == "true" ]] || fail "async worker is not alive"
[[ "$runtime_health_status" != "danger" ]] || fail "runtime health is danger: ${runtime_health_message}"
if [[ "$MAX_SHORTLINK_AVG_MS" -gt 0 && "$short_avg_ms" -gt "$MAX_SHORTLINK_AVG_MS" ]]; then
  fail "shortlink average ${short_avg_ms}ms exceeded MAX_SHORTLINK_AVG_MS=${MAX_SHORTLINK_AVG_MS}"
fi
if [[ "$MAX_ADMIN_AVG_MS" -gt 0 && "$admin_avg_ms" -gt "$MAX_ADMIN_AVG_MS" ]]; then
  fail "admin average ${admin_avg_ms}ms exceeded MAX_ADMIN_AVG_MS=${MAX_ADMIN_AVG_MS}"
fi
if [[ "$MAX_SHORTLINK_P95_MS" -gt 0 && "$short_p95_ms" -gt "$MAX_SHORTLINK_P95_MS" ]]; then
  fail "shortlink p95 ${short_p95_ms}ms exceeded MAX_SHORTLINK_P95_MS=${MAX_SHORTLINK_P95_MS}"
fi
if [[ "$MAX_ADMIN_P95_MS" -gt 0 && "$admin_p95_ms" -gt "$MAX_ADMIN_P95_MS" ]]; then
  fail "admin p95 ${admin_p95_ms}ms exceeded MAX_ADMIN_P95_MS=${MAX_ADMIN_P95_MS}"
fi
if [[ -n "$MAX_ASYNC_QUEUE_SIZE" && "$async_queue_size" -gt "$MAX_ASYNC_QUEUE_SIZE" ]]; then
  fail "async queue size ${async_queue_size} exceeded MAX_ASYNC_QUEUE_SIZE=${MAX_ASYNC_QUEUE_SIZE}"
fi
if [[ -n "$MAX_ASYNC_DROPPED_EVENTS" && "$async_dropped_events" -gt "$MAX_ASYNC_DROPPED_EVENTS" ]]; then
  fail "async dropped events ${async_dropped_events} exceeded MAX_ASYNC_DROPPED_EVENTS=${MAX_ASYNC_DROPPED_EVENTS}"
fi
if [[ -n "$MAX_ASYNC_BATCH_FAILURES" && "$async_batch_write_failures" -gt "$MAX_ASYNC_BATCH_FAILURES" ]]; then
  fail "async batch write failures ${async_batch_write_failures} exceeded MAX_ASYNC_BATCH_FAILURES=${MAX_ASYNC_BATCH_FAILURES}"
fi

write_smoke_artifacts

echo "Performance smoke test passed"
echo "baseUrl=${BASE_URL}"
echo "resultId=${result_id}"
echo "shortCode=${short_code}"
echo "syntheticChannel=${SYNTHETIC_CHANNEL}"
echo "syntheticCampaign=${SYNTHETIC_CAMPAIGN}"
echo "runId=${RUN_ID}"
echo "shortlinkHits=${SHORTLINK_HITS}"
echo "shortlinkTotalMs=${short_elapsed_ms}"
echo "shortlinkAvgMs=${short_avg_ms}"
echo "shortlinkP95Ms=${short_p95_ms}"
echo "maxShortlinkAvgMs=${MAX_SHORTLINK_AVG_MS}"
echo "maxShortlinkP95Ms=${MAX_SHORTLINK_P95_MS}"
echo "adminHits=${ADMIN_HITS}"
echo "adminTotalMs=${admin_elapsed_ms}"
echo "adminAvgMs=${admin_avg_ms}"
echo "adminP95Ms=${admin_p95_ms}"
echo "maxAdminAvgMs=${MAX_ADMIN_AVG_MS}"
echo "maxAdminP95Ms=${MAX_ADMIN_P95_MS}"
echo "asyncQueueSize=${async_queue_size}"
echo "asyncQueueCapacity=${async_queue_capacity}"
echo "asyncDrainLimit=${async_drain_limit}"
echo "asyncDroppedEvents=${async_dropped_events}"
echo "asyncTotalFlushedEvents=${async_total_flushed_events}"
echo "asyncLastFlushAt=${async_last_flush_at}"
echo "asyncLastBatchSize=${async_last_batch_size}"
echo "asyncBatchWriteFailures=${async_batch_write_failures}"
echo "asyncWorkerAlive=${async_worker_alive}"
echo "asyncMode=${async_mode}"
echo "rocketMqAvailable=${rocketmq_available}"
echo "rocketMqConsumerEnabled=${rocketmq_consumer_enabled}"
echo "rocketMqConsumerPersistenceReady=${rocketmq_consumer_persistence_ready}"
echo "rocketMqFallbackToLocal=${rocketmq_fallback_to_local}"
echo "rocketMqTopic=${rocketmq_topic}"
echo "rocketMqPublishedEvents=${rocketmq_published_events}"
echo "rocketMqPublishFailures=${rocketmq_publish_failures}"
echo "rocketMqFallbackEvents=${rocketmq_fallback_events}"
echo "rocketMqShadowLocalEvents=${rocketmq_shadow_local_events}"
echo "runtimeHealthStatus=${runtime_health_status}"
echo "runtimeHealthMessage=${runtime_health_message}"
echo "readinessStatus=${readiness_status}"
echo "maxAsyncQueueSize=${MAX_ASYNC_QUEUE_SIZE}"
echo "maxAsyncDroppedEvents=${MAX_ASYNC_DROPPED_EVENTS}"
echo "maxAsyncBatchFailures=${MAX_ASYNC_BATCH_FAILURES}"
if [[ -n "$SMOKE_OUT_DIR" ]]; then
  echo "smokeArtifactsDir=${SMOKE_OUT_DIR}"
fi
