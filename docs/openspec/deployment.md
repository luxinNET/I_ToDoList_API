# Deployment OpenSpec

Production runs on Tencent Cloud Lighthouse with:

- Nginx as HTTPS reverse proxy.
- Spring Boot JAR managed by systemd.
- PostgreSQL 17 and Redis 8.4 on the same host for initial deployment.
- Environment variables for secrets.
- Daily PostgreSQL backups.
- Only `/actuator/health` publicly exposed from Actuator.
