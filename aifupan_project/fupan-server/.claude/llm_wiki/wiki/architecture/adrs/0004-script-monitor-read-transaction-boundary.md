---
adr: "0004"
title: detail 接口"读+自动写已读"的事务边界设计
status: accepted
date: 2026-06-08
source: archive/20260608_B9-read-semantics-to-report-column.md
---

<!-- Source: archive/20260608_B9-read-semantics-to-report-column.md (harvested 2026-06-08) -->

# ADR-0004: detail 接口"读+自动写已读"的事务边界设计

**Status:** accepted
**Date:** 2026-06-08
**Deciders:** 用户（业务决策人）/ beta（技术实现）

## Context

`qualityReportDetail` 和 `patrolReportDetail` 新增"读时自动写已读"副作用。需要决定：写已读是否在同一事务内？写失败是否应该阻塞详情读取？并发多请求是否需要加分布式锁？这三个问题影响实现复杂度、可靠性和用户体验。

## Decision

写已读使用独立的 Spring Bean 方法 `ScriptMonitorReportWriteService#markReadIfNeeded`（独立事务 `@Transactional`），在 `qualityReportDetail` 中 catch 写已读异常（fail-safe），写失败不影响详情返回；并发幂等依赖 `UPDATE ... WHERE is_read=0`（无需分布式锁）。

## Alternatives Considered

**Alternative A: 同事务写（detail 方法本身加 @Transactional，写已读与读在同一事务）**

- Pros: 写已读与详情读取原子一致；实现简单（无需额外 catch block）。
- Cons: 读操作不应该触发大事务（MongoDB 查询在事务外，读视频 Feign 调用也在事务外，实际上事务只包裹 MySQL 部分）；写已读 DB 异常会导致整个 detail 接口返回错误（用户无法看到报告详情）；违反"写失败不应影响读"的直觉原则。
- Failure conditions: DB 抖动时 detail 接口整体不可用（用户无法看到报告内容）。
- Estimated complexity: S
- Why not chosen: 副作用写失败不应阻塞主路径（详情读取），已读状态可容忍最终一致（失败后用户下次打开再写即可）。

**Alternative B: 独立事务 + fail-safe（本次选择）**

- Pros: 写失败静默处理，不影响详情；独立事务避免 self-call 失效（复用 C3 修补的 `ScriptMonitorReportWriteService` 独立 Bean 模式）；UPDATE 谓词幂等，无分布式锁复杂度。
- Cons: 写失败时响应 isRead 可能为 stale 值（0），用户需下次打开才看到 isRead=1；需要额外 catch block；失败后需要告警。
- Failure conditions: DB 持续不可用时 is_read 长期不更新（但报告内容仍可读）。
- Estimated complexity: M
- Why chosen: 已读是"轻量副作用"，最终一致可接受；告警机制（§5.5.6）确保持续失败时有感知。

**Alternative C: 异步写已读（MQ 或 ThreadPoolExecutor）**

- Pros: 彻底解耦写已读与详情响应；高吞吐场景下响应时间最优。
- Cons: 引入异步通道增加架构复杂度；MQ 接入需要新增消息定义 + Consumer；ThreadPoolExecutor 在 Spring Boot 关闭时有任务丢失风险；写已读时序不严格（可能并发两个请求各自异步写，但幂等谓词仍然保证最终结果一致）；over-engineering（已读写入 P99 < 5ms，无需异步化）。
- Failure conditions: MQ 故障或线程池满载时 is_read 更新延迟，前端轮询可见 isRead 短暂为 0。
- Estimated complexity: L
- Why not chosen: YAGNI — 单次 UPDATE 耗时极低，同步独立事务已足够；引入 MQ 成本不匹配问题规模。

## Consequences

**Positive**
- 写已读与详情读取解耦，DB 抖动时用户仍可看到报告内容。
- 幂等 UPDATE 无锁设计，高并发安全。
- 复用 `ScriptMonitorReportWriteService` Bean 模式，与 `finishReport`/`restoreOrFail` 等方法体系一致。

**Negative**
- 写失败时响应 `isRead=0`（stale），用户需重新打开报告才触发重试写入。
- 需要额外告警监控写已读失败率（§5.5.6）。

## Risks

- Risk: catch block 吞掉异常导致已读长期不更新（无感知）→ Mitigation: `log.warn` + 监控告警（`script_monitor_mark_read_fail_count > 10 in 1h`）确保运维有感知。
- Risk: 并发双请求各自判断 `canConfirm=true && report.isRead=0`，两个请求都尝试 UPDATE → Mitigation: `WHERE id=? AND is_read=0` 谓词确保只有第一次 UPDATE 命中 1 行，第二次命中 0 行；结果幂等，`confirmed_at` 取第一次写入值。
