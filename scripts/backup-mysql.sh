#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-deploy/.env}"
BACKUP_DIR="${BACKUP_DIR:-backups}"
COMPOSE_FILE="${COMPOSE_FILE:-deploy/docker-compose.yml}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

[[ -f "$ENV_FILE" ]] || fail "env file not found: $ENV_FILE"
command -v docker >/dev/null 2>&1 || fail "docker is required"

mkdir -p "$BACKUP_DIR"
timestamp="$(date +%Y%m%d%H%M%S)"
backup_file="$BACKUP_DIR/wuxing-mysql-$timestamp.sql.gz"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T mysql \
  sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers "$MYSQL_DATABASE"' \
  | gzip -c >"$backup_file"

chmod 600 "$backup_file"
echo "MySQL backup created: $backup_file"
