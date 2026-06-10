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

run bash -n scripts/quality-check.sh
run bash -n scripts/deploy-preflight.sh
run bash -n scripts/docker-smoke-test.sh
run bash -n scripts/external-shortlink-preflight.sh
run bash -n scripts/external-shortlink-smoke-test.sh

echo
echo "==> Ensure generated artifacts are not tracked"
if git ls-files | grep -E '(^frontend/(dist|node_modules)/|^backend/target/)' >/tmp/wuxing-tracked-generated.txt; then
  cat /tmp/wuxing-tracked-generated.txt
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

run mvn -q -f backend/pom.xml test
run npm --prefix frontend run build
run docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
run docker compose --env-file deploy/.env.external.example -f deploy/docker-compose.yml -f deploy/docker-compose.external-mode.yml config

echo
echo "Quality gate passed."
