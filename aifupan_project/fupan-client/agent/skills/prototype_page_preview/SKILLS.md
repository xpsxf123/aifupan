---
name: "前端原型页面预览器"
description: "从布局JSON生成可预览HTML原型。需要可视化快速校验页面布局与交互时调用。"
---

# 前端原型页面预览器

## 输入参数
- `layout_data`：必填，JSON或文本
- `schema_sources`：选填，数组，支持传入 `web-页面结构.json`、`web-数据模型.json`、`web-表单验证.json`、`web-接口依赖.json` 的文本或路径
- `page_hint`：选填，页面名称或类型提示（如 `列表页`、`表单页`、`详情页`、`看板页`），用于渲染策略选择
- `autofill`：选填，默认`true`；开启后根据 schema 自动补齐字段、按钮与列
- `mock_strategy`：选填，`sample|random`，默认`sample`
- `mock_records`：选填，默认`20`，用于列表页生成记录数
- `output_format`：选填，默认`html`
- `theme`：选填，默认`light`
- `interactive_mode`：选填，默认`mock`
- `prototype_css_href`：选填，默认`styles/prototype.css`，用于引入外部原型样式文件

> 兼容说明：`schema_sources` 同时支持 .md 文档（内嵌```json代码块）与 .json 文件路径，内部自动解析

## 渲染规则
- 布局/区域/元素渲染、交互模拟、样式规范（light/dark）
- 使用 Element Plus 的 CDN 引入方式加载组件与样式，原型基于 Element Plus 组件结构渲染表单、表格、按钮、分页等；可通过 `prototype_css_href` 叠加工程自定义样式

> 采用 element-plus 的 CDN 引入方式，采用 Element 的样式为基础来实现

### 字段映射策略
- 表单区：从 `web-页面结构.json` 的 `包含元素` 中提取 `元素类型=表单/筛选`，结合 `web-数据模型.json` 字段约束与 `web-表单验证.json` 规则生成 `el-form`
- 列表区：若 `页面类型=列表页` 或推断为列表，则从 `web-数据模型.json` 中选取核心字段生成 `el-table` 列；分页默认开启
- 详情/看板：生成 `el-card` 指标卡与占位图；依据 `page_hint` 渲染

### Mock 数据规则
- `sample`：依据字段类型生成可读示例（如姓名、手机号、组织、数值区间）
- `random`：在合理范围内生成随机值；记录数由 `mock_records` 控制
- 输出同时附带 `mock_json`，并在 HTML 内注入 `window.__MOCK_DATA__`

## 输出
- `prototype_html`：完整HTML字符串
- `mock_json`：与页面对应的示例数据 JSON

## 文档更新铁则（MANDATORY）
- 就地更新目标原型文档；版本记录写入根 `version_log.md`

## 说明
- 本技能在生成 HTML 模板时会通过 CDN 注入 Element Plus：
  - https://unpkg.com/element-plus/dist/index.css
  - https://unpkg.com/element-plus
- 并在 `<head>` 中追加 `<link rel="stylesheet" href="{prototype_css_href}">` 以加载统一原型样式文件
- 模板会注入占位：`{{PAGE_TITLE}}`、`{{PAGE_TYPE}}`、`{{PAGE_SCHEMA}}`、`{{MOCK_DATA}}` 并由内置渲染器根据 schema 决定渲染表单/表格/卡片等
