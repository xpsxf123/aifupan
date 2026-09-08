---
name: "接口依赖分析器"
description: "从FRD与API文档提取接口清单、参数与响应格式，输出Markdown（内嵌json代码块）。用于前端直接消费。"
---

# 接口依赖分析器

## 技能目的
识别前端需要调用的后端接口，包括用途、方法、路径、参数与响应结构，生成接口清单Markdown（内嵌json代码块）。

## 输入参数
- `frd_document`：必填，文本。FRD文档完整内容。
- `api_document`：选填，文本。后端API文档，用于补充细节。

## 输出
- `api_dependencies_md`：接口依赖Markdown（内嵌```json代码块，详见模板）。

## 分析逻辑
1. 扫描FRD的功能点，识别相关接口。
2. 提取HTTP方法与路径、请求参数与响应结构。
3. 记录接口被调用的页面与关联数据实体。

## 模板
参见 `templates/schema.md`。
