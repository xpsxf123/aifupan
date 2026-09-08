## 1. 目的

本文件用于描述 `aifupan-data-block` 标签的**基础语法**与**解析/缓存/渲染规则**。

关于“指定渲染（visible=true）”场景下各 `type` 对应的**JSON 字段规范**，统一放在目录：

- [data-block/README.md](file:///c:/Users/JYJY/Desktop/fupan-client/src/components/aiContentRenderer/prompt-doc/data-block/README.md)

## 2. 标签语法（必须双标签）

~~~text
<aifupan-data-block visible="true" type="structured" cacheKey="example">
{ "hello": "world" }
</aifupan-data-block>
~~~

说明：

- data-block 内部推荐输出“纯 JSON”（不带 ` ```json `），避免提示词要求“严格 JSON”时产生冲突。
- 如需在 Markdown 场景增强可读性，也允许使用 ` ```json ... ``` ` fenced code block；前端会优先提取 fenced 内容。
- 兼容写法：允许将开始标签写成 `<aifupan-data-block ... />`（随后仍需以 `</aifupan-data-block>` 结束），前端会在解析前自动归一化为 `<aifupan-data-block ...>`。

## 3. 属性约定

| 属性 | 是否必填 | 默认值 | 说明 |
|------|----------|--------|------|
| `visible` | 否 | `false` | 是否渲染该数据块。`false` 时仅缓存并从页面移除可见内容 |
| `type` | 条件必填 | `structured`（仅当 `visible=true` 且未传时） | 渲染类型（路由到不同 JSON 渲染器） |
| `cacheKey` | 否 | 自动生成 | 缓存键，用于前端后续读取该数据块 |
| `contentType` | 否 | `text` | 内容类型：`json \| html \| text`。等价于使用 ` ```json / ```html / ```text ` fenced code block |
| `resource-data` | 否 | `false` | 资源数据标记（布尔属性）。用于后端/其它服务按标签截取资源数据；前端仅负责透传与缓存 |

重要规则：

- 当 `visible="false"`（或不传 `visible`）时：允许不传 `type`（因为不会发生 UI 渲染，仅做缓存/过滤）
- 当 `visible="true"` 时：建议传 `type`；不传则前端默认按 `structured` 处理
 - `contentType` 的确定优先级：fenced code block 语言（若存在） > `contentType` 属性 > 默认值（一般为 `text`；但当 `type` 要求 JSON 时会默认按 `json`）

## 4. 两种使用方式

### 4.1 隐藏但缓存（visible=false）

适用：需要把 JSON 传给前端逻辑使用，但不希望用户看到。

~~~text
<aifupan-data-block visible="false" cacheKey="quota">
{ "limit": 10, "used": 3 }
</aifupan-data-block>
~~~

资源数据标记示例（仅用于标记与截取，前端会缓存该块）：

~~~text
<aifupan-data-block resource-data visible="false">
"摸鱼话术": <font color="red">2</font>处
"摸鱼话术2": <font color="red">2</font>处
</aifupan-data-block>
~~~

### 4.2 指定渲染（visible=true + type）

适用：需要把 JSON 渲染成结构化 UI。

~~~text
<aifupan-data-block visible="true" type="scriptQualityReport" cacheKey="scriptQualityReport">
{ "summary": { "crashCount": 0 }, "details": { "crash": [] } }
</aifupan-data-block>
~~~

## 5. type 枚举（前端已注册）

各 type 的详细 JSON 字段规范见对应链接：

| type | 说明 | JSON 字段规范 |
|------|------|---------------|
| `structured` | 通用 JSON 数据块 | [data-block/structured.md](file:///c:/Users/JYJY/Desktop/fupan-client/src/components/aiContentRenderer/prompt-doc/data-block/structured.md) |
| `scriptQualityReport` | 话术质检报告数据 | [data-block/scriptQualityReport.md](file:///c:/Users/JYJY/Desktop/fupan-client/src/components/aiContentRenderer/prompt-doc/data-block/scriptQualityReport.md) |

## 6. 前端解析与缓存规则（实现口径）

实现位置：

- [dataBlockPreprocessor.js](file:///c:/Users/JYJY/Desktop/fupan-client/src/components/aiContentRenderer/renderers/dataBlockPreprocessor.js)
- [dataBlockStore.js](file:///c:/Users/JYJY/Desktop/fupan-client/src/components/aiContentRenderer/dataBlockStore.js)
- [registry.js](file:///c:/Users/JYJY/Desktop/fupan-client/src/components/aiContentRenderer/dataBlocks/registry.js)

解析要点：

- 提取：从原始输出文本中提取所有 `<aifupan-data-block ...>...</aifupan-data-block>`
- 属性：
  - `visible` 仅识别 `"true"/"1"` 为真，其余均为 false
  - `cacheKey` 未提供则自动生成（包含时间戳 + 序号）
  - `type`：显式提供优先；`visible=true` 未提供则默认 `'structured'`
- JSON：
  - 若内部存在 ` ```json / ```html / ```text ` fenced code block，则按其 language 决定 `contentType` 并提取 fenced 内容
  - 若不存在 fenced code block，则按 `contentType` 属性解析；未提供则默认按 `text`
  - 当 `contentType=json` 时：解析为对象（失败则 `data=null`，仍保留 `raw`）
  - 当 `contentType=html/text` 时：不做结构化解析，`data` 直接存储字符串
- 可见性：
  - `visible=false`：从最终 HTML 中移除该块，不展示
  - `visible=true`：替换为占位节点并按 `type` 动态挂载对应 Vue 组件渲染（若 type 对 `contentType` 有要求但不满足，则不渲染，仅缓存）
- 缓存：缓存的是整个 block 对象（`cacheKey/type/visible/raw/data/contentType/resourceData`）
