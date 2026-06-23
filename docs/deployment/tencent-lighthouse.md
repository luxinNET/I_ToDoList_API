# Tencent Cloud Lighthouse Deployment

## Topology

Client HTTPS -> Nginx -> `127.0.0.1:8080` Spring Boot JAR -> PostgreSQL 17 / Redis 8.4.

## Server Baseline

1. Create a non-root user, for example `deploy`.
2. Configure SSH key login.
3. Disable SSH password login after confirming key login works.
4. Open only required security group ports: 22, 80, 443.
5. Restrict SSH source IP when possible.
6. Install JDK 21 Temurin, PostgreSQL 17, Redis 8.4, Nginx.

## Environment

Create `/etc/itodo/itodo.env` owned by root and readable by the service user only:

```ini
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=itodo
DB_USERNAME=itodo_app
DB_PASSWORD=replace-me
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=replace-me
JWT_SECRET=replace-with-strong-secret-at-least-32-bytes
CORS_ALLOWED_ORIGIN_PATTERNS=https://app.example.com,https://admin.example.com
WECHAT_MINI_PROGRAM_APP_ID=replace-me
WECHAT_MINI_PROGRAM_SECRET=replace-me
```

Production startup fails if `JWT_SECRET` is missing, too short, or left as the committed default. Do not use wildcard CORS origins with credentials in production.

## systemd Service

Copy [itodo.service](itodo.service) to `/etc/systemd/system/itodo.service` and review the paths/user names.

```bash
sudo cp docs/deployment/itodo.service /etc/systemd/system/itodo.service
sudo systemctl daemon-reload
sudo systemctl enable --now itodo
sudo systemctl status itodo
```

The unit runs `/opt/itodo/current/i-todo.jar`, so deployments should publish timestamped releases under `/opt/itodo/releases/<release>/` and move the `current` symlink.

## Nginx

Use [nginx.conf.example](nginx.conf.example), replace the domain and certificate paths, then run:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Keep HSTS commented out until HTTPS and all subdomains are verified.

## Deploy

Build the JAR in CI or a trusted build host, then run:

```bash
sudo ./scripts/deploy.sh target/i-todo-0.0.1-SNAPSHOT.jar
```

This creates `/opt/itodo/releases/<timestamp>/i-todo.jar`, updates `/opt/itodo/current`, restarts systemd, and runs the health check.

## Rollback

List releases and switch back to a known-good one:

```bash
ls -1 /opt/itodo/releases
sudo ./scripts/rollback.sh 20260623-120000
```

Rollback restarts the service and runs [healthcheck.sh](../../scripts/healthcheck.sh). If health check fails, inspect `journalctl -u itodo -n 200 --no-pager` before choosing another release.

## Backups and Restore Tests

Run daily PostgreSQL backups with [backup-postgres.sh](../../scripts/backup-postgres.sh). Periodically test restore using [restore-test.md](restore-test.md) and record the result in the operations log.
