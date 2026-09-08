# 项目-复盘客户端-CSharp推送体系

## 本文档用途

- 统一说明：C#（客户端）如何调用前端、前端如何注册/分发 action、返回值如何回传给 C#。
- 给“新增一个推送 action + UI 响应”提供可复用的标准做法与边界。

## 定义

“C# 推送”指客户端通过全局入口调用 JS，并把 `action/data` 发送给前端；前端通过一个任务分发器按 action 回调对应 handler。

核心能力由 `src/utils/notifyFromcsharp.js` 提供。

## 数据流

### 1) C# → JS 入口

- 全局入口：`window.notifyFromCSharp(requestDataString)`
- 解析：`JSON.parse(requestDataString)` 得到 `requestData`
- 分发：按 `requestData.action` 查找任务集合并执行

### 2) 前端注册 action

在任意组件内注册：

- `this.$CSharpNotify.addTask(action, callback, errCallback)`

行为约定：

- `requestData.code === 0 && requestData.status === 200` 时走 callback
- 否则走 errCallback（如提供）

### 3) JS → C# 回包

- handler 返回的对象若不含 `code`，会被包装为 `{ code: 0, data: <handlerReturn> }`
- 最终会 stringify 为字符串回传给 C#

## 推荐实现方式（组件内自注册）

推荐把“监听注册 + UI 展示”封装成组件（如标题栏角标/弹窗），组件内部完成：

- mounted：`addTask(action, handler)`
- destroyed：如有需要，提供清理逻辑（当前实现多为覆盖式注册，清理需结合 notifyFromcsharp.js 实现）

这样能保证：

- 推送能力就近归属到 UI 组件
- 页面/布局引用组件即可获得完整能力

## 现有 action 示例（项目已落地）

以下 action 在标题栏相关组件中已使用：

- `networkStatus`：通知前端网络环境（good/exhausted/poor）
- `thirdPartyAuthStatus`：通知前端第三方授权状态（platform/status）
- `openNetworkFatiguePage`：通知前端打开“网络疲惫”弹窗（open=1）

UI 组件落点：

- `src/views/layout/common/title/` 目录下若干 badge/dialog 组件

## 新增 action 的最小步骤

- 明确 action 名称与 data schema（字段/枚举/默认值）
- 在目标 UI 组件内注册 `addTask(action, handler)`
- handler 内只做“状态入库/组件状态更新”，避免做大量 IO
- 若需要调用接口，使用既有 `$httpClient/$httpBack` 约定，不在 handler 内写死域名

## 风险边界

- Web 形态：部分本地能力不可用（例如 client 请求会 reject），新增 action 前需确认运行形态。
- handler 返回结构：建议返回 `{ code, data, msg }` 以保持协议一致，避免 C# 侧解析不稳定。

## 相关链接（知识图谱实体）

- [页面组件]、[状态管理]
- `wiki/项目-复盘客户端-架构概览.md`
- `wiki/项目-复盘客户端-请求与接口体系.md`

