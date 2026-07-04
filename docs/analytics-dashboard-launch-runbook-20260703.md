# 五行人格卡实时数据中台上线 Runbook

日期：2026-07-03

## 1. 本地确认到的项目现状

- 前端：Vue 3 + Vue Router 4 + TypeScript + Vite。
- 后端：Spring Boot 3 + Java 17 + MyBatis 注解 SQL。
- 数据库：MySQL 8.x，测试/本地可使用 H2。
- Redis：`deploy/docker-compose.yml` 中已有 `redis:7.4-alpine`，当前在线统计本轮先走 MySQL 心跳表，Redis 仍保留给既有缓存能力。
- 后台：已有 `/admin` 运维后台，管理端 API 使用 `X-Admin-Token` 保护。
- 现有埋点：已有 `/api/events` 与 `visit_event`；本轮新增 `/api/analytics/*` 会话与心跳接口，并复用 `visit_event` 记录关键行为。
- 部署方式：仓库内生产部署路径以 Docker Compose 为主，包含 `mysql`、`redis`、`backend`、`nginx` 四个服务。

## 2. 本轮新增能力

- 用户端匿名 analytics：
  - `visitorId`：localStorage 生成并复用。
  - `sessionId`：sessionStorage 生成并复用。
  - `session_start`：进入项目后发送。
  - `heartbeat`：默认 30 秒发送。
  - `session_end`：页面关闭时尽量用 `sendBeacon` 发送。
  - `page_view`：路由变化时发送。
  - `result_generated`、`share_image_click`、`copy_match_code`、`copy_link`、`share_click`、`match_enter`：按关键动作记录。

- 后台实时监控：
  - 数据中台采用板块分页，不再把所有统计模块瀑布式堆在同一长页。
  - 当前分页：实时监控、核心概览、趋势运行、归因短链。
  - 当前在线用户。
  - 当前在线会话。
  - 今日 PV / UV。
  - 今日结果生成。
  - 今日分享点击。
  - 今日匹配入口。
  - 最近 1 小时 / 24 小时 / 7 天 / 30 天趋势曲线。
  - 最近 24 小时事件流。

## 3. 新增接口

用户端：

- `POST /api/analytics/session/start`
- `POST /api/analytics/heartbeat`
- `POST /api/analytics/event`
- `POST /api/analytics/session/end`

管理端，均需 `X-Admin-Token`：

- `GET /api/admin/metrics/realtime`
- `GET /api/admin/metrics/timeseries?range=1h|24h|7d|30d`
- `GET /api/admin/metrics/events?range=24h`
- `GET /api/admin/metrics/funnel`

## 4. 新增表与迁移

迁移文件：

- `docs/analytics-realtime-dashboard-migration-20260703.sql`

新增表：

- `analytics_visitor`
- `analytics_session`
- `analytics_metric_snapshot`

隐私边界：

- 只保存 visitor/session/IP/UA 的 hash。
- 不保存姓名、出生年月日时、性别、出生地。
- analytics event 仅允许记录事件名、页面路径、结果 ID、短码、渠道、活动和设备类型等运营字段。

## 5. 新增环境变量

已写入 `deploy/.env.example`、`deploy/.env.external.example` 和 Docker Compose：

```bash
ANALYTICS_ENABLED=true
ANALYTICS_HEARTBEAT_INTERVAL=30000
ANALYTICS_ONLINE_WINDOW=120000
ADMIN_TOKEN=<生产环境必须配置强 token>
```

## 6. 本地质量门禁

已执行通过：

```bash
npm --prefix frontend run build
mvn -q -DskipTests package
mvn -q test
node scripts/verify-frontend-contracts.mjs
```

测试覆盖：

- analytics session / heartbeat / event 能驱动实时指标。
- 管理端 metrics 接口无 token 会返回 401。
- 趋势接口能返回点位。
- 前端现有路由、分享、测试流和后台选择器契约未被破坏。
- `/admin` 后台已通过桌面与 390px 移动端浏览器检查：4 个板块分页入口可见，切换后仅展示当前板块，无横向溢出。

## 7. 上线前必须补充的信息

正式部署前请确认：

- 服务器 IP。
- SSH 用户名。
- SSH key 或密码。
- 服务器部署目录，例如 `/opt/wuxing-persona-card`。
- 线上域名，例如 `wuxingcard.cn`。
- 生产 `.env` 路径。
- 生产 `ADMIN_TOKEN` 是否已配置。
- 数据库连接方式，确认是否就是 Docker Compose 内的 `wuxing-mysql`。
- 是否允许执行数据库 migration。
- 是否允许重启 Docker Compose 服务。
- 如果有外层 HTTPS/Nginx：外层 Nginx 配置路径和证书路径。

缺少以上信息时，不执行生产覆盖、迁移或重启。

## 8. 建议部署流程

以下命令需在服务器部署目录执行，并以实际目录为准。

### 8.1 备份

```bash
cd /opt
backup_dir="/opt/wuxing-persona-card.backup-$(date +%Y%m%d%H%M%S)"
cp -a /opt/wuxing-persona-card "$backup_dir"
echo "$backup_dir"
```

如果使用 Docker Compose 内 MySQL：

```bash
cd /opt/wuxing-persona-card/deploy
docker compose exec mysql sh -lc 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers "$MYSQL_DATABASE" > /tmp/wuxing-db-backup.sql'
docker compose cp mysql:/tmp/wuxing-db-backup.sql "$backup_dir/wuxing-db-backup.sql"
```

同时确认备份内包含：

- `deploy/.env`
- `deploy/docker-compose.yml`
- `deploy/nginx.conf`
- 当前源码与构建上下文
- 数据库备份 SQL

### 8.2 上传最新代码或压缩包

如果使用 Git：

```bash
cd /opt/wuxing-persona-card
git status -sb
git pull --ff-only origin main
```

如果 GitHub 网络不稳定，使用本机打包上传：

```bash
tar -xzf /tmp/wuxing-persona-card-latest.tar.gz -C /opt/wuxing-persona-card
cp "$backup_dir/deploy/.env" /opt/wuxing-persona-card/deploy/.env
```

### 8.3 执行迁移

执行前必须确认已完成数据库备份。

```bash
cd /opt/wuxing-persona-card/deploy
docker compose exec -T mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < ../docs/analytics-realtime-dashboard-migration-20260703.sql
```

### 8.4 构建与重启

```bash
cd /opt/wuxing-persona-card/deploy
docker compose build backend nginx
docker compose up -d backend nginx
docker compose ps
```

### 8.5 缓存策略确认

`deploy/nginx.conf` 已设置：

- `index.html`：`no-cache, no-store, must-revalidate`
- `/assets/`：`public, max-age=31536000, immutable`
- `/api/analytics`：独立事件限流
- `/api/`：普通 API 限流

重启后执行：

```bash
docker compose exec nginx nginx -t
docker compose restart nginx
```

## 9. 上线验收清单

用户端：

- `https://wuxingcard.cn/` 首页可访问。
- `/test` 能完成出生信息和 5 题。
- `/result/{id}` 是最新星曜取象结果页。
- 保存分享图可用。
- 复制短码可用。
- 双人匹配入口可用。
- 移动端无横向溢出。
- 用户端 console 无明显错误。
- 埋点失败不影响主流程。

数据后台：

- `/admin` 需要 token。
- `/api/admin/metrics/realtime` 无 token 返回 401。
- 输入生产 token 后能看到当前在线用户。
- 页面采用板块分页：实时监控、核心概览、趋势运行、归因短链。
- 切换板块时不是瀑布式一划到底，当前页之外的统计区应隐藏。
- heartbeat 后在线用户数会更新。
- 趋势曲线可切换 1h / 24h / 7d / 30d。
- 今日 PV / UV / 结果生成 / 分享点击 / 匹配入口可见。
- 空数据状态不显示 `undefined` / `NaN`。
- 最近事件流不展示敏感出生信息。

命令验收：

```bash
curl -s -i https://wuxingcard.cn/api/readiness
curl -s -i -H "X-Admin-Token: $ADMIN_TOKEN" https://wuxingcard.cn/api/admin/metrics/realtime
curl -s -i https://wuxingcard.cn/api/admin/metrics/realtime
```

## 10. 回滚方案

如果新版启动失败：

```bash
cd /opt
mv /opt/wuxing-persona-card "/opt/wuxing-persona-card.failed-$(date +%Y%m%d%H%M%S)"
cp -a "$backup_dir" /opt/wuxing-persona-card
cd /opt/wuxing-persona-card/deploy
docker compose up -d backend nginx
docker compose ps
```

如果只是 Nginx 配置问题：

```bash
cp "$backup_dir/deploy/nginx.conf" /opt/wuxing-persona-card/deploy/nginx.conf
cd /opt/wuxing-persona-card/deploy
docker compose build nginx
docker compose up -d nginx
docker compose exec nginx nginx -t
```

本轮新增表不影响旧业务表；通常无需回滚数据库结构。如必须回滚数据，请先停服务，再从备份 SQL 恢复，避免覆盖新增真实结果数据。

## 11. 当前阻塞点

本地开发、构建、测试、部署脚本与迁移文件已准备好；正式部署仍需你提供服务器连接信息和生产环境确认项。未经确认前，不执行生产覆盖、数据库迁移或服务重启。
