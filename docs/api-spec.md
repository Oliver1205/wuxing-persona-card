# API 说明

当前状态：第一版 MVP 已实现以下接口。统一响应结构为：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误响应示例：

```json
{
  "code": 400,
  "message": "birthMonth must be between 1 and 12",
  "data": null
}
```

## 1. 健康检查

```http
GET /api/health
```

返回服务状态和当前时间。

## 2. 获取题目配置

```http
GET /api/questions
```

返回 5 道价值取向题，每题 5 个选项，选项 code 对应五行元素。

## 3. 创建测算结果

```http
POST /api/results
Content-Type: application/json
X-Client-Id: <client-id>
```

请求体：

```json
{
  "birthYear": 2002,
  "birthMonth": 8,
  "birthDay": null,
  "birthTimeRange": null,
  "answers": [
    {"questionCode": "Q1", "optionCode": "METAL"},
    {"questionCode": "Q2", "optionCode": "WATER"},
    {"questionCode": "Q3", "optionCode": "METAL"},
    {"questionCode": "Q4", "optionCode": "EARTH"},
    {"questionCode": "Q5", "optionCode": "FIRE"}
  ]
}
```

后端行为：

- 校验出生年月、可选日期、可选时段、5 道题答案。
- 计算五行主副比例。
- 生成星官、关键词和三段正向文案。
- 保存 `user_result`。
- 生成内置短链接并保存 `short_link`。
- 写入 `TEST_SUBMIT`、`RESULT_CREATED`、`SHORT_LINK_CREATED` 事件。

## 4. 查询测算结果

```http
GET /api/results/{resultId}
X-Client-Id: <client-id>
```

后端优先读取 Redis `result:{resultId}`，未命中再查 MySQL，并写入 `RESULT_VIEW` 事件。

## 5. 短链接访问

```http
GET /s/{shortCode}
```

行为：

- 校验短码为 Base62，长度 6 或 7。
- 写入 `SHORT_LINK_VISIT` 事件。
- 优先读取 Redis `shortlink:code:{shortCode}`。
- Redis 未命中查 `short_link`。
- 有效短码 302 到 `/result/{resultId}?sc={shortCode}`。
- 无效短码写入 Redis 空值缓存 `shortlink:null:{shortCode}` 并返回 404。

## 6. 记录前端事件

```http
POST /api/events
Content-Type: application/json
X-Client-Id: <client-id>
```

请求体：

```json
{
  "eventType": "START_TEST_CLICK",
  "pagePath": "/",
  "resultId": null,
  "shortCode": null
}
```

支持事件包括：

```text
PAGE_VIEW_HOME
START_TEST_CLICK
TEST_SUBMIT
RESULT_CREATED
RESULT_VIEW
SHORT_LINK_CREATED
SHORT_LINK_COPY
SHORT_LINK_VISIT
```

## 7. 管理后台总览

```http
GET /api/admin/overview
X-Admin-Token: <admin-token>
```

返回：

- 总 PV / UV / UIP
- 首页访问量
- 开始测试点击量
- 测试提交量
- 结果生成量
- 短链生成量
- 短链访问量
- 完成率
- 热门五行组合
- 热门星官
- 最近结果
- 最近短链

## 8. 短链接列表

```http
GET /api/admin/short-links?page=1&pageSize=20
X-Admin-Token: <admin-token>
```

返回短码、短链接、结果 ID、五行组合、星官、创建时间、PV、UV、UIP、最近访问时间。

## 9. 短链接访问日志

```http
GET /api/admin/short-links/{shortCode}/visits?page=1&pageSize=20
X-Admin-Token: <admin-token>
```

返回单条短链访问日志，只展示 hash 后的匿名标识。
