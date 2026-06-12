# 生产运维收口 Runbook

创建日期：2026-06-12

本文档记录五行人格卡当前真实服务器的可维护操作。它不替代学习手册，也不展开面试讲解；只保留生产调试、备案切换、安全收口和故障复查命令。

## 1. 当前现实状态

- 服务器公网 IP：`82.157.137.36`
- 服务器仓库：`/opt/wuxing-persona-card`
- 备案通过前可用入口：`http://82.157.137.36`
- 备案通过后推荐入口：`https://www.wuxingcard.cn`
- 当前短链模式：`SHORT_LINK_MODE=internal`
- 当前容器入口：`NGINX_HTTP_PORT=127.0.0.1:8088`
- 宿主机 Nginx 负责公网 `80/443`，容器 Nginx 只在本机端口服务。

## 2. 备案前 IP 调试模式

备案未通过时，腾讯云会拦截指向中国大陆服务器的域名访问。此时不要把线上分享入口配置成域名，先使用 IP：

```bash
cd /opt/wuxing-persona-card
BASE_URL=http://82.157.137.36 \
NGINX_HTTP_PORT_VALUE=127.0.0.1:8088 \
APPLY_COMPOSE=true \
scripts/set-production-entry.sh
```

验证：

```bash
BASE_URL=http://82.157.137.36 scripts/production-health-check.sh

set -a
. deploy/.env
set +a
BASE_URL=http://82.157.137.36 ADMIN_TOKEN="$ADMIN_TOKEN" scripts/production-smoke-test.sh
```

后台临时入口：

```text
http://82.157.137.36/admin
```

## 3. 备案后正式域名模式

备案通过后，先确认域名访问不再显示腾讯云备案拦截页，再切回正式入口：

```bash
cd /opt/wuxing-persona-card
BASE_URL=https://www.wuxingcard.cn \
SHORT_LINK_EXTERNAL_DOMAIN_VALUE=www.wuxingcard.cn \
NGINX_HTTP_PORT_VALUE=127.0.0.1:8088 \
APPLY_COMPOSE=true \
scripts/set-production-entry.sh
```

验证：

```bash
DOMAIN=www.wuxingcard.cn EXPECTED_IP=82.157.137.36 scripts/domain-dns-readiness.sh
DOMAIN=www.wuxingcard.cn BASE_URL=https://www.wuxingcard.cn scripts/domain-bind-preflight.sh

set -a
. deploy/.env
set +a
BASE_URL=https://www.wuxingcard.cn ADMIN_TOKEN="$ADMIN_TOKEN" scripts/production-smoke-test.sh
```

正式后台入口：

```text
https://www.wuxingcard.cn/admin
```

## 4. 安全收口

真实服务器做完联调后执行：

```bash
cd /opt/wuxing-persona-card
scripts/server-security-audit.sh
```

必须关注：

- `deploy/.env` 权限建议为 `600` 或 `640`。
- 临时 SSH 公钥 `codex-wuxingcard-domain-20260612` 应在本轮远程维护结束后删除。
- 服务器密码曾出现在截图里，建议在腾讯云控制台重置一次。
- `ADMIN_TOKEN` 如果被复制到不可信位置，应在服务器本地轮换。
- MySQL、Redis、Spring Boot 不应暴露公网，只应在 Docker 内网或本机监听。

删除临时 SSH 公钥：

```bash
python3 - <<'PY'
from pathlib import Path

path = Path.home() / ".ssh" / "authorized_keys"
marker = "codex-wuxingcard-domain-20260612"
if path.exists():
    lines = [line for line in path.read_text().splitlines() if marker not in line]
    path.write_text("\n".join(lines) + ("\n" if lines else ""))
PY
chmod 600 ~/.ssh/authorized_keys
```

轮换后台 token：

```bash
cd /opt/wuxing-persona-card
new_token="$(openssl rand -base64 32 | tr -d '\n')"
cp deploy/.env "deploy/.env.bak-token-$(date +%Y%m%d%H%M%S)"
python3 - "$new_token" <<'PY'
from pathlib import Path
import sys

token = sys.argv[1]
path = Path("deploy/.env")
lines = path.read_text().splitlines()
for index, line in enumerate(lines):
    if line.startswith("ADMIN_TOKEN="):
        lines[index] = f"ADMIN_TOKEN={token}"
        break
else:
    lines.append(f"ADMIN_TOKEN={token}")
path.write_text("\n".join(lines) + "\n")
PY
scripts/deploy-preflight.sh deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --force-recreate backend
echo "$new_token"
```

## 5. 备份和恢复

创建备份：

```bash
cd /opt/wuxing-persona-card
scripts/backup-mysql.sh
```

恢复演练必须在确认环境里执行，不要直接对生产库试错：

```bash
CONFIRM_RESTORE=yes scripts/restore-mysql.sh backups/<backup-file>.sql.gz
```

## 6. 故障判断顺序

1. `curl -I http://82.157.137.36/admin`：确认服务器和应用入口是否活着。
2. `curl -fsS http://82.157.137.36/api/health`：确认后端是否活着。
3. `docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps`：确认容器状态。
4. `sudo ss -ltnp | grep -E ':(80|443|8088)'`：确认宿主机和容器端口。
5. `sudo tail -80 /var/log/nginx/error.log`：确认宿主机 Nginx 错误。
6. `docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs --tail=120 backend nginx`：确认应用日志。

备案未完成时，域名访问显示腾讯云备案拦截页是预期现象，不应按应用故障处理。
