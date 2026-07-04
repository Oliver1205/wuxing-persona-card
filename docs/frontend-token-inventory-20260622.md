# 2026-06-22 前端 Token 盘点

本文件从当前 `frontend/src/style.css` 和 `frontend/src/utils/elementVisuals.ts` 读取整理，用于后续前端视觉改造前确认真实 token。它补充 `docs/frontend-visual-system.md`，不替代视觉系统。

## 当前全局基线

来源：`frontend/src/style.css`

| 主题 | 当前值 | 说明 |
| --- | --- | --- |
| UI 字体 | `HarmonyOS Sans SC`, `MiSans`, `PingFang SC`, `Inter`, `Noto Sans SC`, `Hiragino Sans GB`, `Microsoft YaHei`, `sans-serif` | 全局 UI 字体栈。 |
| Display 字体 | `HarmonyOS Sans SC`, `MiSans`, `PingFang SC`, `Noto Sans SC`, `sans-serif` | 当前 display 仍偏无衬线；结果身份标题如需宋体风格，应单独设计。 |
| 正文色 | `#24302f` | 页面主要文字。 |
| 页面底色 | `#f6f3ec` | 暖纸底。 |
| body 最小宽 | `320px` | 移动端下限。 |
| button 高度 | `min-height: 44px` | 触控目标基线。 |
| button 圆角 | `8px` | 控件上限基线。 |
| 主按钮色 | `#2f6f5e` | 深绿主行动。 |
| 次按钮边框 | `rgba(36, 48, 47, 0.18)` | 白底边框次行动。 |
| 页面 padding | `28px 18px` | H5 页面基础留白。 |
| shell 宽度 | `min(100%, 760px)` | H5 内容居中宽度。 |
| panel 背景 | `rgba(255, 255, 255, 0.78)` | 卡片/面板底。 |
| panel 边框 | `rgba(36, 48, 47, 0.12)` | 轻边框。 |
| panel 阴影 | `0 12px 36px rgba(31, 48, 43, 0.08)` | 克制阴影。 |
| eyebrow 色 | `#7b5d35` | 小标题/提示。 |
| h1 | `52px`, `font-weight: 950`, `line-height: 1.08` | 首屏大标题，不能滥用到后台工具面板。 |
| h2 | `24px`, `font-weight: 900`, `line-height: 1.25` | 页面/模块标题。 |
| notice 强调色 | `#d79b43` | 左边线提示。 |
| error 背景 | `#fff0ed` | 错误态浅底。 |
| error 文本 | `#9d3929` | 错误态文字。 |
| input/select 高度 | `min-height: 44px` | 表单触控基线。 |
| input/select 边框 | `rgba(36, 48, 47, 0.16)` | 表单边框。 |

## 五行色

来源：`frontend/src/utils/elementVisuals.ts`

| code | 名称 | 主色 | 浅底 | 关键词 |
| --- | --- | --- | --- | --- |
| `METAL` | 金 | `#bf8918` | `#f8f0dc` | 收束、淬炼、清醒 |
| `WOOD` | 木 | `#2f705e` | `#e7f2ed` | 生长、舒展、灵动 |
| `WATER` | 水 | `#1e5f9f` | `#e4eef7` | 润泽、沉静、蓄藏 |
| `FIRE` | 火 | `#b84d35` | `#f8e6de` | 热烈、上扬、明朗 |
| `EARTH` | 土 | `#7b5c32` | `#eee5d3` | 承载、滋养、收成 |
| fallback | 未知 | `#2f705e` | `#e7f2ed` | 观察、平衡、生长 |

## 当前 CSS 风格判断

当前样式已经符合：

- 暖纸底。
- 深绿主行动。
- 8px 控件圆角。
- 44px 移动触控下限。
- 白底轻边框面板。
- 五行色集中在元素语义，不做全屏装饰。

仍要注意：

- `h1` 为 52px，只适合 H5 首屏或强身份标题，不能复制到后台卡片。
- 当前 CSS 没有把 token 全部声明成 `--color-*` 变量；后续大改可以逐步抽变量，但小修不要为了 token 化做大范围重构。
- `--font-display` 当前仍是无衬线；如果要结果页更有“中式书写感”，应局部验证宋体/衬线 fallback，不要全局替换。
- `filter-bar` 当前桌面是 4 个 `minmax(140px, 1fr)` 加 3 个 auto；移动端表单/筛选改造要特别查横向溢出。

## 改造规则

### 可以稳定复用

- `#f6f3ec` 页面底色。
- `#24302f` 正文色。
- `#2f6f5e` 主行动色。
- `#d79b43` 轻提示/强调。
- `8px` 圆角上限。
- `44px` 控件高度下限。
- panel 的轻边框和轻阴影。

### 不要随意改

- 五行主色和浅底。
- testid 相关控件结构。
- 分享图输出比例。
- 后台 `includeSynthetic`、`statSource`、`perf-test` 的可见口径。

### 需要先截图验证

- 全局字体栈。
- H1/H2 字号。
- `.panel` padding / shadow。
- `.filter-bar` 栅格。
- 后台表格和移动卡片转换。
- 结果页身份卡的字体和间距。

## Codex 使用方式

前端视觉任务开始时，Codex 应先读：

```text
docs/frontend-token-inventory-20260622.md
docs/frontend-visual-system.md
frontend/src/style.css
frontend/src/utils/elementVisuals.ts
```

然后再决定：

1. 只做小修。
2. 局部调整页面样式。
3. 抽取或整理全局 token。
4. 新增截图/DOM 断言。

不要跳过真实 CSS 直接按审美词生成新色板。
