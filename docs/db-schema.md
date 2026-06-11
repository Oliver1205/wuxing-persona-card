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

## 短链项目库

建议库名：`link`

短链项目自带 SQL 位于：

```text
/Users/linyuxiang/JavaBackend/01_Projects/shortlink/resources/database/link.sql
```

五行项目第一版已经内置 `short_link` 表以确保 MVP 闭环可独立运行。后续接入独立短链服务时，可把本表降级为业务绑定表，保留 `result_id`、`short_code`、`short_url` 等字段。
