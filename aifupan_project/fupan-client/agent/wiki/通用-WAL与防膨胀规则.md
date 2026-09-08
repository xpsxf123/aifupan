# 通用-WAL与防膨胀规则

WAL（Write-Ahead Log）用于把“新增知识/变更决策”先写成碎片，避免直接改动聚合文档导致冲突与膨胀。

## 规则
- 先写 WAL：新增知识先追加到 `wiki/<domain>/wal/`
- 再合并：按窗口合并到 `wiki/<domain>/index.md`
- 控制体积：`index.md` 只放摘要与链接，细节下沉到独立页面
- 禁止敏感信息：Token/密钥/个人数据禁止写入 WAL/wiki

## 合并策略（推荐）
- 合并时追加“合并批次标题”，并把片段移动到 `wal/applied/`
- 当域 `index.md` 过长时停止合并，先做拆分

## 模板
- 参见 `agent/templates/`：
  - focus_card.md
  - wal_api_append.md / wal_router_append.md / wal_state_append.md / wal_component_append.md

## 相关链接
- 通用-上下文漏斗与写回
- 通用-生命周期与门禁
