# 路由模块说明

## 1. 模块概述

本模块负责处理应用的路由逻辑，包括路由初始化、动态路由生成以及全局导航守卫。

- **核心文件**: `src/router/index.js`
- **权限控制**: `src/router/prmission.js`
- **路由配置**: `src/config/menu.js` (配置文档见 [src/config/README.md](../config/README.md))

## 2. 路由生成机制

项目采用了**动态路由**方案。`router/index.js` 会读取 `src/config/menu.js` 中的 JSON 配置，并将其转换为 Vue Router 可识别的路由对象。

### 组件自动加载

系统利用 Vite 的 `import.meta.glob` 特性，自动建立组件路径映射：

- `Layout` -> `src/layout/index.vue`
- `views/**/*` -> `src/views/**/*.vue`

配置中的 `component` 字符串会被自动替换为对应的组件导入函数。

## 3. 权限控制流程 (Permission)

文件: `src/router/prmission.js`

全局前置守卫 (`router.beforeEach`) 处理逻辑如下：

1.  **Token 校验**: 检查用户是否持有访问令牌。
2.  **白名单放行**: 如果访问 `/login` 等白名单页面，直接放行。
3.  **用户信息获取**:
    - 如果已登录但无用户信息（如页面刷新），触发 `userStore.getUserInfo()`。
    - 根据用户信息中的角色 (`roles`)，配合路由配置中的 `meta.roles` 进行访问拦截。
4.  **路由重定向**:
    - 如果用户已登录且访问 `/login`，自动重定向到首页。

## 4. 常用操作

### 4.1 获取当前路由信息

在 Vue 组件中：

```javascript
import { useRoute } from 'vue-router'
const route = useRoute()
console.log(route.path, route.meta.title)
```

### 4.2 编程式导航

```javascript
import { useRouter } from 'vue-router'
const router = useRouter()

// 跳转页面
router.push('/user/list')
router.push({ name: 'UserList' })
```

## 5. 常见问题

**Q: 新增页面后报 404？** A: 请检查 `src/config/menu.js` 中的 `component` 路径是否正确，且对应的 `.vue` 文件确实存在于 `src/views/` 目录下。

**Q: 菜单栏不显示新路由？** A: 检查 `meta.hidden` 是否被设为了 `true`，或者当前登录账号的角色是否在 `meta.roles` 允许范围内。

## 6. Meta 配置参数说明

在 `src/config/menu.js` 中配置路由时，`meta` 对象支持以下参数：

- `title` (String): 页面标题，显示在菜单和浏览器标题中。
- `icon` (String): 菜单图标组件名称。
- `isNav` (Boolean): 是否在侧边栏导航中显示。
- `roles` (Array): 权限控制，允许访问该路由的角色数组。
- `notLayout` (Boolean): 是否不使用标准 Layout 布局。
  - `true`: 页面将全屏显示，不包含侧边栏和顶部导航（即使组件使用了 Layout）。
  - `false` / `undefined`: 默认显示标准 Layout 布局。
