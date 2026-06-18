# iTodo

iTodo is a cross-platform TodoList HTTP API for mini-program, web, and mobile clients.

## Stack

- Java 21 LTS (Temurin)
- Spring Boot 4.0.6
- Spring MVC, Validation, Spring Security + JWT
- PostgreSQL 17, MyBatis-Plus, HikariCP, Flyway
- Redis 8.4, Redisson
- springdoc-openapi
- Actuator, Micrometer

## Local Development

Prepare PostgreSQL and Redis, then set environment variables if defaults are not suitable:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/itodo
export DB_USERNAME=itodo_app
export DB_PASSWORD=itodo_dev_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=change-me-to-a-strong-256-bit-secret
```

Run:

```bash
mvn spring-boot:run
```

Useful endpoints in `dev`:

- Health: `GET /actuator/health`
- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `GET /swagger-ui/index.html`

## Documentation

- API contract: [docs/openapi/openapi.yaml](docs/openapi/openapi.yaml)
- OpenSpec docs: [docs/openspec/](docs/openspec/)
- Deployment docs: [docs/deployment/](docs/deployment/)

## Security

Do not commit `.env` files or production secrets. Production deployment should use Nginx HTTPS reverse proxy and environment variables for DB, Redis, JWT, and WeChat mini-program credentials.
