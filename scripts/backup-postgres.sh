#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR=${BACKUP_DIR:-/var/backups/itodo/postgres}
DB_NAME=${DB_NAME:-itodo}
DB_USERNAME=${DB_USERNAME:-itodo_app}
STAMP=$(date +%Y%m%d-%H%M%S)

mkdir -p "$BACKUP_DIR"
pg_dump -U "$DB_USERNAME" -d "$DB_NAME" -Fc -f "$BACKUP_DIR/$DB_NAME-$STAMP.dump"
find "$BACKUP_DIR" -type f -name "*.dump" -mtime +30 -delete
