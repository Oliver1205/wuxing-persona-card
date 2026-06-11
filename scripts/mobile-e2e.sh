#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/frontend"

E2E_BASE_URL="${E2E_BASE_URL:-http://127.0.0.1:5174}"
E2E_ADMIN_TOKEN="${E2E_ADMIN_TOKEN:-dev-token}"

export E2E_BASE_URL
export E2E_ADMIN_TOKEN

if ! npm exec -- playwright --version >/dev/null 2>&1; then
  echo "Playwright is not installed. Run: cd frontend && npm install && npx playwright install chromium" >&2
  exit 1
fi

npm run e2e:mobile
