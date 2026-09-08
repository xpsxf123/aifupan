# 使用指南

- 技能名称：前端需求文档生成器
- 输入：
  - `requirement_document`（必填，PRD/BRD/FRD/DRD整合文本）
  - `document_depth`（选填，full/outline/core，默认full）
  - `output_format`（选填，markdown/json，默认markdown）
- 输出：
  - `front_end_doc_md`（当选择markdown时）或 `front_end_doc_json`（当选择json时）

步骤：
- 传入需求合集与输出参数；根据`output_format`选择模板（`templates/template.md`或`templates/schema.json`）组装内容。
- 将生成结果保存到遵循“需求-文档拆分生成规则”的前端文档目录结构中。

