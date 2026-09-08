# Markdown 指令式自定义标签输出规范（方案草案）

## 一、目标

将原有的 `<aifupan-xxx>...</aifupan-xxx>` / `<aifupan-xxx />` 形式，升级为“更接近 Markdown 的指令语法”，降低流式输出时的“HTML 感”，并减少 Markdown 渲染阶段的显示干扰。

## 二、核心约束

1. 输出必须是纯文本流，以标准 Markdown 为主体。
2. 自定义标签用于“声明渲染意图/容器边界/数据块”，不应替代 Markdown 的语义表达。
3. 推荐仅在“行首”使用自定义标签指令（减少误匹配），指令后换行再写 Markdown 内容块。
4. 若用户要求纯 Markdown（降级/纯文本），渲染器应支持“移除所有指令，仅保留 Markdown”。

## 三、指令语法定义

### 3.1 开始标签（Block Start）

语法：

`[!##]{...参数...}`

示例：

```text
[!##]{element="card",type="default"}
### 核心结论
- 互动偏弱
- 转化链路不顺
[##!]
```

### 3.2 结束标签（Block End）

语法：

`[##!]`

说明：

- 仅表示“闭合最近的一个未闭合开始标签”。
- 推荐独占一行（行首 + 单独一行）。

### 3.3 单标签（Self Closing）

语法：

`[!##!]{...参数...}`

示例：

```text
[!##!]{element="title",level=1,variant="filled"}
# 项目复盘报告
```

## 四、参数（属性）格式

### 4.1 语法

`{key=value, key2="value2", key3='value3'}`

约束：

- 以逗号分隔键值对，允许空格。
- value 支持：数字 / 布尔 / 字符串（单引号或双引号）。
- 不支持在 value 内直接出现未配对的 `}`。

### 4.2 固定保留键（建议）

- `element`（必填）：声明渲染组件类型（等价于旧方案中 `aifupan-` 后的基础类型）。
- `type`（可选）：组件变体/渲染类型（等价于旧方案中 `aifupan-xxx-variant` 或 `type="..."`）。
- `theme` / `skin`（可选）：全局主题/皮肤声明（也可用单标签声明）。
- `visible`（可选）：用于数据块类指令，控制是否渲染（见数据块设计）。
- `cacheKey`（可选）：用于数据块类指令，声明缓存键（见数据块设计）。

### 4.3 element 建议枚举（示例）

- `title` / `card` / `card-head` / `card-body`
- `callout` / `note` / `quote-modern`
- `columns-2` / `column`
- `table` / `echarts-pie` / `echarts-line` / `mermaid`
- `data-block`（结构化数据块，见数据块设计）

## 五、与标准 Markdown 的冲突分析与取舍

### 5.1 原始设想 `[!##](...)` 的冲突点

`[文本](链接)` 在标准 Markdown 中是“行内链接”语法：

- `[!##](element='title')` 会被 Markdown-it 识别为 `<a href="element='title'">!##</a>`。
- 这会导致：未接入自定义解析器时页面出现“奇怪的链接”；且在流式渲染中链接结构可能被浏览器默认样式影响。

因此：不建议使用 `()` 作为参数容器。

### 5.2 推荐方案 `[!##]{...}` 的兼容性

- `[!##]{...}` 不符合标准 Markdown link 语法，Markdown-it 会把它当作普通文本，不会自动变成链接。
- 更适合“先做指令预解析 → 再做 Markdown 渲染 → 再回填组件 HTML”的管线。

## 六、与旧方案的兼容映射（设计目标）

本方案设计时要求“兼容旧语义”，建议提供映射规则（渲染器侧实现，本文仅定义约定）：

- `<aifupan-card>...</aifupan-card>` → `[!##]{element="card"} ... [##!]`
- `<aifupan-callout type="info">...</aifupan-callout>` → `[!##]{element="callout",type="info"} ... [##!]`
- `<aifupan-title-1-filled /> # 标题` → `[!##!]{element="title",level=1,variant="filled"} # 标题`

说明：

- 兼容并不要求 1:1 文本还原，而是保证渲染结果一致。
- 渲染器可支持“旧标签 + 新指令”混用（过渡期）。

