# 部署 OpenSpec

## 生产部署目标

生产环境运行在腾讯云轻量应用服务器上：

- Nginx 作为 HTTPS 反向代理。
- Spring Boot JAR 由 systemd 管理。
- PostgreSQL 17 和 Redis 8.4 初期部署在同一台服务器。
- 所有密钥通过环境变量或受保护的环境文件注入。
- 配置每日 PostgreSQL 备份。
- Actuator 只公开 `/actuator/health`。

## Nginx 规则

- HTTP 自动跳转 HTTPS。
- 添加安全响应头。
- 限制请求体大小。
- 对认证接口做限流。
- 阻止访问隐藏文件。
- `/actuator/**` 除 health 外禁止公网访问。

## 服务器安全

- 使用非 root 用户部署应用。
- 使用 SSH key 登录。
- 禁用 root SSH 登录和密码登录。
- 安全组只开放 22、80、443。
- PostgreSQL 和 Redis 不暴露公网。

## 备份

- 每日执行 PostgreSQL dump。
- 至少保留 7 天日备份。
- 定期测试恢复流程。
- 环境变量文件和密钥需要单独安全备份。
