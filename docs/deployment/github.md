# GitHub Repository Setup

当前项目已设计为私有 GitHub 仓库优先。

## 推荐仓库设置

- Repository name: `i_todo`
- Visibility: Private
- Default branch: `main`
- Branch protection: MVP 初期可暂缓，第一次可运行后再开启

## 本地初始化步骤

```bash
git init
git branch -M main
git add .
git commit -m "Initial TodoList API project"
```

## 方式 A：已安装 GitHub CLI

```bash
gh auth login
gh repo create i_todo --private --source=. --remote=origin --push
```

## 方式 B：先在 GitHub 网页创建私有仓库

创建空仓库后，把仓库地址添加为 remote：

```bash
git remote add origin git@github.com:<owner>/i_todo.git
git push -u origin main
```

或使用 HTTPS：

```bash
git remote add origin https://github.com/<owner>/i_todo.git
git push -u origin main
```

## 安全注意事项

- 不提交 `.env`、数据库密码、Redis 密码、JWT 密钥、微信小程序 `appid`/`secret`。
- 第一次推送前检查 `git status --short` 和 `git diff --cached`。
- 如使用 HTTPS，建议使用 GitHub Personal Access Token，不使用账号密码。
