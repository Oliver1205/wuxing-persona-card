# 七小时工作流交接索引

更新时间：2026-07-02

这份文档是给明天验收用的轻量索引。完整架构说明看
`docs/system-architecture-core-functions-20260702.md`，这份只回答四个问题：

1. 现在应该先看什么。
2. 这轮到底改了什么。
3. 哪些东西已经验证。
4. 哪些东西还不能误以为已经上线。

## 1. 明天建议先打开的文件

| 目的 | 文件 |
| --- | --- |
| 看整个系统架构、核心链路和简历表达 | `docs/system-architecture-core-functions-20260702.md` |
| 看 120 个人格名称候选稿 | `docs/persona-label-review-v5-20260702.md` |
| 看 API 和状态机契约 | `docs/api-spec.md` |
| 看数据库内部字段与用户展示边界 | `docs/db-schema.md` |
| 看前端答题状态机 | `frontend/src/utils/testFlowMachine.ts` |
| 看后端提交流状态机 | `backend/src/main/java/com/wuxing/persona/service/TestFlowStateMachine.java` |
| 看五行计算入口 | `backend/src/main/java/com/wuxing/persona/service/ElementCalculateService.java` |
| 看结果创建主链路 | `backend/src/main/java/com/wuxing/persona/service/ResultService.java` |
| 看 120 人格注册表 | `backend/src/main/java/com/wuxing/persona/service/PersonaArchetypeRegistry.java` |
| 看用户端结果文案组装 | `backend/src/main/java/com/wuxing/persona/service/ResultTextService.java` |

建议阅读顺序是：

```text
TestPage.vue
  -> testFlowMachine.ts
  -> ResultController.java
  -> ResultService.java
  -> ElementCalculateService.java
  -> TestFlowStateMachine.java
  -> PersonaArchetypeRegistry.java
  -> ResultTextService.java
```

这条线回答的是：一次用户答题，如何从 H5 操作变成一张稳定可复访、可分享、可统计的人格卡。

## 2. 本轮核心交付

### 2.1 前端交互

- 出生年份范围改为 `1950-2026`，同时受当前年份保护，不会出现未来年份。
- 当前年份下，未来月份不可点，当前月份可继续。
- 答题页底部动作统一：
  - 左侧：基础信息 / 上一题。
  - 右侧：进入第 1 题 / 下一题 / 生成我的人格卡。
- 浏览器返回在答题过程中优先回到上一题，不再直接跳回首页。
- 这些按钮状态不再散落在页面里判断，而是由 `frontend/src/utils/testFlowMachine.ts` 统一推导。

### 2.2 后端状态机

- 新增 `TestFlowStateMachine`，在结果计算前确认请求已经达到可提交状态。
- 新增 `TestFlowPolicy`，统一出生年份上下限和题目数量。
- 后端会规范化题号，比如 `q1`、` Q1 ` 会按同一题处理。
- 后端会拒绝不完整答案、重复题号、未知题号、无效年月和未来月份，避免绕过前端生成半成品结果。

### 2.3 架构文档

- `docs/system-architecture-core-functions-20260702.md` 已经整理：
  - 目录地图。
  - 生成结果主流程。
  - 分享短链流程。
  - 后台统计流程。
  - 核心类速查。
  - 后端简历表达抓手。
  - 状态机说明。
  - 质量检查记录。
  - 明天建议阅读路线。

### 2.4 人格名称候选稿

- `docs/persona-label-review-v5-20260702.md` 已整理 120 个候选人格名称。
- 规则：
  - 全部 4 个汉字。
  - 每个名字只含一个“的”字。
  - 避免后台字段和技术分类词。
  - 当前只是候选稿，等待你逐条审美确认。

注意：v5 名称已在你确认后写入后端注册表，当前本地预览会使用这版名称；如果验收时挑出个别名字，再按单条回改即可。

## 3. 已经跑过的质量检查

| 检查 | 结果 |
| --- | --- |
| 后端状态机与五行计算核心测试 | 通过 |
| 后端全量 Maven 测试 | 通过 |
| 前端构建 | 通过 |
| 前端契约检查 | 通过 |
| 人格名称文档检查 | 通过 |
| 移动端主流程 E2E | 非沙箱环境 11/11 通过 |
| `git diff --check` | 通过 |
| `scripts/quality-check.sh` 前半段 | 通过到 Docker fresh schema 阶段 |

`scripts/quality-check.sh` 在 fresh MySQL schema smoke 阶段被本机 Docker daemon 拦住：

```text
Docker API 500 Internal Server Error
```

这属于本机 Docker 环境问题，不是当前代码测试断言失败。Docker 恢复后建议重跑：

```bash
env REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh
```

## 4. 明天可以直接跑的验收命令

```bash
git diff --check
node scripts/verify-persona-label-docs.mjs
node scripts/verify-frontend-contracts.mjs
mvn -q -f backend/pom.xml -Dtest=TestFlowStateMachineTest,ElementCalculateServiceTest test
npm --prefix frontend run build
```

如果本地前后端还在运行，可以跑移动端主流程：

```bash
env E2E_BASE_URL=http://127.0.0.1:5178 E2E_ADMIN_TOKEN=dev-token scripts/mobile-e2e.sh
```

如果 5178 没开，先启动或换成实际 Vite 端口。

## 5. 不能误判为已经完成的事

| 事项 | 当前边界 |
| --- | --- |
| 线上部署 | 没有执行 |
| v5 人格名称正式替换后端注册表 | 已在用户确认后执行，本地预览可验收 |
| 完整 120 正文逐条人工终审 | 没有完成 |
| 公安备案修改 | 没有自动处理 |
| fresh MySQL Docker smoke | 因本机 Docker API 500 未完成 |

## 6. 下一轮最值得做的事

1. 先让你人工过 `docs/persona-label-review-v5-20260702.md` 的 120 个名字。
2. 确认通过后，把选定名称同步到 `PersonaArchetypeRegistry`。
3. 继续审 120 条正文，重点查：
   - 是否泄露后台字段。
   - 是否有“底色清晰型”这类后台式分类词。
   - 是否有 `/5`、`dominant`、`balanced`、`personaTypeId`。
   - 是否足够第二人称、足够像用户能分享的结果。
4. Docker 恢复后跑完整 `scripts/quality-check.sh`。
5. 最后再统一打包上线，不要把本地候选稿半路直接推生产。
