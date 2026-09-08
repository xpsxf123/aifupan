# Architecture Index (Baselines & ADRs)

This domain records architecture baselines and ADRs (Architecture Decision Records).

## Hard Rules (MUST)
- When making cross-cutting technical choices (module boundaries, middleware, global patterns), you MUST consult existing ADRs or add a new one.

## Baselines & Guards
- Security baseline: [../preferences/security_rules.md](../preferences/security_rules.md)

## ADR List

| ADR # | Title | Status | Decision Summary | Date | Doc Link |
|---|---|---|---|---|---|
| **Words Architecture** | replay-words 模块架构基线 (6 ADRs) | Accepted | 双模块 Controller、Producer 模式、MongoDB 长文本、数据混淆、手动时间戳/软删除、雪花 ID | 2026-05-08 | `[words_architecture.md](words_architecture.md)` |
| **CRM Architecture** | CRM 子系统架构基线 (6 ADRs) | Accepted | CRM 归属 power 模块、明细-快照双层模型、API Key 鉴权、phone→userId 换算、Spring Event 出站、扩展表不冗余手机号 | 2026-05-19 | `[crm_architecture.md](crm_architecture.md)` |
| **Order Architecture** | replay-order 模块架构基线 | Accepted | Feign 7 出站 / 3 入站、Strategy+Factory 下单模式、无 MQ、过期监听已注释、Spring Event 出站 CRM | 2026-05-20 | `[order_architecture.md](order_architecture.md)` |
| **Agent Architecture** | replay-agent 模块架构基线 | Accepted | 全部同步请求-响应、无 MQ/定时任务、Redis INCR 计数器销售轮询、Bll 层事务边界 | 2026-05-20 | `[agent_architecture.md](agent_architecture.md)` |
| **Power Architecture** | replay-power 模块架构基线 | Accepted | 27 Feign 出站、1 MQ Producer、1 XxlJob、MD5 密码哈希、token @TableLogic 历史遗留 | 2026-05-20 | `[power_architecture.md](power_architecture.md)` |
| **Third Architecture** | replay-third 模块架构基线 | Accepted | 短信 Chain of Responsibility 故障转移、AI 模型 Factory+Strategy、弹幕 TableStore 双查询路径、无 MQ | 2026-05-20 | `[third_architecture.md](third_architecture.md)` |
| **Video Architecture** | replay-video 模块架构基线 | Accepted | 0 Feign 暴露 / 4 Feign 消费、6 XxlJob 邮件账号池、Redis Hash+ZSet+Lua 原子分配、MongoDB 视频内容提取 | 2026-05-20 | `[video_architecture.md](video_architecture.md)` |
| **AI Architecture** | replay-ai 模块架构基线 | Accepted | Bll/Rse 双层、13 Feign 消费 / 2 Feign 暴露、异步线程池 HTML 生成、MongoDB+MySQL 跨数据源最终一致性、无 MQ | 2026-05-20 | `[ai_architecture.md](ai_architecture.md)` |
| **Activity Architecture** | replay-activity 模块架构基线 | Accepted | 无 MQ / 无 Redis、Feign 出站 reward/agent/order、物理删除（无 isDeleted）、@Resource DI 遗留 | 2026-05-20 | `[activity_architecture.md](activity_architecture.md)` |
| **Reward Architecture** | replay-reward 模块架构基线 | Accepted | MQ Consumer 在 replay-api（RocketMqListener）、9 Feign 出站、DistributedLock 在 Listener 层、Pair-write 模式 | 2026-05-20 | `[reward_architecture.md](reward_architecture.md)` |
| **System Architecture** | replay-system 模块架构基线 | Accepted | 1 Feign 暴露（DictDataFeign，16+ 消费方）、cache-aside 非原子、物理删除（非软删）、@Transactional 缺失 | 2026-05-20 | `[system_architecture.md](system_architecture.md)` |
| **Catch-All** | 跨模块缺失架构基线（待补） | Proposed | 订单资产扣减的精确并发模型、reward 双发防护、activity 物理删除审计合规、各模块 Redis 缓存策略标准化 | 2026-05-20 | (待写入) |
| **ADR-0001** | 合并报告输出格式选型 — Pure Markdown vs JSON + wrapper vs 双层 | Accepted | 巡检 buildSummary mirror 质检 Bll Markdown+标签范式，取代 JSON.parseObject | 2026-06-07 | `[adrs/0001-patrol-summary-markdown.md](adrs/0001-patrol-summary-markdown.md)` |
| **ADR-0002** | 现网提示词更新 sql 策略 — UPDATE vs 仅改 INSERT | Accepted | 新建 replay-32.sql 做 UPDATE 覆盖现网已执行行，消除 WHERE NOT EXISTS 幂等保护导致的静默数据故障 | 2026-06-07 | `[adrs/0002-patrol-summary-sql-update-strategy.md](adrs/0002-patrol-summary-sql-update-strategy.md)` |

---

## Archive Extraction SOP
If `Propose` makes a global architecture decision, you MUST write it back here during `Archive`.

### Append Template
```markdown
| ADR-{XXX} | {decision title} | Accepted | {one-line reason} | {YYYY-MM-DD} | `[{doc_link}]` |
```
