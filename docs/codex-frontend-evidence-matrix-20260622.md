# Codex 前端工作流证据矩阵

记录时间：以 `custom-skills/codex-frontend-workflow-workspace/final-audit/final-completion-audit-report.md` 的 `generated at` 为准
目标：为十小时最终审计提供逐项证据索引，避免把“看起来完成”误当成“已经证明完成”。

## 证据原则

- 文件存在不等于可复用，必须有入口或执行方式。
- build 通过不等于视觉完成，视觉任务必须有浏览器、截图、DOM 或明确未验证说明。
- `.skill` 包存在不等于可安装可信，必须检查包内容、排除维护 eval，并比对源码。
- eval 通过是 smoke evidence，不替代真实长期使用反馈。
- 严格至少十小时未到期前，任何阶段性证据都不能证明整体完成；当前已到期并由 final audit 证明本地 `complete_ready`。

## 要求到证据

| 要求 | 当前证据 | 验证方式 | 当前判断 |
| --- | --- | --- | --- |
| 严格至少十小时 | 开始 `2026-06-22 00:13:45 CST`；最早 `2026-06-22 10:13:45 CST`；最终复核时间已超过最早 complete 时间。 | `date`、`get_goal`、final audit。 | 已证明。 |
| 长期可复用 workflow | `custom-skills/codex-frontend-workflow/SKILL.md`、`MAINTENANCE.md`、模板、references、failure modes。 | `verify_frontend_workflow_assets.mjs`。 | 已证明。 |
| 可安装/可迁移 skill | `.skill` 包、安装副本、源码目录一致。 | `diff -qr`、`verify_skill_package.mjs`、`shasum -a 256`。 | 已证明当前包可信。 |
| webcoding 可直接使用 | `docs/codex-webcoding-frontend-protocol-20260622.md`。 | README/docs-site/manifest 入口检查。 | 已证明入口可发现。 |
| 前端审美可执行 | `docs/codex-frontend-style-brief-library-20260622.md`。 | trigger eval style brief 正负例；manifest 入口。 | 已证明阶段性可用。 |
| 五行项目落地 | `docs/frontend-workflow-playbook.md`、`references/wuxing-persona-card.md`、视觉系统、token 盘点、surface map。 | README/docs-site/manifest 入口；真实文件盘点。 | 已证明流程落地。 |
| 热门前端技能调研 | `docs/frontend-market-skills-research-20260622.md`。 | npm 官方 downloads API、24 包机器基线 fixture、`typescript`/`react`/`vite` live spot-check、Stack Overflow Developer Survey 2025 人工摘录、文档入口、`verify_market_research_reproducibility.mjs`。 | 已证明；固定区间表格和来源 URL 已与 fixture 比对，抽样 live API 与 fixture 一致，热门度只作为选型信号。 |
| 不盲目迁移技术栈 | `SKILL.md` Technology Principles；市场调研决策章节。 | 文档审阅；eval 里的 Vue/Wuxing/React/greenfield 场景。 | 已证明阶段性原则。 |
| 多 agent 协作经验 | eval worker 和已完成审查反馈进入 QA/skill 记录；因用量限制失败的审查 agent 不计入证明。 | `docs/frontend-workflow-qa-record-20260622.md`。 | 已证明阶段性。 |
| 多 agent 防漂移护栏 | `SKILL.md` 决策表、真实任务样例决策表、最终汇报字段、最小命令块和五行视觉默认口径。 | `verify_frontend_workflow_assets.mjs` 内容断言。 | 已机器校验。 |
| eval 行为证明 | 9 eval with skill `45/45`，baseline `27/45`。 | `grade_full_benchmark.py`。 | 已证明 smoke。 |
| trigger 边界证明 | 26 trigger eval，0 false positive，0 false negative。 | `trigger_eval_report.mjs`。 | 已证明 smoke。 |
| 交付物可审计 | 83 个 artifact，README/docs-site 入口。 | `verify_artifact_manifest.mjs`。 | 已证明。 |
| 有序收口链路 | `run_ordered_closeout_check.mjs`、`frontend-workflow-closeout-sequence-report.md` 和 workflow doctor。 | 顺序运行 assets、project onboarding、market research、artifact manifest、package、final audit、release readiness、requirement audit、workflow doctor、workflow doctor verifier。 | 当前 `phase_closeout_passed_not_ready`。 |
| 原始要求逐项审计 | `requirement-completion-audit-report.md`。 | `run_requirement_completion_audit.mjs`。 | `phase_requirements_accounted_for`，12/14 已证明，evidence blocking 0；2 个 release/open 项只影响发布态。 |
| 发布前严格门禁 | `run_release_readiness_check.mjs`。 | release readiness report。 | 当前 `not_ready`，因为 17 个 workflow 文档 local-only。 |
| 未跟踪文档也被检查 | `verify_wuxing_frontend_workflow_docs.mjs` 和 `wuxing-docs-report.md`。 | 直接读取文件系统，不依赖 git tracked 状态。 | 已证明。 |
| 非五行迁移性 | `fixtures/dashboard-tool/index.html`、`verify_dashboard_fixture.mjs`、dashboard desktop/mobile screenshots。 | Playwright 浏览器验证和 artifact verifier。 | 已证明阶段性可迁移。 |
| QA 诚实边界 | 未修改真实 H5 页面，不声称页面视觉验收完成。 | QA 记录、审计表、采纳方案。 | 已证明当前边界。 |
| 五行项目主质量线 | README/docs-site 与 workflow docs 挂在真实项目内。 | `env REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh`。 | 已证明当前项目主质量门禁通过。 |

## 核心命令

```bash
node custom-skills/codex-frontend-workflow-workspace/verify_frontend_workflow_assets.mjs
node custom-skills/codex-frontend-workflow-workspace/verify_artifact_manifest.mjs
node custom-skills/codex-frontend-workflow-workspace/verify_skill_package.mjs
node custom-skills/codex-frontend-workflow-workspace/run_ordered_closeout_check.mjs
node custom-skills/codex-frontend-workflow-workspace/run_frontend_workflow_doctor.mjs
python3 custom-skills/codex-frontend-workflow-workspace/grade_full_benchmark.py
node custom-skills/codex-frontend-workflow-workspace/trigger_eval_report.mjs
node custom-skills/codex-frontend-workflow-workspace/verify_market_research_reproducibility.mjs
git -C wuxing-persona-card diff --check
env REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh
diff -qr custom-skills/codex-frontend-workflow /Users/linyuxiang/.codex/skills/codex-frontend-workflow
shasum -a 256 custom-skills/codex-frontend-workflow-workspace/dist/codex-frontend-workflow.skill
```

## 当前关键结果

| 项 | 结果 |
| --- | --- |
| artifact manifest | 83/83 |
| ordered closeout | `phase_closeout_passed_not_ready`，phase healthy yes |
| workflow doctor | `phase_healthy_not_release_ready`，一条命令汇总 final audit、release readiness、manifest、package、trigger、benchmark 和 Git 可见性；doctor verifier 已确认一致性 |
| requirement audit | `phase_requirements_accounted_for`，proved 12/14，evidence blocking 0，release blocking 2 |
| final completion audit | `complete_ready`，23/23 |
| Wuxing docs verifier | 19/19，覆盖 tracked/untracked workflow docs |
| market research verifier | pass，固定 npm 日期区间、24 包机器基线 fixture、`typescript`/`react`/`vite` live spot-check 口径、Stack Overflow 人工摘录口径和“不迁移”规则均通过 |
| dashboard fixture | desktop 1280x820 与 mobile 390x844 均通过，无横向溢出，控件 >=44px |
| package verification | pass，18 archive entries，14 content files，0 eval entries |
| release readiness | `not_ready`，需要 workflow docs Git-visible |
| semantic guardrails | 多 agent 决策表、最小命令块、最终汇报字段、五行视觉口径均由 `verify_frontend_workflow_assets.mjs` 断言 |
| package SHA256 | `a0cf0f954e8ce705b0fe0612acc9633bdf5633a4be3abb54e8701e16d321f6a2` |
| full benchmark | with skill 45/45，without skill 27/45 |
| trigger eval | 26/26，false positive 0，false negative 0 |
| Wuxing quality gate | `env REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh` 通过 |
| 总校验 | `verify_frontend_workflow_assets.mjs` 通过 |

## 最终审计注意事项

这份矩阵已经按十小时后最终审计更新；后续如果继续改 runtime、docs 或真实页面，必须重新运行核心命令，确认当前状态仍然满足矩阵中的每一项。
