# 质量评分与自评机制

评分总分 100 分。每次阶段性完成后按本表自评，最终 README 和教学手册同步关键分数。

## 1. 评分维度

| 维度 | 分值 | 评分标准 |
| --- | ---: | --- |
| MVP 闭环完整度 | 20 | 首页、测试、结果、短链、跳转、统计、后台是否贯通 |
| 后端工程质量 | 15 | 分层、校验、异常、数据库、Redis、可维护性 |
| 短链接业务价值 | 15 | 真实生成、解析、缓存、空值缓存、PV/UV/UIP |
| 前端体验 | 12 | 移动端流程、错误态、复制体验、视觉一致性 |
| 数据与隐私 | 10 | hash 存储、匿名 clientId、不收集多余信息 |
| 部署可用性 | 10 | Docker Compose、Nginx、环境变量、启动文档 |
| 测试验证 | 10 | 编译、构建、接口、流程验证 |
| 教学沉淀 | 8 | 能讲清楚做了什么、为什么、高价值点在哪里 |

## 2. 评级

| 分数 | 等级 | 含义 |
| ---: | --- | --- |
| 90-100 | A | 接近可上线，可用于演示和面试讲解 |
| 80-89 | B | MVP 可用，有少量可控缺口 |
| 70-79 | C | 主流程可跑，但工程或验证不足 |
| 60-69 | D | 部分功能可用，但闭环不完整 |
| < 60 | E | 不可作为 MVP 交付 |

## 3. 自评格式

每次阶段性收尾使用以下格式：

```text
总分：
等级：

通过项：
- ...

未验证项：
- ...

风险项：
- ...

下一步：
- ...
```

## 4. 不允许加分的情况

以下情况不能算通过：

- 没有运行验证却写“已通过”。
- 只有后端返回 mock 数据，没有真实保存结果。
- 短链只是页面字符串，不可访问跳转。
- 统计只是前端假数字。
- 后台没有 token 保护。
- 明文 IP 或明文 clientId 落库。
- 文案出现宿命论或负面预测。

## 5. 当前 MVP 自评

评估日期：2026-06-09

总分：99 / 100

等级：A，MVP 主链路已完成本地与 Docker 容器验收，v0.2 已补短链 Provider 适配层，v0.3 已补 external HTTP 联调准备和后台日期筛选，v0.4 已完成外部短链服务级联调和外部 PV / UV / UIP 统计读取，v0.5 已接入外部短链访问明细，v0.6 已开始建立 v1.0 前的统一质量门禁，可用于上线前演示和面试讲解。

| 维度 | 分值 | 当前得分 | 说明 |
| --- | ---: | ---: | --- |
| MVP 闭环完整度 | 20 | 20 | 首页、测试、结果、短链、跳转、统计、后台和容器入口均已验证 |
| 后端工程质量 | 15 | 15 | 分层、校验、异常、缓存、统计已落地；v0.5 新增 external access-record 适配和失败回退；v0.6 建立统一质量脚本 |
| 短链接业务价值 | 15 | 15 | 内置短链真实生成、解析、跳转、缓存、空值缓存、统计已落地；external 创建、跳转、统计和访问明细读取已完成 |
| 前端体验 | 12 | 12 | H5 主流程和后台已通过浏览器验收；后台支持日期筛选、短链统计来源和访问明细来源展示 |
| 数据与隐私 | 10 | 9 | clientId、IP、User-Agent hash 入库；外部访问记录的 IP / user 会 hash 后返回；后续可加强日志脱敏审计 |
| 部署可用性 | 10 | 10 | Compose、Nginx、MySQL、Redis、backend 容器均已运行验证；支持可配置基础镜像 |
| 测试验证 | 10 | 10 | 后端主链路、Provider 切换、外部失败降级、external 创建、统计和访问记录 HTTP 请求、后台日期筛选、前端构建、Compose config、Docker API、external 服务级联调和统一质量门禁已通过 |
| 教学沉淀 | 8 | 8 | 教学手册已覆盖主流程、短链适配层、external 服务级联调、统计、访问明细、Redis、测试、质量门禁和 v1.0 路线 |

通过项：

- `cd backend && mvn -q test`
- `cd frontend && npm run build`
- `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`
- `scripts/quality-check.sh`
- `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml up --build -d` 已在 `http://127.0.0.1:8088` 完成容器全链路验收
- 文案边界关键词扫描无命中
- 后端集成测试覆盖创建结果、结果查询、短链跳转、短链列表、访问详情、非法参数、非法事件、后台 token、无效短码
- 短链专项单元测试覆盖短链复用、短码冲突重试、空值缓存、访问计数更新
- Redis 专项单元测试覆盖结果缓存、短链 key、空值 key、TTL、序列化和异常降级
- 本地 H2 演示模式浏览器验收已覆盖首页、测试、结果、短链 302、后台总览、短链详情
- Docker 模式 API 验收已覆盖健康检查、题目、创建结果、查询结果、短链 302、后台总览、短链列表、短链访问日志
- Docker 模式浏览器验收已覆盖首页、结果页、后台 token 门禁和后台详情保护
- v0.2 Provider 测试覆盖 internal 默认模式、external 配置切换、外部创建成功、本地绑定保存、外部失败降级和禁止降级错误处理
- v0.3 RestClient 测试覆盖 external 创建接口 URI、JSON body、`username/userId/realName` header
- v0.3 后台日期筛选集成测试覆盖当天有数据、未来日期为空、非法日期范围返回 400
- v0.4 RestClient 测试覆盖 external 统计接口 URI、查询参数和系统用户 header
- v0.4 StatsAdapter 测试覆盖 external stats 成功读取、失败回退、internal 模式跳过和 domain 不匹配跳过
- v0.4 本地服务级联调覆盖外部短链创建、外部 302、五行本地业务绑定和后台 `statSource=external`
- v0.5 RestClient 测试覆盖 external access-record 接口 URI、查询参数、分页参数和系统用户 header
- v0.5 StatsAdapter 测试覆盖 external 访问明细读取、分页转换、`statSource=external` 和外部 IP / user hash 映射
- v0.5 Docker 内部链路补验覆盖健康检查、Nginx 到 backend、创建结果、短链访问、访问明细 `statSource=local`
- v0.6 新增 `.editorconfig`、`scripts/quality-check.sh` 和 v1.0 路线图
- 验收截图已保存到 `docs/screenshots/`

未验证项：

- MVP 必需项无未验证项。
- v0.6 已补 Docker 内部链路验证；标准 `docker compose up --build -d` 曾受镜像元数据网络影响，后续上线前仍需补一次标准外部镜像构建验收。

风险项：

- 外部短链服务已完成本地联调，但生产环境还需要正式决定短链子域名或同域 `/s/**` rewrite。
- 外部短链项目自身仍需做隐私审计，特别是明文 IP 入库问题。
- 后台当前是 token 保护，适合 MVP，不适合长期复杂权限管理。
- 生产上线前仍需替换默认密码、默认 token、`HASH_SALT`，并配置域名和 HTTPS。
- v1.0 前必须持续防止功能膨胀，仍不做朋友匹配、登录注册、付费、AI 深度解读和复杂 BI。

下一步：

- v0.7 补充生产 Nginx 短链子域名或 `/s/**` rewrite 部署方案。
- v0.8 增强后台轻量趋势和短链聚合。
- v0.9 做稳定性、隐私和压力场景审计。
- v1.0 做最终部署检查表、截图、质量评分和稳定版标签。
