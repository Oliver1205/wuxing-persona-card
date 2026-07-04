# Codex Webcoding 前端协议

记录日期：2026-06-22
关联 skill：`codex-frontend-workflow`

这份协议用于你以后在 webcoding 里让 Codex 做前端：既要表达清楚“我要什么样式”，也要防止 Codex 只凭感觉改 CSS、跳过真实项目、跳过浏览器验证。

## 路径口径

本文中的 `docs/...` 是 `wuxing-persona-card` 仓库内路径。

本文中的 `custom-skills/...` 是 workspace 根目录 `/Users/linyuxiang/JavaBackend/06_Tools/skills` 下的路径。运行 skill、package、manifest、final audit 和 git visibility 命令时，应从该 workspace 根目录执行。

## 使用方式

每次开始前端任务时，把下面这段作为第一条消息。

```text
请使用 codex-frontend-workflow skill，并遵守 Codex Webcoding 前端协议。

项目：[项目名/路径]
当前分支/commit：[branch + commit；不知道就让 Codex 先查]
目标页面或链路：[路由、页面、组件、用户流程]
用户场景：[谁，在什么设备上，完成什么任务]
我想要的视觉效果：[关键词、参考截图、竞品、不要什么]
参考材料：[截图/Figma/链接/现有页面；没有就写无]
必须保留：[路由、接口、字段、testid、埋点、E2E、短链/统计口径]
允许修改范围：[目录/文件/模块]
禁止修改范围：[接口契约/数据口径/无关页面/部署配置等]
技术栈/依赖策略：[沿用现有栈 / 允许新增但要说明理由 / 禁止迁移]
启动命令和端口：[dev/preview/backend 命令、URL、端口；不知道就让 Codex 先查]
视口矩阵：[desktop / mobile / 具体设备宽高]
测试数据/fixture：[账号、token、mock、resultId、shortCode；没有就让 Codex 生成或说明]
时间盒：[例如 3 小时 / 至少 10 小时]
验收要求：[build / typecheck / contract / E2E / screenshots / live gate / QA 记录]
证据落盘：[QA 记录路径、截图目录、报告路径；不确定就让 Codex 提议]

请先输出：
1. Task Frame
2. 要读取的文件
3. Brief Gate 判断：proceed / proceed with assumptions / ask first / split
4. 任务分流：完整流程 / 轻量修复 / 非视觉调试 / greenfield / QA closeout
5. 第一片最小可验收改造

在没有完成浏览器或截图证据前，不要说视觉完成。
```

## Codex 必须先做什么

在写代码前，Codex 必须给出：

| 项 | 内容 |
| --- | --- |
| Flow | 用户从哪里进、到哪里结束。 |
| User and scenario | 目标用户、设备、业务场景。 |
| Desired visual direction | 审美关键词、参考、反目标。 |
| Must preserve | 接口、路由、字段、testid、E2E、统计口径。 |
| Change scope | 允许修改和禁止修改的目录、模块、契约。 |
| Stack / dependency policy | 沿用现有栈、允许新增依赖的条件、禁止迁移的边界。 |
| Run mode | dev/preview/backend 启动命令、URL、端口。 |
| Viewport and data | 验收视口矩阵、测试数据、fixture 或 mock 来源。 |
| Acceptance evidence | build、E2E、截图、DOM、QA 记录。 |
| Evidence output paths | QA 记录、截图、报告和可复跑命令的落盘位置。 |
| Files to inspect | router、页面、组件、API、styles、tests、docs。 |

如果 Codex 直接开始改代码，没有先给这些内容，就让它暂停并补 Task Frame。对于风格、截图、完整页面、严格时间盒或可复用流程任务，还要先用 `custom-skills/codex-frontend-workflow/templates/frontend-brief-quality-gate.md` 判断这份 brief 是否足够开工。

## Brief Gate 口径

| 判断 | 何时使用 | Codex 下一步 |
| --- | --- | --- |
| proceed | flow、用户、视觉方向、保留项、证据都清楚。 | 直接进入真实项目读取和实现计划。 |
| proceed with assumptions | 只有低风险信息缺失，能从项目读取补齐。 | 先写明假设，再做最小切片。 |
| ask first | 缺失项会改变页面、架构、视觉方向或业务含义。 | 只问一个阻塞问题。 |
| split | 一条 brief 同时覆盖多个不相关页面或冲突风格。 | 拆成独立任务逐个验收。 |

## 任务分流口令

你可以用这些短句快速控制 Codex。

| 你说 | Codex 应该怎么做 |
| --- | --- |
| “这是完整前端改造” | 走 Task Frame、视觉方向、token、薄切片、浏览器证据。 |
| “这是小修，不要展开完整流程” | 只定位目标样式/组件，做最小改动，跑聚焦验证。 |
| “这是纯 build/type bug” | 不触发视觉流程，直接修类型或构建。 |
| “这是 QA closeout” | 收集命令、截图、DOM、console/network、未验证项。 |
| “这是 greenfield tool” | 第一屏直接做可用工具，不做营销落地页。 |

## 视觉输入怎么写

不要只写“高级一点”。更稳定的写法：

```text
我想要：
- 关键词：[克制/数据感/温润/高密度/轻快/专业]
- 色彩：[暖纸底、深绿主行动、低饱和提示色]
- 密度：[移动端一屏一任务 / 后台高密度可扫描]
- 组件：[按钮、输入、表格、卡片、空态、错误态]
- 反目标：[不要紫蓝渐变、不要装饰球、不要营销 hero、不要卡片套卡片]
```

五行项目默认：

```text
克制中式数据感：暖纸底、深绿主行动、五行色只做信息编码、移动端一屏一任务、后台高密度可扫描。
```

## 验收口令

前端任务结束前，直接要求：

```text
请按 Webcoding 前端协议做验收，不要把未跑过的检查写成已通过。
列出：
- build/typecheck
- contract/E2E
- desktop/mobile browser
- screenshots
- DOM overflow/touch target
- console/network
- QA record
- 未验证项和残余风险
```

## 五行项目常用任务模板

### 结果页和分享区

```text
请使用 codex-frontend-workflow skill，并遵守 Webcoding 前端协议。

项目：五行人格卡
目标：优化 `/result/:resultId` 结果页和分享区
用户场景：手机用户看完人格结果后，理解自己是什么人格，并愿意保存或分享
视觉效果：克制中式数据感，身份先被看见，分享区像产品能力
必须保留：短链参数、channel/campaign 归因、shareCard.ts 900x1200 输出、E2E 锚点
验收：build、verify-frontend-contracts、mobile-e2e、showcase screenshots、QA 记录
```

### 后台访问明细

```text
请使用 codex-frontend-workflow skill，并遵守 Webcoding 前端协议。

项目：五行人格卡
目标：优化 `/admin/short-links/:shortCode` 移动端访问明细
用户场景：后台运营者在手机上查看来源、referer、campaign 和访问记录
视觉效果：运营工具，高密度但可扫描，长文本不撑破布局
必须保留：includeSynthetic、statSource、perf-test 口径、导出和分页行为
验收：build、contract、目标页截图、DOM 横向溢出、console/network、QA 记录
```

### 小型移动端修复

```text
请使用 codex-frontend-workflow skill，但这是小修，不要展开完整流程。

项目：五行人格卡
问题：[按钮溢出/文字重叠/触控目标不足]
目标页面：[路由/组件]
验收视口：[iPhone SE / 390x844 / desktop]
要求：最小改动，保留接口和 E2E，跑聚焦浏览器检查。
```

## Codex 最终汇报必须包含

```text
完成：
-

验证：
- `command`: 结果
- Browser/screenshots:
- DOM/console/network:
- Visual QA Scorecard:
- Environment blockers:
- Not applicable:

文件：
- `path`: 用途

未验证：
-

Residual risk：
-
```

## 红线

如果出现下面情况，视为没有完成：

- 没有读真实项目文件就开始改。
- 没有明确保留接口、路由、字段、testid 或 E2E。
- 只跑 build 就说视觉完成。
- 没有桌面/移动端浏览器或截图证据。
- 把环境阻塞写成已通过。
- 说“至少十小时”但提前标记整体完成。

## 本协议和其他文档的关系

| 文档 | 作用 |
| --- | --- |
| `docs/codex-frontend-start-here-20260622.md` | 一页式起点，帮助选择任务路线、提示词和验收证据。 |
| `docs/codex-frontend-operating-system-20260622.md` | 总控路线图。 |
| `docs/codex-frontend-prompt-pack-20260622.md` | 五行项目专项可复制提示词；通用提示词见 skill 模板。 |
| `docs/codex-frontend-real-task-examples-20260622.md` | 结果页、测试页、后台、截图还原、小修和 QA 的真实任务样例。 |
| `docs/frontend-workflow-playbook.md` | 五行项目级执行细则。 |
| `docs/frontend-visual-system.md` | 五行视觉规范。 |
| `custom-skills/codex-frontend-workflow/SKILL.md` | Codex 自动触发规则。 |
| `custom-skills/codex-frontend-workflow/templates/frontend-brief-quality-gate.md` | 开工前判断 brief 是否足够、是否需要假设/提问/拆分。 |
| `custom-skills/codex-frontend-workflow/references/frontend-failure-modes.md` | 发现工作流跑偏时的纠偏清单。 |
