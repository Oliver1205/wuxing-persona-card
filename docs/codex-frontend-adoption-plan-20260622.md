# Codex 前端工作流最终采纳方案

记录日期：2026-06-22
状态：本地完成态 `complete_ready` 已达成；发布态 `release_ready` 仍因 17 个 workflow 文档 local-only 而阻塞。

## 路径口径

- `docs/...`：`wuxing-persona-card` 仓库内文档。
- `custom-skills/...`：workspace 根目录 `/Users/linyuxiang/JavaBackend/06_Tools/skills` 下的可复用 skill、模板、脚本和证据。
- `custom-skills/codex-frontend-workflow-workspace/...`：从 workspace 根目录运行的验证和报告区，不属于五行前端应用本身。

## 采纳目标

以后所有重要前端任务都按同一套方式进入 Codex：

1. 先把“想要的样式”写成可执行 brief。
2. Codex 先读真实项目，不凭框架印象猜。
3. 视觉方向先落到 token、组件规则和反目标。
4. 代码改动薄切片推进，保护路由、接口、字段、testid 和 E2E。
5. 完成证据按任务类型分级：文档/流程任务可用入口、JSON、manifest 和 QA 记录证明；视觉/交互任务必须有浏览器、截图、DOM 或 E2E 的真实页面证据，QA 记录只能承载这些证据，不能单独替代页面验收。

## 固定入口

| 用途 | 入口 |
| --- | --- |
| 一页式起点 | `docs/codex-frontend-start-here-20260622.md` |
| 自动触发和执行规则 | `custom-skills/codex-frontend-workflow/SKILL.md` |
| 失败模式和纠偏经验 | `custom-skills/codex-frontend-workflow/references/frontend-failure-modes.md` |
| webcoding 开场话术 | `docs/codex-webcoding-frontend-protocol-20260622.md` |
| brief 质量门 | `custom-skills/codex-frontend-workflow/templates/frontend-brief-quality-gate.md` |
| 视觉 QA 评分卡 | `custom-skills/codex-frontend-workflow/templates/frontend-visual-qa-scorecard.md` |
| 审美翻译 | `docs/codex-frontend-style-brief-library-20260622.md` |
| 真实任务样例 | `docs/codex-frontend-real-task-examples-20260622.md` |
| 五行项目执行 | `docs/frontend-workflow-playbook.md` |
| 当前文件地图 | `docs/frontend-current-surface-map-20260622.md` |
| 当前 token | `docs/frontend-token-inventory-20260622.md` |
| 证据矩阵 | `docs/codex-frontend-evidence-matrix-20260622.md` |
| QA 记录模板 | `custom-skills/codex-frontend-workflow/templates/frontend-qa-record.md` |
| 新项目接入 | `custom-skills/codex-frontend-workflow/templates/frontend-project-onboarding.md` |

## 你以后怎么发起任务

最推荐的开场：

```text
请使用 codex-frontend-workflow skill。

项目：五行人格卡
目标：[页面/链路/工具]
默认风格：克制中式数据感：暖纸底、深绿主行动、五行色只做信息编码、移动端一屏一任务、后台高密度可扫描。
Style brief：[从 Style Brief 库选择或自己描述]
必须保留：[路由/API 字段/testid/埋点/统计口径]
时间盒：[例如 3 小时 / 至少 10 小时]
验收证据：[build/typecheck/E2E/screenshot/DOM overflow/QA 记录]

先输出 Task Frame 和需要读取的真实文件；如果风格不明确，先给 2-3 个方向让我选。
```

## Codex 必须怎么执行

| 阶段 | Codex 行为 | 产物 |
| --- | --- | --- |
| 1. Brief Gate | 判断 proceed / proceed with assumptions / ask first / split。 | 开工决策。 |
| 2. Task Frame | 明确 flow、用户、视觉方向、保留项、证据、先读文件。 | 任务定界。 |
| 3. 真实项目读取 | 读 routes、页面、组件、API wrapper、style/token、E2E、QA 文档、git status。 | 文件地图和风险边界。 |
| 4. Style Brief | 把审美词翻成颜色、密度、字体、组件、动效、反目标。 | 可执行设计约束。 |
| 5. Token 和组件 | 优先复用现有系统，只在必要时补 token 或小组件规则。 | 可复用视觉语言。 |
| 6. 薄切片实现 | 一次只改一条链路或一个页面簇。 | 最小有效改动。 |
| 7. 浏览器证据 | 桌面、移动、无溢出、无重叠、console/network、截图或 DOM。 | 可验收证据。 |
| 8. QA 记录 | 写已验证、未验证、环境阻塞、残余风险。 | 可追溯交付。 |
| 9. Visual QA Scorecard | 对真实视觉/交互任务做硬门禁和 20 分评分。 | 用户可判断是否符合目标样式。 |

## 路由分流

| 请求 | 路线 | 不该做什么 |
| --- | --- | --- |
| 产品级样式、截图还原、H5 体验 | 完整前端工作流 | 不直接乱改 CSS。 |
| 一个按钮、溢出、重叠小问题 | 轻量修复 | 不输出长篇流程。 |
| build/type/lint 错误 | 非视觉调试 | 不强行截图验收。 |
| 从零做 app/tool/dashboard | Greenfield 前端 | 不做营销落地页壳子。 |
| 发布前验收 | QA closeout | 不把没跑的命令写成通过。 |
| Java/Word/Excel/算法/README-only | 不触发此 skill | 使用对应领域流程。 |

## 五行项目默认验收

| 目标 | 最低证据 |
| --- | --- |
| 首页、测试、结果、匹配 H5 | build、contract、mobile E2E 或 showcase screenshot。 |
| 分享区和分享图 | 分享图生成、短链参数、无重复分享盒、移动端截图。 |
| 后台数据中台 | contract、desktop screenshot、mobile overflow DOM 检查。 |
| 短链详情长文本 | referer/campaign/shortUrl 不撑破，截图和 DOM。 |
| 纯流程文档 | README/docs-site 可发现、JSON 可解析、artifact manifest 通过。 |

## 多 Agent 使用规则

大任务可以拆：

- Context agent：读项目和契约，不改代码。
- Visual agent：给 style brief、token、组件规则，不改业务逻辑。
- Page agent：按薄切片改页面。
- Contract agent：补 E2E、DOM、截图断言。
- Integration agent：处理端口、代理、后端、CORS、live gate。
- Docs agent：写 QA 记录和最终汇报。

小修不要拆太多，避免管理成本比问题本身更大。

## 完成定义

一次前端任务完成时，最终回复必须包含：

```text
Done:
Verification:
Files:
Screenshots / Browser evidence:
Visual QA Scorecard: hard gates / score / decision
Not verified:
Residual risk:
```

如果没有浏览器证据，必须明确说“本次没有验证视觉”，不能用 build 代替页面验收。

## 证据口径

| 任务类型 | 可以证明完成的证据 | 不能替代的证据 |
| --- | --- | --- |
| 文档/流程/skill | README/docs-site 入口、manifest、JSON parse、package verifier、QA 记录。 | 不能证明真实页面视觉已完成。 |
| 小视觉修复 | 目标组件/样式 diff、目标视口浏览器或 DOM 检查。 | 不能只用 QA 记录或 build。 |
| 页面视觉/交互改造 | browser viewport、screenshot、DOM overflow/touch target、E2E/contract。 | QA 记录只能记录结果，不能单独替代页面证据。 |
| 视觉结果是否符合预期 | visual QA scorecard、用户 brief、浏览器证据。 | 不能用“看起来不错”替代评分和证据。 |
| 发布前 QA | 命令结果、浏览器证据、截图、console/network、未验证项。 | 不能把未跑项写成通过。 |

## 维护规则

| 事件 | 要更新 |
| --- | --- |
| 新项目复用 | 填 `frontend-project-onboarding.md`，再建项目 reference。 |
| 用户新增审美偏好 | 更新 Style Brief 库或项目视觉系统。 |
| 用户反馈“不是我要的样式” | 用 `frontend-visual-qa-scorecard.md` 复盘 brief fit、层级、token、响应式和浏览器证据，再决定回到 brief 还是继续薄切片。 |
| skill 误触发/漏触发 | 更新 `evals/trigger-evals.json` 并跑 trigger report。 |
| workflow 行为变化 | 更新 `evals/evals.json` 并跑 full benchmark。 |
| 重复出现前端工作流失误 | 更新 `references/frontend-failure-modes.md`，必要时补 eval。 |
| runtime 文件变化 | 同步安装副本、重打 `.skill` 包、记录 SHA。 |
| 五行页面/端口/脚本变化 | 更新 Playbook、surface map、token inventory 或 QA 记录。 |

## 当前证据

| 项 | 结果 |
| --- | --- |
| 内容 eval | 9 eval，with skill `45/45`，baseline `27/45`。 |
| trigger eval | 26/26，false positive 0，false negative 0。 |
| artifact manifest | 以 `artifact-manifest-report.md` 为准，README/docs-site 入口全部可发现。 |
| Wuxing docs verifier | 19 个 tracked/untracked workflow 文档全部通过存在性和 trailing whitespace 检查。 |
| ordered closeout | `run_ordered_closeout_check.mjs` 当前为 `phase_closeout_passed_not_ready`，说明顺序证据链健康但尚未 release-ready。 |
| workflow doctor | `run_frontend_workflow_doctor.mjs` 当前为 `phase_healthy_not_release_ready`，`verify_frontend_workflow_doctor.mjs` 确认其没有读旧报告或错报状态。 |
| 非五行 fixture | dashboard fixture 已用真实浏览器生成桌面/移动截图和 DOM 指标，证明流程不只绑定五行项目。 |
| final completion audit | `complete_ready`；23/23 通过。 |
| `.skill` 包 | 以 `package-verification/skill-package-report.md` 为准；内容文件与源码 SHA 必须一致。 |
| 十小时时间盒 | 已超过 `2026-06-22 10:13:45 CST` 并由 final audit 证明满足。 |

## 最终确认清单

十小时结束后，先判断本地完成态；发布态另看 Git 可见性：

- 时间超过 `2026-06-22 10:13:45 CST`。
- `run_final_completion_audit.mjs` 生成 `complete_ready`。
- `run_release_readiness_check.mjs` 若仍因 local-only docs 阻塞，可记录为本地 `complete_ready`，但不能作为发布态 `release_ready`。
- `run_requirement_completion_audit.mjs` 仍为 `phase_requirements_accounted_for` 或最终完成态，且没有 `missing_*` / `failed*` 项。
- `verify_frontend_workflow_assets.mjs` 通过。
- `verify_artifact_manifest.mjs` 通过。
- `verify_skill_package.mjs` 证明 `.skill` 包内容与源码一致且不包含 `evals/`。
- `verify_wuxing_frontend_workflow_docs.mjs` 覆盖 tracked/untracked workflow 文档。
- `run_ordered_closeout_check.mjs` 证明 assets、project onboarding、market research、artifact manifest、package、final audit、release readiness、requirement audit、workflow doctor、workflow doctor verifier 的顺序收口链路健康。
- `run_frontend_workflow_doctor.mjs` 可一条命令查看当前 workflow 健康度、阻塞项和下一步；`verify_frontend_workflow_doctor.mjs` 用于确认报告新鲜度和一致性。
- `trigger_eval_report.mjs` 仍为 0 false positive / 0 false negative。
- `grade_full_benchmark.py` 仍保持 with skill 满分或有解释。
- README、docs-site、操作系统、采纳方案彼此可发现。
- `diff -qr` 证明源码 skill 与安装副本一致。
- `.skill` 包 SHA 已记录。
- 本轮未修改真实 H5 页面时，不把页面视觉验收写成已完成。
- Runtime skill 的 `MAINTENANCE.md` Release Checklist 已执行到当前阶段；最终 complete 前必须再次复跑。

发布态 `release_ready` 还需要额外满足：17 个 local-only workflow docs 已 add/commit，严格 Git visibility 检查通过，release readiness 报告为 `release_ready`。
