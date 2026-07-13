# iTodo 前端 AI 对接指南

本文档面向负责开发 iTodo 前端的 AI/工程师，目标是让前端在不了解后端实现细节的情况下，快速理解产品、数据模型、鉴权方式和 API 对接方式。

更完整、机器可读的接口定义见：

- `docs/openapi/openapi.yaml`

## 1. 产品定位

**iTodo** 是一个跨端 TodoList / 任务管理 API，面向小程序、Web、移动端等客户端。

产品核心能力类似 Microsoft To Do / Apple Reminders：

- 用户注册、登录、刷新 token、退出登录
- 管理任务清单 List
- 管理任务 Todo
- 管理子任务 Step
- 智能视图：我的一天、重要、计划内、已完成、全部
- 标签 Tag
- 提醒 Reminder
- 搜索 Search
- 客户端同步 Sync

前端应该围绕“用户登录后管理自己的任务数据”来设计。

## 2. 推荐前端信息架构

建议前端至少包含以下页面/模块：

1. 登录 / 注册页
2. 主应用布局
   - 左侧或底部导航：
     - 我的一天
     - 重要
     - 计划内
     - 已完成
     - 全部
     - 自定义清单列表
     - 标签管理入口
   - 中间任务列表
   - 右侧或弹窗任务详情
3. 清单管理
   - 新建清单
   - 修改清单
   - 删除清单
   - 清单排序
4. 任务管理
   - 新建任务
   - 编辑标题、备注、截止日期、提醒时间、重复规则、重要、我的一天
   - 完成 / 取消完成
   - 删除任务
   - 排序
5. 子任务管理
6. 标签管理与任务绑定
7. 提醒管理
8. 搜索
9. 同步初始化与增量同步

## 3. 基础 URL

生产环境建议：

```text
https://api.itodo.site
```

本地开发默认：

```text
http://localhost:8080
```

所有业务 API 都在：

```text
/api/v1
```

例如：

```text
GET /api/v1/todos
POST /api/v1/auth/login
```

## 4. 响应格式

后端统一返回 `ApiResponse<T>` 包装。

成功响应通常类似：

```json
{
  "success": true,
  "data": {},
  "traceId": "..."
}
```

失败响应通常类似：

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "details": [
      {
        "field": "title",
        "message": "must not be blank"
      }
    ]
  },
  "traceId": "..."
}
```

前端应统一处理：

- `success === true`：使用 `data`
- `success === false`：展示 `error.message`
- 如果存在 `error.details`，可展示字段级校验错误
- `traceId` 可在报错反馈/日志中记录，便于后端排查

## 5. 分页格式

分页接口的 `data` 通常是：

```json
{
  "items": [],
  "total": 100,
  "page": 1,
  "size": 20
}
```

常见查询参数：

```text
page=1
size=20
```

`size` 最大通常为 `100`。

## 6. 鉴权方式

后端使用 JWT Bearer Token。

登录成功后会返回：

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "accessTokenExpiresAt": "2026-...",
  "refreshTokenExpiresAt": "2026-..."
}
```

除公开认证接口外，其余 `/api/v1/**` 接口都需要 Header：

```http
Authorization: Bearer <accessToken>
```

建议前端同时传客户端信息 Header：

```http
X-Client-Type: WEB
X-Client-Version: 1.0.0
X-Device-Id: <稳定设备ID>
X-Trace-Id: <前端生成的请求追踪ID，可选>
```

### 6.1 Token 使用建议

- `accessToken`：短期有效，用于 API 请求
- `refreshToken`：长期有效，用于换取新的 access token
- access token 过期或接口返回 401 时：
  1. 调用 `/api/v1/auth/refresh`
  2. 成功后重放原请求
  3. refresh 失败则跳转登录页

### 6.2 Token 存储建议

Web 前端建议优先考虑：

- access token 存内存
- refresh token 根据产品安全要求选择 HttpOnly Cookie 或安全存储策略

如果 MVP 阶段必须存在 localStorage，需要注意 XSS 风险。

## 7. 认证 API

### 7.1 注册

```http
POST /api/v1/auth/register
```

请求体：

```json
{
  "email": "user@example.com",
  "phone": null,
  "password": "password123",
  "displayName": "用户昵称"
}
```

说明：

- `email` 和 `phone` 至少提供一个
- `password` 最少 8 位

### 7.2 登录

```http
POST /api/v1/auth/login
```

请求体：

```json
{
  "account": "user@example.com",
  "password": "password123",
  "clientType": "WEB",
  "deviceId": "web-device-id"
}
```

### 7.3 刷新 Token

```http
POST /api/v1/auth/refresh
```

请求体：

```json
{
  "refreshToken": "...",
  "clientType": "WEB",
  "deviceId": "web-device-id"
}
```

### 7.4 退出登录

```http
POST /api/v1/auth/logout
```

### 7.5 退出所有设备

```http
POST /api/v1/auth/logout-all
```

### 7.6 获取当前认证主体

```http
GET /api/v1/auth/me
```

## 8. 用户 API

### 8.1 获取个人资料

```http
GET /api/v1/users/me
```

### 8.2 更新个人资料

```http
PATCH /api/v1/users/me
```

### 8.3 修改密码

```http
PATCH /api/v1/users/me/password
```

### 8.4 删除账号

```http
DELETE /api/v1/users/me
```

## 9. 清单 List API

清单是任务的分组。

### 9.1 查询清单

```http
GET /api/v1/lists
```

响应 `data` 是数组：

```json
[
  {
    "id": "uuid",
    "name": "工作",
    "color": "#1677ff",
    "icon": "briefcase",
    "sortOrder": 1000,
    "isSystem": false,
    "isShared": false,
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

### 9.2 创建清单

```http
POST /api/v1/lists
```

```json
{
  "name": "工作",
  "color": "#1677ff",
  "icon": "briefcase"
}
```

### 9.3 更新清单

```http
PATCH /api/v1/lists/{listId}
```

```json
{
  "name": "新名称",
  "color": "#52c41a",
  "icon": "home"
}
```

### 9.4 删除清单

```http
DELETE /api/v1/lists/{listId}
```

注意：系统清单不能删除。

### 9.5 清单排序

```http
POST /api/v1/lists/reorder
```

```json
{
  "items": [
    { "id": "uuid-1", "sortOrder": 1000 },
    { "id": "uuid-2", "sortOrder": 2000 }
  ]
}
```

## 10. 任务 Todo API

Todo 是核心任务对象。

### 10.1 Todo 数据结构

典型响应：

```json
{
  "id": "uuid",
  "listId": "uuid",
  "title": "完成项目部署",
  "note": "补充说明",
  "status": "ACTIVE",
  "importance": "IMPORTANT",
  "dueDate": "2026-06-30",
  "remindAt": "2026-06-30T09:00:00Z",
  "repeatRule": null,
  "completedAt": null,
  "sortOrder": 1000,
  "myDay": true,
  "version": 3,
  "createdAt": "...",
  "updatedAt": "..."
}
```

字段说明：

- `status`：`ACTIVE` / `COMPLETED`
- `importance`：`NORMAL` / `IMPORTANT`
- `dueDate`：日期，不含时间
- `remindAt`：提醒时间快照，用于计划视图
- `myDay`：是否加入“我的一天”
- `version`：任务版本，任务被修改时递增

### 10.2 查询任务

```http
GET /api/v1/todos
```

支持查询参数：

```text
listId=uuid
status=ACTIVE|COMPLETED
important=true|false
myDay=true|false
dueFrom=2026-06-01
dueTo=2026-06-30
remindFrom=2026-06-01T00:00:00Z
remindTo=2026-06-30T23:59:59Z
keyword=关键词
tagId=uuid
page=1
size=20
```

示例：

```http
GET /api/v1/todos?listId=<listId>&status=ACTIVE&page=1&size=20
```

### 10.3 创建任务

```http
POST /api/v1/todos
```

```json
{
  "listId": "uuid",
  "title": "买牛奶",
  "note": "顺便买面包",
  "dueDate": "2026-06-30",
  "remindAt": "2026-06-30T09:00:00Z",
  "repeatRule": null,
  "myDay": true,
  "important": false,
  "sortOrder": 1000
}
```

说明：

- `title` 必填
- `listId` 可为空，后端会使用默认清单

### 10.4 获取单个任务

```http
GET /api/v1/todos/{todoId}
```

### 10.5 更新任务

```http
PATCH /api/v1/todos/{todoId}
```

```json
{
  "title": "新的标题",
  "note": "新的备注",
  "dueDate": "2026-07-01",
  "remindAt": "2026-07-01T09:00:00Z",
  "myDay": false,
  "important": true
}
```

清空可空字段：

```json
{
  "clearNote": true,
  "clearDueDate": true,
  "clearRemindAt": true,
  "clearRepeatRule": true
}
```

### 10.6 删除任务

```http
DELETE /api/v1/todos/{todoId}
```

### 10.7 完成 / 取消完成

```http
POST /api/v1/todos/{todoId}/complete
POST /api/v1/todos/{todoId}/uncomplete
```

### 10.8 重要标记

```http
POST /api/v1/todos/{todoId}/important
DELETE /api/v1/todos/{todoId}/important
```

### 10.9 我的一天

```http
POST /api/v1/todos/{todoId}/my-day
DELETE /api/v1/todos/{todoId}/my-day
```

### 10.10 任务排序

```http
POST /api/v1/todos/reorder
```

```json
{
  "listId": "uuid",
  "items": [
    { "id": "todo-uuid-1", "sortOrder": 1000 },
    { "id": "todo-uuid-2", "sortOrder": 2000 }
  ]
}
```

## 11. 智能视图 API

这些接口可以直接作为前端导航入口。

```http
GET /api/v1/views/my-day/todos
GET /api/v1/views/important/todos
GET /api/v1/views/planned/todos
GET /api/v1/views/completed/todos
GET /api/v1/views/all/todos
```

都支持：

```text
page=1
size=20
```

建议前端导航：

- 我的一天 → `/views/my-day/todos`
- 重要 → `/views/important/todos`
- 计划内 → `/views/planned/todos`
- 已完成 → `/views/completed/todos`
- 全部 → `/views/all/todos`

## 12. 子任务 Step API

子任务属于某个 Todo。

### 12.1 查询子任务

```http
GET /api/v1/todos/{todoId}/steps
```

### 12.2 创建子任务

```http
POST /api/v1/todos/{todoId}/steps
```

```json
{
  "title": "子任务标题",
  "isCompleted": false,
  "sortOrder": 1000
}
```

### 12.3 更新子任务

```http
PATCH /api/v1/todos/{todoId}/steps/{stepId}
```

```json
{
  "title": "新的子任务标题",
  "isCompleted": true,
  "sortOrder": 2000
}
```

### 12.4 删除子任务

```http
DELETE /api/v1/todos/{todoId}/steps/{stepId}
```

### 12.5 子任务排序

```http
POST /api/v1/todos/{todoId}/steps/reorder
```

```json
{
  "items": [
    { "id": "step-uuid-1", "sortOrder": 1000 },
    { "id": "step-uuid-2", "sortOrder": 2000 }
  ]
}
```

## 13. 标签 Tag API

标签属于当前用户，可绑定到任务。

### 13.1 查询标签

```http
GET /api/v1/tags
```

### 13.2 创建标签

```http
POST /api/v1/tags
```

```json
{
  "name": "工作",
  "color": "#1677ff"
}
```

### 13.3 更新标签

```http
PATCH /api/v1/tags/{tagId}
```

```json
{
  "name": "家庭",
  "color": "#52c41a"
}
```

### 13.4 删除标签

```http
DELETE /api/v1/tags/{tagId}
```

### 13.5 绑定标签到任务

```http
POST /api/v1/todos/{todoId}/tags/{tagId}
```

### 13.6 从任务移除标签

```http
DELETE /api/v1/todos/{todoId}/tags/{tagId}
```

### 13.7 按标签查询任务

```http
GET /api/v1/todos?tagId={tagId}
```

## 14. 提醒 Reminder API

Reminder 是独立提醒记录，属于某个 Todo。

### 14.1 查询提醒

```http
GET /api/v1/reminders
```

支持参数：

```text
status=PENDING|SENT|CANCELLED
remindFrom=2026-06-01T00:00:00Z
remindTo=2026-06-30T23:59:59Z
page=1
size=20
```

### 14.2 创建任务提醒

```http
POST /api/v1/todos/{todoId}/reminders
```

```json
{
  "remindAt": "2026-06-30T09:00:00Z",
  "channel": "IN_APP"
}
```

当前 MVP 只支持：

```text
channel=IN_APP
```

### 14.3 更新提醒

```http
PATCH /api/v1/todos/{todoId}/reminders/{reminderId}
```

```json
{
  "remindAt": "2026-07-01T09:00:00Z",
  "channel": "IN_APP",
  "status": "PENDING"
}
```

### 14.4 删除提醒

```http
DELETE /api/v1/todos/{todoId}/reminders/{reminderId}
```

说明：

- 创建、更新、删除提醒后，后端会同步更新 Todo 的 `remindAt` 快照。
- 如果一个 Todo 有多个 pending reminders，`todo.remindAt` 会是最早的提醒时间。

## 15. 搜索 API

```http
GET /api/v1/search/todos?keyword=部署&page=1&size=20
```

搜索范围：

- Todo 标题
- Todo 备注

返回格式同 Todo 分页。

## 16. 同步 Sync API

同步 API 用于多端客户端初始化和增量同步。

### 16.1 全量初始化

```http
GET /api/v1/sync/bootstrap
```

返回当前用户完整数据快照：

```json
{
  "currentVersion": 123,
  "lists": [],
  "todos": [],
  "steps": [],
  "tags": [],
  "todoTags": [],
  "reminders": []
}
```

建议前端：

- 首次登录后调用
- 本地缓存丢失后调用
- 增量同步无法处理时重新调用

### 16.2 增量变更

```http
GET /api/v1/sync/changes?sinceVersion=123&limit=100
```

返回：

```json
{
  "sinceVersion": 123,
  "currentVersion": 130,
  "changes": [
    {
      "version": 124,
      "resourceType": "TODO",
      "resourceId": "uuid",
      "operation": "UPDATE",
      "changedAt": "..."
    }
  ]
}
```

`resourceType` 可能值：

```text
LIST
TODO
STEP
TAG
TODO_TAG
REMINDER
```

`operation` 可能值：

```text
CREATE
UPDATE
DELETE
```

注意：

- 当前增量接口返回的是 metadata，不返回资源完整 payload。
- 前端可以根据变更类型重新拉取对应资源，或在复杂场景下重新 bootstrap。
- `currentVersion` 应保存在客户端本地，作为下次 `sinceVersion`。

## 17. 推荐前端启动流程

登录后：

1. 保存 token
2. 调用：

```http
GET /api/v1/sync/bootstrap
```

3. 将 `lists/todos/steps/tags/todoTags/reminders/currentVersion` 写入前端状态或本地缓存
4. 渲染默认视图，例如“我的一天”或“全部”
5. 后续用户操作直接调用对应 CRUD API
6. 每次写操作成功后，可以：
   - 直接用接口返回值更新本地状态
   - 或调用 sync changes 拉取最新变更

## 18. 推荐前端状态模型

建议前端按实体归一化存储：

```ts
type AppState = {
  currentVersion: number
  listsById: Record<string, TodoList>
  todosById: Record<string, Todo>
  stepsById: Record<string, TodoStep>
  tagsById: Record<string, Tag>
  todoTags: Array<{ todoId: string; tagId: string }>
  remindersById: Record<string, Reminder>
}
```

列表页面通过 selector 派生：

- 当前 list 的 todos
- myDay todos
- important todos
- planned todos
- completed todos
- tag filtered todos

## 19. 常见错误码

后端常见 `error.code`：

```text
VALIDATION_FAILED    请求参数错误
UNAUTHORIZED         未登录或 token 无效
FORBIDDEN            无权限
RESOURCE_NOT_FOUND   资源不存在或不属于当前用户
RESOURCE_CONFLICT    资源冲突，例如标签重名
RATE_LIMITED         请求过于频繁
INTERNAL_ERROR       服务端未知错误
```

前端建议：

- 401：尝试 refresh token，失败后跳转登录
- 403：提示无权限
- 404：提示资源不存在，并从本地状态移除或刷新
- 409：提示冲突，例如标签重名
- 429：提示稍后重试
- 500：提示系统异常，并记录 traceId

## 20. 前端开发注意事项

### 20.1 日期时间

- `dueDate` 是 `YYYY-MM-DD`
- `remindAt` / `createdAt` / `updatedAt` 是 ISO date-time
- 前端展示时应转换为用户本地时区

### 20.2 幂等行为

以下操作可以按幂等处理：

- 给任务绑定已存在标签
- 移除任务上不存在的标签

前端可以乐观更新，但失败时要回滚。

### 20.3 排序

后端使用 `sortOrder` 排序。

建议前端初始间隔使用：

```text
1000, 2000, 3000...
```

拖拽排序后调用 reorder API 批量提交。

### 20.4 OpenAPI 优先级

本文档用于理解产品和对接方式。字段细节和接口 schema 以 `docs/openapi/openapi.yaml` 为准。

## 21. 最小可用前端 MVP

如果先做 MVP，建议只做这些：

1. 登录 / 注册
2. Token refresh
3. 清单列表
4. Todo 列表
5. 创建 / 更新 / 删除 Todo
6. 完成 / 取消完成
7. 重要 / 我的天
8. 子任务
9. 标签
10. 提醒
11. 搜索
12. sync bootstrap

可以暂缓：

- 离线编辑
- 复杂冲突解决
- 多端实时推送
- 复杂重复规则 UI
- 微信小程序专属能力

## 22. 快速 API 调用示例

### 登录

```bash
curl -X POST https://api.itodo.site/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "account": "user@example.com",
    "password": "password123",
    "clientType": "WEB",
    "deviceId": "web-demo"
  }'
```

### 查询任务

```bash
curl https://api.itodo.site/api/v1/todos \
  -H 'Authorization: Bearer <accessToken>'
```

### 创建任务

```bash
curl -X POST https://api.itodo.site/api/v1/todos \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "完成前端 MVP",
    "myDay": true,
    "important": true
  }'
```

### 全量同步

```bash
curl https://api.itodo.site/api/v1/sync/bootstrap \
  -H 'Authorization: Bearer <accessToken>'
```
