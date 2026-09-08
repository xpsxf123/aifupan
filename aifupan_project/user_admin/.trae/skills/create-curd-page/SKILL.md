---
name: 'create-curd-page'
description: '能够从零创建整套 CURD 组件库，或基于现有组件快速生成管理表格页面。包含完整源码脚手架和使用规范。'
---

# CURD 组件系统 (脚手架与使用指南)

本技能包含两部分能力：

1.  **组件脚手架**: 在新项目中从零初始化整套 `src/components/Curd` 组件代码。
2.  **开发指南**: 基于该组件库快速开发标准化的增删改查页面。

## 1. 组件脚手架 (Component Scaffolding)

如果项目中缺少 `src/components/Curd` 目录，请按照 `references/component-scaffolding.md` 中的文件结构和源码进行初始化。

IMPORTANT: You MUST follow the `references/component-scaffolding.md` for the component source code.

## 2. 开发规范 (Development Guide)

在创建新功能模块时，请遵循 `references/usage-guide.md` 中的开发流程和配置说明。

IMPORTANT: You MUST follow the `references/usage-guide.md` for best practices and configuration details.

### 完整示例结构

```vue
<template>
  <Curd :api="api" :search-config="searchConfig" :table-columns="tableColumns" :form-config="formConfig">
    <!-- Custom slots... -->
  </Curd>
</template>
```
