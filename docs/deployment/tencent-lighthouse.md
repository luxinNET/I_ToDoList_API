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
JWT_SECRET=replace-with-strong-secret
WECHAT_MINI_PROGRAM_APP_ID=replace-me
WECHAT_MINI_PROGRAM_SECRET=replace-me
```

## systemd Service

Create `/etc/systemd/system/itodo.service`:

```ini
[Unit]
Description=iTodo Spring Boot API
After=network.target postgresql.service redis-server.service

[Service]
User=deploy
Group=deploy
WorkingDirectory=/opt/itodo
EnvironmentFile=/etc/itodo/itodo.env
ExecStart=/usr/bin/java -jar /opt/itodo/i-todo.jar
Restart=always
RestartSec=10
SuccessExitStatus=143
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ReadWritePaths=/opt/itodo/logs

[Install]
WantedBy=multi-user.target
```

Reload and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now itodo
sudo systemctl status itodo
```

## Nginx

Use [nginx.conf.example](nginx.conf.example), replace the domain and certificate paths, then run:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## Backups

Run daily PostgreSQL backups and periodically test restore. See [security-checklist.md](security-checklist.md).
