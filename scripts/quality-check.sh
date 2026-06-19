#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

run() {
  echo
  echo "==> $*"
  "$@"
}

echo "Quality gate for wuxing-persona-card"

run git diff --check

echo
echo "==> Verify quality gate script tracking status"
quality_gate_scripts=(
  scripts/verify-frontend-contracts.mjs
  scripts/verify-wuxing-preview.mjs
  scripts/verify-wuxing-preview-flow.mjs
  scripts/verify-wuxing-browser.mjs
  scripts/mysql-schema-smoke-test.sh
  scripts/verify-eight-hour-artifacts.sh
  scripts/frontend-live-gate.sh
  scripts/performance-limit-test.sh
  scripts/local-preview-smoke-test.sh
)
missing_tracked_scripts=()
for script in "${quality_gate_scripts[@]}"; do
  if ! git ls-files --error-unmatch "$script" >/dev/null 2>&1; then
    missing_tracked_scripts+=("$script")
  fi
done
if (( ${#missing_tracked_scripts[@]} > 0 )); then
  printf 'Untracked quality gate script: %s\n' "${missing_tracked_scripts[@]}" >&2
  if [[ "${CI:-}" == "true" || "${REQUIRE_TRACKED_QUALITY_SCRIPTS:-0}" == "1" ]]; then
    echo "Quality gate scripts must be tracked before CI/release." >&2
    exit 1
  fi
  echo "WARNING: Track these scripts before CI/release; local dirty-tree checks continue for iteration." >&2
fi

run bash -n scripts/quality-check.sh
run bash -n scripts/deploy-preflight.sh
run bash -n scripts/docker-smoke-test.sh
run bash -n scripts/capture-showcase-screenshots.sh
run bash -n scripts/frontend-live-gate.sh
run bash -n scripts/mysql-schema-smoke-test.sh
run bash -n scripts/performance-limit-test.sh
run bash -n scripts/performance-smoke-test.sh
run bash -n scripts/local-preview-smoke-test.sh
run bash -n scripts/verify-eight-hour-artifacts.sh
run bash -n scripts/production-smoke-test.sh
run bash -n scripts/domain-dns-readiness.sh
run bash -n scripts/domain-bind-preflight.sh
run bash -n scripts/production-health-check.sh
run bash -n scripts/set-production-entry.sh
run bash -n scripts/server-security-audit.sh
run bash -n scripts/backup-mysql.sh
run bash -n scripts/restore-mysql.sh
run bash -n scripts/deploy-rollback.sh
run bash -n scripts/external-shortlink-preflight.sh
run bash -n scripts/external-shortlink-smoke-test.sh

echo
echo "==> Ensure generated artifacts are not tracked"
tracked_generated_file="$(mktemp)"
trap 'rm -f "$tracked_generated_file"' EXIT
if git ls-files | grep -E '(^frontend/(dist|node_modules)/|^backend/target/)' >"$tracked_generated_file"; then
  cat "$tracked_generated_file"
  echo "Generated artifacts must not be tracked." >&2
  exit 1
fi

echo
echo "==> Scan user-facing source for forbidden deterministic or negative claims"
if rg -n "命中注定|一定会|破财|生病|遭遇灾祸|你们相克|不适合结婚|不适合合作|运势|死亡|疾病" \
  frontend/src backend/src/main/java backend/src/main/resources; then
  echo "Forbidden wording found in user-facing source." >&2
  exit 1
fi

echo
echo "==> Verify fresh MySQL schema has no duplicate legacy DDL"
if rg -n "ALTER TABLE visit_event|CREATE INDEX idx_visit_event|CREATE INDEX idx_short_link_status_created_at" \
  backend/src/main/resources/db/schema.sql; then
  echo "Fresh schema.sql must not contain duplicate legacy ALTER/CREATE INDEX statements." >&2
  exit 1
fi

run mvn -q -f backend/pom.xml test
run npm --prefix frontend run build
run node scripts/verify-frontend-contracts.mjs
run node scripts/verify-wuxing-preview.mjs
run node scripts/verify-wuxing-preview-flow.mjs
run node scripts/verify-wuxing-browser.mjs
run scripts/mysql-schema-smoke-test.sh
run docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
run docker compose --env-file deploy/.env.external.example -f deploy/docker-compose.yml -f deploy/docker-compose.external-mode.yml config

echo
echo "Quality gate passed."
