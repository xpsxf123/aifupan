# Skill: component_abstractor

## Summary

Detect reusable components/hooks/utils in a frontend codebase and extract them into:

- public reusable assets under `skills/` when they are generic
- project-bound private assets under `self_skills/` when they are business-specific

The primary output is documentation-first skills (component description + usage docs) to enable forced reuse in later page generation.

## Inputs

- project_root (required): absolute path
- scan_targets (optional):
  - components_dir (default `src/components`)
  - hooks_dir (default `src/hooks`)
  - utils_dir (default `src/utils`)
- classification (optional):
  - public_criteria: when to promote as public skill
  - private_criteria: when to keep as self skill
- output (optional):
  - self_skills_root (default `智能体模型/agent/self_skills`)
  - update_index: boolean (default true)

## Outputs

- A set of skill docs:
  - `self_skills/<skill_name>/SKILLS.md` (+ optional templates/)
  - optionally: a generated catalog file for quick lookup
- A recommendation list:
  - which assets should be promoted to public reusable skills
  - which assets must stay private to the project

## Hard Rules

- For feature/page implementation, enforce reuse priority:
  - 1) search and reuse `self_skills/` with high similarity
  - 2) then search and reuse `skills/`
  - 3) only create new code if no suitable match exists
- Any extracted skill must include:
  - what it is, when to use it, constraints, and source references

## Suggested Automation

- Use Manager skill `workspace_bootstrapper` template `harvest_frontend_self_skills.mjs` to generate initial self skills stubs.
- Then enrich each generated self skill with:
  - props/contracts (if component)
  - hooks signature and side effects (if hook)
  - input/output types and edge cases (if util)

