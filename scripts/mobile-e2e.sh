#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/frontend"

E2E_BASE_URL="${E2E_BASE_URL:-http://127.0.0.1:5175}"
E2E_ADMIN_TOKEN="${E2E_ADMIN_TOKEN:-dev-token}"

python3 - "$E2E_BASE_URL" "${ALLOW_PUBLIC_E2E:-0}" <<'PY'
import ipaddress
import sys
from urllib.parse import urlparse

url, allow_public = sys.argv[1], sys.argv[2]
host = urlparse(url).hostname or ""

def is_local_or_private(value):
    if value in {"localhost", "0.0.0.0", "::1"} or value.startswith("127."):
        return True
    try:
        return ipaddress.ip_address(value).is_private
    except ValueError:
        return False

if allow_public != "1" and not is_local_or_private(host):
    raise SystemExit(
        "ERROR: refusing to run mobile E2E against a public URL. "
        "Set ALLOW_PUBLIC_E2E=1 only during an authorized test window."
    )
PY

export E2E_BASE_URL
export E2E_ADMIN_TOKEN

if ! npm exec -- playwright --version >/dev/null 2>&1; then
  echo "Playwright is not installed. Run: cd frontend && npm install && npx playwright install chromium" >&2
  exit 1
fi

npm run e2e:mobile -- --trace=retain-on-failure --reporter=line,html
