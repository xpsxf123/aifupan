---
name: 'auth-implementation'
description: '实现基于RBAC和通配符的权限控制系统。包含权限码转换、验证逻辑、指令和Hooks的完整脚手架。'
---

# 权限控制系统 (Auth Implementation)

本技能提供了一套完整的、生产级可用的 RBAC 权限管理系统。它基于 "平台:页面:操作" 的权限码设计，支持通配符 (`*`) 匹配。

## 1. 核心能力

- **多级权限码**: 支持 `admin:user:add` 格式。
- **通配符匹配**: 支持 `admin:*` 或 `admin:user:*` 等复杂匹配。
- **全栈集成**: 提供 Vue 指令、Hooks、Pinia Store 和路由守卫。

## 2. 快速脚手架 (Scaffolding)

在新项目中，请按照以下结构创建文件。

### 2.1 目录结构

```
src/auth/
├── config.js       // 权限配置文件
├── core.js         // 核心逻辑 (Converter & Validator)
├── store.js        // Pinia Store
├── hooks.js        // Composition API
├── directives.js   // Vue Directives
├── guard.js        // Router Guard
└── index.js        // Entry
```

### 2.2 核心代码实现

IMPORTANT: You MUST follow the `references/implementation-guide.md` for the detailed code implementation of each file.

## 3. 使用指南 (Usage)

### 3.1 注册插件

在 `main.js` 中：

```javascript
import { auth } from '@/auth/directives'
app.directive('auth', auth)
import '@/auth/guard' // 激活路由守卫
```

### 3.2 组件使用

```html
<!-- 按钮控制 -->
<button v-auth="'admin:user:add'">添加用户</button>

<!-- 脚本控制 -->
<script setup>
  import { usePermission } from '@/auth'
  const { hasPermission } = usePermission()

  if (hasPermission('admin:user:edit')) {
    // logic
  }
</script>
```

### 3.3 路由配置

```javascript
{
  path: '/user',
  meta: { permission: 'admin:user:MENU' }
}
```
