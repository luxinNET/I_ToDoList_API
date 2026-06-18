# CLAUDE.md

## Project Overview

iTodo is a cross-platform TodoList HTTP API for mini-program, web, and mobile clients.

## Tech Stack

- JDK 21 LTS Temurin
- Spring Boot 4.0.6
- Spring MVC + Jakarta Validation
- Spring Security 7.x + JWT
- PostgreSQL 17 + HikariCP
- MyBatis-Plus 3.5.16
- Redis 8.4 + Redisson
- Flyway migrations
- springdoc-openapi
- Lombok + MapStruct + Hutool
- Actuator + Micrometer

## Commands

- Build and test: `mvn test`
- Package: `mvn clean package`
- Run locally: `mvn spring-boot:run`
- Run with profile: `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run`

## Architecture Rules

- Keep API paths under `/api/v1`.
- Use DTOs for API input/output; do not expose persistence entities directly.
- Validate all write request DTOs with Jakarta Validation.
- Use MapStruct for DTO mapping when entities are implemented.
- Use Flyway for every schema change; never manually mutate production schema.
- Use MyBatis-Plus for CRUD and XML only for complex SQL.
- All user-owned data queries must scope by authenticated user or shared-list permission.

## Security Rules

- Never commit secrets, tokens, passwords, or Tencent/WeChat credentials.
- Read secrets from environment variables.
- Protect `/api/v1/**` except public auth endpoints.
- Use short-lived access tokens and rotating refresh tokens.
- Store only refresh-token hashes.
- Do not use wildcard CORS with credentials.
- Hide or protect Swagger and Actuator metrics in production.

## Deployment

Production target is Tencent Cloud Lighthouse with Nginx HTTPS reverse proxy and systemd-managed Spring Boot JAR.

## Verification

Before considering a change done:

1. Run `mvn test`.
2. Confirm Flyway migrations apply cleanly in a PostgreSQL-backed environment.
3. Check `/actuator/health` locally when the app can start.
4. Keep OpenAPI/OpenSpec docs aligned with API behavior.
