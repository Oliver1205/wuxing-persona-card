# 外部短链服务对接说明

适用阶段：v1.1

外部服务路径：`/Users/linyuxiang/JavaBackend/01_Projects/shortlink`

推荐服务：`aggregation`，默认端口 `8003`。

## 1. 关键配置对齐

五行项目配置：

```text
SHORT_LINK_MODE=external
SHORT_LINK_EXTERNAL_BASE_URL=http://host.docker.internal:8003
SHORT_LINK_EXTERNAL_GROUP_ID=wuxing_persona
SHORT_LINK_EXTERNAL_DOMAIN=s.example.com
SHORT_LINK_EXTERNAL_FALLBACK_TO_INTERNAL=true
SHORT_LINK_EXTERNAL_STATS_ENABLED=true
SHORT_LINK_EXTERNAL_STATS_ENABLE_STATUS=0
SHORT_LINK_EXTERNAL_STATS_CACHE_TTL_SECONDS=60
```

外部 aggregation 默认配置重点：

```text
server.port=8003
short-link.domain.default=nurl.ink:8003
short-link.demo-mode.enable=false
short-link.goto-domain.white-list.enable=false
```

生产要求：

- `SHORT_LINK_EXTERNAL_DOMAIN` 必须等于外部服务生成短链时使用的 `domain`。
- `SHORT_LINK_EXTERNAL_STATS_CACHE_TTL_SECONDS` 默认 `60`，用于降低后台短链列表重复刷新时的外部统计 HTTP 调用；设为 `0` 可关闭。
- 如果短链服务对外使用 `s.example.com`，不要继续使用本地默认 `nurl.ink:8003`。
- 五行 `APP_BASE_URL` 必须是用户可访问的 H5 域名，否则外部短链 302 会跳到不可访问地址。

## 2. 系统用户 header

五行后端调用外部接口时固定传递：

```http
username: wuxing_system
userId: wuxing-system
realName: wuxing-system
```

这些值来自：

```text
SHORT_LINK_EXTERNAL_SYSTEM_USERNAME
SHORT_LINK_EXTERNAL_SYSTEM_USER_ID
SHORT_LINK_EXTERNAL_SYSTEM_REAL_NAME
```

外部短链项目的统计接口会按用户上下文校验分组归属。生产环境应创建专用系统用户和 `wuxing_persona` 分组，不建议复用 demo 的 `admin/admin123456`。

## 3. 创建短链

五行调用：

```http
POST /api/short-link/v1/create
Content-Type: application/json
username: wuxing_system
userId: wuxing-system
realName: wuxing-system
```

请求体：

```json
{
  "domain": "s.example.com",
  "originUrl": "https://wuxing.example.com/result/R202606100001",
  "gid": "wuxing_persona",
  "createdType": 0,
  "validDateType": 0,
  "validDate": null,
  "describe": "五行人格卡结果 R202606100001"
}
```

响应中五行只依赖：

```json
{
  "code": "0",
  "data": {
    "fullShortUrl": "https://s.example.com/Abc123"
  }
}
```

五行会从 `fullShortUrl` 提取 `shortCode=Abc123`，并保存本地业务绑定：

```text
resultId -> shortCode -> shortUrl
```

## 4. 短链访问

推荐生产入口：

```text
https://s.example.com/Abc123 -> 外部短链服务 -> 302 -> https://wuxing.example.com/result/{resultId}
```

同域备选：

```text
https://wuxing.example.com/s/Abc123 -> Nginx rewrite -> 外部短链服务 /Abc123
```

五行后端继续保留 `/s/{shortCode}` 兼容入口：

- internal 模式下负责真实跳转。
- external 模式下可作为外部服务不可用时的本地兼容入口。
- 明天部署外部短链入口后，需要直接访问 `shortUrl` 再验证一次外部 302。

## 5. 统计接口

短链列表 PV / UV / UIP：

```http
GET /api/short-link/v1/stats
```

查询参数：

```text
fullShortUrl=s.example.com/Abc123
gid=wuxing_persona
enableStatus=0
startDate=2026-06-10
endDate=2026-06-11
```

短链访问明细：

```http
GET /api/short-link/v1/stats/access-record
```

额外分页参数：

```text
current=1
size=20
```

五行后台使用 `statSource` 标记数据来源：

- `external`：外部统计接口成功返回且 domain 匹配。
- `local`：external 关闭、失败、domain 不匹配或当前为 internal 模式。

## 6. 失败与降级

| 外部失败 | 五行处理 |
| --- | --- |
| 创建接口不可达 | 默认降级到 `InternalShortLinkProvider` |
| 创建接口返回非 `code=0` | 抛错后按 fallback 配置处理 |
| 外部短码与本地已有绑定冲突 | 默认降级；关闭 fallback 时抛错 |
| 外部短链已创建，但本地绑定写入失败 | 返回 500，并提示需要人工或补偿治理 |
| 统计接口失败 | 后台短链列表回退本地 PV / UV / UIP |
| 访问明细接口失败 | 后台短链详情回退本地 `visit_event` |
| domain 不匹配 | 不调用外部统计，防止错误查询 |

当前五行项目没有调用外部短链删除/禁用接口，也没有 outbox 补偿任务。因此“外部已创建、本地绑定失败”不能自动宣称恢复成功；上线前如果强依赖 external 模式，应补外部短链撤销接口、补偿队列或后台巡检任务。

## 7. 最小联调顺序

1. 启动外部短链项目 aggregation。
2. 确认外部 MySQL `link` 库、Redis、Nacos 可用。
3. 准备 `wuxing_system` 和 `wuxing_persona`。
4. 配置五行 `deploy/.env.external`。
5. 运行：

```bash
scripts/external-shortlink-preflight.sh deploy/.env.external --probe
```

6. 启动五行 external Compose。
7. 运行：

```bash
WUXING_BASE_URL=http://127.0.0.1:8088 \
ADMIN_TOKEN=<your-admin-token> \
scripts/external-shortlink-smoke-test.sh
```

8. 直接访问脚本输出的 `shortUrl`，确认外部短链服务自身 302 成功。
