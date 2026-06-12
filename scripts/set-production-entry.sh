#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/deploy/.env}"
BASE_URL="${BASE_URL:-}"
SHORT_LINK_EXTERNAL_DOMAIN_VALUE="${SHORT_LINK_EXTERNAL_DOMAIN_VALUE:-}"
NGINX_HTTP_PORT_VALUE="${NGINX_HTTP_PORT_VALUE:-127.0.0.1:8088}"
APPLY_COMPOSE="${APPLY_COMPOSE:-false}"
COMPOSE_FILE="${COMPOSE_FILE:-$ROOT_DIR/deploy/docker-compose.yml}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required"
}

host_from_base_url() {
  python3 - "$BASE_URL" <<'PY'
from urllib.parse import urlparse
import sys

value = sys.argv[1]
parsed = urlparse(value)
if parsed.scheme not in {"http", "https"} or not parsed.netloc:
    raise SystemExit("BASE_URL must include http:// or https:// and a host")
print(parsed.netloc)
PY
}

set_env_value() {
  local key="$1"
  local value="$2"
  python3 - "$ENV_FILE" "$key" "$value" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
key = sys.argv[2]
value = sys.argv[3]
lines = path.read_text(encoding="utf-8").splitlines()
updated = False
for index, line in enumerate(lines):
    if line.startswith(f"{key}="):
        lines[index] = f"{key}={value}"
        updated = True
        break
if not updated:
    lines.append(f"{key}={value}")
path.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY
}

[[ -f "$ENV_FILE" ]] || fail "env file not found: $ENV_FILE"
[[ -n "$BASE_URL" ]] || fail "BASE_URL is required, for example BASE_URL=http://82.157.137.36"
require_command python3

domain_value="${SHORT_LINK_EXTERNAL_DOMAIN_VALUE:-$(host_from_base_url)}"
backup_file="${ENV_FILE}.bak-entry-$(date +%Y%m%d%H%M%S)"
cp "$ENV_FILE" "$backup_file"

set_env_value APP_BASE_URL "$BASE_URL"
set_env_value SHORT_LINK_EXTERNAL_DOMAIN "$domain_value"
set_env_value NGINX_HTTP_PORT "$NGINX_HTTP_PORT_VALUE"

"$ROOT_DIR/scripts/deploy-preflight.sh" "$ENV_FILE"

echo "Production entry updated"
echo "envFile=$ENV_FILE"
echo "backupFile=$backup_file"
echo "appBaseUrl=$BASE_URL"
echo "shortLinkExternalDomain=$domain_value"
echo "nginxHttpPort=$NGINX_HTTP_PORT_VALUE"

if [[ "$APPLY_COMPOSE" == "true" ]]; then
  require_command docker
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --force-recreate backend nginx
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps
fi
