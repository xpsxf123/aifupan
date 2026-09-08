---
name: "布局结构分析器"
description: "从FRD提取页面整体与区域布局要求，输出Markdown（内嵌json代码块）。用于布局渲染。"
---

# 布局结构分析器

## 技能目的
识别页面的整体与局部布局定义，包括栅格系统、区域顺序、间距与样式要求，生成布局结构Markdown（内嵌json代码块）。

## 输入参数
- `frd_document`：必填，文本。FRD文档完整内容。

## 输出
- `layout_structure_md`：布局结构Markdown（内嵌```json代码块，详见模板）。

## 分析逻辑
1. 从FRD识别整体布局结构与主体区域布局。
2. 抽取栅格系统的列分布与对齐样式。
3. 识别卡片、描述列表等容器与显示项。

## 模板
参见 `templates/schema.md`。
