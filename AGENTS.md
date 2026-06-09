# AGENTS.md｜五行人格卡 MVP 开发指令文档

> 用途：本文件作为 Codex / AI 编码工具的项目开发说明。请把它放在仓库根目录，作为项目级开发指令。  
> 项目阶段：第一版 MVP。  
> 核心目标：短链接系统真实接入业务，并快速上线获得反馈。

---

## 0. 给 Codex 的总指令

你正在开发一个名为「五行人格卡」的 H5 微项目。请严格按照本文档实现第一版 MVP，不要自行扩大需求范围。

开发时遵守以下原则：

1. **先完成完整单人测算闭环**：引导页 → 测试页 → 后端生成结果 → 结果页 → 短链接 → 短链访问 → 数据统计。
2. **短链接是真实业务核心，不是形式功能**：短链必须能生成、解析、访问、统计 PV/UV/UIP，并能在后台看到数据。
3. **快速上线优先**：第一版采用简单、稳定、可部署的实现，不追求复杂架构。
4. **不做朋友匹配**：第一版完全不开发双人匹配功能，只预留扩展空间。
5. **不做登录注册**：普通用户匿名使用；后台只做简单访问保护。
6. **所有解读必须正向、娱乐、友好**：禁止封建迷信、恐吓式预测、灾祸、疾病、死亡、破财、婚姻定论等表达。
7. **如果已有仓库代码，先阅读现有结构再改动**。不要无理由重构，不要删除已有功能。
8. **如果是空仓库，按本文档建议的 monorepo 结构初始化项目**。
9. **每完成一个模块后给出可验证结果**：能运行、能访问、能通过基本测试。

---

## 1. 项目一句话定位

「五行人格卡」是一个基于出生年月和价值取向题的传统文化娱乐人格测试 H5 网站。用户完成测试后，系统生成五行属性组合、年月星官、人格关键词、正向性格解读和人格卡片，并为该结果生成专属短链接，方便分享和统计访问数据。

项目不是命理预测工具，而是娱乐化的人格解读和社交分享工具。

---

## 2. 第一版 MVP 核心目标

第一优先级：

- 把短链接能力接入真实业务场景。
- 用户结果页必须能生成短链。
- 短链必须可以访问并解析到同一个结果。
- 短链访问必须统计 PV、UV、UIP。
- 数据中台必须能看到短链业务数据。

第二优先级：

- 快速上线。
- H5 流程完整可用。
- 云服务器单机部署。
- 不追求复杂功能，先获得真实体验反馈。

---

## 3. MVP 必须做与坚决不做

### 3.1 必须做

H5 端必须包含：

- 引导页 `/`
- 开始测试页 `/test`
- 结果页 `/result/:resultId`
- 短链接访问路径 `/s/:shortCode`
- 简单数据中台 `/admin`

后端必须包含：

- 接收用户输入
- 计算五行百分比组合
- 生成年月星官
- 生成 3–5 个正向人格关键词
- 生成优点和相处优势解读
- 保存用户结果
- 生成短链接
- 解析短链接
- 记录访问日志
- 统计 PV、UV、UIP、点击量、提交量、短链访问量
- Redis 缓存短链解析和热门结果

### 3.2 坚决不做

第一版禁止开发以下功能：

- 朋友匹配 / 双人匹配
- 登录注册
- 用户历史记录
- 社区、评论、点赞、关注
- 付费系统
- AI 深度解读
- 复杂八字排盘
- 多套卡片模板
- 复杂图片编辑器
- 复杂后台权限系统
- 复杂 BI 大屏

如果用户或后续需求提到这些功能，先标记为二期或三期，不要在 MVP 中实现。

---

## 4. 推荐技术栈

如果仓库已有技术栈，优先沿用已有栈。若是空仓库，建议使用以下技术栈：

### 4.1 前端

- Vue 3
- Vite
- TypeScript
- 移动端 H5 优先
- 可使用轻量移动端 UI 组件库，也可以先手写基础样式
- 使用 localStorage 保存匿名 clientId
- 使用 fetch 或 axios 调用后端接口

### 4.2 后端

- Java 17+
- Spring Boot
- Maven
- MySQL
- Redis
- MyBatis / MyBatis-Plus / Spring Data JPA 三者任选其一；如果没有既有约束，建议 MyBatis-Plus，便于快速 CRUD
- Lombok 可选
- Spring Validation 用于参数校验

### 4.3 部署

- 云服务器单机部署
- Docker Compose 管理 MySQL、Redis、后端服务、Nginx
- Nginx 对外提供 H5 静态页面、后端 API 代理、短链接路径代理
- HTTPS 后续使用免费证书配置

---

## 5. 推荐仓库结构

如果是空仓库，建议按以下结构创建：

```text
wuxing-persona-card/
  AGENTS.md
  README.md
  docs/
    product-spec.md
    api-spec.md
    db-schema.md
    deploy.md
  frontend/
    package.json
    vite.config.ts
    src/
      main.ts
      router/
      api/
      pages/
        GuidePage.vue
        TestPage.vue
        ResultPage.vue
        AdminDashboard.vue
        AdminShortLinkDetail.vue
      components/
        ElementRatioCard.vue
        PersonaCard.vue
        ShareLinkBox.vue
        QuestionCard.vue
        StatCard.vue
      utils/
        clientId.ts
        tracker.ts
      assets/
        cards/
  backend/
    pom.xml
    src/main/java/...
      WuxingPersonaApplication.java
      common/
      config/
      controller/
        ResultController.java
        ShortLinkController.java
        EventController.java
        AdminController.java
      service/
        ResultService.java
        ElementCalculateService.java
        StarOfficerService.java
        ShortLinkService.java
        VisitStatService.java
        AdminStatService.java
      repository/
      mapper/
      entity/
      dto/
      vo/
      enums/
      util/
    src/main/resources/
      application.yml
      db/migration/ or schema.sql
  deploy/
    docker-compose.yml
    nginx.conf
    .env.example
```

---

## 6. H5 用户主流程

### 6.1 引导页 `/`

用户看到：

- 项目标题：五行人格卡
- 副标题：传统文化趣味人格测试，仅供娱乐
- 示例说明：输入出生年月和 5 道价值取向题，生成你的五行人格卡
- 按钮：开始测试

系统需要做：

- 记录页面访问事件：`PAGE_VIEW_HOME`
- 点击按钮时记录事件：`START_TEST_CLICK`
- 跳转到 `/test`

### 6.2 测试页 `/test`

用户输入：

- 出生年份：必填
- 出生月份：必填
- 出生日期：可选，可选择“不透露”
- 出生时段：可选，可选择“不透露”
- 5 道价值取向题：必填

系统需要做：

- 校验出生年月合法性
- 校验 5 道题都有答案
- 提交到后端 `POST /api/results`
- 提交时记录事件：`TEST_SUBMIT`

### 6.3 结果页 `/result/:resultId`

用户看到：

- 人格卡片主视觉
- 五行组合百分比，例如 70% 金 / 30% 水
- 年月星官名称
- 3–5 个关键词
- 五行布局解释
- 正向性格亮点
- 相处优势
- 专属短链接
- 一键复制按钮

系统需要做：

- 调用 `GET /api/results/{resultId}` 获取结果
- 记录结果页访问事件：`RESULT_VIEW`
- 用户点击复制短链时记录：`SHORT_LINK_COPY`

### 6.4 短链接访问 `/s/:shortCode`

访问路径：

```text
https://your-domain.com/s/abc123
```

后端行为：

1. 接收短码 `abc123`
2. 记录短链访问事件：`SHORT_LINK_VISIT`
3. 从 Redis 查询短码映射
4. Redis 未命中则查 MySQL
5. 查到后跳转到 `/result/{resultId}?sc=abc123`
6. 查不到则跳转到一个友好错误页或返回 404 页面

---

## 7. 用户输入字段定义

### 7.1 前端表单字段

```json
{
  "birthYear": 2002,
  "birthMonth": 8,
  "birthDay": null,
  "birthTimeRange": null,
  "answers": [
    { "questionCode": "Q1", "optionCode": "WOOD" },
    { "questionCode": "Q2", "optionCode": "WATER" },
    { "questionCode": "Q3", "optionCode": "METAL" },
    { "questionCode": "Q4", "optionCode": "EARTH" },
    { "questionCode": "Q5", "optionCode": "FIRE" }
  ]
}
```

字段说明：

- `birthYear`：必填，合理范围建议 1900 到当前年份
- `birthMonth`：必填，1 到 12
- `birthDay`：可选，1 到 31 或 null
- `birthTimeRange`：可选
- `answers`：必填，必须包含 5 道题答案

### 7.2 出生时段枚举

```text
MORNING    上午
NOON       中午
AFTERNOON  下午
EVENING    傍晚
NIGHT      夜晚
UNKNOWN    不透露 / 不确定
```

---

## 8. 价值取向题设计

第一版使用 5 道题，每题 5 个选项，每个选项对应一个五行元素。题目是娱乐化人格倾向，不做心理学专业测评声明。

### Q1：做决定时，你更看重什么？

- METAL：标准、边界和清晰判断
- WOOD：成长空间和长期可能
- WATER：感受、关系和细节变化
- FIRE：行动效率和当下热情
- EARTH：稳定、安全和可持续

### Q2：和别人相处时，你更常扮演什么角色？

- METAL：帮大家理清规则的人
- WOOD：提出计划和方向的人
- WATER：倾听情绪和理解细节的人
- FIRE：带动气氛和推进节奏的人
- EARTH：稳住局面和照顾整体的人

### Q3：面对压力时，你更倾向于？

- METAL：拆解问题，建立秩序
- WOOD：寻找新的成长路径
- WATER：先感受和观察，再慢慢调整
- FIRE：先行动起来，边做边修正
- EARTH：保持稳定，把眼前事情做好

### Q4：你最欣赏哪种能力？

- METAL：清醒判断和高效执行
- WOOD：持续成长和创造可能
- WATER：共情理解和灵活适应
- FIRE：热情表达和感染他人
- EARTH：可靠承载和长期陪伴

### Q5：如果要完成一个项目，你更愿意负责？

- METAL：规则制定、标准检查、关键决策
- WOOD：规划路线、设计方案、推动成长
- WATER：观察反馈、沟通协调、情绪支持
- FIRE：启动项目、对外表达、快速推进
- EARTH：资源统筹、稳定执行、兜底收尾

---

## 9. 五行计算规则 MVP 版

重要：该规则是娱乐化人格生成规则，不是八字排盘，不是命理预测。

### 9.1 初始分

五个元素初始分都为 20：

```text
METAL = 20
WOOD  = 20
WATER = 20
FIRE  = 20
EARTH = 20
```

### 9.2 出生年份加权

使用简化规则：`birthYear % 5` 决定年份倾向元素，给该元素 +8 分。

```text
0 -> METAL
1 -> WATER
2 -> WOOD
3 -> FIRE
4 -> EARTH
```

### 9.3 出生月份加权

月份是第一版主要时间因素，给主元素 +25 分，辅助元素 +10 分。

```text
1月  -> WATER +25, EARTH +10
2月  -> WOOD  +25, WATER +10
3月  -> WOOD  +25, FIRE  +10
4月  -> WOOD  +25, EARTH +10
5月  -> FIRE  +25, WOOD  +10
6月  -> FIRE  +25, EARTH +10
7月  -> EARTH +25, FIRE  +10
8月  -> METAL +25, EARTH +10
9月  -> METAL +25, WATER +10
10月 -> EARTH +25, METAL +10
11月 -> WATER +25, METAL +10
12月 -> WATER +25, EARTH +10
```

### 9.4 出生日期加权，可选

如果用户填写出生日期：

```text
birthDay % 5 == 0 -> METAL +6
birthDay % 5 == 1 -> WATER +6
birthDay % 5 == 2 -> WOOD  +6
birthDay % 5 == 3 -> FIRE  +6
birthDay % 5 == 4 -> EARTH +6
```

如果不填写，则不加权。

### 9.5 出生时段加权，可选

```text
MORNING   -> WOOD  +8
NOON      -> FIRE  +8
AFTERNOON -> EARTH +8
EVENING   -> METAL +8
NIGHT     -> WATER +8
UNKNOWN   -> 不加权
null      -> 不加权
```

### 9.6 价值取向题加权

每道题选项对应一个元素，被选择的元素 +12 分。五道题最多增加 60 分。

### 9.7 百分比归一化

计算五个元素总分后归一化为百分比。

前端第一版重点展示前两名元素：

```text
主元素：分数最高的元素
副元素：分数第二高的元素
展示百分比：主元素和副元素按二者分数重新归一化到 100%
```

例如五元素原始分：

```text
METAL 90
WATER 40
WOOD 30
FIRE 25
EARTH 35
```

主副展示：

```text
METAL = 69%
WATER = 31%
```

结果页可以显示：

```text
你的五行人格组合：69% 金 / 31% 水
```

后台可保存完整五元素分布，前端默认展示主副组合。

---

## 10. 年月星官规则 MVP 版

第一版只基于出生年月中的月份生成星官。年份只参与五行权重，不做复杂星官组合。

星官名称用于娱乐化展示，避免严肃命理表达。

```text
1月  玄冰星官  水系  安静、沉淀、观察
2月  青芽星官  木系  生长、开始、温和
3月  春林星官  木系  规划、舒展、创造
4月  竹风星官  木系  耐心、引导、方向
5月  赤阳星官  火系  行动、热情、表达
6月  炎庭星官  火系  推进、点燃、外放
7月  山雨星官  土系  承接、稳定、协调
8月  白露星官  金系  清醒、判断、秩序
9月  金桂星官  金系  标准、边界、执行
10月 岩衡星官  土系  平衡、统筹、守护
11月 澄夜星官  水系  洞察、倾听、适应
12月 雪川星官  水系  冷静、深度、包容
```

---

## 11. 文案生成规则

第一版不接入 AI，不动态生成不可控长文。使用模板化、配置化文案。

### 11.1 文案原则

必须使用：

- “你可能更偏向……”
- “这代表你在关系中常常……”
- “你的优势更像是……”
- “适合你的相处方式是……”

禁止使用：

- “你命中注定……”
- “你一定会……”
- “你会破财 / 生病 / 遭遇灾祸……”
- “你们相克……”
- “不适合结婚 / 不适合合作……”
- “运势、灾祸、死亡、疾病、破财”等内容

### 11.2 关键词生成

关键词来自主元素、副元素、星官共同组合。

示例：

```text
METAL: 清醒、秩序、判断、执行、边界
WOOD: 成长、规划、创造、耐心、引导
WATER: 观察、共情、适应、细腻、洞察
FIRE: 热情、行动、表达、感染力、突破
EARTH: 稳定、承载、协调、可靠、安全感
```

返回 3–5 个关键词，优先使用主元素 2–3 个、副元素 1–2 个、星官 1 个。

### 11.3 结果解读结构

结果页详细解读分为三段：

1. **五行布局解释**
   - 解释出生年月和价值取向如何形成当前主副元素
   - 强调娱乐化人格倾向

2. **性格亮点**
   - 只讲优点、优势、正向特质

3. **相处优势**
   - 讲别人和你相处会感受到什么
   - 给轻松友好的建议

---

## 12. 结果页返回数据结构

`POST /api/results` 成功后返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "resultId": "R202601010001",
    "primaryElement": "METAL",
    "primaryElementName": "金",
    "primaryPercent": 70,
    "secondaryElement": "WATER",
    "secondaryElementName": "水",
    "secondaryPercent": 30,
    "allElementScores": {
      "METAL": 90,
      "WOOD": 30,
      "WATER": 40,
      "FIRE": 25,
      "EARTH": 35
    },
    "starOfficerCode": "BAILU",
    "starOfficerName": "白露星官",
    "keywords": ["清醒", "判断", "秩序", "洞察"],
    "layoutExplanation": "你的出生月份带来了偏金的清醒感，价值取向中又体现出水系的观察力，因此形成了金水组合。",
    "strengthText": "你更擅长在复杂信息中抓住重点，也能保持冷静和边界感。",
    "relationshipText": "和你相处的人通常会感到安心，因为你既能理清问题，也愿意留意细节。",
    "cardImageKey": "metal_water_default",
    "shortCode": "abc123",
    "shortUrl": "https://your-domain.com/s/abc123",
    "createdAt": "2026-06-08T12:00:00"
  }
}
```

---

## 13. 后端接口设计

### 13.1 获取题目配置

```http
GET /api/questions
```

返回 5 道价值取向题及选项。

### 13.2 创建测算结果

```http
POST /api/results
Content-Type: application/json
X-Client-Id: <frontend-client-id>
```

请求体见第 7 节。

后端行为：

1. 参数校验
2. 计算五行分数
3. 计算星官
4. 生成关键词和解读文案
5. 保存 user_result
6. 生成短链接记录
7. 返回结果详情
8. 记录 `RESULT_CREATED` 事件

### 13.3 查询结果

```http
GET /api/results/{resultId}
X-Client-Id: <frontend-client-id>
```

行为：

- 优先查 Redis `result:{resultId}`
- 未命中查 MySQL
- 返回结果详情
- 记录 `RESULT_VIEW` 事件

### 13.4 短链接访问

```http
GET /s/{shortCode}
```

行为：

- 记录 `SHORT_LINK_VISIT`
- 解析短码
- 成功则 302 跳转 `/result/{resultId}?sc={shortCode}`
- 失败则返回 404 或跳转 `/not-found`

### 13.5 记录前端事件

```http
POST /api/events
Content-Type: application/json
X-Client-Id: <frontend-client-id>
```

请求示例：

```json
{
  "eventType": "START_TEST_CLICK",
  "pagePath": "/",
  "resultId": null,
  "shortCode": null
}
```

### 13.6 管理后台总览

```http
GET /api/admin/overview
X-Admin-Token: <admin-token>
```

返回：

- 总 PV
- 总 UV
- 总 UIP
- 首页访问量
- 开始测试点击量
- 提交次数
- 结果生成数
- 短链生成数
- 短链访问数
- 完成率
- 热门五行组合

### 13.7 短链接列表

```http
GET /api/admin/short-links?page=1&pageSize=20
X-Admin-Token: <admin-token>
```

返回字段：

- shortCode
- resultId
- primaryElement
- secondaryElement
- starOfficerName
- createdAt
- pv
- uv
- uip
- lastVisitAt

### 13.8 短链接访问日志

```http
GET /api/admin/short-links/{shortCode}/visits?page=1&pageSize=20
X-Admin-Token: <admin-token>
```

返回该短链访问日志列表。

---

## 14. 数据库表设计

### 14.1 user_result

用途：保存一次用户测算结果。

关键字段：

```sql
id BIGINT PRIMARY KEY AUTO_INCREMENT
result_id VARCHAR(64) UNIQUE NOT NULL
birth_year INT NOT NULL
birth_month INT NOT NULL
birth_day INT NULL
birth_time_range VARCHAR(32) NULL
answer_json TEXT NOT NULL
primary_element VARCHAR(32) NOT NULL
secondary_element VARCHAR(32) NOT NULL
primary_percent INT NOT NULL
secondary_percent INT NOT NULL
all_element_scores_json TEXT NOT NULL
star_officer_code VARCHAR(64) NOT NULL
star_officer_name VARCHAR(64) NOT NULL
keywords_json TEXT NOT NULL
layout_explanation TEXT NOT NULL
strength_text TEXT NOT NULL
relationship_text TEXT NOT NULL
card_image_key VARCHAR(128) NULL
status TINYINT NOT NULL DEFAULT 1
created_at DATETIME NOT NULL
updated_at DATETIME NOT NULL
```

索引：

```sql
UNIQUE KEY uk_result_id(result_id)
INDEX idx_created_at(created_at)
INDEX idx_primary_secondary(primary_element, secondary_element)
```

### 14.2 short_link

用途：保存短链与结果的映射。

关键字段：

```sql
id BIGINT PRIMARY KEY AUTO_INCREMENT
short_code VARCHAR(32) UNIQUE NOT NULL
result_id VARCHAR(64) NOT NULL
original_path VARCHAR(255) NOT NULL
short_url VARCHAR(255) NOT NULL
pv_count BIGINT NOT NULL DEFAULT 0
uv_count BIGINT NOT NULL DEFAULT 0
uip_count BIGINT NOT NULL DEFAULT 0
last_visit_at DATETIME NULL
status TINYINT NOT NULL DEFAULT 1
created_at DATETIME NOT NULL
updated_at DATETIME NOT NULL
```

索引：

```sql
UNIQUE KEY uk_short_code(short_code)
INDEX idx_result_id(result_id)
INDEX idx_created_at(created_at)
```

### 14.3 visit_event

用途：记录页面访问、按钮点击、短链访问、结果生成等事件。

关键字段：

```sql
id BIGINT PRIMARY KEY AUTO_INCREMENT
event_type VARCHAR(64) NOT NULL
page_path VARCHAR(255) NULL
result_id VARCHAR(64) NULL
short_code VARCHAR(32) NULL
client_id_hash VARCHAR(128) NULL
ip_hash VARCHAR(128) NULL
user_agent_hash VARCHAR(128) NULL
referer VARCHAR(512) NULL
created_at DATETIME NOT NULL
```

索引：

```sql
INDEX idx_event_type_created(event_type, created_at)
INDEX idx_short_code_created(short_code, created_at)
INDEX idx_result_id_created(result_id, created_at)
INDEX idx_client_id(client_id_hash)
INDEX idx_ip_hash(ip_hash)
```

### 14.4 question_config，可选

第一版题目可以写在代码常量中，也可以放数据库。为了快速开发，建议先写在代码枚举或 JSON 配置中，后续再抽表。

---

## 15. Redis 缓存设计

### 15.1 短链解析缓存

```text
shortlink:code:{shortCode} -> resultId
TTL: 24h 或长期
```

短链不存在时，写空值缓存：

```text
shortlink:null:{shortCode} -> 1
TTL: 5min
```

用途：减少无效短链反复打数据库。

### 15.2 结果缓存

```text
result:{resultId} -> result detail JSON
TTL: 1h 或 24h
```

用途：热门结果页减少数据库查询。

### 15.3 统计辅助缓存

```text
stat:shortlink:pv:{shortCode}
stat:site:pv:{yyyyMMdd}
```

MVP 可同步写数据库，同时用 Redis 做计数辅助。不要第一版就引入消息队列。

---

## 16. PV / UV / UIP 统计规则

### 16.1 clientId

前端首次访问时生成匿名 clientId：

```text
wuxing_client_id = UUID
```

保存在 localStorage。每次请求通过 header 传给后端：

```http
X-Client-Id: <uuid>
```

后端不要明文保存 clientId，保存 hash：

```text
client_id_hash = sha256(clientId + salt)
```

### 16.2 IP

后端从请求中获取 IP，做 hash 后保存：

```text
ip_hash = sha256(ip + salt)
```

不要保存明文 IP。

### 16.3 UV

UV = 指定条件下 distinct `client_id_hash` 数量。  
如果没有 clientId，则可使用 `ip_hash + user_agent_hash` 的组合兜底。

### 16.4 UIP

UIP = 指定条件下 distinct `ip_hash` 数量。

### 16.5 PV

PV = 指定条件下事件总数。

---

## 17. 数据中台设计

第一版后台路径：

```text
/admin
```

后台采用极简样式，不做复杂 UI。

### 17.1 后台访问保护

不要做复杂账号系统。使用以下任一方式：

方案 A：Nginx Basic Auth 保护 `/admin`。  
方案 B：后端配置 `ADMIN_TOKEN`，前端后台请求时带 `X-Admin-Token`。

如果不确定，优先使用方案 B，开发更直接。

### 17.2 总览页内容

数字卡片：

- 总 PV
- 总 UV
- 总 UIP
- 首页访问量
- 开始测算点击数
- 测试提交数
- 结果生成数
- 短链接生成数
- 短链接访问数
- 完成率：结果生成数 / 开始测算点击数

表格：

- 热门五行组合
- 热门星官
- 最近生成结果
- 最近短链访问

### 17.3 短链接面板

列表字段：

- 短码
- 短链接
- resultId
- 五行组合
- 星官
- 创建时间
- PV
- UV
- UIP
- 最近访问时间

详情页字段：

- 访问时间
- 事件类型
- clientId hash
- IP hash
- User-Agent hash
- Referer

---

## 18. 前端页面要求

### 18.1 视觉方向

第一版以清爽、移动端友好为主，不要追求复杂动画。

关键词：

- 轻国风
- 卡片化
- 温和色彩
- 移动端优先
- 分享友好

必须有免责声明：

```text
本结果为传统文化元素启发下的娱乐性人格解读，不构成现实决策建议。
```

### 18.2 结果卡片

第一版可以先使用静态人物图或占位图。根据主元素选择卡片资源：

```text
METAL -> metal_default.png
WOOD  -> wood_default.png
WATER -> water_default.png
FIRE  -> fire_default.png
EARTH -> earth_default.png
```

卡片展示：

- 五行主副比例
- 星官名称
- 关键词标签
- 人物图
- 娱乐声明

### 18.3 复制短链接

优先使用 Clipboard API。失败时降级为选中文本让用户手动复制。

复制成功后显示轻提示：

```text
短链接已复制，发给朋友看看吧
```

---

## 19. 后端工程要求

### 19.1 分层

请至少分为：

- controller
- service
- repository / mapper
- entity
- dto
- vo
- util
- enums

不要把计算规则写在 controller 里。

### 19.2 参数校验

所有外部输入必须校验：

- birthYear 合法
- birthMonth 1–12
- birthDay 可空；不为空时 1–31
- answers 必须 5 道
- optionCode 必须是合法五行枚举
- shortCode 只能包含 base62 字符，长度限制

### 19.3 统一返回结构

建议：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误时：

```json
{
  "code": 400,
  "message": "birthMonth must be between 1 and 12",
  "data": null
}
```

### 19.4 异常处理

实现全局异常处理：

- 参数错误
- 结果不存在
- 短链不存在
- 系统错误

### 19.5 日志

关键流程打印业务日志：

- 创建结果
- 生成短链
- 访问短链
- 短链解析失败
- 管理后台查询

不要打印用户完整出生信息到日志。

---

## 20. 短链接实现要求

### 20.1 短码生成

使用 base62 字符：

```text
0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
```

长度建议 6 位或 7 位。

生成方式：

- 随机生成
- 查询 Redis / MySQL 确认不重复
- 冲突则重试，最多 5 次

### 20.2 短链绑定

每个 resultId 默认生成一个短链。  
如果同一个 resultId 已经有可用短链，直接返回已有短链，不重复创建。

### 20.3 短链访问

访问时必须：

- 记录访问事件
- 更新 short_link 的 pv_count / last_visit_at
- 后续通过事件表统计 UV/UIP
- Redis 未命中查库并回写缓存
- 无效短链写空值缓存

---

## 21. 部署方案

### 21.1 第一版部署架构

```text
浏览器
  -> Nginx
    -> H5 静态页面
    -> /api/** 代理到 Spring Boot
    -> /s/**   代理到 Spring Boot 短链解析
    -> /admin  H5 后台页面
Spring Boot
  -> MySQL
  -> Redis
```

### 21.2 Docker Compose 服务

建议包含：

- mysql
- redis
- backend
- nginx

前端可以在构建后由 nginx 托管静态文件。

### 21.3 环境变量

`.env.example` 至少包含：

```text
APP_BASE_URL=https://your-domain.com
MYSQL_DATABASE=wuxing_persona
MYSQL_USER=wuxing
MYSQL_PASSWORD=change-me
MYSQL_ROOT_PASSWORD=change-me
REDIS_HOST=redis
REDIS_PORT=6379
ADMIN_TOKEN=change-me
HASH_SALT=change-me
```

### 21.4 安全要求

- MySQL 不暴露公网
- Redis 不暴露公网
- 管理后台必须有 token 或 Basic Auth
- 用户输入和访问日志不要保存明文 IP
- 页面明确显示娱乐声明

---

## 22. 开发任务拆分

请按以下顺序开发，不要跳跃式同时做所有内容。

### 阶段 1：项目初始化

- 创建前后端目录
- 配置基础依赖
- 创建 Docker Compose 初版
- 创建 README

验收：前端和后端都能本地启动。

### 阶段 2：后端基础能力

- 数据库表
- Entity / Mapper
- 统一返回结构
- 全局异常处理
- 枚举定义

验收：能连接 MySQL 和 Redis，能启动后端。

### 阶段 3：五行结果生成

- 题目配置接口
- 五行计算服务
- 星官服务
- 文案模板服务
- 创建结果接口
- 查询结果接口

验收：提交出生年月和 5 道题，能返回完整结果。

### 阶段 4：短链接模块

- 短码生成
- 短链表
- 生成短链
- 短链解析
- 302 跳转
- Redis 缓存
- 无效短链缓存

验收：复制短链后可打开同一个结果页。

### 阶段 5：访问统计

- 前端 clientId
- 事件记录接口
- 短链访问日志
- PV/UV/UIP 统计逻辑

验收：后台能看到访问量变化。

### 阶段 6：H5 页面

- 引导页
- 测试页
- 结果页
- 短链复制
- 基础移动端样式

验收：用户可完整完成一次测算并复制短链。

### 阶段 7：数据中台

- 总览页
- 短链列表
- 短链访问详情
- 管理 token

验收：能查看网站整体数据和短链数据。

### 阶段 8：部署

- Docker Compose 完整化
- Nginx 配置
- `.env.example`
- 部署文档

验收：云服务器可公网访问。

---

## 23. 测试与验收清单

开发完成后必须验证：

### 23.1 H5 流程

- 首页可打开
- 点击开始测试可跳转
- 出生年月必填校验正常
- 出生日期和时段可不填
- 5 道题必须答完
- 提交后生成结果
- 结果页显示五行比例、星官、关键词、解读
- 短链接可复制

### 23.2 后端

- `GET /api/questions` 正常
- `POST /api/results` 正常
- `GET /api/results/{resultId}` 正常
- `GET /s/{shortCode}` 正常跳转
- 无效短链不报 500

### 23.3 数据统计

- 首页 PV 增加
- 开始测算点击数增加
- 提交数增加
- 结果生成数增加
- 短链生成数增加
- 短链访问 PV 增加
- UV/UIP 统计能去重

### 23.4 数据中台

- `/admin` 需要 token
- 总览数据正常
- 短链列表正常
- 短链详情日志正常

### 23.5 合规边界

- 页面有娱乐声明
- 文案没有恐吓预测
- 文案没有疾病、灾祸、死亡、破财等内容
- 文案没有“命中注定”“相克”“必然失败”等绝对化表达

---

## 24. README 必须包含

后续生成 README 时，至少包含：

- 项目介绍
- 在线地址，若已有
- 技术栈
- 项目架构图
- 核心流程图
- 短链接接入说明
- 数据统计说明
- 数据库表说明
- 本地启动方式
- Docker 部署方式
- MVP 功能边界
- 后续迭代计划
- 娱乐声明与隐私说明

---

## 25. 面试表达重点

这个项目要能支撑 Java 后端实习面试。请在 README 和代码结构中突出以下点：

1. 从 0 到 1 上线的 H5 微项目。
2. 短链接不是 Demo，而是接入了真实业务结果页分享。
3. 短链支持生成、解析、跳转、访问统计。
4. 统计 PV、UV、UIP，并在数据中台展示。
5. Redis 用于短链解析缓存和热门结果缓存。
6. 无效短链使用空值缓存，避免恶意请求反复打数据库。
7. 用户输入隐私克制，不收集昵称和性别，生日日期与时段可选。
8. 结果文案全部正向、娱乐化，避免封建迷信和负面预测。
9. 云服务器单机部署，Nginx 对外代理，Docker Compose 管理服务。

---

## 26. 最终完成定义 Definition of Done

只有同时满足以下条件，才算第一版 MVP 完成：

1. 用户能在 H5 上完成完整单人测算。
2. 后端能稳定返回五行百分比、星官、关键词和解读。
3. 每个结果能生成唯一短链接。
4. 短链接能被访问，并能跳转到对应结果页。
5. 短链接访问能记录 PV、UV、UIP。
6. `/admin` 能看到网站整体数据和短链接数据。
7. Redis 缓存短链解析和结果详情。
8. 项目能通过 Docker Compose 在云服务器部署。
9. README 写清项目定位、启动方式、架构和短链接亮点。
10. 全站文案符合娱乐测试边界，不包含负面宿命论或封建迷信表达。

