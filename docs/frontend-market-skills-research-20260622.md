# 2026-06-22 热门前端技能基线

## 数据口径

记录时间：2026-06-22
下载区间：2026-06-14 到 2026-06-20
来源：npm 官方 downloads API。
补充来源：Stack Overflow Developer Survey 2025 官方 Technology 页面。

本记录用于指导 Codex 后续前端工作流的默认判断：哪些能力应该优先纳入任务定界、技术选型、验收和交付表达。它不是要求五行人格卡项目迁移技术栈，也不是 npm 全生态排名。

热门度只作为选型信号，不能替代现有项目栈、用户任务、维护成本和验收证据。

样本说明：下表是 24 个手选前端相关包的样本内排序，选择标准是“对 Codex 前端工作流有直接启发”的框架、构建、类型、质量、浏览器验收、状态/数据、组件/动效和可视化能力。不同生态的包不可直接等价比较；例如 React 专属包只能说明 React adapter/package signal，不能推导为 Vue、Svelte 或静态项目的默认依赖。

## 复核记录

最近一次完整 fixture 复核时间：2026-06-22 04:09:47 CST。
最近一次 live spot-check 时间：2026-06-22 07:51 CST。

复核方式：官方 npm downloads API 固定日期区间。不要用 `last-week` 复核这份表；`last-week` 是滚动口径，未来会返回新的自然周数据。

```bash
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/react'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/vue'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/@angular/core'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/svelte'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/next'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/vite'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/typescript'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/tailwindcss'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/@tanstack/react-query'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/framer-motion'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/lucide-react'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/storybook'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/playwright'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/webpack'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/zustand'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/three'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/cypress'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/eslint'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/prettier'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/vitest'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/react-router'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/@radix-ui/react-dialog'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/@playwright/test'
curl -s 'https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/@vueuse/core'
```

复核结论：

- 下载区间仍为 `2026-06-14` 到 `2026-06-20`。
- 非 scoped 包和 scoped 包均已通过 npm 官方 API 查询；scoped 包使用单包 URL 单独复核。
- 复核值与下表记录一致。
- 2026-06-22 07:51 CST 额外 live spot-check 了 `typescript`、`react`、`vite` 三个固定区间 API 响应，返回值分别为 `216848355`、`147219461`、`141270510`，与 fixture 和下表一致。
- 周下载量不是用户满意度，也不等同于项目适配度；它只作为生态体量和 Codex 默认能力覆盖面的信号。

原始 JSON 摘要：

```json
{"downloads":147219461,"start":"2026-06-14","end":"2026-06-20","package":"react"}
```

完整 npm 基线 fixture：`custom-skills/codex-frontend-workflow-workspace/market-research/npm-download-baseline-20260614-20260620.json`，包含 24 个包的 `package`、`downloads`、`source_url` 和 `retrieved_at`。`verify_market_research_reproducibility.mjs` 默认做本地 fixture consistency：逐项比对本表的下载量文本、固定来源 URL 和 fixture。它默认不 live 请求 npm 或 Stack Overflow；上面的 live spot-check 只是抽样外部源复核，不等同于 24 包全量 live 重算。需要端到端来源复核时，单独按上面的固定 URL 或官方页面重新查询。

Stack Overflow Technology 页面复核口径：打开官方 Technology 页面，读取 `Web frameworks and technologies` 与 `Other tools` / platforms 相关表格中的 `All Respondents` 百分比；本文件只保存对前端 workflow 有直接决策价值的人工摘录条目。该部分是人工摘录，当前未机器复算；官方页面：`https://survey.stackoverflow.co/2025/technology/`。

## npm 周下载基线

| 样本内排序 | 包 | 周下载量 | 工作流含义 |
| ---: | --- | ---: | --- |
| 1 | `typescript` | 216,848,355 | 默认把类型契约、DTO、组件 props 和 API wrapper 当成前端质量核心。 |
| 2 | `react` | 147,219,461 | greenfield 或招聘通用场景优先覆盖 React 思维和组件组合能力。 |
| 3 | `vite` | 141,270,510 | 本地开发、预览、构建和代理验证默认按现代 Vite 工作流思考。 |
| 4 | `eslint` | 140,981,271 | 代码质量、可维护性和团队规则需要进入前端完成定义。 |
| 5 | `tailwindcss` | 121,490,373 | 代表 token 化、响应式 utility 和设计约束思路；不代表必须引入 Tailwind。 |
| 6 | `prettier` | 116,913,073 | 统一格式应自动化，避免把审美讨论浪费在缩进和换行。 |
| 7 | `lucide-react` | 85,448,032 | React adapter/package signal：图标按钮、工具栏和状态操作优先用成熟图标库；Vue/Svelte/静态项目先查现有依赖或等价本地方案。 |
| 8 | `vitest` | 70,820,742 | Vite 项目的单测、组件逻辑和轻量回归测试应优先可运行。 |
| 9 | `playwright` | 63,206,317 | 浏览器、截图、DOM、移动端视口和 E2E 证据应成为前端完成定义的一部分。 |
| 10 | `@radix-ui/react-dialog` | 60,420,824 | React adapter/package signal：无头组件和可访问交互模式值得学习；非 React 项目先找当前栈已有组件或更小替代。 |
| 11 | `@tanstack/react-query` | 58,074,107 | React adapter/package signal：数据请求、缓存、加载态、错误态和失效刷新是产品级前端的重要能力；非 React 项目先查现有 server-state 层。 |
| 12 | `webpack` | 50,748,665 | 仍要能读懂 legacy 构建和老项目约束，但新项目不默认选它。 |
| 13 | `react-router` | 48,643,953 | 路由参数、嵌套路由、返回路径和深链都是交互完成的一部分。 |
| 14 | `zustand` | 42,902,037 | 轻量状态管理在中小型交互工具里很常见，适合避免过度架构。 |
| 15 | `@playwright/test` | 42,302,116 | Playwright test runner 进一步证明浏览器级验收应自动化。 |
| 16 | `next` | 42,047,404 | SEO、服务端渲染、全栈 React、内容站或复杂产品站需要考虑 Next.js。 |
| 17 | `framer-motion` | 38,829,738 | 动效要服务反馈和状态转换，不把动效当装饰。 |
| 18 | `storybook` | 17,854,183 | 复杂组件库、后台控件和设计系统可用 Storybook 做组件级证据。 |
| 19 | `vue` | 13,742,307 | 五行项目当前技术栈；后续改造应尊重 Vue 3 / Vite / TypeScript 现实。 |
| 20 | `three` | 11,819,077 | 3D 或可视化场景用成熟渲染库，不手写低层图形逻辑。 |
| 21 | `@vueuse/core` | 8,704,351 | Vue 项目可借鉴组合式工具沉淀交互状态和浏览器能力。 |
| 22 | `cypress` | 7,125,850 | 仍是常见 E2E 工具；本项目当前更偏 Playwright。 |
| 23 | `@angular/core` | 6,023,227 | 企业级 Angular 生态仍需识别，但不作为五行项目路线。 |
| 24 | `svelte` | 4,719,222 | 新项目可关注其体验优势，但当前五行项目不迁移。 |

## Stack Overflow 2025 辅助信号

Stack Overflow Developer Survey 2025 的 Technology 页面把使用率和开发者态度放在同一份调查中，适合补足 npm 下载量的盲区。

### Web frameworks and technologies

| 排名信号 | 技术 | All Respondents 使用率 | 工作流含义 |
| ---: | --- | ---: | --- |
| 1 | Node.js | 48.7% | 前端工作流经常要读懂 Node 工具链、dev server、脚本和本地代理。 |
| 2 | React | 44.7% | React 是通用前端能力重点，但五行项目仍以 Vue 现实栈为准。 |
| 3 | jQuery | 23.4% | 旧系统仍常见，Codex 需要识别 legacy DOM 约束。 |
| 4 | Next.js | 20.8% | SEO、SSR 和内容型产品站才优先考虑，不作为所有项目默认。 |
| 5 | Express | 19.9% | 前端联调常遇到 Node API 层和代理层，需要把接口契约写清。 |
| 6 | ASP.NET Core | 19.7% | 企业项目可能不是纯 JS 后端，前端验收要贴合真实后端栈。 |
| 7 | Angular | 18.2% | 企业级框架常见，适合作为跨项目识别能力，不迁移五行项目。 |
| 8 | Vue.js | 17.6% | 五行项目当前栈仍有广泛生态基础。 |

### Platforms and build tools

| 技术 | All Respondents 使用率 | 工作流含义 |
| --- | ---: | --- |
| npm | 56.8% | 安装、脚本、锁文件和包审计是默认前端交付面。 |
| Vite | 25.4% | 与五行项目现状一致，优先沉淀 Vite build、preview、proxy 和 E2E。 |
| Webpack | 18.4% | 旧项目仍需要读懂 Webpack，但 greenfield 不默认选它。 |

### 技术采纳原因

Stack Overflow Survey 2025 的 technology adoption 相关图表中，技术背书因素包括 `Easy-to-use API`、`Robust and complete API`、`Reputation for quality`、`Reliability and low latency` 等。这里作为人工摘录的辅助信号，不作为 npm 下载量的替代来源。对应到本工作流：

- 选型不能只看热门；必须检查 API 完整度、可靠性、迁移成本和项目契约。
- Codex 交付时要说明为什么沿用现有栈、为什么新增依赖、为什么拒绝迁移。
- 对五行人格项目，Vue 3 / Vite / TypeScript / Playwright 是已验证基础，React/Next/Tailwind/Radix/TanStack 只作为方法参考或 greenfield 候选。

## 技能分层

### 必备层

- TypeScript：接口、状态、DTO、props、路由参数和测试 fixture 不靠猜。
- Vite：开发端口、代理、preview、build 和 HMR 都必须纳入验收。
- Playwright：用真实浏览器证明页面状态，不只看代码或 build。
- ESLint / Prettier / Vitest：把质量、格式和轻量回归测试放进可重复命令。
- 设计 token：颜色、间距、字号、圆角、阴影和触控目标先系统化，再改页面。

### 产品体验层

- React / Vue 组件化：根据项目现状选择，不为追热点迁移。
- Tailwind 思维：把视觉规则拆成 utility/token，但在已有 CSS 项目里优先沿用现有样式系统。
- 图标库：按钮能用熟悉符号就不要用文字堆满工具栏。
- 状态管理：先判断本地状态、URL 状态、服务端状态，不急着上全局 store。

### 复杂项目层

- TanStack Query：适合高频数据、缓存、刷新、加载态和错误态统一的 React 项目。
- Storybook：适合沉淀组件状态、视觉回归和设计系统文档。
- Next.js：适合 SEO、内容、SSR/RSC 或全栈 React 项目，不适合作为所有前端默认选项。
- Three.js：只在真实 3D 或沉浸式场景使用，并且必须有 canvas 截图和像素级非空验证。

## 对五行人格卡的落地选择

五行人格卡当前是 Vue 3 / Vite / TypeScript / Playwright 项目，所以本轮策略是：

- 不迁移 React、Next.js 或 Tailwind。
- 继续使用现有 `frontend/src/style.css` 和 `frontend/src/utils/elementVisuals.ts` 管理视觉 token。
- 借鉴 Tailwind 的 token 化和响应式约束思想，而不是引入新 CSS 框架。
- 借鉴 TanStack Query 的状态分层思想，把加载态、错误态、缓存态和刷新行为写清楚。
- 继续把 Playwright、showcase screenshots、mobile E2E 和 live gate 当作前端验收核心。
- 图标和控件优先遵守项目现有依赖；新增依赖必须有明确交互收益。
- 新增依赖前必须先检查现有 `package.json`，说明交互收益、可替代方案、验证命令和回滚方式；热门度本身不能作为新增依赖理由。

## Codex 默认决策

后续收到前端任务时：

1. 先判断是现有项目改造还是 greenfield。
2. 现有项目优先尊重当前栈、路由、接口、测试和样式系统。
3. greenfield 工具类应用优先考虑 TypeScript + Vite + 真实第一屏工具体验。
4. 需要 SEO 或内容站时再考虑 Next.js。
5. 需要复杂数据缓存时考虑 TanStack Query 或项目已有数据层。
6. 需要组件系统时考虑 Storybook 或项目已有文档站。
7. 任何视觉改造都必须最终落到浏览器、移动端视口、截图、DOM 和 QA 记录。

## 参考链接

- npm downloads API fixed range: `https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/...`
- Stack Overflow Developer Survey 2025 Technology: `https://survey.stackoverflow.co/2025/technology/`
- React: `https://react.dev/`
- Vue: `https://vuejs.org/`
- Vite: `https://vite.dev/guide/`
- TypeScript: `https://www.typescriptlang.org/docs/`
- Tailwind CSS: `https://tailwindcss.com/docs`
- Playwright: `https://playwright.dev/docs/intro`
- Storybook: `https://storybook.js.org/docs`
- TanStack Query: `https://tanstack.com/query/latest/docs/framework/react/overview`
- shadcn/ui: `https://ui.shadcn.com/docs`
- Radix UI: `https://www.radix-ui.com/primitives/docs/overview/introduction`
- lucide: `https://lucide.dev/guide/packages/lucide-react`
