---
name: "前端原型布局数据生成器"
description: "将需求/思维导图/原型图转为标准布局数据（Markdown内嵌json代码块）。用于后续渲染或预览。"
---

# 前端原型布局数据生成器

## 输入参数
- `requirement_document`：必填
- `mindmap_content`：选填
- `prototype_flow_content`：选填
- `data_format`：选填，默认`md_code`（可选：`md_code|json`）

## 分析与生成规则
- 页面结构解析、区域划分、元素类型识别
- 当 `data_format=md_code` 时：输出 Markdown 文档并在其中嵌入```json代码块
- 支持从 `web-页面结构`/`web-数据模型` 等 md 文档的 json 代码块解析生成布局

## 输出
- `layout_md`：符合模板的 Markdown 文档（内嵌```json），见 `templates/schema.md`

## 文档更新铁则（MANDATORY）
- 对文档的更新采用就地修改；版本记录写入根 `version_log.md`
