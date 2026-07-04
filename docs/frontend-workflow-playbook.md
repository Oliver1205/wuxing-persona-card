# 五行项目级前端工作流 Playbook

记录日期：2026-06-22
关联 skill：`codex-frontend-workflow`

## 目标

这个 playbook 是五行人格卡的项目级前端流程。通用规则由 `codex-frontend-workflow` skill 承载；本文件负责把通用流程落到五行项目的路由、端口、视觉资产、QA 记录和验收脚本上。

Codex 不能凭感觉美化页面，必须先理解真实项目，再把视觉目标转成 token、组件、页面和浏览器证据。

## 直接可用提示词

```text
请使用 codex-frontend-workflow skill。

目标：[页面/链路]
我想要的视觉效果：[描述]
必须保留：[接口/路由/testid/埋点/数据字段]
时间盒：[例如 3 小时 / 至少 10 小时]
验收要求：[build / contract / E2E / screenshots / live gate / QA 记录]

先输出 Task Frame 和要读取的文件，不要直接开写。如果视觉方向不明确，先给 2-3 个方向让我确认。确认后按薄切片实现，并用浏览器和截图证明结果。
```

想直接复制真实任务，优先打开 `docs/codex-frontend-real-task-examples-20260622.md`，里面已经有结果页、测试页、后台短链详情、截图还原、小修和 QA closeout 的完整样例。

## 启动阶段

Codex 必须先输出：

```text
Flow:
User and scenario:
Desired visual direction:
Must preserve:
Acceptance evidence:
Files to inspect:
```

## 任务分流

长期复用时先判断任务形状，不要所有前端相关请求都套完整流程。

| 请求类型 | 路线 | 做法 |
| --- | --- | --- |
| 想要某种样式、截图/Figma 还原、产品级视觉、移动端体验、后台视觉层级 | 完整前端工作流 | Task Frame、真实项目读取、视觉方向、token/组件、薄切片、浏览器证据、QA 记录。 |
| 一个明确的小视觉 bug，例如按钮溢出、文字重叠、触控目标不足 | 轻量修复 | 只读目标样式/组件，做最小改动，跑聚焦视口或 DOM 检查。 |
| 纯 build/type/lint 报错，不涉及页面渲染 | 非视觉调试 | 不要求视觉方向和截图，直接定位错误并跑对应命令。 |
| 后端/API-only、README-only、Java review、Word/Excel | 不触发此流程 | 使用对应领域流程。 |
| 从零做 site/app/tool/game | Greenfield 前端 | 第一屏必须是可用体验，不做空 landing shell，启动本地服务并验桌面/移动端。 |
| QA closeout 或发布前检查 | 验证流程 | 收集命令、浏览器、截图、DOM、环境阻塞和残余风险。 |

五行项目默认读取：

- `frontend/src/router/index.ts`
- `frontend/src/pages`
- `frontend/src/components`
- `frontend/src/api`
- `frontend/src/style.css`
- `frontend/src/utils/elementVisuals.ts`
- `docs/frontend-token-inventory-20260622.md`
- `frontend/src/utils/shareCard.ts`
- `docs/frontend-current-surface-map-20260622.md`
- `scripts/verify-frontend-contracts.mjs`
- `frontend/e2e/mobile-main-flow.spec.mjs`
- `frontend/e2e/showcase-screenshots.spec.mjs`
- `docs/frontend-visual-system.md`
- `docs/frontend-qa-record-20260619.md`
- `docs/local-preview-runbook.md`
- `docs/ci-browser-e2e-plan.md`
- `design-final/01-home.png`
- `design-final/02-birth.png`
- `design-final/03-question.png`
- `design-final/04-result.png`
- `design-final/05-share.png`
- `docs/screenshots/showcase/`
- `docs/frontend-market-skills-research-20260622.md`
- `docs/codex-frontend-prompt-pack-20260622.md`

## 本地模式

后续前端改造默认使用开发联调模式，作品演示或生产预览再使用 preview 模式。

下面端口是默认候选，不是永远事实。每次 live gate、截图或 E2E 前先确认当前进程和 env：

- `FRONTEND_URL` / `APP_BASE_URL` / `E2E_BASE_URL` 必须指向同一个前端入口。
- `BACKEND_URL` / `BACKEND_PROXY_TARGET` 必须指向同一个后端入口。
- 如果本轮实际使用 `48082`、`5176` 或其他临时端口，QA 记录必须写实际端口，不沿用旧默认值。

| 模式 | 前端 | 后端 | 用途 |
| --- | --- | --- | --- |
| 开发改造 | `http://127.0.0.1:5175` | `http://127.0.0.1:48081` | Codex 改页面、跑 E2E、截图、live gate。 |
| 生产预览演示 | `http://127.0.0.1:4173` | `http://127.0.0.1:48081` | 构建后预览、文档站体验入口、演示截图。 |

开发改造启动：

```bash
cd backend
SERVER_PORT=48081 APP_BASE_URL=http://127.0.0.1:5175 ADMIN_TOKEN=dev-token \
mvn spring-boot:run -Dspring-boot.run.profiles=local

cd frontend
BACKEND_PROXY_TARGET=http://127.0.0.1:48081 npm run dev -- --host 127.0.0.1 --port 5175
```

生产预览演示启动：

```bash
cd backend
SERVER_PORT=48081 APP_BASE_URL=http://127.0.0.1:4173 ADMIN_TOKEN=dev-token \
mvn spring-boot:run -Dspring-boot.run.profiles=local

cd frontend
npm run build
BACKEND_PROXY_TARGET=http://127.0.0.1:48081 npm run preview -- --host 127.0.0.1 --port 4173
```

不要混用 `APP_BASE_URL` 和 `E2E_BASE_URL`：前端短链、截图、live gate 必须指向同一个前端入口。后端同理，`BACKEND_URL` 和 `BACKEND_PROXY_TARGET` 必须和实际进程一致。

运行前置检查：

```bash
curl -s -i "$BACKEND_URL/api/readiness"
```

## 前端改造入口速查

| 路由 / 场景 | 主要文件 | 视觉资产 | 主要验收 |
| --- | --- | --- | --- |
| `/` 首页 | `GuidePage.vue` | `design-final/01-home.png`、`iphone-se-01-home.png` | build、contract、showcase screenshot |
| `/test` 出生和问答 | `TestPage.vue`、`QuestionCard.vue` | `design-final/02-birth.png`、`design-final/03-question.png` | build、contract、mobile E2E |
| `/result/:resultId` 结果 | `ResultPage.vue`、`PersonaCard.vue`、`ElementRatioCard.vue` | `design-final/04-result.png`、`iphone-se-04-result.png` | build、contract、mobile E2E、showcase |
| 分享区 | `ShareLinkBox.vue`、`shareCard.ts` | `design-final/05-share.png`、`iphone-se-05-shared-result.png` | contract、mobile E2E、share screenshot |
| `/match/...` 匹配 | `MatchPage.vue` | `iphone-se-06-match.png`、`desktop-09-match.png` | mobile E2E、showcase |
| `/admin` 后台 | `AdminDashboard.vue`、`StatCard.vue` | `desktop-06-admin-overview.png`、移动后台截图 | contract、showcase、live gate |
| `/admin/short-links/:shortCode` | `AdminShortLinkDetail.vue` | `desktop-08-shortlink-detail.png`、移动短链详情截图 | contract、showcase、live gate |
| 全局视觉 | `style.css`、`elementVisuals.ts` | `docs/frontend-visual-system.md` | build、contract、目标页截图 |

## 视觉方向阶段

如果用户没有明确指定方向，Codex 给 2-3 个方向，每个方向只写：

- 关键词。
- 色彩 token。
- 字体层级。
- 页面密度。
- 组件规则。
- 反目标。

五行项目默认方向：

```text
克制中式数据感：暖纸底、深绿主行动、五行色只做信息编码、移动端一屏一任务、后台高密度可扫描。
```

## 实现阶段

顺序固定：

1. 保护契约：路由、API wrapper、字段、testid、埋点、可访问性语义。
2. 更新 token 或共享组件。
3. 修改目标页面。
4. 同步契约脚本或 E2E。
5. 截图和 QA 记录。

不要：

- 多个 agent 同时改同一个 `.vue` 文件。
- 为视觉效果改接口字段。
- 只改 CSS 不看真实浏览器。
- 把未验证项写成已验证。

## 多 Agent 分工

| Agent | 职责 | 禁止 |
| --- | --- | --- |
| Context | 读 router、页面、组件、API、QA 文档，输出文件地图。 | 不改代码。 |
| Visual | 生成视觉方向、token、截图差异清单。 | 不改业务逻辑。 |
| Page | 改目标页面和组件。 | 不改 QA 脚本之外的无关文件。 |
| Contract | 补 `verify-frontend-contracts.mjs`、E2E、截图断言。 | 不重写页面视觉。 |
| Integration | 处理端口、API、CORS、live gate、后台口径。 | 不凭页面加载宣布通过。 |
| Docs | 更新 QA 记录、playbook、索引。 | 不夸大验证结果。 |

## 验收矩阵

| 改动 | 前置条件 | 必跑 | 通过标准 |
| --- | --- | --- | --- |
| 文档或流程 | 不需要服务 | 文件存在、链接搜索、JSON/Markdown 自检 | 文档能从 README/docs-site 找到，JSON 可解析。 |
| 小样式 | 目标页面可打开 | `npm --prefix frontend run build`、目标页浏览器检查 | 无横向溢出、文字不重叠、目标控件样式正确。 |
| 页面结构 | 前端依赖已安装 | build、`node scripts/verify-frontend-contracts.mjs` | 不破坏路由、testid、分享/后台契约。 |
| H5 主流程 | 前后端开发模式已启动 | build、contract、`scripts/mobile-e2e.sh` | `/test -> /result -> /share/match` 主流程全绿。 |
| 视觉截图 | 前后端开发模式已启动 | `scripts/capture-showcase-screenshots.sh` | iPhone SE、安卓宽屏、桌面目标截图落盘且几何校验通过。 |
| 前后端联调 | 先确认实际前端/后端 env，同源入口一致 | `scripts/frontend-live-gate.sh` 或 `scripts/local-preview-smoke-test.sh` | readiness、短链、匹配、后台导出和 runtime 均通过。 |
| 发布前 | 质量脚本已纳入 git | `REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh` | 后端测试、前端 build、契约、静态预览、schema 和 compose config 通过。 |

`scripts/quality-check.sh` 不等于真实前后端联调。它证明 fresh checkout 的质量门可跑；真实短链、匹配、后台导出和前端代理要看 `frontend-live-gate.sh` 或 `local-preview-smoke-test.sh`。

失败归类：

- 代码回归：本轮改动导致 build、contract、E2E 或 DOM 检查失败，必须修。
- 环境阻塞：端口、Docker、浏览器依赖、网络、权限导致，记录阻塞点和可复跑命令。
- 未覆盖风险：当前没有条件验证，最终汇报必须明确标注。

## QA 记录策略

新一轮视觉或交互改造新建当日记录：

```text
docs/frontend-qa-record-YYYYMMDD.md
```

`docs/frontend-qa-record-20260619.md` 是历史基线，不继续混写。新记录需要从 README 或 docs-site 链接，或者在阶段性文档中说明它仍是草稿。

## QA 记录模板

```markdown
# YYYY-MM-DD 前端 QA 记录

## 本轮范围

-

## 关键改动

-

## 已验证

- `command`: 通过/失败/环境阻塞

## 截图与视觉复核

- `path`: 视口、页面、复核点

## In-App Browser DOM 复核

- 页面：
- 横向溢出：
- 触控目标：
- Console：
- Network：

## 未验证或环境阻塞

-

## 残余风险

-
```

## 最终汇报格式

```markdown
**Done**
完成了什么。

**Verification**
- `command`: result
- Browser/screenshots:

**Files**
- `path`: purpose

**Residual Risk**
- 未覆盖项。
```

真实 UI 改造最终验收时，QA 记录不能单独替代页面证据。最终汇报必须至少包含浏览器、截图、DOM 或 E2E 之一；文档/skill 工作流可以用 manifest、package verifier、JSON parse、README/docs-site 入口和 QA 记录证明，但必须明确“不涉及真实页面视觉验收”。

## 至少 N 小时时间盒纪律

如果用户要求“至少 N 小时”：

1. 记录开始时间和最早收口时间。
2. 在最早收口时间前不把整体 goal 标记为 complete。
3. 可以阶段性汇报“skill 已创建”“eval 已完成”“文档已接入”，但不能说整个工作流完成。
4. 时间窗口内继续做有价值的补强：trigger eval、review HTML、模板、QA 文档、打包、安装验证、项目索引、下一轮 eval。
