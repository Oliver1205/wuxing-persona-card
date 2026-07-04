# API 说明

当前状态：第一版 MVP 已实现以下接口。统一响应结构为：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误响应示例：

```json
{
  "code": 400,
  "message": "birthMonth must be between 1 and 12",
  "data": null
}
```

## 1. 健康检查

```http
GET /api/health
```

返回服务状态和当前时间。

## 1.1 就绪检查

```http
GET /api/readiness
```

返回服务是否具备处理核心业务请求的基础就绪状态。当前 `scope` 为 `core_schema`，表示它会检查 `user_result`、`short_link`、`visit_event`、`site_daily_metric`、`short_link_daily_metric` 等核心表是否可查询；Redis、RocketMQ 和访问事件运行态要继续看后台 runtime 接口。

正常时返回 HTTP `200`：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "UP",
    "scope": "core_schema",
    "service": "wuxing-persona-backend",
    "time": "2026-06-14T07:00:00+08:00",
    "coreTables": {
      "user_result": "ok",
      "short_link": "ok",
      "visit_event": "ok",
      "site_daily_metric": "ok",
      "short_link_daily_metric": "ok"
    }
  }
}
```

如果核心表不可用，返回 HTTP `503`，`data.status` 为 `DOWN`，对应表标记为 `unavailable`。生产 smoke、域名绑定预检和压测脚本会用这个接口拦截“进程还活着但数据库 schema 不可用”的情况。

## 2. 获取题目配置

```http
GET /api/questions
```

返回 5 道价值取向题，每题 5 个选项，选项 code 对应五行元素。前端答题卡、五行图例和结果分布都依赖这组稳定枚举：

| elementCode / optionCode | elementName | 前端图例关键词 |
| --- | --- | --- |
| `METAL` | 金 | 收束 / 淬炼 / 清醒 |
| `WOOD` | 木 | 生长 / 舒展 / 灵动 |
| `WATER` | 水 | 润泽 / 沉静 / 蓄藏 |
| `FIRE` | 火 | 热烈 / 上扬 / 明朗 |
| `EARTH` | 土 | 承载 / 滋养 / 收成 |

单题结构示例：

```json
{
  "questionCode": "Q1",
  "title": "做决定时，你更看重什么？",
  "options": [
    {
      "optionCode": "METAL",
      "optionText": "标准、边界和清晰判断",
      "elementCode": "METAL",
      "elementName": "金"
    }
  ]
}
```

前端排序可以调整展示顺序，但提交时必须回传后端给出的 `questionCode` 和 `optionCode`，不要回传 A/B/C/D 这类纯展示序号。

## 3. 创建测算结果

```http
POST /api/results
Content-Type: application/json
X-Client-Id: <client-id>
X-Session-Id: <session-id>
X-Channel: <channel>
X-Campaign: <campaign>
```

请求体：

```json
{
  "birthYear": 2002,
  "birthMonth": 8,
  "birthDay": null,
  "birthTimeRange": null,
  "answers": [
    {"questionCode": "Q1", "optionCode": "METAL"},
    {"questionCode": "Q2", "optionCode": "WATER"},
    {"questionCode": "Q3", "optionCode": "METAL"},
    {"questionCode": "Q4", "optionCode": "EARTH"},
    {"questionCode": "Q5", "optionCode": "FIRE"}
  ]
}
```

后端行为：

- 校验出生年月、可选日期、可选时段、5 道题答案。
  - `birthYear` 必须在 `1950-2026` 范围内，并且不允许超过当前年份。
  - 当前年份下，`birthMonth` 不允许超过当前月份。
  - 如果传入 `birthDay`，后端会校验它必须是真实日历日期，例如非闰年 `2 月 29 日` 会被拒绝。
- 计算五行主副比例。
- 生成 120 人格分流 id、星官、关键词、日主依据、主从关系、点睛元素、天人特质和成长建议。
- 保存 `user_result`。
- 生成内置短链接并保存 `short_link`。
- 写入 `TEST_SUBMIT`、`RESULT_CREATED`、`SHORT_LINK_CREATED` 事件。

### 3.1 答题状态机契约

测算提交流程现在拆成两层状态机，避免把“页面按钮能不能点”和“后端能不能生成结果”混在一起：

| 层级 | 位置 | 负责内容 | 不负责内容 |
| --- | --- | --- | --- |
| 前端交互状态机 | `frontend/src/utils/testFlowMachine.ts` | 出生信息页、当前题号、左下按钮文案、右下按钮文案、禁用态、浏览器返回上一题 | 不计算五行、不保存结果、不判断真实日期 |
| 后端提交流状态机 | `TestFlowStateMachine` | 判断提交请求是否具备出生年月和 5 道有效题号答案，给出缺失题号 | 不决定选项五行是否合法、不计算分数、不写数据库 |
| 五行计算服务 | `ElementCalculateService` | 校验年份、月份、日期、时辰、选项枚举，并计算五行分数 | 不处理页面步骤、不决定按钮文案 |

前端状态机的产品规则：

- 出生信息页右下按钮：未选年月时显示“选择月份后继续”，选完后显示“进入第 1 题”。
- 五题答题页左下按钮：第 1 题为“基础信息”，第 2-5 题为“上一题”。
- 五题答题页右下按钮：第 1-4 题为“下一题”，第 5 题为“生成我的人格卡”或“生成双人匹配”。
- 浏览器返回键在答题中优先回到上一题；如果用户停在出生信息页，则按原始浏览器历史返回，通常会回到首页。
- 未选择当前题答案时，右下主按钮保持禁用，避免用户跳过关键问题。

后端状态机的提交边界：

- `birthYear` 和 `birthMonth` 缺失时，请求停在 `BIRTH_REQUIRED`。
- 题号会按 `trim + upper-case` 规范化；正常 API 入口还会用 DTO 限制答案数量为 5 条。状态机只统计 `Q1-Q5` 且 `optionCode` 非空的答案，未知题号和空选项不会把完成度虚高。
- 任一题缺失时，请求停在 `ANSWERING`，错误信息会指出缺失题号。
- 五题齐全后才进入 `READY_TO_SUBMIT`，随后由 `ElementCalculateService` 继续做真实日期和五行枚举校验。
- 重复题号沿用旧错误文案 `answers must contain 5 unique questions`，避免破坏已有前端和测试契约。

返回结果字段是前端结果页、分享图和双人匹配页的共同契约：

| 字段 | 用途 |
| --- | --- |
| `primaryElement` / `secondaryElement` | 驱动五行人物、图例色彩、匹配页双方摘要 |
| `primaryElementName` / `secondaryElementName` | 展示中文主副元素 |
| `primaryPercent` / `secondaryPercent` | 结果卡主副比例条 |
| `allElementScores` | 完整五行分布图，key 必须使用 `METAL/WOOD/WATER/FIRE/EARTH` |
| `personaTypeId` | 后端稳定分流 id，只用于排查和统计，不应作为用户端文案原样展示 |
| `personaLabel` | 四字人格标签，当前要求包含“的” |
| `accentElement` / `accentElementName` | 点睛元素 |
| `relationKind` | 主从关系类型，前端不直接展示原始后台词 |
| `starOfficerName` / `keywords` | 结果身份、人格短句、分享图摘要 |
| `dayMasterText` | 日主依据说明 |
| `primarySecondaryText` | 主元素和副元素关系 |
| `accentText` | 点睛元素说明 |
| `heavenText` / `humanText` | 内在世界和外部感受 |
| `starOfficerText` | 星官或星宿记忆锚点说明 |
| `growthAdvice` | 成长建议卡片 |
| `layoutExplanation` / `strengthText` / `relationshipText` | 兼容旧结果页字段，新页面优先使用结构化文案字段 |
| `shortCode` / `shortUrl` | 分享链接、双人匹配短码、短链回流追踪 |

前端不再依赖静态占位图生成结果卡；五行人物、图例和分布图都从这组字段渲染。

## 4. 查询测算结果

```http
GET /api/results/{resultId}
X-Client-Id: <client-id>
X-Session-Id: <session-id>
X-Channel: <channel>
X-Campaign: <campaign>
```

后端优先读取 Redis `result:{resultId}`，未命中再查 MySQL，并写入 `RESULT_VIEW` 事件。

## 5. 双人五行匹配

### 5.1 校验剪贴板候选短码

```http
GET /api/matches/candidates/{shortCode}
```

行为：

- 只接受 Base62、长度 6 或 7 的纯短码。
- 短码必须已经绑定一张有效结果卡。
- 返回首页确认弹窗需要的轻量摘要：短码、resultId、显示名、主副五行、关键词和创建时间。

### 5.2 创建我的结果并计算双人匹配

```http
POST /api/matches
Content-Type: application/json
X-Client-Id: <client-id>
X-Session-Id: <session-id>
X-Channel: <channel>
X-Campaign: <campaign>
```

请求体：

```json
{
  "partnerShortCode": "1cgeMu",
  "birthYear": 2005,
  "birthMonth": 3,
  "birthDay": null,
  "birthTimeRange": null,
  "answers": [
    {"questionCode": "Q1", "optionCode": "WOOD"},
    {"questionCode": "Q2", "optionCode": "FIRE"},
    {"questionCode": "Q3", "optionCode": "WOOD"},
    {"questionCode": "Q4", "optionCode": "EARTH"},
    {"questionCode": "Q5", "optionCode": "WATER"}
  ]
}
```

返回：

- `partnerResult`：剪贴板短码对应的人格卡。
- `currentResult`：当前用户刚创建的人格卡。
- `partnerShortCode` / `currentShortCode`：匹配结果页可刷新访问所需的两个短码。
- `compatibilityScore`、`relationLabel`、`headline`、`summary`、`strengths`、`suggestions`。

匹配页前端会复用 `ResultDetail` 中的五行字段展示双方主副元素，并用 `compatibilityScore` 绘制分数条。后端应保证 `compatibilityScore` 在 `0-100` 范围内，`strengths` 和 `suggestions` 至少各返回 1 条可展示文本。

### 5.3 通过两个短码查询匹配结果

```http
GET /api/matches/{partnerShortCode}/{currentShortCode}
```

行为：

- 两个短码都必须有效且不能相同。
- 不新增匹配表，实时读取两张结果卡并计算匹配结果。
- 匹配文案仅用于娱乐和自我观察，不输出决定论关系判断。

## 6. 短链接访问

```http
GET /s/{shortCode}?channel=share&campaign=result-card
```

行为：

- 校验短码为 Base62，长度 6 或 7。
- 写入 `SHORT_LINK_VISIT` 事件。
- 优先读取 Redis `shortlink:code:{shortCode}`。
- Redis 未命中查 `short_link`。
- 有效短码 302 到 `/result/{resultId}?sc={shortCode}`；external 模式创建给外部短链服务的原始落地 URL 直接使用 `/result/{resultId}?channel=share&campaign=result-card`，让外部短链回流也进入分享落地态。
- 如果访问短链时带 `channel` / `campaign` / `utm_source` / `utm_campaign`，会写入访问事件，并继续透传到结果页 query；例如 `/s/KWfD1W?channel=share&campaign=result-card` 会跳转到 `/result/{resultId}?sc=KWfD1W&channel=share&campaign=result-card`。
- 结果页看到 `sc`、`channel=share` 或 `channel=shared-result` 时进入分享落地态：隐藏二次分享盒，保留“我也测一张”作为主要回流入口。
- 无效短码写入 Redis 空值缓存 `shortlink:null:{shortCode}` 并跳转 `/not-found`。

## 7. 记录前端事件

```http
POST /api/events
Content-Type: application/json
X-Client-Id: <client-id>
X-Session-Id: <session-id>
X-Channel: <channel>
X-Campaign: <campaign>
```

请求体：

```json
{
  "eventType": "START_TEST_CLICK",
  "pagePath": "/",
  "resultId": null,
  "shortCode": null,
  "sessionId": "optional-session-id",
  "channel": "organic",
  "campaign": "spring-launch"
}
```

支持事件包括：

```text
PAGE_VIEW_HOME
START_TEST_CLICK
TEST_FORM_START
QUESTION_ANSWER_SELECT
TEST_SUBMIT_ATTEMPT
TEST_SUBMIT
RESULT_CREATED
RESULT_VIEW
SHORT_LINK_CREATED
SHORT_LINK_COPY
SAVE_SHARE_IMAGE_SUCCESS
NATIVE_SHARE_SUCCESS
SHARE_PANEL_VIEW
SHARED_RESULT_CTA_CLICK
RETAKE_TEST_CLICK
MATCH_CLIPBOARD_DETECTED
MATCH_SHORT_CODE_ENTERED
MATCH_MODE_ACCEPT
MATCH_MODE_DISMISS
MATCH_RESULT_VIEW
SHORT_LINK_VISIT
```

v2.1 归因规则：

- 前端优先通过 header 传 `X-Session-Id`、`X-Channel`、`X-Campaign`。
- `/api/events` 也支持在 body 中传 `sessionId`、`channel`、`campaign`。
- 后端只保存 `session_id_hash`，不保存明文 sessionId。
- 后端会根据 User-Agent 自动写入 `device_type`。
- v2.2 起，后台趋势会返回 `metricSource` 和 `aggregatedThroughDate`，用于说明日趋势来自实时事件还是日聚合表。

部署边界：生产环境仍推荐前端和后端通过同源 Nginx 反代暴露 `/api/*` 与 `/s/*`，这是默认、最少浏览器兼容成本的上线方式。

如果未来拆成独立 API 域名，后端已支持显式 CORS 白名单：

```bash
CORS_ALLOWED_ORIGINS=https://www.wuxingcard.cn,https://wuxingcard.cn
CORS_MAX_AGE_SECONDS=3600
```

开启后，后端允许 `GET`、`POST`、`OPTIONS`，允许前端当前使用的 `Content-Type`、`X-Client-Id`、`X-Session-Id`、`X-Channel`、`X-Campaign`、`X-Admin-Token` 请求头，并暴露 `Content-Disposition` 与 `Location`。默认不配置 `CORS_ALLOWED_ORIGINS` 时不开放跨域，避免误把任意来源放进生产 API。`MvpFlowIntegrationTest` 已覆盖允许来源 preflight 通过、未配置来源 preflight 拒绝。

## 8. 管理后台总览

```http
GET /api/admin/overview?includeSynthetic=false&forceRefresh=false
X-Admin-Token: <admin-token>
```

查询参数：

- `includeSynthetic`：默认 `false`，排除 `channel=perf-test` 的测试流量；设为 `true` 时包含全量，适合压测和 smoke 复核。
- `forceRefresh`：默认 `false`，命中后台 overview 短缓存；设为 `true` 时跳过读缓存并重新计算，适合上线 smoke 或刚写入样本后的验收检查。

返回：

- 总 PV / UV / UIP
- 首页访问量
- 开始测试点击量
- 测试提交量
- 结果生成量
- 短链生成量
- 短链访问量
- 完成率
- 增长漏斗 `funnelSteps`
- Top Channel `topChannels`
- Top Campaign `topCampaigns`
- 日趋势来源 `metricSource`
- 已聚合截止日期 `aggregatedThroughDate`
- 热门五行组合
- 热门星官
- 最近结果
- 最近短链

## 9. 短链接列表

```http
GET /api/admin/short-links?page=1&pageSize=20
X-Admin-Token: <admin-token>
```

支持的查询参数：

- `page` / `pageSize`：页码和每页数量，`pageSize` 最大 100。
- `keyword`：按短码、结果 ID、短链或星官关键词过滤，最多 64 个字符。
- `startDate` / `endDate`：按短链创建时间过滤，格式为 `YYYY-MM-DD`。
- `includeSynthetic`：默认 `false`，排除 `channel=perf-test` 的测试流量；压测复盘或 smoke 校验可设为 `true`。
- `statSource`：可选 `local` 或 `external`。该筛选需要计算统计来源，最多扫描 500 条短链；超过上限会返回 400，提示缩小日期或关键词。显式筛选外部来源时，如果外部统计服务不可用，会返回 502，而不是静默显示为空或当成本地统计。

返回短码、短链接、结果 ID、五行组合、星官、创建时间、PV、UV、UIP、统计来源、统计口径和最近访问时间。

- `statSource`：`local` 表示五行项目本地统计，`external` 表示独立短链服务统计。
- `metricSource`：`live_event` 表示实时访问事件聚合，`daily_metric` 表示日聚合表，`external` 表示外部短链统计。

## 10. 短链接访问日志

```http
GET /api/admin/short-links/{shortCode}/visits?page=1&pageSize=20
X-Admin-Token: <admin-token>
```

支持的查询参数：

- `page` / `pageSize`：页码和每页数量，`pageSize` 最大 100。
- `startDate` / `endDate`：按访问时间过滤，格式为 `YYYY-MM-DD`。
- `includeSynthetic`：默认 `false`，排除 `channel=perf-test`；设为 `true` 时包含测试流量。
- `statSource`：可选 `local` 或 `external`。不传时默认读取本地访问记录，且仅在 `includeSynthetic=true` 的兼容口径下优先尝试外部明细；传 `external` 时明确读取外部短链平台访问明细，外部不可用时返回空页，不回退成本地记录。

返回单条短链访问日志，只展示 hash 后的匿名标识。`statSource` 控制明细来源，`includeSynthetic` 只控制是否排除测试流量，两者不要混用为同一口径。

## 11. 短链接 CSV 导出

```http
GET /api/admin/short-links/export?keyword=KWfD1W&includeSynthetic=true
X-Admin-Token: <admin-token>
```

支持的查询参数：

- `keyword`：按短码、结果 ID、短链或星官关键词过滤。
- `statSource`：按统计来源过滤，例如 `local` 或 `external`。
- `startDate` / `endDate`：按短链创建时间过滤，格式为 `YYYY-MM-DD`。
- `includeSynthetic`：默认 `false`，后台运营口径会排除 `channel=perf-test`；本地 smoke 和压测复盘需要设置为 `true`，这样刚生成的验证数据才会出现在导出结果中。

响应：

```http
Content-Type: text/csv;charset=UTF-8
Content-Disposition: attachment; filename="wuxing-short-links-2026-06-15.csv"
```

CSV 使用 UTF-8 BOM，表头为：

```csv
shortCode,resultId,shortUrl,elementCombo,starOfficerName,pv,uv,uip,statSource,metricSource,createdAt,lastVisitAt
```

该接口和 `/api/admin/short-links` 使用同一筛选口径，适合运营下载短链明细，也用于本地联调脚本确认后台 token、短链查询、统计口径和 CSV header 没有错配。

## 12. 访问事件异步队列状态

```http
GET /api/admin/visit-events/runtime
X-Admin-Token: <admin-token>
```

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "queueSize": 0,
    "queueCapacity": 2048,
    "drainLimit": 64,
    "droppedAsyncEvents": 0,
    "totalFlushedEvents": 120,
    "lastFlushAt": "2026-06-12T03:30:00",
    "lastBatchSize": 16,
    "batchWriteFailures": 0,
    "workerAlive": true,
    "asyncMode": "local",
    "rocketMqAvailable": false,
    "rocketMqFallbackToLocal": true,
    "rocketMqTopic": "wuxing-visit-event",
    "rocketMqPublishedEvents": 0,
    "rocketMqPublishFailures": 0,
    "rocketMqFallbackEvents": 0,
    "rocketMqShadowLocalEvents": 0,
    "rocketMqConsumerEnabled": false,
    "rocketMqConsumerPersistenceReady": false,
    "healthStatus": "ok",
    "healthMessage": "访问事件异步写入正常。"
  }
}
```

用于性能 smoke 和后台排查：确认短链访问事件队列是否积压、后台 writer 是否存活、是否持续排水，以及低延迟是否以丢弃过多低价值事件或批量写失败为代价。`totalFlushedEvents` 表示批量写成功、同步写成功，或批量失败后单条降级写成功的累计数，不把最终失败的写入尝试算作成功。`healthStatus` 取值为 `ok`、`watch`、`danger`；`danger` 表示 writer 异常、写库失败、队列接近满载或 MQ 无回退等需要立即处理的状态。`asyncMode` 可为 `local`、`rocketmq` 或测试/本地验收使用的 `sync`；`asyncMode=rocketmq` 时，这组字段还会展示 MQ 发布、回退和 shadow 本地落库状态。生产启用 MQ consumer 前，推荐保持 `rocketMqConsumerEnabled=false`。即使配置了 `rocketMqConsumerEnabled=true`，只有 `rocketMqConsumerPersistenceReady=true` 时才会让 MQ consumer 接管落库，否则仍保持 shadow 本地写入。

## 13. 手动刷新增长聚合

```http
POST /api/admin/analytics/aggregate?startDate=2026-06-10&endDate=2026-06-10
X-Admin-Token: <admin-token>
```

行为：

- 只允许聚合今天以前的已闭合日期。
- 单次最多聚合 31 天。
- 重复聚合同一天会先删除旧快照再写入新快照，避免重复数据。
- 生成 `site_daily_metric` 和 `short_link_daily_metric`。
- 聚合成功后会推进 overview 缓存版本，让下一次 `/api/admin/overview` 读取直接使用新聚合口径。

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "startDate": "2026-06-10",
    "endDate": "2026-06-10",
    "daysAggregated": 1,
    "shortLinkRowsAggregated": 3,
    "aggregatedAt": "2026-06-11T14:00:00"
  }
}
```
