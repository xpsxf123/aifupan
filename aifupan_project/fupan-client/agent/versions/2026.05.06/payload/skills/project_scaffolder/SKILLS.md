# Skill: project_scaffolder

## Summary

Scaffold or initialize a frontend project so it is ready for Trae multi-agent collaboration, with a consistent docs/wiki/skills structure.

This skill supports both:

- build: generate a new project skeleton
- init: join an existing project (already has code) and install the agent doc system

## Inputs

- mode (required): `build_new` | `init_existing`
- project_root (required): absolute path
- frontend_stack (optional): `react` | `vue` | `unknown`
- repo_layout (optional): `src/` | `apps/` | custom
- constraints (optional):
  - existing_conventions: naming/style conventions to keep
  - forbidden_changes: paths that must not be modified
  - package_manager: `npm` | `pnpm` | `yarn` (if known)
- agent_install (optional):
  - install_agents: boolean (default true)
  - install_gate_runner: boolean (default true)
  - writeback_wal: boolean (default true)

## Outputs

- For build_new:
  - a minimal runnable frontend skeleton following the selected stack
  - initial docs/wiki scaffolding aligned with the agent system
- For init_existing:
  - agent management layer installed (or reconciled) into the project:
    - `.trae/` + `智能体模型/` (if missing)
    - `self_skills/` ready for project-bound skills
  - a bootstrap report (what was detected/added/left untouched)

## Hard Rules

- Do not overwrite existing project files unless explicitly allowed by the user
- Prefer keeping the repo’s current conventions; only add the agent system as an additive layer
- When implementing features later:
  - search `self_skills/` first, then use reusable `skills/`

## Recommended One-Click Path

- Use Manager skill `workspace_bootstrapper` templates to install the management layer and industry agents into the target project.
- Then run the front-end learning flow:
  - `legacy_wiki_bootstrapper` to reverse-engineer the project architecture and conventions
  - generate `self_skills/` from existing reusable components (see `component_abstractor`)

