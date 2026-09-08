# Domain Index (Vocabulary & State)

This index defines the project's vocabulary. The Agent MUST use these terms during `Explorer` and `Propose` to avoid domain drift.

## Core Concepts & State Machines

| Concept | Definition | Related Concepts | Details |
|---|---|---|---|
| **Words Domain (复盘分析)** | 直播复盘内容分析核心领域：主播管理、视频分析、词汇引擎、数据看板、行业热榜、对比分析 | Anchor, Video, AI, Trade | `[words_domain.md](words_domain.md)` |
| **CRM Domain (客户经营)** | 客户关系管理核心领域：客户管理、销售跟进、快照状态、AI画像、阶段事件、订单事件出站 | Customer, Sales, Order, Agent | `[crm_domain.md](crm_domain.md)` |
| **Order Domain (订单/套餐/资产)** | 订单与商品体系：商品类型、套餐版本、订单生命周期（8状态）、邀请码、用户资产（总账/批次/流水）、AI计费、[订单过期流程](order_expiration.md) | Commodity, Package, Order, Invitation, UserProperty | `[order_domain.md](order_domain.md)` |
| **Agent Domain (渠道/佣金)** | 代理渠道体系：代理管理、渠道标签、佣金分配规则、邀请码生成/兑换、平台销售轮询 | Agent, Channel, Commission, InviteCode | `[agent_domain.md](agent_domain.md)` |
| **Power Domain (用户/租户/权限)** | RBAC 权限体系：用户管理、租户管理、角色菜单、设备指纹、销售统计数据 | User, Tenant, Role, Menu, Fingerprint | `[power_domain.md](power_domain.md)` |
| **Third Domain (第三方集成)** | 第三方服务集成：微信支付/支付宝、七牛云存储、短信（阿里云+联麓）、AI模型（豆包/通义/DeepSeek）、蝉妈妈、字节即梦 | WechatPay, Alipay, SMS, AI, Qiniu | `[third_domain.md](third_domain.md)` |
| **Video Domain (视频热搜)** | 视频分析领域：视频提取、热搜管理、达人管理、邮件账号池、群组管理 | Video, HotSearch, Talent, EmailPool, Group | `[video_domain.md](video_domain.md)` |
| **AI Domain (AI诊断)** | AI 诊断分析：诊断提示词模板、AI对话记录、诊断线索、分享记录、违规检测、HTML报告生成 | Diagnosis, Prompt, Conversation, Cue, Violation | `[ai_domain.md](ai_domain.md)` |
| **Activity Domain (邀请活动)** | 邀请活动体系：活动配置、进度管理、进度奖励、邀请关系 | Activity, Progress, Reward, Invite | `[activity_domain.md](activity_domain.md)` |
| **Reward Domain (奖励发放)** | 奖励记录体系：发奖记录、设备指纹防刷、子账号穿透、进度码字典 | Reward, Fingerprint, SubAccount, ProgressCode | `[reward_domain.md](reward_domain.md)` |
| **System Domain (字典/配置)** | 系统配置体系：字典类型、字典数据、登录轮播图 | DictType, DictData, LoginImage | `[system_domain.md](system_domain.md)` |

---

## Archive Extraction SOP
If an `openspec.md` introduces new terms, roles, enum values, or state transitions, the Agent MUST extract them here during `Archive`.

### Append Template
```markdown
| {term} | {1–2 sentence definition and boundary} | {Related Concepts / Synonyms} | `[{details_doc}]` |
```

Anti-bloat rule: if the vocabulary exceeds 30 concepts, you MUST split into per-line dictionaries (example: `dictionary_xxx.md`) and keep this file as a router.
