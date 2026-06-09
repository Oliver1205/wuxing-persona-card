# 短链接系统评估与五行人格卡集成方案

评估对象：`/Users/linyuxiang/JavaBackend/01_Projects/shortlink`

来源仓库：`https://gitee.com/nageoffer/shortlink`

评估日期：2026-06-08

当前落地策略：五行人格卡 v0.1 已先实现内置短链接能力，确保 MVP 不依赖外部服务也能独立运行。v0.2 已新增短链适配层，支持 `internal` / `external` Provider 配置切换；外部服务创建失败时默认降级到内置短链，确保测算主链路不被外部服务可用性拖垮。

## 1. 项目结论

该项目是一个教学型 SaaS 短链接系统，功能完整但架构明显重于五行人格卡 MVP。它已经实现了短链生成、短链解析跳转、Redis 缓存、空值缓存、布隆过滤器防穿透、PV/UV/UIP 统计、访问记录、按分组统计、控制台登录和网关鉴权。

五行人格卡第一版不需要完整接入它的 SaaS 用户体系、控制台前端、复杂网关和多租户后台。建议把它作为独立短链服务使用，五行后端通过内部 HTTP 调用创建短链和查询统计；短链公网入口由 Nginx 代理到短链服务。

推荐最终集成方式：

```text
浏览器
  -> Nginx
    -> /              五行 H5
    -> /api/wuxing/** 五行后端
    -> /s/{code}      短链服务跳转入口

五行后端
  -> MySQL / Redis
  -> 内部调用短链服务创建短链、查询短链统计

短链服务
  -> MySQL / Redis
```

## 2. 模块解析

短链仓库主要模块：

| 模块 | 作用 | 五行 MVP 是否需要 |
| --- | --- | --- |
| `project` | 核心短链服务，包含创建、跳转、缓存、统计写入和统计查询 | 需要 |
| `admin` | SaaS 控制台后端，用户、分组、回收站、短链管理接口 | 不建议第一版接入 |
| `gateway` | Spring Cloud Gateway，校验 `username/token` 并转发 | 不建议第一版接入 |
| `aggregation` | 把 `admin` 和 `project` 聚合成一个应用，默认端口 8003 | 可作为单机部署起点 |
| `console-vue` | 短链控制台前端 | 不需要，五行项目有自己的 `/admin` |

对五行项目来说，最有价值的是 `project` 模块；如果想少启动几个服务，可以优先研究 `aggregation` 模块。

## 3. 五行项目会用到的功能

### 3.1 创建短链

核心接口：

```http
POST /api/short-link/v1/create
Content-Type: application/json
```

请求体：

```json
{
  "originUrl": "https://your-domain.com/result/R202606080001",
  "gid": "wuxing_persona",
  "createdType": 0,
  "validDateType": 0,
  "validDate": null,
  "describe": "五行人格卡结果 R202606080001"
}
```

响应重点字段：

```json
{
  "data": {
    "gid": "wuxing_persona",
    "originUrl": "https://your-domain.com/result/R202606080001",
    "fullShortUrl": "http://nurl.ink:8003/abc123"
  }
}
```

五行后端在 `POST /api/results` 创建结果后调用该接口，把返回的 `fullShortUrl` 保存到自己的 `user_result` 或 `short_link_binding` 字段里，并返回给前端。

### 3.2 短链访问跳转

短链项目默认入口：

```http
GET /{short-uri}
```

它会：

1. 根据域名和短码组装 `fullShortUrl`。
2. 优先查 Redis `short-link:goto:{fullShortUrl}`。
3. Redis 未命中时查 MySQL `t_link_goto_*` 和 `t_link_*`。
4. 查不到时写空值缓存 `short-link:is-null:goto_{fullShortUrl}`。
5. 查到后记录统计并 302 跳转到原始链接。

五行项目文档要求路径是 `/s/{shortCode}`。需要二选一：

1. 推荐：给短链服务使用独立子域名，例如 `s.your-domain.com/{code}`，避免和 H5 路由冲突。
2. 若必须同域 `/s/{code}`，需要改短链项目 Controller 或 Nginx rewrite，把 `/s/abc123` 转成短链服务的 `/abc123`。

### 3.3 短链统计

单条短链统计：

```http
GET /api/short-link/v1/stats?fullShortUrl=nurl.ink:8003/abc123&gid=wuxing_persona&enableStatus=0&startDate=2026-06-08&endDate=2026-06-08
```

返回包含：

- `pv`
- `uv`
- `uip`
- 按天统计
- 小时统计
- 浏览器、OS、设备、网络、地区等统计

分组统计：

```http
GET /api/short-link/v1/stats/group?gid=wuxing_persona&startDate=2026-06-08&endDate=2026-06-08
```

访问记录：

```http
GET /api/short-link/v1/stats/access-record?fullShortUrl=...&gid=wuxing_persona&enableStatus=0&startDate=...&endDate=...&current=1&size=20
```

五行后台 `/admin` 可以调用这些接口展示短链 PV/UV/UIP 和访问记录。

## 4. 已覆盖 AGENTS 要求的能力

短链项目已覆盖：

- Base62 短码生成：`HashUtil.hashToBase62`
- 短链唯一性：唯一索引 + 布隆过滤器 + 重试
- Redis 解析缓存：`short-link:goto:{fullShortUrl}`
- 无效短链空值缓存：`short-link:is-null:goto_{fullShortUrl}`
- 短链跳转：`ShortLinkService.restoreUrl`
- PV/UV/UIP：访问日志 + Redis set 去重 + 聚合表
- 短链访问记录：`t_link_access_logs`
- 管理统计接口：`/api/short-link/v1/stats*`

这些都能支撑五行人格卡 README 和面试表达里的“短链接接入真实业务”。

## 5. 必须改造或规避的点

### 5.1 路由冲突

短链项目默认 `GET /{short-uri}`，会抢占根路径。五行 H5 也需要 `/`、`/test`、`/result/:resultId`、`/admin`。

建议：

- 生产用独立短链子域名：`https://s.your-domain.com/{code}`。
- 或改短链入口为 `/s/{short-uri}`。

### 5.2 隐私合规

五行项目要求不保存明文 IP，保存 hash；短链项目当前 `t_link_access_logs.ip` 保存明文 IP，UV cookie 也直接保存原值。

建议：

- 将 `remoteAddr` 入库前改为 `sha256(ip + HASH_SALT)`。
- 访问日志返回给五行后台时只展示 hash。
- 如果需要地区统计，需要先确认是否允许调用 IP 地理位置服务；MVP 可以关闭或保留为后续优化。

### 5.3 登录和分组

短链统计接口会校验 `gid` 是否属于当前 `UserContext.username`。五行项目不做登录注册。

建议：

- 使用固定内部系统用户：`wuxing_system`。
- 初始化固定分组：`gid = wuxing_persona`。
- 五行后端内部调用短链服务时带请求头：

```http
username: wuxing_system
userId: wuxing-system
realName: wuxing-system
```

或者在短链项目中增加内部 API token，绕开 SaaS 用户登录体系。

### 5.4 白名单

`project` 模块 dev 配置默认开启跳转域名白名单，只允许少数域名。`aggregation` 配置中白名单默认关闭。

建议：

- 本地调试时关闭 `short-link.goto-domain.white-list.enable`。
- 生产如果开启白名单，把五行主域名加入 `details`。

### 5.5 架构复杂度

原项目依赖 Nacos、Gateway、ShardingSphere、Redisson、Redis Stream、Sentinel。五行 MVP 是单机快速上线，不宜一次性全上。

建议第一版：

- 不接 `gateway`。
- 不接 `console-vue`。
- 优先用 `aggregation` 或单独 `project` 服务。
- 如果时间紧，可以先把短链项目的核心思想移植到五行后端，后续再切成独立短链服务。

## 6. 推荐集成步骤

### 已完成：v0.2 适配层

五行项目当前已经完成：

- `ShortLinkService` 从具体实现改为门面服务。
- `ShortLinkProvider` 定义统一短链接口。
- `InternalShortLinkProvider` 保留 v0.1 内置短链完整能力。
- `ExternalShortLinkProvider` 预留外部短链服务创建链路。
- `ExternalShortLinkClient` 封装 HTTP 调用边界。
- `SHORT_LINK_MODE=internal|external` 支持配置切换。
- `SHORT_LINK_EXTERNAL_FALLBACK_TO_INTERNAL=true` 支持外部服务失败降级。
- 外部返回 `fullShortUrl` 后，五行项目仍保存本地 `short_link` 业务绑定。

这意味着后续不需要再大改 `ResultService` 和 `ShortLinkController`，只需要补外部服务真实联调、鉴权和统计读取。

### 阶段 A：本地跑通短链服务

目标：能用短链项目创建短链并跳转。

动作：

1. 初始化短链 MySQL 表。
2. 启动 Redis。
3. 使用 `aggregation` 模块或 `project` 模块启动服务。
4. 调用 `/api/short-link/v1/create` 创建一个指向五行结果页的短链。
5. 访问短链确认 302 跳转。

### 阶段 B：五行后端接入创建短链

目标：五行结果创建后自动生成短链。

动作：

1. 启动外部短链服务。
2. 设置 `SHORT_LINK_MODE=external`。
3. 设置 `SHORT_LINK_EXTERNAL_BASE_URL`、`SHORT_LINK_EXTERNAL_GROUP_ID` 和内部系统用户 header。
4. 调用五行 `POST /api/results`，确认 `ExternalShortLinkProvider` 创建短链并保存本地绑定。
5. 若外部服务不可用，确认 `fallback-to-internal=true` 时主流程自动回到内置短链。

### 阶段 C：Nginx 整合入口

目标：用户访问五行短链时能跳转结果页。

推荐 Nginx：

```nginx
location /s/ {
    rewrite ^/s/(.*)$ /$1 break;
    proxy_pass http://shortlink:8003;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

更推荐使用子域名：

```nginx
server {
    server_name s.your-domain.com;
    location / {
        proxy_pass http://shortlink:8003;
    }
}
```

### 阶段 D：五行后台接入统计

目标：`/admin` 能看到每条短链 PV/UV/UIP。

动作：

1. 五行后台短链列表读取本地结果表 + 短链绑定。
2. 对每条短链调用独立短链服务的统计接口。
3. 展示 `pv/uv/uip/lastVisitAt`。
4. 明细页调用访问记录接口。

## 7. 建议保留和裁剪

保留：

- 短码生成和冲突处理
- Redis 解析缓存
- 空值缓存
- 布隆过滤器防穿透
- 访问统计聚合表
- PV/UV/UIP 查询接口

裁剪或暂不接入：

- SaaS 用户注册登录
- 原控制台前端
- 回收站
- 批量创建
- Sentinel 限流
- Nacos 服务发现
- Gateway token 鉴权
- IP 地区外部接口
- 多租户复杂分组

## 8. 与五行 MVP 的最终关系

五行项目自己负责：

- 用户测算流程
- 结果生成和结果页
- 五行后台展示
- 结果和短链的业务绑定
- 娱乐声明与隐私合规

短链项目负责：

- 短码生成
- 短链解析
- 302 跳转
- Redis 缓存
- 无效短链空值缓存
- 短链 PV/UV/UIP
- 短链访问日志

最推荐的项目叙述：

> 五行人格卡结果页生成后，业务后端调用独立短链接服务创建分享链接。用户访问短链时，短链服务通过 Redis 优先解析映射，未命中再查 MySQL，命中后跳转回对应结果页，同时异步记录 PV/UV/UIP 和访问日志。五行数据中台聚合展示测算数据与短链访问数据，让短链接能力真正服务于业务分享闭环。

v0.2 面试表达可以进一步升级为：

> 我没有把外部短链服务直接写死到业务流程里，而是先抽象出 `ShortLinkProvider` 适配层。默认内置 Provider 保证项目独立可运行，External Provider 负责对接外部短链服务并保留本地业务绑定；外部服务异常时可以按配置降级。这样既保留 MVP 稳定性，也给后续服务化集成留下清晰边界。
