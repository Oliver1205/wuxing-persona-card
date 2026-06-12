# 真实域名上线信息收集清单

创建日期：2026-06-12

这份清单用于在正式绑定真实域名前，把 Codex 无法从仓库里自动获得的外部信息一次性收齐。填写完这些信息后，就可以进入 `docs/domain-server-runbook.md` 的服务器执行阶段。

## 1. 必填信息

| 信息 | 示例 | 用途 |
| --- | --- | --- |
| 主域名 | `wuxing.example.com` | 设置 DNS、`APP_BASE_URL`、HTTPS 证书和 smoke 测试 |
| 服务器公网 IP | `82.157.137.36` | 校验 DNS A 记录是否解析到正确服务器 |
| 服务器 SSH 入口 | `ubuntu@82.157.137.36` | 登录服务器，同步 `/opt/wuxing-persona-card` 并重启服务 |
| DNS 服务商 | 腾讯云 DNSPod / 阿里云 / Cloudflare | 添加或确认 A 记录 |
| 是否已备案或接入备案 | 已备案 / 未备案 / 不确定 | 判断中国大陆服务器上的域名访问是否可能被拦截 |
| HTTPS 方案 | Certbot / 云厂商证书 / CDN 证书 / 先临时 HTTP | 决定宿主机 Nginx 和证书签发步骤 |
| 是否需要短链子域名 | 暂不需要 / `s.example.com` | 首次上线建议暂不启用，先保持 internal 短链 |

## 2. 敏感信息处理

不要直接把这些明文写进 Git、截图或长期文档：

```text
ADMIN_TOKEN
HASH_SALT
MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD
证书私钥
服务器密码
```

推荐做法：

- `ADMIN_TOKEN`、`HASH_SALT`、数据库密码可以由 Codex 在服务器上生成，只写入服务器本地 `deploy/.env`。
- 如果你已经有 token 或密码，可以只告诉 Codex“我来手动输入”，不要把明文发到聊天里。
- SSH 如果是密钥登录，只需要确认本机能否 `ssh <user>@<ip>`，不需要发送私钥。
- 云厂商/DNS 控制台如果需要你手动操作，只需要告诉 Codex 操作结果和解析记录截图中的非敏感字段。

## 3. 推荐首发答案模板

把下面信息补齐后发给 Codex：

```text
主域名：
服务器公网 IP：
服务器 SSH：
服务器仓库路径：/opt/wuxing-persona-card
DNS 服务商：
DNS A 记录是否已添加：
域名是否已备案或接入备案：
HTTPS 方案：
是否允许 Codex 在服务器生成 ADMIN_TOKEN / HASH_SALT / 数据库密码：
是否需要短链子域名：
是否允许临时 HTTP 验证：
```

## 4. DNS 记录建议

首次上线只需要主域名：

```text
主机记录：<主域名前缀>
记录类型：A
记录值：<服务器公网 IP>
TTL：默认或 600 秒
```

短链子域名建议第二阶段再做：

```text
主机记录：s
记录类型：A
记录值：<服务器公网 IP 或独立短链服务 IP>
TTL：默认或 600 秒
```

## 5. Codex 拿到信息后的执行顺序

1. 本地确认 `origin/main` 已包含最新 runbook 和 Nginx/TLS 模板。
2. 登录服务器，确认 `/opt/wuxing-persona-card`、Docker、Compose、UFW 和当前容器状态。
3. 同步 `origin/main`，或在 GitHub 网络不通时改用上传同步。
4. 更新服务器本地 `deploy/.env`，确保 `APP_BASE_URL=https://<主域名>`。
5. 让 Compose Nginx 只监听 `127.0.0.1:8088`。
6. 配置宿主机 Nginx 临时 HTTP 站点，完成 DNS 首绑验证。
7. 签发或安装 HTTPS 证书。
8. 启用 `deploy/host-nginx-domain-tls.example.conf` 的 HTTPS 配置。
9. 执行 `scripts/domain-bind-preflight.sh`、`scripts/production-smoke-test.sh` 和 `scripts/performance-smoke-test.sh`。
10. 更新学习文档和工作日志，记录真实域名上线证据。

## 6. 当前不能由仓库自动完成的事

- 注册或购买域名。
- 修改 DNS 控制台中的解析记录。
- 完成中国大陆服务器可能需要的备案/接入备案。
- 获取你的 SSH 密钥、云控制台权限或证书私钥。
- 在没有真实域名的情况下证明 `https://<主域名>` 可访问。
