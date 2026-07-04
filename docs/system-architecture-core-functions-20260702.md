# 五行人格卡系统架构与核心函数导览

更新时间：2026-07-02

## 1. 本轮执行提示词

把用户口语要求转成可执行标准：

- 以至少七小时为周期，先做计划、再改代码、每一步后做质量检查。
- 项目要能作为后端简历项目展示，所以后端链路、数据模型、缓存、短链、统计、质量门都要讲得清楚。
- 产出一份系统文档，解释项目架构、核心功能函数、关键参数、数据流和验收命令。
- 引入明确的答题流程状态机，让用户从出生信息、五题答题、提交结果到结果页的状态可推导、可测试、可维护。
- 优化 H5 操作体验：出生年份支持 1950-2026，并且不会超过当前年份；年份选择更清晰；答题页左侧返回上一题、右侧进入下一题或生成结果。
- 所有改动先在本机预览和测试，确认后再考虑统一上线。

## 2. 一句话架构

五行人格卡是一个“人格测试内容 + 短链传播 + 匿名统计后台”的全栈项目：

```text
Vue H5 测试页
  -> Spring Boot 创建结果
  -> 五行计算与 120 人格注册表分流
  -> MySQL 保存结果与短链
  -> Redis 缓存结果 / 短码 / 后台概览
  -> /s/{shortCode} 短链回流
  -> visit_event 记录匿名访问
  -> Admin 后台统计 PV / UV / UIP / 漏斗 / 访问明细
```

## 2.1 代码目录地图

从简历项目和代码阅读角度，可以先把仓库理解成五层：

| 目录 | 关注点 | 你应该重点看什么 |
| --- | --- | --- |
| `frontend/src/pages` | 用户页面和后台页面 | 首页、测试页、结果页、匹配页、后台统计页 |
| `frontend/src/components` | 可复用展示组件 | 结果卡、五行比例、短链复制、ICP备案页脚 |
| `frontend/src/utils` | 前端纯逻辑和工具 | `testFlowMachine.ts`、分享图、归因、五行视觉配置 |
| `frontend/src/api` | 前端 API 契约 | 请求封装、结果/匹配/后台接口类型 |
| `backend/src/main/java/com/wuxing/persona/controller` | HTTP 入口 | 结果、题目、匹配、短链、事件、后台 |
| `backend/src/main/java/com/wuxing/persona/dto` | 请求参数 | `CreateResultRequest`、`AnswerRequest`、`CreateMatchRequest` |
| `backend/src/main/java/com/wuxing/persona/service` | 核心业务 | 五行计算、120 分流、结果文案、短链、访问事件、后台统计 |
| `backend/src/main/java/com/wuxing/persona/mapper` | MyBatis SQL | 结果、短链、访问事件、统计表读写 |
| `backend/src/main/resources/db` | 数据库结构 | MySQL / H2 schema |
| `scripts` | 质量门和上线辅助 | 构建检查、移动端 E2E、预览验收、生产预检 |
| `docs` | 架构、接口、审稿和运维 | 系统文档、API 文档、人格候选稿、截图证据 |

这个项目最适合按“前端体验 -> 后端结果生成 -> 短链传播 -> 统计后台 -> 质量门”的顺序阅读，不建议一上来从数据库表开始看。

## 2.2 主流程时序

### 生成一张人格卡

```text
TestPage.vue
  -> fetchQuestions()
  -> 用户选择出生年月、可选日期时段、5 道题
  -> testFlowMachine.ts 推导按钮状态和步骤
  -> POST /api/results
  -> ResultController.create
  -> ResultService.create
  -> ElementCalculateService.calculate
  -> TestFlowStateMachine.requireReadyToSubmit
  -> WuxingCalendarTerms 年月日时五行权重
  -> PersonaArchetypeRegistry.resolve 进入 120 分流
  -> ResultTextService.build 生成用户可读结果文案
  -> user_result 入库
  -> short_link 入库
  -> Redis 缓存结果
  -> VisitEventService 记录匿名事件
  -> ResultPage.vue 展示结果
```

### 打开一条分享短链

```text
/s/{shortCode}
  -> ShortLinkController.redirect
  -> ShortLinkService.resolveAndRecord
  -> Redis 查 shortlink:code:{shortCode}
  -> 未命中时查 MySQL short_link
  -> 记录 SHORT_LINK_VISIT
  -> 302 到 /result/{resultId}
  -> ResultPage.vue
  -> GET /api/results/{resultId}
  -> Redis 查 result:{resultId}
  -> 未命中时查 MySQL user_result
  -> 返回 ResultDetailVO
```

### 后台统计

```text
用户访问 / 生成结果 / 打开短链
  -> VisitEventService 清洗和匿名化
  -> local 队列或 sync / rocketmq 模式
  -> visit_event
  -> AdminStatService 聚合 PV / UV / UIP / 漏斗 / 短链趋势
  -> AdminController
  -> AdminDashboard.vue / AdminShortLinkDetail.vue
```

## 2.3 核心类速查

| 类 / 文件 | 一句话职责 | 简历可讲点 |
| --- | --- | --- |
| `ResultService` | 创建结果的业务编排中心 | 事务边界、短链一致性、缓存降级、事件降级 |
| `ElementCalculateService` | 把出生信息和五题答案计算成五行分数 | 领域规则封装、输入边界、日期真实性校验 |
| `TestFlowStateMachine` | 后端提交流状态机 | 防止绕过前端提交半成品答卷 |
| `testFlowMachine.ts` | 前端答题交互状态机 | 左右按钮、浏览器返回、禁用态、步骤可达性统一推导 |
| `PersonaArchetypeRegistry` | 120 种人格分流注册表 | 稳定 `personaTypeId`、完整组合覆盖、文案不临时拼凑 |
| `ResultTextService` | 日主、星官、主从、点睛、天人、建议的文案组合 | 用户文案和后台字段隔离 |
| `StarOfficerService` | 根据月份给出星官记忆锚点 | 传统星宿只作文化锚点，不做现实命运判断 |
| `ShortLinkService` | 短链生成、解析、访问记录 | Provider 策略模式，内置/外部短链可切换 |
| `RedisCacheService` | 结果、短码、后台概览缓存 | cache-aside、空值缓存、缓存故障降级 |
| `VisitEventService` | 匿名访问事件处理 | 隐私保护、本地队列、MQ 预留、测试同步模式 |
| `AdminStatService` | 后台统计聚合 | PV/UV/UIP、漏斗、趋势、短链排行 |

## 2.4 后端简历表达抓手

这不是最终简历文案，但可以作为你以后提炼项目经历的技术素材：

| 亮点 | 代码证据 | 可以怎么讲 |
| --- | --- | --- |
| 答题流程状态机 | `TestFlowStateMachine`、`testFlowMachine.ts` | 将出生信息、五题答题、上一题、下一题、提交结果抽象成可测试状态，减少页面条件分支和非法提交 |
| 120 人格稳定分流 | `PersonaArchetypeRegistry` | 用 `primary + secondary + accent + relationKind` 固化 120 种结果，避免运行时临时拼凑文案 |
| 主链路事务一致性 | `ResultService.create` | 结果入库和短链创建在事务内完成，避免有结果无短链的半成品数据 |
| 缓存降级策略 | `RedisCacheService`、`ResultService.getDetail` | 结果页、短码、后台概览使用 cache-aside；Redis 异常时回退数据库，不阻塞用户主流程 |
| 匿名访问统计 | `VisitEventService`、`visit_event` | 只保存 hash 后的 client/session/ip，不保存明文个人识别信息 |
| 异步事件边界 | `VisitEventService` | 访问事件失败只降级记录日志，不影响结果生成和短链访问 |
| Provider 策略模式 | `ShortLinkService` | 内置短链和外部短链服务通过统一接口切换，降低第三方服务耦合 |
| 本地与生产质量门 | `scripts/verify-frontend-contracts.mjs`、`scripts/mobile-e2e.sh`、Maven tests | 前后端契约、移动端主流程、文案禁词、状态机边界都有自动化检查 |

### 2.5 面试讲解版技术主线

如果后面把这个项目放进后端简历，可以按下面这条线讲，重点不是“五行”本身，而是一个完整 H5 产品背后的服务端设计：

1. **状态机保护业务流程**
   - 问题：H5 页面容易出现用户跳题、浏览器返回、重复提交、前端状态和后端状态不一致。
   - 方案：前端用 `testFlowMachine.ts` 统一推导按钮和步骤；后端用 `TestFlowStateMachine` 在进入计算前确认出生年月与 5 题已完成。
   - 价值：交互逻辑和提交资格都有可测试的单一入口，后续题目数量或步骤变化更容易维护。

2. **结果生成链路可追踪**
   - 问题：一个结果页同时涉及计算、文案、短链、事件和缓存，如果散在多个入口里会很难排查。
   - 方案：`ResultService.create` 作为主编排，按“计算 -> 文案 -> 入库 -> 短链 -> 事件 -> 缓存 -> VO”顺序执行。
   - 价值：主链路清晰，出问题可以按阶段定位；事务边界也更明确。

3. **120 人格从临时拼接变成稳定注册表**
   - 问题：结果文案如果每次临时拼，会出现用户端后台词泄露、文案风格不统一、同一类结果不稳定。
   - 方案：用 `primary + secondary + accent + relationKind` 固定分流，`PersonaArchetypeRegistry` 管理 120 条模板，并通过测试禁止后台字段外泄。
   - 价值：结果可复现、可审稿、可测试，也方便后续运营迭代单个模板。

4. **访问统计不阻塞主流程**
   - 问题：访问事件、短链统计、后台报表都重要，但不能拖慢用户生成结果。
   - 方案：`VisitEventService` 支持异步写入、本地队列、MQ 预留和降级日志；Redis 缓存异常时回退数据库。
   - 价值：用户体验和统计能力解耦，适合解释为“可观测但不侵入主链路”的设计。

5. **质量门覆盖前后端契约**
   - 问题：H5 产品很容易出现后端常量改了、前端按钮没改，或者结果文案又泄露内部字段。
   - 方案：Maven tests 覆盖后端领域边界，`verify-frontend-contracts.mjs` 覆盖前端契约和状态机运行，移动端 E2E 覆盖真实主流程。
   - 价值：这不是单纯写页面，而是把产品体验、后端边界和文案安全都纳入自动检查。

## 3. 前端路由与页面职责

入口文件：

- `frontend/src/main.ts`：挂载 Vue 应用。
- `frontend/src/router/index.ts`：定义所有前端路由。
- `frontend/src/api/request.ts`：统一 API 请求、错误处理和来源归因 header。

核心路由：

| 路由 | 页面 | 职责 |
| --- | --- | --- |
| `/` | `GuidePage.vue` | 首页、开始测试、短码匹配入口 |
| `/test` | `TestPage.vue` | 出生信息、五题答题、提交结果或双人匹配 |
| `/result/:resultId` | `ResultPage.vue` | 单人人格卡结果展示、分享、短链复制 |
| `/match/:partnerShortCode/:currentShortCode` | `MatchPage.vue` | 两张人格卡的关系匹配 |
| `/admin` | `AdminDashboard.vue` | 后台概览、趋势、漏斗、短链列表 |
| `/admin/short-links/:shortCode` | `AdminShortLinkDetail.vue` | 单条短链访问明细 |
| `/:pathMatch(.*)*` | `NotFoundPage.vue` | 无效路径和无效短码兜底 |

## 4. 答题流程状态机

本轮新增两层状态机：

- `frontend/src/utils/testFlowMachine.ts`
- `backend/src/main/java/com/wuxing/persona/service/TestFlowStateMachine.java`

前端状态机负责交互体验：用户现在停在出生信息、哪一道题、能不能进入下一步、左下按钮和右下按钮应该显示什么。
后端状态机负责领域边界：一次提交进入五行计算前，必须已经完成出生年月和 5 道题，否则不会进入结果生成链路。

### 4.1 前端答题状态

它把原先散落在 `TestPage.vue` 里的步骤判断收敛成一个可推导的状态：

| 状态 | 含义 |
| --- | --- |
| `birth` | 正在填写出生年份、月份、可选日期和时段 |
| `question` | 正在回答五道价值题 |
| `submitting` | 已完成输入，正在请求后端生成结果 |
| `blocked` | 题目加载失败，不能继续答题 |
| `finished` | 流程已经完成或越界兜底 |

关键输入：

| 参数 | 来源 | 作用 |
| --- | --- | --- |
| `stepIndex` | `TestPage.vue` | 当前步骤，0 表示基础信息，1-5 表示题目 |
| `questionCount` | `GET /api/questions` | 题目数量，目前稳定为 5 |
| `birthInfoComplete` | 前端表单 | 年份和月份是否已选 |
| `activeQuestionAnswered` | 前端答案表 | 当前题是否已选择 |
| `submitting` | 提交请求状态 | 控制按钮禁用和 loading 文案 |
| `loading` | 题目加载状态 | 防止题目未加载时进入答题 |
| `questionListUnavailable` | 题目列表状态 | 题目接口失败时进入 blocked |
| `matchMode` | URL query `matchCode` | 决定最后提交到结果页还是匹配页 |

关键输出：

| 输出 | 页面使用点 |
| --- | --- |
| `primaryActionText` | 右下按钮：进入第 1 题 / 下一题 / 生成结果 |
| `previousActionText` | 左下按钮：基础信息 / 上一题 |
| `primaryActionDisabled` | 没选出生月份或没选答案时禁用 |
| `canGoPrevious` | 是否显示左下返回按钮 |
| `stepCaption` | 底部操作区当前进度 |
| `actionSummaryText` | 底部操作区辅助提示 |
| `topBackLabel` | 左上角返回按钮语义 |

前端状态转移：

| 当前状态 | 用户动作 | 守卫条件 | 下一状态 |
| --- | --- | --- | --- |
| `birth` | 点右下主按钮 | 年份和月份已选、题目已加载 | `question(1)` |
| `birth` | 点右下主按钮 | 年份或月份缺失 | 留在 `birth` 并显示错误提示 |
| `birth` | 点右下主按钮 | 题目列表为空或加载失败 | `blocked` |
| `question(n)` | 点右下主按钮 | 当前题已选择且不是最后一题 | `question(n+1)` |
| `question(n)` | 点右下主按钮 | 当前题未选择 | 留在 `question(n)` 并禁用或提示 |
| `question(last)` | 点右下主按钮 | 当前题已选择 | `submitting`，提交后进入结果页或匹配页 |
| `question(n)` | 点左下按钮 | `n=1` | 回到 `birth` |
| `question(n)` | 点左下按钮 | `n>1` | 回到 `question(n-1)` |
| `question(n)` | 浏览器返回 | 当前 history state 属于测试流 | 回到上一题，不直接跳首页 |

设计取舍：

- 本轮没有引入 XState 这类外部状态机库，因为当前状态数量少、转移规则稳定，用纯函数更轻、更容易测试，也不会给 H5 首屏引入额外包体积。
- 状态机只表达“能不能走到下一步、按钮显示什么、当前在哪一步”，不直接操作 DOM、不发请求、不写路由；这些副作用仍留在 `TestPage.vue`。
- 如果后续要做服务端草稿、断点恢复、答题会话过期，可以把现在的纯函数状态快照升级成数据库里的 `test_session`，而不是推倒重写。
- 状态机不判断答案选项是不是合法五行枚举；它只判断流程完整性。具体选项是否合法、日期是否真实、五行分数怎么加权，仍由 `ElementCalculateService` 负责。

状态机的价值：

- 页面组件只负责渲染和副作用，流程规则集中维护。
- 浏览器返回、底部返回、步骤条跳转都能走同一套步骤边界。
- 后续要加“草稿保存”“恢复答题”“中途退出提醒”时，不需要继续堆条件判断。

### 4.2 后端提交流状态

后端状态机目前保持轻量，不引入草稿表，只负责判断“这次请求是否已经具备生成结果的资格”：

| 状态 | 含义 |
| --- | --- |
| `BIRTH_REQUIRED` | 缺少出生年份或月份，不能提交 |
| `ANSWERING` | 出生信息已具备，但 5 道题未完整回答 |
| `READY_TO_SUBMIT` | 出生年月和 5 道题都完整，可以进入五行计算 |

关键类：

| 文件 | 职责 |
| --- | --- |
| `TestFlowPolicy.java` | 后端统一维护出生年份边界和题目数量 |
| `TestFlowStage.java` | 后端状态枚举 |
| `TestFlowState.java` | 状态快照，包含已答题数、要求题数、缺失题号 |
| `TestFlowStateMachine.java` | 纯函数式状态推导与 `requireReadyToSubmit` 守卫 |

接入点：

```text
ResultService.create
  -> ElementCalculateService.calculate
  -> TestFlowStateMachine.requireReadyToSubmit
  -> validateBirth
  -> 五行分数计算
```

这层设计的意义是：前端可以被绕过，但后端不会接受“缺出生信息”或“只答了部分题”的请求；以后要做服务端草稿、幂等提交、断点恢复时，也有明确的状态入口可以扩展。
状态机接入时保留了已有 API 错误契约：重复题号仍返回 `answers must contain 5 unique questions`，不会被误判成普通缺题。

本轮继续收紧了状态机的“已答题”定义：题号会按 `trim + upper-case` 规范化；只有 `Q1-Q5` 范围内的题号，并且该题存在非空选项，才计入完成度。未知题号不会把进度虚高，空选项也不会让流程误判为可提交；具体选项是不是合法五行，则继续由 `ElementCalculateService.parseElement` 负责，避免状态机和五行计算职责混在一起。

## 5. 出生年份策略

本轮把年份范围统一为：

```text
1950 <= birthYear <= 2026
```

前端位置：

- `frontend/src/utils/testFlowMachine.ts`
  - `MIN_BIRTH_YEAR = 1950`
  - `MAX_BIRTH_YEAR = 2026`
- `frontend/src/pages/TestPage.vue`
  - 滑杆、手动输入、快捷年份、刻度都使用这组边界。

后端位置：

- `backend/src/main/java/com/wuxing/persona/common/TestFlowPolicy.java`
  - `MIN_BIRTH_YEAR = 1950`
  - `MAX_BIRTH_YEAR = 2026`
  - `REQUIRED_QUESTION_COUNT = 5`
- `backend/src/main/java/com/wuxing/persona/dto/CreateResultRequest.java`
  - `@Min(TestFlowPolicy.MIN_BIRTH_YEAR)`
  - `@Max(TestFlowPolicy.MAX_BIRTH_YEAR)`
  - `@Size(min = TestFlowPolicy.REQUIRED_QUESTION_COUNT, max = TestFlowPolicy.REQUIRED_QUESTION_COUNT)`
- `backend/src/main/java/com/wuxing/persona/service/ElementCalculateService.java`
  - 服务层再次校验，避免绕过 Controller validation。

测试位置：

- `backend/src/test/java/com/wuxing/persona/service/ElementCalculateServiceTest.java`
  - 覆盖 1949 和 2027 非法。
- `scripts/verify-frontend-contracts.mjs`
  - 自动检查前后端年份边界是否同步。

## 6. 后端主链路

### 6.1 Controller 层

| 文件 | 核心方法 | 作用 |
| --- | --- | --- |
| `ResultController.java` | `create` | `POST /api/results`，创建人格结果 |
| `ResultController.java` | `get` | `GET /api/results/{resultId}`，查询结果 |
| `QuestionController.java` | `list` | 返回五道题与五行选项 |
| `MatchController.java` | `candidate` / `create` / `get` | 短码候选、创建匹配、查询匹配 |
| `ShortLinkController.java` | `redirect` | `/s/{shortCode}` 解析并 302 到结果页 |
| `EventController.java` | `record` | 记录前端行为事件 |
| `AdminController.java` | `overview` / `shortLinks` / `visits` / `exportShortLinks` | 后台统计和 CSV 导出 |

### 6.2 DTO 与参数

`CreateResultRequest` 是结果生成的核心请求体：

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `birthYear` | `Integer` | 必填，1950-2026，且不能超过当前年份 |
| `birthMonth` | `Integer` | 必填，1-12 |
| `birthDay` | `Integer` | 可选，1-31，服务层校验真实日期 |
| `birthTimeRange` | `String` | 可选，取值见 `BirthTimeRange` |
| `answers` | `List<AnswerRequest>` | 必填，固定 5 题 |

`AnswerRequest`：

| 字段 | 说明 |
| --- | --- |
| `questionCode` | `Q1` 到 `Q5` |
| `optionCode` | `METAL / WOOD / WATER / FIRE / EARTH` |

`CreateMatchRequest` 继承 `CreateResultRequest`，额外增加：

| 字段 | 说明 |
| --- | --- |
| `partnerShortCode` | 好友人格卡短码 |

接口细节见 `docs/api-spec.md`。本轮已在 `POST /api/results` 下补充 `3.1 答题状态机契约`，把前端交互状态机、后端提交流状态机、五行计算服务三者的职责边界写清楚。后续如果调整按钮文案、题目数量、年份范围或后端提交规则，需要同步更新代码、测试和 API 文档。

### 6.3 结果生成 Service

结果创建主入口：

- `backend/src/main/java/com/wuxing/persona/service/ResultService.java`
- 核心方法：`create(CreateResultRequest request, String clientId, HttpServletRequest servletRequest)`

执行顺序：

```text
ElementCalculateService.calculate
  -> StarOfficerService.byMonth
  -> ResultTextService.build
  -> insertWithResultIdRetry
  -> ShortLinkService.createForResult
  -> VisitEventService.record(TEST_SUBMIT / RESULT_CREATED / SHORT_LINK_CREATED)
  -> RedisCacheService.setResult
  -> ResultDetailVO
```

重要设计点：

- `insertWithResultIdRetry` 会处理 resultId 极小概率碰撞。
- `ResultDetailVO` 是前端结果页、分享图、匹配页共同契约。
- 结果创建和短链创建在事务里完成，避免有结果无短链。

可靠性边界：

| 事项 | 当前处理 |
| --- | --- |
| 五行计算失败 | 直接中断创建，不写入半成品结果 |
| resultId 碰撞 | 最多重试 5 次，仍失败则返回业务异常 |
| 结果入库成功但短链失败 | 事务回滚，避免产生不可分享的人格卡 |
| 访问事件写入失败 | 记录日志并降级，不让低价值统计阻塞用户拿到结果 |
| Redis 写入失败 | 记录日志并回退数据库读取，不影响主结果 |
| 结果查询缓存命中 | 直接返回缓存，同时异步记录 `RESULT_VIEW` |
| 结果查询缓存未命中 | 查库、补缓存、异步记录访问 |

这条边界可以作为后端简历里的重点：核心业务数据保持事务一致，统计和缓存属于可降级能力，失败时不影响用户主流程。

## 7. 五行计算与 120 人格分流

### 7.1 五行分数计算

入口：

- `ElementCalculateService.calculate(CreateResultRequest request)`

规则：

```text
五行初始分：每个元素 20
出生年份纳音：+8
出生月份主元素：+25
出生月份副元素：+10
出生日期日主元素：如果填写 birthDay，则 +6
出生时段元素：如果填写有效时段，则 +8
五题答案：每题对应元素 +12
```

排序后得到：

- `primaryElement`
- `secondaryElement`
- `primaryPercent`
- `secondaryPercent`
- `allElementScores`

### 7.2 120 人格注册表

入口：

- `PersonaArchetypeRegistry.java`

固定规则：

```text
primary + secondary + accent + relationKind
```

组合数量：

```text
5 个主元素
* 4 个副元素
* 3 个点睛元素
* 2 种主从关系
= 120 种人格
```

`resolve(ElementScoreResult scoreResult)` 的职责：

1. 使用五行总分确定主元素和副元素。
2. 从剩余三个元素中选择 `accentElement`。
3. 根据主副比例确定 `RelationKind`。
4. 返回稳定的 `PersonaArchetype`。

每个 `PersonaArchetype` 包含：

| 字段 | 说明 |
| --- | --- |
| `personaTypeId` | 后台稳定 id，不能直接暴露成用户文案 |
| `primaryElement` | 主元素 |
| `secondaryElement` | 副元素 |
| `accentElement` | 点睛元素 |
| `relationKind` | 主导型或均衡型 |
| `personaLabel` | 四字人格标签，必须包含“的” |
| `keywords` | 结果页标签 |
| `dayMasterFrame` | 日主说明框架 |
| `primarySecondaryText` | 主从元素关系 |
| `accentText` | 点睛元素说明 |
| `heavenText` | 内在世界 |
| `humanText` | 外部感受 |
| `growthAdvice` | 成长建议 |

### 7.3 文案生成

入口：

- `ResultTextService.build(ElementScoreResult scoreResult, StarOfficer starOfficer, CreateResultRequest request)`

关键原则：

- 用户端不能出现 `WATER-EARTH-FIRE-dominant`、`personaTypeId`、`2/5` 这类后台字段。
- 日主说明要从真实出生日期推导；没有填写日期时，不强行伪造日柱。
- 星官名称来自 `StarOfficerService.byMonth`，作为传统星宿记忆锚点，不做现实命运判断。
- 主从元素关系要用具体意象解释，例如“深潭的水”和“堤岸的土”如何互相作用。
- 点睛元素要有辨识度，不能只是“火补充活力”这种平铺直叙。

审阅产物：

- `docs/persona-archetype-catalog-20260628.md`
  - 由 `PersonaArchetypeCatalogExportTest` 从运行时注册表导出。
  - 当前包含 120 条人格标题和对应文案，用于逐条审稿。
  - 标题已统一为 4 个汉字且只包含一个“的”，避免过直白的后台分类感。
- `docs/persona-label-review-20260702.md`
  - 单独列出 120 个当前候选人格名称。
  - 按主元素、辅助元素、点睛元素、关系分类，便于优先审阅名字本身。
  - 后续如果只改名字，先改 `PersonaArchetypeRegistry` 显式标签表，再重新跑注册表测试和导出测试。
- `docs/persona-label-review-v2-20260702.md`
  - 这是“单字状态词 + 的 + 双字意象”的第二版候选名，只用于审阅，不代表已经写入后端注册表。
  - 机械校验结果为 120 条、120 个唯一、全部四字、全部只含一个“的”、未发现后台字段。
  - 如果你认可 v2 的方向，再把最终选择同步进 `PersonaArchetypeRegistry`。
- `docs/persona-label-review-v3-20260702.md`
  - 这是“状态词 + 的 + 双字意象”的第三版候选名，只用于审阅，不代表已经写入后端注册表。
  - 机械校验结果为 120 条、120 个唯一、全部四字、全部只含一个“的”、未发现后台字段。
- `docs/persona-label-review-v4-20260702.md`
  - 这是“场景 / 气质 + 的 + 核心意象”的第四版候选名，只用于审阅，不代表已经写入后端注册表。
  - v4 的目标是减少“词库自动拼接感”，让名字更像可分享的人格称号。
  - 已做二次人工修词，替换了“火边的镜”“铃后的枝”等偏生硬表达；机械校验结果为 120 条、120 个唯一、全部四字、全部只含一个“的”、未发现后台字段。
- `docs/persona-label-review-v5-20260702.md`
  - 这是“二字画面 + 的 + 一字核心”的第五版候选名，只用于审阅，不代表已经写入后端注册表。
  - v5 的目标是让名称更像有画面感的人格称号：前两个字吸收辅助元素和点睛元素的气氛，最后一个字承接主元素的核心意象。
  - 机械校验结果为 120 条、120 个唯一、全部四字、全部只含一个“的”、未发现后台字段；上线前仍需要你逐条确认审美方向。

## 8. 短链、缓存与访问统计

### 8.1 短链 Provider

入口：

- `ShortLinkService.java`

它只暴露统一业务方法：

| 方法 | 作用 |
| --- | --- |
| `createForResult(resultId)` | 给结果创建短链 |
| `resolveAndRecord(shortCode, clientId, request)` | 解析短码并记录访问 |
| `getByResultId(resultId)` | 根据结果找短链 |
| `getByShortCode(shortCode)` | 根据短码找短链 |

内部根据配置选择：

- `InternalShortLinkProvider`
- `ExternalShortLinkProvider`

这是一处适合写进简历的策略模式边界：业务层不需要知道短链来自内置算法还是外部短链服务。

### 8.2 Redis 缓存

入口：

- `RedisCacheService.java`

缓存 key：

| Key | TTL | 作用 |
| --- | --- | --- |
| `result:{resultId}` | 24 小时 | 结果页详情缓存 |
| `shortlink:code:{shortCode}` | 7 天 | 短码到结果 id |
| `shortlink:null:{shortCode}` | 5 分钟 | 无效短码空值缓存 |
| `admin:overview:v{version}:{rangeKey}` | 45 秒 | 后台概览缓存 |

设计价值：

- 结果页和短链访问属于高频读取，用 cache-aside 降低数据库压力。
- 无效短码空值缓存可以防止错误短码反复打数据库。
- 后台概览用版本号失效，避免逐 key 删除。

### 8.3 访问事件

入口：

- `VisitEventService.java`
- `VisitEventMapper.java`
- `visit_event` 表

记录内容：

- `event_type`
- `page_path`
- `result_id`
- `short_code`
- `client_id_hash`
- `session_id_hash`
- `ip_hash`
- `channel`
- `campaign`
- `device_type`
- `event_date`

隐私边界：

- 不保存明文 IP、clientId、sessionId、User-Agent。
- 统计展示的是 PV / UV / UIP，不是个人画像追踪。

投递模式：

| 模式 | 使用场景 | 行为 |
| --- | --- | --- |
| `local` | 默认生产和本地开发 | 请求线程只入本地有界队列，后台 worker 批量写库 |
| `rocketmq` | 未来访问高峰削峰 | 先发布 MQ，consumer 未接管前保持本地 shadow 写入 |
| `sync` | 集成测试和本地确定性验收 | 清洗匿名化后直接写库，避免质量门被异步调度波动影响 |

本轮把 `MvpFlowIntegrationTest` 切到 `sync`，生产默认仍是 `local`，因此短链热路径“不等待统计落库”的设计没有改变。

## 9. 数据库核心表

### 9.1 `user_result`

保存一次人格结果。

重点字段：

| 字段 | 说明 |
| --- | --- |
| `result_id` | 业务结果 id |
| `birth_year` / `birth_month` / `birth_day` / `birth_time_range` | 出生信息 |
| `answer_json` | 五题答案 |
| `primary_element` / `secondary_element` | 主副元素 |
| `primary_percent` / `secondary_percent` | 主副比例 |
| `all_element_scores_json` | 完整五行分数 |
| `persona_type_id` | 120 分流 id |
| `accent_element` / `relation_kind` | 点睛元素和关系类型 |
| `persona_label` | 四字人格标签 |
| `day_master_text` / `primary_secondary_text` / `accent_text` | 结果页主体文案 |
| `heaven_text` / `human_text` / `growth_advice_json` | 天人特质和建议 |
| `star_officer_code` / `star_officer_name` | 星官锚点 |

### 9.2 `short_link`

保存短码与结果之间的绑定。

重点字段：

| 字段 | 说明 |
| --- | --- |
| `short_code` | 6 或 7 位短码 |
| `result_id` | 对应人格结果 |
| `original_path` | 原始落地页 |
| `short_url` | 完整短链接 |
| `pv_count` / `uv_count` / `uip_count` | 访问计数 |
| `last_visit_at` | 最近访问时间 |

### 9.3 `visit_event`

保存匿名访问事件，是后台统计来源。

### 9.4 `site_daily_metric` / `short_link_daily_metric`

保存日聚合结果，让后台趋势查询不用每次扫全量事件。

## 10. 质量门与验收命令

本轮已通过：

```bash
npm --prefix frontend run build
mvn -q -f backend/pom.xml test
mvn -q -f backend/pom.xml -Dtest=TestFlowStateMachineTest,ElementCalculateServiceTest,ResultTextServiceTest,PersonaArchetypeRegistryTest,PersonaArchetypeCatalogExportTest,StarOfficerServiceTest test
mvn -q -f backend/pom.xml -Dtest=PersonaArchetypeRegistryTest,ResultTextServiceTest,PersonaArchetypeCatalogExportTest test
mvn -q -f backend/pom.xml -Dtest=PersonaArchetypeCatalogExportTest -DexportPersonaCatalog=true test
node scripts/verify-persona-label-docs.mjs
node scripts/verify-frontend-contracts.mjs
git diff --check
env E2E_BASE_URL=http://127.0.0.1:5178 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh
```

本轮总质量脚本执行情况：

- `env REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh` 已通过脚本语法检查、生成物检查、敏感文案扫描、Maven 全测、前端构建、前端契约检查、静态预览检查、离线浏览器检查。
- 该脚本停在 `scripts/mysql-schema-smoke-test.sh`，原因不是 schema 报错，而是本机 Docker Desktop 服务端对 `docker info` / `docker version` 返回 `500 Internal Server Error`。
- 两个 Docker context（`desktop-linux` 和 `default`）都复现同样错误；`docker compose ... config` 不依赖 daemon 的配置解析已通过。
- 因此 Docker 容器型 MySQL fresh schema smoke 需要在 Docker Desktop 恢复后重跑，不能作为本轮代码失败结论。

质量脚本边界：

- `scripts/verify-wuxing-preview.mjs`、`scripts/verify-wuxing-preview-flow.mjs`、`scripts/verify-wuxing-browser.mjs` 面向 `outputs/wuxing-frontend-flow-preview.html` 这类历史静态预览证据。
- 当前真实 H5 页面以 `frontend/src/pages/TestPage.vue`、`frontend/src/utils/testFlowMachine.ts`、`frontend/e2e/mobile-main-flow.spec.mjs` 和 `scripts/verify-frontend-contracts.mjs` 为准。
- 因此如果静态预览里仍出现 `question-prev/question-next` 这类旧 testid，不代表线上 Vue 页面仍用旧交互；当前 `/test` 的真实底部按钮契约是 `test-previous-action` 和 `test-primary-action`。

本轮浏览器人工验收：

- H5 `/test` 年份输入和滑条支持 `1950-2026`，运行时上限还会按当前年份收紧；如果选择当前年份，未来月份会禁用。
- 快捷年份包含 `2026` 和更早年份入口。
- 出生信息选完后，右下按钮从禁用态变为“进入第 1 题”。
- 第 1 题页面左侧为“基础信息”，右侧为“下一题”。
- 选项未选时右侧禁用，选中后右侧可点击。
- 从第 2 题按浏览器返回键，会回到第 1 题，不会直接跳回首页；如果用户直达 `/test` 且仍停在基础信息页，则浏览器返回会按原始浏览器历史处理。
- 新后端进程接口实测：重复题号仍返回 `answers must contain 5 unique questions`，状态机未破坏旧契约。
- 本轮新增移动端状态机 E2E：`test flow uses 1950 year entry and intuitive question navigation` 已通过，覆盖 1950 入口、右侧下一题、左侧上一题、浏览器返回上一题。
- 新增用例加入后，非沙箱完整移动端主流程 E2E 已重跑，`scripts/mobile-e2e.sh` 共 10 条测试全部通过。

继续完整验收建议：

```bash
git diff --check
mvn -q -f backend/pom.xml -Dtest=TestFlowStateMachineTest,ElementCalculateServiceTest,ResultServiceTest,ResultTextServiceTest,PersonaArchetypeRegistryTest test
npm --prefix frontend run build
node scripts/verify-frontend-contracts.mjs
env E2E_BASE_URL=http://127.0.0.1:5178 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh
env REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh
```

## 11. 后续架构优化方向

适合继续增强、也适合写进简历的方向：

1. 将 `TestFlowPolicy` 从编译期常量升级为可配置策略，并通过启动校验保证前端契约、后端 DTO、服务层边界一致。
2. 在当前后端状态机基础上增加“答题会话”模型，把纯提交校验扩展成可恢复的服务端 draft flow。
3. 给 `PersonaArchetypeRegistry` 增加导出接口或离线校验脚本，保证 120 条文案上线前可审阅。
4. 对 `ResultService.create` 增加请求幂等键，避免用户重复点击造成多张结果卡。
5. 对后台统计加异步聚合任务调度，让日聚合从手动 API 变成定时任务。
6. 把短链热路径压测、Redis 命中率、MySQL 查询计划整理成简历里的性能优化证据。

## 12. 本轮明确未做的事

为了避免误解，下面这些事情本轮没有擅自推进：

| 事项 | 当前状态 | 原因 |
| --- | --- | --- |
| v5 人格名称写入后端 | 已同步 | 已按你确认的 v5 名称写入 `PersonaArchetypeRegistry`，可进入本地前端验收 |
| 线上服务器部署 | 未执行 | 本轮目标是本地深度打磨和质量验证，不在你未确认前推生产 |
| 公安备案状态修改 | 未自动处理 | 需要按平台/网警流程处理，代码侧只保证备案号和 ICP 展示能力 |
| Docker fresh MySQL smoke | 未完成 | 本机 Docker daemon 返回 500，配置解析已通过，fresh schema 需 Docker 恢复后重跑 |
| 完整 120 文案人工终审 | 未完成 | 后端注册表已有 120 条并通过机械禁词/完整性测试，但最终语感仍建议按导出文档逐条审 |

## 13. 建议代码阅读路线

如果要从后端简历项目角度理解这个系统，建议按下面顺序看代码。

### 13.1 先看用户主流程

1. `frontend/src/pages/TestPage.vue`
   - 看 H5 如何收集出生信息、五题答案、匹配短码。
   - 重点看它如何调用 `testFlowMachine.ts`，而不是自己散写流程判断。
2. `backend/src/main/java/com/wuxing/persona/controller/ResultController.java`
   - 看 `POST /api/results` 如何进入后端。
3. `backend/src/main/java/com/wuxing/persona/service/ResultService.java`
   - 看创建结果、生成短链、记录事件、写缓存的完整事务边界。
4. `backend/src/main/java/com/wuxing/persona/service/ElementCalculateService.java`
   - 看出生年月、日、时段和五题答案如何变成五行分数。
5. `backend/src/main/java/com/wuxing/persona/service/PersonaArchetypeRegistry.java`
   - 看 120 种人格如何稳定分流。
6. `backend/src/main/java/com/wuxing/persona/service/ResultTextService.java`
   - 看日主、星官、主从、点睛、天人、建议如何组合成结果页文案。

### 13.2 再看传播与统计链路

1. `ShortLinkService.java`
   - 统一短链业务入口，内部可以切内部短链或外部短链服务。
2. `ShortLinkController.java`
   - `/s/{shortCode}` 解析短码并回流结果页。
3. `VisitEventService.java`
   - 匿名化访问事件、本地队列、MQ 预留和测试同步模式。
4. `AdminController.java`
   - 后台概览、趋势、短链访问明细和 CSV 导出。
5. `RedisCacheService.java`
   - 结果页、短码、后台概览缓存，以及空值缓存。

### 13.3 最后看质量门

1. `TestFlowStateMachineTest.java`
   - 流程状态机的边界测试。
2. `ElementCalculateServiceTest.java`
   - 年份、日期、重复题号、缺题等领域边界。
3. `PersonaArchetypeRegistryTest.java`
   - 120 分流完整性、标签格式、正文禁词、文案完整度。
4. `ResultTextServiceTest.java`
   - 真实结果文案不能泄露后台字段。
5. `MvpFlowIntegrationTest.java`
   - 从创建结果到短链、访问记录的集成链路。
6. `scripts/verify-frontend-contracts.mjs`
   - 前后端契约同步检查，尤其是年份范围和状态机常量。

### 13.4 明天建议阅读路线

如果你想快速把项目吃透，不建议一上来就随机翻文件。可以按下面 5 步看，每一步只抓一个核心问题：

| 步骤 | 先回答的问题 | 主要文件 | 看完应该知道什么 |
| --- | --- | --- | --- |
| 1. 用户怎么完成测试 | 页面状态怎么变化，为什么按钮在左/右下角 | `TestPage.vue`、`testFlowMachine.ts` | 出生信息、五题、上一题、下一题、提交结果是怎么被状态机统一控制的 |
| 2. 请求怎么进后端 | 前端点“生成”后进入哪个接口 | `ResultController.java`、`CreateResultRequest.java` | 请求字段、校验入口、错误响应从哪里来 |
| 3. 结果怎么被算出来 | 年月、时辰、五题如何变成主副五行 | `ElementCalculateService.java`、`WuxingCalendarTerms.java`、`ElementScoreResult.java` | 五行分数、主副比例、年份范围、真实日期校验的来源 |
| 4. 文案怎么稳定分流 | 为什么不是临时拼文案 | `PersonaArchetypeRegistry.java`、`ResultTextService.java`、`PersonaArchetypeRegistryTest.java` | 120 种人格、禁词规则、四字标签、星官说明和成长建议的生产方式 |
| 5. 数据怎么落库 | 哪些字段是用户可见，哪些字段只给后台用 | `docs/db-schema.md`、`UserResultEntity.java`、`ResultDetailVO.java` | `persona_type_id` 等内部字段为什么要存、为什么不能直接展示 |
| 6. 分享和统计怎么闭环 | 结果页如何被短链传播，后台怎么统计 | `ShortLinkService.java`、`VisitEventService.java`、`AdminController.java`、`RedisCacheService.java` | 短链、访问事件、缓存、后台看板之间的关系 |

读代码时可以用这个问题做主线：**一次用户答题，如何从 H5 状态机变成一个稳定可复访、可分享、可统计的人格结果？** 这条线跑通以后，再看样式、内容审稿和部署脚本会轻松很多。

## 14. 本轮新增质量检查记录

| 检查项 | 结论 |
| --- | --- |
| `docs/persona-label-review-20260702.md` 条数 | 120 条 |
| 标签唯一性 | 120 个唯一 |
| 标签格式 | 全部为 4 个汉字，且只含一个“的” |
| 审阅稿与后端注册表 | 完全一致 |
| 审阅稿后台字段扫描 | 未发现英文五行枚举、`dominant`、`balanced`、`personaTypeId`、`命中类型` |
| v2 名称候选稿 | `docs/persona-label-review-v2-20260702.md` 通过 120 条、唯一性、四字、“的”字、后台字段扫描 |
| v3 名称候选稿 | `docs/persona-label-review-v3-20260702.md` 通过 120 条、唯一性、四字、“的”字、后台字段扫描；该稿暂未写入后端注册表 |
| v4 名称候选稿 | `docs/persona-label-review-v4-20260702.md` 已二次人工修词，并通过 120 条、唯一性、四字、“的”字、后台字段扫描；该稿采用“场景 / 气质 + 的 + 核心意象”，暂未写入后端注册表 |
| v5 名称候选稿 | `docs/persona-label-review-v5-20260702.md` 通过 120 条、唯一性、四字、“的”字、后台字段扫描；该稿采用“二字画面 + 的 + 一字核心”，并已按独立质检反馈替换 24 个生硬名称；用户确认后已同步到后端注册表，进入本地前端验收 |
| 名称审阅稿脚本 | `node scripts/verify-persona-label-docs.mjs` 通过，覆盖 5 份 `persona-label-review*.md` 文档，并检查 001-120 编号连续性 |
| H5 年份快捷入口 | 浏览器实测 9 个入口：2026、2018、2010、2005、2002、1996、1988、1970、1950 |
| H5 年份区横向溢出 | 408px 移动视口下 `overflowX = 0` |
| H5 答题按钮位置 | 408px 移动视口实测第 1 题：左下 `test-previous-action` 为“基础信息”，右下 `test-primary-action` 为“下一题”，未选答案时右侧禁用，且两者无文本溢出 |
| 新增状态机 E2E | `env E2E_BASE_URL=http://127.0.0.1:5178 E2E_ADMIN_TOKEN=dev-token npm --prefix frontend exec -- playwright test e2e/mobile-main-flow.spec.mjs --browser=chromium -g "test flow uses 1950 year entry"` 通过 |
| 新增年份边界 E2E | `env E2E_BASE_URL=http://127.0.0.1:5178 E2E_ADMIN_TOKEN=dev-token npm --prefix frontend exec -- playwright test e2e/mobile-main-flow.spec.mjs --browser=chromium -g "latest supported birth year"` 通过，覆盖最新支持年份、当前年份未来月份禁用、当前月份可继续 |
| 完整移动端 E2E | 沙箱内 Chromium 启动被 macOS `MachPortRendezvousServer Permission denied` 拦截；同一命令在非沙箱重跑通过，`env E2E_BASE_URL=http://127.0.0.1:5178 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh` 11/11 通过 |
| 后端状态机边界 | `mvn -q -f backend/pom.xml -Dtest=TestFlowStateMachineTest,ElementCalculateServiceTest test` 通过，覆盖未知题号与空选项不计入完成度、额外未知题号最终被计算服务拒绝，并覆盖题号大小写/空格规范化与重复题号保护 |
| API 状态机契约 | `docs/api-spec.md` 已补充 `3.1 答题状态机契约`，说明前端状态机、后端状态机和 `ElementCalculateService` 的职责边界，避免维护时把交互状态、提交资格和五行计算混在一起 |
| 结果文案用户端禁词 | `mvn -q -f backend/pom.xml -Dtest=PersonaArchetypeRegistryTest,ResultTextServiceTest,PersonaArchetypeCatalogExportTest test` 通过；注册表与结果文案均禁止 `personaTypeId`、`dominant`、`balanced`、`/5`、`第二属性` 等后台感表达 |
| 前端构建 | `npm --prefix frontend run build` 通过 |
| 前端契约 | `node scripts/verify-frontend-contracts.mjs` 通过；除静态边界外，已在 Node 中真实执行 `testFlowMachine.ts`，覆盖年份夹取、步骤夹取、出生信息态、题目态、禁用态、上一题/下一题文案、最后提交态和题目加载失败态；并固化 `test-previous-action` 与 `test-primary-action` 作为底部左右按钮契约 |
| 独立 QA 复核 | 独立 QA agent 未发现阻塞问题；其提出的文档年份边界、浏览器返回边界、状态机越界分支、名称稿编号连续性已处理并复测通过 |
| 七小时工作流交接索引 | `docs/seven-hour-workflow-handoff-20260702.md` 已补充，整理明天阅读路线、核心改动、验收命令、未上线边界和下一轮建议 |
| 主质量脚本 | `scripts/quality-check.sh` 已通过 `git diff --check`、脚本语法检查、禁词扫描、后端全量测试、前端构建、前端契约、静态预览与静态浏览器检查；随后在 `scripts/mysql-schema-smoke-test.sh` 调用 Docker 时因本机 Docker API 返回 `500 Internal Server Error` 中止 |
| 工作树格式检查 | `git diff --check` 通过 |

这条审阅稿只解决“名字能不能过眼”的问题；真正上线前，仍然要结合 `docs/persona-archetype-catalog-20260628.md` 看每个名字对应的正文是否贴合。
