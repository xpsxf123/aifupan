# Markdown 指令式自定义标签：输出内容二次校验规则（方案草案）

## 一、适用范围

用于校验 AI 输出文本中包含的 `[!##]` / `[!##!]` / `[##!]` 指令是否满足解析与渲染的要求。

## 二、指令语法校验

### 2.1 基本格式

- BlockStart：`[!##]{...}`
- BlockEnd：`[##!]`
- Self：`[!##!]{...}`

### 2.2 参数格式

- 参数必须使用 `{}` 包裹，内部为 `key=value` 对。
- key：建议为字母/数字/下划线/连字符组合（不含空格）。
- value：支持数字/布尔/字符串；字符串必须用单引号或双引号包裹（当包含空格/逗号/等号时必须加引号）。

### 2.3 必填字段

- BlockStart/Self 必须包含 `element`。

### 2.4 配对与嵌套

- BlockStart 与 BlockEnd 必须正确配对，不允许交叉闭合。
- 建议限制最大嵌套层级（如 4 层），避免输出过度复杂。

## 三、element 白名单（建议）

基础白名单（示例）：

- 容器类：`card`/`card-head`/`card-body`/`columns-2`/`column`/`callout`/`note`
- 文本类：`title`/`highlight`/`font`/`badge`
- 可视化类：`echarts-pie`/`echarts-line`/`mermaid`
- 数据类：`data-block`

不在白名单内的 element 输出应给出 warning（兼容期可降级为纯 Markdown）。

## 四、data-block 特殊校验

当 `element="data-block"` 时：

 - 当 `visible=true` 时建议提供 `type`（用于路由到对应 JSON 渲染器）；不提供则默认按 `structured` 处理；当 `visible=false` 时允许不传 `type`
- `visible` 必须为布尔或 0/1（推荐 true/false）
- 内部必须存在可解析为 JSON 的正文（推荐放在 ```json fenced code block 内）

## 五、自动修正建议

- 若缺失 `visible`，默认 `false`
- 若缺失 `cacheKey`，由渲染器自动生成
- 若出现无法配对的 BlockEnd，可直接忽略该结束指令并记录 warning
