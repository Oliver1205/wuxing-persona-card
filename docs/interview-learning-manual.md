# 五行人格卡面试学习手册

## 1. 项目定位

五行人格卡是一个以“测算结果页分享”为真实业务场景的 Java 全栈项目。你面试时不要只说“我做了一个人格测试页面”，而要说清楚：

```text
匿名用户测算 -> 结果生成 -> 专属短链 -> 朋友访问 -> 访问事件 -> 后台数据中台
```

这个项目适合后端面试的原因是：它有真实业务闭环、短链系统、Redis 缓存、MySQL 统计查询、匿名隐私设计、Docker Compose 部署和可解释的性能取舍。

## 2. 系统架构

![五行人格卡架构与热路径图](assets/wuxing-architecture-map.svg)

```mermaid
flowchart LR
  Browser["移动端浏览器"] --> Nginx["Nginx 入口"]
  Nginx --> Frontend["Vue H5"]
  Nginx --> Api["Spring Boot API"]
  Nginx --> Short["/s/{shortCode} 短链入口"]
  Api --> MySQL["MySQL: 结果/短链/事件"]
  Api --> Redis["Redis: 结果缓存/短码映射/空值缓存"]
  Api --> Provider["ShortLinkProvider"]
  Provider --> Internal["Internal Provider"]
  Provider --> External["External Provider"]
```

各层职责：

- Vue H5：完成首页、测试页、结果页、分享动作和后台页面展示。
- Nginx：托管静态资源，转发 `/api/**` 和 `/s/**`，承担第一层限流。
- Spring Boot：处理结果生成、短链创建/解析、事件记录、后台统计。
- MySQL：保存 `user_result`、`short_link`、`visit_event` 和日聚合表。
- Redis：缓存结果详情、短码到 resultId 映射、无效短码空值。

## 3. 核心链路

### 创建结果链路

```mermaid
sequenceDiagram
  participant U as User
  participant F as Vue H5
  participant R as ResultService
  participant S as ShortLinkService
  participant DB as MySQL
  participant C as Redis

  U->>F: 填出生信息和 5 道题
  F->>R: POST /api/results
  R->>R: 计算五行分数和文案
  R->>DB: insert user_result
  R->>S: createForResult(resultId)
  S->>DB: insert/select short_link
  S->>C: cache shortCode -> resultId
  R->>DB: write TEST_SUBMIT / RESULT_CREATED / SHORT_LINK_CREATED
  R->>C: cache result detail
  R-->>F: resultId + shortUrl + 人格结果
```

面试要点：

- 创建结果是强业务链路，结果和短链要尽量一起成功。
- external 短链失败时可以降级 internal，保证用户仍能拿到可访问结果。
- 一次提交会放大成多次 DB 写，所以要控制事务边界和外部调用风险。

### 结果读取链路

```text
GET /api/results/{resultId}
  -> Redis result:{resultId}
  -> 未命中查 user_result + short_link
  -> 写 RESULT_VIEW 事件
  -> 返回结果页数据
```

面试要点：

- 结果详情是读多写少，适合缓存。
- 事件记录失败不应该影响用户查看结果。
- 如果 Redis 不可用，主流程仍可回源 MySQL。

### 短链跳转链路

```mermaid
sequenceDiagram
  participant B as Browser
  participant N as Nginx
  participant P as InternalShortLinkProvider
  participant C as Redis
  participant DB as MySQL

  B->>N: GET /s/{shortCode}
  N->>P: resolveAndRecord
  P->>P: validate shortCode
  P->>C: read shortlink:code:{shortCode}
  alt cache miss
    P->>DB: select short_link
    P->>C: cache shortCode -> resultId
  end
  P->>DB: insert SHORT_LINK_VISIT event
  P->>DB: touch last_visit_at
  P-->>B: 302 /result/{resultId}?sc={shortCode}
```

面试要点：

- 短链是传播峰值入口，必须低延迟。
- 当前优化点是：跳转请求不再同步做 PV/UV/UIP 三次实时聚合，只记录事件和轻量更新时间。
- 后台统计仍可从事件表或日聚合表计算，用户跳转链路不被统计查询拖慢。

## 4. 数据模型

```mermaid
erDiagram
  USER_RESULT ||--|| SHORT_LINK : "result_id"
  SHORT_LINK ||--o{ VISIT_EVENT : "short_code"
  SITE_DAILY_METRIC ||--o{ VISIT_EVENT : "event_date"
  SHORT_LINK_DAILY_METRIC ||--o{ VISIT_EVENT : "short_code + date"
```

核心表：

- `user_result`：保存出生信息、答案、五行结果、星官、关键词和文案。
- `short_link`：保存 shortCode、resultId、shortUrl、累计计数和最后访问时间。
- `visit_event`：保存匿名事件，包括 eventType、resultId、shortCode、clientIdHash、ipHash、channel、campaign、deviceType。
- `site_daily_metric` / `short_link_daily_metric`：用于把明细事件聚合成更适合后台查询的数据。

## 5. 缓存设计

| 缓存 | Key | Value | 作用 |
| --- | --- | --- | --- |
| 结果详情 | `result:{resultId}` | ResultDetailVO JSON | 减少结果页重复查库 |
| 短码映射 | `shortlink:code:{shortCode}` | resultId | 降低短链跳转延迟 |
| 空短码 | `shortlink:null:{shortCode}` | `1` | 防止不存在短码反复打 DB |

缓存不是替代数据库，而是削峰。面试时要讲清楚 Redis 异常时会回源 MySQL，并且空值缓存 TTL 较短，避免长期误伤后续合法短码。

## 6. 性能与抗峰值设计

当前最重要的性能取舍：

1. 用户核心链路优先：创建结果、查看结果、短链跳转要保成功率。
2. 统计类查询后置：后台 PV/UV/UIP 不应该阻塞短链 302。
3. Redis 承担读热点：结果和短链映射都适合缓存。
4. Nginx 先挡一层：`/api/**`、`/api/events`、`/s/**` 可以有不同限流策略。
5. Admin overview 可以接受 45 秒级短缓存，避免运营刷新反复触发 live 聚合查询。
6. 统计表补齐 `created_at`、`status + created_at`、`event_type + created_at + short_code` 等索引，让后台日期筛选、短链列表和日聚合更容易走索引。
7. 后台短链列表要避免 N+1 查询：一页短链先批量拉结果信息，再按短码集合一次聚合 PV/UV/UIP。
8. 单机阶段不急着上 MQ、分库分表、ES：先把热点查询、缓存和降级做好。

高峰场景回答模板：

> 如果某张人格卡在群里突然传播，最大压力会落在 `/s/{shortCode}`。我的处理是短码解析优先走 Redis，未命中才查 MySQL；访问事件保留用于统计，但跳转链路不再同步做三次 `COUNT DISTINCT`，只轻量更新最后访问时间。后台统计可以走事件表或日聚合表，这样用户 302 延迟不会被运营查询拖慢。

短链跳转写放大的回答可以这样补充：

> 访问事件每次都写，因为这是统计明细的事实来源；但 `short_link.last_visit_at` 只是列表展示字段，不需要每次跳转都同步刷新。我把它改成至少间隔 30 秒才更新一次，减少同一热门短码在瞬时传播时对 `short_link` 行的反复写压力。

Admin overview 的回答可以这样补充：

> 后台总览不是用户核心链路，不需要秒级强实时。我给相同日期范围的 overview 加了 45 秒 Redis 短缓存，命中时直接返回，未命中再查事件表和聚合表；Redis 异常时退回 live 计算。这个取舍能减少运营刷新对主库的重复压力，同时不改变事件明细的真实性。

数据库索引可以这样补充：

> 我没有只靠 Redis 掩盖慢查询。对于后台总览和聚合任务，`visit_event` 补了按时间范围、时间 + client/ip 去重、事件类型 + 时间 + 短码的组合索引；`short_link` 补了 `status + created_at` 索引。这样即使缓存失效，常见日期筛选和短链列表也更容易落在可控查询路径上。

后台短链列表可以这样补充：

> 列表页不能按每条短链再查一次结果、三次统计，否则 pageSize=100 时会放大成几百次查询。我把结果信息改成按 resultId 集合批量读取，把 PV/UV/UIP 改成按 shortCode 集合一次 `GROUP BY` 聚合，再在 Java 里映射回列表项。这样接口语义不变，但数据库往返和重复 distinct 统计会少很多。

## 7. 安全与隐私

- 不做登录注册，不收集昵称和性别。
- clientId、IP、User-Agent hash 后入库。
- Referer 入库前去掉 query 和 fragment，避免泄露参数。
- 后台接口要求 `X-Admin-Token`。
- 安全响应头由后端和 Nginx 配置协同提供。
- 文案只做娱乐性人格解读，不做宿命、财富、疾病、婚恋判断。

## 8. 部署链路

```mermaid
flowchart TB
  User["用户浏览器"] --> Nginx["Nginx: 80/443"]
  Nginx --> Static["Vue dist 静态资源"]
  Nginx --> Backend["Spring Boot backend: 8080"]
  Backend --> MySQL["MySQL private network"]
  Backend --> Redis["Redis private network"]
```

常用命令：

```bash
cp deploy/.env.example deploy/.env
scripts/deploy-preflight.sh deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
BASE_URL=http://127.0.0.1:8088 ADMIN_TOKEN=dev-token scripts/docker-smoke-test.sh
BASE_URL=http://127.0.0.1:8088 ADMIN_TOKEN=dev-token SHORTLINK_HITS=30 scripts/performance-smoke-test.sh
```

面试要点：

- 不能直接暴露 Spring Boot 8080 到公网，公网入口应走 Nginx。
- MySQL 和 Redis 不暴露公网。
- 上线前必须替换 `ADMIN_TOKEN`、`HASH_SALT`、数据库密码和 `APP_BASE_URL`。
- 性能 smoke 不是压测报告，而是回归检查：它会创建一个真实结果，连续访问短链，并重复读取后台总览，用输出的 `shortlinkAvgMs` 和 `adminAvgMs` 观察热点链路是否明显退化。

## 9. 面试追问速答

| 问题 | 回答方向 |
| --- | --- |
| 为什么要短链 Provider？ | 隔离 internal 和 external 实现，上层结果生成不关心短链来源 |
| external 挂了怎么办？ | 可配置降级 internal，结果页仍可生成可访问短链 |
| 为什么要存本地 short_link？ | 绑定五行业务 resultId，支持后台统计和兼容跳转 |
| PV/UV/UIP 怎么算？ | PV 是事件数，UV 是 clientIdHash 去重，UIP 是 ipHash 去重 |
| 为什么 hash IP？ | 满足匿名统计，减少敏感信息落库 |
| Redis 挂了怎么办？ | 缓存读取失败返回 null，回源 DB；写缓存失败记录 warn 不阻断主流程 |
| 高峰短链怎么抗？ | Redis 映射、Nginx 限流、事件明细后置统计、避免跳转时实时 distinct 聚合 |
| 后台数据为什么能缓存？ | 后台总览用于运营观察，45 秒短缓存能削峰，明细和日聚合仍是权威来源 |
| 后台短链列表为什么不慢？ | 分页先拿短链，再批量取 result 信息和按 shortCode 聚合统计，避免一页触发 N+1 查询 |
| 当前项目边界？ | 单机商业化作品，不是大型分布式平台；后续才考虑 MQ、分库分表、多租户 |

## 10. 面试讲解图谱

### 一分钟总图

```mermaid
flowchart LR
  U["匿名用户"] --> T["5 道题 + 出生年月"]
  T --> R["ResultService 生成人格卡"]
  R --> S["ShortLinkService 绑定短码"]
  S --> V["朋友访问 /s/{code}"]
  V --> E["visit_event 明细事件"]
  E --> A["Admin 数据中台"]
```

这张图用于开场。你要先说清楚它不是单纯页面，而是“测算结果页分享”这个真实业务闭环。

### 热点链路图

```mermaid
flowchart TB
  Share["群聊里短链突然传播"] --> Nginx["Nginx 限流入口"]
  Nginx --> Cache{"Redis 有短码映射?"}
  Cache -- yes --> Redirect["直接 302 到结果页"]
  Cache -- no --> DB["MySQL 查 short_link"]
  DB --> Fill["回填 Redis"]
  Fill --> Redirect
  Redirect --> Event["写 SHORT_LINK_VISIT 明细"]
  Event --> Admin["后台异步/聚合口径查看"]
```

这张图用于回答高并发。核心句子是：短链跳转先保证 302 低延迟，统计查询不阻塞用户跳转。

### 后台统计图

```mermaid
flowchart LR
  Event["visit_event 明细"] --> Live["按范围 live 查询"]
  Event --> Daily["site_daily_metric / short_link_daily_metric"]
  Live --> Overview["Admin overview"]
  Daily --> Overview
  Overview --> Redis["45 秒 overview cache"]
```

这张图用于解释为什么后台可以缓存：后台看趋势，不是金融交易，不需要每次刷新都实时重算。

## 11. 代码阅读路线

按这个顺序读代码，能最快形成面试表达：

| 顺序 | 文件/模块 | 你要看懂什么 |
| --- | --- | --- |
| 1 | `frontend/src/pages/TestPage.vue` | 卡片式问答、出生信息、自动前进、提交入口 |
| 2 | `frontend/src/pages/ResultPage.vue` | 分享结果页、朋友回流入口、保存分享图 |
| 3 | `backend/.../ResultService.java` | 结果生成、短链创建、事件写入、缓存写入 |
| 4 | `backend/.../ShortLinkService.java` | 短链门面和 Provider 调用边界 |
| 5 | `backend/.../InternalShortLinkProvider.java` | 短码解析、Redis 缓存、302 跳转热路径 |
| 6 | `backend/.../AdminStatService.java` | 后台总览、日期范围、overview 短缓存 |
| 7 | `backend/.../RedisCacheService.java` | 结果缓存、短码缓存、空值缓存、降级 |
| 8 | `scripts/performance-smoke-test.sh` | 如何用脚本证明短链热点链路没有明显退化 |

前端体验追问可以这样回答：

> 答题页的核心不是把按钮做多，而是降低犹豫和误操作。出生年份如果界面已经显示默认值，就应该同步作为有效选择；选中答案后不立刻硬切页面，而是给一个短确认反馈再自动进入下一题；移动端底部 sticky 操作条要留出安全区，不能挡住最后一个选项。

## 12. 五分钟面试讲解稿

### 0-60 秒：先讲业务闭环

> 这个项目叫五行人格卡，是一个以结果页分享为核心场景的 Java 全栈项目。用户完成出生信息和 5 道题后，后端生成一张人格结果卡，并绑定专属短链接。朋友打开短链会回到同一张结果页，后台能看到 PV、UV、UIP、渠道、活动和短链访问明细。所以它不是单纯 H5 页面，而是“测算、分享、回流、统计”的完整闭环。

### 60-150 秒：讲核心架构

> 前端是 Vue H5，负责首页、卡片式答题、结果页和后台展示；公网入口走 Nginx，转发静态资源、`/api/**` 和 `/s/**`；后端是 Spring Boot，核心模块包括 `ResultService`、`ShortLinkService`、`ShortLinkProvider`、`VisitEventService` 和 `AdminStatService`；MySQL 保存结果、短链和访问事件，Redis 缓存结果详情、短码映射和无效短码。

### 150-240 秒：讲高峰值取舍

> 这个项目最容易被追问的是短链传播高峰。我的设计是让 `/s/{shortCode}` 跳转尽量轻：短码解析优先查 Redis，未命中才查 MySQL；访问事件作为事实明细写入，但不在跳转链路实时做 PV/UV/UIP 聚合；`last_visit_at` 这种展示字段也做低频更新，避免同一个热门短码把一行反复打热。后台统计可以走 live 查询、日聚合和 45 秒 overview 缓存。

### 240-300 秒：讲隐私、验证和边界

> 项目刻意不做登录注册，也不收集昵称和性别；clientId、IP、User-Agent 都 hash 后入库，Referer 去掉 query 和 fragment。验证上有后端集成测试、前端构建、Compose 配置检查、性能 smoke、截图流程和统一 `scripts/quality-check.sh`。当前边界是单机商业化作品，不是大型分布式平台；如果真实流量继续上升，下一步才考虑消息队列、异步写事件、多实例、告警和更完整的权限系统。

### 面试官压迫式追问

| 追问 | 不慌的回答 |
| --- | --- |
| 你这是不是玩具项目？ | 我用人格测试做轻产品入口，但工程闭环是真实的：短链、访问事件、后台统计、缓存、部署和质量门禁都有落地。 |
| 为什么不用 MQ？ | 当前单机阶段先优化热路径、索引、缓存和降级，避免过早复杂化；MQ 是流量继续放大后的下一步。 |
| 统计会不会拖慢跳转？ | 跳转链路只做短码解析、事件写入和轻量字段更新，不同步做 distinct 聚合，后台统计走独立查询路径。 |
| Redis 挂了是不是全挂？ | 不会。结果和短码缓存读取失败会回源 MySQL，写缓存失败只记录 warn，不阻断用户核心链路。 |
| UV 准不准？ | 匿名项目只能做到相对可信：优先 clientId hash，缺失时用 IP/User-Agent 兜底；它适合运营观察，不等同登录用户数。 |

## 13. 8 小时学习路线

| 时间 | 学习目标 |
| --- | --- |
| 第 1 小时 | 跑通业务闭环：首页、测试、结果、短链、后台 |
| 第 2 小时 | 读 `ResultService`，讲清创建结果链路 |
| 第 3 小时 | 读 `ShortLinkService` 和 Provider，讲清 internal/external |
| 第 4 小时 | 读 `VisitEventService` 和 mapper，讲清 PV/UV/UIP |
| 第 5 小时 | 读 `RedisCacheService`，讲清缓存和降级 |
| 第 6 小时 | 读 Nginx/Compose，讲清部署拓扑 |
| 第 7 小时 | 用 Mermaid 图复述核心链路 |
| 第 8 小时 | 按面试追问表做口头演练 |
