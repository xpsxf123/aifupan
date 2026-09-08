# 扩展 Markdown 渲染器设计说明（aifupan 自定义标签体系）

## 1. 概述

本渲染器解析包含 `aifupan-` 自定义标签的 Markdown 文本，实时渲染为富交互界面。

**核心设计**：
- 基于标准 Markdown 解析器（如 markdown-it 或 marked）处理纯 Markdown 部分。
- 通过预扫描提取自定义标签，替换为占位符，待 Markdown 转 HTML 后，再将占位符替换为组件 DOM。
- 支持流式渲染：逐行接收数据，对已闭合的标签立即渲染。

## 2. 整体流程

1. **标签扫描**：使用正则表达式识别所有 `<aifupan-...>` 标签。
2. **内容替换**：将每个完整标签替换为唯一占位符（如 `[AIFUPAN_0]`），记录标签信息（类型、属性、内部内容）。
3. **Markdown 解析**：将替换后的纯 Markdown 文本交给 Markdown 解析器，生成 HTML 字符串。
4. **组件渲染**：遍历记录的标签信息，调用对应渲染函数生成 DOM 元素。
5. **占位替换**：在生成的 HTML 中查找占位符，替换为真实 DOM。

### 2.1 设计原则

- 保持 Markdown 主体优先，自定义标签主要负责声明渲染意图。
- 单标签优先绑定“下一行/下一块” Markdown 数据。
- 双标签优先作为容器，内部仍允许标准 Markdown 继续表达正文。
- 对数据型标签按类型分流：有些标签应先提取结构化数据，再渲染；有些标签应先做 Markdown 转 HTML，再套用样式容器。

## 2.2 主题与皮肤控制

渲染器除了解析标签组件外，还需要支持文档级主题与皮肤：

- 主题（Theme）：控制主色调、标题气质、告警块颜色倾向、图表和卡片色彩基调
- 皮肤（Skin）：控制背景质感、阴影强度、边框风格、是否偏向玻璃感、纸张感或极简文档感

推荐枚举：

- Theme：`aurora`、`business`、`warm`
- Skin：`glass`、`paper`、`minimal`

解析器应支持两种策略：

1. 显式模式：文档中出现 `<aifupan-theme-xxx />`、`<aifupan-skin-xxx />`
2. 自动模式：若未显式声明，则根据内容特征自动推断

自动推断建议：

- 出现代码、Mermaid、流程图、监控、技术方案：优先 `aurora + glass`
- 出现复盘、分析、统计、表格、周报、报告：优先 `business + paper`
- 出现活动、亮点、海报、故事、庆典：优先 `warm + glass`
- 普通规则说明、接口说明、规范文档：优先 `business + minimal`

## 3. 标签解析细节

### 3.1 标签识别正则

可参考以下模式：
`/<aifupan-([a-z0-9-]+)(?:\s+([^>]*?))?\s*(\/?)>|<\/aifupan-([a-z0-9-]+)>/gi`

### 3.2 属性解析

将属性字符串 `key="value"` 解析为对象。注意处理转义引号。

### 3.3 内部内容处理

对于双标签，提取开始和结束标签之间的原始内容（可能包含 Markdown 或纯文本）。该内容在组件渲染时可能需要进一步解析（如表格数据提取、Markdown 转换等）。

对于单标签，还应支持“意图绑定”模式：

- 如 `<aifupan-echarts-pie />` 后紧跟标准 Markdown 表格，则该表格应被识别为该标签的数据源
- 如 `<aifupan-title-1-filled /> 标题文本`，则该行尾随文本应被识别为标题内容
- 绑定成功后，被消费的 Markdown 数据块不再走原始 Markdown 输出，而是直接进入对应组件渲染逻辑

## 4. 组件渲染映射表

| 标签类型 | 渲染逻辑 |
|----------|----------|
| `title-{n}` 及变体 | 创建 `<h{n}>`，附加对应 CSS 类（如 `.title-filled`）。双标签形式则将内部文本作为标题内容。 |
| `highlight` | 渲染为 `<mark>` 或带 `.highlight` 类的 `<span>`。 |
| `badge` | 渲染为 `<span class="badge">`。 |
| `callout` | 渲染为 `<div class="callout callout-{type}">`，内部内容经 Markdown 转换。 |
| `card` 系列 | 渲染为 `<div class="card">` 外层容器，优先解析内部 `card-head` 与 `card-body` 组合块；旧的 `title` 属性仅作兼容回退。 |
| `card-head` / `card-body` | `card-head` 渲染卡片头部，`card-body` 渲染正文区域，二者仅建议出现在 `card` 内部。 |
| `columns-2` | 渲染为 `<div class="columns-2">`，优先解析内部多个 `column` 子标签；若不存在 `column`，再回退到 `---` 分栏。 |
| `column` | 渲染为单列容器，内部 Markdown 独立转换，并负责清理列首尾空白与段落外边距。 |
| `list-check` | 解析内部列表，渲染为带复选框的列表。 |
| `list-icon` | 解析内部列表，并将 `icon` 语义值映射为可见符号，如 `check -> ✅`、`warning -> ⚠️`。 |
| `table-*` | 包裹内部 Markdown 表格，附加对应表格样式类（如 `.table-striped`）。 |
| `echarts-*` | 解析内部表格为图表数据，优先输出可直接查看的 SVG/HTML 图形；若运行环境具备 ECharts，可再增强为交互式图表。对于 `pie`，需支持单标签绑定后续 Markdown 表格的独立解析路径。 |
| `progress` | 统一解析 `value/status`，渲染为带填充宽度的进度条；旧的 `progress-*` 简写仅兼容，不再推荐生成。 |
| `statistic` | 仅适合数值型统计卡片；若传入文字内容，渲染器会降级为普通洞见卡片样式。 |
| `mermaid` | 优先读取内部标准 ` ```mermaid ` fenced code block，解析节点、判断节点与分支边，渲染为 PRD 风格纵向树形 SVG；无法稳定解析时再回退为代码块展示。 |
| `font` | 渲染为可控颜色、字号、粗细、背景色的行内文本样式，替代原始 `<font>` HTML 标签。 |
| `timeline` | 解析内部列表，每条渲染为时间线条目。 |
| `steps` | 解析内部列表，渲染为步骤条组件。 |
| `code` 增强 | 提取标题和行号属性，渲染带工具栏的代码块。 |
| `tabs` / `accordion` | 按二级标题分割内容，渲染交互式标签页或折叠组件。 |
| `divider` | 渲染为 `<hr class="divider">` 或带文字的分割线。 |
| `tooltip` | 渲染为带 `data-tooltip` 属性的元素，配合 CSS 或 JS 实现悬浮提示。 |
| `emoji` | 渲染为 `<span class="emoji">`，可调整大小。 |
| `theme-*` / `skin-*` | 不直接显示内容，用于控制渲染根容器类名。 |

## 5. 流式渲染策略

- 维护文本缓冲区，每次收到新块追加到缓冲区。
- 扫描缓冲区中完整的标签（单标签已闭合，双标签有对应的结束标签）。
- 对于完整标签，可立即执行上述流程，生成 DOM 片段并追加到容器。
- 对于未闭合的标签，保留在缓冲区，等待后续数据。
- 对于用户可见性，可在标签未完成时显示占位动画（如加载中），或先以纯文本形式展示标签内部内容（隐藏标签符号）。

### 5.1 组合容器解析策略

针对 `card` 与 `columns-*`，建议增加一层结构化拆分：

1. `card` 内部先查找 `card-head` 与 `card-body`
2. 若命中，则分别渲染头部区和内容区，避免外部标题与卡片标题重复
3. `columns-*` 内部先查找多个 `column`
4. 若命中，则按 `column` 显式分栏渲染，避免使用 `---` 时因空行、段落、列表导致布局错位
5. 若未命中显式子标签，再回退到旧语法保证兼容

## 6. 样式表

渲染器应提供一套默认 CSS，涵盖：

- 标题变体（`.title-filled`, `.title-bordered`, `.title-leftbar` 等）
- 表格样式（`.table-striped`, `.table-bordered`, `.table-compact`, `.table-hover`）
- 卡片、提示框、徽章、进度条、时间线、步骤条等。
- 图表容器默认尺寸（如高度 300px）。
- 文档主题类（如 `.ai-mdtag-theme-business`、`.ai-mdtag-theme-aurora`、`.ai-mdtag-theme-warm`）
- 文档皮肤类（如 `.ai-mdtag-skin-glass`、`.ai-mdtag-skin-paper`、`.ai-mdtag-skin-minimal`）
- 卡片头部、卡片正文、分栏容器、列块的首尾外边距修正
- 分栏容器的 `min-width: 0`、`align-items: stretch` 与列内元素 margin 重置

用户可通过 CSS 变量覆盖主题色、圆角、阴影等。

## 7. 安全性

- 内部 Markdown 解析使用安全库（如 markdown-it 配合 DOMPurify）。
- 自定义标签的属性值进行 HTML 转义。
- 图表数据中的文本内容转义。
- Mermaid 代码应经过清洗，防止 XSS。
- Mermaid 解析需支持 `{}` 判断节点，识别为菱形节点；同一判断节点的多条外连边应视为多个分支。
- Mermaid 连线应优先使用可见的纯色 SVG 路径，箭头尺寸保持克制，避免仅显示箭头不显示线条。

## 8. 扩展性

提供注册 API：
```javascript
renderer.registerTag('aifupan-mytag', (tagName, attributes, innerMarkdown) => {
  // 返回 HTMLElement
});
