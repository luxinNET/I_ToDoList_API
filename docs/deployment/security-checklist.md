# Production Security Checklist

## Host

- [ ] SSH key login works.
- [ ] Password SSH login disabled.
- [ ] Root SSH login disabled.
- [ ] Firewall/security group exposes only 22, 80, 443.
- [ ] System packages updated.

## Application

- [ ] `SPRING_PROFILES_ACTIVE=prod`.
- [ ] `JWT_SECRET` is strong, provided by environment variable, and not the committed default.
- [ ] Production startup fails when JWT secret is missing, too short, or default.
- [ ] `CORS_ALLOWED_ORIGIN_PATTERNS` contains only trusted HTTPS origins.
- [ ] WeChat credentials are environment variables only.
- [ ] Swagger UI and API docs disabled or protected in production.
- [ ] Only `/actuator/health` is publicly reachable.
- [ ] `/actuator/info`, metrics, and prometheus are not publicly reachable.
- [ ] Logs do not include passwords, JWTs, refresh tokens, or Authorization headers.

## Nginx

- [ ] HTTPS certificate installed.
- [ ] HTTP redirects to HTTPS.
- [ ] HSTS enabled only after HTTPS and subdomains are verified.
- [ ] Security headers enabled.
- [ ] Auth endpoints rate-limited.
- [ ] Hidden files blocked.

## Database and Redis

- [ ] PostgreSQL app user has least privileges.
- [ ] PostgreSQL not exposed publicly.
- [ ] Redis requires password and is not exposed publicly.
- [ ] Daily backups configured.
- [ ] Restore process tested using [restore-test.md](restore-test.md).

## Deployment Operations

- [ ] systemd unit installed from [itodo.service](itodo.service).
- [ ] Deployment uses release directories and `/opt/itodo/current` symlink.
- [ ] Rollback tested with [rollback.sh](../../scripts/rollback.sh).
- [ ] `/actuator/health` checked after deploy and rollback.
