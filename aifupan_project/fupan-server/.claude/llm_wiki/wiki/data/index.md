# Data Index (Models)

This index is the routing table for database tables, ER notes, and index strategy.

## Hard Rules (MUST)
- You MUST NOT guess schemas by scanning the entire codebase.
- During `Archive`, the Agent MUST extract table changes from `openspec.md` and append them to the table below.

## Core Tables

| Table Name | Store Type | Purpose | Key Fields / Index Notes | Retention Policy | Source Spec |
|---|---|---|---|---|---|
| **Words Data (60 MySQL + 2 MongoDB)** | MySQL + MongoDB | 复盘分析全部数据模型：主播、视频、AI、词汇、看板、行业、对比 | 详见 `words_data.md` | 软删除 (isDeleted) | `[words_data.md](words_data.md)` |
| **CRM Data (6 核心 + 2 扩展)** | MySQL | CRM 数据模型：客户、销售、跟进、快照、订单、AI画像、阶段事件 | 详见 `crm_data.md` | 软删除 (isDeleted) | `[crm_data.md](crm_data.md)` |
| **Order Data (20 张)** | MySQL | 订单数据模型：商品/套餐/订单/支付/邀请码/用户资产/AI计费 | DDL 详见 `order_data_detail.md` | 软删除 (isDeleted)；流水表无删除 | `[order_data.md](order_data.md)` |
| **Agent Data (9 张)** | MySQL | 代理渠道数据模型：代理/渠道/佣金/邀请码/平台销售 | 详见 `agent_data.md` | 软删除 (isDeleted) | `[agent_data.md](agent_data.md)` |
| **Power Data (23 张)** | MySQL | RBAC 数据模型：用户/租户/角色/菜单/设备指纹/销售统计 | 详见 `power_data.md` | 软删除 (isDeleted)；token表用@TableLogic | `[power_data.md](power_data.md)` |
| **Third Data (7 张)** | MySQL + TableStore | 第三方集成数据模型：支付/短信/文件/AI模型/弹幕 | 详见 `third_data.md` | 软删除 (isDeleted) | `[third_data.md](third_data.md)` |
| **Video Data (18 MySQL + 1 MongoDB)** | MySQL + MongoDB | 视频热搜数据模型：热搜/达人/群组/邮件账号/视频内容提取 | 详见 `video_data.md` | 软删除 (isDeleted)；MongoDB upsert | `[video_data.md](video_data.md)` |
| **AI Data (4 MySQL + 1 MongoDB)** | MySQL + MongoDB | AI 诊断数据模型：提示词/诊断配置/对话记录/违规检测 | 详见 `ai_data.md` | 软删除 (isDeleted)；MongoDB TTL | `[ai_data.md](ai_data.md)` |
| **Activity Data (4 张)** | MySQL | 邀请活动数据模型：活动/进度/进度奖励/邀请关系 | 详见 `activity_data.md` | 物理删除（无 isDeleted） | `[activity_data.md](activity_data.md)` |
| **Reward Data (1 张 + 1 视图实体)** | MySQL | 奖励记录数据模型：发奖记录 | 详见 `reward_data.md` | 软删除 (isDeleted) | `[reward_data.md](reward_data.md)` |
| **System Data (3 张)** | MySQL | 系统配置数据模型：字典类型/字典数据/登录轮播图 | 详见 `system_data.md` | 物理删除（实际用 removeById） | `[system_data.md](system_data.md)` |

---

## Archive Extraction SOP
Append a new row during `Archive` using the template below.

### Append Template
```markdown
| {Table Name} | {Store Type} | {one-line purpose} | `{key fields and index notes}` | {Retention Policy} | `[{spec_doc_name}]` |
```

Anti-bloat rule: if this index grows beyond 50 tables, you MUST split by module (example: `auth_tables.md`, `trade_tables.md`) and keep only top-level links here.
