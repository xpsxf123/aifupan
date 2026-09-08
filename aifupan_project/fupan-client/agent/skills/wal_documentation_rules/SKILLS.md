---
name: "WAL 写回规则"
description: "用 WAL（Write-Ahead Log）方式做可控写回：先写碎片，再定期合并到 index，避免 wiki/INDEX 膨胀与冲突。"
---

# WAL 写回规则

## 技能目的
- 把“新增知识/变更记录”写成可追加碎片，降低多人协作冲突
- 把“稳定知识”按窗口合并到域 index，防止 wiki 失控膨胀
- 提供可复用的 WAL 模板，统一写法与字段

## 输入参数
- `change_summary`：必填，文本。本次变更摘要（做了什么、为什么、影响范围）
- `wal_type`：必填，枚举。`api` | `router` | `state` | `component` | `spec` | `other`
- `evidence`：选填，列表。证据锚点（文件路径/函数名/接口名/PRD 版本）

## 输出
- `wal_file_path`：建议写入的 WAL 片段路径
- `wal_content`：可直接落盘的 WAL 内容（Markdown）
- `followup_compaction`：是否需要合并（以及合并到哪个 index）
- `index_link_updates`：建议补齐的链接（从域 index → 具体 wiki 页面）

## 推荐目录（与本仓库 agent/wiki 对齐）
- `wiki/api/wal/`
- `wiki/router/wal/`
- `wiki/state/wal/`
- `wiki/components/wal/`
- `wiki/specs/wal/`
- `wiki/archive/`

## WAL 写作规范（强制）
- 只追加：WAL 文件永远是 append-only，不做重写
- 一片一事：一个 WAL 文件只记录一个主题（接口一次变更/一个组件一次沉淀）
- 必须可回溯：至少包含一个证据锚点（文件/接口/版本）
- 不写敏感信息：Token/密钥/个人数据禁止写入

## WAL 合并规则（推荐）
- 合并窗口：当 WAL 积累到一定量或域 index 明显缺入口时再合并
- 合并目标：合并到对应域的 `index.md`，并把片段移动到 `wal/applied/`
- 超长预警：域 index 超过阈值（例如 400 行）禁止继续合并，优先拆分

## 模板
- 推荐使用 `agent/templates/` 下的 WAL 模板：
  - `wal_api_append.md`
  - `wal_router_append.md`
  - `wal_state_append.md`
  - `wal_component_append.md`

## 边界
- WAL 不是发布日志：只记录可复用知识与重要决策，不记录临时对话细节
