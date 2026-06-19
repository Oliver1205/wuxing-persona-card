# 五行人格项目展示 PPT 资产包

## 交付成品

- PPT 文件: [`wuxing-persona-project-showcase.pptx`](/Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/docs/artifacts/presentations/wuxing-persona-project-showcase.pptx)
- 12 页总览图: [`contact-sheet.png`](/Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/docs/artifacts/presentations/contact-sheet.png)
- 逐页讲稿: [`wuxing-showcase-speaker-notes.md`](/Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/docs/artifacts/presentations/wuxing-showcase-speaker-notes.md)
- 压测视觉简报: [`performance-visual-brief.md`](/Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/docs/performance-visual-brief.md)

## 内容结构

| 页码 | 主题 |
| ---: | --- |
| 1 | 项目定位：90 秒生成可分享人格卡 |
| 2 | 传播闭环：进入、完成、生成、分享、回流 |
| 3 | 卡片式问答降低移动端理解成本 |
| 4 | 结果页承接共鸣、解释和分享 |
| 5 | 后端把测算变成可持久化结果资产 |
| 6 | 部署架构：Nginx、Spring Boot、MySQL、Redis |
| 7 | 短链热路径低延迟设计 |
| 8 | 数据中台：证据索引、口径、复盘摘要与运行态 |
| 9 | 统计模型：`live_event + daily_metric` 分层 |
| 10 | 隐私与安全边界 |
| 11 | 本地阶梯压测记录与生产 QPS 边界 |
| 12 | 后续优化路线 |

## 讲解辅助

讲稿文档把每一页拆成“核心说法、怎么讲、证据位置、不要夸大、可能追问”，适合两种场景：

- 作品集展示：按 5 分钟主线讲产品闭环、工程闭环和性能闭环。
- 面试表达：遇到追问时直接切到对应页的证据位置和边界说明。
- 压测讲解：第 11 页可配合压测视觉简报，讲清 512/768 配置阶梯、本地边界和后续公网压测矩阵。

## 构建与调试材料

- 幻灯片源码: [`outputs/.../slides`](/Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase/slides)
- 单页预览: [`outputs/.../preview`](/Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase/preview)
- 布局检查 JSON: [`outputs/.../layout/final`](/Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase/layout/final)
- 构建清单: [`artifact-build-manifest.json`](/Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/docs/artifacts/presentations/artifact-build-manifest.json)

## 复现命令

```bash
PATH=/Users/linyuxiang/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin:/Users/linyuxiang/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH \
node /Users/linyuxiang/.codex/plugins/cache/openai-primary-runtime/presentations/26.601.10930/skills/presentations/scripts/build_artifact_deck.mjs \
  --workspace /Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase \
  --slides-dir /Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase/slides \
  --out /Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/docs/artifacts/presentations/wuxing-persona-project-showcase.pptx \
  --preview-dir /Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase/preview \
  --layout-dir /Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase/layout/final \
  --contact-sheet /Users/linyuxiang/JavaBackend/06_Tools/skills/wuxing-persona-card/outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase/preview/contact-sheet.png \
  --slide-count 12
```
