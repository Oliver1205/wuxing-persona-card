# 2026-06-22 前端工作流 QA 记录

## 本轮范围

- 将长期可复用的 Codex 前端流程沉淀为 `codex-frontend-workflow` skill。
- 将五行人格卡项目接入该流程，形成项目级视觉系统、Playbook 和索引入口。
- 用 eval、安装校验、打包产物和文档链接证明流程可复用，而不是只停留在口头方案。
- 吸收独立审查意见：补充任务分流、负触发边界、通用技术决策原则，并把五行 Playbook 明确为项目级流程。
- 遵守用户要求的至少十小时时间盒：目标开始时间为 `2026-06-22 00:13:45 CST`，最早整体收口时间为 `2026-06-22 10:13:45 CST`。

路径口径：`docs/...` 是 `wuxing-persona-card` 仓库内路径；`custom-skills/...` 是 workspace 根目录 `/Users/linyuxiang/JavaBackend/06_Tools/skills` 下的路径。

## 关键改动

- 新增 reusable skill 源码：`custom-skills/codex-frontend-workflow/`。
- 同步安装到 Codex skill 目录：`/Users/linyuxiang/.codex/skills/codex-frontend-workflow`。
- 新增 skill 维护手册：`custom-skills/codex-frontend-workflow/MAINTENANCE.md`。
- 新增失败模式与纠偏经验：`custom-skills/codex-frontend-workflow/references/frontend-failure-modes.md`。
- 新增 brief 质量门：`custom-skills/codex-frontend-workflow/templates/frontend-brief-quality-gate.md`，用于判断 proceed / proceed with assumptions / ask first / split。
- 新增真实任务样例模板：`custom-skills/codex-frontend-workflow/templates/frontend-usage-scenarios.md`。
- 新增五行真实任务样例库：`docs/codex-frontend-real-task-examples-20260622.md`。
- 新增非五行 dashboard fixture 和浏览器证据：
  - `custom-skills/codex-frontend-workflow-workspace/fixtures/dashboard-tool/index.html`
  - `custom-skills/codex-frontend-workflow-workspace/verify_dashboard_fixture.mjs`
  - `custom-skills/codex-frontend-workflow-workspace/verify_dashboard_fixture_artifacts.mjs`
  - `custom-skills/codex-frontend-workflow-workspace/fixture-verification/dashboard-fixture-report.md`
- 在 `custom-skills/codex-frontend-workflow/MAINTENANCE.md` 新增 Release Checklist，明确最终收口必须重新生成 package evidence、project evidence，并最后运行 final audit。
- 新增交付物清单和校验脚本：
  - `custom-skills/codex-frontend-workflow-workspace/frontend-workflow-artifact-manifest.json`
  - `custom-skills/codex-frontend-workflow-workspace/verify_artifact_manifest.mjs`
  - `custom-skills/codex-frontend-workflow-workspace/verify_skill_package.mjs`
  - `custom-skills/codex-frontend-workflow-workspace/run_final_completion_audit.mjs`
  - `custom-skills/codex-frontend-workflow-workspace/run_release_readiness_check.mjs`
  - `custom-skills/codex-frontend-workflow-workspace/artifact-manifest/artifact-manifest-report.md`
  - `custom-skills/codex-frontend-workflow-workspace/package-verification/skill-package-report.md`
  - `custom-skills/codex-frontend-workflow-workspace/final-audit/final-completion-audit-report.md`
  - `custom-skills/codex-frontend-workflow-workspace/release-readiness/release-readiness-report.md`
- 新增项目内流程文档：
  - `docs/codex-frontend-operating-system-20260622.md`
  - `docs/codex-frontend-adoption-plan-20260622.md`
  - `docs/codex-frontend-evidence-matrix-20260622.md`
  - `docs/codex-webcoding-frontend-protocol-20260622.md`
  - `docs/codex-frontend-style-brief-library-20260622.md`
  - `docs/codex-frontend-ten-hour-completion-audit-20260622.md`
  - `docs/frontend-current-surface-map-20260622.md`
  - `docs/frontend-token-inventory-20260622.md`
  - `docs/frontend-market-skills-research-20260622.md`
  - `docs/codex-frontend-prompt-pack-20260622.md`
  - `docs/frontend-visual-system.md`
  - `docs/frontend-workflow-playbook.md`
  - `docs/codex-frontend-workflow-skill-20260622.md`
- 更新入口：
  - `README.md`
  - `docs-site/index.html`

## 已验证

| 命令或检查 | 结果 | 说明 |
| --- | --- | --- |
| `date '+%Y-%m-%d %H:%M:%S %Z'` | 通过 | 本轮检查时间锚点：`2026-06-22 00:58:42 CST`。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_frontend_workflow_assets.mjs` | 通过 | skill 必需文件、模板、参考文档和打包产物存在。 |
| `node -e ... evals/evals.json trigger-evals.json ...` | 通过 | `evals.json` 和 `trigger-evals.json` 均可解析。 |
| `git -C wuxing-persona-card diff --check` | 通过 | tracked diff 无 whitespace error；未跟踪文档另由 `verify_wuxing_frontend_workflow_docs.mjs` 覆盖。 |
| `diff -qr custom-skills/codex-frontend-workflow /Users/linyuxiang/.codex/skills/codex-frontend-workflow` | 通过 | skill 源码与安装副本已在本轮早段验证一致。 |
| `unzip -l custom-skills/codex-frontend-workflow-workspace/dist/codex-frontend-workflow.skill` | 历史记录 | 早段包为 15 个 archive entries、12 个内容文件；最终以本表后续最新复核和 `skill-package-report.md` 为准。 |
| `shasum -a 256 custom-skills/codex-frontend-workflow-workspace/dist/codex-frontend-workflow.skill` | 历史记录 | 早段包 SHA；最终以本表后续最新复核和 `skill-package-report.md` 为准。 |
| `curl -s https://api.npmjs.org/downloads/point/last-week/...` | 历史记录 | 早段只取得主包和 `@tanstack/react-query`；后续已用 scoped 单包 URL 完整复核，最终以市场基线文档为准。 |
| `curl -s https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/...` | 通过 | 使用固定日期区间重新复核非 scoped 包和 scoped 单包；数值与市场基线文档一致，避免 `last-week` 滚动口径漂移。 |
| `curl -s https://api.npmjs.org/downloads/point/2026-06-14:2026-06-20/{typescript,react,vite}` | 通过 | 2026-06-22 07:51 CST live spot-check 返回 `216848355`、`147219461`、`141270510`，与 fixture 和市场文档一致；这是抽样复核，不是 24 包全量 live 重算。 |
| `node custom-skills/codex-frontend-workflow-workspace/trigger_eval_report.mjs` | 通过 | 26 条 trigger eval 全部通过，false positive 0，false negative 0；已覆盖 style brief、visual QA scorecard 正负边界。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_market_research_reproducibility.mjs` | 通过 | 固定 npm 日期区间、24 包机器基线 fixture、表格下载量、固定来源 URL、Stack Overflow 人工摘录口径和“不迁移”规则均通过。 |
| `python3 custom-skills/codex-frontend-workflow-workspace/grade_iteration.py` | 通过 | iteration-1 metadata 已同时输出 `expectations` 和 `assertions` 字段。 |
| `python3 custom-skills/codex-frontend-workflow-workspace/grade_full_benchmark.py` | 通过 | 全量 9 eval：with skill 45/45，without skill 27/45。 |
| `rg codex-frontend-operating-system README.md docs-site/index.html docs` | 通过 | 总控文档已从 README、docs-site 和 Skill 接入记录可发现。 |
| `rg codex-webcoding-frontend-protocol README.md docs-site/index.html docs` | 通过 | Webcoding 协议已从 README、docs-site、操作系统和 Skill 接入记录可发现。 |
| `rg codex-frontend-ten-hour-completion-audit README.md docs-site/index.html docs` | 通过 | 完成审计已从 README、docs-site 和操作系统可发现。 |
| `rg frontend-current-surface-map README.md docs-site/index.html docs` | 通过 | 当前文件地图已从 README、docs-site、操作系统和 Playbook 可发现。 |
| `rg frontend-token-inventory README.md docs-site/index.html docs` | 通过 | Token 盘点已从 README、docs-site、操作系统、视觉系统和 Playbook 可发现。 |
| `date '+%Y-%m-%d %H:%M:%S %Z'` | 通过 | 总控入口巡检时间：`2026-06-22 01:29:18 CST`。 |
| `date '+%Y-%m-%d %H:%M:%S %Z'` | 通过 | Webcoding 协议巡检时间：`2026-06-22 01:34:26 CST`。 |
| `date '+%Y-%m-%d %H:%M:%S %Z'` | 通过 | 完成审计巡检时间：`2026-06-22 01:37:29 CST`。 |
| `date '+%Y-%m-%d %H:%M:%S %Z'` | 通过 | 当前文件地图巡检时间：`2026-06-22 01:40:12 CST`。 |
| `date '+%Y-%m-%d %H:%M:%S %Z'` | 通过 | Token 盘点巡检时间：`2026-06-22 01:45:02 CST`。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_skill_package.mjs` | 通过 | `.skill` 包 18 个 archive entries、14 个内容文件；无 `evals/`；14 个内容文件与源码 SHA 一致。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_wuxing_docs_git_visibility.mjs` | 通过/报告型 | 19 个入口/文档目标中 2 个 tracked，17 个 local-only，0 missing；发布或 GitHub Pages 前需用 `REQUIRE_WUXING_DOCS_TRACKED=1` 严格检查并提交。 |
| `node custom-skills/codex-frontend-workflow-workspace/run_final_completion_audit.mjs` | 通过 | 十小时后最终复跑生成 `complete_ready`；23/23 通过；requirement audit 已从 final audit 中拆出以避免旧报告循环依赖。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_artifact_manifest.mjs` | 通过 | 83 个核心交付物全部存在，README/docs-site 入口全部可发现。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_frontend_workflow_assets.mjs` | 通过 | 总校验已纳入 artifact manifest、package verifier、Wuxing docs verifier、dashboard fixture artifacts、最终审计脚本、failure modes、证据矩阵、通用/项目分层和语义护栏断言；审计时间以 final audit 报告 `generated at` 为准。 |
| `node custom-skills/codex-frontend-workflow-workspace/run_ordered_closeout_check.mjs` | 通过 | 有序执行 assets、project onboarding、market fixture consistency、artifact manifest、package、final audit、release readiness、requirement audit、workflow doctor、workflow doctor verifier；同一 `run_id` 串起报告链，当前 `phase_closeout_passed_not_ready`，phase healthy yes。 |
| `node custom-skills/codex-frontend-workflow-workspace/run_frontend_workflow_doctor.mjs` | 通过 | 一条命令汇总 final audit、release readiness、requirement audit、manifest、package、market、trigger、benchmark 和 Git 可见性；当前 `phase_healthy_not_release_ready`。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_frontend_workflow_doctor.mjs` | 通过 | 校验 workflow doctor 报告晚于当前 final audit、release readiness、requirement audit、artifact manifest、package、market、benchmark 和 Git visibility，并确认同一 `run_id`、状态、计数、SHA、benchmark、trigger 与源报告一致。 |
| `node custom-skills/codex-frontend-workflow-workspace/run_release_readiness_check.mjs` | 预期失败 | 当前 `not_ready`；final audit 已 `complete_ready`，但 17 个 workflow 文档仍是 local-only。 |
| `node custom-skills/codex-frontend-workflow-workspace/run_requirement_completion_audit.mjs` | 通过 | 原始要求 14 项中 12 项已证明，2 项开放为 Git 可见性和 release readiness；evidence blocking 为 0，release blocking 为 2；脚本校验 release readiness 晚于 final audit，避免读到旧报告还误判通过。 |
| final audit JSON parse set | 通过 | 当前 final audit 内置口径为 14 个 JSON 文件，包含 npm baseline fixture；doctor JSON 由 `verify_frontend_workflow_doctor.mjs` 在 closeout 后校验。 |
| `diff -qr custom-skills/codex-frontend-workflow /Users/linyuxiang/.codex/skills/codex-frontend-workflow` | 通过 | 最新复核源码与 Codex 安装副本一致。 |
| `unzip -l custom-skills/codex-frontend-workflow-workspace/dist/codex-frontend-workflow.skill` | 通过 | 最新复核为 18 个 archive entries、14 个内容文件，已包含多 Agent 拆分模板、`frontend-brief-quality-gate.md`、`frontend-visual-qa-scorecard.md`、增强后的新项目 onboarding、project reference 模板和非五行 dashboard reference。 |
| `shasum -a 256 custom-skills/codex-frontend-workflow-workspace/dist/codex-frontend-workflow.skill` | 通过 | SHA256 更新为 `a0cf0f954e8ce705b0fe0612acc9633bdf5633a4be3abb54e8701e16d321f6a2`。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_wuxing_frontend_workflow_docs.mjs` | 通过 | 覆盖 README、docs-site、本轮 tracked/untracked workflow 文档，19/19 通过，检查文件存在和 trailing whitespace。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_dashboard_fixture.mjs` | 通过 | 通过提权运行 Playwright Chromium 生成非五行 dashboard fixture 浏览器证据；desktop 1280x820 与 mobile 390x844 均无横向溢出、控件 >=44px。 |
| `node custom-skills/codex-frontend-workflow-workspace/verify_dashboard_fixture_artifacts.mjs` | 通过 | 常规审计检查 dashboard fixture report JSON 和 desktop/mobile screenshot artifacts。 |
| `env REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh` | 通过 | 2026-06-22 10:18 CST 最终复跑通过；五行项目主质量门禁包含 `git diff --check`、脚本语法检查、后端 `mvn test`、前端 build、frontend contracts、预览浏览器检查、MySQL schema smoke 和 Docker compose config。 |
| `npm --prefix frontend run build` | 通过 | 2026-06-22 15:33 CST 视觉落地后通过；生成 `dist/assets/index-BHkLtgH4.css` 和 `dist/assets/index-Dh4piE8U.js`。 |
| `node scripts/verify-frontend-contracts.mjs` | 通过 | 2026-06-22 15:33 CST 视觉落地后通过；保留路由、API、testid、分享、后台移动报表和 typography 约束。 |
| `curl -s -i http://127.0.0.1:48082/api/readiness` | 通过 | 2026-06-22 15:34 CST 本地后端 `48082` readiness 200；核心表 `user_result`、`short_link`、`visit_event`、`site_daily_metric`、`short_link_daily_metric` 均 ok。 |
| `env E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh` | 通过 | 2026-06-22 15:35 CST 9 个 Chromium 移动主流程用例全绿。 |
| `env E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token SHOWCASE_SCREENSHOT_DIR=docs/screenshots/showcase scripts/capture-showcase-screenshots.sh` | 通过 | 2026-06-22 15:36 CST 11 个 Chromium showcase 截图用例全绿，刷新 iPhone SE、430 宽安卓、桌面结果页和后台统计截图。 |
| `npm --prefix frontend run build` | 通过 | 2026-06-22 16:00 CST A/C 混合字体规则落地后通过；生成 `dist/assets/index-B1SoXVZ0.css` 和 `dist/assets/index-Cs-kSwcg.js`。 |
| `node scripts/verify-frontend-contracts.mjs` | 通过 | 2026-06-22 16:00 CST A/C 混合字体规则落地后通过；固定字号、testid、移动布局和 typography 约束仍通过。 |
| `env E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token SHOWCASE_SCREENSHOT_DIR=docs/screenshots/showcase scripts/capture-showcase-screenshots.sh` | 通过 | 2026-06-22 16:06 CST A/C 混合字体规则落地后 11 个 Chromium showcase 截图用例全绿，并刷新截图。 |

## 语义护栏检查

`verify_frontend_workflow_assets.mjs` 现在不只检查文件存在，还断言以下长期复用护栏：

- `SKILL.md` 必须保留多 Agent 决策表、并行改同文件的禁止边界、最终汇报的 DOM 和 console/network 字段。
- 通用提示词包和五行提示词包必须保留 `Console/network`、环境阻塞、未验证和残余风险字段。
- Brief Gate 的五行默认视觉口径必须是 `restrained Chinese-style data/product feel`，并禁止回退到 `mystic order` / `mystical decoration` 这类易带偏词。
- Start Here 和真实任务样例必须保留可直接运行的最小命令块，以及完成前 `not_ready` / `active_timebox_not_complete` 和完成后 `release_ready` / `complete_ready` 的状态语义。
- 新项目 onboarding 模板必须保留 15 分钟决策、技术决策门、证据契约和多 Agent 边界。
- project reference 模板必须保留证据契约、多 Agent ownership、热门依赖决策规则和维护触发条件。

## 最新审查反馈吸收

- 多 Agent 边界：已完成的审查反馈可作为改进来源；后段因用量限制失败的审查 agent 已关闭，不计入完成证明，只作为环境/用量边界记录。
- 审计链：移除 final audit 对 requirement audit 的依赖，避免 final/requirement 互相读取旧报告。
- 报告新鲜度：requirement audit 校验 release readiness 晚于 final audit；doctor verifier 校验 doctor 晚于 final、release、requirement、manifest、package、market、benchmark 和 Git visibility，并检查同一 `run_id`。
- 阶段状态：assets verifier 不再硬编码当前阶段状态，改为检查完成前/完成后状态语义。
- 市场调研：新增 24 包 npm baseline fixture，verifier 改为明确的 local fixture consistency；文档标明样本内排序、React adapter/package signal 和 Stack Overflow 人工摘录未机器复算；额外加入 `typescript`、`react`、`vite` 三项 npm API live spot-check，避免把本地 fixture 误读成刚刚全量 live 重算。
- 通用/项目分层：MAINTENANCE 拆分 Generic Release Checklist 和 Wuxing Adapter Release Checklist；通用 brief gate / visual scorecard 去五行化，五行默认口径转入 `references/wuxing-persona-card.md`；五行提示词包明确标注为项目专项。
- 本地完成态/发布态分层：`docs/codex-frontend-start-here-20260622.md` 的最终确认口径已拆成本地 `complete_ready` 与发布态 `release_ready`，避免把 local-only docs 的 Git 可见性 blocker 误读成流程交付未完成。
- 报告链硬化：artifact manifest 补 `workflow_started_at` 并校验十小时差值；final audit 改为真实重跑 dashboard fixture 浏览器验证；benchmark 报告写入 `generated_at`、`run_id`、输出 hash 和“重扫既有输出”的边界说明。
- 历史模板：2026-06-21 三小时 prompt 标记 deprecated，并指向 2026-06-22 Webcoding 协议。
- 经验沉淀：failure modes 新增 report-state races、hardcoded phase statuses、weak market evidence、generic/project layer mixing。

## 十小时续跑机制

- Goal 状态：以 `get_goal` 最终实时结果为准；未到最早收口前不能是 `complete`，十小时后需等 final audit 证明 `complete_ready` 后再标记。
- 开始时间：`2026-06-22 00:13:45 CST`。
- 最早整体 complete 时间：`2026-06-22 10:13:45 CST`。
- 续跑依据：`date`、`get_goal` 实时状态、ordered closeout 和 final audit；不把未验证的 heartbeat 作为完成证据。
- 当前阶段结论：本地完成态已由 final audit 证明为 `complete_ready`；发布态仍因 local-only docs 不是 `release_ready`。

## Eval 结果

### Iteration 1

| 对照 | 得分 | 结论 |
| --- | ---: | --- |
| with skill | 15/15 | 能稳定要求任务定界、项目读取、视觉方向和证据链。 |
| without skill | 13/15 | 在强 prompt 下也能完成多数项，但更容易弱化视觉确认和项目证据。 |

静态 review 页面：

```text
custom-skills/codex-frontend-workflow-workspace/iteration-1/review.html
```

### Iteration 2

| 用例 | 结果 | 说明 |
| --- | --- | --- |
| compact-start | 通过 | 小型 React/Vite dashboard 场景能短而准地进入任务，不输出冗长流程。 |
| minimum-timebox | 通过 | 明确记录开始时间和最早收口时间，不在十小时窗口前声明完成。 |

记录文件：

```text
custom-skills/codex-frontend-workflow-workspace/iteration-2/benchmark.md
```

### Trigger Eval

| 指标 | 结果 |
| --- | ---: |
| total | 26 |
| positives | 13 |
| negatives | 13 |
| passed | 26/26 |
| false positive | 0 |
| false negative | 0 |

记录文件：

```text
custom-skills/codex-frontend-workflow-workspace/trigger-eval/trigger-eval-report.md
```

### Iteration 3 Full Benchmark

| 对照 | 得分 | 结论 |
| --- | ---: | --- |
| with skill | 45/45 | 覆盖完整流程、轻量修复、非视觉降级、greenfield、截图还原、后台表格和 visual QA scorecard 场景。 |
| without skill | 27/45 | 在通用和五行强 prompt 下尚可，但明显缺小修分流、截图证据、DOM 验收、过度触发边界和视觉评分卡。 |

记录文件：

```text
custom-skills/codex-frontend-workflow-workspace/iteration-3/benchmark.md
```

### Independent Audit Feedback

| 审计方向 | 吸收结果 |
| --- | --- |
| 市场调研可复现性 | 将 npm 复核命令从滚动 `last-week` 改为固定 `2026-06-14:2026-06-20` 区间，并在市场基线中保存原始 JSON 摘要。 |
| Stack Overflow 辅助信号 | 在市场基线中补充 Technology 页面复核口径：读取相关表格的 `All Respondents` 百分比，只保存对 workflow 决策有直接价值的条目。 |
| 新项目 onboarding | 在 `Technology Decision Gate` 和 project reference template 中新增 `Market signal source and date`，防止以后只写“热门”却没有证据日期。 |
| release readiness 可读性 | `run_release_readiness_check.mjs` 把 strict git visibility 失败压缩成单行摘要，避免 Markdown 表格被多行堆栈破坏。 |
| 阶段/发布语义 | `run_requirement_completion_audit.mjs`、`run_final_completion_audit.mjs` 和 ordered closeout 已区分 evidence blocking 与 release blocking。 |

## 截图与视觉复核

本轮已按用户审美 brief 修改真实 H5 页面样式，视觉方向是温暖纸感、克制橙色主 CTA、宋体气质标题、数据卡片化结果页和可见但不廉价的五行色。

已刷新并目检：

- `docs/screenshots/showcase/iphone-se-01-home.png`：首页墨色、CTA 力度和五行样例卡已恢复稳定，不再受淡入动画半透明截图影响。
- `docs/screenshots/showcase/android-wide-01-home.png`：430 宽安卓首屏信息层级、输入框和 CTA 可读。
- `docs/screenshots/showcase/iphone-se-04-result.png`：结果页包含主副五行数据条、人格短码、身份句、人格共鸣、命盘解释、完整分布和分享模块。
- `docs/screenshots/showcase/iphone-se-10-admin-report-expanded.png`：后台移动统计展开态仍显示短链移动卡片，不回退到桌面宽表。

2026-06-22 16:06 CST 已按用户选择定稿 A/C 混合字体规则：大标题、人格卡名称、身份句保留宋体气质；CTA、题目、年份数字、结果数据条和后台操作继续使用现代黑体。

## In-App Browser DOM 复核

- 页面：`/`、`/test`、`/result/:resultId`、移动后台统计和短链详情经 Playwright Chromium 复核。
- 横向溢出：`capture-showcase-screenshots.sh` 内置 `expectNoHorizontalOverflow` 通过。
- 触控目标：`capture-showcase-screenshots.sh` 内置 `expectMinimumTouchTargets` 通过，移动端目标保持至少 44px。
- 控件文字：`expectNoControlTextOverflow` 通过，按钮、链接、输入框、短码和 URL 无文本溢出。
- Console/Network：未单独手工展开 DevTools；本轮以 Playwright E2E、readiness 和接口流转作为网络行为证据。

## 未验证或环境阻塞

- 官方 `package_skill.py` 依赖 `PyYAML`，当前系统 Python 和 Codex bundled Python 都缺少 `yaml` 模块；已用 zip 兼容 `.skill` 产物替代。
- 未来新会话中的自动触发效果仍需要真实任务继续观察；当前 trigger eval 已覆盖正例和负例并通过启发式 smoke，但不等同于长期线上使用统计。
- 十小时时间盒已满足；本地 goal 可以在最终 evidence chain 健康时标记 complete，但发布态仍需处理 local-only docs。
- `/Users/linyuxiang/JavaBackend/06_Tools/skills` 本身不是 git 仓库；五行项目是独立仓库，当前 `outputs/` 为既有未跟踪目录，本轮不触碰。

## 残余风险

- 如果后续继续修改 skill 源码，必须重新同步到 `/Users/linyuxiang/.codex/skills/codex-frontend-workflow` 并再次 `diff -qr`。
- 如果五行项目新增页面、端口、脚本或视觉资产，需要同步更新 `references/wuxing-persona-card.md` 和 `docs/frontend-workflow-playbook.md`。
- 文档型验收不能替代页面型验收；一旦进入真实 UI 改造，仍要跑 build、contract、浏览器、截图/DOM/E2E，并把 QA 记录作为证据承载而非单独替代项。
