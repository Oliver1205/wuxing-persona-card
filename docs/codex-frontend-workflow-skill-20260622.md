# Codex 前端工作流 Skill 接入记录

记录日期：2026-06-22
关联方案：`docs/codex-frontend-design-workflow-20260621.md`

## 目标

把“让 Codex 稳定做出想要的前端样式”从一次性文档升级为长期可复用 skill。后续只要用户提出前端样式、UI/UX、移动端 H5、后台界面、截图验收、可复用前端流程等任务，就应优先触发这套工作流。

## Skill 名称

`codex-frontend-workflow`

## 安装位置

源码位置：

```text
/Users/linyuxiang/JavaBackend/06_Tools/skills/custom-skills/codex-frontend-workflow
```

Codex 可触发位置：

```text
/Users/linyuxiang/.codex/skills/codex-frontend-workflow
```

已验证源码副本与安装副本 `diff -qr` 一致。

## 文件结构

```text
custom-skills/codex-frontend-workflow/
├── SKILL.md
├── README.md
├── MAINTENANCE.md
├── evals/evals.json
├── evals/trigger-evals.json
├── references/frontend-failure-modes.md
├── references/project-reference-template.md
├── references/wuxing-persona-card.md
└── templates/
    ├── frontend-brief-quality-gate.md
    ├── frontend-project-onboarding.md
    ├── frontend-prompt-pack.md
    ├── frontend-qa-record.md
    ├── frontend-usage-scenarios.md
    └── frontend-task-brief.md
```

## 触发场景

使用这套 skill 的典型请求：

- “帮我把这个页面做成我想要的样式。”
- “这个前端太像 demo，按产品级体验重做。”
- “继续优化五行人格卡的结果页、分享页、匹配页。”
- “做一个长期可复用的前端开发流程。”
- “要有多 agent 分工、验收命令、截图和最终汇报格式。”
- “不要只说好看，要用浏览器和截图证明。”

## 默认工作流

1. Brief Gate：判断 proceed、proceed with assumptions、ask first 或 split。
2. 任务定界：明确 flow、用户场景、视觉方向、不可破坏的接口/路由/testid、验收证据。
3. 读取真实项目：router、页面、组件、API wrapper、style.css、E2E、QA 记录。
4. 视觉方向：先确定审美关键词、token、组件规则、动效和反目标。
5. 实现切片：先 token 和共享组件，再页面局部，不破坏现有契约。
6. 浏览器验收：桌面/移动、主流程、DOM、console、network、截图。
7. 质量门：build、contract、mobile E2E、showcase screenshots、live gate、quality check。
8. QA 归档：记录范围、改动、命令、截图、DOM 复核、未验证项和残余风险。

## 五行项目专用基线

项目参考文件在 skill 的：

```text
references/wuxing-persona-card.md
```

该参考文件固定了：

- 当前 Vue 3 / Vite / TypeScript / Playwright 技术栈。
- `/`、`/test`、`/result/:resultId`、`/match/...`、`/admin`、`/admin/short-links/:shortCode` 路由。
- “克制中式数据感”的视觉基线。
- `design-final/*.png`、`docs/screenshots/showcase`、`shareCard.ts` 等视觉资产。
- `scripts/quality-check.sh`、`scripts/frontend-live-gate.sh`、`scripts/mobile-e2e.sh`、`scripts/capture-showcase-screenshots.sh` 等验收入口。

市场技能基线：

```text
docs/frontend-market-skills-research-20260622.md
```

该文档记录 2026-06-14 到 2026-06-20 的 npm 官方周下载基线，并补充 Stack Overflow Developer Survey 2025 的使用率和技术采纳因素，用来指导 TypeScript、Vite、Playwright、React/Vue、Tailwind 思维、数据请求、组件系统和图标库的优先级判断。

通用原则已经同步到 `SKILL.md` 的 `Technology Principles`：热门技术只作为决策信号，不作为迁移命令；现有项目优先尊重当前栈，greenfield 才按场景选择 TypeScript、Vite、Next.js、TanStack Query、Storybook 或图标库。

## 使用模板

任务开始时可以直接贴：

```text
请使用 codex-frontend-workflow skill。
目标：[页面/链路]
我想要的视觉效果：[描述]
必须保留：[接口/路由/testid/埋点/数据字段]
时间盒：[例如 3 小时 / 10 小时]
验收要求：[build / E2E / screenshots / live gate / QA 记录]
```

五行项目专项提示词包：

```text
docs/codex-frontend-operating-system-20260622.md
docs/codex-webcoding-frontend-protocol-20260622.md
docs/codex-frontend-prompt-pack-20260622.md
custom-skills/codex-frontend-workflow/templates/frontend-prompt-pack.md
```

其中 `docs/codex-frontend-operating-system-20260622.md` 是人和 Codex 都能读取的总控入口，用来解释材料顺序、任务分流、执行阶段、五行落地表、验收分级和最终完成定义。
`docs/codex-webcoding-frontend-protocol-20260622.md` 用于 webcoding 会话，规定第一条消息、任务分流口令、验收口令和最终汇报格式。

## 当前 eval 集

已创建 9 个内容评测用例和 25 个 trigger 边界用例：

1. Vue + Vite H5 页面从 demo 感升级为产品级移动端体验。
2. 五行人格卡结果页、分享区、匹配页继续按克制中式风格优化。
3. 生成长期复用的前端工作流模板，包含多 agent 分工、验收命令和最终汇报格式。
4. 小型 CSS 溢出修复走轻量路径，不展开完整流程。
5. TypeScript build 报错不强行触发视觉工作流。
6. React/Vite 运营看板从零搭建时第一屏就是可用工具，不做落地页。
7. 截图参考还原时先保护接口、路由和 E2E 锚点。
8. 后台访问明细移动端溢出时按运营工具表格/卡片策略处理。
9. 真实 UI 最终验收时使用 visual QA scorecard，避免用 build 通过或“看起来不错”替代视觉完成。

trigger 边界用例覆盖：

- 五行项目、H5、dashboard、截图验收、QA closeout、移动端触控等正例。
- Java 后端 review、Word、Excel、算法、纯 README、纯 build/type bug、纯 CSS 概念解释等负例。

每个用例都有 `with_skill` 与 `without_skill` 对照输出，workspace：

```text
custom-skills/codex-frontend-workflow-workspace/iteration-1
```

第一轮自动评分：

| 对照 | 通过数 |
| --- | ---: |
| with skill | 15/15 |
| without skill | 13/15 |

静态 review HTML：

```text
custom-skills/codex-frontend-workflow-workspace/iteration-1/review.html
```

结论：skill 对普通 Vue H5 polish 场景提升最明显，能强制先做任务框架、真实项目读取和视觉方向确认；五行专项与长期模板 prompt 本身约束较强，所以 baseline 也能通过多数检查。

第二轮补充评测：

```text
custom-skills/codex-frontend-workflow-workspace/iteration-2/benchmark.md
```

- `compact-start`：验证小型 greenfield dashboard 场景能短而准地进入任务。
- `minimum-timebox`：验证至少十小时时间盒会记录开始时间和最早收口时间，不提前声明完成。

第三轮全量 9 eval benchmark：

```text
custom-skills/codex-frontend-workflow-workspace/iteration-3/benchmark.md
```

| 对照 | 通过数 |
| --- | ---: |
| with skill | 45/45 |
| without skill | 27/45 |

覆盖场景：

- Vue/Vite H5 polish。
- 五行结果页/分享区/匹配页。
- 长期复用流程模板。
- 小型 CSS 视觉修复轻量路径。
- 纯 build/type 调试不触发视觉流程。
- greenfield React/Vite 工具第一屏。
- 截图参考还原。
- 后台表格移动端适配。
- 视觉 QA scorecard 最终验收。

## 打包产物

官方 `package_skill.py` 需要 `PyYAML`，当前系统 Python 3.9.6 和 Codex bundled Python 3.12.13 都缺少 `yaml` 模块，所以官方打包暂时环境阻塞。

已生成 zip 兼容 `.skill` 备份包：

```text
custom-skills/codex-frontend-workflow-workspace/dist/codex-frontend-workflow.skill
```

包内包含：

- `codex-frontend-workflow/SKILL.md`
- `codex-frontend-workflow/README.md`
- `codex-frontend-workflow/MAINTENANCE.md`
- `codex-frontend-workflow/references/wuxing-persona-card.md`
- `codex-frontend-workflow/references/frontend-failure-modes.md`
- `codex-frontend-workflow/references/project-reference-template.md`
- `codex-frontend-workflow/references/projects/dashboard-tool.md`
- `codex-frontend-workflow/templates/frontend-task-brief.md`
- `codex-frontend-workflow/templates/frontend-brief-quality-gate.md`
- `codex-frontend-workflow/templates/frontend-qa-record.md`
- `codex-frontend-workflow/templates/frontend-prompt-pack.md`
- `codex-frontend-workflow/templates/frontend-project-onboarding.md`
- `codex-frontend-workflow/templates/frontend-usage-scenarios.md`
- `codex-frontend-workflow/templates/frontend-visual-qa-scorecard.md`

root `evals/` 已排除，符合官方 packager 的默认排除意图。

当前 zip 兼容包 SHA256：

```text
以 custom-skills/codex-frontend-workflow-workspace/package-verification/skill-package-report.md 为准。
```

包内 archive entries 和内容文件数以 `verify_skill_package.mjs` 的最新报告为准。

包内容验证报告：

```text
custom-skills/codex-frontend-workflow-workspace/package-verification/skill-package-report.md
```

当前验证结果：

- content files: 以最新报告为准
- expected content files: 以最新报告为准
- forbidden eval entries: 0
- unexpected files: 0
- source/package content match: yes

## Trigger eval 报告

```text
custom-skills/codex-frontend-workflow-workspace/trigger-eval/trigger-eval-report.md
```

当前结果：

- total: 26
- positives: 13
- negatives: 13
- passed: 26/26
- false positive: 0
- false negative: 0

## 本轮 QA 记录

```text
docs/frontend-workflow-qa-record-20260622.md
```

该记录保存本轮验证命令、eval 结果、打包状态、未验证项和十小时时间盒约束。

## 后续维护

1. 每次发现 Codex 在前端工作里“跳过视觉确认”“只跑 build 不看浏览器”“忘记截图/QA 记录”，先更新 `references/frontend-failure-modes.md`，必要时再回到 `SKILL.md` 补规则。
2. 每次五行项目新增页面、脚本或验收标准，应同步更新 `references/wuxing-persona-card.md`。
3. 每次修改 skill 源码后，要重新复制到 `~/.codex/skills/codex-frontend-workflow` 并做 `diff -qr`。
4. 最终收口前按 `MAINTENANCE.md` 的 Release Checklist：先刷新 package evidence，再刷新 project evidence，最后运行 final audit。
5. 如果后续安装 `PyYAML`，重新运行官方 `package_skill.py` 生成正式包。
6. 十小时工作流结束前，不把当前 goal 标记为 complete。
