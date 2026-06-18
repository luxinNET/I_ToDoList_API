# 基础设施架构

## 配置分层

项目使用 Spring profiles 区分环境：

- `dev`：本地开发默认环境。
- `test`：测试环境，关闭 Swagger UI 和 OpenAPI 文档入口。
- `prod`：生产环境，关闭 Swagger UI 和 OpenAPI 文档入口，限制 Actuator 暴露范围。

主要配置文件：

- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-test.yml`
- `src/main/resources/application-prod.yml`

敏感配置只允许通过环境变量注入，禁止写入仓库。

## 本地依赖

本地开发依赖 PostgreSQL 17 和 Redis 8.4。可使用 `docker-compose.dev.yml` 启动：

```bash
docker compose -f docker-compose.dev.yml up -d
```

环境变量示例见 `.env.example`。

## 统一响应

Controller 默认返回 `ApiResponse<T>`：

```json
{
  "success": true,
  "data": {},
  "traceId": "..."
}
```

错误响应：

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "details": []
  },
  "traceId": "..."
}
```

## Trace ID

`TraceIdFilter` 负责：

- 优先读取请求头 `X-Trace-Id`。
- 如果请求未提供，则生成 UUID。
- 将 traceId 写入 MDC。
- 将 traceId 写入响应头 `X-Trace-Id`。

日志格式中包含 traceId，便于排查跨端请求问题。

## 安全基础设施

阶段 1 已建立基础安全外壳：

- 无状态 Session。
- 关闭 CSRF。
- CORS 白名单通过环境变量配置。
- 认证失败返回 JSON 错误。
- 权限不足返回 JSON 错误。
- 密码编码器使用 BCrypt。

JWT 校验过滤器将在认证与用户模块阶段实现。

## Redis/Redisson 限流

阶段 1 提供 `RateLimiterService`，为后续认证模块接入：

- 登录限流。
- 注册限流。
- 刷新令牌限流。

默认规则在 `application.yml` 中配置，可通过配置文件调整。

## Actuator

开发环境暴露：

- `health`
- `info`
- `metrics`
- `prometheus`

生产环境默认只建议公开 `/actuator/health`，其他端点应由 Nginx 或安全组限制访问。
