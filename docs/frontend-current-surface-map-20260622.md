# 2026-06-22 前端当前文件地图

本文件从当前五行人格卡代码树读取整理，用于 `codex-frontend-workflow` 开工前快速定位真实前端表面。它不是设计愿景，而是当前代码和验收脚本的索引。

## 路由表

来源：`frontend/src/router/index.ts`

| 路由 | name | 页面组件 | 主要任务 |
| --- | --- | --- | --- |
| `/` | `guide` | `GuidePage.vue` | 首页引导、开始测试、手动短码匹配。 |
| `/test` | `test` | `TestPage.vue` | 出生信息、逐题答题、匹配邀请续流。 |
| `/result/:resultId` | `result` | `ResultPage.vue` | 展示人格结果、五行比例、分享和回流。 |
| `/match/:partnerShortCode/:currentShortCode` | `match` | `MatchPage.vue` | 双人五行匹配结果。 |
| `/admin` | `admin` | `AdminDashboard.vue` | 数据中台、漏斗、趋势、短链列表、导出。 |
| `/admin/short-links/:shortCode` | `admin-short-link-detail` | `AdminShortLinkDetail.vue` | 短链访问明细、来源、referer、campaign。 |
| `/:pathMatch(.*)*` | `not-found` | `NotFoundPage.vue` | 404 状态和返回入口。 |

## API Wrapper

来源：`frontend/src/api`

| 文件 | 职责 |
| --- | --- |
| `request.ts` | 请求基础封装、前后端代理口径、请求头。 |
| `questions.ts` | 题目读取。 |
| `results.ts` | 创建结果、读取结果详情。 |
| `matches.ts` | 创建双人匹配、读取匹配结果。 |
| `events.ts` | 访问/行为事件上报。 |
| `admin.ts` | 后台总览、短链列表、短链详情、导出。 |
| `types.ts` | 前端 DTO、结果、匹配、后台统计、短链访问类型。 |

重要类型：

- `CreateResultRequest`：出生信息和 5 道题答案。
- `CreateMatchRequest`：在结果创建基础上加入 `partnerShortCode`。
- `ResultDetail`：主副五行、比例、星官、关键词、解释、短码和短链。
- `MatchResult`：双方结果、匹配分、关系标签、优势和建议。
- `AdminOverview`：PV/UV/UIP、漏斗、趋势、渠道、短链列表和 synthetic 口径。
- `ShortLinkVisit`：访问时间、事件类型、hash、channel、campaign、device、referer、statSource。

## 页面与组件关系

| 场景 | 页面 | 关键组件/工具 | 风险点 |
| --- | --- | --- | --- |
| 首页 | `GuidePage.vue` | `tracker.ts`、`attribution.ts` | 不自动读剪贴板；手动短码入口不能压过主行动。 |
| 测试 | `TestPage.vue` | `QuestionCard.vue` | 出生信息、题目选项、按钮触控、答题进度。 |
| 结果 | `ResultPage.vue` | `PersonaCard.vue`、`ElementRatioCard.vue`、`ElementSpectrum.vue`、`ElementLegend.vue` | 身份信息、五行比例、解释文本、分享回流差异。 |
| 分享 | `ResultPage.vue` | `ShareLinkBox.vue`、`shareCard.ts` | `900x1200` 分享图、短链不带 channel/campaign、复制/系统分享 fallback。 |
| 匹配 | `MatchPage.vue` | `ElementMark.vue`、`ElementLegend.vue` | 双方短码、匹配结果、无 `.error-state`、无图形回退。 |
| 后台总览 | `AdminDashboard.vue` | `StatCard.vue` | token 登录、移动端 report toggle、CSV 导出、synthetic 口径。 |
| 短链详情 | `AdminShortLinkDetail.vue` | 移动访问卡片、桌面表格 | 长 referer/campaign 不得横向溢出，移动端隐藏 `.table-wrap`。 |
| 全局视觉 | `style.css` | `elementVisuals.ts`、`ElementMark.vue` | 五行色只做信息编码，避免图形回退和装饰化。 |

## E2E 和截图契约

### `frontend/e2e/mobile-main-flow.spec.mjs`

默认：

- `E2E_BASE_URL=http://127.0.0.1:5175`
- `E2E_ADMIN_TOKEN=dev-token`
- viewport `390x844`

覆盖：

- 首页进入测试。
- 出生年份/月选择。
- 5 道题选项。
- 结果页可见。
- 无横向溢出。
- 无 element mark 图形回退。
- 分享图下载，PNG `900x1200`。
- 短码/链接复制 fallback。
- 原生分享不可用 fallback。
- 短链跳转到分享回流结果页。
- 分享回流页不显示二次分享盒。
- 后台登录、移动报表分组、CSV 导出。
- 移动短链详情访问卡片可见，`.table-wrap` 隐藏。
- 手动短码匹配和匹配请求体字段。

高价值锚点：

- `start-test-link`
- `birth-year-quick-2002`
- `birth-inline-primary-action`
- `question-Q1-option-METAL` 到 `question-Q5-option-EARTH`
- `test-primary-action`
- `save-share-image`
- `native-share`
- `copy-tools-toggle`
- `copy-match-code`
- `copy-share-link`
- `manual-match-code`
- `manual-match-submit`
- `match-accept-button`
- `admin-login-button`
- `admin-mobile-report-toggle`
- `admin-export-csv`
- `shortlink-visit-card`

### `frontend/e2e/showcase-screenshots.spec.mjs`

默认：

- `E2E_BASE_URL=http://127.0.0.1:5175`
- `SHOWCASE_SCREENSHOT_DIR=docs/screenshots/showcase`
- mobile viewport：`iphone-se 375x667`、`android-wide 430x932`
- desktop viewport：`1280x900`

覆盖截图：

- mobile home。
- birth card 和 birth ready。
- question card。
- result。
- shared result。
- mobile admin overview。
- mobile short link detail。
- desktop result。
- desktop admin overview。
- desktop short link detail。
- desktop match。
- desktop not found。

关键几何断言：

- `expectNoHorizontalOverflow`
- `expectNoControlTextOverflow`
- `expectMinimumTouchTargets`
- `expectVerticalOrder`
- `expectNoElementMarkGraphics`

## Contract Script

来源：`scripts/verify-frontend-contracts.mjs`

运行前提：

- 需要先存在 `frontend/dist`，通常先跑 `npm --prefix frontend run build`。

关键覆盖：

- dist 中保留 `/api/questions`、`/api/results`、`/api/matches`、`/api/admin/short-links/export`。
- dist/source 中保留 `manual-match-code`、`matchCode`、`shared-result`、`share-box`、`start-test-link`、`test-primary-action`、`copy-share-link`、`admin-login-button`。
- 前端事件和归因头保留 `X-Channel`、`X-Campaign`。
- E2E 中保留题目 testid、分享 fallback、native share payload、后台 token 清理。
- CORS、external 短链、schema、运行手册和 deploy env 有对应契约检查。

## 本地模式

开发改造默认：

```text
frontend: http://127.0.0.1:5175
backend:  http://127.0.0.1:48081
admin:    dev-token
```

生产预览演示：

```text
frontend: http://127.0.0.1:4173
backend:  http://127.0.0.1:48081
admin:    dev-token
```

不要混用 `APP_BASE_URL` 和 `E2E_BASE_URL`。短链、截图和 live gate 必须指向同一个前端入口。

## 改造前必查清单

前端任务开工前，Codex 至少确认：

- 目标路由和页面组件。
- 是否涉及 API wrapper 或 DTO。
- 是否涉及现有 testid。
- 是否涉及分享图或短链参数。
- 是否涉及后台 synthetic / statSource / perf-test 口径。
- 需要跑哪些 E2E、showcase 或 contract。
- 是否需要新建 `docs/frontend-qa-record-YYYYMMDD.md`。

## 页面改造最小证据

| 改造范围 | 最小证据 |
| --- | --- |
| 首页/测试/结果/匹配 | build、contract、mobile E2E 或目标截图、DOM overflow 检查。 |
| 分享区 | 分享图尺寸、短链参数、fallback、回流页二次分享盒检查。 |
| 后台总览 | token 登录、移动分组、CSV 导出、desktop/mobile 截图。 |
| 短链详情 | 长文本不溢出、移动卡片、桌面表格、includeSynthetic/statSource 口径。 |
| 全局视觉 token | 至少一个 H5 页面和一个后台页面截图或 DOM 复核。 |

## 与前端工作流的关系

- `docs/codex-frontend-operating-system-20260622.md` 说明总流程。
- `docs/codex-webcoding-frontend-protocol-20260622.md` 说明怎么向 Codex 发需求。
- `docs/frontend-workflow-playbook.md` 说明五行项目怎么执行。
- 本文件说明当前代码树真实入口和验收锚点。
