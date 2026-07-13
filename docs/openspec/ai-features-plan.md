# iTodo AI 功能扩展方案

> 目标：在现有 iTodo（Spring Boot 4 + PostgreSQL + Redis）基础上，最低成本实现 6 个 AI/规划相关功能。

---

## 一、项目现状（已具备）

| 能力 | 现状 |
|---|---|
| 任务 CRUD | 完整。`todos` 表含 due_date / remind_at / importance / my_day / completed_at |
| 子任务 | `todo_steps` 扁平 checklist，无嵌套层级 |
| 提醒 | `reminders` 表 + `ReminderService`，但 **无调度器推送**（PENDING 永远不会变 SENT）|
| 标签 | 多对多 `todo_tags`，完整 |
| 同步 | `SyncChangeService` 全链路就绪，新增资源只需加 `SyncResourceType` |
| 智能视图 | `SmartViewController` 仅 5 个固定过滤（my-day/important/planned/completed/all），无 AI |
| 搜索 | 纯 `ILIKE`，无聚合、无日历、无报表 |
| HTTP 客户端 | `RestClient.Builder` Bean 已配置（微信登录在用）|
| 缓存/限流/锁 | Redisson + `RateLimiterService` 就绪 |
| 调度/异步 | **未启用** `@EnableScheduling` / `@EnableAsync` |
| LLM 集成 | **完全没有**，pom 无任何 AI 依赖 |

**关键缺口**：无 LLM 客户端、无定时任务、无日历聚合接口、无报表统计、无目标/计划概念、todos 无 parent_id。

---

## 二、总体策略：最低成本最优实现

### 原则

1. **不引入重量级 AI SDK**（不装 Spring AI / LangChain4j）。用现有 `RestClient` 封装一个 150 行的 `LlmClient`，对接 OpenAI 兼容协议（DeepSeek / 通义千问 / Kimi / 智谱均兼容），零新依赖。
2. **最大化复用现有基础设施**：RestClient、Redisson（缓存+限流）、SyncChangeService、MyBatis-Plus、Flyway、CurrentUser 鉴权链路全部零改动复用。
3. **AI 结果一律"预览→确认"**：所有 AI 生成（建任务、拆解、计划）先返回结构化预览，用户确认后才落库，避免脏数据。
4. **规则优先 + AI 增强**：个性化建议先用规则筛选（逾期/今日到期/重要未完成），再 AI 排序生成理由，降低 LLM 调用成本。
5. **LLM 结果缓存**：相同输入用 Redis 缓存 5 分钟，时间解析类高频请求缓存 1 小时。
6. **配置驱动**：`app.llm.*` 集中管理，支持随时切换模型/开关降级。

### 实施分期

| 阶段 | 功能 | 成本 | 依赖 |
|---|---|---|---|
| **P0** | 日历视图 + 自然语言建任务 | 低 | 仅日历无需 AI；建任务需 LlmClient |
| **P1** | 自动任务拆解 + 智能日报/周报 | 中 | 复用 P0 的 LlmClient；需 @EnableScheduling |
| **P2** | 个性化任务建议 + 计划模块 | 中高 | 需新建 goals 表 + 调度预热 |

> 公共基础设施（LlmClient + PromptManager + @EnableScheduling + app.llm 配置）在 P0 阶段一次性建设，后续功能复用。

---

## 三、公共基础设施（P0 一次性建设）

### 3.1 LLM 选型与配置

#### 选型对比

| 模型 | 中文能力 | 价格（¥/百万token） | 速度 | JSON稳定性 | 推荐度 |
|---|---|---|---|---|---|
| **DeepSeek-V3** | ⭐⭐⭐⭐⭐ | 1/2 | 快 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ 主推 |
| **通义千问 Qwen3-32B** | ⭐⭐⭐⭐⭐ | 2/2 | 中 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ 备用 |
| **智谱 GLM-4-Flash** | ⭐⭐⭐⭐ | 0.1/0.1 | 极快 | ⭐⭐⭐ | ⭐⭐⭐⭐ 轻量任务 |
| **Kimi (Moonshot)** | ⭐⭐⭐⭐ | 12/12 | 慢 | ⭐⭐⭐ | ⭐⭐ |
| **腾讯混元** | ⭐⭐⭐⭐ | 4/4 | 中 | ⭐⭐⭐ | ⭐⭐⭐ 腾讯云部署时 |

**推荐方案**：DeepSeek-V3 为主（所有功能），智谱 GLM-4-Flash 做轻量任务路由（意图分类、简单过滤），后期可选。

#### 多模型配置（支持路由，不改代码）

```yaml
app:
  llm:
    enabled: true
    cache-ttl-seconds: 300
    daily-quota-per-user: 100
    providers:
      - name: deepseek
        base-url: https://api.deepseek.com
        api-key: ${LLM_API_KEY_DEEPSEEK:}
        models:
          - name: deepseek-chat
            use-for: [PARSE_TODO, DECOMPOSE, PLAN_GENERATE, REPORT]
            temperature: 0.3
            max-tokens: 1024
            timeout-seconds: 30
      - name: zhipu
        base-url: https://open.bigmodel.cn/api/paas/v4
        api-key: ${LLM_API_KEY_ZHIPU:}
        models:
          - name: glm-4-flash
            use-for: [CLASSIFY_INTENT, SUGGESTION]
            temperature: 0.1
            max-tokens: 512
            timeout-seconds: 15
```

`LlmClient` 按 `TaskType` 枚举自动路由到对应模型，一行配置切换。

#### LLM 客户端实现

**不引入新依赖**，基于现有 `RestClient.Builder` 封装：

```
src/main/java/com/example/itodo/ai/
├── LlmClient.java                 # 接口
├── OpenAiCompatibleLlmClient.java # 实现（RestClient 调 /v1/chat/completions）
├── LlmProperties.java             # @ConfigurationProperties("app.llm")
├── LlmConfig.java                 # Bean 注册 + 多模型路由
├── dto/ChatRequest.java
├── dto/ChatResponse.java
└── prompt/PromptManager.java      # 模板加载 + few-shot 管理
```

**LlmClient 接口**：

```java
public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);
    <T> T chatForObject(String systemPrompt, String userPrompt, Class<T> type);
    AiParseResult parseTodo(String userInput, String timezone, List<String> userTags);
}
```

**LlmClient 接口**：

```java
public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);           // 纯文本
    <T> T chatForObject(String systemPrompt, String userPrompt,    // 结构化输出
                        Class<T> type);                            // JSON → 对象
}
```

**降级策略**：`app.llm.enabled=false` 时，AI 接口返回 `503 AI_SERVICE_DISABLED`；LLM 调用超时/失败时，自然语言建任务降级为"整句作为 title，不解析时间"。

### 3.2 结构化输出方案

#### 使用 `response_format` 强制 JSON 输出

DeepSeek / 通义千问均支持 OpenAI 兼容协议的 `response_format`：

```java
// 请求体
Map<String, Object> body = new HashMap<>();
body.put("model", "deepseek-chat");
body.put("response_format", Map.of("type", "json_object"));  // 强制 JSON 输出
body.put("messages", messages);
```

配合 System Prompt 注入 JSON Schema（见 3.4），解析失败率 < 1%。

#### 后端解析容错

LLM 输出可能包含 ```json 标记或解释性文字，需要清洗：

```java
String cleanJson = rawOutput
    .replaceAll("```json", "")
    .replaceAll("```", "")
    .trim();
// 再用 Jackson 解析
```

### 3.3 小模型 + 大模型路由方案

**MVP 阶段：纯大模型（DeepSeek-V3）**，架构简单，零调试成本。

**用户量 > 1000 后：可选引入小模型路由**，降本 90%：

```
用户输入
  → 小模型（GLM-4-Flash，¥0.1/百万token）做意图分类
    → valid=false → 拒绝（不消耗主模型 quota）
    → valid=true, 简单任务（时间解析明确） → 小模型直接解析
    → valid=true, 复杂任务（需拆解/计划/歧义高） → 调大模型（DeepSeek-V3）
```

**不推荐 MVP 做小模型路由的原因**：
1. 用户量少时成本差异可忽略（1000 用户 × 日均 10 次 × ¥0.01 = ¥100/月）
2. 小模型 JSON 输出稳定性差，增加调试成本
3. 路由逻辑本身有错误率（可能把复杂任务误判给小模型）

### 3.4 输入有效性判定（多层防御）

#### 第一层：输入基础校验（零成本，毫秒级）

```java
@Component
public class AiInputValidator {
    private static final int MIN_LENGTH = 5;
    private static final int MAX_LENGTH = 500;
    private static final Pattern INJECTION_PATTERN = 
        Pattern.compile("忽略以上指令|你是一个|system prompt", Pattern.CASE_INSENSITIVE);
    
    public ValidationResult validate(String text) {
        if (text.length() < MIN_LENGTH || text.length() > MAX_LENGTH) {
            return invalid("输入长度需在 5-500 字符之间");
        }
        if (INJECTION_PATTERN.matcher(text).find()) {
            return invalid("检测到无效输入");
        }
        if (isGibberish(text)) {  // 字符熵值检测
            return invalid("请输入有效内容");
        }
        return valid();
    }
}
```

#### 第二层：LLM 意图分类（轻量，用小模型）

在调大模型解析之前，先问小模型（GLM-4-Flash）：

```
System: 判断用户输入是否与"任务管理、待办事项、日程规划、效率提升"相关。
        只输出 JSON：{"valid": true/false, "category": "todo|plan|report|invalid", "reason": "..."}

User: [用户输入]
```

无效内容直接返回 `400 INVALID_INPUT`，不消耗主模型 quota。

#### 第三层：Prompt Injection 防御

对用户输入做转义，明确告诉模型这是用户输入而非指令：

```java
String safeInput = "<user_input>" + escapeXml(userText) + "</user_input>";
```

#### 第四层：LLM 输出校验（后端兜底）

LLM 返回的 JSON 解析后，后端做合理性校验：
- `dueDate` 不能超过当前时间 + 5 年
- `remindAt` 不能在过去
- `title` 不能为空或纯特殊字符
- 校验失败 → 降级处理（整句作 title，不解析时间）

### 3.5 启用调度

`ITodoApplication` 加 `@EnableScheduling`，新增 `AiScheduler` 用于日报/建议预热（P1/P2 用）。无需 Quartz/Redisson 调度，Spring 原生 `@Scheduled` 足够单机场景。

### 3.6 限流

复用现有 `RateLimiterService`，给 AI 接口加 `app.llm.daily-quota-per-user` 限制，防止滥用。

---

## 四、功能详细设计

### 功能 1：日历视图（P0，纯后端，无 AI）

**需求**：日历视图模式查看任务；点击日期 → 下方列表显示该日任务。

**方案**：`due_date` 索引（`idx_todos_owner_due_date`）已就绪，新增 2 个聚合接口，零 AI 成本。

**接口**：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/calendar?from=2026-07-01&to=2026-07-31` | 月概览：返回每日任务统计 |
| GET | `/api/v1/calendar/{date}/todos` | 某日任务列表（复用 `todoService.queryTodos` 的 dueFrom=dueTo=date） |

**响应（月概览）**：

```json
{
  "items": [
    { "date": "2026-07-01", "total": 5, "completed": 2, "active": 3, "overdue": 0 },
    { "date": "2026-07-02", "total": 0, "completed": 0, "active": 0, "overdue": 0 }
  ]
}
```

**实现**：新建 `CalendarController` + `CalendarService`，用 MyBatis-Plus 按 `due_date` 分组 `GROUP BY` 聚合。某日任务直接委托 `todoService.queryTodos(dueFrom=date, dueTo=date)`，零重复逻辑。

**成本**：约 2 个文件，半天。

---

### 功能 2：自然语言创建任务（P0）

**需求**："下周三下午提醒我准备季度报告" → 自动识别时间 + 任务。

**方案**：LLM 解析为结构化 `CreateTodoRequest`，**预览→确认**两步。

**接口**：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/ai/todos/parse` | 解析预览（不落库），返回结构化结果 |
| POST | `/api/v1/ai/todos` | 确认创建（接收预览结果，落库 + 触发同步） |

**请求**：

```json
{ "text": "下周三下午提醒我准备季度报告", "timezone": "Asia/Shanghai" }
```

**解析预览响应**：

```json
{
  "previewId": "cache-key-from-redis",
  "todo": {
    "title": "准备季度报告",
    "note": null,
    "dueDate": "2026-07-08",
    "remindAt": "2026-07-08T14:00:00+08:00",
    "importance": "NORMAL",
    "tags": ["工作"],
    "listId": null
  },
  "explanation": "识别到：任务=准备季度报告，时间=下周三(7月8日)下午14:00"
}
```

**Prompt 设计要点**：
- System prompt 注入当前日期、时区、用户已有标签列表（提升 tag 匹配率）
- 输出强制 JSON Schema（title/note/dueDate/remindAt/importance/tags）
- few-shot 示例覆盖"明天/下周一/月底/每天/三天后"等中文时间表达
- LLM 只负责语义理解，**时间计算由后端用 Hutool `DateUtil` 兜底校验**（LLM 算错日期时以后端为准）

**缓存**：相同 `(text, timezone, currentDate)` 缓存 1 小时（Redis），previewId 直接用缓存 key。

**降级**：LLM 不可用时，整句作为 title，不解析时间，返回 `parseConfidence: LOW`。

**成本**：1 个 Controller + 1 个 Service + Prompt 模板，约 1.5 天。

---

### 功能 3：自动任务拆解（P1）

**需求**：输入"组织团队 offsite" → AI 生成子任务清单。

**方案**：LLM 生成 step 列表，**预览→确认**后批量写入 `todo_steps`。

**接口**：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/ai/todos/{todoId}/decompose` | AI 生成子任务预览 |
| POST | `/api/v1/ai/todos/{todoId}/decompose/confirm` | 确认后批量创建 steps |

**请求**（decompose）：

```json
{ "hint": "5人团队，预算2万，下月底前" }
```

**预览响应**：

```json
{
  "previewId": "xxx",
  "steps": [
    { "title": "确定 offsite 主题和目标", "sortOrder": 0 },
    { "title": "调研并比选 3 个场地", "sortOrder": 1 },
    { "title": "制定预算明细表", "sortOrder": 2 },
    { "title": "收集团队成员时间偏好", "sortOrder": 3 },
    { "title": "预定场地并发送邀请", "sortOrder": 4 }
  ]
}
```

**实现**：
- Prompt 注入 todo 的 title + note + hint，要求输出 JSON 数组
- 确认时循环调用现有 `todoStepService.createStep()`，每次自动 `recordChange` 接入同步链路
- 已有 steps 的 todo 拆解时提示"将追加"

**成本**：约 1 天。

---

### 功能 4：智能日报/周报（P1）

**需求**：基于任务完成情况，LLM 生成工作总结。

**方案**：后端聚合统计 + LLM 润色总结，结果存 `ai_reports` 表，支持定时生成 + 手动触发。

**接口**：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/ai/reports/daily?date=2026-07-01` | 获取日报（无则实时生成） |
| GET | `/api/v1/ai/reports/weekly?weekStart=2026-06-29` | 获取周报 |
| POST | `/api/v1/ai/reports/generate` | 手动触发（指定 type + date） |

**新增表 `ai_reports`**：

```sql
CREATE TABLE ai_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL,           -- DAILY / WEEKLY
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    stats_json JSONB NOT NULL,           -- 统计数据
    content TEXT NOT NULL,               -- LLM 生成的总结文本
    model VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_ai_reports_user_type_period UNIQUE (user_id, type, period_start)
);
```

**聚合统计（`ReportAggregator`，纯 SQL）**：

```json
{
  "totalCompleted": 12,
  "totalCreated": 15,
  "overdueCount": 2,
  "byList": [{ "listName": "工作", "completed": 8, "active": 5 }],
  "byTag": [{ "tag": "紧急", "completed": 3 }],
  "completionRate": 0.8,
  "streakDays": 5
}
```

**Prompt**：将 stats_json 注入 system prompt，要求 LLM 输出 3 段：① 今日完成概览 ② 亮点与不足 ③ 明日建议。控制 200 字内。

**定时生成**（`AiScheduler`）：

```java
@Scheduled(cron = "0 0 23 * * *")        // 每天 23:00 生成当日日报
@Scheduled(cron = "0 0 9 * * MON")       // 每周一 09:00 生成上周周报
```

用 Redisson 分布式锁防止多实例重复生成。

**成本**：聚合查询 + 表 + 定时，约 2 天。

---

### 功能 5：个性化任务建议（P2）

**需求**：分析用户历史，建议"现在最适合做的任务"。

**方案**：**规则筛选 + AI 排序**，降低成本。纯 AI 推荐成本高且不稳定。

**规则筛选层**（`SuggestionRuleEngine`，纯 Java/SQL）：

| 规则 | 来源 | 权重 |
|---|---|---|
| 逾期未完成 | `due_date < today AND status=ACTIVE` | 高 |
| 今日到期 | `due_date = today AND status=ACTIVE` | 高 |
| 重要未完成 | `importance=IMPORTANT AND status=ACTIVE` | 中 |
| 长期搁置 | `updated_at < now()-14d AND status=ACTIVE` | 中 |
| 有提醒即将触发 | `remind_at` 在未来 2 小时内 | 中 |

筛选 Top 20 候选 → 注入 LLM 重新排序 + 生成推荐理由。

**接口**：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/ai/suggestions?limit=5` | 获取建议 |

**响应**：

```json
{
  "items": [
    {
      "todoId": "...",
      "title": "准备季度报告",
      "reason": "今天到期且标记为重要，建议优先处理",
      "priority": "HIGH",
      "estimatedMinutes": null
    }
  ]
}
```

**新增表 `ai_suggestions`**（缓存预热结果，避免每次实时调 LLM）：

```sql
CREATE TABLE ai_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payload_json JSONB NOT NULL,     -- 建议列表
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_suggestions_user_expires ON ai_suggestions(user_id, expires_at);
```

**定时预热**：每天 08:00 + 13:00 生成，有效期 6 小时。实时请求优先读缓存，过期则实时生成。

**降级**：LLM 不可用时，直接返回规则筛选结果（无 reason 字段）。

**成本**：规则引擎 + 表 + 调度，约 2 天。

---

### 功能 6：计划模块（P2）

**需求**：长期目标制定与跟踪；"告诉我你的目标，我会帮你制定详细的执行计划，并自动生成待办任务"。

**方案**：新建 `goals` 模块，AI 生成里程碑 + 任务清单，确认后批量创建 todos 并关联。

**新增表**：

```sql
CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/PAUSED/COMPLETED/ARCHIVED
    target_date DATE,
    color VARCHAR(32),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_goals_owner ON goals(owner_id, deleted_at);

CREATE TABLE goal_todos (
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    todo_id UUID NOT NULL REFERENCES todos(id) ON DELETE CASCADE,
    milestone_label VARCHAR(128),   -- 里程碑分组
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (goal_id, todo_id)
);
```

**接口**：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/goals` | 目标列表 |
| POST | `/api/v1/goals` | 创建目标 |
| GET | `/api/v1/goals/{goalId}` | 目标详情 |
| PATCH | `/api/v1/goals/{goalId}` | 更新 |
| DELETE | `/api/v1/goals/{goalId}` | 删除 |
| POST | `/api/v1/goals/{goalId}/ai-plan` | AI 生成执行计划（预览） |
| POST | `/api/v1/goals/{goalId}/ai-plan/confirm` | 确认 → 批量建 todos + 关联 |
| GET | `/api/v1/goals/{goalId}/todos` | 目标下任务列表 |
| GET | `/api/v1/goals/{goalId}/progress` | 进度统计 |
| POST | `/api/v1/goals/{goalId}/todos/{todoId}` | 手动关联已有任务 |
| DELETE | `/api/v1/goals/{goalId}/todos/{todoId}` | 取消关联 |

**AI 计划生成**：

请求 `POST /api/v1/goals/{goalId}/ai-plan`：

```json
{ "context": "我是前端工程师，想在3个月内转型全栈，每天可用2小时" }
```

预览响应：

```json
{
  "previewId": "xxx",
  "milestones": [
    {
      "label": "第1月：后端基础",
      "todos": [
        { "title": "完成 Node.js 基础教程", "dueDate": "2026-07-15", "importance": "IMPORTANT" },
        { "title": "搭建第一个 Express API", "dueDate": "2026-07-31" }
      ]
    },
    {
      "label": "第2月：数据库与认证",
      "todos": [
        { "title": "学习 PostgreSQL 基础", "dueDate": "2026-08-15" },
        { "title": "实现 JWT 认证流程", "dueDate": "2026-08-31", "importance": "IMPORTANT" }
      ]
    }
  ]
}
```

确认后：循环调用 `todoService.createTodo()`（放入默认系统清单）+ 写 `goal_todos` 关联 + 每次 `recordChange`。新增 `SyncResourceType.GOAL` / `GOAL_TODO` 接入同步。

**进度统计**（`GET /goals/{goalId}/progress`）：

```json
{
  "totalTodos": 8,
  "completedTodos": 3,
  "completionRate": 0.375,
  "byMilestone": [
    { "label": "第1月：后端基础", "total": 2, "completed": 2, "rate": 1.0 },
    { "label": "第2月：数据库与认证", "total": 2, "completed": 1, "rate": 0.5 }
  ],
  "daysToTarget": 75
}
```

**成本**：新模块 + AI 计划生成 + 同步接入，约 3-4 天。

---

## 五、数据库迁移（V2）

新增 `src/main/resources/db/migration/V2__ai_and_goals.sql`：

```sql
-- AI 报告
CREATE TABLE ai_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    stats_json JSONB NOT NULL,
    content TEXT NOT NULL,
    model VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_ai_reports_user_type_period UNIQUE (user_id, type, period_start)
);

-- AI 建议
CREATE TABLE ai_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payload_json JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_suggestions_user_expires ON ai_suggestions(user_id, expires_at);

-- 目标
CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    target_date DATE,
    color VARCHAR(32),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_goals_owner ON goals(owner_id, deleted_at);

-- 目标-任务关联
CREATE TABLE goal_todos (
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    todo_id UUID NOT NULL REFERENCES todos(id) ON DELETE CASCADE,
    milestone_label VARCHAR(128),
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (goal_id, todo_id)
);
CREATE INDEX idx_goal_todos_todo ON goal_todos(todo_id);

-- 同步资源类型扩展（无需改表，SyncResourceType 枚举新增 GOAL/GOAL_TODO/AI_REPORT）
```

> **不修改 todos 表**。计划模块的层级用 `goal_todos` 表达，不引入 `parent_id`，保持现有任务结构不变，降低迁移风险。

---

## 六、新增接口清单汇总

| 模块 | 接口数 | 新增 |
|---|---|---|
| 日历视图 | 2 | `GET /calendar`, `GET /calendar/{date}/todos` |
| 自然语言建任务 | 2 | `POST /ai/todos/parse`, `POST /ai/todos` |
| 任务拆解 | 2 | `POST /ai/todos/{id}/decompose`, `.../confirm` |
| 日报周报 | 3 | `GET /ai/reports/daily`, `GET /ai/reports/weekly`, `POST /ai/reports/generate` |
| 个性化建议 | 1 | `GET /ai/suggestions` |
| 计划模块 | 9 | `/goals` CRUD + `/ai-plan` + 关联 + 进度 |
| **合计** | **19** | |

**SyncResourceType 新增**：`GOAL`、`GOAL_TODO`（AI 报告/建议不入同步，实时生成）。

---

## 七、成本估算

| 项 | 成本 |
|---|---|
| **开发** | P0 约 1 周 / P1 约 1.5 周 / P2 约 2 周，合计约 4-5 周 |
| **LLM 调用** | DeepSeek 定价约 ¥1/百万 input token、¥2/百万 output token。单用户日均 10 次 AI 调用 × 500 token ≈ 0.01 元/用户/日。1000 活跃用户约 ¥300/月 |
| **基础设施** | 零新增。复用现有 PG + Redis + RestClient，无需新中间件 |
| **依赖** | 零新增 Maven 依赖 |

---

## 八、风险与对策

| 风险 | 对策 |
|---|---|
| LLM 解析时间不准 | 后端用 Hutool `DateUtil` 二次校验；LLM 输出仅供参考 |
| LLM 服务不可用 | `app.llm.enabled` 开关；建任务降级为整句作 title；建议降级为纯规则 |
| LLM 成本失控 | 每用户每日 quota；Redis 缓存；规则优先减少 LLM 调用 |
| AI 生成脏数据 | 所有 AI 操作"预览→确认"两步，不直接落库 |
| 同步链路断裂 | goals/goal_todos 接入 `SyncChangeService`，复用现有机制 |
| 多实例定时重复 | Redisson 分布式锁包裹 `@Scheduled` 任务 |

---

## 九、实施建议

1. **先做 P0**：日历视图半天可上线（纯后端），自然语言建任务验证 LlmClient 可用性。
2. **LLM 选型**：推荐 DeepSeek（性价比最高）或通义千问（阿里生态），均为 OpenAI 兼容协议，切换只需改 `base-url` + `model` + `api-key`。
3. **Prompt 版本管理**：Prompt 模板放 `src/main/resources/prompts/*.txt`，便于迭代不重启（可选，MVP 可硬编码）。
4. **前端配合**：日历视图和计划模块是纯展示，前端工作量大；AI 功能前端只需加"输入框 + 预览卡片 + 确认按钮"。
