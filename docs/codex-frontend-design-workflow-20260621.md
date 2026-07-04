# Codex 前端设计工作流方案

记录日期：2026-06-21
适用项目：五行人格卡 `wuxing-persona-card`
目标：让 Codex 在 web coding 中稳定做出“你想要的前端样式”，并且能用截图、交互、契约和质量门禁证明结果可靠。

## 0. 当前状态说明

这份文档是 2026-06-21 三小时时间盒的初始方案基线，保留用于说明思路来源。

2026-06-22 起，当前有效工作流已升级为“至少十小时”版本；当前权威入口是：

- `docs/codex-frontend-start-here-20260622.md`
- `docs/codex-frontend-operating-system-20260622.md`
- `docs/codex-frontend-adoption-plan-20260622.md`
- `custom-skills/codex-frontend-workflow/SKILL.md`

后续判断是否完成时，以 `custom-skills/codex-frontend-workflow-workspace/final-audit/final-completion-audit-report.md` 和十小时时间盒为准，不以本文件的三小时描述为准。

## 1. 本轮时间盒

本轮按三小时硬时间盒组织，启动核对时间为 `2026-06-21 16:39:26 CST`，理论截止点为 `2026-06-21 19:39:26 CST`。

这类工作流的核心不是把时间耗满，而是在时间盒内完成可复核交付：

1. 搜索和确认热门前端生态与可用 skill。
2. 读取五行项目现有前端资产、页面、脚本、QA 记录。
3. 形成一套可复制给 Codex 执行的前端设计流程。
4. 给出五行人格项目的专用落地方案。
5. 标注后续执行时的验收门禁和风险边界。

## 2. 市场与生态结论

本轮使用 npm 官方下载统计 API 查询 `2026-06-14` 到 `2026-06-20` 的最近一周下载量。它不能直接等同于“最好”，但很适合判断通用度、生态成熟度和 Codex 生成代码时的可迁移性。

| 能力层 | 代表技术 | 最近一周 npm 下载量 | 结论 |
| --- | --- | ---: | --- |
| 基础语言 | `typescript` | 216,848,355 | 前端工程协作默认需要类型约束。 |
| UI 框架 | `react` | 147,219,461 | 市场通用度最高，适合通用 artifact、招聘、组件生态。 |
| 构建工具 | `vite` | 141,270,510 | 现代前端默认脚手架能力，五行项目也在使用 Vite。 |
| 样式系统 | `tailwindcss` | 121,490,373 | 快速原型和设计 token 化很强，但要防止千篇一律。 |
| 工程检查 | `eslint` | 140,981,271 | 质量门禁基础能力。 |
| 工程格式 | `prettier` | 116,913,073 | 多人协作和 AI 生成代码收敛很重要。 |
| 图标 | `lucide-react` | 85,448,032 | 通用图标生态强，适合工具按钮、后台、操作入口。 |
| 单元测试 | `vitest` | 70,820,742 | Vite 生态默认测试选择之一。 |
| 可访问组件底座 | `@radix-ui/react-dialog` | 60,420,824 | Radix 代表无样式、可访问组件底座，shadcn/ui 也基于它。 |
| 数据请求 | `@tanstack/react-query` | 58,074,107 | 复杂前端状态、缓存、加载态、错误态的成熟方案。 |
| 路由 | `react-router` | 48,643,953 | React 生态主流路由。 |
| 端到端测试 | `@playwright/test` | 42,302,116 | 浏览器验收首选之一，五行项目已采用 Playwright。 |
| 状态管理 | `zustand` | 42,902,037 | 轻量状态管理，适合中小型 React 应用。 |
| 动效 | `framer-motion` | 38,829,738 | 做高质量过渡和微交互时有价值。 |
| SSR/全栈框架 | `next` | 42,047,404 | React 生产应用常见选择。 |
| Vue 框架 | `vue` | 13,742,307 | 五行项目当前技术栈，中文社区和渐进式开发友好。 |
| Vue 路由 | `vue-router` | 7,275,383 | 五行项目当前路由栈。 |
| Vue 工具 | `@vueuse/core` | 8,704,351 | Vue 组合式工具库，后续可按需引入。 |

本轮也尝试使用 `find-skills` 指南中的 `caliber skills --query "frontend react design system tailwind vue ui ux"` 搜索社区 skill，但当前机器没有安装 `caliber`，所以不能从该 registry 拉取排名。结论改为基于本地已安装 skill、五行项目现有资产、npm 官方下载统计和主流前端资料综合判断。

推荐的通用前端能力栈不是单一框架，而是：

1. `frontend-design` 类型能力：先定审美方向和用户场景。
2. `theme / brand token` 类型能力：把视觉语言固定成颜色、字体、间距、组件层级。
3. `React + Vite + Tailwind + shadcn/ui` 类型能力：用于复杂原型和通用前端 artifact。
4. `Vue + Vite + TypeScript` 类型能力：用于五行项目真实落地。
5. `Playwright + screenshots + contract scripts` 类型能力：用浏览器和证据链验收。

## 3. 本地可用 skill 组合

本地已安装的前端相关 skill，按落地价值排序如下。

| skill | 用法 | 在本方案中的位置 |
| --- | --- | --- |
| `frontend-design` | 建立清晰审美方向，避免通用 AI 页面感。 | 每次设计任务开局必须使用。 |
| `webapp-testing` | 用 Playwright 做动态页面侦察、截图、DOM、交互验收。 | 每次交付前必须使用。 |
| `web-artifacts-builder` | React + TypeScript + Vite + Tailwind + shadcn/ui 快速构建复杂 artifact。 | 需要先做独立视觉原型时使用。 |
| `theme-factory` | 先确认主题，再统一应用颜色和字体。 | 项目缺乏明确视觉 token 时使用。 |
| `brand-guidelines` | 把品牌色、字体、fallback、层级固化为规范。 | 抽象成长期设计系统时使用。 |
| `canvas-design` | 生成海报、分享图、视觉背景、静态艺术资产。 | 五行结果卡、分享图、首屏视觉资产时使用。 |
| `algorithmic-art` | 生成可复现、可调参的视觉图形。 | 五行能量图、动态背景、仪表盘视觉时使用。 |
| `java-backend-reviewer` | 从 API、CORS、鉴权、状态码、数据口径审联调风险。 | 前端涉及接口和后台时使用。 |
| `doc-coauthoring` | 将验收结果整理成可交付记录。 | 大版本、PR、QA closeout 使用。 |

五行项目中，真实实现优先遵守现有 Vue 栈，不为了追逐热门而迁移 React。React/Tailwind/shadcn 更适合作为“快速视觉原型”或“新项目起步模板”，不是当前项目的默认改造方向。

## 4. 五行项目现有基线

五行项目已经具备一条比较成熟的前端证据链，后续流程应复用它，而不是重新发明一套轻飘飘的规范。

### 4.1 当前技术栈

- 前端：Vue 3、Vue Router 4、TypeScript、Vite、vue-tsc、Playwright。
- 路由：`/`、`/test`、`/result/:resultId`、`/match/:partnerShortCode/:currentShortCode`、`/admin`、`/admin/short-links/:shortCode`、404。
- 页面：`GuidePage`、`TestPage`、`ResultPage`、`MatchPage`、`AdminDashboard`、`AdminShortLinkDetail`。
- 组件：`QuestionCard`、`PersonaCard`、`ElementMark`、`ElementLegend`、`ElementSpectrum`、`ElementRatioCard`、`ShareLinkBox`、`StatCard`。
- API 层：统一走 `frontend/src/api/request.ts`，业务模块在 `frontend/src/api`。
- 全局视觉：`frontend/src/style.css` 通过 CSS variables、移动端优先、44px 触控目标、8px radius、中文字体 fallback 来收敛风格。

### 4.2 当前视觉资产

- `design-final/01-home.png` 到 `05-share.png`：五张高优先级视觉参考。
- `frontend/src/utils/elementVisuals.ts`：五行色板和元素字章。
- `frontend/src/utils/shareCard.ts`：`900x1200` 分享卡 Canvas 生成。
- `docs/screenshots/showcase/`：多视口截图基线。
- `outputs/wuxing-frontend-flow-preview.html`：早期五页静态流预览。

### 4.3 当前验收资产

- `scripts/quality-check.sh`：合并前主质量门。
- `scripts/frontend-live-gate.sh`：真实前后端联调和浏览器验收入口。
- `scripts/mobile-e2e.sh`：移动主流程 E2E。
- `scripts/capture-showcase-screenshots.sh`：多视口截图采集。
- `scripts/verify-frontend-contracts.mjs`：API、testid、触控、分享、后台移动端契约。
- `scripts/verify-eight-hour-artifacts.sh`：截图几何、PPT、报告和文档存在性。
- `docs/frontend-qa-record-20260619.md`：目前最完整的前端迭代 QA 记录，可作为工作日志模板。

## 5. 标准工作流

这套流程适合以后你对 Codex 说：“帮我把这个前端做成我想要的样式。”

### 阶段 0：任务定界

Codex 必须先输出这五项，不能直接开写：

1. 本次改的是哪条业务链路。
2. 用户是谁，使用场景是什么。
3. 本次视觉目标是什么，至少用一句话描述审美方向。
4. 必须保持不变的接口、数据字段、路由和测试锚点。
5. 本次验收会跑哪些命令、截哪些图、检查哪些 DOM 或交互。

五行项目示例：

```text
链路：/test 出生信息 -> 逐题问答 -> /result -> 分享短链 -> /match。
用户：手机端首次访问者，目标是快速完成测试并愿意分享。
视觉方向：克制中式、温润但不玄学，五行元素是信息结构，不是装饰堆叠。
不可破坏：/api/questions、/api/results、/s/{shortCode}、matchCode、ShareLinkBox testid。
验收：build、contract、mobile E2E、showcase screenshots、live gate、QA record。
```

### 阶段 1：上下文盘点

Codex 必须读取这些文件：

1. `frontend/src/router/index.ts`，确认页面和路径。
2. 目标页面 `.vue` 文件，确认真实 DOM、状态、交互。
3. 相关组件 `.vue` 文件，确认可复用能力。
4. `frontend/src/api` 对应模块，确认接口契约。
5. `frontend/src/style.css`，确认全局视觉规则。
6. `docs/frontend-qa-record-20260619.md`，确认已知质量标准。
7. `scripts/verify-frontend-contracts.mjs`，确认不能破坏的测试锚点。
8. `frontend/e2e/mobile-main-flow.spec.mjs` 和 `frontend/e2e/showcase-screenshots.spec.mjs`，确认浏览器验收覆盖面。

输出格式：

```text
页面链路：
- 入口：
- 状态：
- 主要组件：
- API：
- 已有测试锚点：
- 风险：
```

### 阶段 2：视觉方向确认

Codex 先给出 2 到 3 个视觉方向，不直接改代码。每个方向必须包含：

1. 视觉关键词。
2. 色彩 token 草案。
3. 字体和字号层级。
4. 卡片、按钮、输入、列表、图表规则。
5. 动效克制原则。
6. 不做什么。

五行人格项目推荐默认方向：

```text
方向名：克制中式数据感
气质：温润、可信、轻仪式感，不神神叨叨。
底色：纸白/暖灰，而不是大面积金色、红色或紫色。
主色：沉稳青绿，承载行动按钮和成功态。
辅助色：五行色只用于信息编码，不当作全屏装饰。
字体：中文系统字体优先，标题厚重但不夸张，正文可读。
边角：8px 或更小，避免泡泡感。
动效：只用于状态变化和步骤推进，不做大面积漂浮装饰。
```

### 阶段 3：设计 token 固化

不允许边写边随手配色。每次视觉改造先整理 token：

```css
:root {
  --color-bg: #f6f3ec;
  --color-text: #24302f;
  --color-primary: #2f6f5e;
  --color-accent: #d79b43;
  --radius-control: 8px;
  --control-min-height: 44px;
}
```

Codex 应将 token 映射到：

1. 全局布局：页面、shell、panel。
2. 表单控件：input、select、button。
3. 信息卡：结果卡、统计卡、短链详情卡。
4. 反馈态：loading、empty、error、disabled。
5. 移动端断点。

### 阶段 4：实现策略

实现顺序固定为：

1. 先改最小组件或最小页面切片。
2. 保留现有 API wrapper、路由和测试锚点。
3. 把重复 UI 抽成已有风格组件，而不是新建一套孤立样式。
4. 每完成一个页面切片，立刻做 build 或静态契约检查。
5. 最后统一做多视口视觉检查。

五行项目中，优先改这些层：

| 层级 | 位置 | 原则 |
| --- | --- | --- |
| 全局风格 | `frontend/src/style.css` | 只抽共性，不把页面私有样式塞全局。 |
| 视觉映射 | `frontend/src/utils/elementVisuals.ts` | 五行颜色只表达信息关系。 |
| 结果卡 | `PersonaCard.vue`、`ResultPage.vue` | 最强用户感知面。 |
| 分享区 | `ShareLinkBox.vue`、`shareCard.ts` | 要像产品能力，不像调试工具。 |
| 测试流 | `TestPage.vue`、`QuestionCard.vue` | 一屏一任务，移动端推进顺滑。 |
| 后台 | `AdminDashboard.vue`、`AdminShortLinkDetail.vue` | 密度、可读、可扫描，不做营销风。 |

### 阶段 5：浏览器侦察

使用 `webapp-testing` 的“侦察再行动”：

1. 启动前后端或确认端口已存在。
2. 访问目标页面，等待页面稳定。
3. 截图桌面和移动端。
4. 检查 DOM 是否有横向溢出。
5. 检查按钮、链接、summary、输入控件是否至少 44px。
6. 检查中文长文案是否换行合理。
7. 检查 console 和 network 错误。
8. 走一次真实主流程。

不能只说“页面能打开”。必须说明：

```text
视口：
页面：
交互：
DOM：
Network：
Console：
截图路径：
残余风险：
```

### 阶段 6：质量门

每次前端视觉交付至少跑：

```bash
npm --prefix frontend run build
node scripts/verify-frontend-contracts.mjs
```

涉及主流程、分享、匹配、后台时继续跑：

```bash
E2E_BASE_URL=http://127.0.0.1:5175 \
E2E_ADMIN_TOKEN=dev-token \
scripts/mobile-e2e.sh

E2E_BASE_URL=http://127.0.0.1:5175 \
E2E_ADMIN_TOKEN=dev-token \
scripts/capture-showcase-screenshots.sh
```

发布或 PR 前跑：

```bash
scripts/quality-check.sh
```

端口已启动且要宣称真实联调通过时跑：

```bash
FRONTEND_URL=http://127.0.0.1:5175 \
BACKEND_URL=http://127.0.0.1:48081 \
ADMIN_TOKEN=dev-token \
scripts/frontend-live-gate.sh
```

### 阶段 7：QA 记录归档

每次可见前端改造都要追加或创建 QA 记录，格式：

```text
本轮范围：
关键改动：
已验证：
截图与视觉复核：
In-App Browser DOM 复核：
未验证或环境阻塞：
下一步：
```

推荐基线：`docs/frontend-qa-record-20260619.md`。

## 6. 给 Codex 的标准提示词模板

Deprecated：下面这段只保留为 2026-06-21 三小时基线的历史样例，不再作为新任务的推荐入口。新任务请优先复制 `docs/codex-webcoding-frontend-protocol-20260622.md` 或 `docs/codex-frontend-start-here-20260622.md` 里的最新模板。

```text
请按三小时以内的严格前端工作流处理，不要直接开写。

目标：
我想把 [页面/链路] 做成 [视觉目标]，业务上必须保持 [接口/字段/路由/测试锚点] 不破坏。

你必须先做：
1. 读取 router、目标页面、组件、API wrapper、style.css、现有 QA 记录和 E2E/契约脚本。
2. 列出页面链路、前端方法、接口字段、现有测试锚点和风险。
3. 给出 2 到 3 个视觉方向，让我确认。如果我已经指定方向，就把它转成 token。

确认后你再实现：
1. 优先沿用现有 Vue/Vite/TypeScript 架构。
2. 不引入新组件库，除非你证明现有代码无法优雅完成。
3. 保留已有 testid、路由、API 契约和埋点。
4. 移动端优先，所有可点控件至少 44px。
5. 不要出现大面积紫色渐变、漂浮装饰球、过度圆角、模板化卡片堆叠、营销 landing page 感。
6. 视觉改造必须落到 token、组件、页面和截图验收。

验收必须包含：
1. npm --prefix frontend run build
2. node scripts/verify-frontend-contracts.mjs
3. 如果涉及主流程，跑 scripts/mobile-e2e.sh
4. 如果涉及可视变化，跑 scripts/capture-showcase-screenshots.sh
5. 如果涉及真实前后端联调，跑 scripts/frontend-live-gate.sh
6. 最后更新 QA 记录，说明截图路径、DOM 复核、未验证项和残余风险。

最终输出：
1. 改了哪些文件。
2. 每个页面现在达到了什么视觉效果。
3. 验收命令结果。
4. 截图或浏览器证据。
5. 哪些风险没有覆盖。
```

## 7. 五行人格项目专用落地方案

### 7.1 第一阶段：统一视觉协议

目标：把“克制中式数据感”固化为设计协议。

产出：

1. `docs/frontend-visual-system.md`，描述颜色、字体、空间、卡片、按钮、输入、后台表格和截图标准。
2. `frontend/src/style.css` token 梳理，只保留真正全局规则。
3. `frontend/src/utils/elementVisuals.ts` 明确五行色彩用途。
4. `scripts/verify-frontend-contracts.mjs` 加入视觉系统契约，例如按钮高度、禁用态、分享图尺寸、元素标识稳定性。

验收：

```bash
npm --prefix frontend run build
node scripts/verify-frontend-contracts.mjs
```

### 7.2 第二阶段：用户主流程体验打磨

目标：让 `/test -> /result -> /s -> /match` 像一个完整产品，不像功能拼装。

重点：

1. `GuidePage`：首屏只保留必要决策，剪贴板和短码入口不压迫。
2. `TestPage`：出生信息、月份、题卡推进更顺，错误态更温和。
3. `QuestionCard`：选项状态、进度、返回和继续动作清晰。
4. `ResultPage`：结果、五行解释、分享主动作层级稳定。
5. `ShareLinkBox`：主分享与备用复制继续分层，展示干净短链。
6. `MatchPage`：关系解释比五行装饰更重要。

验收：

```bash
npm --prefix frontend run build
node scripts/verify-frontend-contracts.mjs
E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh
E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh
```

### 7.3 第三阶段：后台工作台专业化

目标：后台像可运营工具，不像 H5 展示页。

重点：

1. `AdminDashboard`：指标卡、趋势、漏斗、排行、短链列表层级清晰。
2. `AdminShortLinkDetail`：移动端卡片与桌面表格各自优化。
3. 筛选、分页、导出、刷新、空态、错误态统一。
4. `includeSynthetic`、`statSource`、`perf-test` 口径显示清楚。
5. 长文本、长短码、长 campaign 不撑破布局。

验收：

```bash
npm --prefix frontend run build
node scripts/verify-frontend-contracts.mjs
E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh
```

### 7.4 第四阶段：分享图与作品集资产

目标：让用户看到的分享卡和项目对外展示图都有统一审美。

重点：

1. `shareCard.ts` 保持 `900x1200` 输出稳定。
2. 五行字章、结果标题、主元素、短链、娱乐声明布局稳定。
3. 产出 iPhone SE、Android wide、desktop 三类截图矩阵。
4. contact sheet 和展示 PPT 使用同一套视觉系统。

验收：

```bash
node scripts/verify-frontend-contracts.mjs
E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh
scripts/verify-eight-hour-artifacts.sh
```

### 7.5 第五阶段：发布前总门禁

目标：避免“本机看起来好了，但 CI 或 fresh checkout 断链”。

验收：

```bash
REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh
```

如果前后端端口已启动，再跑：

```bash
FRONTEND_URL=http://127.0.0.1:5175 \
BACKEND_URL=http://127.0.0.1:48081 \
ADMIN_TOKEN=dev-token \
scripts/frontend-live-gate.sh
```

## 8. Codex 执行时的分工方式

如果继续使用多个 agent，建议这样分：

| agent | 职责 | 禁止事项 |
| --- | --- | --- |
| 视觉 agent | 只提炼视觉方向、token、截图审美差异。 | 不改业务代码。 |
| 页面 agent | 改目标页面和组件。 | 不改 QA 脚本和后端接口。 |
| 契约 agent | 补 `verify-frontend-contracts.mjs`、E2E 和截图断言。 | 不重写视觉实现。 |
| 联调 agent | 查 API、CORS、端口、live gate、后台数据口径。 | 不凭页面加载就宣布通过。 |
| 文档 agent | 更新 QA 记录和交付说明。 | 不把未验证项写成已验证。 |

当前五行项目建议不要并行改同一个 `.vue` 文件。并行更适合“一个改页面，一个补测试，一个写文档”。

## 9. 验收标准

一个前端任务只有同时满足下面条件，才能算完成：

1. 视觉方向和 token 有记录。
2. 页面实现与目标方向一致。
3. 移动端没有横向溢出。
4. 关键可点控件至少 44px。
5. API wrapper、路由、testid、埋点没有被破坏。
6. build 通过。
7. 契约脚本通过。
8. 主流程或后台变更对应的 E2E 通过。
9. 可视变化有截图落盘。
10. QA 记录写清楚已验证和未验证。

## 10. 参考来源

- npm downloads API 固定区间示例：`https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/{package}`
- React Learn：`https://react.dev/learn`
- Vite Guide：`https://vite.dev/guide/`
- Tailwind CSS Docs：`https://tailwindcss.com/docs`
- shadcn/ui Docs：`https://ui.shadcn.com/docs`
- Radix UI Primitives：`https://www.radix-ui.com/primitives`
- Playwright Docs：`https://playwright.dev/docs/intro`
- 五行项目现有基线：`docs/development-standards.md`、`docs/local-preview-runbook.md`、`docs/frontend-qa-record-20260619.md`、`docs/ci-browser-e2e-plan.md`
