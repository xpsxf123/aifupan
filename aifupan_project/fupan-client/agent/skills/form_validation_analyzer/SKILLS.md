---
name: "表单验证分析器"
description: "从FRD与DRD提取表单字段验证规则，输出Markdown（内嵌json代码块）。用于前端直接生成校验逻辑。"
---

# 表单验证分析器

## 技能目的
识别表单元素与字段约束，生成字段验证规则与错误提示，输出验证规则Markdown（内嵌json代码块），供表单渲染/校验技能使用。

## 输入参数
- `frd_document`：必填，文本。FRD文档完整内容。
- `drd_document`：必填，文本。DRD文档完整内容。
- `form_id`：选填，文本。指定分析的表单ID。

## 输出
- `validation_rules_md`：验证规则Markdown（内嵌```json代码块，详见模板）。

## 分析逻辑
1. 从FRD识别表单元素集合。
2. 从DRD获取字段约束（必填、长度、格式、范围、唯一等）。
3. 生成验证时机与规则、错误提示与复杂性要求。

## 模板
参见 `templates/schema.md`。
