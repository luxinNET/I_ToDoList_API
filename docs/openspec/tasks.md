# 任务 OpenSpec

## 任务生命周期

- 新任务默认状态为 `ACTIVE`。
- 完成任务时设置 `status=COMPLETED`，并写入 `completed_at`。
- 取消完成时恢复 `status=ACTIVE`，并清空 `completed_at`。
- 删除任务使用软删除，写入 `deleted_at`。
- 每次创建、更新、删除任务都应更新 `updated_at`，并递增版本号或写入同步变更记录。

## 任务视图

- 我的一天：`my_day=true` 的任务。
- 重要：`importance=IMPORTANT` 的任务。
- 计划内：存在 `due_date` 或 `remind_at` 的任务。
- 已完成：`status=COMPLETED` 的任务。

## 字段规则

- `title` 必填，长度建议不超过 255。
- `note` 可选，用于保存较长备注。
- `due_date` 表示截止日期，不包含具体时间。
- `remind_at` 表示提醒时间，使用带时区时间。
- `repeat_rule` 预留重复任务规则，MVP 可先只保存不执行。
- `sort_order` 用于同一清单内排序。

## 步骤/子任务

- 一个任务可以包含多个步骤。
- 步骤有独立完成状态。
- 步骤删除使用软删除。
- 步骤排序通过 `sort_order` 控制。

## 权限

每个任务查询必须包含当前认证用户 `owner_id`，或通过共享清单权限校验。禁止只按任务 ID 查询用户数据。

## 过滤查询

`GET /api/v1/todos` 支持以下过滤条件：

- `listId`
- `status`
- `important`
- `myDay`
- `dueFrom`
- `dueTo`
- `keyword`
- `page`
- `size`
