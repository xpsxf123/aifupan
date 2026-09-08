---
name: "前端需求文档生成器"
description: "根据拆分好的需求文档生成金字塔结构的前端需求文档或JSON。输入PRD/BRD/FRD/DRD合集时调用。"
---

# 前端需求文档生成器（金字塔结构）

## 技能目的
将已拆分的需求文档（PRD/BRD/FRD/DRD整合）转换为从上至下的金字塔结构前端需求文档，或输出同构的JSON。

## 触发时机
- 需求拆分完成，需要输出前端需求总文档（Markdown或JSON）时。

## 输入参数
- `requirement_document`：必填，文本。PRD/BRD/FRD/DRD整合内容。
- `document_depth`：选填，文本。可选：`full` | `outline` | `core`，默认`full`。
- `output_format`：选填，文本。可选：`markdown` | `json`，默认`markdown`。

## 输出
- `front_end_doc_md`：当`output_format=markdown`时返回的Markdown内容（遵循模板）。
- `front_end_doc_json`：当`output_format=json`时返回的JSON内容（遵循schema）。

## 分析与组装逻辑
1. 需求解析：提取项目信息、模块、页面、功能、数据模型、接口、交互、权限。
2. 模块归类：依据PRD的模块与FRD页面归属建立层级。
3. 页面需求提取：界面元素、交互行为、数据展示、表单验证、权限、接口依赖。
4. 组件需求识别：公共组件与业务组件，Props与Events需求。
5. 数据需求映射：前端模型、展示规则、枚举映射、格式化规则。
6. 接口需求映射：清单、参数来源、响应处理与错误处理。
7. 交互流程需求：主流程、分支、异常与反馈提示。
8. 文档组装：按金字塔结构输出所选深度的内容。

## 模板
- Markdown模板：`templates/template.md`
- JSON模板：`templates/schema.json`

## 使用说明（示例）
1. 输入：`requirement_document`（整合文本），可选 `document_depth`，`output_format`。
2. 返回：按`output_format`输出文档；保存到遵循“文档拆分生成规则”的目标目录。

## 文档更新铁则（MANDATORY）
- 前端需求文档的增补/修订必须“直接修改原文档（Markdown或JSON）”。
- 在Markdown文档“版本变更记录”登记摘要与日期；JSON文档通过项目根 `version_log.md` 记录本次变更。
