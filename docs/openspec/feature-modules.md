# 功能模块 OpenSpec

## 设计原则

本项目面向跨端 TodoList 场景，功能设计参考成熟任务管理产品的通用能力，但仓库内文档、接口、代码命名均使用中性表达，不体现任何第三方品牌名称。

## 功能模块总览

| 模块 | MVP | 说明 |
| --- | --- | --- |
| 认证与账号 | 是 | 邮箱、手机号、微信小程序登录，JWT 与 refresh token rotation。 |
| 用户资料 | 是 | 昵称、头像、账号状态、密码修改。 |
| 设备会话 | 是 | 记录客户端类型、版本、设备 ID，用于多端登录和刷新令牌管理。 |
| 清单 | 是 | 自定义清单、默认清单、排序、软删除。 |
| 智能视图 | 是 | 我的一天、重要、计划内、已完成、全部任务等视图通过过滤条件生成。 |
| 任务 | 是 | 标题、备注、状态、重要性、截止日期、提醒时间、排序、软删除。 |
| 任务步骤 | 是 | 子任务/检查项，支持完成状态和排序。 |
| 标签 | 是 | 用户自定义标签，任务可绑定多个标签。 |
| 提醒 | 是 | 保存提醒时间和提醒状态；通知投递可后续扩展。 |
| 重复任务 | 预留 | MVP 保存 `repeat_rule`，后续实现重复实例生成。 |
| 搜索 | 是 | 按关键字搜索标题和备注。 |
| 多端同步 | 是 | bootstrap + changes 增量同步。 |
| 共享协作 | 后续 | 清单邀请、成员权限、协作编辑。 |
| 指派任务 | 后续 | 共享清单中将任务指派给成员。 |
| 附件 | 后续 | 文件上传、下载、对象存储。 |
| 推送通知 | 后续 | APP、小程序、邮件等提醒投递。 |
| 审计日志 | 是 | 记录认证、共享、重要数据修改等操作。 |

## 认证与账号

接口范围：

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/logout-all`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/wechat-mini-program/login`
- `POST /api/v1/auth/wechat-mini-program/bind-phone`

关键规则：

- 一个用户可绑定多个身份来源。
- 邮箱和手机号都必须唯一。
- 微信小程序身份通过 `provider` + `provider_subject` 唯一识别。
- Refresh token 按设备记录，支持单设备退出和全部设备退出。

## 清单与智能视图

清单接口：

- `GET /api/v1/lists`
- `POST /api/v1/lists`
- `GET /api/v1/lists/{listId}`
- `PATCH /api/v1/lists/{listId}`
- `DELETE /api/v1/lists/{listId}`
- `POST /api/v1/lists/reorder`

智能视图接口建议：

- `GET /api/v1/views/my-day/todos`
- `GET /api/v1/views/important/todos`
- `GET /api/v1/views/planned/todos`
- `GET /api/v1/views/completed/todos`
- `GET /api/v1/views/all/todos`

智能视图也可以由 `GET /api/v1/todos` 的过滤条件实现；单独 view API 用于前端更直观地对接。

## 任务

任务接口：

- `GET /api/v1/todos`
- `POST /api/v1/todos`
- `GET /api/v1/todos/{todoId}`
- `PATCH /api/v1/todos/{todoId}`
- `DELETE /api/v1/todos/{todoId}`
- `POST /api/v1/todos/{todoId}/complete`
- `POST /api/v1/todos/{todoId}/uncomplete`
- `POST /api/v1/todos/{todoId}/important`
- `DELETE /api/v1/todos/{todoId}/important`
- `POST /api/v1/todos/{todoId}/my-day`
- `DELETE /api/v1/todos/{todoId}/my-day`
- `POST /api/v1/todos/reorder`

过滤条件：

- 清单：`listId`
- 状态：`status`
- 重要：`important`
- 我的一天：`myDay`
- 截止日期：`dueFrom`、`dueTo`
- 提醒时间：`remindFrom`、`remindTo`
- 标签：`tagId`
- 关键字：`keyword`
- 分页：`page`、`size`

## 任务步骤

接口：

- `GET /api/v1/todos/{todoId}/steps`
- `POST /api/v1/todos/{todoId}/steps`
- `PATCH /api/v1/todos/{todoId}/steps/{stepId}`
- `DELETE /api/v1/todos/{todoId}/steps/{stepId}`
- `POST /api/v1/todos/{todoId}/steps/reorder`

## 标签

接口：

- `GET /api/v1/tags`
- `POST /api/v1/tags`
- `PATCH /api/v1/tags/{tagId}`
- `DELETE /api/v1/tags/{tagId}`
- `POST /api/v1/todos/{todoId}/tags/{tagId}`
- `DELETE /api/v1/todos/{todoId}/tags/{tagId}`

## 提醒与重复任务

提醒接口：

- `GET /api/v1/reminders`
- `POST /api/v1/todos/{todoId}/reminders`
- `PATCH /api/v1/todos/{todoId}/reminders/{reminderId}`
- `DELETE /api/v1/todos/{todoId}/reminders/{reminderId}`

重复任务规则：

- MVP 阶段只保存 `repeat_rule`。
- 后续使用标准化规则生成下一次任务或下一次提醒。
- 重复任务执行需要幂等，避免重复生成实例。

## 搜索

接口建议：

- `GET /api/v1/search/todos?keyword=`

搜索范围：

- 任务标题。
- 任务备注。
- 清单名称。
- 标签名称。

MVP 可先使用 PostgreSQL `ILIKE`，后续再扩展全文索引。

## 多端同步

接口：

- `GET /api/v1/sync/bootstrap`
- `GET /api/v1/sync/changes?sinceVersion=`

MVP 策略：

- 服务端为最终数据源。
- 客户端保存最后同步版本。
- 创建、修改、删除都写入 `sync_changes`。
- 如果客户端版本太旧，服务端可要求重新 bootstrap。

## 后续协作能力

共享清单接口建议：

- `POST /api/v1/lists/{listId}/shares`
- `GET /api/v1/lists/{listId}/members`
- `PATCH /api/v1/lists/{listId}/members/{userId}`
- `DELETE /api/v1/lists/{listId}/members/{userId}`
- `POST /api/v1/share-invitations/{token}/accept`

任务指派接口建议：

- `POST /api/v1/todos/{todoId}/assignee`
- `DELETE /api/v1/todos/{todoId}/assignee`
- `GET /api/v1/views/assigned-to-me/todos`

附件接口建议：

- `POST /api/v1/todos/{todoId}/attachments`
- `GET /api/v1/todos/{todoId}/attachments`
- `DELETE /api/v1/todos/{todoId}/attachments/{attachmentId}`

这些能力不进入 MVP，待核心任务闭环稳定后实现。
