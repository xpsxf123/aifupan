# Workflow Templates — Immutable SSOT

This directory contains the **immutable templates** used to instantiate per-run artifacts
under `.claude/runs/<run_id>/`. Files here are checked in and never mutated by
the agent loop.

## Contract

| Path | Role | Mutability |
|---|---|---|
| `.claude/wiki/templates/*.template.md` | Template (SSOT structure) | **Immutable** — checked in, commit-safe |
| `.claude/runs/<run_id>/<name>.md`  | Instance (per-task state) | **Mutable** — runtime, ignored by commits |
| `.claude/runs/_active.json` | Active-run pointer | **Mutable** — runtime, ignored by commits |
| `.claude/runs/engine_state.json` | Global harness state | Mutable singleton (intentional, not per-task) |

## run_id Convention

Format: `<YYYYMMDD_HHMMSS>_<slug>`

- `YYYYMMDD_HHMMSS`: UTC- or local-time stamp captured when the agent first enters
  the `Explorer` phase of a fresh task.
- `slug`: lowercase kebab-case, ≤ 32 characters, derived from the task intent
  (e.g. `svs_wordlist_fix`, `workflow_isolation`).

Example: `20260520_181500_workflow_isolation`

## Lifecycle

1. **Explorer entry**: agent resolves `run_id`, creates `runs/<run_id>/`, copies every
   template into it filling the placeholders, and writes `_active.json` pointing to the
   new `run_id`.
2. **Phases**: each phase updates files inside `runs/<run_id>/`, never anywhere else.
3. **Archive**: `python3 .claude/scripts/tools/archive_session_artifacts.py --run-id <id>`
   moves the instance's `openspec.md`/`focus_card.md` into `.claude/wiki/archive/`,
   replaces them with read-only pointers, and marks `_active.json` cleared.
4. **Resume**: on session start, agent reads `_active.json`; if `phase` is not `Archived`
   it resumes from the recorded phase using the artifacts in that `run_id` directory.

## Why templates are separate from instances

The previous design overloaded `runs/current_task.md` as both "schema reminder" and
"live task state". A new task overwrote the previous task's progress; resuming a paused
task after starting another was impossible. Separating immutable templates from
per-`run_id` instances eliminates both problems.
