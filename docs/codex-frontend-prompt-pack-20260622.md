# 五行 Codex 前端提示词包

记录日期：2026-06-22
关联 skill：`codex-frontend-workflow`

这份提示词包是五行人格卡项目适配层，用于在 webcoding 或 Codex 里稳定表达“我要的前端样式”，并要求 Codex 用真实项目、视觉 token、浏览器和截图来闭环。

通用提示词包在 `custom-skills/codex-frontend-workflow/templates/frontend-prompt-pack.md`；新项目应先用通用包，再按项目 reference 补专用字段，避免把五行路由、短链、视觉 token 误带到其他项目。

## 1. 启动一轮前端改造

```text
请使用 codex-frontend-workflow skill。

目标：[五行人格卡的页面/链路，例如结果页、分享区、匹配页、后台短链详情]
我想要的视觉效果：[关键词、参考截图、不要什么]
用户场景：[普通访问者/分享回流用户/后台运营者，在手机或桌面上做什么]
必须保留：[路由、API、testid、短链参数、admin token、统计口径、E2E 锚点]
时间盒：[例如 3 小时 / 至少 10 小时]
验收要求：[build / verify-frontend-contracts / mobile-e2e / showcase screenshots / live gate / QA 记录]

最终交付前必须使用 frontend visual QA scorecard；硬门禁未过时只能说视觉未完成验收。

先输出 Task Frame 和要读取的文件，不要直接开写。视觉方向不明确时，先给 2-3 个方向让我确认；如果方向明确，就转成 token、组件规则和第一片实现计划。
```

## 2. 把审美变成可执行设计

```text
请把下面这段审美要求转成五行人格卡可执行的视觉方向：

[粘贴你的描述]

只输出：
- 关键词
- 色彩 token
- 字体层级
- 页面密度
- 组件规则
- 动效规则
- 反目标

必须沿用“克制中式数据感”：暖纸底、深绿主行动、五行色只做信息编码、移动端一屏一任务、后台高密度可扫描。
```

## 3. 结果页/分享页优化

```text
请优化五行人格卡的结果页和分享区。

重点：
- 结果身份先被看见，再解释五行比例。
- 分享区像产品能力，不像调试工具。
- 分享回流页不要出现二次分享盒。
- 保留短链参数、归因字段、shareCard.ts 的 900x1200 输出。

请先读：
- frontend/src/pages/ResultPage.vue
- frontend/src/components/PersonaCard.vue
- frontend/src/components/ShareLinkBox.vue
- frontend/src/utils/shareCard.ts
- docs/frontend-visual-system.md
- docs/frontend-qa-record-20260619.md
- docs/frontend-workflow-playbook.md
```

## 4. 后台运营页面优化

```text
请按运营工具方式优化后台页面。

目标页面：[AdminDashboard / AdminShortLinkDetail]
重点问题：[长 referer / campaign / table overflow / 筛选密度 / 移动端可读性]

要求：
- 桌面端保留宽表和密度。
- 移动端改成分组、折叠或卡片，不靠横向滚动承载主信息。
- includeSynthetic、statSource、perf-test 口径必须清楚。
- 长文本不能撑破布局。
- 验证 DOM 横向溢出、截图和 console/network。
```

## 5. 小修轻量路径

```text
这是小型前端修复，不要展开完整流程。

问题：[按钮溢出/文字重叠/输入框占位不对/触控目标太小]
目标文件或页面：[路径或路由]
验收视口：[iPhone SE / Android wide / desktop]

请先定位相关样式和组件，做最小改动，然后用聚焦浏览器检查证明修好了。
```

## 6. 十小时时间盒

```text
请严格开展至少十小时的前端工作流。

目标：[要沉淀的流程或要完成的页面改造]
要求：
- 记录开始时间和最早 complete 时间。
- 未到最早 complete 时间前不能说整体完成，也不能把 goal 标记 complete。
- 可以阶段性产出 skill、eval、文档、截图、QA 记录、review 结果。
- 每一阶段都要有可追踪文件或验证命令。
```

## 7. QA 收口

```text
请做本轮五行人格卡前端 QA closeout。

范围：[页面/链路]
必须覆盖：
- npm --prefix frontend run build
- node scripts/verify-frontend-contracts.mjs
- scripts/mobile-e2e.sh 或说明为什么不适用
- scripts/capture-showcase-screenshots.sh 或目标截图
- scripts/frontend-live-gate.sh 或 local-preview-smoke-test.sh
- iPhone SE、安卓宽屏、桌面视口
- DOM 横向溢出、触控目标、console、network
- docs/frontend-qa-record-YYYYMMDD.md

不要把未跑过的检查写成已通过。
```

## 8. 多 Agent 拆分

```text
这次是较大的五行人格卡前端任务，可以使用多个 agent。

请按责任边界拆分：
- Context agent：只读路由、页面、API、E2E、QA 记录，输出必须保留项和风险。
- Visual agent：只输出五行项目 style brief、token、组件规则、反目标，不改业务逻辑。
- Page agent：只负责目标页面/组件的第一片实现，列出改动文件。
- Contract agent：只负责 build、verify-frontend-contracts、mobile-e2e、showcase screenshots、DOM 和 console/network。
- Docs agent：只负责 QA record、证据矩阵和未验证项，不把未跑过的页面视觉写成已通过。

小修不要拆 agent。并行 agent 不得覆盖彼此未审阅的改动。
```

## 9. 最终汇报格式

```text
完成：
-

验证：
- `command`: 结果
- Browser/screenshots:
- DOM:
- Console/network:

文件：
- `path`: 用途

未验证：
-

环境阻塞：
-

不适用：
-

残余风险：
-
```

## 10. 视觉 QA 评分

```text
请在最终交付前使用 frontend visual QA scorecard。

如果硬门禁不满足，不要说视觉完成：
- 真实页面/fixture 是否渲染：
- 桌面和移动范围：
- overflow/文字/触控：
- 主任务是否仍可完成：
- 旧路由/API/testid/统计/可访问性是否保留：
- 命令、URL、截图、DOM、console/network、未验证项是否记录：

请给 0-20 分评分，并说明结论：
- 18-20：可交付
- 14-17：阶段可接受，但要列 follow-up
- 10-13：需要再迭代
- 0-9：回到 brief 或真实项目读取

评分项：brief fit、product task clarity、visual hierarchy、token consistency、responsive behavior、component states、accessibility and touch、data integrity、browser health、handoff quality。
```
