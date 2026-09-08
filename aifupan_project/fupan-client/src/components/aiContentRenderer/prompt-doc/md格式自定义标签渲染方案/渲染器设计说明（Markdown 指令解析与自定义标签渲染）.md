# 渲染器设计说明（Markdown 指令解析与自定义标签渲染）

## 一、目标

在不改变整体渲染管线思想的前提下，将“自定义标签声明”从 HTML-like 的 `<aifupan-xxx>`，升级为“Markdown 指令式”写法：

- 更接近 Markdown 文档观感
- 更利于流式输出时的可读性
- 降低 Markdown 渲染阶段对自定义标签的误处理概率

## 二、核心抽象

### 2.1 指令（Directive）

三类指令：

- BlockStart：`[!##]{...}`
- BlockEnd：`[##!]`
- Self：`[!##!]{...}`

### 2.2 统一参数层

将所有自定义能力统一抽象为：

- `element`：组件/能力类型（主路由）
- `type`：变体/子类型（分流）
- `attrs`：其余参数 map（透传给 renderer）

## 三、模块拆分建议（渲染器侧）

1. `directiveTokenizer`：从原始文本中按行扫描指令（支持流式增量）。
2. `directiveParser`：将 Token 流构建为 AST（处理嵌套/闭合）。
3. `rendererRegistry`：`(element, type) -> renderer` 映射表（可扩展）。
4. `dataBlockStore`：用于缓存 `data-block` 中的 JSON（visible=false 时必须落在此处）。
5. `htmlComposer`：占位、回填、纯 Markdown 降级策略。

## 四、数据块（data-block）能力设计

### 4.1 设计动机

允许在 AI 输出中携带“业务可消费的 JSON 数据”，并满足两类需求：

- **渲染型**：把 JSON 解析为 UI（卡片/表格/报告）
- **隐藏型**：页面不显示，但前端缓存可取出用于后续逻辑（如按钮控制、二次请求参数、埋点等）

### 4.2 参数约定（建议）

- `element="data-block"`（固定）
- `type`：渲染器类型（例：`structured` / `scriptQualityReport` / `quota` 等）
- `visible`：true/false（默认 false）
- `cacheKey`：缓存键（可选；不传则自动生成）

### 4.3 渲染策略

- visible=false：
  - 解析并缓存 JSON
  - 从最终 HTML 中移除该块（不占位，不展示）
- visible=true：
  - 解析 JSON
  - 交给 `type` 对应 renderer 生成 HTML
  - 不直接原样输出 JSON（避免污染阅读）

## 五、与 Markdown 渲染的交互策略

### 5.1 为什么必须“先指令预解析”

指令文本若直接走 Markdown-it，可能会被解释成链接、强调、列表等结构，造成：

- DOM 结构改变，导致后续回填难以定位
- 渲染时出现“多余的可点击链接/奇怪符号”

因此：必须先把指令识别并替换为可控占位，再进行 Markdown 渲染。

### 5.2 纯 Markdown（pureMd）降级

pureMd 模式建议策略：

- 移除所有指令本体
- 保留指令内部 Markdown 文本（容器的内部内容仍可渲染）
- 对 data-block 视 `visible/cacheKey` 决定：
  - visible=false：移除整个数据块正文
  - visible=true：可选（渲染 or 移除），取决于业务是否允许 pureMd 仍展示数据块内容

