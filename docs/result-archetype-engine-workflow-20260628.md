# 五行人格结果引擎交付标准

本文档用于固化结果页和 120 类人格分流引擎的上线前标准。后续改动先在本机预览验证，再统一上线。

## 核心目标

- 后端统一生成稳定的 `personaTypeId`，规则固定为 `primary + secondary + accent + relationKind`。
- 120 类人格必须完整覆盖：5 个主元素、4 个副元素、3 个点睛元素、2 种关系，共 120 条。
- 用户结果页只展示命中的那一条人格内容，不在前端临时拼凑核心分析。
- 用户端不得出现后台字段、枚举值或调试口径，例如 `dominant`、`balanced`、`personaTypeId`、`WATER-EARTH-FIRE-dominant`、`2/5`。
- 人格标签必须刚好四个汉字，只能出现一个“的”，且“的”只能放在第 2 个字或第 3 个字。允许结构为“酷的沙砾”“热的瀑布”，也允许“流动的砾”“清醒的锁”，整体要适合分享，正向、有画面感，不使用直白低质组合。

## 内容骨架

结果内容按以下顺序组织：

1. **人格标签与星官**
   - 展示四字人格标签、主副元素、星官名称。
   - 星官必须来自有记载的二十八宿名称，不能自行捏造。
   - 星官说明要交代所属方位/七宿、传统意象和放在本卡里的记忆锚点。

2. **日主依据**
   - 优先从真实出生日推导日柱天干。
   - 表述方式为“你的日主核心是某某，像……”，再解释这个日主如何影响性格底色。
   - 若缺少出生日，必须说明不强行展开具体日柱，只使用综合五行结构。

3. **主从关系**
   - 主元素要讲清楚“是什么状态的元素”。
   - 副元素不能只写“辅助”，也要讲清楚“是什么状态的元素”，以及如何调和、承接、推动或校准主元素。
   - `dominant` 关系写成主元素定底色、副元素调和节奏。
   - `balanced` 关系写成两种元素接近、互相轮流显影。

4. **点睛元素**
   - 不写平淡的“补充一点某元素”。
   - 要写得更有记忆点：像暗火、清铃、青芽、回声、隐台这类可感知意象。
   - 点睛元素不一定占比最高，但要解释它为什么能让人格更有辨识度。

5. **天人特质**
   - 单独成区块。
   - “天”讲内在运作：如何处理感受、信息、情绪和自我判断。
   - “人”讲外部落地：别人如何感受到你，你如何行动、表达、承接关系。

6. **成长建议**
   - 给具体、可执行的建议。
   - 方向包括多步计划、听取他人意见、固定学习/工作时间、复盘、边界和表达练习。

## 文案硬规则

- 以第二人称写，面向用户阅读，不写后台解释。
- 不写“命中类型”“后台字段”“比例型名称”“分类代码”。
- 不写 `2/5` 之类评分痕迹，只写“五题结果更偏向某属性”。
- 不把结果写成现实命运判断，保持娱乐性人格解读边界。
- 句子要有依据、有画面，不要堆砌玄学词，也不要像通用 MBTI 模板。

## 前端展示要求

- H5 结果页优先：标题紧凑、正文可读、无横向溢出。
- 顶部人格卡居中，背景跟随主元素，但不能大面积廉价铺色。
- 中式线条和边框要有秩序感，参考窗棂、屋檐、折线，不使用无意义装饰线。
- 正文行宽适中，移动端允许增加行数，减少一行字数。
- 结果页卡片之间要有清晰层次，但避免卡片套卡片导致拥挤。

## 验收清单

- 120 类全覆盖。
- 120 个 `personaTypeId` 唯一。
- 120 个 `personaLabel` 均为四个汉字，只含一个“的”，且结构只能是“一字 + 的 + 二字”或“二字 + 的 + 一字”。
- 用户可见文本不包含后台枚举或调试字段。
- 至少抽测 5 个代表性组合的日主、主从、点睛、天人和建议文案。
- 前端本地构建通过。
- 前端契约脚本通过。
- 后端相关单测通过。
- H5 结果页截图检查：无横向溢出，标题居中，正文可读，视觉层级自然。

## 2026-07-02 当前标签基线

> 本节为当前有效基线。下方 2026-06-28 记录保留为历史审计过程，其中出现的旧标签例子不再代表当前上线口径。

- 已把 120 个四字人格标签统一到“二字场景 + 的 + 主体器物/地貌”的命名体系，前两字承接副元素和点睛元素，最后一字落回主元素，减少机械拼接感。
- 当前后端标签表扫描结果：`count=120`、`unique=120`、`bad=[]`、`dup=[]`。
- 当前真实 API 水土火样例：`WATER-EARTH-FIRE-dominant` 返回 `星坛的潮`，星官为 `虚宿`，主 / 辅 / 点睛为水 / 土 / 火。
- 当前 H5 结果页样例：`http://127.0.0.1:5178/result/R20260702035549406362625`，移动端 `408 × 758` 视口无横向溢出，用户可见文本未发现 `personaTypeId`、`dominant`、`balanced`、英文五行枚举、`/5`、`命中类型` 等后台口径。
- 当前审阅稿：`docs/persona-archetype-catalog-20260628.md` 已由 `PersonaArchetypeCatalogExportTest` 重新导出，包含最新 120 类标题和正文。
- 当前验证通过：
  - `mvn -q -f backend/pom.xml -Dtest=PersonaArchetypeRegistryTest,ResultTextServiceTest,PersonaArchetypeCatalogExportTest test`
  - `mvn -q -f backend/pom.xml -Dtest=PersonaArchetypeCatalogExportTest -DexportPersonaCatalog=true test`
  - 浏览器 DOM 扫描：结果页 `overflow=0`、禁词命中为空、`星坛的潮 · 虚宿` 可见。

## 2026-06-28 本轮审计记录

- 已将 120 类人格注册器的文案验收写入 `PersonaArchetypeRegistryTest`：覆盖 120 条唯一分流、四字含“的”标签、主从/点睛/天人/成长建议完整度、第二人称表达、后台词泄露拦截。
- 已将 `ResultTextService.build()` 的真实生成路径写入全量审计：遍历 120 种命中组合，确认最终用户可见文本不出现 `dominant`、`balanced`、`personaTypeId`、英文五行枚举、`2/5`、`命中类型`、`主从关系：`、`占比` 等旧口径。
- 已清理 `ResultTextService` 中不再走主路径的旧拼文案方法，避免未来维护时误接回“占比”“比例接近”等旧表达。
- 已把通用人格模板改为更面向用户的第二人称表述：主元素负责定主调，副元素负责校准节奏、承接边界或补足落点，点睛元素统一使用更有画面感的暗记意象。
- 已把人格标签格式固化为硬约束：`酷的沙砾`、`热的瀑布`、`流动的砾`、`清醒的锁` 这两种四字结构合法，缺少“的”、多于四字、多个“的”或夹杂英文都会被单测拦截。
- 已把人格标签从规则拼接升级为 120 条显式命名表：每个 `primary + secondary + accent + relationKind` 组合都有独立四字标签，避免出现机械拼接感。当前独立扫描结果：120 条、120 个唯一、0 个非法。
- 本轮验证通过：
  - `mvn -q -Dtest=PersonaArchetypeRegistryTest,ResultTextServiceTest,ResultServiceTest test`
  - `npm --prefix frontend run build`
  - `node scripts/verify-frontend-contracts.mjs`
  - H5 结果页 Playwright DOM 检查：`outputs/result-page-mobile-20260628-120-audit.png`，无横向溢出，无后台字段泄露。

## 2026-06-28 标签库升级记录

- 后端 `PersonaArchetypeRegistry` 使用显式 `PERSONA_LABELS` 表维护 120 个结果标签。
- 标签表初始化阶段会检查总数为 120，写入阶段会检查每条标签都是四个汉字、只含一个“的”，且“的”只能在第 2 或第 3 个字。
- 已保留用户指定方向中的可分享短语，例如 `迅猛的剑`、`热的瀑布`、`酷的沙砾`、`果断的沙`。
- 已做第二轮标签审美打磨，替换明显机械或别扭的短语，例如把 `明热的泊` 调整为 `明亮的泊`，把 `厚的瀑布` 调整为 `厚土的潮`，把 `稳的银锁` 调整为 `稳重的锁`。
- 已做第三轮标签读感清理：减少 `里的`、`边的`、`上的`、`下的`、`畔的` 等位置式表达，改为更像人格短语的意象组合，例如 `灯下的竹` 改为 `灯影的竹`，`雨里的火` 改为 `雨焰的枝`。
- 本轮验证通过：
  - `mvn -q -Dtest=PersonaArchetypeRegistryTest,ResultTextServiceTest,ResultServiceTest test`
  - `mvn -q -Dtest=PersonaArchetypeCatalogExportTest -DexportPersonaCatalog=true test`
  - `npm --prefix frontend run build`
  - `node scripts/verify-frontend-contracts.mjs`
  - 独立 Node 扫描：`count=120`、`unique=120`、`invalid=[]`、`positionLike=[]`。
  - 本地真实 API 样例：`FIRE-WATER-EARTH-dominant` 返回 `明亮的泊`，用户可见字段无后台词泄露。
  - 浏览器 DOM 验收：`http://127.0.0.1:5178/result/R20260628125128741961011` 无横向溢出，顶部身份卡标题居中；截图保存为 `outputs/result-page-mobile-20260628-explicit-labels.png`。

## 2026-06-28 用户可见文案清理记录

- 已把用户可见文案中的“第二属性”继续收敛为“辅助气质”“校准节奏”“不是简单陪衬”等解释性语言，避免结果页读起来像后台分类说明。
- 已补强禁止词测试：结果页正文不得出现 `第二属性`、`命中类型`、`dominant`、`balanced`、`personaTypeId`、`WATER-EARTH-FIRE-dominant`、`2/5` 等后台或调试口径。
- 已把 120 条人格目录导出改为内容审阅稿：目录只保留标签、元素关系、点睛元素、审阅索引和正文，不再输出后端分流编号。
- 已重新抽测真实 API 样例：`WATER-EARTH-FIRE-dominant` 的用户标签为 `流动的砾`，用户可见字段无后台词泄露，开头采用“你的日主核心是癸水，是深潭的静水……”的具体日主说明。
- 已用移动端 Playwright 检查 `http://127.0.0.1:5178/result/R20260628132340517522893`：无横向溢出，四字人格标签合法，用户可见文本未发现后台词；截图保存为 `outputs/result-page-mobile-20260628-user-copy-clean.png`。

### 5 组真实 API 抽样

| 样例 | 结果地址 | 标签 | 星官 | 主 / 辅 / 点睛 | 禁词扫描 |
| --- | --- | --- | --- | --- | --- |
| 水土样例 | `http://127.0.0.1:5178/result/R20260628140910417357592` | 流动的砾 | 虚宿 | 水 / 土 / 火 | 通过 |
| 火水样例 | `http://127.0.0.1:5178/result/R20260628140910444698401` | 热的瀑布 | 井宿 | 火 / 水 / 木 | 通过 |
| 金木样例 | `http://127.0.0.1:5178/result/R20260628140910449250581` | 清醒的竹 | 昴宿 | 金 / 木 / 水 | 通过 |
| 木火样例 | `http://127.0.0.1:5178/result/R20260628140910455946812` | 热烈的星 | 角宿 | 木 / 火 / 水 | 通过 |
| 土金样例 | `http://127.0.0.1:5178/result/R20260628140910461625986` | 坚石的剑 | 张宿 | 土 / 金 / 火 | 通过 |

抽样规则：使用真实 `/api/questions` 题目结构提交答案，只扫描结果页会渲染的解释性字段；API 内部仍保留分流编号供后端路由和测试使用，但不得出现在用户页面正文。

H5 回归：以上 5 个结果页已用 `408 × 758` 移动端视口逐页打开，标签均为四字且只含一个“的”，页面正文禁词扫描通过，横向溢出均为 0；首个样例截图保存为 `outputs/result-page-mobile-20260628-five-sample-v2-first.png`。

## 2026-06-28 标签与建议文案二次打磨

- 已对 120 条四字标签做新一轮审美筛选，重点替换机械、直白或读感别扭的组合，例如 `台火的花` 改为 `台焰的花`，`厚生的砾` 改为 `厚土的砾`，`流动的台` 改为 `流银的台`，`坚决的剑` 改为 `坚石的剑`。
- 保留用户明确给过方向的标杆短语，例如 `迅猛的剑`、`热的瀑布`、`果断的沙`、`酷的沙砾`。
- 已删除早期自动拼标签的废弃函数，当前 120 标签只来自显式表，避免未来误接回机械拼接逻辑。
- 已把成长建议里的“副元素/附属标签”式表达改成用户可读语言：`给节奏一个校准`，正文改为“某元素适合在旁边提醒你……”，不再像字段解释。
- 重新验证：`PersonaArchetypeRegistryTest`、`ResultTextServiceTest`、`ResultServiceTest` 通过；标签扫描结果为 `count=120`、`unique=120`、`invalid=[]`、`duplicates=[]`。
- 重启本地后端后复测真实接口：`http://127.0.0.1:5178/result/R20260628142556936403308` 返回 `坚石的剑`，成长建议包含 `给节奏一个校准`，未发现“副元素/附属标签/第二属性”残留。

## 2026-06-28 120 正文生成器升级

- 已把非试点人格的默认文案从薄模板升级为元素关系生成器：主从关系会先描述主元素的具体状态，再说明第二层元素如何收束、推进或校准，并补充五行相生/相制解释。
- 点睛元素不再写成平铺的“补充项”，统一改为暗线式表达：它不是最响的一笔，而是藏在结构里的记忆机关，用来解释为什么这张卡有额外辨识度。
- 天/人描述已随主元素、第二层元素和点睛元素生成，分别表达内在运行与外部感受，避免所有人格读起来像同一段泛化 MBTI 文案。
- 成长建议已绑定元素：主元素给第一行动方式，第二层元素给校准方式，点睛元素给启动机关，最后收束到复盘、输出和时间块。
- 新增测试拦截旧模板句式：`两种材料在同一件器物里`、`辅助气质，不只是陪衬`、`藏在画面暗处的一枚记号`、`两种声音彼此校准` 等语句不得回流。
- 重新导出 `docs/persona-archetype-catalog-20260628.md`，目录审阅稿已更新到新版正文；标签扫描仍为 `count=120`、`unique=120`、`invalid=[]`、`duplicates=[]`。
- 验证通过：`mvn -q -f backend/pom.xml -Dtest=PersonaArchetypeRegistryTest,ResultTextServiceTest,ResultServiceTest test`、`mvn -q -f backend/pom.xml -Dtest=PersonaArchetypeCatalogExportTest -DexportPersonaCatalog=true test`、`npm --prefix frontend run build`、`node scripts/verify-frontend-contracts.mjs`。
- 本地真实页面复测：`http://127.0.0.1:5178/result/R20260628145547786215591` 在 `408 × 758` H5 视口无横向溢出、无控制台错误、无后台词泄漏；截图保存为 `outputs/result-page-mobile-20260628-generator-upgrade.png`。
- 继续增强天/人段落：新增每个主元素的“内心常问什么”和“外部从哪里认出你”，让默认生成文本减少重复尾句，也更贴近用户要求的天人分开分析。
- 最终 H5 抽样：`http://127.0.0.1:5178/result/R20260628151210342840307` 在移动端视口无横向溢出、无控制台错误、无后台词泄漏；截图保存为 `outputs/result-page-mobile-20260628-heaven-human-final.png`。
- 继续降低模板感：主从关系中的第二层元素效果改为按五行变化，例如金给边界锋面、木给表达延展、水给细腻回旋、火给启动热度、土给承接节奏；点睛元素也改成更有记忆点的暗线描述。
- 已修正点睛元素在天/人段落里的读感：把容易变成长句动词的表达压成 `清响边界`、`缝隙生门`、`深处回声`、`伏火入口`、`隐台落点` 这类短意象，避免出现“带来把判断……”这种不顺的句子。
- 最终截图：`http://127.0.0.1:5178/result/R20260628152503461647543` 在移动端视口无横向溢出、无控制台错误、无后台词泄漏；截图保存为 `outputs/result-page-mobile-20260628-generator-final.png`。
- 移动端全流程回归通过：`env E2E_BASE_URL=http://127.0.0.1:5178 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`，9 条 Playwright 用例通过，覆盖创建结果、分享图下载、短链跳转、双人匹配、后台概览、无效 token 和短链详情错误态。

## 2026-06-28 标签规则与浏览器验收收口

- 已再次确认 120 类人格标签规则落在后端硬测试里：每个 `personaLabel` 必须刚好四个汉字，只能出现一个“的”，且“的”只能位于第 2 或第 3 个字；`PersonaArchetypeRegistryTest` 会遍历 120 种 `primary + secondary + accent + relationKind` 组合，缺任意模板或标签格式不合规都会失败。
- 已修正 showcase 首页视觉顺序断言：检查真实展示节点 `.secondary-copy`，避免用父容器 `.home-secondary` 造成错误的视觉顺序判断。
- 已补齐移动端触控尺寸：测试页返回按钮、步骤圆点、年份控件、快捷标签、底部主按钮，以及结果页分享备选按钮和重新测试入口均保持不小于 44px。
- 已重新生成一条真实 API 水土样例：`http://127.0.0.1:5178/result/R20260628162950746344391`，返回标签为 `流动的砾`，四字且只含一个“的”；用户会看到的正文未发现 `命中类型`、`dominant`、`balanced`、`personaTypeId`、`第二属性`、`副元素` 等后台或字段式口径。
- 本轮 Playwright 在沙箱内首次失败，原因是 macOS 拒绝 Chromium `MachPortRendezvousServer` 注册，属于浏览器进程权限问题；已用同一命令在非沙箱环境重跑，排除代码回归。
- 本轮验证通过：
  - `mvn -q -f backend/pom.xml -Dtest=PersonaArchetypeRegistryTest,ResultTextServiceTest,ResultServiceTest test`
  - `npm --prefix frontend run build`
  - `node scripts/verify-frontend-contracts.mjs`
  - `env E2E_BASE_URL=http://127.0.0.1:5178 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh`，9 条通过。
  - `env E2E_BASE_URL=http://127.0.0.1:5178 E2E_ADMIN_TOKEN=dev-token SHOWCASE_SCREENSHOT_DIR=docs/screenshots/showcase scripts/capture-showcase-screenshots.sh`，11 条通过，截图已更新到 `docs/screenshots/showcase`。
