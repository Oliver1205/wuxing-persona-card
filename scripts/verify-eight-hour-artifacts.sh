#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_file() {
  local path="$1"
  [[ -s "$path" ]] || fail "required artifact is missing or empty: $path"
}

require_contains() {
  local path="$1"
  local pattern="$2"
  rg -q "$pattern" "$path" || fail "required pattern '$pattern' not found in $path"
}

require_png_geometry() {
  local path="$1"
  local expected_width="$2"
  local min_height="$3"
  python3 - "$path" "$expected_width" "$min_height" <<'PY'
import pathlib
import struct
import sys

path = pathlib.Path(sys.argv[1])
expected_width = int(sys.argv[2])
min_height = int(sys.argv[3])
data = path.read_bytes()
if data[:8] != b"\x89PNG\r\n\x1a\n":
    raise SystemExit(f"ERROR: {path} is not a PNG file")
width, height = struct.unpack(">II", data[16:24])
if width != expected_width or height < min_height:
    raise SystemExit(f"ERROR: {path} has unexpected geometry: {width}x{height}")
PY
}

required_files=(
  "docs/eight-hour-performance-showcase-delivery.md"
  "docs/admin-data-center-guide.md"
  "docs/admin-metric-dictionary.md"
  "docs/local-preview-runbook.md"
  "docs/performance-reports/README.md"
  "docs/performance-visual-brief.md"
  "docs/production-load-observability-checklist.md"
  "docs/performance-optimization-plan.md"
  "docs/artifacts/presentations/README.md"
  "docs/artifacts/presentations/wuxing-showcase-speaker-notes.md"
  "docs/artifacts/presentations/wuxing-persona-project-showcase.pptx"
  "docs/artifacts/presentations/contact-sheet.png"
  "scripts/performance-limit-test.sh"
  "scripts/local-preview-smoke-test.sh"
  "docs/screenshots/showcase/iphone-se-01-home.png"
  "docs/screenshots/showcase/iphone-se-02-test-birth-card.png"
  "docs/screenshots/showcase/iphone-se-02b-test-birth-ready.png"
  "docs/screenshots/showcase/iphone-se-03-test-question-card.png"
  "docs/screenshots/showcase/iphone-se-04-result.png"
  "docs/screenshots/showcase/iphone-se-05-shared-result.png"
  "docs/screenshots/showcase/iphone-se-06-match.png"
  "docs/screenshots/showcase/iphone-se-07-not-found.png"
  "docs/screenshots/showcase/iphone-se-08-admin-overview.png"
  "docs/screenshots/showcase/iphone-se-09-shortlink-detail.png"
  "docs/screenshots/showcase/iphone-se-10-admin-report-expanded.png"
  "docs/screenshots/showcase/iphone-se-11-admin-report-core.png"
  "docs/screenshots/showcase/iphone-se-12-admin-report-trend.png"
  "docs/screenshots/showcase/android-wide-01-home.png"
  "docs/screenshots/showcase/android-wide-02-test-birth-card.png"
  "docs/screenshots/showcase/android-wide-02b-test-birth-ready.png"
  "docs/screenshots/showcase/android-wide-03-test-question-card.png"
  "docs/screenshots/showcase/android-wide-04-result.png"
  "docs/screenshots/showcase/android-wide-05-shared-result.png"
  "docs/screenshots/showcase/android-wide-06-match.png"
  "docs/screenshots/showcase/android-wide-07-not-found.png"
  "docs/screenshots/showcase/android-wide-08-admin-overview.png"
  "docs/screenshots/showcase/android-wide-09-shortlink-detail.png"
  "docs/screenshots/showcase/android-wide-10-admin-report-expanded.png"
  "docs/screenshots/showcase/android-wide-11-admin-report-core.png"
  "docs/screenshots/showcase/android-wide-12-admin-report-trend.png"
  "docs/screenshots/showcase/desktop-06-admin-overview.png"
  "docs/screenshots/showcase/desktop-07-result.png"
  "docs/screenshots/showcase/desktop-08-shortlink-detail.png"
  "docs/screenshots/showcase/desktop-09-match.png"
  "docs/screenshots/showcase/desktop-10-not-found.png"
)

for file in "${required_files[@]}"; do
  require_file "$file"
done

screenshot_geometry_checks=(
  "docs/screenshots/showcase/iphone-se-01-home.png 375 667"
  "docs/screenshots/showcase/iphone-se-02-test-birth-card.png 375 667"
  "docs/screenshots/showcase/iphone-se-02b-test-birth-ready.png 375 667"
  "docs/screenshots/showcase/iphone-se-03-test-question-card.png 375 667"
  "docs/screenshots/showcase/iphone-se-04-result.png 375 667"
  "docs/screenshots/showcase/iphone-se-05-shared-result.png 375 667"
  "docs/screenshots/showcase/iphone-se-06-match.png 375 667"
  "docs/screenshots/showcase/iphone-se-07-not-found.png 375 667"
  "docs/screenshots/showcase/iphone-se-08-admin-overview.png 375 667"
  "docs/screenshots/showcase/iphone-se-09-shortlink-detail.png 375 667"
  "docs/screenshots/showcase/iphone-se-10-admin-report-expanded.png 375 667"
  "docs/screenshots/showcase/iphone-se-11-admin-report-core.png 375 667"
  "docs/screenshots/showcase/iphone-se-12-admin-report-trend.png 375 667"
  "docs/screenshots/showcase/android-wide-01-home.png 430 932"
  "docs/screenshots/showcase/android-wide-02-test-birth-card.png 430 932"
  "docs/screenshots/showcase/android-wide-02b-test-birth-ready.png 430 932"
  "docs/screenshots/showcase/android-wide-03-test-question-card.png 430 932"
  "docs/screenshots/showcase/android-wide-04-result.png 430 932"
  "docs/screenshots/showcase/android-wide-05-shared-result.png 430 932"
  "docs/screenshots/showcase/android-wide-06-match.png 430 932"
  "docs/screenshots/showcase/android-wide-07-not-found.png 430 932"
  "docs/screenshots/showcase/android-wide-08-admin-overview.png 430 932"
  "docs/screenshots/showcase/android-wide-09-shortlink-detail.png 430 932"
  "docs/screenshots/showcase/android-wide-10-admin-report-expanded.png 430 932"
  "docs/screenshots/showcase/android-wide-11-admin-report-core.png 430 932"
  "docs/screenshots/showcase/android-wide-12-admin-report-trend.png 430 932"
  "docs/screenshots/showcase/desktop-06-admin-overview.png 1280 900"
  "docs/screenshots/showcase/desktop-07-result.png 1280 900"
  "docs/screenshots/showcase/desktop-08-shortlink-detail.png 1280 900"
  "docs/screenshots/showcase/desktop-09-match.png 1280 900"
  "docs/screenshots/showcase/desktop-10-not-found.png 1280 900"
)

for screenshot_check in "${screenshot_geometry_checks[@]}"; do
  read -r screenshot_path expected_width min_height <<<"$screenshot_check"
  require_png_geometry "$screenshot_path" "$expected_width" "$min_height"
done

archived_contact_sheet="docs/artifacts/presentations/contact-sheet.png"

report_dirs=(
  "docs/performance-reports/20260614-010218"
  "docs/performance-reports/20260614-010616"
  "docs/performance-reports/20260614-010708"
  "docs/performance-reports/20260614-011017"
  "docs/performance-reports/workflow-health-verify"
  "docs/performance-reports/workflow-health-env-card"
  "docs/performance-reports/workflow-shortlink-location-verify"
  "docs/performance-reports/workflow-health-analysis-section"
  "docs/performance-reports/workflow-mixed-current-sanity"
  "docs/performance-reports/workflow-admin-current-sanity"
  "docs/performance-reports/workflow-result-current-sanity"
  "docs/performance-reports/workflow-shortlink-current-sanity"
)

for dir in "${report_dirs[@]}"; do
  require_file "$dir/report.md"
  require_file "$dir/summary.json"
done

python3 - "${report_dirs[@]}" <<'PY'
import json
import pathlib
import sys

for raw_dir in sys.argv[1:]:
    path = pathlib.Path(raw_dir) / "summary.json"
    with path.open("r", encoding="utf-8") as fp:
        summary = json.load(fp)
    required = ["baseUrl", "stopReason", "stages"]
    missing = [key for key in required if key not in summary]
    if missing:
        raise SystemExit(f"ERROR: {path} missing keys: {', '.join(missing)}")
    if not isinstance(summary["stages"], list) or not summary["stages"]:
        raise SystemExit(f"ERROR: {path} has no stage records")
    legacy_workflow_dirs = {"workflow-health-verify"}
    if pathlib.Path(raw_dir).name.startswith("workflow-") and pathlib.Path(raw_dir).name not in legacy_workflow_dirs:
        newer_required = ["runId", "workload", "syntheticChannel"]
        newer_missing = [key for key in newer_required if key not in summary]
        if newer_missing:
            raise SystemExit(f"ERROR: {path} missing newer-format keys: {', '.join(newer_missing)}")
    if pathlib.Path(raw_dir).name == "workflow-health-analysis-section":
        analysis = summary.get("analysis")
        if not isinstance(analysis, list) or not analysis:
            raise SystemExit(f"ERROR: {path} missing non-empty analysis list")
        if "stageCooldownSeconds" not in summary:
            raise SystemExit(f"ERROR: {path} missing stageCooldownSeconds")
PY

require_contains "docs/eight-hour-performance-showcase-delivery.md" "项目展示 PPT"
require_contains "docs/eight-hour-performance-showcase-delivery.md" "本地预览 smoke"
require_contains "docs/eight-hour-performance-showcase-delivery.md" "按目的找成果"
require_contains "docs/eight-hour-performance-showcase-delivery.md" "本地复测入口"
require_contains "docs/eight-hour-performance-showcase-delivery.md" "建议提交拆分"
require_contains "docs/eight-hour-performance-showcase-delivery.md" "Smoke 证据落盘验证"
require_contains "docs/artifacts/presentations/artifact-build-manifest.json" "archivedContactSheet"
require_contains "docs/performance-reports/README.md" "legacy mixed"
require_contains "docs/performance-reports/README.md" "workflow-health-verify"
require_contains "docs/performance-reports/README.md" "workflow-shortlink-location-verify"
require_contains "docs/performance-reports/README.md" "workflow-mixed-current-sanity"
require_contains "docs/performance-reports/README.md" "workflow-admin-current-sanity"
require_contains "docs/performance-reports/README.md" "workflow-result-current-sanity"
require_contains "docs/performance-reports/README.md" "workflow-shortlink-current-sanity"
require_contains "docs/performance-reports/workflow-health-verify/report.md" 'Workload: `health`'
require_contains "docs/performance-reports/workflow-health-analysis-section/report.md" "自动结论与下一步"
require_contains "docs/performance-reports/workflow-health-analysis-section/report.md" "Stage cooldown seconds"
require_contains "docs/performance-reports/workflow-mixed-current-sanity/report.md" 'Workload: `mixed`'
require_contains "docs/performance-reports/workflow-admin-current-sanity/report.md" 'Workload: `admin`'
require_contains "docs/performance-reports/workflow-result-current-sanity/report.md" 'Workload: `result`'
require_contains "docs/performance-reports/workflow-shortlink-current-sanity/report.md" 'Workload: `shortlink`'
require_contains "scripts/performance-smoke-test.sh" "SMOKE_OUT_DIR"
require_contains "docs/performance-reports/README.md" "SMOKE_OUT_DIR"
require_contains "scripts/performance-limit-test.sh" "preflight-failed.json"
require_contains "docs/performance-reports/README.md" "preflight-failed.json"
require_contains "docs/performance-optimization-plan.md" "RocketMQ"
require_contains "docs/production-load-observability-checklist.md" "公网分路径压测命令模板"
require_contains "docs/production-load-observability-checklist.md" "preflight-failed.json"
require_contains "docs/admin-data-center-guide.md" "perf-test"
require_contains "docs/admin-data-center-guide.md" "采集链路"
require_contains "docs/admin-data-center-guide.md" "人格分布"

echo "Eight-hour artifact verification passed"
echo "archivedContactSheet=$archived_contact_sheet"
echo "reportsChecked=${#report_dirs[@]}"
