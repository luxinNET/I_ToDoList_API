# 认证 OpenSpec

## 令牌模型

- Access token 使用 JWT Bearer token。
- Access token 默认有效期为 15 分钟。
- Refresh token 默认有效期为 30 天。
- Refresh token 只在服务端保存哈希值，不保存明文。
- 每次刷新 token 时执行 refresh token rotation：签发新 refresh token，并使旧 refresh token 失效。
- 如果检测到已失效 refresh token 被再次使用，应撤销同设备或同 token family 的会话。

## 登录方式

MVP 支持以下登录方式：

- 邮箱 + 密码。
- 手机号 + 密码。
- 微信小程序 `code` 登录。

## 微信小程序登录

- 客户端提交微信 `code`。
- 服务端通过微信接口换取 `openid` / `unionid`。
- 根据 `provider=WECHAT_MINI_PROGRAM` 和 `openid` 查找或创建用户身份。
- 微信 `appid` 和 `secret` 只能通过环境变量注入，不允许提交到仓库。

## 客户端请求头

```http
X-Client-Type: web | mini-program | ios | android
X-Client-Version: 1.0.0
X-Device-Id: client-generated-device-id
Authorization: Bearer <access-token>
```

## 安全规则

- 登录、注册、刷新 token 接口需要 Redis/Redisson 限流。
- 密码使用 Spring Security `PasswordEncoder` 加密保存。
- 不在日志中打印密码、JWT、refresh token 或完整 `Authorization` 请求头。
- 生产环境 CORS 只允许配置的可信域名。
