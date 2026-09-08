# LLM Wiki Knowledge Graph (Root Index)

This file is the root of the wiki. Use it to navigate by drilling down through indexes. Do not guess paths.

## Hard Rules (MUST)
- *All paths are relative to this file (`.claude/llm_wiki/`).*
- Start navigation from this file, then drill down via `index.md` files. Read at most 1–2 index files per step, analyze, then decide the next exact file to read.
- DO NOT jump directly to random documents by guessing paths.
- Per-change: openspecs land in `archive/` (cold storage). Per-module wiki files (`<module>_<topic>.md`) are NOT touched per-change — they are sublimated from archive by `@knowledge-harvester` when a module's archive accumulation crosses the threshold (default 3 new archives, see `.claude/scripts/wiki/harvest_threshold.py`). Architecture changes (Workflow intent) are an exception: `@architecture-curator` writes them directly per-change. Shared domain `index.md` files are routing-only — no per-change edits.

## 0. Project Entry
- **[CLAUDE.md](../../CLAUDE.md)** — the single entry point (modes, lifecycle, rules pointers).

## 1. Philosophy & Templates
- **[Purpose](purpose.md)** — why this wiki exists and what it optimizes for.
- **[OpenSpec Schema](schema/openspec_schema.md)** — contract template for Propose phase.
- **[Sub-agent Contract Schema](schema/subagent_contract_schema.md)** — payload format for sub-agent dispatch.
- **[Skills Index](../skills/trae-skill-index/SKILL.md)** — central navigator for all skills.

## 2. Active Domains (drill-down indexes)

| Domain | Purpose | Per-module files |
|---|---|---|
| **[Domain](wiki/domain/index.md)** | Business vocabulary, terms, state machines | 11 modules: words, crm, order, agent, power, third, video, ai, activity, reward, system |
| **[API](wiki/api/index.md)** | Externally exposed APIs and contracts | 11 modules: words, crm, order, agent, power, third, video, ai, activity, reward, system |
| **[Data](wiki/data/index.md)** | DB tables, indexes, ER notes | 11 modules: words, crm, order, agent, power, third, video, ai, activity, reward, system |
| **[Architecture](wiki/architecture/index.md)** | Architecture decisions, ADRs, cross-module flows | 11 modules: words, crm, order, agent, power, third, video, ai, activity, reward, system |
| **[Specs](wiki/specs/index.md)** | Active or recently-closed openspec proposals | `2026-05-14-crm-spec-*.md` |
| **[Research](wiki/research/index.md)** | Feasibility / discovery reports from `/h-research` (Scenario F) | one per study |
| **[Testing](wiki/testing/index.md)** | Testing standards, evidence requirements | — |
| **[Reviews](wiki/reviews/index.md)** | Review artifacts, PR design reviews, audit reports | — |
| **[Preferences](wiki/preferences/index.md)** | Project constraints, security rules, anti-patterns | `security_rules.md` |
| **[Frontend API](wiki/frontend-api/index.md)** | 前后端接口契约（面向前端消费方）；R\<T\> 封装规范 + 全局错误码 + 各模块接口文档 | `script_monitor.md`, `script_monitor_b5.md` |

## 3. Cold Storage
- **[Archive](archive/index.md)** — archived `openspec.md` files (one per completed Standard task). Date-prefixed for traceability. Source for milestone wiki regeneration.

## 4. Operations
- **[Incidents](incidents/README.md)** — 线上事故 / 客户投诉 / QA 抓到的 bug 事后档案（`failure_memory.py` 第二数据源）。格式见 [incidents/_TEMPLATE.md](incidents/_TEMPLATE.md)，由 `@incident` 快捷符或 `@debugger` 触发写入。
