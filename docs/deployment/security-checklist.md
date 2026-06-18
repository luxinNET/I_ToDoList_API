# Production Security Checklist

## Host

- [ ] SSH key login works.
- [ ] Password SSH login disabled.
- [ ] Root SSH login disabled.
- [ ] Firewall/security group exposes only 22, 80, 443.
- [ ] System packages updated.

## Application

- [ ] `SPRING_PROFILES_ACTIVE=prod`.
- [ ] JWT secret is strong and not committed.
- [ ] WeChat credentials are environment variables only.
- [ ] Swagger UI and API docs disabled or protected in production.
- [ ] Only `/actuator/health` is publicly reachable.
- [ ] Logs do not include passwords, JWTs, refresh tokens, or Authorization headers.

## Nginx

- [ ] HTTPS certificate installed.
- [ ] HTTP redirects to HTTPS.
- [ ] HSTS enabled only after HTTPS is verified.
- [ ] Security headers enabled.
- [ ] Auth endpoints rate-limited.
- [ ] Hidden files blocked.

## Database and Redis

- [ ] PostgreSQL app user has least privileges.
- [ ] PostgreSQL not exposed publicly.
- [ ] Redis requires password and is not exposed publicly.
- [ ] Daily backups configured.
- [ ] Restore process tested.
