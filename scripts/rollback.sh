#!/usr/bin/env bash
set -euo pipefail

REMOTE_DIR=${REMOTE_DIR:-/opt/itodo}
SERVICE=${SERVICE:-itodo}
HEALTHCHECK=${HEALTHCHECK:-./scripts/healthcheck.sh}

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <release-directory-name>" >&2
  echo "Example: $0 20260623-120000" >&2
  exit 1
fi

RELEASE="$1"
RELEASE_DIR="$REMOTE_DIR/releases/$RELEASE"

if [[ ! -f "$RELEASE_DIR/i-todo.jar" ]]; then
  echo "Release jar not found: $RELEASE_DIR/i-todo.jar" >&2
  exit 1
fi

ln -sfn "$RELEASE_DIR" "$REMOTE_DIR/current"
systemctl restart "$SERVICE"
"$HEALTHCHECK"
systemctl status "$SERVICE" --no-pager
