# 前端视觉与联调 QA 记录

## 范围

- 首页、题卡、结果页、分享页的视觉收口。
- 五行元素标识改为纯文字，不再给 `金木水火土` 加简笔画或线描符号。
- 本地前端 `5175` 与后端 `48123` 的 live 联调。
- 静态预览与 `design-final` 展示图质量门。

## 关键调整

- `ElementMark.vue` 和分享图 canvas 的五行标识均保持纯文字。
- 首页 motto 改为纯文字 `简于形 · 明于心`，去掉菱形/竖线装饰。
- 首页移动端首屏留白轻微收紧，标题位置更利落，iPhone SE 首屏可自然露出样例卡入口。
- 题卡未选项隐藏右侧状态点，仅选中项显示确认勾。
- 出生月份从横向滑动轨道改为响应式网格，12 个月直接可见；可选日期也改为自适应小格，避免横向隐藏和按钮重叠。
- 常用年份快捷项从横向滚动改为自适应网格，减少移动端隐藏式横向交互。
- 答题主流程移除出生页到题卡的 `Transition` 包裹，避免浏览器过渡收尾不稳定时卡在“第 1 题但题卡不出现”。
- 极窄屏月份网格保持 3 列，同时降低月份按钮高度和提示字号，避免 2 列导致出生页过长。
- 结果页删除重复分享说明，只保留一个 `ShareLinkBox`，其中集中放置保存图、复制短码、复制链接和系统分享。
- 结果页分享区只保留 `保存分享图` 为主按钮，复制链接动作为次级按钮，避免“双主按钮”抢层级。
- 移动端分享区改为紧凑栅格：保存图全宽主按钮，`复制短码` / `复制链接` 并排，系统分享全宽次级按钮。
- 分享反馈状态补充 `aria-live="polite"`，复制、保存图、系统分享结果会作为状态变化被读屏识别。
- 结果页解释区从连续长段落改为三张解释卡：判定依据、元素强弱、互动总览，桌面三列、移动端单列，提升扫读节奏。
- 后台坏 token 状态改为成功前不写入本地 token，失效后清空输入框、旧数据和本地缓存，并用中文错误提示作为 `role="alert"` 状态。
- 后台 runtime 调试请求增加序号保护，坏 token reset 后旧的异步 runtime / visit-event runtime 响应不会回填旧状态。
- 后台短链访问详情页统一为“短链排查”布局：hero、口径 chip、筛选条、分页条和表格卡片层级更接近数据中台。
- 后台短链详情页补齐坏 token 中文错误态、缓存清理和 `role="alert"`，空状态/加载态也改为可访问状态。
- 匹配页错误态补 `role="alert"` / `aria-live="polite"`；沟通建议从重复卡片改为编号步骤列表，移动端阅读节奏更轻。
- 结果页错误态补 `role="alert"` / `aria-live="polite"`；404 页改为完整状态页，提供返回首页和重新测一张。
- `design-final/01-home.png`、`04-result.png`、`05-share.png` 由脚本重绘关键区域，去掉旧补丁块和线描残留。
- `outputs/render-wuxing-question-previews.py` 补充干净页脚覆盖层，重新生成 Q1/Q5 静态题卡预览，去掉底部旧模板边框残留。

## 已通过验证

- `scripts/quality-check.sh`
- `node scripts/verify-frontend-contracts.mjs`
- `node outputs/verify-wuxing-preview.mjs`
- `node outputs/verify-wuxing-preview-flow.mjs`
- `FRONTEND_URL=http://127.0.0.1:5175 BACKEND_URL=http://127.0.0.1:48123 scripts/frontend-live-gate.sh`
- `FRONTEND_URL=http://127.0.0.1:5175 BACKEND_URL=http://127.0.0.1:48123 ADMIN_TOKEN=dev-token scripts/local-preview-smoke-test.sh`
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`
- `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh`

最近一次前端主流程浏览器复核：

- `/test` 出生月份 `monthCount=12`，月份网格无横向溢出。
- 日期网格样例无按钮重叠，`不透露` 按钮跨两列后不再压住 `1日`。
- 选择月份后进入第 1 题，出生面板不再残留，题卡 5 个选项即时出现。
- 从第 1 题到第 5 题完整提交成功，进入 `/result/R...`。
- 结果页 `shareBoxes=1`、`quickActions=0`、`nextStepPanels=0`、`saveButtons=1`、`copyButtons=2`、`elementMarkSvg=0`。
- 分享按钮层级复核：`primaryCount=1`，`save-share-image` 为主按钮，`copy-share-link` 为 secondary。
- 分享按钮密度复核：390px 视口下 `docWidth=390`，按钮按 `1-2-1` 排布且无溢出；1280px 视口下分享盒 `760x252`，操作区为右侧纵列且无溢出。
- 分享按钮交互复核：在 in-app Browser 无法自动写入剪贴板时，复制短码会选中 `CDHmJY` 并提示手动复制；复制链接会选中完整 `/s/CDHmJY?channel=share&campaign=result-card` 链接并提示手动复制；系统分享取消后无错误文案。
- 新增静态契约：禁止 `TestPage` 主流程重新包 `Transition`，禁止出生月份/日期回退到横向 rail，锁定分享区主次按钮层级和移动端密度。
- 后台 `/admin` 登录复核：输入 `dev-token` 后从 `待登录` 进入 `已连接`，运营指标、风险建议、趋势图、运行态和短链列表均渲染；桌面保留高密度数据台，移动窄屏折叠为可阅读布局。
- 后台短链明细页复核：`/admin/short-links/CDHmJY?includeSynthetic=true&keyword=CDHmJY&statSource=local` 在 390px 和 1280px 视口下均无页面级横向溢出，过滤器、分页器和表格容器正常渲染。
- 年份快捷项浏览器复核：6 个 `birth-year-quick-*` 全部在可视宽度内，`.quick-row` 为 grid 且无横向溢出。
- 390px 出生信息页复核：页面无横向溢出，12 个月 3 列网格稳定，底部操作区为文档流内静态区域，不遮挡表单；展开“补充日期和时段”后 32 个日期按钮无重叠、无横向溢出。
- 390px 结果页密度复核：`docWidth=390`、`elementSvg=0`，标题字号保持 22/24px，连续卡片、五行分布和分享区均无横向溢出。
- 双人匹配 UI 复核：以短码 `CDHmJY` 进入匹配测试流，5 题完整提交后进入 `/match/CDHmJY/vT10gu`；匹配页无错误文案、无横向溢出、无 element svg，标题和两张人格卡在 681px 视口下自然下排。
- 双人匹配响应式复核：`/match/BoRSue/99nkkK` 在 390px 与 1280px 下均无横向溢出，移动端标题 30px、人物卡单列、五行参照与 6 条建议卡正常渲染，`elementSvg=0`。
- 匹配页人物比例条细修：390px 下双方卡片内比例保持“左百分比 - 中间色条 - 右百分比”的紧凑横排，色条宽约 215px，无溢出。
- 匹配页标题细修：390px 下匹配标题从 30px 收到 28px，保留无溢出并降低首屏压迫感。
- 短链回流复核：打开 `/s/CDHmJY?channel=perf-test&campaign=browser-shortlink-audit` 后跳转到 `/result/R20260616005522561050773?sc=CDHmJY&channel=perf-test&campaign=browser-shortlink-audit`，共享结果页正常渲染且无 element svg。
- 保存分享图按钮复核：in-app Browser 中点击 `save-share-image` 后出现 `分享图已生成`，无 `分享图生成失败` / canvas 不支持文案，分享区仍正常。
- mobile E2E 复跑：Chromium 4 个用例通过，覆盖移动端完整测试、保存图 PNG 下载文件名、文件头、文件大小、`900x1200` 宽高、复制短码/复制链接 fallback、共享结果回流、后台 CSV 导出、双人匹配链路、系统分享成功 payload 和后台坏 token 失败态。
- 系统分享成功分支复核：新增 Chromium 用例 mock `navigator.share`，验证分享 payload 的标题、文案、`/s/{shortCode}` 短链以及 `channel=share&campaign=result-card` 归因，且不会显示 fallback tip。
- 后台坏 token 分支复核：新增 Chromium 用例覆盖本地残留 `stale-token` 和手动输入 `wrong-token` 两条路径，确认提示 `管理 token 无效，请重新输入。`、`增长漏斗` 不显示、CSV 导出禁用、token 输入框清空且 `localStorage` 不残留旧 token。
- 后台短链详情坏 token 复核：新增 Chromium 用例直达 `/admin/short-links/sample`，确认提示 `管理 token 无效，请返回后台重新登录。`、错误节点 `role="alert"` / `aria-live="polite"` 且 `localStorage` 不残留旧 token。
- 首页 390px 复核：大标题、三枚入口按钮、手动短码输入和五列样例卡均无横向溢出，样例元素标识 `elementSvg=0`。
- 共享结果态复核：`/result/R20260616012822381142353?sc=BoRSue&channel=perf-test&campaign=result-card` 无横向溢出，`shareBoxes=0`，保留 1 个“我也测一张”入口。
- 404 异常页复核：`/no/such/page` 在 390px 下 panel 与返回首页按钮无横向溢出。
- 静态题卡预览目检：`visual-check-question-q1.png` 和 `visual-check-question-q5-selected.png` 底部旧边框残留已清理，页脚水形更干净；`node outputs/verify-wuxing-preview.mjs` 通过且 `visualPreviews=2`。
- 结果页解释区复核：390px 下 `docWidth=390`、`elementSvg=0`、解释区三张卡无横向溢出；桌面 `desktop-07-result.png` 目检三列解释卡和分享区排版自然。
- 桌面 artifact 复核：`desktop-07-result.png`、`desktop-08-shortlink-detail.png` 与 `desktop-09-match.png` 纳入 `verify-eight-hour-artifacts.sh`，并校验 PNG 文件头、宽度 `1280`、高度不少于 `900`。
- showcase 截图复跑：Chromium 6 个截图用例通过，重新生成 `iphone-se`、`android-wide` 主流程图和匹配页图、`desktop-07-result.png`、`desktop-06-admin-overview.png`、`desktop-08-shortlink-detail.png`、`desktop-09-match.png`；目检首页、结果页、共享结果页、匹配页、桌面结果页、后台总览和短链明细页排版正常。
- 匹配页截图复核：`iphone-se-06-match.png`、`android-wide-06-match.png`、`desktop-09-match.png` 均用真实短码进入 `/match/{partner}/{current}`，移动端标题、人物卡、比例条、五行参照和优势/建议步骤无横向溢出；截图前额外断言无 `.error-state`。
- 移动匹配 artifact 复核：`iphone-se-06-match.png` 校验 PNG 宽度 `375`、高度不少于 `667`；`android-wide-06-match.png` 校验宽度 `430`、高度不少于 `932`。
- TestPage 状态语义复核：题目加载态补 `role="status"` / `aria-live="polite"`，错误态补 `role="alert"` / `aria-live="polite"`，提交中提示补 `role="status"` / `aria-live="polite"`，并纳入 `test-page-state-a11y-contract`。
- TestPage 按钮文案复核：题卡页底部返回按钮从固定 `上一张` 改为第 1 题 `基础信息`、后续题 `上一题`，`iphone-se-03-test-question-card.png` 复跑后按钮宽度和层级正常。
- TestPage 题目加载失败复核：新增 Chromium 用例 mock `/api/questions` 返回 500，确认 `题目加载失败，请刷新重试` 保持可见，主按钮禁用且文案为 `题目加载失败`。
- 首页手动短码复核：新增 Chromium 用例覆盖 6-7 位短码格式校验，确认输入 `abc` 时 `aria-invalid="true"`，提示区域常驻并带 `role="status"` / `aria-live="polite"`，文案 `请输入 6 到 7 位短码` 可见；匹配邀请区补 `aria-live="polite"`。
- 首页手动短码旧状态复核：有效短码出现匹配邀请后，再输入无效短码会清空旧邀请；点击 `暂时不用` 后同页重新提交有效短码可以重新出现邀请，避免旧 `matchCode` 被误用；通过受控 promise 延迟有效短码请求，立即改成无效短码后释放旧响应，并等待候选接口 response，旧响应不会回填旧邀请。
- 首页 invalid 态目检：`outputs/visual-check-home-invalid-short-code.png` 显示错误提示不挤压主 CTA，短码输入框红棕边框反馈克制，手动短码区与样例卡之间无重叠。
- 状态提示一致性复核：`ShareLinkBox` 提示、`ResultPage` 加载态/分享图状态、`MatchPage` 加载态均补 `role="status"` / `aria-live="polite"`，并纳入前端契约。
- 后台状态提示复核：`AdminDashboard` 的请求处理中、外部短链运行态检查、外部短链警告、访问事件运行态警告统一补 `role="status"` / `aria-live="polite"`，并纳入前端契约。
- 404 状态页复核：`iphone-se-07-not-found.png`、`android-wide-07-not-found.png`、`desktop-10-not-found.png` 均断言 `页面不存在` 和 `重新测一张`，并检查无横向溢出；404 页保留纯文字品牌行 `五行人格卡`，没有引入图标或简笔画。
- 移动 404 artifact 复核：`iphone-se-07-not-found.png` 校验 PNG 宽度 `375`、高度不少于 `667`；`android-wide-07-not-found.png` 校验宽度 `430`、高度不少于 `932`。
- 匹配页图例复核：桌面匹配页保留外层 `五行参照`，内部 compact 图例隐藏重复的 `五行图例` 标题，`desktop-09-match.png` 目检无新增拥挤或错位。
- 2026-06-16 03:24 质量门复跑通过：`scripts/quality-check.sh` 覆盖 `git diff --check`、后端 Maven 测试、前端 build、前端合同、静态预览、Docker compose 配置和 artifact 几何检查。
- 2026-06-16 03:16 移动端 E2E 复跑通过：7 个 Chromium 用例覆盖移动主流程、匹配创建、首页手动短码格式校验、旧邀请清理和慢响应防回填、题目加载失败保护、系统分享 payload、后台坏 token 清理和短链详情坏 token 可访问错误态。
- 2026-06-16 03:38 截图复跑通过：`scripts/capture-showcase-screenshots.sh` 7 个 Chromium 用例通过，刷新 `iphone-se` / `android-wide` 主流程、桌面结果、后台总览、短链详情、桌面匹配和 404 截图。
- 2026-06-16 03:38 移动端 E2E 复跑通过：`scripts/mobile-e2e.sh` 7 个 Chromium 用例通过；首页手动短码竞态用例新增校验恢复邀请包含当前 `partner.shortCode`，并继续覆盖题目失败、系统分享、后台坏 token 和短链详情坏 token。
- 2026-06-16 03:38 质量门复跑通过：`scripts/quality-check.sh` 再次覆盖 `git diff --check`、后端 Maven 测试、前端 build、前端合同、静态预览、Docker compose 配置和 artifact 检查。
- 2026-06-16 03:38 浏览器 DOM 复核：in-app Browser 打开 390px 首页，`bodyScrollWidth=390`、无横向溢出，首页标题/主 CTA/手动短码入口存在，元素标识仅渲染纯文字 `金`、`木`、`水`、`火`、`土`；该环境截图接口超时、输入操作受虚拟剪贴板限制，交互证明仍以 Chromium E2E 为准。
- 2026-06-16 03:49 首页入口密度复核：`GuidePage.vue` 将剪贴板检测从主 actions 降级到手动短码输入区下方，文案收短为 `检测剪贴板`；`iphone-se-01-home.png` 和 `android-wide-01-home.png` 目检首屏更松、无横向溢出，移动端 E2E 7 个用例通过。
- 2026-06-16 03:50 静态预览浏览器验证复核：修复 `wuxing-frontend-flow-preview.html` 在 file:// 下 `navigator.clipboard.writeText` reject 后不 fallback 的问题，`copyText()` 现在回退到 `fallbackCopyText()`；`verify-wuxing-browser.mjs` 纳入 `quality-check.sh`，完整打开静态预览并输出 7 张 `browser-check-*` 截图。
- 2026-06-16 03:50 静态题卡排版复核：修复浏览器版第 5 题题号与题干压字，题干、提示和选项列表整体下移；`verify-wuxing-browser.mjs` 新增 q1/q5 rect 断言，保证题号、题干、提示和选项不重叠；`browser-check-03b-question-q5-selected.png` 目检正常。
- 2026-06-16 03:55 静态预览复制降级复核：`copyText()` 返回真实复制结果，`document.execCommand('copy')` 为 false 时提示 `请手动复制链接`，不再把失败误报为 `分享链接已复制`；`verify-wuxing-preview.mjs` 锁定该文案分支。
- 2026-06-16 03:55 本地预览 runbook 更新：`docs/local-preview-runbook.md` 明确 `quality-check.sh` 会启动 Chromium 验证 `file://` 静态预览，但仍不会启动前后端端口；真实 `result -> /s -> match -> admin CSV` 联调继续以 live smoke 为准。
- 2026-06-16 04:01 静态预览一致性复核：浏览器版题卡第 1 题返回按钮同步为 `基础信息`，未选项移除 `›` 模板箭头，题干和选项字号略收；`verify-wuxing-preview.mjs` 锁定无 `option-arrow` / 无 `›`，`verify-wuxing-preview-flow.mjs` 锁定第 1 题返回文案。
- 2026-06-16 04:01 质量门复跑通过：`scripts/quality-check.sh` 包含新增的 `outputs/verify-wuxing-browser.mjs`，并覆盖 q1/q5 题卡 rect 间距、静态预览复制 fallback、前端 build、前端合同和后端 Maven 测试。
- 2026-06-16 04:04 移动首页布局合同复核：`showcase-screenshots.spec.mjs` 新增 `expectNoHorizontalOverflow`、`expectNoElementMarkGraphics` 和 `expectVerticalOrder`，锁定首页标题、指标、主按钮、短码输入、剪贴板检测、声明和样例卡按顺序排列且不重叠；`verify-frontend-contracts.mjs` 输出 `mobile-home-showcase-layout-contract`。
- 2026-06-16 04:07 静态题卡文案一致性复核：`wuxing-frontend-flow-preview.html` 初始 DOM、运行时渲染、`verify-wuxing-preview-flow.mjs`、`verify-wuxing-browser.mjs` 与 `render-wuxing-question-previews.py` 全部同步第 1 题返回按钮为 `基础信息`；重新生成 `visual-check-question-q1.png` 和 `visual-check-question-q5-selected.png`。
- 2026-06-16 04:09 质量门复跑通过：完整 `scripts/quality-check.sh` 通过，覆盖移动首页布局合同、静态题卡文案一致性、静态预览浏览器截图、前端构建、后端 Maven 测试和 Docker compose 配置。
- 2026-06-16 04:15 静态题卡箭头一致性复核：`render-wuxing-question-previews.py` 删除未选项 `›` 绘制，重新生成 `visual-check-question-q1.png` 和 `visual-check-question-q5-selected.png`；`verify-wuxing-preview.mjs` 增加 renderer 源码断言，HTML 与 PNG renderer 均禁止模板箭头。
- 2026-06-16 04:16 移动主链路视觉合同复核：`mobile-main-flow.spec.mjs` 在结果页、共享结果页、匹配页和 native share 入口新增无横向溢出、无 `.element-mark svg/path` 断言，并在匹配页显式拒绝 `.error-state`；`verify-frontend-contracts.mjs` 输出 `mobile-main-flow-visual-contract`。
- 2026-06-16 04:16 移动端 E2E 复跑通过：`scripts/mobile-e2e.sh` 7 个 Chromium 用例通过；首次沙箱启动 Chromium 因 macOS MachPort 权限失败，按规则非沙箱重跑通过，业务断言和新增视觉断言均通过。
- 2026-06-16 04:17 截图复跑通过：`scripts/capture-showcase-screenshots.sh` 7 个 Chromium 用例通过，刷新 `iphone-se` / `android-wide` 主流程、移动匹配、移动 404、桌面结果、后台总览、短链详情、桌面匹配和桌面 404 截图；抽样目检首页、结果页、匹配页、404 和后台总览无阻塞排版问题。
- 2026-06-16 04:17 质量门复跑通过：完整 `scripts/quality-check.sh` 通过，覆盖 `git diff --check`、后端 Maven 测试、前端 build、前端合同、静态预览、静态预览浏览器截图和 Docker compose 配置。
- 2026-06-16 04:22 live smoke 复跑通过：前端 dev server 以 `BACKEND_PROXY_TARGET=http://127.0.0.1:48123` 指向预览后端；`scripts/local-preview-smoke-test.sh` 使用 `FRONTEND_URL=http://127.0.0.1:5175`、`BACKEND_URL=http://127.0.0.1:48123` 跑通结果创建、短链跳转、共享归因、匹配创建/回读、后台 runtime 和 CSV 导出。
- 2026-06-16 04:22 in-app Browser DOM 复核：应用内浏览器打开 live 首页 `http://127.0.0.1:5175/`，标题为 `五行人格卡`，主按钮 `开始测试` 和手动短码输入存在，`scrollWidth=390`、无横向溢出、`.element-mark svg/path` 数量为 0；该环境截图接口本轮仍超时，视觉证明以 showcase / browser-check PNG 为准。
- 2026-06-16 04:27 agent P2 收口复核：`mobile-main-flow.spec.mjs` 的 `createResultByApi()` 支持 per-test campaign，手动短码、native share 和匹配用例的 API seed campaign 分离；`expectNoElementMarkGraphics()` 扩展到 svg/path/img/canvas 和 `background-image`；前端 contract 增加至少 4 个主链路页面覆盖次数检查；`verify-wuxing-preview-flow.mjs` 夹具初始返回文案同步为 `基础信息`。
- 2026-06-16 04:27 移动端 E2E 复跑通过：`scripts/mobile-e2e.sh` 7 个 Chromium 用例通过，覆盖 per-test campaign、结果/共享/匹配/native share 页面无横向溢出、无元素图形回退、匹配页无 `.error-state`。
- 2026-06-16 04:27 质量门复跑通过：完整 `scripts/quality-check.sh` 通过，覆盖最新 E2E visual contract、静态 flow 夹具、前端 build、后端 Maven 测试、静态预览浏览器验证和 Docker compose 配置。
- 2026-06-16 04:27 live smoke 复跑通过：在 `FRONTEND_URL=http://127.0.0.1:5175`、`BACKEND_URL=http://127.0.0.1:48123` 下再次跑通结果创建、短链跳转、共享归因、匹配创建/回读、后台 runtime 和 CSV 导出。
- 2026-06-16 04:30 控件文字适配复核：in-app Browser DOM 审计 live 首页、结果页、匹配页、404 页，均无横向溢出、无 `.element-mark` 图形回退，按钮/链接/输入/短码/链接文本无 `scrollWidth/clientWidth` 或 `scrollHeight/clientHeight` 溢出。
- 2026-06-16 04:31 showcase 控件文字合同复核：`showcase-screenshots.spec.mjs` 新增 `expectNoControlTextOverflow()`，覆盖移动首页、出生页、题卡、结果页、共享结果、匹配页、404，以及桌面结果、后台总览、短链详情、匹配和 404；`scripts/capture-showcase-screenshots.sh` 7 个 Chromium 用例通过，`verify-frontend-contracts.mjs` 输出 `showcase-control-text-fit-contract`。
- 2026-06-16 04:35 最小字号复核：前端源内 `font-size: 10px` 已清零；移动端出生月份副标签从 10px 提到 11px，后台筛选小提示和旅程 code 从 10px 提到 11px。`iphone-se-02-test-birth-card.png`、`android-wide-02-test-birth-card.png` 与 `desktop-06-admin-overview.png` 目检无挤压，`scripts/capture-showcase-screenshots.sh`、`scripts/mobile-e2e.sh` 和完整 `scripts/quality-check.sh` 均通过。
- 2026-06-16 04:39 guard 口径统一复核：`mobile-main-flow.spec.mjs` 与 `showcase-screenshots.spec.mjs` 的 element-mark 检查均覆盖 svg/path/img/canvas 和 `.element-mark, .element-mark *` 的 `background-image`；showcase 控件文字检查扩展到 `select` / `textarea`；前端 contract 改为统计 `await` 调用次数，避免把 helper 定义误计入覆盖面。`scripts/mobile-e2e.sh`、`scripts/capture-showcase-screenshots.sh` 与完整 `scripts/quality-check.sh` 均通过。
- 2026-06-16 04:43 API/DTO 字段契约复核：`verify-frontend-contracts.mjs` 新增 `api-request-dto-contract`，锁定前端 `CreateResultRequest` / `CreateMatchRequest` 与后端 `CreateResultRequest` / `CreateMatchRequest` / `AnswerRequest` 同时保留 `birthYear`、`birthMonth`、`birthDay`、`birthTimeRange`、`answers`、`partnerShortCode`、`questionCode`、`optionCode`；移动端匹配 E2E 直接断言 `/api/matches` POST body 使用 `partnerShortCode`、`birthYear`、`birthMonth` 和 5 组 `questionCode/optionCode`，不是展示序号。`scripts/mobile-e2e.sh` 和完整 `scripts/quality-check.sh` 均通过。
- 2026-06-16 04:44 artifact 几何复核：`scripts/verify-eight-hour-artifacts.sh` 通过，校验截图/报告等 12 项交付物，包含 `docs/artifacts/presentations/contact-sheet.png` 与 `outputs/.../wuxing-showcase/preview/contact-sheet.png` 等联系表和展示产物。
- 2026-06-16 04:46 API/DTO 注解契约复核：`verify-frontend-contracts.mjs` 新增 `assertDecoratedField()`，锁定后端 `birthYear @NotNull/@Min(1900)`、`birthMonth @NotNull/@Min(1)/@Max(12)`、`birthDay @Min(1)/@Max(31)`、`answers @Valid/@NotNull/@Size(min = 5, max = 5)`、`partnerShortCode @NotBlank`、`questionCode/optionCode @NotBlank`；匹配 E2E 请求监听改为精确 `new URL(request.url()).pathname === '/api/matches'`，并断言 `birthDay: null`、`birthTimeRange: null`。
- 2026-06-16 04:51 移动端 E2E 复跑通过：`scripts/mobile-e2e.sh` 7 个 Chromium 用例通过，覆盖移动主流程、短码匹配、请求体字段、无横向溢出、无元素图形回退、系统分享 payload、后台坏 token 清理和短链详情坏 token。
- 2026-06-16 04:52 showcase 截图复跑通过：`scripts/capture-showcase-screenshots.sh` 7 个 Chromium 用例通过，刷新 `iphone-se` / `android-wide` 主流程、桌面结果、后台总览、短链详情、桌面匹配和 404 截图；控件文字溢出和 element-mark 纯文字检查均在截图前执行。
- 2026-06-16 04:52 质量门复跑通过：完整 `scripts/quality-check.sh` 通过，覆盖 `git diff --check`、后端 Maven 测试、前端 build、前端合同、静态预览、静态预览浏览器截图、Docker compose 配置和最新 API/DTO 注解契约。
- 2026-06-16 04:53 live smoke 复跑通过：`FRONTEND_URL=http://127.0.0.1:5175`、`BACKEND_URL=http://127.0.0.1:48123` 下创建 `resultId=R20260616045311196124074`、`shortCode=7ayMr3`，短链 `/s/7ayMr3` 跳转正常，双人匹配 `matchId=7ayMr3-L3AOuJ`、`compatibilityScore=79`，后台 runtime health 和 CSV 导出正常，前后端 readiness 均为 `UP`。
- 2026-06-16 04:54 live DOM 复核：用 Chromium 打开 live 首页、结果页、匹配页、404 页，四页均 `scrollWidth=390`、无横向溢出、按钮/链接/输入/短码/URL 文本无 `scrollWidth/clientWidth` 或 `scrollHeight/clientHeight` 溢出，`.element-mark svg/path/img/canvas` 数量为 0，`.element-mark, .element-mark *` 背景图数量为 0，body 字号为 `16px`。
- 2026-06-16 04:55 静态设计图复核：目检 `design-final/04-result.png`、`design-final/05-share.png`、`outputs/browser-check-04-result.png`、`outputs/browser-check-05-share.png`，五行字样仅保留纯文字，未再出现旧版线稿/简笔画；分享页二维码、按钮和短码区域无挤压。
- 2026-06-16 05:01 后台移动布局复核：发现 `AdminDashboard` 窄屏仍保留 1024px 横向桌面画布，登录/自动加载后容易停在右侧切片；改为 `max-width: 760px` 下响应式折叠布局，筛选区、指标卡、证据导航、行动建议、雷达卡、旅程卡、图表区和 runtime 区均降为单列或窄双列，表格仅在 `.table-wrap` 内横向滚动。
- 2026-06-16 05:02 后台移动 showcase 复跑通过：`showcase-screenshots.spec.mjs` 新增 `iphone-se-08-admin-overview.png` 和 `android-wide-08-admin-overview.png`，断言移动后台 `.admin-desktop-page` `scrollLeft=0`、无页面级横向滚动、`.admin-page` 左边缘在视口内、控件文字不溢出；`scripts/capture-showcase-screenshots.sh` 9 个 Chromium 用例通过。
- 2026-06-16 05:10 移动端 E2E 复跑通过：`scripts/mobile-e2e.sh` 7 个 Chromium 用例通过，确认移动主流程、匹配请求体、分享图下载、系统分享、后台坏 token 和短链详情坏 token 不受后台响应式修改影响。
- 2026-06-16 05:10 质量门复跑通过：完整 `scripts/quality-check.sh` 通过，覆盖 `git diff --check`、后端 Maven 测试、前端 build、前端合同、静态预览、静态预览浏览器截图、Docker compose 配置和最新后台移动响应式合同。
- 2026-06-16 05:12 live smoke 复跑通过：`FRONTEND_URL=http://127.0.0.1:5175`、`BACKEND_URL=http://127.0.0.1:48123` 下创建 `resultId=R20260616051202368774923`、`shortCode=nO5JmX`，短链 `/s/nO5JmX` 跳转正常，双人匹配 `matchId=nO5JmX-Ntl4D6`、`compatibilityScore=79`，后台 runtime health 和 CSV 导出正常，前后端 readiness 均为 `UP`。
- 2026-06-16 05:16 交付口径复核：`scripts/verify-eight-hour-artifacts.sh` 纳入 `iphone-se-08-admin-overview.png` 与 `android-wide-08-admin-overview.png` required file 和 PNG 几何校验；`docs/project-promotion-kit.md` 更新为 21 张 showcase 截图；`docs/eight-hour-performance-showcase-delivery.md` 更新后台移动口径为“移动窄屏已折叠为可阅读布局，无页面级横向桌面画布”。
- 2026-06-16 05:16 截图等待逻辑复核：移动后台 showcase 不再用固定 `waitForTimeout(250)`，改为等待 `.focus-grid` 和 `#shortlink-section` 可见后再截图；`scripts/verify-eight-hour-artifacts.sh`、`node scripts/verify-frontend-contracts.mjs`、`scripts/capture-showcase-screenshots.sh` 均通过。
- 2026-06-16 05:18 live DOM 复核：前台首页、结果页、匹配页、404 页均 `scrollWidth=390`、无横向溢出、无控件文字溢出、`.element-mark` 图形和背景图数量为 0；后台移动总览 `scrollWidth=390`、`.admin-desktop-page scrollLeft=0`、`scrollWidth=clientWidth=390`、`.admin-page` 左边缘为 14px，控件文字无溢出。
- 2026-06-16 05:21 最终护栏加固复核：`verify-frontend-contracts.mjs` 增加移动后台截图 required file 和几何断言的双保险；桌面短链详情 showcase 删除固定 `waitForTimeout(300)`，改由详情页内容可见性驱动。`node scripts/verify-frontend-contracts.mjs`、`scripts/verify-eight-hour-artifacts.sh`、`scripts/capture-showcase-screenshots.sh` 9 个 Chromium 用例均通过。
- 2026-06-16 05:23 截图包盘点：`docs/screenshots/showcase` 共 21 张 PNG，`iphone-se-*` 宽度均为 375，`android-wide-*` 宽度均为 430，`desktop-*` 宽度均为 1280；移动后台长图约 15.5k-15.8k px 高，属于后台信息折叠后的线性展开，不是页面级横向溢出。
- 2026-06-16 05:29 完整质量门复跑通过：`scripts/quality-check.sh` 通过，覆盖 `git diff --check`、后端 Maven 测试、前端 build、`verify-frontend-contracts.mjs`、静态预览、`outputs/verify-wuxing-browser.mjs` 浏览器静态预览和 Docker compose 配置。
- 2026-06-16 05:29 live smoke 复跑通过：`FRONTEND_URL=http://127.0.0.1:5175`、`BACKEND_URL=http://127.0.0.1:48123` 下创建 `resultId=R20260616052921422222696`、`shortCode=ahzTk2`，短链 `/s/ahzTk2` 跳转正常，双人匹配 `matchId=ahzTk2-X5JRPe`、`compatibilityScore=79`，后台 runtime health、CSV 导出、后端 `/api/readiness` 与前端同源 `/api/readiness` 均正常。
- 2026-06-16 05:32 浏览器 live DOM 复核：in-app Browser 打开结果页 `390px` 视口，`scrollWidth=390`、无横向溢出，`.element-mark` 共 7 个，`svg/path/img/canvas` 图形计数为 0，背景图计数为 0；匹配页 `.element-mark` 共 9 个，图形计数和背景图计数同样为 0。
- 2026-06-16 05:34 后台移动字体微调：`AdminDashboard.vue` 在 `max-width: 760px` 下将筛选区 `input/select` 提升到 `14px`、保留 `44px` 触控高度，`toggle-row` 提升到 `13px`；浏览器复核 `documentScrollWidth=390`、`bodyScrollWidth=390`、筛选控件无溢出。
- 2026-06-16 05:36 后台字体微调后复跑通过：`node scripts/verify-frontend-contracts.mjs`、`npm --prefix frontend run build`、`scripts/capture-showcase-screenshots.sh` 9 个 Chromium 用例、`scripts/mobile-e2e.sh` 7 个 Chromium 用例、`scripts/verify-eight-hour-artifacts.sh` 均通过。
- 2026-06-16 05:37 双 agent 复审结论：视觉评审和 QA/联调评审均未发现 P0/P1，也没有必须马上修的 P2；确认 `ElementMark.vue` 为纯文字 `span`，`shareCard.ts` 的五行字使用 `fillText(name)`，结果页、匹配页、分享图不再出现简笔画。
- 2026-06-16 05:41 前端契约补强：`verify-frontend-contracts.mjs` 增加 `admin-mobile-filter-typography-contract`，锁定移动后台筛选输入/选择框 `14px`、`font-weight: 850` 和测试流量切换行 `13px`，避免后续回退成过小的 12px 主控件。
- 2026-06-16 05:42 生产 dist 离线捕获：`outputs/capture-current-frontend-offline.mjs` 打开构建后的 home/test/result/shared-result/match 五页，全部 `horizontalOverflow=0`、`markGraphicCount=0`、`markBackgroundCount=0`；普通结果页 `shareBoxCount=1`，共享结果页 `shareBoxCount=0`。
- 2026-06-16 05:43 一键 live gate：新增 `scripts/frontend-live-gate.sh`，串联 `local-preview-smoke-test.sh`、`mobile-e2e.sh`、`capture-showcase-screenshots.sh` 和 `verify-eight-hour-artifacts.sh`；以 `FRONTEND_URL=http://127.0.0.1:5175`、`BACKEND_URL=http://127.0.0.1:48123` 运行通过，生成 `resultId=R20260616054301856508826`、`shortCode=OJt0Lh`、`matchId=OJt0Lh-uUJyLS`，移动 E2E 7/7、showcase 9/9、交付物校验通过。
- 2026-06-16 05:55 稳定性巡检：连续 10 分钟、20 次检查均返回前端首页 HTTP 200，后端 `/api/readiness` 为 `UP`，`user_result`、`short_link`、`visit_event`、`site_daily_metric`、`short_link_daily_metric` 均为 `ok`。
- 2026-06-16 05:56 稳定性巡检后 live gate 复跑通过：`FRONTEND_URL=http://127.0.0.1:5175`、`BACKEND_URL=http://127.0.0.1:48123` 下生成 `resultId=R20260616055621612816592`、`shortCode=sgvRur`、`matchId=sgvRur-1MZfuA`，移动 E2E 7/7、showcase 9/9、交付物校验通过。
- 2026-06-16 06:07 第二轮稳定性巡检：再次连续 10 分钟、20 次检查均返回前端首页 HTTP 200，后端 `/api/readiness` 为 `UP`，五张核心表均为 `ok`。
- 2026-06-16 06:08 第二轮稳定巡检后 live gate 复跑通过：`FRONTEND_URL=http://127.0.0.1:5175`、`BACKEND_URL=http://127.0.0.1:48123` 下生成 `resultId=R20260616060753419795423`、`shortCode=3eVQKQ`、`matchId=3eVQKQ-wOnAOf`，移动 E2E 7/7、showcase 9/9、交付物校验通过。
- 2026-06-16 06:17 最终视觉 agent 复审：未发现 P0/P1/P2 必修问题；确认当前交付口径中的 ElementMark 与分享图字形均为纯文字。非交付目录 `outputs/live-result-no-figure.png`、`outputs/selfcheck-final-showcase/iphone-se-04-result.png` 是旧临时截图，未被 `docs/screenshots/showcase`、`docs/project-promotion-kit.md` 或当前演示幻灯引用；若打包整个 `outputs/`，应排除这些旧临时图。
- 2026-06-16 06:19 第三轮稳定性巡检：再次连续 10 分钟、20 次检查均返回前端首页 HTTP 200，后端 `/api/readiness` 为 `UP`，五张核心表均为 `ok`。
- 2026-06-16 06:26 QA agent 追补 P2 已修复：`scripts/verify-eight-hour-artifacts.sh` 从零散截图校验升级为 21 张 showcase 截图统一几何矩阵；`ExternalShortLinkProvider` 创建外部短链时把 `originUrl/originalPath` 固定为 `/result/{resultId}?channel=share&campaign=result-card`；`ResultPage` 把 `channel=share` 识别为分享落地态；`scripts/deploy-preflight.sh` 开始拒绝 `dev-token/dev-salt/local-token/local-salt` 并要求 `ADMIN_TOKEN >= 24`、`HASH_SALT >= 32`。
- 2026-06-16 06:26 P2 契约复跑通过：`node scripts/verify-frontend-contracts.mjs` 新增 `all-showcase-png-geometry-contract` 和 `external-share-landing-contract` 并通过；`mvn -q -f backend/pom.xml -Dtest=ExternalShortLinkProviderTest test` 通过；`bash -n scripts/external-shortlink-smoke-test.sh scripts/deploy-preflight.sh scripts/verify-eight-hour-artifacts.sh` 分别通过；临时 `.env` 实测强密钥通过、`dev-token`、短 token 和 `dev-salt` 均被拒。
- 2026-06-16 06:27 交付物门禁复跑通过：`scripts/verify-eight-hour-artifacts.sh` 校验 21 张截图、PPT 联系表和 12 组性能报告均通过；iPhone 截图宽度 375、Android 截图宽度 430、桌面截图宽度 1280 的几何约束继续有效。
- 2026-06-16 06:28 完整质量门复跑通过：`scripts/quality-check.sh` 通过，覆盖 `git diff --check`、后端 Maven 全量测试、前端 build、`verify-frontend-contracts.mjs`、静态预览、`outputs/verify-wuxing-browser.mjs` 浏览器静态预览、Docker compose 配置和新增的 external share landing 契约。
- 2026-06-16 06:28 live gate 复跑通过：`FRONTEND_URL=http://127.0.0.1:5175`、`BACKEND_URL=http://127.0.0.1:48123` 下生成 `resultId=R20260616062800713626733`、`shortCode=pH4RpK`、`matchId=pH4RpK-yj9PVv`，本地 smoke、移动 E2E 7/7、showcase 9/9 和 21 张截图交付物校验均通过。
- 2026-06-16 06:31 in-app Browser live DOM 复核：390px 视口打开普通结果页、`channel=share` 分享落地页、匹配页和后台总览，四页均 `horizontalOverflow=0`、控件文字溢出数为 0、`.element-mark svg/path/img/canvas` 为 0、背景图残留为 0。普通结果页 `shareBoxCount=1`，分享落地页 `shareBoxCount=0` 且 `sharedEntryBannerCount=1`，确认外部分享回流不会再显示二次分享盒。
- 2026-06-16 06:32 视觉截图目检：刷新后的 `iphone-se-04-result.png`、`iphone-se-05-shared-result.png`、`android-wide-06-match.png`、`iphone-se-08-admin-overview.png` 均未出现五行简笔画；结果页短码、保存/复制按钮和匹配页图例文字无挤压，移动后台改为纵向信息流后无页面级横向桌面画布。
- 2026-06-16 06:53 第四轮稳定性巡检：连续 20 分钟、40 次检查全部通过，前端首页均返回 HTTP 200，后端 `/api/readiness` 均为 `UP`。本轮巡检先在沙箱内误报 localhost 不可达，随后用非沙箱本机访问确认端口监听正常并完成完整巡检。
- 2026-06-16 06:54 最后轻量门禁：`node scripts/verify-frontend-contracts.mjs` 通过，继续覆盖 `all-showcase-png-geometry-contract`、`external-share-landing-contract`、`text-only-element-marks` 和 typography constraints；`scripts/verify-eight-hour-artifacts.sh` 通过，继续确认 21 张截图、PPT 联系表和 12 组性能报告。
- 2026-06-16 06:56 十小时尾段巡检：额外 3 次本机健康检查全部通过，前端首页 HTTP 200，后端 `/api/readiness` 为 `UP`，工作流计时超过完整 10 小时后收口。
- 视觉系统自检：颜色不是单一色相，主色为深青/金/暖底并辅以蓝灰和火土色；前端质量门继续锁定 `letter-spacing: 0` 和无 viewport 字号缩放。

最近一次 live smoke 覆盖：

- 后端 `/api/readiness`
- 前端同源 `/api/readiness`
- 创建人格结果
- `/s/{shortCode}` 短链跳转
- query 透传
- 双人匹配候选、创建和回读
- 后台 runtime health
- 后台短链 CSV 导出和默认 synthetic 排除口径
- 2026-06-16 03:24 复跑通过：`resultId=R20260616032455738145232`，`shortCode=gnNbJt`，`matchId=gnNbJt-GGPDn1`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 03:38 复跑通过：`resultId=R20260616033809182552436`，`shortCode=BHn4F4`，`matchId=BHn4F4-OoJGm6`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 03:54 复跑通过：`resultId=R20260616035453542915094`，`shortCode=XUbjcV`，`matchId=XUbjcV-Pwr3zr`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 04:22 复跑通过：`resultId=R20260616042214955273176`，`shortCode=MSNWhQ`，`matchId=MSNWhQ-WHsjGL`，`currentShortCode=WHsjGL`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 04:27 复跑通过：`resultId=R20260616042732543761000`，`shortCode=pQKfLc`，`matchId=pQKfLc-Kh4qi3`，`currentShortCode=Kh4qi3`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 04:53 复跑通过：`resultId=R20260616045311196124074`，`shortCode=7ayMr3`，`matchId=7ayMr3-L3AOuJ`，`currentShortCode=L3AOuJ`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 05:12 复跑通过：`resultId=R20260616051202368774923`，`shortCode=nO5JmX`，`matchId=nO5JmX-Ntl4D6`，`currentShortCode=Ntl4D6`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 05:29 复跑通过：`resultId=R20260616052921422222696`，`shortCode=ahzTk2`，`matchId=ahzTk2-X5JRPe`，`currentShortCode=X5JRPe`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 05:43 复跑通过：`resultId=R20260616054301856508826`，`shortCode=OJt0Lh`，`matchId=OJt0Lh-uUJyLS`，`currentShortCode=uUJyLS`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 05:56 复跑通过：`resultId=R20260616055621612816592`，`shortCode=sgvRur`，`matchId=sgvRur-1MZfuA`，`currentShortCode=1MZfuA`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。
- 2026-06-16 06:08 复跑通过：`resultId=R20260616060753419795423`，`shortCode=3eVQKQ`，`matchId=3eVQKQ-wOnAOf`，`currentShortCode=wOnAOf`，`compatibilityScore=79`，短链跳转和带 `channel=perf-test&campaign=result-card` 的共享跳转均正常，`runtimeHealth=ok`、前后端 readiness 均为 `UP`。

## 浏览器验收边界

Codex in-app Browser 当前仍不支持下载事件，因此不用于证明“文件已落盘”。真实下载验收由非沙箱 Chromium Playwright 覆盖。

本轮已经跑通：

- `scripts/mobile-e2e.sh`：验证分享图 PNG 下载文件名、文件头、文件大小、`900x1200` 宽高、复制 fallback、系统分享成功 payload、共享回流、后台 CSV、后台坏 token 清理、短链详情坏 token 清理和双人匹配。
- `scripts/capture-showcase-screenshots.sh`：生成 `iphone-se`、`android-wide`、移动端匹配页、移动端 404 页、移动后台总览、桌面结果页、桌面匹配页、桌面 404 页、桌面后台总览和桌面短链详情截图。

最后一轮用以下方式补足：

- `quality-check` 静态质量门。
- live smoke 前后端链路。
- in-app Browser DOM 检查：首页 motto 纯文字、结果页仅一个分享模块、保存/复制入口存在、元素标识纯文字。
- 双 agent 复审，无剩余 P0/P1。
