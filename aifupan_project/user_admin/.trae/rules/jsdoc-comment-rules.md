## 代码注释补全规则（JSDoc）

本规则用于统一项目内的注释规范，保证每次生成/修改代码时都具备可读的 JSDoc 注释，便于团队协作与 AI 复用。

### 1. 语言与格式

- 所有注释必须使用中文。
- 统一使用 JSDoc 块注释：`/** ... */`。
- 不写无意义注释（例如：把代码重复描述一遍）。

### 2. 文件头注释（强制）

- 每个 `.vue` / `.js` 文件顶部必须包含文件头 JSDoc：
  - `@file`：文件名
  - `@description`：该文件的业务用途/职责

示例：

```js
/**
 * @file index.vue
 * @description 角色管理页面
 */
```

### 3. 函数与核心逻辑注释（强制）

以下场景必须写函数级 JSDoc：

- 业务逻辑函数（事件处理、数据转换、请求封装、计算逻辑）
- 对外暴露的方法（export、defineExpose、组件对外 API）
- 拦截器、回调函数、适配层函数（beforeRequest/afterRequest 等）

函数级 JSDoc 至少包含：

- `@description`：函数用途（一句话讲清楚）
- `@param`：每个参数的类型与含义（无参数可省略）
- `@returns`：返回值说明（无返回可省略）

示例：

```js
/**
 * @description 将分页参数转换为后端格式（page/limit）
 * @param {Object} params - 原始请求参数
 * @returns {Object}
 */
const beforeRequest = (params) => {}
```

### 4. 配置对象注释（强制）

以下配置对象必须在声明前补充说明：

- 表格列配置（tableColumns）
- 表单配置（formConfig）
- 搜索配置（searchConfig）
- 组件 props 入参说明（可用 `@property`）
- API 对象（如 Curd 的 `api: { list/add/edit/del }`）

示例：

```js
/**
 * @description 页面接口（Curd 约定：list/add/edit/del）
 */
const api = { ... }
```

### 5. 注释更新原则

- 修改逻辑时必须同步更新注释，保持“注释描述”和“真实行为”一致。
- 删除功能/字段时必须同步移除无效注释，避免误导。
