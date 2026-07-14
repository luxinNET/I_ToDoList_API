#!/usr/bin/env bash
# ============================================================================
# iTodo 生产服务器一键部署 + schema 补全脚本
# 用法（在腾讯云服务器上，需 root 或能 sudo 的账号）：
#   cd /opt/itodo-src
#   git pull --ff-only            # 先确保本脚本已随仓库拉到服务器
#   bash scripts/prod_deploy.sh            # 完整：重编 + 重启 + 补表 + 校验
#   SKIP_BUILD=1 bash scripts/prod_deploy.sh   # 跳过 maven 重编（确认 jar 已含最新代码时用）
# 说明：
#   - DB 连接信息从正在运行的 itodo 进程环境 (/proc/<pid>/environ) 自动读取，
#     无需手动填密码。前提：systemd 单元里通过 Environment=/EnvironmentFile= 注入了 DB_*。
#   - schema 脚本幂等，可重复执行，只建缺失表/索引。
# ============================================================================
set -euo pipefail

DEPLOY_DIR="/opt/itodo-src"
cd "$DEPLOY_DIR"

echo "==> [1/6] 拉取最新代码"
git pull --ff-only

if [ "${SKIP_BUILD:-0}" != "1" ]; then
  echo "==> [2/6] 以 deploy 用户 maven 重编 (clean package)"
  sudo -u deploy bash -c "cd $DEPLOY_DIR && MAVEN_OPTS='-Xmx512m' mvn -Dmaven.repo.local=.m2/repository -s .m2/settings.xml -DskipTests clean package"
else
  echo "==> [2/6] 跳过 maven 重编 (SKIP_BUILD=1)"
fi

echo "==> [3/6] 重启 itodo 服务"
sudo systemctl restart itodo
sleep 3
sudo systemctl status itodo --no-pager | head -5

echo "==> [4/6] 从运行进程读取 DB 连接信息"
PID=$(systemctl show -p MainPID itodo | cut -d= -f2)
if [ -z "$PID" ] || [ "$PID" = "0" ]; then
  echo "!! 未能获取 itodo 进程 PID，请手动 export DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD 后重跑本脚本的 5/6 步"
  exit 1
fi
# shellcheck disable=SC1090
eval "$(cat /proc/$PID/environ | tr '\0' '\n' | grep -E '^DB_HOST=|^DB_PORT=|^DB_NAME=|^DB_USERNAME=|^DB_PASSWORD=' | sed 's/^/export /')"

echo "==> [5/6] 执行幂等 schema 补全脚本"
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" \
  -v ON_ERROR_STOP=1 -f scripts/prod_ensure_schema.sql

echo "==> [6/6] 校验表是否已齐全"
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -c "\dt"

echo ""
echo "DONE. 现在用真实小程序 code 调一次 /api/v1/auth/wechat-mini-program/login 验证登录。"
