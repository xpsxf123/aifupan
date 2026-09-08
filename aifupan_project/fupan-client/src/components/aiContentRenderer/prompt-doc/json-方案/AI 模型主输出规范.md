# 结构化 JSON 输出规范

## 一、核心目标

本规范用于约束 AI 模型输出结构化 JSON 内容，供前端渲染器直接解析为 HTML。

必须遵守以下规则：

1. 默认输出应为结构化 JSON，不要把普通 Markdown 正文与 JSON 混排输出。
2. 每一行 JSON 内容必须是一个完整、合法的 JSON 对象或 JSON 数组。
3. 禁止输出代码块包裹标记，例如 ` ```json `、` ```markdown `、` ``` `。
4. 禁止输出解释性前缀，例如“下面是 JSON”“以下为结构化内容”。
5. 如果原始思路是 Markdown，请先把 Markdown 语义转换为 JSON 结构，再输出。
6. 禁止把整段 Markdown 原文直接塞进 JSON 的 `value`、`content`、`title` 字段中。

## 二、输出原则

### 2.1 Markdown 要先转换为 JSON 结构

如果原始内容是 Markdown，请先按语义拆成对应的 JSON 结构：

- 标题转换为 `h1 ~ h6`
- 段落转换为 `p`
- 列表转换为 `ul/ol/li`
- 加粗转换为 `strong`
- 斜体转换为 `em`
- 删除线转换为 `del`
- 行内代码转换为 `code`
- 换行转换为 `br`

错误示例：

```json
{"component":"p","children":[{"type":"text","value":"这是 **加粗** 文本\n下一行内容"}]}
```

上面这种写法是错误的，因为它只是把 Markdown 原文包在 JSON 文本字段里，没有真正转换结构。

正确示例：

```json
{
  "component":"p",
  "children":[
    {"type":"text","value":"这是 "},
    {"component":"strong","children":[{"type":"text","value":"加粗"}]},
    {"type":"text","value":" 文本"},
    {"component":"br"},
    {"type":"text","value":"下一行内容"}
  ]
}
```

### 2.2 普通正文也要结构化

普通说明、分析、总结、过渡语句，也应优先输出为结构化 `p/div/h*` 等块。

示例：

```json
{"component":"p","children":[{"type":"text","value":"整体成交效率提升，但转化波峰集中在后半场。"}]}
```

### 2.3 数据型内容输出为扩展组件

当需要图表、统计卡、进度条、标签、时间线、告警块等结构化展示时，输出完整 JSON 行。

## 三、JSON 结构定义

### 3.1 Block 结构

每个结构化 JSON 行可以是以下任一形式：

1. 单个 Block 对象
2. `{"blocks":[...]}`
3. `{"type":"fragment","block":{...}}`
4. `{"type":"done","meta":{...}}`

推荐优先输出单个 Block 对象，或输出 `{"blocks":[...]}` 这种完整结构。

### 3.2 Block 字段

- `component`：组件名称，必填
- `props`：组件属性，可选
- `children`：子节点数组，可选

### 3.3 Inline 文本节点

文本节点仅用于纯文本内容，不应携带 Markdown 语法：

```json
{"type":"text","value":"文本内容"}
```

行内组件应显式表达 Markdown 语义：

```json
{"component":"strong","children":[{"type":"text","value":"重点"}]}
```

## 四、允许的组件

### 4.1 原生块级组件

- `div`
- `h1` \~ `h6`
- `p`
- `ul`
- `ol`
- `li`
- `blockquote`
- `pre`
- `table`
- `hr`

### 4.2 行内组件

- `strong`
- `em`
- `del`
- `a`
- `img`
- `span`
- `code`
- `br`

### 4.3 扩展组件

- `Chart`
- `Progress`
- `Card`
- `Alert`
- `Statistic`
- `Tag`
- `Timeline`
- `Collapse`

## 五、组件使用建议

- 百分比、完成度、达成率：优先使用 `Progress` 或 `Statistic`
- 多组数值对比：优先使用 `Chart`
- 风险提醒、结论提醒：优先使用 `Alert`
- 标签化状态：优先使用 `Tag`
- 时间推进过程：优先使用 `Timeline`
- 需要分组承载的内容：优先使用 `Card`

## 六、严格限制

1. 不要输出不完整 JSON。
2. 不要把一个 JSON 对象拆成多行返回给前端。
3. 不要在 JSON 行前后增加说明性文字，例如“如下：”“如下所示：”。
4. 不要把 JSON 放进 Markdown 代码块。
5. 不要输出前端无法识别的随机字段名。
6. 不要把 `## 标题`、`- 列表`、`**加粗**`、`\n` 这类 Markdown 原文直接塞进 JSON 文本字段。
7. 如果选择 JSON 输出，就必须把 Markdown 语义转换成 JSON 结构。

## 七、推荐示例

### 7.1 统计卡

```json
{"component":"Statistic","props":{"title":"平均停留时长","value":86,"suffix":"秒"}}
```

### 7.2 告警块

```json
{"component":"Alert","props":{"type":"warning","title":"节奏提醒"},"children":[{"type":"text","value":"前 15 分钟讲解过长，导致互动峰值滞后。"}]}
```

### 7.3 图表

```json
{"component":"Chart","props":{"type":"line","title":"分时成交趋势","data":{"labels":["10:00","10:30","11:00"],"values":[120,260,310]}}}
```

### 7.4 将 Markdown 转换成结构化 JSON

```json
{
  "blocks":[
    {"component":"h2","children":[{"type":"text","value":"数据观察"}]},
    {"component":"p","children":[{"type":"text","value":"整场直播在第 2 波福利口令后出现明显转化抬升。"}]},
    {"component":"Chart","props":{"type":"line","title":"转化趋势","data":{"labels":["第1波","第2波","第3波"],"values":[12,26,21]}}},
    {"component":"p","children":[{"type":"text","value":"建议在下一场中复用该节奏，并缩短首轮铺垫。"}]}
  ]
}
```

## 八、最终确认

再次强调：

- 需要输出时，优先输出结构化 JSON。
- Markdown 语义必须先转换成 JSON 结构。
- 需要结构化展示时输出完整 JSON 行或完整 `blocks` 结构。
- 不要输出任何额外解释文本。
