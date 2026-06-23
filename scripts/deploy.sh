#!/usr/bin/env bash
set -euo pipefail

APP_JAR=${1:-target/i-todo-0.0.1-SNAPSHOT.jar}
REMOTE_DIR=${REMOTE_DIR:-/opt/itodo}
SERVICE=${SERVICE:-itodo}

if [[ ! -f "$APP_JAR" ]]; then
  echo "Jar not found: $APP_JAR" >&2
  exit 1
fi

RELEASE=${RELEASE:-$(date +%Y%m%d-%H%M%S)}
RELEASE_DIR="$REMOTE_DIR/releases/$RELEASE"

install -d "$RELEASE_DIR" "$REMOTE_DIR/logs"
cp "$APP_JAR" "$RELEASE_DIR/i-todo.jar"
ln -sfn "$RELEASE_DIR" "$REMOTE_DIR/current"
systemctl restart "$SERVICE"
./scripts/healthcheck.sh
systemctl status "$SERVICE" --no-pager
