#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${DOMAIN:-}"
EXPECTED_IP="${EXPECTED_IP:-}"
CHECK_WWW="${CHECK_WWW:-true}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

info() {
  echo "==> $*"
}

resolve_host() {
  local host="$1"
  python3 - "$host" <<'PY'
import socket
import sys

host = sys.argv[1]
addresses = sorted({
    item[4][0]
    for item in socket.getaddrinfo(host, None, proto=socket.IPPROTO_TCP)
})
print("\n".join(addresses))
PY
}

check_host() {
  local host="$1"
  local resolved_ips

  info "Resolve ${host}"
  resolved_ips="$(resolve_host "$host" || true)"
  [[ -n "$resolved_ips" ]] || fail "${host} did not resolve"
  echo "$resolved_ips"

  if [[ -n "$EXPECTED_IP" ]] && ! grep -Fx "$EXPECTED_IP" <<<"$resolved_ips" >/dev/null; then
    fail "${host} does not resolve to EXPECTED_IP=${EXPECTED_IP}"
  fi
}

[[ -n "$DOMAIN" ]] || fail "DOMAIN is required, for example DOMAIN=wuxingcard.com"
[[ "$DOMAIN" != http://* && "$DOMAIN" != https://* && "$DOMAIN" != */* ]] || fail "DOMAIN must be a host name only, without scheme or path"
command -v python3 >/dev/null 2>&1 || fail "python3 is required"

check_host "$DOMAIN"

if [[ "$CHECK_WWW" == "true" ]]; then
  check_host "www.${DOMAIN}"
fi

echo "DNS readiness passed"
echo "domain=${DOMAIN}"
if [[ -n "$EXPECTED_IP" ]]; then
  echo "expectedIp=${EXPECTED_IP}"
fi
