# PostgreSQL Restore Test

Run this procedure periodically and before major releases to prove backups are usable.

## Prerequisites

- A recent backup created by `scripts/backup-postgres.sh`.
- A disposable PostgreSQL database that is not production.
- Credentials for a database user allowed to create/drop objects in the disposable database.

## Procedure

```bash
export RESTORE_DB=itodo_restore_test
export BACKUP_FILE=/var/backups/itodo/itodo_YYYYmmdd_HHMMSS.dump

createdb "$RESTORE_DB"
pg_restore --clean --if-exists --no-owner --dbname "$RESTORE_DB" "$BACKUP_FILE"
```

## Verification

```bash
psql "$RESTORE_DB" -c "select count(*) from users;"
psql "$RESTORE_DB" -c "select count(*) from todos;"
psql "$RESTORE_DB" -c "select max(version) from sync_changes;"
```

Record the backup filename, restore date, operator, and verification result in the operations log.

## Cleanup

```bash
dropdb "$RESTORE_DB"
```

Never restore production backups into a publicly reachable database or developer workstation without approval.
