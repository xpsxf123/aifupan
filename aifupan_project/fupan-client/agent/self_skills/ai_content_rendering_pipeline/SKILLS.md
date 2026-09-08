# ai_content_rendering_pipeline

## 本技能用途

- 统一 AI 内容渲染能力的使用方式：AI 内容容器、渲染管线、mdTag 标签占位与回填策略。
- 给“新增一种 AI 元素/图表/标签”提供可扩展落点。

## 适用场景

- AI 分析页（only/contrast/报告/分享）需要渲染 AI 输出内容
- 后端返回内容中包含 aifupan 自定义标签，需要渲染成组件/图表/流程图

## 输入

- AI 渲染元素列表：`els:Array`
- 渲染类型/频道：`type:String`
- 场景与模型：`scene/aiModel`
- 是否对比：`isCompare`

## 输出

- 页面侧对 `aiContent` 的正确接入方式（props/events）
- 新增渲染能力时的扩展点（parser 分发与标签定义）

## 统一入口

- 公共渲染组件：`src/components/aiContentRenderer/index.vue`
- 公共分发入口：`src/components/aiContentRenderer/renderers/aiContentRenderParser.js`
- 兼容入口（历史路径，内部转发到公共实现）：
  - `src/components/analysis/ai/common/aiContentRenderParser.js`
  - `src/components/analysis/ai/common/pureMdRenderParser.js`
  - `src/components/analysis/ai/common/mdTagRenderParser.js`
  - `src/components/analysis/ai/common/jsonRenderParser.js`

## 渲染管线约定

- mdTag：采用“完整 aifupan 标签占位 → 普通 Markdown 转 HTML → 回填标签 HTML”的策略
- pureMd：清除 aifupan 自定义标签，仅保留 Markdown 并渲染为 HTML（用于纯文本/降级模式）
- 内置能力：
  - 残留 Markdown 二次清洗
  - `aifupan-font` 行内样式标签
  - SVG/HTML 图表渲染（pie/bar/line/area）
  - Mermaid 线性链路漏斗化展示（流程图更偏纵向层级）

## 页面侧接入（示意）

```vue
<aiContent
  :els="els"
  :type="type"
  :scene="scene"
  :aiModel="aiModel"
  :isCompare="isCompare"
  :bindConfig="bindConfig"
  @answerAgain="handleAnswerAgain"
  @generateChartsHandler="handleGenerateCharts"
/>
```

## 扩展点（新增标签/图表/元素）

- 优先在 `src/components/aiContentRenderer/renderers/aiContentRenderParser.js` 做分发扩展，而不是在页面里写分支
- 新增标签时，保持“占位 → 转换 → 回填”的流程一致，避免直接拼接造成 XSS 风险与结构不稳定

