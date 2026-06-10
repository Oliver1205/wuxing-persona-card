# 五行人格卡 MVP 教学手册

本文档用于复盘本项目第一版 MVP：做了什么、为什么这么做、代码里哪些地方最有学习价值、面试时如何表达。

## 1. 项目要解决什么问题

五行人格卡不是一个单纯的测试页面，而是一个带真实分享链路和统计后台的 H5 微项目。

核心工程问题：

1. 如何把用户输入转成稳定、正向、可复用的结果数据。
2. 如何为每个结果生成可分享短链接。
3. 如何让短链接访问回到同一个结果页。
4. 如何统计 PV、UV、UIP，并在后台展示。
5. 如何在不登录的情况下保护用户隐私。

## 2. 为什么先做单人闭环

第一版不做朋友匹配，是因为短链接和统计本身已经足够形成业务闭环：

```text
用户完成测算 -> 得到结果 -> 复制短链 -> 朋友访问 -> 后台看到访问数据
```

这个闭环成立后，后续任何分享、匹配、增长功能都可以复用短链和统计基础设施。

## 3. 代码分层怎么设计

后端按典型 Spring Boot 分层：

| 层 | 代表代码 | 职责 |
| --- | --- | --- |
| controller | `ResultController`、`ShortLinkController`、`AdminController` | 接收请求、读取 header、返回统一响应 |
| service | `ResultService`、`ShortLinkService`、`VisitEventService` | 承载业务流程 |
| mapper | `UserResultMapper`、`ShortLinkMapper`、`VisitEventMapper` | 数据库访问 |
| entity | `UserResultEntity`、`ShortLinkEntity`、`VisitEventEntity` | 表结构映射 |
| dto / vo | `CreateResultRequest`、`ResultDetailVO`、`AdminOverviewVO` | 入参和出参模型 |
| util | `HashUtils`、`IpUtils`、`JsonUtils` | 通用工具 |

关键原则：Controller 不写计算规则，不直接拼统计 SQL，不做缓存细节。这样面试时可以清楚说明“业务流程在 Service，数据访问在 Mapper”。

## 4. 结果生成怎么做

入口是 `POST /api/results`，核心流程在 `ResultService.create`：

```text
参数校验
  -> ElementCalculateService 计算五行分数
  -> StarOfficerService 根据月份生成星官
  -> ResultTextService 生成关键词和三段文案
  -> 保存 user_result
  -> ShortLinkService 生成短链
  -> 写入结果缓存
  -> 返回 ResultDetailVO
```

### 4.1 五行计算

`ElementCalculateService` 把规则拆成几个来源：

- 初始分：五个元素各 20。
- 年份：`birthYear % 5` 影响一个元素。
- 月份：主元素加 25，辅助元素加 10。
- 日期：可选，按 `birthDay % 5` 加权。
- 时段：可选，按枚举加权。
- 题目：5 道题，每题选择的元素加 12。

最后取分数最高的两个元素，按二者分数重新归一化，得到前端展示的主副比例。

### 4.2 星官和文案

`StarOfficerService` 只根据月份生成星官，避免第一版引入复杂规则。

`ResultTextService` 使用模板化文案，不接 AI。这样做有两个好处：

- 输出可控，符合娱乐化、正向边界。
- 可测试、可复用，不会每次返回不可控长文。

## 5. 短链接为什么先内置

用户提供的外部短链项目已经克隆到：

```text
/Users/linyuxiang/JavaBackend/01_Projects/shortlink
```

它功能很完整，但包含 SaaS 控制台、网关、用户体系、分组、更多中间件。第一版五行 MVP 的目标是快速跑通结果分享，所以先内置短链：

```text
resultId -> shortCode -> /s/{shortCode} -> /result/{resultId}?sc={shortCode}
```

v0.1 内置短链的核心原本在 `ShortLinkService`，v0.2 后迁移到 `InternalShortLinkProvider`：

- 生成 6 位 Base62 短码。
- 检查 MySQL 中是否已存在。
- 同一个 resultId 复用已有短链。
- Redis 缓存 `shortlink:code:{shortCode}`。
- 无效短码缓存 `shortlink:null:{shortCode}`。
- 访问短链时写入事件并更新 PV。

后续接入外部短链服务时，不需要替换上层业务流程，只需要补齐 `ExternalShortLinkProvider` 的真实 HTTP 联调、鉴权和统计读取，五行项目继续保存业务绑定。

### 5.1 v0.2 为什么要做短链适配层

v0.2 没有把外部短链项目直接写死进 `ResultService`，而是把短链模块拆成：

```text
ShortLinkService
  -> ShortLinkProvider
    -> InternalShortLinkProvider
    -> ExternalShortLinkProvider
      -> ExternalShortLinkClient
```

这样做有三个工程价值：

1. **保护 v0.1 闭环**：默认 `internal` 模式继续使用内置短链，创建结果、复制短链、访问 `/s/{code}`、后台统计都不回退。
2. **隔离外部不确定性**：外部短链服务可能没启动、鉴权没接好或网络失败，`ExternalShortLinkProvider` 可以按配置降级到内置 Provider。
3. **保留业务绑定**：即使外部服务负责生成短码，五行项目仍保存 `resultId -> shortCode -> shortUrl`，后台和结果页查询不会完全依赖外部系统。

配置方式：

```text
SHORT_LINK_MODE=internal
SHORT_LINK_MODE=external
SHORT_LINK_EXTERNAL_BASE_URL=http://shortlink:8003
SHORT_LINK_EXTERNAL_FALLBACK_TO_INTERNAL=true
```

面试里可以把这段讲成：从“单体 MVP 内置短链”演进到“可插拔服务适配层”，但没有为了架构而破坏稳定闭环。

### 5.2 v0.3 为什么要做 external 联调准备

v0.2 已经把短链抽成 Provider，但如果只是停在接口抽象，面试里仍然容易被追问：“你真的对接过外部系统吗？”

v0.3 做的是联调前最关键的工程收口：

- 对齐外部短链项目真实路径：`POST /api/short-link/v1/create`。
- 对齐外部项目用户上下文：`username`、`userId`、`realName` 三个 header。
- 对齐创建请求体：`domain`、`originUrl`、`gid`、`createdType`、`validDateType`、`validDate`、`describe`。
- 增加外部调用超时，避免外部服务未启动时拖慢用户提交。
- 保留 `fallback-to-internal=true`，让外部短链不可用时不影响 MVP 主流程。

这里的重点不是“必须立刻把外部服务跑起来”，而是让五行后端的 external 模式已经具备明确的 HTTP 协议边界。后续一旦短链项目的 MySQL、Redis 和 `aggregation` 启动，就可以直接通过配置切换联调。

新增的 `RestExternalShortLinkClientTest` 使用无端口的 `ClientHttpRequestFactory` 捕获请求，验证 URI、header 和 JSON body。这样测试不需要连接公网，也不需要本机额外启动短链服务，但能证明五行侧发出的请求结构是对的。

### 5.3 v0.4 为什么要做服务级联调和统计适配

v0.3 证明了“五行侧会发正确的 HTTP 请求”，但还不能证明外部短链项目真的能接住这条链路。v0.4 补上了这一步：

- 本地启动独立短链项目 `aggregation` 服务。
- 初始化固定系统用户和 `wuxing_persona` 分组。
- 五行后端用 `SHORT_LINK_MODE=external` 创建结果。
- 外部短链服务返回真实 `fullShortUrl`。
- 访问外部短链后 302 到五行结果页。
- 五行后台短链列表读取外部 PV / UV / UIP。

这里的工程重点是“服务职责不混在一起”：

```text
五行项目保存业务绑定和人格结果
外部短链服务负责短码基础设施和访问统计
后台聚合两边数据，展示给运营或开发者
```

v0.4 新增的 `ExternalShortLinkStatsAdapter` 不是直接替换本地统计，而是先判断：

1. 当前是否为 `external` 模式。
2. 是否开启 `SHORT_LINK_EXTERNAL_STATS_ENABLED`。
3. 短链域名是否匹配外部短链服务 domain。
4. 外部统计接口是否成功返回。

只有全部满足时，后台短链列表才使用 external 统计，并把 `statSource` 标记为 `external`。否则继续显示本地统计。这种设计能避免外部系统抖动时影响后台可用性，也方便面试时说明“外部依赖要有降级策略”。

## 6. PV / UV / UIP 怎么统计

前端 `clientId.ts` 首次访问生成匿名 ID：

```text
wuxing_client_id = UUID
```

所有请求通过 `X-Client-Id` 传给后端。

后端 `VisitEventService` 做三件事：

1. 解析 clientId、IP、User-Agent。
2. 使用 `HashUtils.sha256(value + salt)` 脱敏。
3. 写入 `visit_event`。

统计口径：

- PV：事件行数。
- UV：去重 `client_id_hash`。
- UIP：去重 `ip_hash`。

这是项目面试里的高价值点：不用登录，也能做基础行为统计，同时不保存明文访问标识。

## 7. Redis 的真实作用

Redis 不是为了“显得技术栈丰富”，而是有明确使用点：

| Key | 作用 |
| --- | --- |
| `result:{resultId}` | 热门结果页缓存 |
| `shortlink:code:{shortCode}` | 短码解析缓存 |
| `shortlink:null:{shortCode}` | 无效短码空值缓存 |

其中空值缓存很适合面试表达：当有人反复访问不存在的短码时，系统不会每次都打数据库。

## 8. 前端怎么组织

前端按页面和组件拆分：

- 页面：`GuidePage`、`TestPage`、`ResultPage`、`AdminDashboard`、`AdminShortLinkDetail`。
- 组件：`QuestionCard`、`PersonaCard`、`ElementRatioCard`、`ShareLinkBox`、`StatCard`。
- API：`request.ts` 统一注入 `X-Client-Id`，其余文件按业务模块拆分。
- 工具：`tracker.ts` 封装事件上报，`clientId.ts` 管理匿名 ID。

值得注意的是：前端不是只展示假数据，所有核心页面都调用真实后端接口。

## 9. 后台为什么这样做

第一版后台不做复杂账号系统，只使用 `X-Admin-Token`：

- 符合 MVP 快速上线。
- 比完全裸奔安全。
- 后续可以替换为登录系统，不影响统计服务。

`AdminStatService` 聚合：

- 站点总 PV / UV / UIP。
- 首页、开始测试、提交、结果、短链访问等业务指标。
- 日趋势中的 PV、结果生成、短链生成和短链访问。
- 热门五行组合和星官。
- 最近结果、最近短链、短链列表和访问日志。

v0.3 又补了日期筛选：

- 总览接口支持 `startDate/endDate`。
- 短链列表按短链创建日期筛选。
- 单条短链访问日志按 `SHORT_LINK_VISIT` 事件时间筛选。

这样做的价值是：项目一旦上线，不同日期的访问数据才能被分析；后续接外部短链统计接口时，也能按同一个时间窗口对齐五行本地数据和短链服务数据。

v0.4 又把这个时间窗口传给外部短链统计接口。外部短链服务用时间边界查询访问日志，所以五行侧会把 `endDate` 向后补一天，确保选择某一天时能覆盖当天完整访问数据。后台列表新增 `statSource` 字段，能看出 PV / UV / UIP 来自本地还是外部服务。

v0.8 增加了后台日趋势和最近记录展示，但刻意没有做复杂 BI 大屏。原因是 MVP 阶段更需要清楚、稳定、可解释的数据：最近 7 天默认趋势，筛选范围最多 14 天，指标来自现有 `visit_event`、`user_result` 和 `short_link` 表。这样既能回答“最近访问有没有变化”，又不会提前引入统计宽表、图表库或消息队列。

v0.9 做的是上线前审计式加固：后台短链详情也复用短码 Base62 校验，避免非法短码进入查询；`visit_event.referer` 入库前去掉 query 和 fragment，减少分享参数或临时 token 留存；external 访问记录 `records=null` 时返回空列表，区分“外部正常但无数据”和“外部接口失败需要回退”。

v1.0 的重点不是再做功能，而是稳定版收口：README 作为 GitHub 仓库主页，发布检查表覆盖主链路、短链能力、隐私、部署和质量门禁，质量评分说明已验证项和剩余风险。这样项目可以作为一条完整 MVP 基线交付，也方便后续按 v1.1 / v2.0 继续扩展。

v1.1 的重点是 external 模式生产接入准备。v0.4 和 v0.5 已经证明五行项目可以调用外部短链服务创建短链、读取 PV / UV / UIP 和访问明细，但真正上线还会遇到配置、domain、系统用户、外部服务可达性、短链入口路由和隐私审计问题。因此 v1.1 不继续堆业务功能，而是补齐：

- external 模式 Compose overlay。
- external 环境样例。
- 部署前预检脚本。
- 创建结果和短链跳转 smoke 脚本。
- 外部错误码、空数据、短码冲突等失败场景测试。
- 对接说明和隐私审计报告。

这类工作很适合面试表达：外部系统接入不是“把 HTTP URL 写进代码”，还需要环境隔离、超时、降级、可观测的验证脚本和隐私边界。五行项目继续保留 internal 模式，就是为了在外部服务没启动或生产配置出错时不让核心测算闭环直接中断。

v1.2-v1.4 的重点是生产质量增强，而不是扩大产品功能边界：

- GitHub Actions 让每次 PR 都自动跑本地同款质量门禁。
- Docker smoke 脚本让部署后的主链路验证变成可重复命令。
- external 运行态状态接口让后台能看到 external 配置、统计开关、fallback 和可选连通性探测。
- 后台短链筛选和 CSV 导出解决上线初期排查问题。
- 安全响应头、后台 token 常量时间比较和 CSV 公式注入防护补齐基础安全细节。
- Testcontainers profile 用真实 MySQL schema 验证主链路，减少 H2 与 MySQL 行为差异。
- 分享图生成用浏览器 Canvas 实现，不引入新依赖，继续围绕结果分享闭环服务。

面试里可以这样讲：我没有在 v1 后立刻加登录、付费或 AI，而是先把项目从“能演示”推进到“更像真实工程”。CI/CD、smoke test、运行态治理、可选容器集成测试和后台排查工具，都是上线后能直接降低风险的工作。

## 10. 测试怎么覆盖

当前测试分两类：

1. 单元测试：五行计算、星官生成。
2. 短链专项单元测试：短链复用、短码冲突重试、空值缓存、计数更新。
3. Redis 缓存专项测试：结果缓存序列化、短链 key、空值 key、TTL、异常降级。
4. 集成测试：`MvpFlowIntegrationTest` 使用 H2 和 MockMvc 跑主流程。

集成测试覆盖：

```text
POST /api/results
GET /api/results/{resultId}
GET /s/{shortCode}
GET /api/admin/overview
GET /api/admin/short-links
GET /api/admin/short-links/{shortCode}/visits
```

边界测试覆盖：

- 出生月份非法时返回 400。
- 5 道题出现重复题号时返回业务错误。
- 前端事件类型非法时返回 400。
- 后台不带 token 时返回 401。
- 不存在的 6 位短码跳转 `/not-found`。
- 格式非法的短码返回 400。
- 同一个 resultId 复用已有短链，不重复创建。
- 短码随机冲突时会重试并保存新短码。
- 无效短码数据库未命中时写入空值缓存。
- 短链访问后按事件表回算 PV、UV、UIP 并更新 `short_link`。
- Redis 缓存层异常时降级为 miss，不影响主业务。

这比只测 Service 更有价值，因为它验证了 Controller、Service、Mapper、SQL、事件统计之间的协作。

本轮补测还发现并修复了一个真实口径问题：短链访问详情原本会把 `SHORT_LINK_CREATED`、`RESULT_CREATED`、`TEST_SUBMIT` 等携带 shortCode 的业务事件也算进去。修复后，`VisitEventMapper.listByShortCode` 和 `countByShortCode` 只统计 `SHORT_LINK_VISIT`，让“短链访问日志”语义更准确。

浏览器验收又发现一个真实部署细节：`HttpServletResponse.sendRedirect("/result/...")` 在真实 Servlet 容器中会输出带后端 Host 的绝对 Location。代理部署时这可能把用户带到后端端口。修复后，`ShortLinkController` 手动设置 `302` 和相对 `Location`，让短链访问稳定跳回同域 H5 路由。

## 11. 当前验证结果

已通过：

```bash
cd backend && mvn -q test
cd frontend && npm run build
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```

Docker 容器验收已通过。由于本机访问 Docker Hub 时曾出现匿名 token 超时，Dockerfile 已支持通过环境变量切换基础镜像源；本次验收使用 `http://127.0.0.1:8088` 作为本地入口：

```bash
APP_BASE_URL=http://localhost:8088 \
NGINX_HTTP_PORT=8088 \
BACKEND_MAVEN_IMAGE=docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-17 \
BACKEND_RUNTIME_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:17-jre \
FRONTEND_NODE_IMAGE=docker.m.daocloud.io/library/node:20-alpine \
FRONTEND_NGINX_IMAGE=docker.m.daocloud.io/library/nginx:1.27-alpine \
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml up --build -d
```

本地浏览器验收：

```bash
cd backend
APP_BASE_URL=http://127.0.0.1:4173 mvn spring-boot:run -Dspring-boot.run.profiles=local

cd frontend
npm run build
npm run preview -- --host 127.0.0.1 --port 4173
```

已验证页面：

- 首页进入测试页。
- 测试页提交出生年月和 5 道题。
- 结果页展示五行比例、星官、关键词、解读和短链。
- 短链后端入口返回相对路径 302。
- 后台总览展示 PV、UV、UIP、提交、结果、短链访问。
- 短链详情展示 hash 后的访问日志。

验收截图：

- `docs/screenshots/local-result-page.png`
- `docs/screenshots/local-admin-overview.png`
- `docs/screenshots/local-shortlink-detail.png`
- `docs/screenshots/docker-home-page.png`
- `docs/screenshots/docker-result-page.png`
- `docs/screenshots/docker-admin-token-gate.png`
- `docs/screenshots/docker-admin-detail-protected.png`

Docker 入口实际验证样例：

```text
resultId: R20260609005159599703
shortCode: 4fB7av
shortUrl: http://localhost:8088/s/4fB7av
short link Location: /result/R20260609005159599703?sc=4fB7av
admin short link PV/UV/UIP: 1/1/1
```

v0.2 新增验证：

- 默认 `internal` 模式走内置 Provider。
- `external` 模式走外部 Provider。
- 外部短链创建成功时保存本地业务绑定。
- 外部短链创建失败且允许降级时回到内置 Provider。
- 外部短链创建失败且禁止降级时返回明确业务错误。
- `mvn test` 覆盖 Provider 切换和外部失败分支。

v0.3 新增验证：

- external RestClient 请求路径、系统用户 header 和 JSON body。
- external 创建请求中的 `domain` 字段。
- 后台日期筛选：当天有数据、未来日期为空。
- 非法日期范围返回 400。

v0.4 新增验证：

- external RestClient 统计接口路径、查询参数和系统用户 header。
- `ExternalShortLinkStatsAdapter` 在 external stats 开启且 domain 匹配时读取外部统计。
- 外部统计接口失败时回退本地统计。
- internal 模式和 domain 不匹配时不调用外部统计。
- 本地服务级联调覆盖外部短链创建、外部 302、五行本地业务绑定和后台 `statSource=external`。

## 12. 面试表达重点

可以这样讲：

1. 这是一个从 0 到 1 的 H5 微项目，不只是 CRUD。
2. 短链接接入真实业务结果页，每个结果都有可分享入口。
3. 短链支持生成、解析、302 跳转、Redis 缓存和空值缓存。
4. 访问统计按 PV、UV、UIP 拆分，并在后台展示。
5. 项目不做登录，通过匿名 clientId 和 hash 方式兼顾统计和隐私。
6. 文案模板化，保证输出正向、可控、可测试。
7. Docker Compose 把 MySQL、Redis、后端、Nginx 组织成单机部署方案，并已跑通过容器验收。
8. v0.2 把短链模块抽象成 Provider 适配层，体现从 MVP 到可服务化集成的演进能力。
9. v0.3 把 external 模式推进到可联调状态，并用日期筛选增强后台数据分析能力。
10. v0.4 完成 external 服务级联调，并让后台短链列表能够读取外部 PV、UV、UIP。
11. v0.5 接入 external 访问明细，并对外部 IP / user 做 hash 后再展示。
12. v0.8 增强后台运营可读性，用轻量日趋势和最近记录解释上线初期数据。
13. v0.9 做稳定性和隐私审计，收敛 Referer 参数、统一短码校验，并补足 external 空记录场景。
14. v1.0 收口 README、发布检查表和质量评分，把项目沉淀成可展示、可部署、可面试讲解的稳定版。
15. v2.0 从“功能闭环”升级到“产品传播闭环”，围绕首页转化、答题完成率、结果身份感、分享动作和短链回流设计增长漏斗。

## 13. 为什么 v2.0 先做产品化闭环

MVP 已经证明用户可以完成测算、拿到结果、复制短链、通过短链回流并在后台看到统计。下一阶段如果直接做登录、付费、朋友匹配或 AI 解读，容易把系统复杂度拉高，却不一定提升真实传播。

v2.0 先做产品化闭环，是因为测评类产品最核心的增长路径通常不是“功能很多”，而是：

```text
首屏愿意点
  -> 题目愿意答完
  -> 结果愿意相信和截图
  -> 链接愿意分享
  -> 朋友点进来也愿意测试
```

因此本阶段的工程重点是把这些动作转成可观察数据：

- `TEST_FORM_START` 看用户是否真正开始填写。
- `QUESTION_ANSWER_SELECT` 看答题过程是否有流失点。
- `TEST_SUBMIT_ATTEMPT` 区分“想提交”和“后端成功创建结果”。
- `SHARE_PANEL_VIEW`、`SHORT_LINK_COPY`、`SAVE_SHARE_IMAGE_SUCCESS`、`NATIVE_SHARE_SUCCESS` 看用户是否真的愿意传播。
- `SHARED_RESULT_CTA_CLICK` 看朋友从分享结果页继续测试的意愿。

这套设计可以在面试中体现三个能力：产品判断、数据意识和后端可演进架构。它不是脱离代码的空文档，而是已经和前端埋点、后端事件枚举、结果页分享能力一起落地。

本轮浏览器验收还抓到一个构建阶段发现不了的问题：Vite 代理原本使用 `/s`，会把 `/src/main.ts` 也误代理到后端，导致开发环境白屏。修复为 `^/s/` 后，既保留短链代理能力，又避免前端源码路径被拦截。这说明商业级改版不能只看 `npm run build`，还需要真实浏览器主流程验收。

## 14. 后续学习建议

优先读代码顺序：

1. `ResultService`：理解主业务流程。
2. `ElementCalculateService`：理解规则如何工程化。
3. `ShortLinkService`：理解短码、缓存、跳转。
4. `service/shortlink/InternalShortLinkProvider`：理解内置短链如何作为默认 Provider。
5. `service/shortlink/ExternalShortLinkProvider`：理解外部服务接入、业务绑定和失败降级。
6. `service/shortlink/RestExternalShortLinkClient`：理解真实 HTTP client 如何对齐外部服务协议。
7. `service/shortlink/ExternalShortLinkStatsAdapter`：理解外部统计读取、domain 防误调和失败回退。
8. `VisitEventService`：理解 PV/UV/UIP 和隐私。
9. `AdminStatService`：理解后台聚合、日期筛选和统计来源切换。
10. `frontend/src/api/request.ts` 和 `tracker.ts`：理解前端如何把匿名 ID 和埋点接入后端。
