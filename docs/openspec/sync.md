# Sync OpenSpec

## MVP Sync Strategy

- Server is the source of truth.
- Writes update `updated_at` and increment a logical version.
- `GET /api/v1/sync/bootstrap` returns initial state.
- `GET /api/v1/sync/changes?sinceVersion=` returns incremental changes.
- Offline write push and field-level conflict handling are deferred.
