# Schema Index

This domain contains global contract templates and the minimal links you need to apply them correctly in the lifecycle.

This file is intentionally English-only to maximize agent execution reliability.

## Quick Start (Recommended)
1. Read the contract template to learn the required document structure.
2. Read the process links to learn where the contract is checked, frozen, and enforced.

## Templates
- **[OpenSpec Schema](openspec_schema.md)**: the proposal contract. It can also carry optional handoff sections (API contract + acceptance criteria) when collaboration is needed.
- **[Sub-Agent Contract Schema](subagent_contract_schema.md)**: the minimal executable contract template for dispatching tasks to sub-agents.

## Process Links (Do not duplicate rules here)
- **[Lifecycle](../../rules/lifecycle.md)**: phase definitions, Profile→Phase mapping, Mounted Roles, Approval Gate.
- **[Policy](../../rules/policy.md)**: hooks, budgets, anti-bloat thresholds, commit policy.
- **[Risk Profiles](../../rules/risk-profiles.md)**: 4-tier risk classification, danger keywords, Triage Probe scoring, shortcut DSL.
- **[Dispatch Template](../../rules/dispatch-template.md)**: 5-section sub-agent dispatch contract.

## Link Rules
- Links inside this repo MUST use relative paths from the current file. Do not hardcode `.claude/` into relative links.
