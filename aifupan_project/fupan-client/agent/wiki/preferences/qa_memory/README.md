# qa_memory（对话蒸馏记忆存档）

本目录用于存放“对话蒸馏摘要”的可提交资产：

- `state.json`：当前会话状态（sessionId、不满足计数、是否需要重置）
- `sessions/`：按 sessionId 归档的蒸馏记录（append-only）

写入方式推荐通过脚本：

- `npm run qa:log -- --q "..." --a "..." --distilled "..." --satisfied true|false`
- `npm run qa:reset`

