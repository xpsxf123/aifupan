# 路由配置说明文档

## 1. 概述

本项目的路由配置采用**集中式管理**，核心配置文件位于 `src/config/menu.js`。该文件不仅定义了 Vue Router 的路由表，还同时作为侧边栏菜单（Sidebar）的数据源。

通过修改此文件，您可以：

- 添加/删除页面路由
- 配置菜单名称、图标
- 设置页面访问权限
- 定义路由层级结构

## 2. 配置格式

配置文件导出一个名为 `menuConfig` 的数组，数组中的每个对象代表一个路由节点。

```javascript
// src/config/menu.js
export const menuConfig = [
  {
    path: '/dashboard',      // 路由路径
    name: 'Dashboard',       // 路由名称（必须唯一）
    // component: 'Layout',  // 可省略，默认使用模板组件
    redirect: '/dashboard/index', // 重定向地址
    meta: {                  // 路由元信息
      title: '首页',          // 菜单标题
      icon: 'House',         // 菜单图标
      isNav: true,           // 是否显示在导航栏
      isWhite: false         // 是否为白名单页面
    },
    children: [ ... ]        // 子路由
  }
]
```

## 3. 参数说明

| 参数字段 | 类型 | 必填 | 说明 | 示例 |
| :-- | :-- | :-- | :-- | :-- |
| `path` | String | 是 | 路由访问路径。一级路由需带 `/`，子路由可不带。 | `'/user'` 或 `'list'` |
| `name` | String | 是 | 路由名称，用于编程式导航和缓存，**必须全局唯一**且建议使用大驼峰命名。 | `'UserList'` |
| `component` | String | 否 | 组件映射标识。详情见下文 [组件映射规则](#4-组件映射规则)。 | `'Layout'` 或 `'views/User/List'` |
| `redirect` | String | 否 | 访问该路径时自动重定向到的地址。 | `'/user/list'` |
| `meta` | Object | 否 | 路由元信息对象，用于配置菜单和权限。 | `{ title: '用户', isNav: true }` |
| `children` | Array | 否 | 子路由数组，结构同父级。 | `[{ path: 'list', ... }]` |

### 3.1 Meta 元信息对象

| Meta 字段 | 类型 | 默认值 | 说明 |
| :-- | :-- | :-- | :-- |
| `title` | String | - | 菜单栏和面包屑中显示的标题文字。 |
| `icon` | String | - | Element Plus 图标名称（如 `User`, `Setting`）。 |
| `roles` | Array | - | 权限控制。仅数组中包含的角色可访问该路由。不填代表所有登录用户可见。 |
| `isNav` | Boolean | `false` | **导航显示控制**。如果为 `true`，则显示在侧边栏菜单中；否则默认隐藏。 |
| `isWhite` | Boolean | `false` | **白名单配置**。如果为 `true`，则直接跳过路由守卫的验证（如Token、权限检查），直接进入页面。 |
| `useTemplate` | Boolean | `true` | **模板组件控制**。配合 `component` 字段使用。详情见下文。 |

## 4. 组件映射规则与扩展功能

### 4.1 默认模板组件

为了简化配置，系统支持**默认使用模板组件 (Layout)**。

- 如果 `component` 字段**为空**（undefined/null）：
  - 系统默认将其设置为 `'Layout'`。
  - **例外**：如果配置了 `meta: { useTemplate: false }`，则不会自动设置为 `'Layout'`。此时您必须手动提供有效的 `component` 值，否则可能导致路由加载失败。

### 4.2 手动指定组件

- **`'Layout'`**: 显式指定使用布局组件。
- **普通组件字符串 (如 `'views/User/List'`)**:
  - 对应组件：`src/views/User/List.vue`
  - 规则：系统会自动在字符串前补全 `../` 并添加 `.vue` 后缀进行匹配。

## 5. 新增页面示例

假设需要新增一个“订单管理”模块，包含“订单列表”页面。

1.  **创建组件文件**：
    - `src/views/Order/List.vue`

2.  **添加路由配置** (`src/config/menu.js`)：

```javascript
{
  path: '/order',
  name: 'Order',
  // component: 'Layout', // 可省略，默认自动加载 Layout
  meta: {
    title: '订单管理',
    icon: 'Tickets',
    isNav: true // 必须配置为 true 才会显示在菜单中
  },
  children: [
    {
      path: 'list',
      name: 'OrderList',
      component: 'views/Order/List',
      meta: {
        title: '订单列表',
        icon: 'List',
        isNav: true
      }
    }
  ]
}
```

## 6. 注意事项

1.  **isNav 默认值**：请注意 `isNav` 默认为 `false`（不显示）。如果您新增了页面但侧边栏看不见，请检查是否配置了 `isNav: true`。
2.  **isWhite 安全性**：开启 `isWhite: true` 会完全绕过登录和权限检查，请谨慎使用，通常仅用于公开页面（如登录页、注册页）。
3.  **路由 Name 唯一性**：请务必保证每个路由对象的 `name` 字段是唯一的。
