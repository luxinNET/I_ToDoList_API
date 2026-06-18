#!/usr/bin/env bash
set -euo pipefail

APP_JAR=${1:-target/i-todo-0.0.1-SNAPSHOT.jar}
REMOTE_DIR=${REMOTE_DIR:-/opt/itodo}
SERVICE=${SERVICE:-itodo}

if [[ ! -f "$APP_JAR" ]]; then
  echo "Jar not found: $APP_JAR" >&2
  exit 1
fi

install -d "$REMOTE_DIR"
cp "$APP_JAR" "$REMOTE_DIR/i-todo.jar"
systemctl restart "$SERVICE"
systemctl status "$SERVICE" --no-pager
