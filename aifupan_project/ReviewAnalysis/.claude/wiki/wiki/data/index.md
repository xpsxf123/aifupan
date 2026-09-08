# Data Index (Models)

This index is the routing table for database tables, ER notes, and index strategy.

## Hard Rules (MUST)
- You MUST NOT guess schemas by scanning the entire codebase.
- During `Archive`, the Agent MUST extract table changes from `openspec.md` and append them to the table below.

## Core Tables

| 数据域 | 说明 | Doc Link |
|---|---|---|
| SQLite 数据库 | 13 个 .db 文件，分域管理 + 文件存储路径 | `[sqlite_tables.md]` |
| 实体模型 | VideoEntity / VideoSliceEntity / UploadFileEntity 字段详情 | `[entity_models.md]` |
| 技术债务登记册 | 已知问题 + 评估工时 + 关联 incident/run_id + 状态跟踪 | `[tech_debt_register.md]` |

---

## Archive Extraction SOP
Append a new row during `Archive` using the template below.

### Append Template
```markdown
| {Table Name} | {Store Type} | {one-line purpose} | `{key fields and index notes}` | {Retention Policy} | `[{spec_doc_name}]` |
```

Anti-bloat rule: if this index grows beyond 50 tables, you MUST split by module (example: `auth_tables.md`, `trade_tables.md`) and keep only top-level links here.
