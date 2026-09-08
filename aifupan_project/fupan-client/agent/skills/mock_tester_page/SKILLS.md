---
name: "mock_tester_page"
description: "Generates a local HTML tester page for a mock backend. Invoke when a mock server is ready and you need searchable endpoints + code/table view + pagination + CRUD + auto-seed to verify data flow quickly."
---

# mock_tester_page

## 目标

- 在 mock 服务已具备可调用接口后，生成一个本地 HTML 调试页面，用于快速验证接口与数据流转
- 支持接口快速定位（中文/Path 搜索、中文与 Path 双向联动）
- 支持响应的两种查看方式：原始 JSON（Code）与表格（Table）
- 在表格模式下支持分页、增删改、自动生成 mock 数据（写入 JSON 持久化“数据库”）

## 输入

- mock 服务 base_url（例如 `http://localhost:3100`）
- 接口清单（必须包含 method/path，建议包含中文名称与示例 body）
  - 推荐结构：`[{ method, path, name, body }]`
- 鉴权规则（如 Header `Token`）
- CRUD 约定（默认采用项目规范）：
  - 同一资源 base 下存在：`/add` `/update` `/delete` `/detail` `/page|list` `/option`
- 分页约定（默认采用项目规范）：
  - 请求体字段：`page` / `limit`
  - 响应体结构：`ApiResponse<PageData<T>>` 且 `data.list` 为数组

## 输出

- `tester.html`（单文件，无依赖；可选拆分 `assets/app.js`）
- 可选：`endpoints.json`（便于后续脚本化替换接口清单）

## 页面功能要求（必须）

### 1) 接口选择与搜索

- 提供 method 选择（GET/POST）
- 提供关键词搜索输入框：同时匹配接口中文名与 path
- 提供两个下拉：
  - “接口中文”下拉：显示中文名（找不到中文名时回退显示 path）
  - “Path”下拉：显示 path
- 两个下拉必须双向联动，同一个接口选中后同步更新

### 2) 请求编辑与发送

- 支持填写 base_url 与 Token
- 支持编辑请求 body（JSON）
- 支持一键格式化 JSON
- 支持重置为该接口的默认 body

### 3) 响应查看：Code / Table

- Code 模式：展示完整 `ApiResponse` JSON
- Table 模式：
  - 若响应为 `data.list`（分页）则展示 list
  - 若响应为 `data` 数组则展示数组
  - 表格区域固定高度，使用滚动条显示；表头固定（sticky）
  - 表格上方提供分页控制：
    - 上一页/下一页
    - 页码输入
    - pageSize 选择（会写入 body 的 `limit`）

### 4) 表格内 CRUD（基于接口命名约定自动推导）

- 当当前选择接口为 `POST .../page` 或 `POST .../list` 时，推导同 base 的：
  - `POST .../add`
  - `POST .../update`
  - `POST .../delete`
- 提供“添加”按钮：调用 add 接口
- 每行提供“修改/删除”按钮：调用 update/delete 接口
- 添加/修改必须使用页面自带的 HTML 弹窗组件（overlay modal），不得使用系统 `prompt/confirm`

### 5) 自动生成 mock 数据（必须）

- 提供“自动生成”按钮与数量输入（1~200）
- 自动生成策略：
  - 以 `add` 接口的示例 body 作为字段模板
  - 生成时跳过 `id` 字段（由后端创建）
  - 关联字段不自动创建关联资源：
    - 对 `*Id` / `*Ids` / `parentId` 等字段：优先调用对应资源的 `.../option` 获取已有 id 列表后随机填充
    - 若 option 返回为空：提示“缺少可用关联数据，请先创建对应资源”
  - 非关联字段可自动填充（name/mobile/email/staffNumber/sort/type/boolean 等）
- 自动生成成功后应刷新当前列表接口并保持当前接口选中

## 失败处理

- Token 缺失：在页面内提示并阻止需要鉴权的功能（CRUD/自动生成）
- 当前接口无法推导 add/update/delete：禁用按钮并提示原因
- 响应非数组且非分页 list：Table 模式提示“暂无可展示数据”

## 约束

- 不依赖第三方库（纯 HTML/CSS/JS）
- 不要求与示例页面样式一致，但必须满足上述功能

