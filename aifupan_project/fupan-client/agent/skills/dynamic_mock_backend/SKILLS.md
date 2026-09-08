---
name: "dynamic_mock_backend"
description: "基于接口文档（OpenAPI 或 Markdown）生成 Node.js 动态 mock 后端：接口 1:1 对齐、JSON 文件持久化（仿数据库）、支持 CRUD+分页+基础校验；可选生成 HTML 测试页用于端到端数据流转验证。"
---

# dynamic_mock_backend

## 适用场景

- 需要“数据流 mock”，而不是“纯假数据 mock”：数据必须可被增删改查并持久化，能复现真实联调链路
- 后端未就绪但接口已定义：前端需要按接口文档 1:1 对齐跑通完整页面交互
- 希望 mock 层能模拟数据库读写行为（JSON 文件），并提供必要的参数校验与错误码

## 目标

- 生成可运行的本地 Node.js mock 服务（HTTP server）
- 按接口文档实现接口 1:1：method/path/入参位置(body/query/header)/响应结构
- 以 JSON 文件作为“数据库”，实现可持续读写（CRUD、分页、下拉等）
- 覆盖基础校验：必填、类型、枚举、长度；失败返回统一 ApiResponse + error_codes 规范
- 可选生成一个 HTML 测试页，用于直接验证“接口调用 → JSON 写入/读取 → 返回”全链路

## 输入

- 接口文档（必选，二选一或混用）：
  - OpenAPI：路径/方法/Schema/响应
  - Markdown 接口文档：如 `api_design.md`、`data_dictionary.md`、`controllers/*.md`
- 错误码规范（建议）：`error_codes.md`
- 可选：核心 JSON（页面动作与接口绑定、字段校验规则、种子数据策略）
- 可选：mock 站点需求（需要哪些接口用于“端到端测试页”）

## 输出

- Node.js mock 服务脚手架（可直接启动）
- JSON 持久化“数据库”文件（可重置/可导入种子数据）
- 接口实现清单（路由表）：接口 1:1 对齐文档，标注鉴权策略与数据落盘位置
- 可选：`tester.html`（可视化测试页）

## 工作步骤（推荐）

1. 汇总接口来源：优先以 `api_design.md` 固定全局响应/分页规范，再以 `data_dictionary.md`/`controllers/*.md` 补齐接口细节
2. 抽取“资源模型”：以 base path 聚合为资源（sub-company/dept/team/employee/role/menu…），将动作映射为 CRUD+list/page+option+tree 等常用形态
3. 生成数据层：单一 JSON 文件或多表 JSON 文件；提供自增 ID、索引、逻辑删除（可选）
4. 生成路由层：method+path 精确匹配；body/query/header 解析；CORS 支持
5. 加入校验与错误码：必填/类型/枚举/长度；鉴权缺失返回 401；不支持方法返回 405
6. 生成测试入口（可选）：HTML 端到端测试页（内置 Token 输入、请求 body 编辑、响应查看、常用 CRUD 按钮）

## 边界

- 不实现复杂业务规则与强一致事务，只提供“数据流正确性”的可复现联调环境
- 鉴权只做最小化模拟（例如：除匿名接口外要求 Header `Token` 存在）
- 导出/文件流等能力可用简化实现（CSV/二进制占位），以满足前端联调为主

## 失败处理

- 接口文档文件名/编码异常（Windows 下常见）：使用 PowerShell 的 `-LiteralPath` 读取内容并基于内容抽取 method/path
- 文档缺失或只提供索引：以已存在文档为准生成可运行 mock，并输出“未覆盖接口清单”供补齐
- Schema 信息不足：默认启用“弱校验”（只校验必填与类型），并允许通过核心 JSON/手工规则增量补齐

## 模板（可落盘复用）

- `templates/mock-backend-json/package.json`
- `templates/mock-backend-json/server.mjs`
- `templates/mock-backend-json/data/db.json`
- `templates/mock-backend-json/tester.html`
- `templates/mock-backend-json/mock.config.json`

## 模板使用约定（重要）

- `templates/mock-backend-json` 是通用模板，不应内置任何“具体项目接口/固定业务数据”
- 生成具体 mock 时，应由生成器根据实际 API 文档产出：
  - `mock.config.json`：定义资源（base/collection/字段必填/分页过滤/鉴权匿名等）与自定义静态路由
  - `tester.html` 内 `testerConfig`：注入接口清单 endpoints 与关联字段映射 relations（用于自动生成数据时填充 *Id/*Ids）
