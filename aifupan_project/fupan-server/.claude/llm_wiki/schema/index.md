# Schema Index

This domain contains global contract templates and the minimal links you need to apply them correctly in the lifecycle.

This file is intentionally English-only to maximize agent execution reliability.

## Quick Start (Recommended)
1. Read the contract template to learn the required document structure.
2. Read the process links to learn where the contract is checked, frozen, and enforced.

## Templates
- **[OpenSpec Schema](openspec_schema.md)**: the proposal contract. It can also carry optional handoff sections (API contract + acceptance criteria) when collaboration is needed.
- **[Sub-Agent Contract Schema](subagent_contract_schema.md)**: the minimal executable contract template for dispatching tasks to sub-agents (e.g., Trae, Qoder, search).

## Process Links (Do not duplicate rules here)
- **[Lifecycle & Routing](../../rules/lifecycle.md)**: routing → profiles → phase details → hooks (single SSOT for the state machine).
- **[Policy](../../rules/policy.md)**: safety constraints, commit rules, write-back protocol, sub-agent dispatch contract.
- **[Dispatch Template](../../rules/dispatch-template.md)**: mandatory payload format when invoking sub-agents.
- **[Skill Precedence](../../rules/skill-precedence.md)**: which skills run in which phase, what's mutually exclusive.

## Link Rules
- Links inside this repo MUST use relative paths from the current file. Do not hardcode `.claude/` into relative links.
