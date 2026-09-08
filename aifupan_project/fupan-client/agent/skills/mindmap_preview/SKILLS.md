---
name: "思维导图预览器"
description: "从Mermaid思维导图代码生成可预览的自包含HTML页面。需要快速图像化预览时调用。"
---

# web-思维导图预览器

## 输入参数
- `mindmap_mermaid`：必填，文本（包含 ```mermaid mindmap ...``` 或纯 mindmap 语法）
- `theme`：选填，默认`default`（可选：default/neutral/dark/forest）
- `fit`：选填，默认`true`（是否自适应容器）
- `title`：选填，页面标题
- `height_mode`：选填，默认`viewport`（viewport/fixed）。viewport：容器高度自动填满编辑器/浏览器视口；fixed：使用`fixed_height`像素高度
- `fixed_height`：选填，`height_mode=fixed`时的高度（像素，默认800）

## 输出
- `preview_html`：可直接在浏览器打开的自包含HTML字符串

## 生成规则
- 通过CDN加载Mermaid（生产可接入内网镜像），在页面`<div id="mindmap">`内渲染
- 自动清洗包裹的代码块标记（```mermaid ...```）
- 支持 `theme`、`fit`、`height_mode` 配置；`viewport` 模式下容器高度随窗口/编辑器尺寸变化自动适配；渲染失败时显示错误占位提示

## 模板
- 参见 `templates/index.html`，以 `{{TITLE}}`、`{{THEME}}`、`{{FIT}}`、`{{MERMAID_CODE}}` 为占位符
  - 自适应参数：`{{HEIGHT_MODE}}`、`{{FIXED_HEIGHT}}`

## 文档更新铁则（MANDATORY）
- 就地更新目标文档（若持久化HTML），并在根 `version_log.md` 记录变更摘要
