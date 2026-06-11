#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-deploy/.env}"
COMPOSE_FILE="${COMPOSE_FILE:-deploy/docker-compose.yml}"
BACKUP_FILE="${1:-}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

[[ -n "$BACKUP_FILE" ]] || fail "usage: $0 <backup.sql.gz>"
[[ -f "$BACKUP_FILE" ]] || fail "backup file not found: $BACKUP_FILE"
[[ -f "$ENV_FILE" ]] || fail "env file not found: $ENV_FILE"
command -v docker >/dev/null 2>&1 || fail "docker is required"

echo "About to restore $BACKUP_FILE into MySQL. Set CONFIRM_RESTORE=yes to proceed."
[[ "${CONFIRM_RESTORE:-}" == "yes" ]] || fail "restore aborted"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

gzip -dc "$BACKUP_FILE" | docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T mysql \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'

echo "MySQL restore completed: $BACKUP_FILE"
