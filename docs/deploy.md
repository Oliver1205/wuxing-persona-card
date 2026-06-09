# 部署说明

当前状态：Docker Compose 初版已配置，并已完成容器运行验收。MySQL、Redis、backend、nginx 均可启动，Nginx 入口可完成 API、短链 302 和后台统计验证。v0.4 已补齐 external 短链创建和统计配置，v0.7 已补生产短链路由示例和部署预检脚本，v1.0 已补稳定版发布检查表，默认仍使用 `internal` 模式。

## 1. 部署架构

```text
浏览器
  -> Nginx
    -> Vue H5 静态资源
    -> /api/** 代理到 Spring Boot
    -> /s/**   代理到 Spring Boot 短链入口
    -> /admin  返回 H5 后台页面
Spring Boot
  -> MySQL
  -> Redis
  -> optional external shortlink service
```

默认短链接内置在五行后端。切换 `SHORT_LINK_MODE=external` 后，五行后端会优先调用独立短链服务创建短链；`SHORT_LINK_EXTERNAL_STATS_ENABLED=true` 时，后台短链列表会读取独立短链服务的 PV / UV / UIP。

## 2. Compose 服务

`deploy/docker-compose.yml` 包含：

| 服务 | 作用 |
| --- | --- |
| `mysql` | 存储测算结果、短链映射、访问事件 |
| `redis` | 缓存结果详情、短链解析、无效短码空值 |
| `backend` | Spring Boot API 与短链入口 |
| `nginx` | 托管前端静态资源并代理 API |

MySQL 初始化脚本挂载自：

```text
backend/src/main/resources/db/schema.sql
```

## 3. 环境变量

复制示例文件：

```bash
cp deploy/.env.example deploy/.env
```

上线前至少替换：

```text
APP_BASE_URL=https://your-domain.com
MYSQL_PASSWORD=<strong-password>
MYSQL_ROOT_PASSWORD=<strong-password>
ADMIN_TOKEN=<strong-random-token>
HASH_SALT=<strong-random-salt>
```

说明：

- `APP_BASE_URL` 会用于生成短链接完整地址。
- `ADMIN_TOKEN` 用于后台接口 `X-Admin-Token` 校验。
- `HASH_SALT` 用于 clientId、IP、User-Agent 的 hash 脱敏。
- `NGINX_HTTP_PORT` 可用于本地避开 80 端口冲突，例如 `8088`。
- `SHORT_LINK_MODE` 默认为 `internal`，可切换为 `external`，让后端优先调用外部短链服务创建短链。
- `SHORT_LINK_EXTERNAL_BASE_URL` 是外部短链服务地址，例如 `http://shortlink:8003`。
- `SHORT_LINK_EXTERNAL_GROUP_ID` 对应外部短链项目分组，例如 `wuxing_persona`。
- `SHORT_LINK_EXTERNAL_DOMAIN` 对应外部短链项目 `short-link.domain.default`，例如 `nurl.ink:8003`。
- `SHORT_LINK_EXTERNAL_FALLBACK_TO_INTERNAL` 默认为 `true`，外部服务不可用时自动降级到内置短链。
- `SHORT_LINK_EXTERNAL_CONNECT_TIMEOUT_MILLIS` 和 `SHORT_LINK_EXTERNAL_READ_TIMEOUT_MILLIS` 用于限制外部短链调用等待时间。
- `SHORT_LINK_EXTERNAL_STATS_ENABLED` 默认为 `false`，开启后后台短链列表会尝试读取外部短链 PV / UV / UIP。
- `SHORT_LINK_EXTERNAL_STATS_ENABLE_STATUS` 对应外部短链统计接口的 `enableStatus`，默认 `0`。
- `BACKEND_MAVEN_IMAGE`、`BACKEND_RUNTIME_IMAGE`、`FRONTEND_NODE_IMAGE`、`FRONTEND_NGINX_IMAGE` 是可选基础镜像参数。默认使用官方镜像，Docker Hub 不稳定时可临时切换到可信镜像源。

上线前执行预检：

```bash
scripts/deploy-preflight.sh deploy/.env
```

预检会阻止常见上线错误，例如仍使用 `change-me`、示例域名、缺少 external 短链配置或 `SHORT_LINK_MODE` 非法。

v1.0 发布检查表见：

```text
docs/v1.0-release-checklist.md
```

## 4. 启动命令

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
```

本地验收若需要避开 80 端口：

```bash
APP_BASE_URL=http://localhost:8088 \
NGINX_HTTP_PORT=8088 \
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
```

如果 Docker Hub 拉取基础镜像超时，可临时指定镜像源：

```bash
APP_BASE_URL=http://localhost:8088 \
NGINX_HTTP_PORT=8088 \
BACKEND_MAVEN_IMAGE=docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-17 \
BACKEND_RUNTIME_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:17-jre \
FRONTEND_NODE_IMAGE=docker.m.daocloud.io/library/node:20-alpine \
FRONTEND_NGINX_IMAGE=docker.m.daocloud.io/library/nginx:1.27-alpine \
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml up --build -d
```

查看日志：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f backend
```

停止：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
```

## 5. Nginx 路由

当前 `deploy/nginx.conf`：

```text
/              -> Vue H5
/api/**        -> backend:8080/api/**
/s/**          -> backend:8080/s/**
/admin         -> Vue H5 后台路由
```

v0.4 已完成外部短链服务级联调。若生产接入独立短链服务，同域路径可改成：

```nginx
location /s/ {
    rewrite ^/s/(.*)$ /$1 break;
    proxy_pass http://shortlink:8003;
}
```

更推荐生产使用短链子域名：

```text
your-domain.com   -> 五行 H5 和 API
s.your-domain.com -> 独立短链服务
```

v0.7 新增完整示例：

```text
deploy/nginx.shortlink-routing.example.conf
```

该文件不被默认 Compose 加载，生产部署时可以按实际域名复制到 Nginx 配置目录后修改 `server_name`。

只验证“外部创建短链 + 外部统计读取 + 失败降级”时，可以先保持 Nginx `/s/**` 指向五行后端；等独立短链服务稳定后，再把 `/s/**` 或短链子域名切到短链服务。

external 模式建议配置：

```text
SHORT_LINK_MODE=external
SHORT_LINK_EXTERNAL_BASE_URL=http://shortlink:8003
SHORT_LINK_EXTERNAL_DOMAIN=s.your-domain.com
SHORT_LINK_EXTERNAL_STATS_ENABLED=true
```

如果使用短链子域名，`SHORT_LINK_EXTERNAL_DOMAIN` 必须与短链服务生成的 `fullShortUrl` 域名一致，例如 `s.your-domain.com`。

## 6. 验证清单

容器启动后验证：

```bash
scripts/deploy-preflight.sh deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml config
curl http://localhost/api/health
curl http://localhost/api/questions
```

浏览器验证：

- 打开 `http://localhost/`。
- 完成一次测试并生成结果。
- 复制短链接并打开。
- 进入 `http://localhost/admin`，输入 `ADMIN_TOKEN` 后查看统计。

## 7. 无 Docker 本地演示

当 Docker daemon 未启动，但需要演示完整 H5 流程时，可以使用 H2 内存库启动后端：

```bash
cd backend
APP_BASE_URL=http://127.0.0.1:4173 mvn spring-boot:run -Dspring-boot.run.profiles=local
```

前端使用生产预览：

```bash
cd frontend
npm run build
npm run preview -- --host 127.0.0.1 --port 4173
```

访问：

```text
http://127.0.0.1:4173/
```

本地后台 token：

```text
dev-token
```

说明：

- `local` profile 使用 H2 内存库，不要求本机 MySQL。
- Redis 不启动时，缓存读写会降级，不影响测算、短链和统计主流程。
- `vite.config.ts` 已为 `dev` 和 `preview` 同时配置 `/api`、`/s` 到后端的代理。
- 该模式用于本地演示和浏览器验收，不替代正式 Docker 部署验证。

## 8. 安全检查

- MySQL 和 Redis 不暴露公网端口。
- `/admin` 接口必须校验 `X-Admin-Token`。
- 不保存明文 IP 和明文 clientId。
- 生产环境替换所有默认密码和默认 token。
- 页面保留娱乐声明。

## 9. 当前验证记录

已通过：

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```

容器启动验收使用第 4 节的 `APP_BASE_URL=http://localhost:8088`、`NGINX_HTTP_PORT=8088` 和可选镜像源参数执行。

容器状态：

```text
wuxing-mysql    Up (healthy)
wuxing-redis    Up (healthy)
wuxing-backend  Up
wuxing-nginx    Up 0.0.0.0:8088->80/tcp
```

Docker 入口验证：

```bash
curl http://127.0.0.1:8088/api/health
curl http://127.0.0.1:8088/api/questions
curl -X POST http://127.0.0.1:8088/api/results ...
curl -i http://127.0.0.1:8088/s/4fB7av
curl http://127.0.0.1:8088/api/admin/overview -H 'X-Admin-Token: change-me'
curl http://127.0.0.1:8088/api/admin/short-links/4fB7av/visits -H 'X-Admin-Token: change-me'
```

本次样例结果：

```text
resultId: R20260609005159599703
shortCode: 4fB7av
shortUrl: http://localhost:8088/s/4fB7av
short link Location: /result/R20260609005159599703?sc=4fB7av
short link PV/UV/UIP: 1/1/1
```

已完成本地演示模式浏览器验收，截图见：

```text
docs/screenshots/local-result-page.png
docs/screenshots/local-admin-overview.png
docs/screenshots/local-shortlink-detail.png
```

已完成 Docker 模式浏览器验收，截图见：

```text
docs/screenshots/docker-home-page.png
docs/screenshots/docker-result-page.png
docs/screenshots/docker-admin-token-gate.png
docs/screenshots/docker-admin-detail-protected.png
```
