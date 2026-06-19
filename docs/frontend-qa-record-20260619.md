# 2026-06-19 前端交互与联调 QA 记录

## 本轮范围

- 结果页分享入口收敛为单一分享模块，统一承载“保存图、复制短码、复制链接、系统分享”。
- 结果页分享区展示干净短链文本，复制和系统分享仍使用带归因参数的真实 URL，避免用户端看到调试感 query。
- 首页匹配入口支持粘贴完整 `/s/{shortCode}?channel=...` 分享链接，不再只接受裸短码。
- 后台短链访问明细补齐 `includeSynthetic` 后端契约，默认排除 `perf-test`，勾选后包含测试流量。
- 后台短链访问明细新增 `statSource=local|external` 来源契约，来源筛选和测试流量开关拆开。
- 测评出生页小屏在月份网格后新增内联继续按钮，选择月份后无需滚到表单末尾再继续。
- 后台移动端密度优化：核心指标小屏双列，详细报表默认折叠，短链访问明细小屏改为卡片列表，访问事件运行态默认收起，减少调试字段压迫。
- 后台日期筛选控件空值态改为中文占位，避免移动端出现 `mm/dd/yyyy` 原生占位破坏中文界面一致性；移动端关键可点控件保持至少 44px 触控目标。
- 结果页分享框继续降噪：外露主动作收敛为“保存分享图 / 系统分享”，复制短码和复制链接进入“复制备用信息”折叠区。
- 匹配页移除装饰性“五行参照”图例，改为基于双方主元素的关系参照说明，避免只摆五个元素字。
- 后端上线风险加固：fresh MySQL schema 去掉重复 DDL；`statSource` 显式筛选增加 500 条扫描上限，外部统计失败返回明确 502。
- 外部短链创建失败语义补强：`fallback-to-internal=false` 时，外部创建失败返回 `502` 语义；本地短码冲突仍保持本地业务错误。
- `quality-check.sh` 不再依赖未跟踪的 `outputs/verify-wuxing-preview*.mjs`，改为调用 `scripts/` 下可纳入仓库的 verifier，并纳入 fresh MySQL Docker schema smoke。

## 关键改动

- `frontend/src/pages/ResultPage.vue`
  - 移除人格卡后的重复快捷操作条，普通结果页只保留一个 `ShareLinkBox` 分享模块。
  - 分享落地页 CTA 带上 `matchCode`，测完自己的卡后可继续进入双人匹配。
- `frontend/src/components/ShareLinkBox.vue`
  - 分享盒统一承载保存分享图、复制短码、复制链接和系统分享。
  - 保留 `#share-box`、`save-share-image`、`copy-tools-toggle`、`copy-match-code`、`copy-share-link`、`native-share` 测试锚点。
  - 分享操作按钮保持 44px 触控目标；复制短码和复制链接默认折叠到“复制备用信息”，减少结果页工具面板感。
  - 页面展示去掉 `channel/campaign` query 的干净短链；复制动作继续复制带分享归因的 URL。
- `frontend/src/pages/MatchPage.vue`
  - 用“关系参照”卡片替代 compact 五行图例，动态说明双方主元素分别贡献的相处节奏和重点。
  - 标题去除后端拼接空格；底部新增短码输入，可直接用当前卡继续匹配新的朋友短码。
- `frontend/src/pages/TestPage.vue`
  - 步骤切换、年份滑杆、年月日选项、底部主按钮和返回按钮统一保持 44px 触控目标。
  - 主行动按钮禁用态改为浅灰绿、无阴影、`opacity: 1`，避免“选择月份后继续”在不可点击时仍像主 CTA。
  - 小屏出生信息步骤在月份选择后提供内联继续按钮，保持首步推进路径可见且不遮挡内容。
- `frontend/src/pages/GuidePage.vue`
  - 剪贴板检测、手动匹配相关按钮保持 44px 触控目标，首页移动端可点区域与后台一致。
- `frontend/src/pages/AdminDashboard.vue`
  - 移动端新增“详细报表”折叠入口，趋势、漏斗、排行和短链列表默认收起；桌面端继续展示完整数据中台。
  - 筛选、清空、快捷日期、分页和聚合刷新后，overview 请求带 `forceRefresh=true`，避免刚写入样本被短缓存遮住。
  - 移动端点击“定位短链明细”等行动建议时，会先展开折叠报表，再滚动到目标区。
  - 日期筛选输入保留原生 `type=date` 与 `YYYY-MM-DD` 值契约，但空值时显示“选择开始日期/选择结束日期”中文占位。
  - 移动端快捷日期按钮、报表展开按钮和行动链接保持 44px 触控目标；筛选占位改为“输入短码或结果 ID”。
- `frontend/src/pages/AdminShortLinkDetail.vue`
  - 移动端隐藏宽表格，改用访问事件卡片展示时间、来源、渠道、活动、设备、referer 和排查哈希字段。
  - 访问时间统一格式到秒，避免移动端长 ISO 时间把卡片撑乱。
  - 外部访问记录缺匿名 hash 时显示 `-`，外部事件码显示为“外部短链访问”，顶部来源改为“明细来源”，并明确测试流量开关只影响 `perf-test` 排除。
  - 外部平台访问明细未返回 channel/campaign 时，前端显示“外部平台未返回”，避免运营把上游未提供字段误读成归因丢失。
  - 日期筛选输入同步使用中文空值占位，有值时覆盖层自动消失，不遮挡真实日期。
  - 返回按钮、分页选择器保持 44px 触控目标；移动端分页只保留“上一页/下一页”主操作，隐藏“首页/末页”边缘操作，访问记录卡片更早出现。
  - 顶部动态 chip 允许长关键词换行，避免硬撑横向。
- `frontend/src/style.css`
  - 新增 `.date-input-shell`，隐藏原生空日期文本并展示中文占位；有值时恢复真实日期颜色。
  - `.inline-debug summary` 改为 `inline-flex` 并保持 44px 触控目标，短链详情“技术明细”可点击区不再过小。
- `frontend/src/pages/GuidePage.vue`
  - 手动匹配输入框文案改为“短码或分享链接”。
  - `normalizeClipboardShortCode()` 支持纯短码、完整 URL、带 query 的 `/s/{code}` 分享链接。
- `backend/src/main/java/com/wuxing/persona/controller/AdminController.java`
  - `GET /api/admin/short-links/{shortCode}/visits` 新增 `includeSynthetic` 参数。
  - `GET /api/admin/short-links/{shortCode}/visits` 新增 `statSource=local|external` 参数，外部来源不再绑定 `includeSynthetic=true`。
- `backend/src/main/java/com/wuxing/persona/service/AdminStatService.java`
  - 本地短链访问明细列表和 total 同步使用 synthetic 排除口径。
  - `statSource` 显式筛选最多扫描 500 条短链，超过时返回业务错误；显式筛外部统计时使用 strict external stats，外部不可用不再静默当成本地统计。
- `backend/src/main/java/com/wuxing/persona/service/shortlink/ExternalShortLinkStatsAdapter.java`
  - 新增 `fetchStatsStrict()`，外部统计失败时返回 `BusinessException(502, ...)`。
  - strict 模式下外部统计未启用或短链域名不匹配也返回明确 502，不再静默当成空外部统计。
- `backend/src/main/java/com/wuxing/persona/service/shortlink/ExternalShortLinkProvider.java`
  - 区分远端创建失败和本地绑定失败：远端失败在 fallback 开启时仍可回退到 internal，fallback 关闭时返回 `BusinessException(502, ...)`。
- `backend/src/main/resources/db/schema.sql`
  - 移除 fresh schema 中重复的 `visit_event` legacy `ALTER TABLE` / `CREATE INDEX` 语句。
- `backend/src/main/java/com/wuxing/persona/mapper/VisitEventMapper.java`
  - 新增短链访问明细的 `count/list ... ExcludingChannel` 查询。
- `backend/src/test/java/com/wuxing/persona/MvpFlowIntegrationTest.java`
  - 新增默认排除、`includeSynthetic=true` 包含测试流量的短链访问明细集成测试。
  - 新增 `statSource` 扫描上限和外部统计失败语义测试。
- `frontend/e2e/mobile-main-flow.spec.mjs`
  - 新增“完整分享链接填入首页匹配框可识别候选”的移动端 E2E。
  - 分享框 E2E 改为先验证复制按钮默认隐藏，再展开 `copy-tools-toggle` 验证复制短码/链接 fallback。
  - 新增后台短链访问详情移动端卡片断言，并确认小屏下宽表格隐藏。
  - 移动浏览器上下文使用真实移动 UA，后端设备识别和移动测试语义保持一致。
- `frontend/e2e/showcase-screenshots.spec.mjs`
  - 新增移动短链详情截图和移动后台展开报表截图；行动建议定位链接会先展开隐藏报表再定位。
- `scripts/quality-check.sh`
  - 静态预览 verifier 改为 `scripts/verify-wuxing-preview*.mjs`。
  - 新增 fresh MySQL schema 重复 legacy DDL 静态检查。
- `scripts/verify-wuxing-preview.mjs`
  - `flow verifier exists` 检查改为指向 `scripts/verify-wuxing-preview-flow.mjs`，不再要求旧的 `outputs/verify-wuxing-preview-flow.mjs` 产物存在。
- `scripts/mysql-schema-smoke-test.sh`
  - 新增 Docker MySQL 8.4 fresh schema 初始化检查，验证 5 张核心表、mapper 依赖列，并插入/查询一条 result + short link + visit event 链路样本。
- `frontend/e2e/showcase-screenshots.spec.mjs`
  - 移动后台截图门新增日期空值/有值 computed-style 断言，以及关键按钮、链接、输入和选择器的 44px 触控目标断言。
  - 短链详情移动端新增超长关键词 chip 断言，确认动态筛选口径不会产生横向溢出。
  - 首页、出生年月、题卡、结果页、分享落地页、匹配页、404 页也纳入 44px 触控目标断言。
  - 可展开的 `<summary>` 技术明细也纳入触控目标检查，避免 disclosure 文本成为低于 44px 的漏网控件。

## 已验证

- `mvn -q -f backend/pom.xml -Dtest=MvpFlowIntegrationTest test`：通过。
- `npm --prefix frontend run build`：通过。
- `node scripts/verify-frontend-contracts.mjs`：通过。
- `node scripts/verify-wuxing-preview.mjs`：通过。
- `node scripts/verify-wuxing-preview-flow.mjs`：通过。
- `node scripts/verify-wuxing-browser.mjs`：通过，生成 7 张 `outputs/browser-check-*` 静态预览截图。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：8 个 Chromium 用例通过。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`：11 个 Chromium 截图用例通过。
- `FRONTEND_URL=http://127.0.0.1:5175 BACKEND_URL=http://127.0.0.1:48081 scripts/frontend-live-gate.sh`：通过，最新后端进程已重启到本轮代码。
- `scripts/quality-check.sh`：通过，覆盖全量后端测试、前端构建、契约、静态预览浏览器验证、fresh schema 静态检查和 Docker compose 配置。

短链访问详情移动端卡片追加验证：

- `npm --prefix frontend run build`：通过。
- `node scripts/verify-frontend-contracts.mjs`：通过，覆盖 `shortlink-visit-card`、移动端隐藏表格、卡片列表展示、时间格式化和移动 UA。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：9 个 Chromium 用例通过，覆盖 `/admin/short-links/{shortCode}?includeSynthetic=true` 小屏卡片可见，并追加外部访问记录匿名 hash 缺失时的移动卡片展示。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`：重新通过，修正桌面短链详情截图断言为表格单元格，避免移动卡片隐藏 DOM 造成严格模式歧义。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`：再次通过，移动后台展开报表截图和移动短链详情截图均纳入 25 张 showcase 产物。
- `FRONTEND_URL=http://127.0.0.1:5175 BACKEND_URL=http://127.0.0.1:48081 scripts/frontend-live-gate.sh`：重新通过，覆盖 smoke、移动 E2E、11 个 showcase 截图用例和 25 张截图产物校验。
- `scripts/quality-check.sh`：重新通过，覆盖 `git diff --check`、后端全量测试、前端构建、契约、静态预览 browser check 和 Docker compose 配置。
- `node scripts/verify-frontend-contracts.mjs`：再次通过，覆盖移动报表定位滚动、短链明细移动卡片、外部访问空 hash、25 张 showcase 截图几何约束和 strict external stats 契约。
- `mvn -q -f backend/pom.xml -Pcontainer-it verify`：命令退出 0，但 `MvpMySqlContainerIT` 因 Testcontainers 无法接入当前宿主 Docker 环境而跳过；这不是 fresh MySQL schema 的通过证据。
- Docker CLI 直连 MySQL 8.4 schema 导入：通过，临时容器成功加载 `backend/src/main/resources/db/schema.sql`，`mysqladmin ping` 返回 `mysqld is alive`，核心表数量为 5。
- `npm --prefix frontend run build`：日期占位小修后重新通过。
- `node scripts/verify-frontend-contracts.mjs`：日期占位小修后重新通过，新增 `admin-date-placeholder-contract`。
- `git diff --check`：日期占位小修后重新通过。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：触控目标和中文占位修正后沙箱外重新通过，9 个 Chromium 用例全绿。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`：用户端全路径触控目标、后台触控目标、日期 computed-style 和长关键词 chip 断言加入后沙箱外重新通过，11 个 Chromium 截图用例全绿。
- `npm --prefix frontend run build`：分享折叠和匹配关系参照调整后重新通过。
- `node scripts/verify-frontend-contracts.mjs`：分享折叠、关系参照和质量契约更新后重新通过。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`：分享折叠和匹配关系参照调整后重新通过，11 个 Chromium 截图用例全绿。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：分享复制折叠交互调整后重新通过，9 个 Chromium 用例全绿。
- `mvn -q -f backend/pom.xml -Dtest=ExternalShortLinkProviderTest test`：通过，覆盖外部创建失败 fallback 与 fallback 关闭时 502 语义。
- `npm --prefix frontend run build`：短链详情移动分页压缩后重新通过。
- `node scripts/verify-frontend-contracts.mjs`：短链详情移动分页压缩契约加入后重新通过。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`：短链详情移动分页压缩后重新通过，11 个 Chromium 截图用例全绿。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：短链详情移动分页压缩后重新通过，9 个 Chromium 用例全绿。
- `npm --prefix frontend run build`：外部访问明细缺失归因提示加入后重新通过。
- `node scripts/verify-frontend-contracts.mjs`：外部访问明细缺失归因提示契约加入后重新通过。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：外部访问明细缺失归因提示加入后重新通过，9 个 Chromium 用例全绿。
- `npm --prefix frontend run build`：测试页禁用主按钮视觉状态调整后重新通过。
- `node scripts/verify-frontend-contracts.mjs`：测试页禁用主按钮视觉契约加入后重新通过。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`：测试页禁用主按钮视觉调整后重新通过，11 个 Chromium 截图用例全绿。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：测试页禁用主按钮视觉调整后重新通过，9 个 Chromium 用例全绿。
- `node scripts/verify-wuxing-preview.mjs`：旧 `outputs/verify-wuxing-preview-flow.mjs` 依赖迁移到 `scripts/` 后重新通过。
- `node scripts/verify-frontend-contracts.mjs`：旧 verifier 依赖迁移后重新通过。
- `scripts/quality-check.sh`：旧 verifier 依赖迁移后重新通过，主门禁不再要求 `outputs/verify-wuxing-preview-flow.mjs`。
- `npm --prefix frontend run build`：后台移动端详细报表二级分组与证据定位专注展开后重新通过。
- `node scripts/verify-frontend-contracts.mjs`：补充移动端详细报表分组、目标分组独占展开、日趋势归属趋势分组、移动 E2E 分组点击契约后通过。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`：后台移动端详细报表二级分组后重新通过，11 个 Chromium 截图用例全绿。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：移动端后台新分组交互适配后重新通过，9 个 Chromium 用例全绿。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`：新增后台“指标与链路”和“趋势与运行态”分组截图后重新通过，11 个 Chromium 截图用例全绿。
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：移动端详细报表改为受控 accordion 后重新通过，9 个 Chromium 用例全绿。
- `node scripts/verify-frontend-contracts.mjs`：新增 `admin-mobile-report-collapse-contract`、`admin-mobile-report-evidence-contract`、`admin-mobile-report-showcase-contract`、`ci-browser-e2e-contract` checked 名称后通过。
- `scripts/verify-eight-hour-artifacts.sh`：showcase PNG 重拍后通过；该阶段为 29 张截图，后续已扩展到当前 31 张 showcase PNG 几何约束。
- `ruby -e "require 'yaml'; YAML.load_file('.github/workflows/quality-gate.yml')"`：CI workflow YAML 解析通过，`browser-e2e` job 已落到质量门 workflow。
- `mvn -q -f backend/pom.xml -Dtest=MvpFlowIntegrationTest#shouldExcludeSyntheticTrafficFromDefaultShortLinkVisitDetails+shouldKeepDailyMetricSourceForAggregatedZeroVisitShortLinks test`：主质量门首次遇到异步访问事件等待抖动后，单独复跑两个失败用例通过。
- `scripts/quality-check.sh`：上述后端异步等待抖动复核后完整重跑通过，覆盖 Maven 测试、前端构建、前端契约、静态预览浏览器验证、fresh MySQL schema smoke 和 docker compose config。
- `scripts/mysql-schema-smoke-test.sh`：沙箱外通过，fresh MySQL 8.4 初始化、核心表/列检查、短链访问样本插入与联表查询均通过。
- `scripts/quality-check.sh`：沙箱外重新通过，主质量门已包含 `mysql-schema-smoke-test.sh`。

## Live Gate 证据

最近一次 `frontend-live-gate.sh`：

- `resultId=R20260619014952151565757`
- `shortCode=NgbPZO`
- `shortUrl=http://127.0.0.1:5175/s/NgbPZO`
- `matchId=NgbPZO-4qBK2n`
- `currentShortCode=4qBK2n`
- `compatibilityScore=79`
- `redirectLocation=/result/R20260619014952151565757?sc=NgbPZO`
- `sharedRedirectLocation=/result/R20260619014952151565757?sc=NgbPZO&channel=perf-test&campaign=result-card`
- `runtimeHealth=ok`
- `readinessStatus=UP`
- `frontendReadinessStatus=UP`
- `syntheticChannel=perf-test`
- `syntheticCampaign=local-preview-smoke`

## 截图与视觉复核

- `docs/screenshots/showcase/iphone-se-04-result.png`：`375x3201`，结果页仅保留单一分享模块，五行标识无简笔画；复制短码/链接折叠为“复制备用信息”。
- `docs/screenshots/showcase/iphone-se-05-shared-result.png`：`375x3026`，分享落地页不显示二次分享盒，CTA 文案与 `matchCode` 行为一致。
- `docs/screenshots/showcase/iphone-se-06-match.png`：`375x2354`，匹配页关系参照替换装饰性五行图例，移动端排版无挤压。
- `docs/screenshots/showcase/iphone-se-02-test-birth-card.png`：基础信息页底部禁用主按钮为浅灰绿，和可点击主 CTA 区分明显。
- `docs/screenshots/showcase/iphone-se-08-admin-overview.png`：`375x3389`，移动后台默认展示核心概览、中文日期占位、44px 触控按钮和详细报表开关，无页面级横向溢出。
- `docs/screenshots/showcase/android-wide-08-admin-overview.png`：`430x3291`，移动后台长报表默认折叠，日期筛选保持中文占位。
- `docs/screenshots/showcase/iphone-se-09-shortlink-detail.png`：`375x1309`，短链访问详情小屏卡片可读，移动分页压缩为“上一页/下一页”，日期筛选/分页/返回按钮和技术明细 disclosure 保持 44px 触控目标，测试 UA 识别为 `mobile`。
- `docs/screenshots/showcase/android-wide-09-shortlink-detail.png`：`430x1309`，短链访问详情小屏卡片可读，移动分页不再被四个大按钮占用。
- `docs/screenshots/showcase/iphone-se-10-admin-report-expanded.png`：`375x5399`，移动端详细报表拆为“指标与链路 / 趋势与运行态 / 归因与短链”；证据跳转只展开目标分组，短链证据图从 1.2 万像素级压到 5.4k。
- `docs/screenshots/showcase/iphone-se-11-admin-report-core.png`：`375x7654`，单独证明“指标与链路”分组展开态；统计卡片、运营雷达和转化链路无横向溢出，移动端长说明已压缩为更紧的摘要阅读。
- `docs/screenshots/showcase/iphone-se-12-admin-report-trend.png`：`375x6435`，单独证明“趋势与运行态”分组展开态；受控 accordion 后不再叠加核心分组。
- `docs/screenshots/showcase/android-wide-10-admin-report-expanded.png`：`430x5246`，移动端二级分组同样无横向溢出，短链证据定位后归因与短链内容可读。
- `docs/screenshots/showcase/android-wide-11-admin-report-core.png`：`430x7392`，安卓宽屏下核心分组展开态可读。
- `docs/screenshots/showcase/android-wide-12-admin-report-trend.png`：`430x6309`，安卓宽屏下趋势分组展开态可读。
- `docs/screenshots/showcase/desktop-06-admin-overview.png`：`1280x6342`，桌面端仍展示完整连续后台报表，不受移动端二级折叠影响。
- `docs/screenshots/showcase/desktop-07-result.png`：`1280x2151`，桌面结果页分享框主动作与复制备用区层级清晰。
- `docs/screenshots/showcase/desktop-08-shortlink-detail.png`：`1280x900`，桌面短链访问详情宽表格可读。
- `docs/screenshots/showcase/desktop-09-match.png`：`1280x1344`，桌面匹配页关系参照三列展示无重叠。

## In-App Browser DOM 复核

- 普通结果页 `/result/R20260619011725328373414`：
  - `hasHorizontalOverflow=false`
  - `hasShareBox=true`
  - `saveShareImageVisible=true`
  - `duplicatePrimarySave=false`
  - `copyToolsToggleVisible=true`
  - `copyCodeVisible=false`（默认折叠，E2E 已覆盖展开后复制）
  - `copyLinkVisible=false`（默认折叠，E2E 已覆盖展开后复制）
  - `nativeShareVisible=true`
  - `elementMarkGraphicCount=0`
  - `overflowingControls=[]`
- 分享落地页 `/result/R20260619011725328373414?sc=GBIdKU`：
  - `hasHorizontalOverflow=false`
  - `hasShareBox=false`
  - `ctaHasMatchCode=true`
  - CTA 链接为 `/test?channel=shared-result&campaign=result-banner&matchCode=GBIdKU` 和 `/test?channel=shared-result&campaign=result-footer&matchCode=GBIdKU`
  - `elementMarkGraphicCount=0`
  - `overflowingControls=[]`
- 后台短链详情 `/admin/short-links/GBIdKU?includeSynthetic=true`：
  - `hasHorizontalOverflow=false`
  - `hasVisitCard=true`
  - `.visit-card-list display=grid`
  - `.table-wrap display=none`
- 后台日期控件空值态：
  - `/admin?token=dev-token` 与 `/admin/short-links/NgbPZO?includeSynthetic=true&token=dev-token` 均 `hasHorizontalOverflow=false`
  - 两个日期 shell 均显示 `afterContent="选择开始日期"` / `afterContent="选择结束日期"`
  - 空值 input 仍为 `type=date`，且带 `aria-label="开始日期，格式 YYYY-MM-DD"` / `aria-label="结束日期，格式 YYYY-MM-DD"`
- 后台日期控件有值态：
  - `startDate=2026-06-01&endDate=2026-06-19` 时覆盖层 `afterContent=none`
  - input 值保持 `2026-06-01` / `2026-06-19`，文字颜色恢复为主文本色
- 后台行动建议定位：
  - 点击 `#shortlink-section` 行动链接后 `reportOpen=true`
  - `hash=#shortlink-section`
  - `shortLinkTop=450`，`shortLinkBottom=802`，`viewportHeight=812`
  - `visibleInViewport=true`
  - `hasHorizontalOverflow=false`
- 后台 `/admin`：
  - `hasHorizontalOverflow=false`
  - `.admin-desktop-page scrollWidth=clientWidth=1280`
  - `focusGridVisible=true`
  - `mobileReportToggleVisible=true`
  - `shortLinkSectionVisible=false`（移动截图默认折叠；E2E 已验证展开后可见）
  - `topChannelVisible=true`
  - `runtimeDetailsOpen=false`
  - `overflowingControls=[]`

本轮 in-app Browser 的 `Page.captureScreenshot` 在长页上超时；视觉截图证据以 Playwright showcase PNG 和静态预览 browser-check PNG 为准。

## Live API 口径复核

对最近 live gate 短码请求后台短链访问明细：

- `?includeSynthetic=false`：HTTP 200，`total=0`。
- `?includeSynthetic=true`：HTTP 200，`total=2`，返回 `perf-test` 测试访问记录。

## Fresh MySQL Schema 复核

- Docker daemon 直连可用，`docker info` 返回 ServerVersion `29.4.2`。
- `mysql:8.4` 本地镜像存在，临时容器 `wuxing-schema-check-codex` 使用 `schema.sql` 初始化成功。
- 初始化后表清单为 `short_link`、`short_link_daily_metric`、`site_daily_metric`、`user_result`、`visit_event`，`table_count=5`。
- `visit_event` 与 `short_link` 核心列检查通过，说明 fresh schema 中重复 legacy DDL 已被清理。
- `scripts/mysql-schema-smoke-test.sh` 已纳入 `quality-check.sh`，主质量门会启动临时 MySQL 8.4、导入 fresh schema、插入 `R-SCHEMA-SMOKE` / `SCSMOK` / `SHORT_LINK_VISIT` 样本并联表查询。
- Testcontainers 路径仍受宿主 Java/docker-java Docker info BadRequest 影响，`MvpMySqlContainerIT` 当前是 skipped；本轮不把它记为通过，而是用 Docker CLI smoke 作为 fresh MySQL schema 强证据。

## 2026-06-19 深夜续轮验证

- 用户侧视觉修复：
  - 出生页移动端不再用 fixed 底部条压住标题，改为月份网格后的内联继续按钮；`iphone-se-02-test-birth-card.png` 复核为 `375x1891`，无标题遮挡、无重复底部动作区。
  - 结果页分享模块展示 `/s/{shortCode}` 干净短链；复制和系统分享仍使用带 `channel=share&campaign=result-card` 的真实 URL，移动 E2E 断言展示文本不含 `channel=` / `campaign=`。
  - 结果页窄屏标题改为“朋友最容易认出的三个表现”，避免“3 / 个表现”断行割裂。
  - 匹配页 headline 去掉后端拼接空格，底部新增新短码输入，可直接跳转 `/match/{newCode}/{currentCode}` 继续匹配。
  - NotFound 异常态补充短链失效/分享链接截断/路径错误说明，并增加复制当前地址恢复动作。
- 后台与接口契约：
  - `GET /api/admin/short-links/{shortCode}/visits` 新增 `statSource=local|external`，`includeSynthetic` 只控制是否排除 `perf-test`。
  - 详情页从后台列表继承 `statSource`，顶部展示“明细来源 外部平台/本地统计”，并提示测试流量开关只影响 `perf-test` 排除。
  - external smoke 追加默认明细、显式 `statSource` 明细和 `includeSynthetic=true` 明细三类请求，并对分页 `total`、首条记录 `statSource` 做断言。
  - external smoke 将裸 `/s/{shortCode}` 与带归因 `/s/{shortCode}?channel=share&campaign=result-card` 分开验证：本地兼容入口默认保留 `sc`，带归因入口必须继续透传 `channel/campaign`。
- CI 与失败诊断：
  - `quality-gate.yml` 增加关键质量门脚本 `git ls-files --error-unmatch` 早期检查，防止未跟踪脚本导致 CI checkout 后断链。
  - `quality-gate.yml` 的早期检查覆盖 `quality-check.sh` 直接引用的 `frontend-live-gate.sh`、`performance-limit-test.sh`、`local-preview-smoke-test.sh`。
  - browser E2E artifact 上传补充 `frontend/test-results/` 与 `frontend/playwright-report/`。
  - `mobile-e2e.sh` 与 `capture-showcase-screenshots.sh` 使用 `--trace=retain-on-failure --reporter=line,html`，CI 失败时保留 Playwright trace。
  - `scripts/verify-eight-hour-artifacts.sh` 不再依赖未跟踪 `outputs/*/presentations/.../contact-sheet.png`，只校验已归档 `docs/artifacts/presentations/contact-sheet.png`。
- 最新通过项：
  - `npm --prefix frontend run build`
  - `node scripts/verify-frontend-contracts.mjs`
  - `git diff --check`
  - `mvn -q -f backend/pom.xml -Dtest=MvpFlowIntegrationTest#shouldNotUseExternalAccessRecordsWhenSyntheticTrafficIsExcluded+shouldAllowExternalAccessRecordsWhenDetailStatSourceIsExternal test`
  - `E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：9 个 Chromium 用例通过。
  - `E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token SHOWCASE_SCREENSHOT_DIR=docs/screenshots/showcase scripts/capture-showcase-screenshots.sh`：11 个 Chromium 截图用例通过。
  - `scripts/verify-eight-hour-artifacts.sh`：`reportsChecked=12`。
  - `scripts/quality-check.sh`：沙箱外完整质量门通过，包含全量 Maven、前端 build、静态预览 verifier、bundled Chromium 静态预览 browser-check、fresh MySQL schema smoke、Docker compose config。
  - `WUXING_BASE_URL=http://127.0.0.1:48082 ADMIN_TOKEN=dev-token scripts/external-shortlink-smoke-test.sh`：通过；`redirect=/result/R20260619035327376105854?sc=KUZT4X`，`sharedRedirect=/result/R20260619035327376105854?sc=KUZT4X&channel=share&campaign=result-card`，默认/显式本地/包含测试流量明细 `firstSource=local`。
- 继续视觉收口：
  - 移动端测试页 header 仅在小屏压缩 padding、标题字号和进度间距，让出生年份控件更早露出。
  - showcase 新增 `iphone-se-02b-test-birth-ready.png` 与 `android-wide-02b-test-birth-ready.png`，专门记录已选月份后“进入第 1 题”的可用态。
  - artifact gate 当前校验 31 张 showcase PNG 几何约束。

## 2026-06-19 上午续轮 P2 收口

- 评审反馈处理：
  - 结果页主卡移动端缩小五行文字标记和身份 eyebrow，`iphone-se-04-result.png` 复核后“你的五行人格身份 · 白露星官”不再把末尾单字挤到下一行。
  - 匹配页桌面标题从 42px 收到 40px，并保留 `text-wrap: balance` 与 headline 空白归一化，`desktop-09-match.png` 视觉更克制。
  - `ElementLegend` 移动端普通态改为 3+2 / 2+2+1 稳定布局，不再让第五项独占整行。
  - 后台短链列表“查看”链接增加 `.detail-link`，最小触控目标达到 44px；showcase 的 mobile admin touch-target 门已通过。
- 短链联调收紧：
  - `scripts/external-shortlink-smoke-test.sh` 现在额外访问 `/s/{shortCode}?channel=perf-test&campaign=external-shortlink-smoke`。
  - 本地统计来源下，脚本断言默认明细排除 perf-test，`includeSynthetic=true` 必须比默认明细多出测试访问。
  - 最新 live smoke 输出：默认明细 `total=2`，`includeSynthetic=true total=3`，`syntheticRedirect=/result/R20260619113547938558646?sc=jg2Aot&channel=perf-test&campaign=external-shortlink-smoke`。
- 质量门收紧：
  - `scripts/quality-check.sh` 增加质量门脚本 Git 跟踪状态检查；本地 dirty-tree 迭代只警告，`CI=true` 或 `REQUIRE_TRACKED_QUALITY_SCRIPTS=1` 时失败。
  - `node scripts/verify-frontend-contracts.mjs` 已覆盖上述 smoke、触控目标、图例布局和标题排版约束。
- 最新通过项：
  - `npm --prefix frontend run build`
  - `node scripts/verify-frontend-contracts.mjs`
  - `git diff --check`
  - `WUXING_BASE_URL=http://127.0.0.1:48082 ADMIN_TOKEN=dev-token scripts/external-shortlink-smoke-test.sh`
  - `E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token SHOWCASE_SCREENSHOT_DIR=docs/screenshots/showcase scripts/capture-showcase-screenshots.sh`：11 个 Chromium 截图用例通过。
  - `E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：9 个 Chromium 主流程用例通过。
  - `scripts/verify-eight-hour-artifacts.sh`：`reportsChecked=12`。
  - `scripts/quality-check.sh`：完整质量门通过，包含全量 Maven、前端 build、前端契约 verifier、静态预览 verifier、bundled Chromium 静态预览 browser-check、fresh MySQL schema smoke 和 Docker compose config；本地输出已明确警告 9 个质量门脚本仍未被 Git 跟踪。

## 2026-06-19 中午续轮交互洁癖收口

- 评审反馈处理：
  - 首页不再在 `onMounted` 自动读取剪贴板；匹配短码识别改为用户点击“检测剪贴板”或手动输入后触发，减少首次访问权限提示和信任感干扰。
  - 五行元素字保持纯文字，不恢复任何简笔画、路径、图片或 canvas；`ElementMark` 默认/compact/legend 字号下调一档，结果页主卡移动端标记也继续收小，避免元素字成为过强装饰焦点。
  - 契约脚本新增首页剪贴板必须手动触发、元素字视觉克制的静态约束。
- 证据口径：
  - 当前 showcase artifact 口径为 31 张 PNG；早前 25/29 张记录保留为历史阶段说明，不再作为当前数量。
  - 本轮继续保留“不覆盖、不删除 outputs/render-wuxing-question-previews.py”的边界。
- 最新通过项：
  - `npm --prefix frontend run build`
  - `node scripts/verify-frontend-contracts.mjs`
  - `git diff --check`
  - live in-app browser DOM：首页默认不出现匹配邀请，剪贴板状态文案为空且隐藏；短码输入、匹配按钮、检测剪贴板按钮均满足 44px 触控目标。
  - `E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：9 个 Chromium 主流程用例通过。
  - `E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token SHOWCASE_SCREENSHOT_DIR=docs/screenshots/showcase scripts/capture-showcase-screenshots.sh`：11 个 Chromium 截图用例通过，已刷新 showcase PNG。
  - `scripts/verify-eight-hour-artifacts.sh`：`reportsChecked=12`。
  - `scripts/quality-check.sh`：完整质量门通过，包含全量 Maven、前端 build、前端契约 verifier、静态预览 verifier、bundled Chromium 静态预览 browser-check、fresh MySQL schema smoke 和 Docker compose config；本地仍明确警告 9 个质量门脚本未被 Git 跟踪。
  - `WUXING_BASE_URL=http://127.0.0.1:48082 ADMIN_TOKEN=dev-token scripts/external-shortlink-smoke-test.sh`：通过；`shortCode=QbalP8`，裸短链跳 `/result/R20260619115514797508008?sc=QbalP8`，share/perf-test 归因均保留，默认明细 `total=2`，`includeSynthetic=true total=3`。

## 2026-06-19 中午续轮发布前补验

- 严格质量脚本跟踪门：
  - `REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh`：按预期失败，失败点为 9 个质量门脚本仍未被 Git 跟踪。
  - 失败清单：`scripts/verify-frontend-contracts.mjs`、`scripts/verify-wuxing-preview.mjs`、`scripts/verify-wuxing-preview-flow.mjs`、`scripts/verify-wuxing-browser.mjs`、`scripts/mysql-schema-smoke-test.sh`、`scripts/verify-eight-hour-artifacts.sh`、`scripts/frontend-live-gate.sh`、`scripts/performance-limit-test.sh`、`scripts/local-preview-smoke-test.sh`。
  - 结论：本地功能质量门可过，但 CI/release 前必须把上述脚本纳入 Git 版本控制；本轮未擅自 `git add`。
- 整合 live gate：
  - `FRONTEND_URL=http://127.0.0.1:5176 BACKEND_URL=http://127.0.0.1:48082 E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token SHOWCASE_SCREENSHOT_DIR=docs/screenshots/showcase scripts/frontend-live-gate.sh`：通过。
  - `local-preview-smoke-test.sh` 产出：`resultId=R20260619120518688988937`，`shortCode=XqMquj`，`matchId=XqMquj-nkZqAt`，`compatibilityScore=79`，`runtimeHealth=ok`，`readinessStatus=UP`，`frontendReadinessStatus=UP`。
  - 同一 gate 内 `mobile-e2e.sh` 9/9、`capture-showcase-screenshots.sh` 11/11、`verify-eight-hour-artifacts.sh` 通过。
- live 浏览器复核：
  - 当前首页 `http://127.0.0.1:5176/` DOM 复核：`hasHorizontalOverflow=false`，默认无匹配邀请，剪贴板状态文案为空且隐藏。
  - 首页首屏输入、匹配按钮、检测剪贴板按钮和主 CTA 均满足 44px 触控目标；元素文字标识为 28px 纯文字。
  - 应用内浏览器截图确认首页首屏层级正常，未见文字遮挡、按钮拥挤或横向溢出。
- CORS/跨域边界：
  - 当前代码和文档仍以同源 Nginx 反代为生产推荐。
  - 独立 API 域名模式已补可配置 CORS：`CORS_ALLOWED_ORIGINS` 默认空，不开放跨域；配置后允许当前前端所需的自定义请求头和 `OPTIONS` preflight。
  - `MvpFlowIntegrationTest` 已覆盖允许来源 preflight 通过、未配置来源 preflight 拒绝；后续如果真的拆成独立 API 域名，仍需在目标域名环境做一次真实浏览器验证。

## 2026-06-19 下午续轮活体视觉补验

- in-app browser 首页复核：
  - 当前用户 tab `http://127.0.0.1:5176/#preview` 保持在首页样例段，未强制刷新或改写用户页面状态。
  - 计算样式检查：`hasHorizontalOverflow=false`，可见按钮/链接/输入均无文本溢出；主 CTA、样例链接、短码输入、匹配按钮、检测剪贴板按钮均满足 44px 触控目标。
  - 首页 5 个 `.element-mark` 均为纯文字，`backgroundImage=none`，无 `svg/path/img/canvas` 子节点；视觉截图确认样例区为字章式五行标识，不再出现简笔画。
  - 浏览器 console `error/warning` 为空。
- live 结果页复核：
  - 复用 live gate 产出的 `resultId=R20260619120518688988937`、`shortCode=XqMquj` 做真实后端数据渲染复核。
  - 分享落地页 `/result/R20260619120518688988937?sc=XqMquj...`：`readyText=true`，`errorState=false`，`hasHorizontalOverflow=false`，元素标识无图形子节点/背景图，控件无文本溢出。
  - 直接结果页 `/result/R20260619120518688988937?...`：分享盒可见，包含 `save-share-image`、`native-share`、`copy-tools-toggle`；无横向溢出、无过小控件、无图形化元素标识。
  - in-app browser 对长结果页执行 `Page.captureScreenshot` 超时；本轮不把截图超时记为页面失败，继续以 DOM/计算样式和 Playwright 已刷新截图作为视觉证据。
- 本地截图复核：
  - 已查看 `docs/screenshots/showcase/iphone-se-04-result.png`、`android-wide-04-result.png`、`iphone-se-08-admin-overview.png`、`android-wide-11-admin-report-core.png`。
  - 结果页移动端层级、卡片间距、分享区域和完整五行分布协调；顶部五行字为轻量文本标记，不像图形装饰。
  - 后台移动端信息密度仍高，但默认概览、风险建议、复盘摘要和报表分组没有出现明显文本遮挡或横向溢出；继续可作为上线前“可用但还可精修”的密集管理视图。

## 2026-06-19 下午续轮后台移动短链收口

- 评审反馈处理：
  - 后台移动端 `#shortlink-section` 不再直接展示横向宽表；桌面保留原表格，移动端改为 `admin-shortlink-mobile-list` 卡片列表。
  - 每张短链卡展示短码、结果 ID、PV/UV/UIP 三个指标、命盘/来源/口径，并保留 44px 详情入口。
  - 移动端分页按钮改为稳定 2 列网格，避免四个全宽按钮纵向过长。
  - showcase E2E 新增断言：移动端展开短链证据后，`admin-shortlink-mobile-list` 必须可见，`#shortlink-section .shortlink-table-wrap` 必须隐藏。
- 视觉证据：
  - 已刷新并查看 `docs/screenshots/showcase/iphone-se-10-admin-report-expanded.png` 与 `android-wide-10-admin-report-expanded.png`，短链列表底部已从桌面宽表变为移动卡片。
  - 已查看 `desktop-06-admin-overview.png`，桌面后台仍保留连续宽表和密集报表阅读能力。
- 最新通过项：
  - `npm --prefix frontend run build`
  - `node scripts/verify-frontend-contracts.mjs`
  - `git diff --check`
  - `E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`：9 个 Chromium 主流程用例通过。
  - `E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token SHOWCASE_SCREENSHOT_DIR=docs/screenshots/showcase scripts/capture-showcase-screenshots.sh`：11 个 Chromium 截图用例通过，并执行了移动短链卡片新断言。
  - `scripts/verify-eight-hour-artifacts.sh`：`reportsChecked=12`。

## 2026-06-19 下午续轮硬风险处理

- CORS/跨域硬风险：
  - 后端新增 `app.cors.allowed-origins` 与 `app.cors.max-age-seconds` 配置，环境变量为 `CORS_ALLOWED_ORIGINS`、`CORS_MAX_AGE_SECONDS`。
  - 默认不配置时不开放跨域；配置白名单后，允许 `GET`、`POST`、`OPTIONS`，允许 `Content-Type`、`X-Client-Id`、`X-Session-Id`、`X-Channel`、`X-Campaign`、`X-Admin-Token`，暴露 `Content-Disposition`、`Location`。
  - `deploy/.env.example`、`deploy/.env.external.example`、`docs/api-spec.md`、`docs/production-operations-runbook.md` 已同步同源推荐和独立 API 域名配置方式。
  - 验证：`mvn -q -f backend/pom.xml -Dtest=MvpFlowIntegrationTest#shouldAllowConfiguredCorsPreflightForIndependentFrontendDomain+shouldRejectCorsPreflightFromUnconfiguredOrigin test` 通过。
- Git 跟踪硬风险：
  - 9 个质量门脚本仍未纳入 Git 跟踪；本轮仍未擅自 `git add`。
  - 需要用户明确允许后，才能把这些脚本和本轮新增证据文件纳入索引，彻底解除 CI fresh checkout 断链风险。

## 评审结论

- 视觉/交互评审：无 P0/P1；结果页重复分享操作已收敛为单一模块并折叠复制备用动作；匹配页已用关系参照替代装饰性图例；短链详情移动端分页已压缩，访问记录更早出现；外部平台缺失归因字段有明确提示；后台移动端默认收至约 3.4k 折叠视图，证据跳转展开态已用二级分组压到约 5.4k，短链列表在移动端已卡片化，桌面完整报表和宽表保持连续展示。
- 契约/联调评审：短链明细 `includeSynthetic` P1 已修；完整分享链接匹配识别 P2 已修；fresh MySQL 重复 DDL P1 已修，并且 Docker CLI schema smoke 已进入主质量门；`statSource` 外部统计扫描和失败语义已加保护；外部短链创建在 fallback 关闭时补为 502 语义；CI workflow 已增加真实 `browser-e2e` job，契约输出也显式列出后台移动报表和 CI 浏览器门。提交前仍需确认 `scripts/verify-*.mjs`、`scripts/mysql-schema-smoke-test.sh` 等未跟踪质量门脚本会随本轮改动纳入版本控制。

## 遗留观察

- 后台移动端已默认折叠长报表，并把详细报表拆为三个二级分组；短链访问详情已改为移动卡片，并覆盖外部访问匿名 hash 缺失场景；后续继续打磨重点转向更细的卡片字段取舍和空态/异常态视觉细节。
- Testcontainers 容器集成测试仍受当前宿主权限/Java Docker 客户端探测影响，后续如需把它纳入强制质量门，需要先修复宿主 Docker 探测链路。
- `outputs/render-wuxing-question-previews.py` 仍位于未跟踪 `outputs/` 目录，本轮没有覆盖、删除或移动该文件。
