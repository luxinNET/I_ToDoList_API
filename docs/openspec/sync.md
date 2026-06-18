# 同步 OpenSpec

## MVP 同步策略

- 服务端是最终数据源。
- 所有写操作更新 `updated_at`，并写入逻辑版本或 `sync_changes` 记录。
- `GET /api/v1/sync/bootstrap` 返回客户端初始化所需的当前用户数据。
- `GET /api/v1/sync/changes?sinceVersion=` 返回指定版本之后的增量变更。
- MVP 阶段暂不实现离线写入推送和字段级冲突合并。

## 变更记录

`sync_changes` 记录以下信息：

- 用户 ID。
- 资源类型，例如 `TODO`、`LIST`、`STEP`、`TAG`。
- 资源 ID。
- 操作类型，例如 `CREATE`、`UPDATE`、`DELETE`。
- 服务端版本。
- 变更时间。

## 客户端建议

- 客户端首次登录后调用 bootstrap。
- 后续定期或启动时调用 changes。
- 客户端保存最后成功同步的 `version`。
- 如果版本过旧或服务端无法提供完整增量，客户端应重新 bootstrap。
