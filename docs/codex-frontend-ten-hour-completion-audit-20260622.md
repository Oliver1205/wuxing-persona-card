# Codex 前端十小时完成审计

记录日期：2026-06-22
目标：严格至少十小时，把前端工作流程沉淀成长期可复用的 Codex frontend workflow / skill / 项目方案，并结合五行人格项目完成可落地交付。

## 时间盒状态

| 项 | 值 |
| --- | --- |
| 开始时间 | `2026-06-22 00:13:45 CST` |
| 最早 complete 时间 | `2026-06-22 10:13:45 CST` |
| 续跑机制 | 以 `date`、`get_goal` 实时状态和复跑审计约束为准；不把未验证的 heartbeat 当完成证据 |
| 当前审计时间 | 以 `custom-skills/codex-frontend-workflow-workspace/final-audit/final-completion-audit-report.md` 的 `generated at` 为准 |
| 当前状态 | 已超过最早 complete 时间；final audit 为 `complete_ready`，本地完成态可收口。 |

## 原始要求拆解

| 要求 | 当前证据 | 状态 | 缺口或下一步 |
| --- | --- | --- | --- |
| 严格至少十小时 | 开始和最早收口时间已写入 QA、操作系统、审计表；最终 `date` 已超过 `2026-06-22 10:13:45 CST`，final audit timebox satisfied。 | 已证明 | 后续只需在新改动后重新审计。 |
| 可长期复用的 Codex 前端 workflow/skill | `custom-skills/codex-frontend-workflow/SKILL.md`、`README.md`、`MAINTENANCE.md`、templates、references、evals。 | 已证明 | 后续真实任务继续补 trigger 边界或维护说明。 |
| webcoding 中可直接使用 | `docs/codex-webcoding-frontend-protocol-20260622.md` 已接入 README/docs-site/操作系统。 | 已证明 | 后续可用真实 webcoding 任务再增加样例 eval。 |
| 搜索热门/通用/下载量高的前端技能 | `docs/frontend-market-skills-research-20260622.md` 记录 npm 官方下载 API、24 包机器基线 fixture 和 Stack Overflow Developer Survey 2025 辅助信号。 | 已证明 | 固定区间表格和来源 URL 已与 fixture 比对；市场基线已纳入 TypeScript、React、Vite、ESLint、Prettier、Vitest、Playwright、Radix、TanStack、Vue 等能力。 |
| 把热门技术转成流程，而非盲目追新 | `SKILL.md` 的 `Technology Principles`；市场基线文档的 Codex 默认决策。 | 已证明 | 后续新项目参考模板继续沿用该原则。 |
| 详细完整的前端设计流程 | `docs/codex-frontend-operating-system-20260622.md`、`docs/frontend-workflow-playbook.md`、`templates/frontend-task-brief.md`。 | 已证明 | 后续真实页面改造要按该流程补浏览器证据。 |
| 结合五行人格项目 | `references/wuxing-persona-card.md`、`docs/frontend-visual-system.md`、五行项目级 Playbook、README/docs-site 入口。 | 已证明 | 若五行页面、端口、脚本变化，需要同步更新。 |
| 最终可落地方案 | 最终采纳方案、操作系统、Webcoding 协议、Style Brief 库、提示词包、Playbook、QA 记录、Skill 接入记录。 | 已证明 | 后续继续用真实任务样例迭代。 |
| 多 agent 协作 | 已完成的审查反馈和 eval worker 结果已进入 QA/skill 记录；后段因用量限制失败的审查 agent 不计入完成证明。 | 已证明 | 后续只在大任务继续使用，多 agent 不作为小修默认；失败或未完成的 agent 只能记录为环境/用量边界。 |
| 自我积累经验 | MAINTENANCE、eval 结果、trigger 报告、QA 记录保存了规则和经验。 | 已证明 | 后续误触发/漏触发继续进入 trigger-evals。 |
| 失败模式沉淀 | `references/frontend-failure-modes.md` 已记录过度触发、弱浏览器证据、泛化审美、契约破坏、移动端漂移、包漂移和时间盒提前完成等纠偏规则。 | 已证明 | 后续真实任务出现新问题时继续追加。 |
| 真实任务样例 | `templates/frontend-usage-scenarios.md` 和 `docs/codex-frontend-real-task-examples-20260622.md` 已给出结果页、测试页、后台、截图还原、小修和 QA closeout 的可复制样例。 | 已证明 | 后续真实使用后继续把高频任务沉淀为样例。 |
| 验收口径收紧 | 采纳方案和样例库已明确 QA 记录不能单独替代视觉/交互页面证据。 | 已证明 | 未来真实 UI 改造必须提供 browser/screenshot/DOM/E2E。 |
| 文档存在性检查 | `verify_wuxing_frontend_workflow_docs.mjs` 已覆盖本轮 workflow docs，19/19 通过，范围为存在性和 trailing whitespace。 | 已证明 | 后续新增 workflow docs 时需纳入检查。 |
| Git 发布可见性检查 | `verify_wuxing_docs_git_visibility.mjs` 已报告 19 个入口/文档目标中 2 个 tracked、17 个 local-only、0 missing。 | 报告型证明 | 发布、push 或 GitHub Pages 前必须用 `REQUIRE_WUXING_DOCS_TRACKED=1` 严格检查并提交 local-only 文档。 |
| 非五行 fixture 浏览器证据 | dashboard fixture 已生成 desktop/mobile 截图，并由 artifact verifier 纳入最终审计。 | 已证明 | 若后续可用更多真实项目，可继续扩展 fixture。 |
| eval 和触发边界证明 | 9 eval `45/45` with skill；trigger eval `26/26`。 | 已证明 | 评分是 smoke，不等于真实长期统计；继续保留残余风险。 |
| 安装和打包可复用 | active skill 目录同步；`.skill` 包 18 个 archive entries、14 个内容文件；SHA256 已记录。 | 已证明 | 每次 runtime 文件变更都要刷新包、包内容校验和 SHA。 |
| 质量证据 | verifier、JSON parse、diff check、rg 链接搜索、benchmark、trigger report、五行项目主 `quality-check.sh`。 | 已证明 | 不等同于真实 H5 页面视觉验证。 |
| 交付物清单可审计 | `artifact-manifest-report.md` 记录 83 个核心交付物，存在性、README 入口、docs-site 入口均通过。 | 已证明 | 新增或删除交付物后需复跑。 |
| 原始要求逐项审计 | `run_requirement_completion_audit.mjs` 已生成 `requirement-completion-audit-report.md`。 | 已证明 | 当前 `phase_requirements_accounted_for`，无 evidence blocking；release blocking 只影响发布态。 |
| 发布前严格门禁 | `run_release_readiness_check.mjs` 已生成 `release-readiness-report.md`。 | 当前 not_ready | final audit 已 `complete_ready`，但 17 个 local-only docs 需 add/commit 后才能 release-ready。 |
| 最终审计可运行 | `run_final_completion_audit.mjs` 已生成 `final-completion-audit-report.md`。 | 已证明 | 当前状态 `complete_ready`，23/23。 |
| 真实 H5 页面视觉验证 | 本轮没有修改真实 H5 页面样式。 | 不适用/未触发 | 如果后续进入真实页面改造，必须跑 build、contract、mobile E2E、screenshots、DOM。 |

## 权威文件清单

| 类型 | 文件 |
| --- | --- |
| 通用 skill | `custom-skills/codex-frontend-workflow/SKILL.md` |
| skill 维护 | `custom-skills/codex-frontend-workflow/MAINTENANCE.md` |
| 失败模式纠偏 | `custom-skills/codex-frontend-workflow/references/frontend-failure-modes.md` |
| 真实任务样例 | `custom-skills/codex-frontend-workflow/templates/frontend-usage-scenarios.md`、`docs/codex-frontend-real-task-examples-20260622.md` |
| 通用提示词 | `custom-skills/codex-frontend-workflow/templates/frontend-prompt-pack.md` |
| Webcoding 协议 | `docs/codex-webcoding-frontend-protocol-20260622.md` |
| 总控操作系统 | `docs/codex-frontend-operating-system-20260622.md` |
| 最终采纳方案 | `docs/codex-frontend-adoption-plan-20260622.md` |
| 证据矩阵 | `docs/codex-frontend-evidence-matrix-20260622.md` |
| 五行项目 Playbook | `docs/frontend-workflow-playbook.md` |
| 五行视觉系统 | `docs/frontend-visual-system.md` |
| 市场技能基线 | `docs/frontend-market-skills-research-20260622.md` |
| Skill 接入记录 | `docs/codex-frontend-workflow-skill-20260622.md` |
| QA 记录 | `docs/frontend-workflow-qa-record-20260622.md` |
| 交付物清单 | `custom-skills/codex-frontend-workflow-workspace/artifact-manifest/artifact-manifest-report.md` |
| 发布前严格门禁 | `custom-skills/codex-frontend-workflow-workspace/release-readiness/release-readiness-report.md` |
| 有序收口链路 | `custom-skills/codex-frontend-workflow-workspace/closeout-sequence/frontend-workflow-closeout-sequence-report.md` |
| 原始要求逐项审计 | `custom-skills/codex-frontend-workflow-workspace/requirement-audit/requirement-completion-audit-report.md` |
| 文档存在性检查 | `custom-skills/codex-frontend-workflow-workspace/wuxing-docs-verification/wuxing-docs-report.md` |
| Git 发布可见性 | report-only：`custom-skills/codex-frontend-workflow-workspace/git-visibility/wuxing-docs-git-visibility-report.md`；strict release：`custom-skills/codex-frontend-workflow-workspace/git-visibility/wuxing-docs-git-visibility-strict-report.md` |
| 非五行 fixture | `custom-skills/codex-frontend-workflow-workspace/fixture-verification/dashboard-fixture-report.md` |
| 最终审计报告 | `custom-skills/codex-frontend-workflow-workspace/final-audit/final-completion-audit-report.md` |
| 全量 eval | `custom-skills/codex-frontend-workflow-workspace/iteration-3/benchmark.md` |
| trigger eval | `custom-skills/codex-frontend-workflow-workspace/trigger-eval/trigger-eval-report.md` |

## 最终收口前必须复核

到 `2026-06-22 10:13:45 CST` 后，才能进行最终完成审计。审计必须重新运行或读取当前结果：

1. `get_goal`：确认时间已超过最早 complete 时间。
2. `node custom-skills/codex-frontend-workflow-workspace/run_final_completion_audit.mjs`。
3. `node custom-skills/codex-frontend-workflow-workspace/run_release_readiness_check.mjs`；若 Git 可见性仍阻塞，记录为本地 `complete_ready` 但不可称为发布态 `release_ready`。
4. `node custom-skills/codex-frontend-workflow-workspace/run_requirement_completion_audit.mjs`，确认原始要求没有 `missing_*` / `failed*` 项。
5. `node custom-skills/codex-frontend-workflow-workspace/verify_frontend_workflow_assets.mjs`。
6. `node custom-skills/codex-frontend-workflow-workspace/verify_artifact_manifest.mjs`。
7. `node custom-skills/codex-frontend-workflow-workspace/verify_skill_package.mjs`。
8. `node custom-skills/codex-frontend-workflow-workspace/run_ordered_closeout_check.mjs`，确认有序执行 assets、project onboarding、market research、artifact manifest、package、final audit、release readiness、requirement audit、workflow doctor、workflow doctor verifier。
9. `node custom-skills/codex-frontend-workflow-workspace/run_frontend_workflow_doctor.mjs`，一条命令汇总当前证据和下一步。
10. `node custom-skills/codex-frontend-workflow-workspace/verify_frontend_workflow_doctor.mjs`，确认 doctor 报告新鲜度和一致性。
11. `node custom-skills/codex-frontend-workflow-workspace/verify_wuxing_docs_git_visibility.mjs`；发布前再用 `REQUIRE_WUXING_DOCS_TRACKED=1`。
12. `run_final_completion_audit.mjs` 内置 JSON parse set；当前口径为 14 个 JSON 文件，包含 npm baseline fixture；doctor JSON 由 `verify_frontend_workflow_doctor.mjs` 在 closeout 后校验。
13. `python3 custom-skills/codex-frontend-workflow-workspace/grade_full_benchmark.py`。
14. `node custom-skills/codex-frontend-workflow-workspace/trigger_eval_report.mjs`。
15. `git -C wuxing-persona-card diff --check`.
16. `rg` 搜索 README/docs-site/docs 中所有关键入口。
17. `diff -qr custom-skills/codex-frontend-workflow /Users/linyuxiang/.codex/skills/codex-frontend-workflow`。
18. `unzip -l` 和 `shasum -a 256` 复核 `.skill` 包。

## 本地 complete 与发布态边界

- 本地工作流/skill/方案沉淀已达到 `complete_ready`。
- 真实页面视觉没有在本轮改造，因此不能把页面视觉验证写成已完成；当前完成的是工作流/skill/方案沉淀。
- 官方 `package_skill.py` 仍因缺少 `PyYAML` 环境阻塞，当前使用 zip 兼容包。
- 发布态 `release_ready` 仍被 Git 可见性阻塞：17 个 workflow 文档 local-only，需要 add/commit 后重跑 release readiness。

## 阶段判断

截至当前 final audit 报告生成时：

- 工作流、skill、文档、Webcoding 协议、五行项目落地、eval、trigger 边界、QA 记录已经形成本地完成闭环。
- 交付物 manifest 已把 83 个核心产物纳入存在性和入口可发现性校验。
- 有序收口链路已生成 `phase_closeout_passed_not_ready`，说明证据链顺序健康，但因 Git 可见性尚未满足，仍不能发布态收口。
- Requirement completion audit 已把原始目标拆成 14 条要求，当前 12 条证明、2 条开放；evidence blocking 为 0，release blocking 为 2。
- Release readiness gate 已把“是否能发布”从 final audit 中拆出；当前结果为 `not_ready`，这是 Git 可见性阻塞下的预期状态。
- Wuxing docs verifier 已覆盖 19 个 tracked/untracked workflow 文档，解决 `git diff --check` 不覆盖未跟踪文件的问题。
- Git visibility report 已明确 17 个五行 workflow 文档仍是 local-only，发布前必须 add/commit。
- 非五行 dashboard fixture 已生成桌面/移动浏览器截图，并用 artifact verifier 纳入最终审计。
- 最终完成审计报告已生成，当前状态为 `complete_ready`。
- `.skill` 包内容已由 `verify_skill_package.mjs` 校验，确认 14 个内容文件与源码一致且不包含 `evals/`。
- 证据矩阵已把原始要求、当前证据、验证方式和当前判断逐项对应。
- 证据足以允许本地 goal complete；发布态仍需处理 local-only docs。
