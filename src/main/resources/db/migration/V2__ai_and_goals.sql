-- V2: AI features and goals module
-- Date: 2026-07-01

-- AI 报告表
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

-- AI 建议缓存表
CREATE TABLE ai_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payload_json JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_suggestions_user_expires ON ai_suggestions(user_id, expires_at);

-- 目标表
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

-- 目标-任务关联表
CREATE TABLE goal_todos (
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    todo_id UUID NOT NULL REFERENCES todos(id) ON DELETE CASCADE,
    milestone_label VARCHAR(128),
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (goal_id, todo_id)
);
CREATE INDEX idx_goal_todos_todo ON goal_todos(todo_id);
