# 混合 JSON 输出二次校验规则

## 一、适用范围

本规则用于校验 AI 模型输出的结构化 JSON 内容。

校验器需要重点防止一种错误情况：把 Markdown 原文直接包进 JSON 的文本字段，而没有转换成结构化节点。

## 二、核心原则

1. 输出应优先为完整 JSON。
2. 若出现 Markdown 原文，需要判断它是否已经被正确转换成结构化 JSON。
3. 如果只是把 Markdown 字符串塞进 JSON 文本字段，则视为不合格结构。
4. 不完整 JSON 仍然不允许直接透传到前端。
5. 对于不完整 JSON 行，应优先提取其中的 `value`、`title`、`content` 等文案字段作为预览文本。

## 三、逐行识别规则

当某一行满足以下任一条件时，视为“疑似 JSON 行”：

- 以 `{` 或 `[` 开头
- 以 `data:{` 或 `data: {` 开头
- 包含 `"type"`、`"component"`、`"blocks"` 等明显结构化字段
- 包含 `":` 这类典型 JSON 键值格式，且同时伴随 `{`、`}`、`[`、`]` 等结构符号

如果一行不满足上述条件，则直接按普通 Markdown 行透传。

## 四、完整 JSON 校验规则

对疑似 JSON 行，按以下顺序处理：

1. 优先尝试对整行做 `JSON.parse`
2. 如果整行失败，可尝试截取其中完整的 `{...}` 或 `[...]` 片段再解析
3. 只要成功解析并且结构符合本规范，即视为有效 JSON 行

允许的结构包括：

1. 单个 Block 对象
2. `{"blocks":[...]}`
3. `{"type":"fragment","block":{...}}`
4. `{"type":"done","meta":{...}}`

## 五、不完整 JSON 的处理

如果某一段内容明显是 JSON，但当前还无法成功解析：

1. 不要把原始 JSON 协议文本输出到前端
2. 不要输出 `{`、`}`、`"type"`、`"component"`、`"index"`、`"total"` 等协议噪音
3. 应从字符串中提取以下字段的可见文本作为预览：
   - `value`
   - `title`
   - `content`
   - `text`
   - `label`
   - `name`
   - `description`
4. 如果提取不到有效文案，则该段内容暂不展示
5. 当后续内容补全并能成功解析时，应使用完整 JSON 渲染结果替换预览结果

## 六、结构化字段校验

### 6.1 Block 基础校验

- `component` 必须为字符串
- `props` 若存在，必须为对象
- `children` 若存在，必须为数组

### 6.2 特殊组件校验

- `Progress.props.percentage` 必须是数值，超出 0-100 时可自动钳位
- `Chart.props.type` 推荐限制为 `line`、`bar`、`pie`
- `table.props.headers` 必须为数组
- `table.props.rows` 必须为二维数组
- `pre.props.content` 必须为字符串

### 6.3 行内节点校验

- 文本节点格式：`{"type":"text","value":"..."}`
- 行内组件允许：`strong`、`em`、`del`、`a`、`img`、`span`、`code`、`br`、`Tag`

### 6.4 Markdown 转 JSON 校验

如果 JSON 文本字段中出现以下 Markdown 痕迹，应视为待修正内容：

- `# `
- `## `
- `- `
- `* `
- `**加粗**`
- `*斜体*`
- `` `代码` ``
- 明显依赖换行语义的长文本

校验器应给出如下修正建议：

1. 标题改为 `h1 ~ h6`
2. 列表改为 `ul/ol/li`
3. 加粗改为 `strong`
4. 斜体改为 `em`
5. 换行改为 `br`
6. 段落改为多个 `p`

## 七、校验失败时的修正策略

当 JSON 校验失败时，按以下优先级修正：

1. 如果能提取完整 JSON 子串，则仅保留该 JSON 子串
2. 如果无法提取完整 JSON，但能提取正文文案，则输出正文文案
3. 如果既不是完整 JSON，也没有正文文案，则丢弃该 JSON 协议片段
4. 不允许把半截 JSON 原样返回给前端页面
5. 如果发现 JSON 内嵌 Markdown 原文，则应提示模型将 Markdown 语义改写为 JSON 结构，而不是继续包裹字符串

## 八、最终输出要求

校验后的最终内容必须满足：

1. 完整 JSON 保持为合法 JSON
2. JSON 内部文本字段不应承载未经转换的 Markdown 原文
3. 不完整 JSON 不直接暴露给前端
4. Markdown 语义应转换为 JSON 结构节点

## 九、推荐校验结果格式

```json
{
  "valid": true,
  "hasJsonLine": true,
  "errors": [],
  "warnings": [],
  "previewTextExtracted": true
}
```

## 十、最终确认

请始终记住：

- 目标不只是校验 JSON 完整性，还要校验 Markdown 是否被正确转换成 JSON 结构
- 校验单位可以是单行 JSON，也可以是完整 `blocks` 结构
- 目标是避免 JSON 协议噪音泄漏到前端页面
- 目标是避免“Markdown 文本被 JSON 字符串包裹”这种伪结构化输出
