# 数据库设计草案

当前状态：第一版 MVP 已落地 SQL，正式脚本见 `backend/src/main/resources/db/schema.sql`。

## 五行项目库

建议库名：`wuxing_persona`

### `user_result`

保存一次用户测算结果。

关键字段：

```text
id
result_id
birth_year
birth_month
birth_day
birth_time_range
answer_json
primary_element
secondary_element
primary_percent
secondary_percent
all_element_scores_json
star_officer_code
star_officer_name
keywords_json
layout_explanation
strength_text
relationship_text
card_image_key
status
created_at
updated_at
```

### `short_link`

保存五行结果和第一版内置短链之间的映射与访问计数。

关键字段：

```text
id
short_code
result_id
original_path
short_url
pv_count
uv_count
uip_count
last_visit_at
status
created_at
updated_at
```

### `visit_event`

保存站内访问、点击、提交和结果生成等事件。

关键字段：

```text
id
event_type
page_path
result_id
short_code
client_id_hash
session_id_hash
ip_hash
user_agent_hash
channel
campaign
device_type
referer
event_date
created_at
```

v2.1 起，增长归因字段说明：

- `session_id_hash`：前端 sessionId hash，不保存明文 sessionId。
- `channel`：来源渠道，例如 `organic`、`share`、`shortlink`。
- `campaign`：活动标识，例如 `spring-launch`、`result-card`。
- `device_type`：根据 User-Agent 粗略归类为 `mobile`、`tablet`、`desktop`、`bot`、`unknown`。
- `event_date`：事件日期，便于后续演进日聚合表。

后台统计相关索引：

- `result_id + event_type + channel`：支撑默认视图排除 `perf-test` 测试流量时，对结果和短链创建事件的反查。
- `event_type + short_code + created_at + channel`：支撑短链列表批量统计 PV/UV/UIP，并兼容测试流量过滤。

## 日聚合表

### `site_daily_metric`

v2.2 新增，用于保存站点级日聚合指标。

关键字段：

```text
id
metric_date
pv
uv
uip
home_views
start_clicks
test_submits
result_created
short_link_created
short_link_visits
aggregated_at
```

规则：

- 只聚合今天以前的闭合日期。
- `metric_date` 唯一。
- 后台日趋势优先读该表，缺失日期回退 `visit_event` 实时查询。

### `short_link_daily_metric`

v2.2 新增，用于保存短链维度日聚合指标。

关键字段：

```text
id
metric_date
short_code
pv
uv
uip
last_visit_at
aggregated_at
```

规则：

- `metric_date + short_code` 唯一。
- 由 `visit_event` 中 `SHORT_LINK_VISIT` 事件聚合生成。

## 短链项目库

建议库名：`link`

短链项目自带 SQL 位于：

```text
/Users/linyuxiang/JavaBackend/01_Projects/shortlink/resources/database/link.sql
```

五行项目第一版已经内置 `short_link` 表以确保 MVP 闭环可独立运行。后续接入独立短链服务时，可把本表降级为业务绑定表，保留 `result_id`、`short_code`、`short_url` 等字段。
