---
adr: 0001
title: 合并报告输出格式选型 — Pure Markdown vs JSON + wrapper vs 双层
status: accepted
date: 2026-06-07
source: archive/20260608_patrol-summary-md-cast.md
---

<!-- Source: archive/20260608_patrol-summary-md-cast.md §9 ADR-1 -->

## Context

互动巡检合并提示词（cueType=21）当前约定 AI 返回纯 JSON（`{"items":[...],"summary":{...}}`）。前端通过 `JSON.parse(summaryJson)` 取字段渲染。话术质检（cueType 对应 `ScriptMonitorGenerateBll`）已在 PATCH #13 改为 Markdown + `<aifupan-data-block>` 标签范式，AI 指令遵循率更高。现在需要决定巡检合并报告是否完全 mirror 质检范式（Pure Markdown），还是保留 JSON 但外包标签（双层）。

存在的制约：

1. AI 对 Markdown 输出格式遵循率高于 JSON；
2. 质检已是 Markdown 范式，维护两套范式增加成本；
3. `patrolReportDetail` 接口前端尚未上线（B5 TBD），改变契约影响面可控；
4. 巡检合并报告包含弹幕明细大表格（N 行 8 列），AI 需在 Markdown 表格基础上额外包 `<aifupan-data-block>` 标签，挑战比质检更大（质检只需包几个数字）。

## Decision

采用方案 A（Pure Markdown）：合并提示词改为 Markdown 全文输出，摘要段用 `<aifupan-data-block>` 标签包裹；`buildSummary` 用正则提取标签内文本存 `summaryJson`；`reportContent` 存 Markdown 全文。前端契约由"JSON.parse"改为"直接渲染 Markdown"。

关键约束：`<aifupan-data-block>` 标签**仅包摘要总结段（3-5 行）**，不包弹幕明细表格，提示词 md 需严格标注此约定。

## Alternatives

### Alternative A: Pure Markdown（选定方案）

- **Pros:** 与质检 Bll `SUMMARY_TAG_PATTERN` 完全统一，零额外代码；AI 指令遵循率高（倾向 Markdown）；前端渲染简单（无需 JSON.parse）；维护成本最低。
- **Cons:** 摘要语义嵌入 `<aifupan-data-block>` 标签内，非结构化字段（effectiveRate 等数字需从 Markdown 文本中人工识读，无直接 JSON 字段）；AI 输出可能把弹幕大表格整个包进标签。
- **Failure conditions:** AI 不遵循标签格式（概率低，已在质检验证有效）；标签内摘要文本无法机器解析 effectiveRate 等具体数字（当前前端仅需文本渲染，非数字消费）。
- **Estimated complexity:** S（直接 mirror 质检 Bll，~15 行改动）
- **Why chosen:** 最简方案；质检已验证可行；前端 B5 尚未上线，契约破坏影响面可控。

### Alternative B: 保留 JSON + 外包 wrapper 标签

- **Pros:** 前端数值字段（effectiveRate、totalUnits 等）可 JSON.parse 取；对未来数据分析友好。
- **Cons:** AI 需同时生成外层 Markdown + 内层 JSON，指令冲突概率大；`buildSummary` 需先正则提取标签，再 JSON.parse 内层，两次解析；与质检范式分裂，维护两套逻辑。
- **Failure conditions:** AI 输出内层 JSON 格式漂移（字段名变化、格式不一致），`buildSummary` JSON.parse 异常，summaryJson 全 null。
- **Estimated complexity:** M（需保留 JSON 解析路径）
- **Why not chosen:** AI 双层指令遵循率低于单一 Markdown；维护两套范式成本高；当前前端无 JSON 数值字段消费需求（B5 仅需文本渲染）。

### Alternative C: 双层（Markdown 全文，标签内仍是 JSON）

- **Pros:** 前端和后端均可解析结构化数据；`reportContent` 是 Markdown，阅读友好。
- **Cons:** AI 需同时生成外层 Markdown 表格 + 内层 JSON，指令最复杂，失败概率最高；`buildSummary` 同样需两次解析；前端 B5 需 JSON.parse 标签内容，渲染逻辑不简单；与 A 相比复杂度高但收益小（当前无数值字段消费需求）。
- **Failure conditions:** AI 指令最复杂，内层 JSON schema 漂移风险；标签包含大 JSON 时 AI 易截断。
- **Estimated complexity:** L（双重格式约定 + 双重解析路径）
- **Why not chosen:** 复杂度最高但收益最低；当前阶段无结构化数值消费需求。

## Consequences

**Positive**

- `buildSummary` 与质检 Bll 完全一致，代码复用率最高，维护成本最低。
- AI 输出格式指令最简单，遵循率最高。
- 前端渲染逻辑简化（Markdown 直渲，无 JSON.parse）。

**Negative**

- `summaryJson` 字段存的是 Markdown 纯文本，失去结构化字段（effectiveRate、totalBarrages 等）的机器可读性。若未来需要对这些数字做汇总统计，需在 AI 提示词层面重新设计或额外字段。
- AI 需要把弹幕明细大表格和摘要段用同一 Markdown 约定表达，`<aifupan-data-block>` 标签只包摘要——这要求提示词设计清晰区分"大表格区"和"摘要区"，否则 AI 可能把表格也包进标签。

## Risks

- **Risk:** AI 把 N 行弹幕表格整个包进 `<aifupan-data-block>` 标签，导致 `summaryJson` 过长（TEXT 字段虽能存，但前端渲染性能问题）。
  → **Mitigation:** 提示词明确约定"标签内仅放概要总结文字（3-5 行）；弹幕明细表格在标签外"，Implement 阶段在提示词 md 中严格约定结构示例。
- **Risk:** AI 输出无标签（指令遵循率低于 100%），导致 `summaryJson = null` 的巡检报告偶发出现。
  → **Mitigation:** log.warn 已覆盖，可通过 APM 日志监控 warn 频率；如频率高则回炉优化提示词。
