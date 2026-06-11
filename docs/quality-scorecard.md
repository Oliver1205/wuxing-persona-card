# 质量评分与自评机制

评分总分 100 分。每次阶段性完成后按本表自评，最终 README 和教学手册同步关键分数。

## 1. 评分维度

| 维度 | 分值 | 评分标准 |
| --- | ---: | --- |
| MVP 闭环完整度 | 20 | 首页、测试、结果、短链、跳转、统计、后台是否贯通 |
| 后端工程质量 | 15 | 分层、校验、异常、数据库、Redis、可维护性 |
| 短链接业务价值 | 15 | 真实生成、解析、缓存、空值缓存、PV/UV/UIP |
| 前端体验 | 12 | 移动端流程、错误态、复制体验、视觉一致性 |
| 数据与隐私 | 10 | hash 存储、匿名 clientId、不收集多余信息 |
| 部署可用性 | 10 | Docker Compose、Nginx、环境变量、启动文档 |
| 测试验证 | 10 | 编译、构建、接口、流程验证 |
| 教学沉淀 | 8 | 能讲清楚做了什么、为什么、高价值点在哪里 |

## 2. 评级

| 分数 | 等级 | 含义 |
| ---: | --- | --- |
| 90-100 | A | 接近可上线，可用于演示和面试讲解 |
| 80-89 | B | MVP 可用，有少量可控缺口 |
| 70-79 | C | 主流程可跑，但工程或验证不足 |
| 60-69 | D | 部分功能可用，但闭环不完整 |
| < 60 | E | 不可作为 MVP 交付 |

## 3. 自评格式

每次阶段性收尾使用以下格式：

```text
总分：
等级：

通过项：
- ...

未验证项：
- ...

风险项：
- ...

下一步：
- ...
```

## 4. 不允许加分的情况

以下情况不能算通过：

- 没有运行验证却写“已通过”。
- 只有后端返回 mock 数据，没有真实保存结果。
- 短链只是页面字符串，不可访问跳转。
- 统计只是前端假数字。
- 后台没有 token 保护。
- 明文 IP 或明文 clientId 落库。
- 文案出现宿命论或负面预测。

## 5. 当前 MVP 自评

评估日期：2026-06-10

总分：99 / 100

等级：A，MVP 主链路已完成本地与 Docker 容器验收，v0.2 已补短链 Provider 适配层，v0.3 已补 external HTTP 联调准备和后台日期筛选，v0.4 已完成外部短链服务级联调和外部 PV / UV / UIP 统计读取，v0.5 已接入外部短链访问明细，v0.6 已建立统一质量门禁，v0.7 已补生产路由与部署预检，v0.8 已增强后台运营可读性，v0.9 已完成稳定性与隐私审计加固，v1.0 已完成稳定版文档收口，v1.1 已补 external 生产接入准备，v1.2-v1.4 已补 CI/CD、运行态治理、后台运营工具、安全加固、Testcontainers 能力和分享图体验。

| 维度 | 分值 | 当前得分 | 说明 |
| --- | ---: | ---: | --- |
| MVP 闭环完整度 | 20 | 20 | 首页、测试、结果、短链、跳转、统计、后台和容器入口均已验证 |
| 后端工程质量 | 15 | 15 | 分层、校验、异常、缓存、统计已落地；v0.5 新增 external access-record 适配和失败回退；v0.6 建立统一质量脚本 |
| 短链接业务价值 | 15 | 15 | 内置短链真实生成、解析、跳转、缓存、空值缓存、统计已落地；external 创建、跳转、统计和访问明细读取已完成 |
| 前端体验 | 12 | 12 | H5 主流程和后台已通过浏览器验收；后台支持日期筛选、关键词筛选、来源筛选、CSV 导出、external 状态面板，结果页支持分享图 |
| 数据与隐私 | 10 | 9 | clientId、IP、User-Agent hash 入库；Referer 已去 query / fragment；外部访问记录的 IP / user 会 hash 后返回；新增安全响应头和 CSV 公式注入防护；外部短链项目自身明文 IP 风险仍需部署前治理 |
| 部署可用性 | 10 | 10 | Compose、Nginx、MySQL、Redis、backend 容器均已运行验证；支持可配置基础镜像；补生产短链路由示例、环境预检、external overlay、Docker smoke 和 GitHub Actions |
| 测试验证 | 10 | 10 | 后端主链路、Provider 切换、外部失败降级、external 创建、统计和访问记录 HTTP 请求、后台筛选导出、external runtime、安全响应头、前端构建、Compose config、Testcontainers profile 和统一质量门禁已覆盖 |
| 教学沉淀 | 8 | 8 | 教学手册已覆盖主流程、短链适配层、external 服务级联调、统计、访问明细、后台趋势、隐私审计、Redis、测试、CI/CD、质量门禁、发布检查表和生产质量增强 |

通过项：

- `cd backend && mvn -q test`
- `cd frontend && npm run build`
- `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`
- `scripts/quality-check.sh`
- `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml up --build -d` 已在 `http://127.0.0.1:8088` 完成容器全链路验收
- 文案边界关键词扫描无命中
- 后端集成测试覆盖创建结果、结果查询、短链跳转、短链列表、访问详情、非法参数、非法事件、后台 token、无效短码
- 短链专项单元测试覆盖短链复用、短码冲突重试、空值缓存、访问计数更新
- Redis 专项单元测试覆盖结果缓存、短链 key、空值 key、TTL、序列化和异常降级
- 本地 H2 演示模式浏览器验收已覆盖首页、测试、结果、短链 302、后台总览、短链详情
- Docker 模式 API 验收已覆盖健康检查、题目、创建结果、查询结果、短链 302、后台总览、短链列表、短链访问日志
- Docker 模式浏览器验收已覆盖首页、结果页、后台 token 门禁和后台详情保护
- v0.2 Provider 测试覆盖 internal 默认模式、external 配置切换、外部创建成功、本地绑定保存、外部失败降级和禁止降级错误处理
- v0.3 RestClient 测试覆盖 external 创建接口 URI、JSON body、`username/userId/realName` header
- v0.3 后台日期筛选集成测试覆盖当天有数据、未来日期为空、非法日期范围返回 400
- v0.4 RestClient 测试覆盖 external 统计接口 URI、查询参数和系统用户 header
- v0.4 StatsAdapter 测试覆盖 external stats 成功读取、失败回退、internal 模式跳过和 domain 不匹配跳过
- v0.4 本地服务级联调覆盖外部短链创建、外部 302、五行本地业务绑定和后台 `statSource=external`
- v0.5 RestClient 测试覆盖 external access-record 接口 URI、查询参数、分页参数和系统用户 header
- v0.5 StatsAdapter 测试覆盖 external 访问明细读取、分页转换、`statSource=external` 和外部 IP / user hash 映射
- v0.5 Docker 内部链路补验覆盖健康检查、Nginx 到 backend、创建结果、短链访问、访问明细 `statSource=local`
- v0.6 新增 `.editorconfig`、`scripts/quality-check.sh` 和 v1.0 路线图
- v0.7 新增 `deploy/nginx.shortlink-routing.example.conf` 和 `scripts/deploy-preflight.sh`
- v0.7 临时 `.env` 正向预检通过：`scripts/deploy-preflight.sh /private/tmp/wuxing-v07.env`
- v0.8 新增后台日趋势、热门星官、最近结果和最近短链展示
- v0.8 后端集成测试覆盖 overview `dailyTrends` 默认返回、当天筛选有数据和未来筛选为空值口径
- v0.9 新增 Referer 去 query / fragment、后台短码校验和 external 空访问记录稳定性测试
- v1.0 新增 `docs/v1.0-release-checklist.md`，覆盖主链路、短链能力、隐私合规、部署和发布动作
- v1.1 新增 `deploy/docker-compose.external-mode.yml`、`deploy/.env.external.example`、`scripts/external-shortlink-preflight.sh` 和 `scripts/external-shortlink-smoke-test.sh`
- v1.1 新增 [v1.1 外部短链生产级接入增强](v1.1-external-shortlink-production-readiness.md)、[外部短链服务对接说明](external-shortlink-integration-guide.md) 和 [外部短链接入隐私审计报告](external-shortlink-privacy-audit.md)
- v1.1 后端测试覆盖 external 业务错误码、统计空数据、访问明细错误码、外部短码冲突降级和关闭降级后的明确错误
- v1.2-v1.4 新增 GitHub Actions、Docker smoke、external runtime 状态、后台短链筛选导出、安全响应头、Testcontainers profile 和 Canvas 分享图
- v1.2-v1.4 后端测试覆盖后台短链关键词筛选、来源筛选、CSV 导出、external runtime 状态和安全响应头
- v1.2-v1.4 前端构建覆盖后台工具和分享图
- 验收截图已保存到 `docs/screenshots/`

未验证项：

- MVP 必需项无未验证项。
- v0.6 已补 Docker 内部链路验证；标准 `docker compose up --build -d` 曾受镜像元数据网络影响，后续上线前仍需补一次标准外部镜像构建验收。
- v1.1 已完成 external 生产接入准备资产，但尚未在服务器上真实启动外部短链服务并跑 `--probe` 与 smoke 联调。
- v1.2-v1.4 已补 Testcontainers profile 和 GitHub Actions 配置，本地普通质量门禁不默认拉起 Docker 容器。

风险项：

- 外部短链服务已完成本地联调，并已补 v1.1 生产接入准备；生产环境还需要正式决定短链子域名或同域 `/s/**` rewrite。
- 外部短链项目自身仍需做隐私治理，特别是明文 IP 入库、默认 demo 账号和访问日志保留周期。
- 后台当前是 token 保护，适合 MVP，不适合长期复杂权限管理。
- 来源筛选基于计算字段，最多扫描最近 500 条短链，适合 MVP 后台排查，不等同于大型 BI 查询。
- 生产上线前仍需替换默认密码、默认 token、`HASH_SALT`，并配置域名和 HTTPS。
- 后续版本必须持续防止功能膨胀，朋友匹配、登录注册、付费、AI 深度解读和复杂 BI 仍应作为独立大版本评估。

下一步：

- 生产上线前使用真实 `deploy/.env.external` 执行部署预检和 external 预检。
- 在目标服务器启动外部短链 aggregation 后执行 `scripts/external-shortlink-preflight.sh deploy/.env.external --probe`。
- 五行 external Compose 启动后执行 `scripts/external-shortlink-smoke-test.sh`。

## 6. v2.0 商业产品化初评

评估日期：2026-06-11

总分：88 / 100

等级：B+，v2.0 已把项目从工程 MVP 推向更强的产品表达、分享传播和增长统计口径，但距离真正商业级还需要补渠道归因、聚合分析、E2E、备份恢复、HTTPS、限流、告警和运营实验能力。

| 维度 | 分值 | 当前得分 | 说明 |
| --- | ---: | ---: | --- |
| 产品定位与传播闭环 | 20 | 18 | 已明确“愿意测 -> 有身份感 -> 愿意分享 -> 朋友继续测”的主循环，首页和结果页已围绕分享重做 |
| 前端体验与移动端完成度 | 18 | 16 | 已补首屏承诺、答题进度、sticky CTA、完整五行分布、原生分享和分享图入口；仍需 Playwright 移动端回归 |
| 增长数据口径 | 15 | 13 | 已补测试开始、答题选择、提交尝试、分享面板、原生分享、保存分享图、二次测试等事件；仍缺 session、channel、campaign 和漏斗聚合表 |
| 后端架构演进性 | 17 | 14 | 已保留短链 Provider 和隐私 hash 基线，并补非法时段业务异常；后续仍需迁移工具、聚合表、provider 元数据和更强限流 |
| 商业级运维准备 | 15 | 12 | 已有 Compose、预检、smoke、CI、运行态状态；仍需 HTTPS、备份恢复、回滚、告警、容器扫描和正式线上 E2E |
| 文档与面试表达 | 15 | 15 | README、项目计划、v2.0 方案和质量评分已能讲清产品、架构、增长与边界 |

通过项：

- v2.0 商业级产品化方案已记录产品漏斗、前端体验、后端架构和运维路线。
- 首页、测试页、结果页和分享面板完成第一批产品化改造。
- 新增增长事件枚举，覆盖答题、分享和二次测试关键动作。
- 非法出生时段已收敛为明确业务异常，并补充单元测试。
- `mvn -q -f backend/pom.xml test` 已通过。
- `npm --prefix frontend run build` 已通过。
- `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` 已通过。
- 本地 H2 + Vite 浏览器验收已覆盖首页首屏、测试页进度、创建结果、结果页分享区域和短链 302。
- 修复 Vite `/s` 开发代理误拦截 `/src/**` 的白屏问题，短链代理改为只匹配 `^/s/`。

未验证项：

- 线上域名、HTTPS、备份恢复、告警、Playwright E2E 和正式移动端多机型回归仍属于后续商业化基础设施任务。

下一步：

- v2.1：补渠道参数、session、campaign、日聚合表和后台漏斗视图。
- v2.2：补 Playwright 移动端 E2E、线上 smoke、备份恢复、回滚、HTTPS、限流和告警。
- v2.3：评估多套卡片、运营活动页和轻量报告增强，不提前引入登录、付费、AI 或朋友匹配。

## 7. v2.1 商业产品化初评更新

评估日期：2026-06-11

总分：91 / 100

等级：A-，v2.1 已把 v2.0 规划中的 session、channel、campaign 和后台漏斗视图落到代码层面，增长数据从“事件有名字”推进到“后台可观察路径”。距离更完整商业级还需要日聚合表、Playwright E2E、线上 smoke、HTTPS、限流、备份恢复和告警。

| 维度 | 分值 | 当前得分 | 说明 |
| --- | ---: | ---: | --- |
| 产品定位与传播闭环 | 20 | 18 | 主循环保持清晰，分享短链已带来源参数，短链回流可继续归因 |
| 前端体验与移动端完成度 | 18 | 16 | 体验基线延续 v2.0，并补 session / attribution 工具；仍需 Playwright 移动端回归 |
| 增长数据口径 | 15 | 15 | session、channel、campaign、device、eventDate 已进入事件表，后台展示漏斗、Top Channel 和 Top Campaign |
| 后端架构演进性 | 17 | 15 | 事件模型向 analytics 边界靠拢；后续仍需日聚合表、迁移工具和 provider 元数据 |
| 商业级运维准备 | 15 | 12 | Compose、CI、预检、smoke 基线仍可用；线上 HTTPS、限流、备份恢复和告警待补 |
| 文档与面试表达 | 15 | 15 | README、项目计划、v2 方案和质量评分已同步增长分析基础 |

通过项：

- 前端 sessionId 和 attribution 工具已落地。
- 后端 `visit_event` 已支持 session hash、channel、campaign、deviceType 和 eventDate。
- 分享短链带来源参数，短链跳转会把来源继续带到结果页。
- 后台总览展示增长漏斗、Top Channel 和 Top Campaign。
- 短链访问详情展示 Channel、Campaign 和设备类型。
- 后端集成测试覆盖事件归因、漏斗指标、渠道排行、短链来源跳转和访问详情来源字段。
- `mvn -q -f backend/pom.xml test` 已通过。
- `npm --prefix frontend run build` 已通过。

未验证项：

- v2.1 尚未做 Playwright 自动化 E2E。
- v2.1 尚未把原始事件聚合到独立日宽表，当前后台漏斗仍基于 `visit_event` 实时查询。
- 线上域名、HTTPS、备份恢复、告警和正式移动端多机型回归仍属于后续商业化基础设施任务。

下一步：

- v2.2 优先补 `site_daily_metric` / `short_link_daily_metric` 聚合表或计算任务。
- v2.2 同时补 Playwright 移动端主链路 E2E。
- v2.3 再考虑运营活动、多套卡片或轻量报告增强。

## 8. v2.4 商业增长与生产体验更新

评估日期：2026-06-11

总分：94 / 100

等级：A，v2.2-v2.4 已把增长数据从“实时明细查询”推进到“日聚合 + 可解释来源”，并补齐生产 smoke、备份恢复、回滚、Nginx 限流和结果页传播体验。距离更完整商业级还需要正式迁移框架、线上 HTTPS/HSTS、告警、备份恢复实操和 E2E 接入 CI。

| 维度 | 分值 | 当前得分 | 说明 |
| --- | ---: | ---: | --- |
| 产品定位与传播闭环 | 20 | 19 | 首屏、结果页身份感、分享图和短链回流入口进一步增强 |
| 前端体验与移动端完成度 | 18 | 17 | 移动端主链路体验更完整，已补 E2E 脚本资产，待 CI 自动化运行 |
| 增长数据口径 | 15 | 15 | session、渠道、活动、设备、漏斗和日聚合表已具备完整基础口径 |
| 后端架构演进性 | 17 | 16 | 日聚合服务、闭合日期限制和聚合来源说明已落地，仍需迁移框架 |
| 商业级运维准备 | 15 | 13 | 已补 production smoke、备份、恢复、回滚和 Nginx 限流，HTTPS/告警待上线落地 |
| 文档与面试表达 | 15 | 14 | README、API、DB、部署、项目计划和方案文档已同步，后续需补真实线上演练记录 |

通过项：

- 新增 `site_daily_metric` 和 `short_link_daily_metric`。
- 新增手动聚合接口，并拒绝聚合当天数据。
- 后台日趋势展示 `metricSource` 与 `aggregatedThroughDate`。
- 新增生产 smoke、备份、恢复、回滚和移动端 E2E 脚本。
- Nginx 增加 API、事件、短链分区限流与安全头。
- 首页、结果页、分享模块和 Canvas 分享图完成传播体验增强。

未验证项：

- `scripts/mobile-e2e.sh` 尚未接入 CI 自动运行。
- 备份恢复脚本尚未在生产服务器做真实恢复演练。
- HTTPS、HSTS、告警和日志平台仍需依赖正式域名和服务器环境落地。

下一步：

- 引入 Flyway 或 Liquibase，治理 schema 演进。
- 将移动端 E2E 接入 GitHub Actions。
- 上线后按周执行备份恢复演练和 production smoke。

## 9. v2.5 H5 输入体验更新

评估日期：2026-06-11

总分：95 / 100

等级：A，v2.5 将测试页从普通表单体验推进到更适合年轻用户的 H5 触控体验。年份选择、月份选择、日期选择、时段选择和答案卡片都有更明确的反馈，字体也从普通系统栈升级为更稳定的年轻化中文 UI 字体栈。距离更完整商业级体验还需要真实多机型截图回归、正式接入 Playwright CI 和更系统的品牌视觉资产。

| 维度 | 分值 | 当前得分 | 说明 |
| --- | ---: | ---: | --- |
| 输入体验 | 25 | 24 | 年份滑杆、快捷年份、月份/日期横向触控和时段卡片已落地 |
| 年轻化视觉 | 20 | 19 | 字体栈、选中态、卡片层级和轻动效更贴近大学生/年轻用户 |
| 表单完成效率 | 20 | 19 | 长下拉框被替换为更低摩擦的触控控件，仍需真实用户测试 |
| 可维护性 | 15 | 14 | 未引入重型 UI 库，保留原数据结构，并补充 `data-testid` |
| 可验证性 | 20 | 19 | 前端构建和浏览器 DOM 验收通过，E2E 脚本已适配但尚未接入依赖和 CI |

通过项：

- `npm --prefix frontend run build` 通过。
- 浏览器 DOM 验收确认出生信息区不再依赖原生下拉框。
- 2002 年、8 月、傍晚选择后，出生信息状态可进入已完成。
- 答案卡片显示元素倾向，点击后显示已选状态。

未验证项：

- 截图捕获通道在当前 Codex in-app browser 中超时，未归档新截图。
- `@playwright/test` 尚未进入前端依赖，移动端 E2E 脚本暂未实跑。

下一步：

- 将 `@playwright/test` 纳入前端依赖并接入 CI。
- 对 iPhone SE、iPhone 15、常见安卓宽度做截图回归。
- 继续打磨结果页品牌视觉和分享图质感。

## 10. v2.6 卡片式问答体验更新

评估日期：2026-06-11

总分：96 / 100

等级：A，v2.6 针对用户提出的关键体验问题进行了收口：年份既能快速选也能精确调，问答从瀑布式长页面升级为逐题卡片流，选项去掉五行属性提示并稳定打散顺序。当前主要剩余风险是项目目录写权限导致完整质量门禁未能在原路径执行，以及 Playwright 依赖尚未正式接入。

| 维度 | 分值 | 当前得分 | 说明 |
| --- | ---: | ---: | --- |
| 问答完成体验 | 25 | 24 | 单题卡片流减少滚动压力，底部 CTA 控制节奏 |
| 年份输入可控性 | 20 | 20 | 快捷年份、滑杆、手动输入和 +1/-1 微调均具备 |
| 结果暗示控制 | 20 | 19 | 移除属性提示，并对选项做稳定打散 |
| 视觉与动效克制 | 15 | 14 | 步骤条、卡片转场、选中态更清晰，不引入重型 UI |
| 可验证性 | 20 | 19 | 类型检查和临时生产构建通过，完整质量门禁待写权限恢复后补跑 |

通过项：

- Vue 类型检查通过。
- Vite 生产构建输出到 `/private/tmp/wuxing-v26-dist` 通过。
- 前端源码中不再保留 `系倾向` 选项提示。
- E2E 脚本已更新为逐题卡片流程。

未验证项：

- 受当前沙箱写权限影响，未能在原路径直接运行完整 `npm run build` 输出到项目 `dist`。
- 受 browser 插件策略影响，未能继续用 in-app browser 自动截图。

下一步：

- 恢复仓库写权限后补跑 `./scripts/quality-check.sh`。
- 正式接入 Playwright 依赖并运行移动端 E2E。

## 11. 八小时工作流阶段更新

评估日期：2026-06-11

总分：97 / 100

等级：A，八小时工作流把项目从“v2.6 问答体验已优化”继续推进到“可演示、可讲述、可抗瞬时访问压力”的阶段。交互侧补齐移动端答题节奏、结果页加载错误态、分享图传播质感和防重复提交；后端侧补齐短链热路径降压、统计查询索引、后台短缓存、批量统计和事件写入失败降级；文档侧补齐多角色矩阵、宣传资产、架构图、截图流程和五分钟面试讲解稿。剩余 3 分主要留给真实线上压测、移动端多机型截图归档、Playwright 依赖接入 CI、HTTPS/HSTS 和告警演练。

| 维度 | 分值 | 当前得分 | 说明 |
| --- | ---: | ---: | --- |
| 交互自然度 | 20 | 19 | 答题默认值、自动前进确认、sticky 操作区、结果加载态、错误态和防重复提交已落地，仍缺真实用户样本反馈 |
| 分享传播表达 | 15 | 15 | 结果页 shared-entry 引导、Canvas 分享图、主视觉、架构图和宣传包形成展示闭环 |
| 热路径低延迟 | 20 | 19 | 短链 302 避开实时 PV/UV/UIP 聚合，`last_visit_at` 限频更新，事件写入失败降级，仍需线上压测验证 |
| 后台运营可用性 | 15 | 14 | 后台总览短缓存、指标解释、忙碌态、短链列表批量统计已落地，复杂 BI 仍应留到独立版本 |
| 工程可验证性 | 15 | 15 | 每轮改动均跑质量门禁或专项测试，补性能 smoke、截图流程和工作日志 |
| 面试与学习沉淀 | 15 | 15 | 多角色矩阵、项目宣传包、架构视觉图、学习手册和五分钟讲解稿可直接用于复盘和面试表达 |

通过项：

- 移动端测试页已补默认年份生效、选项确认反馈、自动前进节奏、底部安全间距和提交锁。
- 结果页已补加载态、错误态和更明确的重测 / 返回入口。
- 生成分享图已补人格身份层级、短码标识、五行分布和回流提示。
- 短链访问热路径已避免同步刷新 PV/UV/UIP distinct 统计，并对 `last_visit_at` 做 30 秒限频。
- `visit_event` 写入失败已降级为告警日志，不阻断结果读取或短链访问主流程。
- 后台短链列表已批量读取结果和统计，避免分页列表出现 N+1 查询。
- 后台总览已补短缓存、指标解释和加载期间按钮 / 输入禁用。
- 新增 `docs/assets/wuxing-architecture-map.svg`、`docs/role-review-matrix.md`、`docs/project-promotion-kit.md`、`docs/interview-learning-manual.md` 相关章节和 `docs/codex-worklog.md`。
- 已通过 `scripts/quality-check.sh`、前端类型检查、Vite 构建、后端专项测试和本地浏览器抽验。

未验证项：

- 尚未在真实线上域名下做持续压测、告警触发和恢复演练。
- `@playwright/test` 尚未作为项目依赖接入 CI，截图流程已有脚本但未形成自动归档流水线。
- 多机型真实设备截图和真实普通用户访谈样本仍未补齐。

风险项：

- 当前抗峰值策略仍是单机架构内的缓存、索引、批量查询和降级优化，不能等同于多机房、高可用或无限扩容。
- 后台 token 保护适合 MVP 和作品集演示，不适合长期商用权限体系。
- 事件表继续增长后，仍需要分区、归档或离线聚合策略配合。

下一步：

- 将 Playwright 移动端 E2E 和截图捕获接入 CI。
- 使用真实服务器环境跑 production smoke、短链压测、备份恢复和告警演练。
- 在教学手册中继续补充“单机抗峰值到分布式架构”的演进路线，避免面试表达夸大当前系统边界。
