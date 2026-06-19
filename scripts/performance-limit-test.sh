#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:18080}"
ADMIN_TOKEN="${ADMIN_TOKEN:-dev-token}"
STEPS="${STEPS:-1,2,4,8,16,32}"
REQUESTS_PER_STAGE="${REQUESTS_PER_STAGE:-120}"
STOP_P95_MS="${STOP_P95_MS:-1200}"
STOP_ERROR_RATE="${STOP_ERROR_RATE:-0.05}"
STOP_ON_RUNTIME_DANGER="${STOP_ON_RUNTIME_DANGER:-1}"
STRICT_RUNTIME_OBSERVATION="${STRICT_RUNTIME_OBSERVATION:-0}"
MAX_QUEUE_USAGE_PERCENT="${MAX_QUEUE_USAGE_PERCENT:-90}"
MAX_DROPPED_ASYNC_EVENTS="${MAX_DROPPED_ASYNC_EVENTS:-0}"
MAX_BATCH_WRITE_FAILURES="${MAX_BATCH_WRITE_FAILURES:-0}"
CONNECT_TIMEOUT="${CONNECT_TIMEOUT:-3}"
READ_TIMEOUT="${READ_TIMEOUT:-10}"
STAGE_COOLDOWN_SECONDS="${STAGE_COOLDOWN_SECONDS:-0}"
WORKLOAD="${WORKLOAD:-mixed}"
OUT_ROOT="${OUT_ROOT:-docs/performance-reports}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
OUT_DIR="${OUT_DIR:-${OUT_ROOT}/${RUN_ID}}"
SYNTHETIC_CHANNEL="${SYNTHETIC_CHANNEL:-perf-test}"
SYNTHETIC_CAMPAIGN="${SYNTHETIC_CAMPAIGN:-performance-limit-test}"
ALLOW_PUBLIC_LOAD_TEST="${ALLOW_PUBLIC_LOAD_TEST:-0}"
ALLOW_ROCKETMQ_SHADOW_LOAD_TEST="${ALLOW_ROCKETMQ_SHADOW_LOAD_TEST:-0}"
DEPLOYMENT_PROFILE="${DEPLOYMENT_PROFILE:-local-h2}"
GIT_SHA="${GIT_SHA:-$(git rev-parse --short HEAD 2>/dev/null || true)}"
GIT_SHA="${GIT_SHA:-unknown}"
if git diff --quiet 2>/dev/null && git diff --cached --quiet 2>/dev/null; then
  DEFAULT_GIT_DIRTY="clean"
else
  DEFAULT_GIT_DIRTY="dirty"
fi
GIT_DIRTY="${GIT_DIRTY:-$DEFAULT_GIT_DIRTY}"
JAVA_VERSION="${JAVA_VERSION:-$(java -version 2>&1 | head -n 1 | tr -d '"')}"
PYTHON_VERSION="${PYTHON_VERSION:-$(python3 --version 2>&1)}"
HOST_NAME="${HOST_NAME:-$(hostname 2>/dev/null || echo unknown)}"

python3 - "$BASE_URL" "$ADMIN_TOKEN" "$STEPS" "$REQUESTS_PER_STAGE" "$STOP_P95_MS" \
  "$STOP_ERROR_RATE" "$CONNECT_TIMEOUT" "$READ_TIMEOUT" "$WORKLOAD" "$OUT_DIR" \
  "$SYNTHETIC_CHANNEL" "$SYNTHETIC_CAMPAIGN" "$RUN_ID" "$ALLOW_PUBLIC_LOAD_TEST" \
  "$GIT_SHA" "$GIT_DIRTY" "$JAVA_VERSION" "$PYTHON_VERSION" "$HOST_NAME" \
  "$STOP_ON_RUNTIME_DANGER" "$MAX_QUEUE_USAGE_PERCENT" "$MAX_DROPPED_ASYNC_EVENTS" \
  "$MAX_BATCH_WRITE_FAILURES" "$ALLOW_ROCKETMQ_SHADOW_LOAD_TEST" "$DEPLOYMENT_PROFILE" \
  "$STAGE_COOLDOWN_SECONDS" "$STRICT_RUNTIME_OBSERVATION" <<'PY'
import csv
import json
import math
import os
import statistics
import sys
import time
import urllib.error
from urllib.parse import urlparse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

(
    base_url,
    admin_token,
    steps_raw,
    requests_per_stage_raw,
    stop_p95_raw,
    stop_error_rate_raw,
    connect_timeout_raw,
    read_timeout_raw,
    workload,
    out_dir,
    synthetic_channel,
synthetic_campaign,
run_id,
allow_public_load_test,
    git_sha,
    git_dirty,
    java_version,
    python_version,
    host_name,
    stop_on_runtime_danger_raw,
    max_queue_usage_percent_raw,
    max_dropped_async_events_raw,
    max_batch_write_failures_raw,
    allow_rocketmq_shadow_load_test,
    deployment_profile,
    stage_cooldown_seconds_raw,
    strict_runtime_observation_raw,
) = sys.argv[1:]

base_url = base_url.rstrip("/")
steps = [int(item.strip()) for item in steps_raw.split(",") if item.strip()]
requests_per_stage = int(requests_per_stage_raw)
stop_p95 = int(stop_p95_raw)
stop_error_rate = float(stop_error_rate_raw)
stop_on_runtime_danger = stop_on_runtime_danger_raw == "1"
strict_runtime_observation = strict_runtime_observation_raw == "1"
max_queue_usage_percent = float(max_queue_usage_percent_raw)
max_dropped_async_events = int(max_dropped_async_events_raw)
max_batch_write_failures = int(max_batch_write_failures_raw)
stage_cooldown_seconds = float(stage_cooldown_seconds_raw)
timeout = max(float(connect_timeout_raw), float(read_timeout_raw))
workload = workload.strip().lower()
supported_workloads = {"mixed", "shortlink", "result", "admin", "health"}
if workload not in supported_workloads:
    raise ValueError(f"unsupported WORKLOAD={workload!r}, expected one of {sorted(supported_workloads)}")

effective_synthetic_campaign = synthetic_campaign
if run_id and run_id not in synthetic_campaign:
    effective_synthetic_campaign = f"{synthetic_campaign}:{run_id}"
if len(effective_synthetic_campaign) > 64:
    effective_synthetic_campaign = effective_synthetic_campaign[:64]

host = (urlparse(base_url).hostname or "").lower()
is_loopback = host in {"localhost", "0.0.0.0", "::1"} or host.startswith("127.")


def write_preflight_failed(reason, phase="preflight", extra=None):
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, "preflight-failed.json")
    payload = {
        "status": "preflight_failed",
        "phase": phase,
        "reason": reason,
        "runId": run_id,
        "baseUrl": base_url,
        "workload": workload,
        "deploymentProfile": deployment_profile,
        "gitSha": git_sha,
        "gitState": git_dirty,
        "isLoopback": is_loopback,
        "allowPublicLoadTest": allow_public_load_test == "1",
        "allowRocketMqShadowLoadTest": allow_rocketmq_shadow_load_test == "1",
        "strictRuntimeObservation": strict_runtime_observation,
        "steps": steps,
        "requestsPerStage": requests_per_stage,
        "stageCooldownSeconds": stage_cooldown_seconds,
        "createdAt": datetime.now().isoformat(timespec="seconds"),
    }
    if extra:
        payload["extra"] = extra
    with open(path, "w", encoding="utf-8") as fp:
        json.dump(payload, fp, ensure_ascii=False, indent=2)
        fp.write("\n")
    return path


def refuse(reason, phase="preflight", extra=None):
    path = write_preflight_failed(reason, phase, extra)
    raise SystemExit(f"{reason}\npreflightFailedJson={path}")


if not is_loopback and allow_public_load_test != "1":
    refuse(
        "Refusing to run load test against non-loopback BASE_URL. "
        "Set ALLOW_PUBLIC_LOAD_TEST=1 only after confirming target,备案/授权, limits, and rollback.",
        "public-safety",
    )
if not is_loopback and requests_per_stage < max(steps) * 2:
    refuse(
        "Refusing public load test because REQUESTS_PER_STAGE is lower than max(STEPS) * 2. "
        "Use a larger sample per stage so configured concurrency is not mistaken for capacity.",
        "public-safety",
    )
if not is_loopback and deployment_profile.strip().lower() in {"", "local", "local-h2", "dev"}:
    refuse(
        "Refusing public load test because DEPLOYMENT_PROFILE still looks local/dev. "
        "Set DEPLOYMENT_PROFILE=public-compose or another explicit production profile label.",
        "public-safety",
    )
if not is_loopback and len(steps) > 1 and stage_cooldown_seconds < 30:
    refuse(
        "Refusing public multi-stage load test because STAGE_COOLDOWN_SECONDS is below 30. "
        "Set STAGE_COOLDOWN_SECONDS=30 or higher so queues and service resources can cool between stages.",
        "public-safety",
    )


def request(method, path, body=None, headers=None, follow_redirects=True):
    payload = None if body is None else json.dumps(body).encode("utf-8")
    request_headers = {
        "Content-Type": "application/json",
        "User-Agent": "wuxing-performance-limit-test/1.0",
        "X-Channel": synthetic_channel,
        "X-Campaign": effective_synthetic_campaign,
        "X-Perf-Run-Id": run_id,
        **(headers or {}),
    }
    opener = urllib.request.build_opener()
    if not follow_redirects:
        class NoRedirect(urllib.request.HTTPRedirectHandler):
            def redirect_request(self, req, fp, code, msg, hdrs, newurl):
                return None

        opener = urllib.request.build_opener(NoRedirect)
    start = time.perf_counter()
    status = 0
    size = 0
    error = ""
    location = ""
    try:
        req = urllib.request.Request(base_url + path, data=payload, headers=request_headers, method=method)
        with opener.open(req, timeout=timeout) as response:
            status = response.getcode()
            location = response.headers.get("Location", "")
            size = len(response.read())
    except urllib.error.HTTPError as exc:
        status = exc.code
        location = exc.headers.get("Location", "")
        try:
            size = len(exc.read())
        except Exception:
            size = 0
        if status not in (301, 302):
            error = str(exc)
    except Exception as exc:
        error = repr(exc)
    elapsed_ms = int((time.perf_counter() - start) * 1000)
    return status, elapsed_ms, size, error, location


def json_request(method, path, body=None, headers=None):
    payload = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        base_url + path,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "User-Agent": "wuxing-performance-limit-test/1.0",
            "X-Channel": synthetic_channel,
            "X-Campaign": effective_synthetic_campaign,
            "X-Perf-Run-Id": run_id,
            **(headers or {}),
        },
        method=method,
    )
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def percentile(values, ratio):
    if not values:
        return 0
    ordered = sorted(values)
    index = max(0, math.ceil(len(ordered) * ratio) - 1)
    return ordered[index]


def runtime_snapshot():
    try:
        return json_request("GET", "/api/admin/visit-events/runtime", headers={"X-Admin-Token": admin_token}).get("data") or {}
    except Exception as exc:
        return {"runtimeError": repr(exc)}


def runtime_observation_error(runtime):
    if runtime.get("runtimeError"):
        return runtime["runtimeError"]
    required_keys = [
        "healthStatus",
        "queueSize",
        "queueCapacity",
        "droppedAsyncEvents",
        "batchWriteFailures",
        "asyncMode",
    ]
    missing = [key for key in required_keys if key not in runtime]
    if missing:
        return "missing runtime keys: " + ", ".join(missing)
    return ""


def require_runtime_observable(runtime, phase):
    if is_loopback and not strict_runtime_observation:
        return
    error = runtime_observation_error(runtime)
    if error:
        target = "strict local" if is_loopback else "public"
        refuse(
            f"Refusing {target} load test because visit-event runtime is not observable during {phase}: {error}",
            phase,
            {"runtime": runtime},
        )


def require_public_runtime_baseline(runtime):
    if is_loopback:
        return
    dropped = runtime.get("droppedAsyncEvents")
    batch_failures = runtime.get("batchWriteFailures")
    if isinstance(dropped, (int, float)) and dropped > max_dropped_async_events:
        refuse(
            "Refusing public load test because preflight droppedAsyncEvents baseline "
            f"{dropped} exceeds MAX_DROPPED_ASYNC_EVENTS={max_dropped_async_events}.",
            "public-runtime-baseline",
            {"runtime": runtime},
        )
    if isinstance(batch_failures, (int, float)) and batch_failures > max_batch_write_failures:
        refuse(
            "Refusing public load test because preflight batchWriteFailures baseline "
            f"{batch_failures} exceeds MAX_BATCH_WRITE_FAILURES={max_batch_write_failures}.",
            "public-runtime-baseline",
            {"runtime": runtime},
        )


def runtime_delta(before, after, key):
    left = before.get(key)
    right = after.get(key)
    if isinstance(left, (int, float)) and isinstance(right, (int, float)):
        return right - left
    return None


def summarize_by_kind(rows):
    result = {}
    for kind in sorted({row["kind"] for row in rows}):
        subset = [row for row in rows if row["kind"] == kind]
        latencies = [row["elapsed_ms"] for row in subset]
        status_counts = {}
        for row in subset:
            status_counts[str(row["status"])] = status_counts.get(str(row["status"]), 0) + 1
        result[kind] = {
            "requests": len(subset),
            "ok": sum(1 for row in subset if row["ok"]),
            "errors": sum(1 for row in subset if not row["ok"]),
            "avgMs": round(statistics.mean(latencies), 2) if latencies else 0,
            "p50Ms": percentile(latencies, 0.50),
            "p95Ms": percentile(latencies, 0.95),
            "p99Ms": percentile(latencies, 0.99),
            "statusCounts": status_counts,
        }
    return result


def runtime_stop_reason(stage):
    if not is_loopback or strict_runtime_observation:
        before_error = runtime_observation_error(stage.get("runtimeBefore") or {})
        if before_error:
            return f"runtimeBefore unavailable: {before_error}"
        after_error = runtime_observation_error(stage.get("runtimeAfter") or {})
        if after_error:
            return f"runtimeAfter unavailable: {after_error}"
    runtime = stage.get("runtimeAfter") or {}
    delta = stage.get("runtimeDelta") or {}
    if stop_on_runtime_danger and runtime.get("healthStatus") == "danger":
        return "runtime healthStatus=danger"
    queue_size = runtime.get("queueSize")
    queue_capacity = runtime.get("queueCapacity")
    if isinstance(queue_size, (int, float)) and isinstance(queue_capacity, (int, float)) and queue_capacity > 0:
        usage = (queue_size * 100) / queue_capacity
        if usage >= max_queue_usage_percent:
            return f"queue usage {usage:.2f}% exceeded MAX_QUEUE_USAGE_PERCENT={max_queue_usage_percent:.2f}%"
    dropped_delta = delta.get("droppedAsyncEvents")
    if isinstance(dropped_delta, (int, float)) and dropped_delta > max_dropped_async_events:
        return f"droppedAsyncEvents delta {dropped_delta} exceeded MAX_DROPPED_ASYNC_EVENTS={max_dropped_async_events}"
    batch_failures_delta = delta.get("batchWriteFailures")
    if isinstance(batch_failures_delta, (int, float)) and batch_failures_delta > max_batch_write_failures:
        return f"batchWriteFailures delta {batch_failures_delta} exceeded MAX_BATCH_WRITE_FAILURES={max_batch_write_failures}"
    return ""


def seed_result():
    body = {
        "birthYear": 2005,
        "birthMonth": 12,
        "birthDay": None,
        "birthTimeRange": "UNKNOWN",
        "answers": [
            {"questionCode": "Q1", "optionCode": "FIRE"},
            {"questionCode": "Q2", "optionCode": "EARTH"},
            {"questionCode": "Q3", "optionCode": "WOOD"},
            {"questionCode": "Q4", "optionCode": "FIRE"},
            {"questionCode": "Q5", "optionCode": "WATER"},
        ],
    }
    response = json_request("POST", "/api/results", body, {"X-Client-Id": f"perf-limit-seed-{run_id}"})
    data = response.get("data") or {}
    result_id = data.get("resultId")
    short_code = data.get("shortCode")
    if not result_id or not short_code:
        raise RuntimeError(f"seed result failed: {response}")
    return result_id, short_code


def build_task(index, result_id, short_code):
    client_id = f"perf-limit-{run_id}-{index}"
    if workload == "shortlink":
        return "shortlink", "GET", f"/s/{short_code}", None, {"X-Client-Id": client_id}, False
    if workload == "result":
        return "result", "GET", f"/api/results/{result_id}", None, {"X-Client-Id": client_id}, True
    if workload == "admin":
        return "admin", "GET", "/api/admin/overview", None, {"X-Admin-Token": admin_token}, True
    if workload == "health":
        return "health", "GET", "/api/health", None, {}, True
    lane = index % 20
    if lane < 12:
        return "shortlink", "GET", f"/s/{short_code}", None, {"X-Client-Id": client_id}, False
    if lane < 16:
        return "result", "GET", f"/api/results/{result_id}", None, {"X-Client-Id": client_id}, True
    if lane < 18:
        return "admin", "GET", "/api/admin/overview", None, {"X-Admin-Token": admin_token}, True
    return "health", "GET", "/api/health", None, {}, True


def run_stage(concurrency, result_id, short_code):
    rows = []
    runtime_before = runtime_snapshot()
    stage_start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        futures = []
        for index in range(requests_per_stage):
            kind, method, path, body, headers, follow_redirects = build_task(index, result_id, short_code)
            futures.append((kind, path, pool.submit(request, method, path, body, headers, follow_redirects)))
        for kind, path, future in futures:
            status, elapsed_ms, size, error, location = future.result()
            if kind == "shortlink":
                ok = status in (301, 302) and bool(location)
                if status in (301, 302) and not location:
                    error = "redirect missing Location header"
            else:
                ok = 200 <= status < 300
            rows.append({
                "kind": kind,
                "path": path,
                "status": status,
                "ok": ok,
                "elapsed_ms": elapsed_ms,
                "size": size,
                "location": location,
                "error": error,
            })
    stage_elapsed_ms = int((time.perf_counter() - stage_start) * 1000)
    latencies = [row["elapsed_ms"] for row in rows]
    ok_count = sum(1 for row in rows if row["ok"])
    error_count = len(rows) - ok_count
    runtime_after = runtime_snapshot()
    return {
        "concurrency": concurrency,
        "requests": len(rows),
        "ok": ok_count,
        "errors": error_count,
        "errorRate": round(error_count / max(1, len(rows)), 4),
        "totalMs": stage_elapsed_ms,
        "rps": round(len(rows) / max(0.001, stage_elapsed_ms / 1000), 2),
        "avgMs": round(statistics.mean(latencies), 2) if latencies else 0,
        "p50Ms": percentile(latencies, 0.50),
        "p95Ms": percentile(latencies, 0.95),
        "p99Ms": percentile(latencies, 0.99),
        "runtimeBefore": runtime_before,
        "runtimeAfter": runtime_after,
        "runtimeDelta": {
            "totalFlushedEvents": runtime_delta(runtime_before, runtime_after, "totalFlushedEvents"),
            "droppedAsyncEvents": runtime_delta(runtime_before, runtime_after, "droppedAsyncEvents"),
            "batchWriteFailures": runtime_delta(runtime_before, runtime_after, "batchWriteFailures"),
            "rocketMqPublishedEvents": runtime_delta(runtime_before, runtime_after, "rocketMqPublishedEvents"),
            "rocketMqFallbackEvents": runtime_delta(runtime_before, runtime_after, "rocketMqFallbackEvents"),
            "rocketMqShadowLocalEvents": runtime_delta(runtime_before, runtime_after, "rocketMqShadowLocalEvents"),
        },
        "byKind": summarize_by_kind(rows),
        "rows": rows,
    }


def write_stage_csv(stage):
    path = os.path.join(out_dir, f"stage-c{stage['concurrency']}.csv")
    with open(path, "w", encoding="utf-8", newline="") as fp:
        writer = csv.DictWriter(
            fp,
            fieldnames=[
                "runId",
                "concurrency",
                "kind",
                "path",
                "status",
                "ok",
                "elapsed_ms",
                "size",
                "location",
                "error",
            ],
        )
        writer.writeheader()
        for row in stage["rows"]:
            writer.writerow({"runId": run_id, "concurrency": stage["concurrency"], **row})
    return path


def stage_markdown(stage):
    runtime = stage.get("runtimeAfter") or {}
    delta = stage.get("runtimeDelta") or {}
    queue_size = runtime.get("queueSize")
    queue_capacity = runtime.get("queueCapacity")
    queue_usage = "-"
    if isinstance(queue_size, (int, float)) and isinstance(queue_capacity, (int, float)) and queue_capacity > 0:
        queue_usage = f"{(queue_size * 100) / queue_capacity:.2f}%"
    return (
        f"| {stage['concurrency']} | {stage['requests']} | {stage['rps']} | "
        f"{stage['avgMs']} | {stage['p50Ms']} | {stage['p95Ms']} | {stage['p99Ms']} | "
        f"{stage['errorRate'] * 100:.2f}% | {runtime.get('queueSize', '-')} | {queue_usage} | "
        f"{delta.get('totalFlushedEvents', '-')} | {delta.get('droppedAsyncEvents', '-')} | "
        f"{delta.get('batchWriteFailures', '-')} | {runtime.get('healthStatus', '-')} |"
    )


started_at = datetime.now().isoformat(timespec="seconds")
health = json_request("GET", "/api/health")
try:
    readiness = json_request("GET", "/api/readiness")
except Exception as exc:
    refuse(f"Readiness check failed before load test: {exc}", "readiness")
readiness_status = ((readiness.get("data") or {}).get("status") or "").upper()
if readiness_status != "UP":
    refuse(
        f"Readiness status is not UP before load test: {readiness_status or '-'}",
        "readiness",
        {"readiness": readiness},
    )
questions = json_request("GET", "/api/questions")
preflight_runtime = runtime_snapshot()
require_runtime_observable(preflight_runtime, "preflight")
require_public_runtime_baseline(preflight_runtime)
if (
    not is_loopback
    and preflight_runtime.get("asyncMode") == "rocketmq"
    and allow_rocketmq_shadow_load_test != "1"
    and (
        preflight_runtime.get("rocketMqAvailable") is False
        or preflight_runtime.get("rocketMqConsumerPersistenceReady") is False
    )
):
    refuse(
        "Refusing public load test in RocketMQ shadow/fallback state. "
        "Set ALLOW_ROCKETMQ_SHADOW_LOAD_TEST=1 only when the goal is to test fallback behavior.",
        "rocketmq-public-safety",
        {"runtime": preflight_runtime},
    )
result_id, short_code = seed_result()
os.makedirs(out_dir, exist_ok=True)

stages = []
stop_reason = ""
for stage_index, concurrency in enumerate(steps):
    stage = run_stage(concurrency, result_id, short_code)
    write_stage_csv(stage)
    stages.append({key: value for key, value in stage.items() if key != "rows"})
    runtime_stop = runtime_stop_reason(stage)
    if runtime_stop:
        stop_reason = runtime_stop
        break
    if stage["p95Ms"] > stop_p95:
        stop_reason = f"p95 {stage['p95Ms']}ms exceeded STOP_P95_MS={stop_p95}"
        break
    if stage["errorRate"] > stop_error_rate:
        stop_reason = f"errorRate {stage['errorRate']:.2%} exceeded STOP_ERROR_RATE={stop_error_rate:.2%}"
        break
    if stage_index < len(steps) - 1 and stage_cooldown_seconds > 0:
        time.sleep(stage_cooldown_seconds)


def dominant_stage_kind(stage):
    by_kind = stage.get("byKind") or {}
    candidates = [
        (kind, stats.get("p95Ms", 0), stats.get("errors", 0))
        for kind, stats in by_kind.items()
        if stats.get("requests", 0) > 0
    ]
    if not candidates:
        return None
    return max(candidates, key=lambda item: (item[1], item[2]))


def analysis_items():
    if not stages:
        return ["本轮没有生成有效阶段数据，请优先检查 BASE_URL、ADMIN_TOKEN 和服务启动状态。"]
    last_stage = stages[-1]
    max_p95 = max(stage.get("p95Ms", 0) for stage in stages)
    max_error_rate = max(stage.get("errorRate", 0) for stage in stages)
    max_queue_usage = 0.0
    total_dropped_delta = 0
    total_batch_failure_delta = 0
    runtime_errors = []
    for stage in stages:
        runtime_before = stage.get("runtimeBefore") or {}
        runtime = stage.get("runtimeAfter") or {}
        before_error = runtime_observation_error(runtime_before)
        after_error = runtime_observation_error(runtime)
        if before_error:
            runtime_errors.append(f"c{stage.get('concurrency')}: before {before_error}")
        if after_error:
            runtime_errors.append(f"c{stage.get('concurrency')}: after {after_error}")
        queue_size = runtime.get("queueSize")
        queue_capacity = runtime.get("queueCapacity")
        if isinstance(queue_size, (int, float)) and isinstance(queue_capacity, (int, float)) and queue_capacity > 0:
            max_queue_usage = max(max_queue_usage, (queue_size * 100) / queue_capacity)
        delta = stage.get("runtimeDelta") or {}
        dropped = delta.get("droppedAsyncEvents")
        batch_failures = delta.get("batchWriteFailures")
        if isinstance(dropped, (int, float)):
            total_dropped_delta += dropped
        if isinstance(batch_failures, (int, float)):
            total_batch_failure_delta += batch_failures

    items = []
    if stop_reason:
        items.append(f"本轮触发停止条件：{stop_reason}。后续先处理该条件，再继续升并发。")
    else:
        items.append(
            f"本轮完成所有阶段，最高 P95 为 {max_p95}ms，最高错误率为 {max_error_rate:.2%}。"
        )

    dominant = dominant_stage_kind(last_stage)
    if dominant:
        kind, p95, errors = dominant
        items.append(f"最高并发阶段最慢接口类型是 `{kind}`，P95 为 {p95}ms，错误数 {errors}。")

    if runtime_errors:
        items.append(
            "访问事件运行态观测不完整："
            + "; ".join(runtime_errors[:3])
            + ("；建议用 STRICT_RUNTIME_OBSERVATION=1 重跑正式报告。" if not strict_runtime_observation else "。")
        )
    elif total_dropped_delta or total_batch_failure_delta:
        items.append(
            f"访问事件运行态出现风险：丢弃增量 {total_dropped_delta}，批量写失败增量 {total_batch_failure_delta}。"
        )
    else:
        items.append(f"访问事件运行态未观察到丢弃或批量写失败，最高队列水位 {max_queue_usage:.2f}%。")

    if workload == "health":
        items.append("这是 `health` 小流量格式验证，只证明脚本、环境卡片和 runtime 采集正常，不代表业务链路容量。")
    elif workload == "shortlink":
        items.append("这是短链热路径验证，重点看 302、Location、P95 和访问事件排水，不应直接推导结果页或后台容量。")
    elif workload == "admin":
        items.append("这是后台查询验证，适合观察 overview 缓存、日期口径和聚合查询，不代表用户端短链高峰。")
    else:
        items.append("这是混合或业务链路验证，可用于观察短链、结果读取、后台查询和健康接口的相对瓶颈。")

    if is_loopback:
        items.append("目标是本机 loopback，结论只能作为本地回归和脚本格式证据；公网容量需要在备案/授权后用真实 Nginx、MySQL、Redis 链路重测。")
    return items


analysis = analysis_items()
summary = {
    "runId": run_id,
    "baseUrl": base_url,
    "environment": {
        "gitSha": git_sha,
        "gitState": git_dirty,
        "javaVersion": java_version,
        "pythonVersion": python_version,
        "hostName": host_name,
        "deploymentProfile": deployment_profile,
    },
    "startedAt": started_at,
    "finishedAt": datetime.now().isoformat(timespec="seconds"),
    "health": health,
    "readiness": readiness,
    "questionsCount": len((questions.get("data") or [])),
    "resultId": result_id,
    "shortCode": short_code,
    "workload": workload,
    "syntheticChannel": synthetic_channel,
    "syntheticCampaign": synthetic_campaign,
    "effectiveSyntheticCampaign": effective_synthetic_campaign,
    "allowPublicLoadTest": allow_public_load_test == "1",
    "allowRocketMqShadowLoadTest": allow_rocketmq_shadow_load_test == "1",
    "preflightRuntime": preflight_runtime,
    "steps": steps,
    "requestsPerStage": requests_per_stage,
    "stopP95Ms": stop_p95,
    "stopErrorRate": stop_error_rate,
    "stopOnRuntimeDanger": stop_on_runtime_danger,
    "strictRuntimeObservation": strict_runtime_observation,
    "maxQueueUsagePercent": max_queue_usage_percent,
    "maxDroppedAsyncEvents": max_dropped_async_events,
    "maxBatchWriteFailures": max_batch_write_failures,
    "stageCooldownSeconds": stage_cooldown_seconds,
    "stopReason": stop_reason or "completed all stages",
    "analysis": analysis,
    "stages": stages,
}
summary_path = os.path.join(out_dir, "summary.json")
with open(summary_path, "w", encoding="utf-8") as fp:
    json.dump(summary, fp, ensure_ascii=False, indent=2)

md_path = os.path.join(out_dir, "report.md")
with open(md_path, "w", encoding="utf-8") as fp:
    fp.write("# 五行人格项目阶梯压测记录\n\n")
    fp.write("## 环境卡片\n\n")
    fp.write(f"- Run ID: `{run_id}`\n")
    fp.write(f"- Git SHA: `{git_sha}`\n")
    fp.write(f"- Git state: `{git_dirty}`\n")
    fp.write(f"- Java: `{java_version}`\n")
    fp.write(f"- Python: `{python_version}`\n")
    fp.write(f"- Host: `{host_name}`\n")
    fp.write(f"- Deployment profile: `{deployment_profile}`\n")
    fp.write(f"- Base URL: `{base_url}`\n")
    fp.write(f"- Started: `{started_at}`\n")
    fp.write(f"- Workload: `{workload}`\n")
    fp.write(f"- Readiness status: `{readiness_status}`\n")
    fp.write(f"- Synthetic Channel: `{synthetic_channel}`\n")
    fp.write(f"- Synthetic Campaign: `{synthetic_campaign}`\n")
    fp.write(f"- Effective Synthetic Campaign: `{effective_synthetic_campaign}`\n")
    fp.write(f"- Public target allowed: `{allow_public_load_test == '1'}`\n")
    fp.write(f"- RocketMQ shadow load allowed: `{allow_rocketmq_shadow_load_test == '1'}`\n")
    fp.write(f"- Stage cooldown seconds: `{stage_cooldown_seconds:g}`\n")
    fp.write(f"- Strict runtime observation: `{strict_runtime_observation}`\n")
    fp.write(f"- Preflight async mode: `{preflight_runtime.get('asyncMode', '-')}`\n")
    fp.write(f"- Preflight runtime health: `{preflight_runtime.get('healthStatus', '-')}`\n")
    fp.write(f"- Preflight RocketMQ available: `{preflight_runtime.get('rocketMqAvailable', '-')}`\n")
    fp.write(f"- Preflight MQ consumer ready: `{preflight_runtime.get('rocketMqConsumerPersistenceReady', '-')}`\n")
    fp.write(f"- Preflight local fallback: `{preflight_runtime.get('rocketMqFallbackToLocal', '-')}`\n")
    fp.write(f"- Result ID: `{result_id}`\n")
    fp.write(f"- Short Code: `{short_code}`\n")
    fp.write(f"- Stop condition: P95 > `{stop_p95}ms`, error rate > `{stop_error_rate:.2%}`, ")
    fp.write(
        f"queue usage >= `{max_queue_usage_percent:.2f}%`, dropped async events > `{max_dropped_async_events}`, "
        f"batch write failures > `{max_batch_write_failures}`, or runtime danger = `{stop_on_runtime_danger}`\n"
    )
    fp.write(f"- Stop reason: **{summary['stopReason']}**\n\n")
    fp.write("## 自动结论与下一步\n\n")
    for item in analysis:
        fp.write(f"- {item}\n")
    fp.write("\n")
    fp.write("| 并发 | 请求数 | RPS | Avg(ms) | P50 | P95 | P99 | 错误率 | 队列 | 队列水位 | 落库增量 | 丢弃增量 | 批量失败增量 | 健康 |\n")
    fp.write("| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |\n")
    for stage in stages:
        fp.write(stage_markdown(stage) + "\n")
    fp.write("\n## 分接口 P95\n\n")
    fp.write("| 并发 | shortlink | result | admin | health |\n")
    fp.write("| ---: | ---: | ---: | ---: | ---: |\n")
    for stage in stages:
        by_kind = stage.get("byKind") or {}
        fp.write(
            f"| {stage['concurrency']} | "
            f"{by_kind.get('shortlink', {}).get('p95Ms', '-')} | "
            f"{by_kind.get('result', {}).get('p95Ms', '-')} | "
            f"{by_kind.get('admin', {}).get('p95Ms', '-')} | "
            f"{by_kind.get('health', {}).get('p95Ms', '-')} |\n"
        )
    fp.write("\n## 解读口径\n\n")
    fp.write("- `WORKLOAD=mixed` 约为 60% 短链、20% 结果读取、10% 后台 overview、10% 健康检查。\n")
    fp.write("- `WORKLOAD=shortlink` 不跟随跳转，只验证 301/302 和 Location，更接近短链热路径本身。\n")
    fp.write("- `WORKLOAD=result|admin` 用于分别观察结果读取和后台查询的单路径边界。\n")
    fp.write("- `WORKLOAD=health` 仍会先执行脚本链路预检和 seed result，再进行 health 小流量请求，不是纯健康接口压测。\n")
    fp.write("- 公网目标会要求 `ALLOW_PUBLIC_LOAD_TEST=1`、每阶样本数至少为最大配置并发的 2 倍，并记录 `DEPLOYMENT_PROFILE`。\n")
    fp.write("- 公网多阶压测要求 `STAGE_COOLDOWN_SECONDS>=30`，避免队列、连接池或 JVM 状态未冷却就进入下一阶。\n")
    fp.write("- 公网且 `asyncMode=rocketmq` 时，如果 MQ 不可用或 consumer 未就绪，默认拒绝压测；只有显式设置 `ALLOW_ROCKETMQ_SHADOW_LOAD_TEST=1` 才测试 shadow/fallback。\n")
    fp.write("- 若 P95 上升但队列不积压，瓶颈更可能在 HTTP/DB 查询；若队列积压或丢弃增加，瓶颈在访问事件异步写入。\n")
    fp.write("- 本脚本适合单机阶梯探测，不替代生产级分布式压测。\n")

print(f"Performance limit test completed: {md_path}")
print(f"summaryJson={summary_path}")
print(f"stopReason={summary['stopReason']}")
PY
