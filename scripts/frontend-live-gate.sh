#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1:5175}"
BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:48081}"
E2E_BASE_URL="${E2E_BASE_URL:-$FRONTEND_URL}"
E2E_ADMIN_TOKEN="${E2E_ADMIN_TOKEN:-${ADMIN_TOKEN:-dev-token}}"

export FRONTEND_URL
export BACKEND_URL
export E2E_BASE_URL
export E2E_ADMIN_TOKEN

run() {
  echo
  echo "==> $*"
  "$@"
}

echo "Frontend live gate for wuxing-persona-card"
echo "frontendUrl=$FRONTEND_URL"
echo "backendUrl=$BACKEND_URL"
echo "e2eBaseUrl=$E2E_BASE_URL"

run scripts/local-preview-smoke-test.sh
run scripts/mobile-e2e.sh
run scripts/capture-showcase-screenshots.sh
run scripts/verify-eight-hour-artifacts.sh

echo
echo "Frontend live gate passed."
