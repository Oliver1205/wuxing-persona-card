#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-deploy/.env}"
COMPOSE_FILE="${COMPOSE_FILE:-deploy/docker-compose.yml}"
TARGET_REF="${TARGET_REF:-}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

[[ -n "$TARGET_REF" ]] || fail "TARGET_REF is required, for example TARGET_REF=v2.1.0-growth-analytics-foundation"
[[ -f "$ENV_FILE" ]] || fail "env file not found: $ENV_FILE"
command -v git >/dev/null 2>&1 || fail "git is required"
command -v docker >/dev/null 2>&1 || fail "docker is required"

echo "Rollback target: $TARGET_REF"
git fetch --tags origin
git checkout "$TARGET_REF"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

echo "Rollback completed: $TARGET_REF"
