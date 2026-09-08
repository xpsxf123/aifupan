# API Index (Contracts)

This index is the routing table for all externally exposed APIs.

## Hard Rules (MUST)
- During `Archive`, the Agent MUST extract API signatures from `openspec.md` and append them into the table(s) below.
- Do not guess API contracts by scanning the whole codebase. Use the wiki as the source of truth, then validate against code when needed.

## Core Domain APIs

| API (Method + Path) | Summary | Auth & Identity | Version | Doc Link | Write-back Date |
|---|---|---|---|---|---|
| **Words APIs** | 复盘分析全部 REST API：客户端 20 Controller + 管理端 39 Controller | Signature + Session | v2.6 | `[words_api.md](words_api.md)` | 2026-05-08 |
| **CRM APIs** | CRM 集成接口：5 个内部 API + 订单事件出站 | APIKey / salescoach-agent | v2.6 | `[crm_api.md](crm_api.md)` | 2026-05-19 |
| **Order APIs** | 订单/商品/套餐/邀请码/用户资产全部 REST API（4 Controller）+ 7 Feign 接口 | Signature + Session | v2.6 | `[order_api.md](order_api.md)` | 2026-05-20 |
| **Agent APIs** | 代理渠道/佣金/邀请码全部 REST API（5 Controller）+ 5 Feign 接口 | Signature + Session | v2.6 | `[agent_api.md](agent_api.md)` | 2026-05-20 |
| **Power APIs** | 用户/租户/角色/菜单/销售统计全部 REST API（14 Controller）+ 27 Feign 接口 | Signature + Session | v2.6 | `[power_api.md](power_api.md)` | 2026-05-20 |
| **Third APIs** | 第三方集成 REST API（1 Controller）+ 8 Feign 接口（支付/短信/AI模型/存储） | Signature + Session | v2.6 | `[third_api.md](third_api.md)` | 2026-05-20 |
| **Video APIs** | 视频热搜/达人/群组 REST API（7 Controller）+ 0 Feign 暴露、4 Feign 消费 | Signature + Session | v2.6 | `[video_api.md](video_api.md)` | 2026-05-20 |
| **AI APIs** | AI 诊断/提示词/对话 REST API（5 Controller）+ 2 Feign 暴露、13 Feign 消费 | Signature + Session | v2.6 | `[ai_api.md](ai_api.md)` | 2026-05-20 |
| **Activity APIs** | 邀请活动/进度/奖励 REST API（3 Controller）+ 6 HTTP + 7 Feign 接口 | Signature + Session | v2.6 | `[activity_api.md](activity_api.md)` | 2026-05-20 |
| **Reward APIs** | 奖励记录/发奖汇总 REST API（3 端点） | Signature + Session | v2.6 | `[reward_api.md](reward_api.md)` | 2026-05-20 |
| **System APIs** | 字典数据/字典类型/登录轮播图 REST API（7 Controller）+ 1 Feign 接口 | Signature + Session | v2.6 | `[system_api.md](system_api.md)` | 2026-05-20 |
| **Common APIs** | 跨模块通用 REST API（9 Controller）：文章/客户端日志/文件/操作日志/系统配置/TencentCOS | Signature + Session | v2.6 | `[common_api.md](common_api.md)` | 2026-05-20 |

---

## Archive Extraction SOP
During `Archive`, append a new row using the template below.

### Append Template
```markdown
| {Method} {Path} | {one-line summary} | {Auth Type} / {Identity Type} | {Version} | `[{spec_doc_name}]` | {YYYY-MM-DD} |
```

Anti-bloat rule: if this table exceeds 50 rows, you MUST split it into per-module sub-indexes (example: `user/`, `trade/`) and keep this file as a high-level router only.
