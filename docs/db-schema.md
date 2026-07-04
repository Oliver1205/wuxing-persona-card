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
persona_type_id
accent_element
relation_kind
persona_label
day_master_text
primary_secondary_text
accent_text
heaven_text
human_text
star_officer_text
growth_advice_json
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

结果引擎字段说明：

- `persona_type_id`：120 人格注册表里的稳定后端 id，只用于存储、排查和统计，不应该原样展示给用户。
- `accent_element`：点睛元素，从非主副元素中选择，用来让人格标签更有辨识度。
- `relation_kind`：主从关系类型，当前对应主导型和均衡型两类。
- `persona_label`：用户可见的人格标签，当前要求四个汉字且包含“的”字。
- `day_master_text`：日主依据说明，从真实出生日柱或出生信息边界出发。
- `primary_secondary_text`：主元素和副元素的关系分析。
- `accent_text`：点睛元素分析。
- `heaven_text` / `human_text`：内在世界与外部感受。
- `star_officer_text`：星官或星宿记忆锚点说明。
- `growth_advice_json`：结构化成长建议，便于前端按卡片展示。

字段边界：

- `persona_type_id`、`accent_element`、`relation_kind` 是系统内部分类字段，主要用于稳定分流、后台统计、问题排查和后续 A/B 版本对比。
- 用户端只展示 `persona_label`、`star_officer_name`、结构化说明文案和五行比例，不展示 `WATER-EARTH-FIRE-dominant` 这类后台组合词。
- 如果未来批量替换 120 种人格文案，应优先改 `PersonaArchetypeRegistry` 和对应测试，再确认数据库历史结果是否需要迁移或继续使用创建时快照。

关键索引：

- `idx_persona_type_id`：支撑后续按 120 人格分布做统计或排查。
- `idx_primary_secondary`：支撑主副五行组合统计。

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
