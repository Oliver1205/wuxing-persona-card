# 数据库迁移治理计划

记录日期：2026-06-12

本文档用于回答“当前有 `schema.sql`，但生产环境如何安全演进表结构”。当前项目仍是单机作品集和演示部署，不声称已经具备成熟生产迁移体系；如果进入长期运行，应把初始化脚本升级为 Flyway 或 Liquibase 管理的版本化迁移。

## 1. 当前边界

现有脚本：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/schema-local.sql
```

当前配置：

```text
spring.sql.init.mode=always
spring.sql.init.continue-on-error=true
```

这套方式的价值是：

- 新环境能快速初始化 MySQL / H2 演示库。
- Docker Compose 和本地 H2 模式都容易拉起。
- Testcontainers profile 可以用真实 MySQL schema 验证主链路。

但它不等于生产迁移体系：

- `CREATE TABLE IF NOT EXISTS` 适合空库初始化，不适合表达每次版本升级的严格顺序。
- `ALTER TABLE` 和 `CREATE INDEX` 重复执行依赖 `continue-on-error` 吞掉已存在错误，容易掩盖真实 DDL 失败。
- 线上表结构漂移后，单个大脚本很难判断当前数据库处在哪个版本。
- 缺少迁移历史、回滚说明、灰度检查和慢 SQL / 锁表风险记录。

## 2. 目标迁移形态

进入生产化后建议引入 Flyway，按版本拆分：

```text
backend/src/main/resources/db/migration/
  V1__init_core_tables.sql
  V2__add_visit_event_attribution.sql
  V3__add_daily_metric_tables.sql
  V4__add_short_link_hot_path_indexes.sql
```

推荐拆分：

| 版本 | 内容 | 来源 |
| --- | --- | --- |
| `V1__init_core_tables.sql` | `user_result`、`short_link`、`visit_event` 初始字段和基础索引 | `schema.sql` 初始建表 |
| `V2__add_visit_event_attribution.sql` | `session_id_hash`、`channel`、`campaign`、`device_type`、`event_date` 和归因索引 | v2.1 增长归因 |
| `V3__add_daily_metric_tables.sql` | `site_daily_metric`、`short_link_daily_metric` | v2.2 日聚合 |
| `V4__add_short_link_hot_path_indexes.sql` | 短链列表、事件日期范围、distinct 辅助索引 | v2.4-v2.6 性能加固 |

迁移完成后：

- `spring.sql.init.mode` 在生产环境应关闭。
- 本地 H2 可继续保留 `schema-local.sql`，但必须说明它是演示脚本，不代表生产迁移。
- CI 至少跑一次真实 MySQL schema 验证，避免 H2 方言掩盖问题。

## 3. 上线迁移步骤

每次 DDL 上线前按这个顺序走：

1. 备份数据库，记录备份文件路径和校验信息。
2. 在测试库执行迁移，保存执行日志。
3. 对核心查询跑 `EXPLAIN`，至少覆盖短链跳转、后台 overview、短链列表和访问明细。
4. 评估是否会锁表；大表加索引要避开高峰期，必要时使用在线 DDL。
5. 先部署兼容旧字段和新字段的应用版本。
6. 执行迁移。
7. 运行 `scripts/production-smoke-test.sh` 和后台 smoke。
8. 观察错误率、慢查询、DB 连接数和 `asyncDroppedEvents`。

## 4. 回滚策略

原则：优先回滚应用，谨慎回滚 DDL。

| 场景 | 处理 |
| --- | --- |
| 新增 nullable 字段失败 | 停止发布，回滚应用，修正迁移脚本后重跑 |
| 新增索引导致锁表或耗时过长 | 停止迁移窗口，恢复应用流量，改在线 DDL 或低峰期执行 |
| 新表创建失败 | 回滚应用，删除半成品表前先确认没有写入数据 |
| 字段删除或改类型 | 不在同版本直接做；先灰度废弃字段，确认无读写后再单独迁移 |
| 数据修复脚本出错 | 优先从备份恢复或写反向修复脚本，不依赖口头手工操作 |

## 5. 面试表达

可以说：

> 当前仓库为了演示和单机部署，使用 `schema.sql` / `schema-local.sql` 快速初始化数据库，并用 Testcontainers 验证真实 MySQL 主链路。我不会把它包装成成熟生产迁移体系。如果项目进入长期生产，我会把现有 DDL 拆成 Flyway 版本脚本，关闭生产 `spring.sql.init`，每次 DDL 都走备份、测试库验证、EXPLAIN、迁移日志和回滚预案。

不要说：

> 已经有完整生产数据库迁移治理。

除非仓库已经真的接入 Flyway / Liquibase，并且 CI、部署和回滚文档都按迁移版本运行。
