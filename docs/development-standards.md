# 开发规范与质量门禁

适用范围：五行人格卡全仓库。

本项目 v1.0 前的目标是可上线、可演示、可讲清楚。所有实现必须服务于单人测算闭环、短链接真实业务接入、匿名统计和可部署稳定性，不做范围外功能。

## 1. 设计原则

1. 业务闭环优先：每次开发都要推进“测试 -> 结果 -> 短链 -> 访问统计 -> 后台”。
2. 分层清晰：Controller 只做参数接收和响应转换；业务规则放 Service；数据库访问放 Mapper/Repository；工具逻辑放 Util。
3. 简单稳定：第一版不引入消息队列、复杂权限、AI 生成、朋友匹配。
4. 隐私克制：不保存明文 IP、明文 clientId；生日日期和时段允许为空。
5. 文案合规：所有结果文案正向、娱乐、友好，不出现宿命论、恐吓预测和负面断言。
6. 可替换短链：第一版可内置短链能力，代码结构必须允许后续替换为独立短链服务。

## 2. 后端编码规范

技术栈：

- Java 17
- Spring Boot
- MyBatis-Plus / MyBatis Mapper
- MySQL
- Redis

约束：

1. 所有外部接口统一返回 `ApiResponse<T>`。
2. 所有请求 DTO 使用 Bean Validation 校验。
3. 所有业务异常使用统一异常类型或全局异常处理输出。
4. Controller 不写五行计算、短码生成、统计聚合等业务细节。
5. Redis 访问要允许失败降级，不因缓存异常影响核心结果查询。
6. 访问日志只保存 hash 后的匿名标识。
7. 短链短码只允许 Base62 字符，长度固定为 6。
8. 管理后台接口必须校验 `X-Admin-Token`。

## 3. 前端编码规范

技术栈：

- Vue 3
- Vite
- TypeScript
- Vue Router

约束：

1. 移动端优先，桌面端自然居中。
2. 用户流程必须一屏一任务：引导、测试、结果、后台分开。
3. API 调用统一走 `src/api/request.ts`。
4. 每次请求自动带 `X-Client-Id`。
5. 埋点统一走 `src/utils/tracker.ts`。
6. 所有页面必须展示或承接娱乐声明。
7. UI 不写复杂动画，不做营销落地页。

## 4. 数据库规范

1. 业务表使用明确索引支撑查询。
2. JSON 字段仅用于 MVP 快速保存结构化结果，后续可拆表。
3. 所有表有 `created_at` 和必要的状态字段。
4. 不保存明文 IP、明文 clientId。
5. 短链访问 PV 可在 `short_link` 冗余，UV/UIP 通过事件表去重。

## 5. 质量门禁

v0.6 起，每个阶段完成前必须先执行统一门禁：

```bash
scripts/quality-check.sh
```

该脚本必须保持可重复执行，不允许依赖本机私有环境或未提交文件。

统一门禁至少覆盖：

1. 编译检查：后端 `mvn test`，前端 `npm run build`。
2. 接口自测：核心 API 有 curl 示例或手动验证记录。
3. 流程自测：H5 能完成当前阶段的主流程。
4. 文案自测：搜索禁止词，不能出现负面宿命表达。
5. 数据自测：关键表能写入和查询。
6. Compose 配置：`docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`。
7. Git 卫生：构建产物、真实 `.env`、密码、token、`node_modules`、`dist`、`target` 不进入提交。

如果因为环境缺失无法执行某个门禁，必须在最终自评中标记为“未验证”，不能假装通过。

完整容器验收建议在每个大版本执行：

```bash
APP_BASE_URL=http://localhost:8088 \
NGINX_HTTP_PORT=8088 \
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml up --build -d
```

如果外部镜像源不可用，可以记录环境阻塞，并使用已有本地镜像做辅助验证；但最终上线前必须补一次标准 Compose 构建验证。

## 6. 代码评审清单

提交前自问：

1. 这个改动是否推进 MVP 闭环？
2. 是否引入了 AGENTS 明确禁止的一期功能？
3. Controller 是否过重？
4. 参数校验和错误返回是否清楚？
5. 是否有明文隐私数据落库？
6. Redis 失败是否会拖垮主流程？
7. 前端失败态是否可理解？
8. README 或教学手册是否同步了关键设计？
9. 是否执行并记录了 `scripts/quality-check.sh`？
10. 是否明确说明了 Docker、external 短链或浏览器验收中未覆盖的项目？

## 7. v1.0 发布纪律

1. 每个版本从 `main` 新建 `feature/vX.Y-...` 分支。
2. 只做当前版本目标内的改动，不顺手扩需求。
3. 每个版本必须有文档、测试、构建和质量评分同步。
4. 合并方式优先走 PR，合并后本地 `main` 同步远端。
5. 稳定版本必须打 tag。
6. v1.0 前不做登录注册、朋友匹配、付费、AI 深度解读和复杂 BI。
