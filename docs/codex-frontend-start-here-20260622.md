# Codex 前端工作流 Start Here

记录日期：2026-06-22
用途：以后要让 Codex 做前端时，先看这一页，再决定打开哪份详细材料；本页同时记录五行人格卡项目的 adapter 证据。

## 路径口径

本页里 `docs/...` 指 `wuxing-persona-card` 仓库内路径。

本页里 `custom-skills/...` 指 workspace 根目录路径：

```text
/Users/linyuxiang/JavaBackend/06_Tools/skills/custom-skills/...
```

如果只在 `wuxing-persona-card` 仓库里查看文档，先回到 workspace 根目录再运行 skill、package、manifest 和 final audit 相关命令。

## 当前结论

这套前端工作流已经沉淀成三层：

| 层级 | 用途 | 入口 |
| --- | --- | --- |
| 通用 skill | 让 Codex 自动按前端流程工作 | `custom-skills/codex-frontend-workflow/SKILL.md` |
| 五行项目落地 | 把流程绑定到五行项目路由、视觉、脚本和 QA | `docs/frontend-workflow-playbook.md` |
| 真实任务样例 | 直接复制发起结果页、测试页、后台、小修或 QA 任务 | `docs/codex-frontend-real-task-examples-20260622.md` |

当前本地完成态已达到 `complete_ready`：十小时时间盒已满足，final audit 23/23；发布态仍是 `not_ready`，因为 17 个 workflow 文档仍是 local-only，发布前需要 add/commit 后重新跑 release readiness。

## 先复制这段

```text
请使用 codex-frontend-workflow skill。

项目：[项目名/路径]
当前分支/commit：[branch + commit；不知道就让 Codex 先查]
目标：[页面/链路/工具]
用户场景：[谁，在什么设备上，完成什么任务]
我想要的视觉效果：[关键词、参考截图、不要什么]
参考材料：[截图/Figma/链接/现有页面；没有就写无]
必须保留：[路由、API、字段、testid、埋点、统计口径、E2E]
允许修改范围：[目录/文件/模块]
禁止修改范围：[接口契约/数据口径/无关页面/部署配置等]
技术栈/依赖策略：[沿用现有栈 / 允许新增但要说明理由 / 禁止迁移]
启动命令和端口：[dev/preview/backend 命令、URL、端口；不知道就让 Codex 先查]
视口矩阵：[desktop / mobile / 具体设备宽高]
测试数据/fixture：[账号、token、mock、resultId、shortCode；没有就让 Codex 生成或说明]
时间盒：[例如 3 小时 / 至少 10 小时]
验收要求：[build / contract / E2E / screenshot / DOM / live gate / QA 记录]
证据落盘：[QA 记录路径、截图目录、报告路径；不确定就让 Codex 提议]

请先输出 Task Frame、要读取的文件、任务分流和第一片最小可验收改造。
视觉/交互任务没有浏览器、截图、DOM 或 E2E 证据前，不要说视觉完成。
```

五行项目专项时，把 `项目` 写成 `五行人格卡`，并沿用 `docs/frontend-workflow-playbook.md` 里的路由、脚本、端口和 QA 记录路径。

Codex 拿到这段后，必须先按 `custom-skills/codex-frontend-workflow/templates/frontend-brief-quality-gate.md` 判断：

| 判断 | 下一步 |
| --- | --- |
| proceed | 直接进入 Task Frame 和真实项目读取。 |
| proceed with assumptions | 说明假设，先做窄切片。 |
| ask first | 只问一个会改变方向的阻塞问题。 |
| split | 拆成独立页面/链路，每片单独验收。 |

## 按场景打开

| 你现在想做 | 先打开 | 原因 |
| --- | --- | --- |
| 不知道怎么描述想要的风格 | `docs/codex-frontend-style-brief-library-20260622.md` | 把审美词翻译成 token、组件、密度和反目标。 |
| 想直接发起一个真实任务 | `docs/codex-frontend-real-task-examples-20260622.md` | 已有结果页、测试页、后台、截图还原、小修、QA closeout 样例。 |
| 想判断做出来的页面是否真达标 | `custom-skills/codex-frontend-workflow/templates/frontend-visual-qa-scorecard.md` | 用硬门禁和 20 分评分卡判断视觉、响应式、浏览器证据和交付诚实度。 |
| 想了解五行项目具体该读哪些文件 | `docs/frontend-workflow-playbook.md` | 列了路由、页面、组件、视觉资产和验收脚本。 |
| 想检查这套流程是否可信 | `docs/codex-frontend-evidence-matrix-20260622.md` | 把要求、证据、验证方式和当前判断逐项对应。 |
| 想做最终收口 | `docs/codex-frontend-ten-hour-completion-audit-20260622.md` | 记录时间盒、审计命令、本地完成态、发布阻塞原因和最终复核清单。 |
| 想维护 skill | `custom-skills/codex-frontend-workflow/MAINTENANCE.md` | 包含 trigger/eval/package/release checklist。 |

## 决策树

| 任务形状 | 路线 | 最低完成证据 |
| --- | --- | --- |
| 页面太 demo、想要产品级样式 | 完整前端工作流 | Task Frame、真实项目读取、Style Brief、代码改动、浏览器/截图/DOM/E2E、QA 记录。 |
| 一个按钮溢出或文字重叠 | 轻量修复 | 定位组件和样式、最小改动、目标视口验证。 |
| 纯 build/type/lint 报错 | 非视觉调试 | 对应命令复现和修复，不强行截图。 |
| 从零做工具或 dashboard | Greenfield | 第一屏就是可用工具，本地 URL，桌面/移动浏览器证据。 |
| 发布前检查 | QA closeout | 命令、浏览器、截图/DOM、console/network、未验证项。 |
| 文档/skill 流程本身 | 流程验收 | manifest、package verifier、JSON parse、docs links、final audit。 |

## 五行默认风格

```text
克制中式数据感：暖纸底、深绿主行动、五行色只做信息编码、移动端一屏一任务、后台高密度可扫描。
```

反目标：

- 不要默认紫蓝渐变。
- 不要装饰球和泛化 AI 审美。
- 不要营销 hero 替代真实工具界面。
- 不要卡片套卡片。
- 不要只跑 build 就说视觉完成。

## 最终确认口径

文档/流程任务先看本地完成态，可以用这些证明：

- README/docs-site 可发现。
- artifact manifest 通过。
- package verifier 通过。
- JSON parse 通过。
- 十小时后 final audit 达到 `complete_ready`；十小时前只能明确说明只剩时间盒。
- QA 记录说明未验证项。

发布态另看 release readiness：只有 Git 可见性、local-only docs、final audit 和 package 门禁都通过，才能称为 `release_ready`；如果 release readiness 仍因 local-only docs 阻塞，只能说本地 `complete_ready`，不能说发布态完成。

真实视觉/交互任务必须额外有页面证据：

- browser viewport。
- screenshot。
- DOM overflow/touch target/text overlap。
- E2E 或 contract。
- console/network。
- visual QA scorecard，真实视觉/交互任务最终交付前必须使用；用户反馈不像想要的样式、截图还原、发布前 QA 时也使用。

QA 记录只能承载这些证据，不能单独替代页面验收。

通用 runtime 最小命令块：

```bash
node custom-skills/codex-frontend-workflow-workspace/verify_frontend_workflow_assets.mjs
node custom-skills/codex-frontend-workflow-workspace/verify_artifact_manifest.mjs
node custom-skills/codex-frontend-workflow-workspace/verify_skill_package.mjs
node custom-skills/codex-frontend-workflow-workspace/run_final_completion_audit.mjs
node custom-skills/codex-frontend-workflow-workspace/run_ordered_closeout_check.mjs
node custom-skills/codex-frontend-workflow-workspace/run_frontend_workflow_doctor.mjs
node custom-skills/codex-frontend-workflow-workspace/verify_frontend_workflow_doctor.mjs
```

五行 adapter closeout 命令块：

```bash
node custom-skills/codex-frontend-workflow-workspace/verify_wuxing_frontend_workflow_docs.mjs
node custom-skills/codex-frontend-workflow-workspace/run_release_readiness_check.mjs
node custom-skills/codex-frontend-workflow-workspace/run_requirement_completion_audit.mjs
cd wuxing-persona-card
env REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh
```

当前 closeout expected status：

- `verify_*` 命令应通过。
- `run_final_completion_audit.mjs` 当前应为 `complete_ready`，23/23 通过。
- `run_release_readiness_check.mjs` 当前应为 `not_ready`，因为 17 个 workflow 文档仍 local-only。
- `run_requirement_completion_audit.mjs` 当前应为 `phase_requirements_accounted_for`，且 evidence blocking 为 0；release blocking 只剩 Git 可见性和 release readiness。
- `run_frontend_workflow_doctor.mjs` 当前应为 `phase_healthy_not_release_ready`。
- `run_ordered_closeout_check.mjs` 当前应为 `phase_closeout_passed_not_ready`，说明本地完成证据链健康但仍未发布态 release-ready。

## 当前证据快照

| 项 | 当前结果 |
| --- | --- |
| final audit | `complete_ready`，23/23。 |
| artifact manifest | 83/83。 |
| ordered closeout | `phase_closeout_passed_not_ready`，说明有序证据链健康，但仍未到 release-ready。 |
| workflow doctor | `phase_healthy_not_release_ready`，一条命令汇总当前证据和下一步；doctor verifier 确认报告与当前审计一致。 |
| requirement audit | `phase_requirements_accounted_for`，12/14 已证明，evidence blocking 0，2 项 release/open 项为 Git 可见性和 release readiness。 |
| package SHA | `a0cf0f954e8ce705b0fe0612acc9633bdf5633a4be3abb54e8701e16d321f6a2`。 |
| release readiness | `not_ready`，原因是 17 个 workflow 文档仍是 local-only。 |
| full benchmark | with skill 45/45，without skill 27/45。 |
| trigger eval | 26/26，false positive 0，false negative 0。 |
| Wuxing docs verifier | 19/19。 |
| dashboard fixture | desktop/mobile 浏览器证据通过。 |
| Wuxing quality gate | `env REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh` 通过。 |

本页可作为本地完成入口；发布前还必须提交 local-only workflow 文档并重新运行 release readiness。
