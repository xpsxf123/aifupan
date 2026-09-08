# HTTP 接口规范 (HTTP Interface Rules)

本规范仅定义前端接口定义与调用的核心约束。详细配置生成请参考 Skill: `scaffold-http`。

## 1. 结构规范 (Structure)

- **模块文件**: 存放在 `src/http/api/` 目录下，按业务模块命名 (如 `user.js`, `order.js`)。
- **聚合入口**: 所有模块必须在 `src/http/api/index.js` 中引入并统一导出。

## 2. 编码规范 (Coding Standards)

- **基本格式**: 使用 `export default` 导出包含 API 方法的对象。
- **实例引用**: 必须从 `../httpConfig` 引入封装好的 `http` 实例。
- **方法命名**: 严禁使用动词短语，统一采用**小驼峰命名法** (CamelCase)，如 `getUserList`, `updateOrder`。
- **参数处理**:
  - GET 请求使用 `{ params }` 传递。
  - POST/PUT 请求直接传递数据对象。
- **注释要求 (JSDoc)**: 每个 API 方法**必须**包含以下注释：
  - `@description`: 接口功能简述。
  - `@param`: 参数类型及含义。
  - `@returns`: 返回值说明。

## 3. 禁忌 (Prohibitions)

- **禁止直连**: 严禁在 Vue 组件中直接导入 `axios` 或 `httpConfig` 发起请求。
- **禁止内联 Mock**: 正式代码中不得包含 `setTimeout` 或写死的 Mock 数据。
- **禁止硬编码**: 接口 URL 应保持简洁，避免硬编码域名（由 base URL 控制）。
