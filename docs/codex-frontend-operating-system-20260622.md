# Codex 前端操作系统（通用流程 + 五行适配版）

记录日期：2026-06-22
关联 skill：`codex-frontend-workflow`
适用范围：webcoding、Codex 本地项目、五行人格卡、后续可复用前端项目。通用 runtime 规则在 `custom-skills/codex-frontend-workflow/`；本文件额外包含五行人格卡 adapter 证据和收口口径。

## 一句话目标

把“我想要这个前端样式”变成一套可重复执行的工程流程：先定界，再读真实项目，再定视觉方向，再薄切片实现，最后用浏览器、截图、DOM、E2E、QA 记录证明结果。

## 总入口

以后做前端任务，先按这个顺序找材料：

| 用途 | 文件 |
| --- | --- |
| 一页式起点 | `docs/codex-frontend-start-here-20260622.md` |
| Codex 自动触发规则 | `custom-skills/codex-frontend-workflow/SKILL.md` |
| 前端失败模式与纠偏 | `custom-skills/codex-frontend-workflow/references/frontend-failure-modes.md` |
| 最终采纳方案 | `docs/codex-frontend-adoption-plan-20260622.md` |
| 证据矩阵 | `docs/codex-frontend-evidence-matrix-20260622.md` |
| webcoding 会话协议 | `docs/codex-webcoding-frontend-protocol-20260622.md` |
| 前端审美 brief 库 | `docs/codex-frontend-style-brief-library-20260622.md` |
| 前端真实任务样例库 | `docs/codex-frontend-real-task-examples-20260622.md` |
| 复制给 Codex 的五行专项提示词 | `docs/codex-frontend-prompt-pack-20260622.md` |
| 新项目前 15 分钟接入 | `custom-skills/codex-frontend-workflow/templates/frontend-project-onboarding.md` |
| 视觉 QA 评分卡 | `custom-skills/codex-frontend-workflow/templates/frontend-visual-qa-scorecard.md` |
| 五行项目怎么跑 | `docs/frontend-workflow-playbook.md` |
| 五行当前文件地图 | `docs/frontend-current-surface-map-20260622.md` |
| 五行当前 token 盘点 | `docs/frontend-token-inventory-20260622.md` |
| 五行视觉怎么保持一致 | `docs/frontend-visual-system.md` |
| 热门技术怎么取舍 | `docs/frontend-market-skills-research-20260622.md` |
| 本轮安装/eval/打包证据 | `docs/codex-frontend-workflow-skill-20260622.md` |
| 本轮 QA 证据 | `docs/frontend-workflow-qa-record-20260622.md` |
| 交付物清单 | `custom-skills/codex-frontend-workflow-workspace/artifact-manifest/artifact-manifest-report.md` |
| 十小时完成审计 | `docs/codex-frontend-ten-hour-completion-audit-20260622.md` |
| skill 维护规则 | `custom-skills/codex-frontend-workflow/MAINTENANCE.md` |

## 工作流路由

每个请求先分流，不同请求走不同强度。

| 请求形状 | 路线 | 成功证据 |
| --- | --- | --- |
| 想要某种样式、截图还原、产品级 polish | 完整前端工作流 | Task Frame、视觉方向、代码改动、浏览器截图、QA 记录。 |
| 一个小视觉 bug | 轻量修复 | 目标文件定位、最小 CSS/组件改动、聚焦视口检查。 |
| 纯 build/type/lint 报错 | 非视觉调试 | 报错复现、类型/构建修复、对应命令通过。 |
| 从零做 app/tool/dashboard | Greenfield 前端 | 第一屏就是可用工具、本地 URL、桌面/移动端验证。 |
| 发布前或 QA closeout | 验证流程 | build、contract、E2E、截图、DOM、console/network、未验证项。 |
| README-only、后端-only、Word/Excel、算法 | 不触发此 skill | 使用对应领域流程。 |

## 任务启动模板

最稳定的启动方式：

```text
请使用 codex-frontend-workflow skill。

目标：[页面/链路/工具]
我想要的视觉效果：[关键词、参考截图、不要什么]
用户场景：[谁在什么设备上完成什么任务]
必须保留：[路由/API/testid/埋点/数据字段/脚本]
时间盒：[例如 3 小时 / 至少 10 小时]
验收要求：[build / typecheck / E2E / screenshots / live gate / QA 记录]

先输出 Task Frame 和要读取的文件。视觉方向不明确时给 2-3 个方向确认；明确时直接转成 token 和第一片实现计划。
```

## 固定执行阶段

### 1. Task Frame

必须先说明：

- Flow：用户走哪条链路。
- User and scenario：谁在什么设备上做什么。
- Desired visual direction：想要的审美和反目标。
- Must preserve：不能破坏的接口、路由、字段、testid、E2E、统计口径。
- Acceptance evidence：用什么证明完成。
- Files to inspect：先读哪些文件。

### 2. 读取真实项目

Codex 先读真实项目，不按框架刻板印象猜。

现有项目前端最少读取：

- router / routes。
- 页面入口。
- 目标组件。
- API wrapper。
- 全局样式和 token。
- E2E / screenshot / contract 脚本。
- QA 记录和 runbook。
- 当前 git status。

五行项目默认读取清单以 `docs/frontend-workflow-playbook.md` 为准。

### 3. 视觉方向

如果用户没有明确方向，先给 2-3 个方向；如果用户已有方向，直接转成：

- 关键词。
- 色彩 token。
- 字体层级。
- 页面密度。
- 组件规则。
- 动效规则。
- 反目标。

五行项目默认方向：

```text
克制中式数据感：暖纸底、深绿主行动、五行色只做信息编码、移动端一屏一任务、后台高密度可扫描。
```

### 4. 薄切片实现

顺序固定：

1. 保护路由、API wrapper、字段、testid、埋点、可访问性语义。
2. 更新 token 或共享组件。
3. 修改目标页面。
4. 同步契约脚本或 E2E。
5. 跑浏览器、截图和 QA 记录。

禁止：

- 为视觉效果改接口字段。
- 多个 agent 同时改同一个页面文件。
- 只跑 build 就声称视觉完成。
- 把未验证项写成已验证。

### 5. 浏览器证据

视觉或交互工作必须有浏览器证据，最低看：

- 桌面视口。
- 手机视口，至少 iPhone SE 或 390 宽。
- 无横向溢出。
- 文字不重叠。
- 触控目标通常不小于 44px。
- Console / Network。
- 截图路径或 DOM 检查结果。

### 6. QA 记录

非平凡前端改造必须留下 QA 记录。

推荐路径：

```text
docs/frontend-qa-record-YYYYMMDD.md
```

记录必须区分：

- 已验证。
- 环境阻塞。
- 未覆盖风险。
- 残余风险。

不要把环境问题写成代码失败，也不要把没跑的脚本写成通过。

### 7. Visual QA Scorecard

当用户需要判断“这个前端是不是我想要的样式”时，在最终回复或 QA 记录里附上 `templates/frontend-visual-qa-scorecard.md` 的结果。

硬门禁：

- 真实页面或 fixture 已渲染。
- 桌面和移动范围被检查或明确不适用。
- overflow、文字重叠、触控和主任务被检查。
- 旧路由、API 字段、testid、统计和可访问性语义没有被误破坏。
- 命令、URL、截图、DOM、console/network、未验证项被记录。

20 分评分项：

- brief fit。
- product task clarity。
- visual hierarchy。
- token consistency。
- responsive behavior。
- component states。
- accessibility and touch。
- data integrity。
- browser health。
- handoff quality。

## 热门技术如何使用

npm 周下载量和 Stack Overflow 2025 调查共同说明 TypeScript、React、Vite、Tailwind、Playwright、TanStack Query、Next.js、Storybook、Vue 等能力值得纳入判断，但不等于迁移命令。

默认原则：

- 现有项目先尊重当前栈。
- 五行项目继续使用 Vue 3 / Vite / TypeScript / Playwright。
- Tailwind 作为 token 化和响应式约束思路，不默认引入。
- React / Next.js 更适合 greenfield、SEO 或全栈 React 场景。
- TanStack Query 只在复杂服务端状态、缓存、刷新、错误态成为核心时考虑。
- Storybook 适合组件库或复杂控件状态，不是每个小页面都需要。
- Playwright 是浏览器证据的核心能力，尤其适合截图、DOM、移动端视口和 E2E。

## 多 Agent 编排

大任务才用多 agent，小修不要拆太碎。

| Agent | 输入 | 输出 | 禁止 |
| --- | --- | --- | --- |
| Context | 需求、路由、页面、API、QA 文档 | 文件地图和契约清单 | 改代码。 |
| Visual | 用户审美、截图、视觉系统 | token、组件规则、反目标 | 改业务逻辑。 |
| Page | 目标页面和组件 | 页面薄切片改动 | 改无关文件。 |
| Contract | E2E、contract、截图脚本 | 验收脚本和断言 | 重写视觉。 |
| Integration | 端口、代理、后端、CORS、live gate | 联调结果和环境归因 | 凭页面打开宣布通过。 |
| Docs | QA 记录、README、索引 | 可追踪证据 | 夸大验证。 |

## 五行项目落地表

| 场景 | 先读 | 视觉重点 | 必要证据 |
| --- | --- | --- | --- |
| 首页 `/` | `GuidePage.vue`、`style.css` | 首屏主行动、手动短码匹配、低压迫引导 | build、contract、showcase。 |
| 测试 `/test` | `TestPage.vue`、`QuestionCard.vue` | 一屏一任务、选中反馈、44px 触控 | build、contract、mobile E2E。 |
| 结果 `/result/:resultId` | `ResultPage.vue`、`PersonaCard.vue`、`ElementRatioCard.vue` | 身份先被看见，五行比例再解释 | build、contract、mobile E2E、截图。 |
| 分享区 | `ShareLinkBox.vue`、`shareCard.ts` | 分享像产品能力，不像调试工具 | 分享图、短链参数、无二次分享盒。 |
| 匹配 `/match/...` | `MatchPage.vue` | 关系解释优先，五行服务于关系说明 | mobile E2E、showcase。 |
| 后台 `/admin` | `AdminDashboard.vue`、`StatCard.vue` | 高密度可扫描、筛选和导出清楚 | contract、showcase、live gate。 |
| 短链详情 | `AdminShortLinkDetail.vue` | 长 referer/campaign 不撑破，移动端可读 | DOM overflow、截图、live gate。 |

## 验收分级

| 改动类型 | 最低验收 | 不能替代的证据 |
| --- | --- | --- |
| 文档/流程 | 文件存在、链接可搜索、JSON 可解析 | 不能证明真实页面没问题。 |
| 小样式 | build、目标视口检查 | 不能省略浏览器。 |
| 页面结构 | build、contract、目标页浏览器 | 不能只看截图。 |
| H5 主流程 | build、contract、mobile E2E | 不能只打开首页。 |
| 视觉截图 | screenshot 脚本、DOM 几何检查、visual QA scorecard | 不能只说“看起来可以”。 |
| 前后端联调 | live gate 或 preview smoke | `quality-check.sh` 不能替代真实代理/短链联调。 |
| 发布前 | tracked quality gate、QA 记录 | 不能隐瞒未验证项。 |

## 十小时时间盒纪律

当用户要求“至少十小时”：

1. 记录开始时间。
2. 计算最早 complete 时间。
3. 未到时间前不能把整体 goal 标记 complete。
4. 可以阶段性完成 skill、eval、文档、截图、QA、review。
5. 时间窗口内继续做有价值补强。
6. 最终完成前必须按需求逐项审计，不用“已经做了很多”替代完成证明。

## 失败模式纠偏

长期复用时，先用 `custom-skills/codex-frontend-workflow/references/frontend-failure-modes.md` 检查工作是否跑偏。

当前固定纠偏清单：

- 小修不要过度展开成完整流程。
- build 通过不能替代视觉证明。
- 避免默认紫蓝渐变、装饰球、嵌套卡片和泛化 AI 审美。
- 视觉改造前必须保护路由、接口、字段、testid、埋点和 E2E。
- 参考截图要抽取约束，不要机械照抄。
- 热门依赖不能因为热门就引入。
- 移动端必须复核溢出、触控和长文本。
- 文档交付必须留下文件、命令、证据路径和未验证项。
- 源码 skill、安装 skill、`.skill` 包不能漂移。
- 至少十小时时间盒不能提前 complete。

本轮时间盒：

```text
开始：2026-06-22 00:13:45 CST
最早收口：2026-06-22 10:13:45 CST
续跑机制：`date`、`get_goal` 实时状态、ordered closeout 和 final audit；不把未验证 heartbeat 当完成证据
```

## 当前证据

| 证据 | 当前结果 |
| --- | --- |
| skill 安装一致性 | `verify_frontend_workflow_assets.mjs` 通过。 |
| 内容 eval | 9 eval，with skill `45/45`，baseline `27/45`。 |
| trigger eval | 26/26，false positive 0，false negative 0。 |
| artifact manifest | 83/83，README/docs-site 入口全部可发现。 |
| ordered closeout | `run_ordered_closeout_check.mjs` 已生成 `phase_closeout_passed_not_ready`，避免手工或并行顺序读到旧报告。 |
| workflow doctor | `run_frontend_workflow_doctor.mjs` 已生成 `phase_healthy_not_release_ready`，并由 `verify_frontend_workflow_doctor.mjs` 确认与当前审计一致。 |
| requirement audit | `phase_requirements_accounted_for`，12/14 已证明，evidence blocking 0，开放项均为发布态阻塞。 |
| Wuxing docs verifier | 19 个 tracked/untracked workflow 文档全部通过存在性和 trailing whitespace 检查。 |
| 非五行 fixture | dashboard fixture 桌面 1280x820 与移动 390x844 浏览器验证通过；无横向溢出、控件 >=44px，截图已落盘。 |
| final completion audit | `run_final_completion_audit.mjs` 已生成报告；当前状态 `complete_ready`，23/23。 |
| `.skill` 包 | 18 个 archive entries、14 个内容文件；root `evals/` 排除；包内容与源码 SHA 一致。 |
| `.skill` SHA256 | `a0cf0f954e8ce705b0fe0612acc9633bdf5633a4be3abb54e8701e16d321f6a2`。 |
| release readiness | `not_ready`，严格发布门禁要求 17 个 local-only workflow 文档被 add/commit。 |
| 五行项目索引 | README 和 docs-site 已接入前端流程文档。 |
| 当前文件地图 | `docs/frontend-current-surface-map-20260622.md` 已从真实路由、API 类型、E2E 和 contract 脚本整理。 |
| 当前 token 盘点 | `docs/frontend-token-inventory-20260622.md` 已从 `style.css` 和 `elementVisuals.ts` 整理。 |
| 十小时完成审计 | `docs/codex-frontend-ten-hour-completion-audit-20260622.md` 已更新为本地完成态。 |
| 页面视觉改造 | 本轮还未修改真实 H5 页面，后续进入页面改造必须重新跑浏览器和截图。 |
| 最终采纳方案 | `docs/codex-frontend-adoption-plan-20260622.md` 已把入口、执行阶段、验收、维护和最终确认清单串联。 |
| 证据矩阵 | `docs/codex-frontend-evidence-matrix-20260622.md` 已把要求、证据、验证方式和当前判断逐项对应。 |

## 后续复用方式

在任何新前端项目中：

1. 使用 `custom-skills/codex-frontend-workflow/templates/frontend-project-onboarding.md` 做前 15 分钟接入。
2. 用 `docs/codex-frontend-style-brief-library-20260622.md` 把审美词翻译成 style brief。
3. 使用 `custom-skills/codex-frontend-workflow/references/project-reference-template.md` 建项目参考文件。
4. 填写栈、路由、视觉基线、技术取舍、先读文件、命令和坑点。
5. 把项目特有提示词放入项目 docs，不污染通用 `SKILL.md`。
6. 第一次真实 UI 改造后，补一条 eval 或 trigger eval。
7. 第一次真实 UI 验收后，把 `frontend-visual-qa-scorecard.md` 结果写进 QA 记录。
8. 每次误触发或漏触发，更新 `trigger-evals.json`。
9. 每次 workflow 行为变更，跑 `grade_full_benchmark.py`。
10. 每次 runtime 文件变更，同步安装副本、刷新 `.skill` 包、记录 SHA。
11. 最终收口前按 `MAINTENANCE.md` Release Checklist 重跑 package evidence、project evidence、final audit 和 release readiness。

## 最终完成定义

这个前端操作系统真正完成时，需要同时满足：

- skill 可触发、可降级、可维护。
- 有市场技能基线，但不盲目追新。
- 有通用流程，也有五行项目级落地。
- 有提示词包，用户可以直接复用。
- 有 eval 和 trigger 边界证据。
- 有 QA 记录和安装/打包证据。
- 有十小时时间盒完整执行记录。
- 如果进入真实页面改造，还必须有浏览器、截图、DOM 和 E2E 证据。
