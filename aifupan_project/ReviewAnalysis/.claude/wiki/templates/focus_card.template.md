# Focus Card — <task title>

Run ID: `<YYYYMMDD_HHMMSS>_<slug>`
Mounted Role: `@Focus Guard`

## Authorized files (boundary)
- `<absolute or repo-relative path>`
- `<...>`

## Forbidden files / directories
- `<paths that are out of scope>`
- `<...>`

## Boundary exception policy
If a need arises to touch a file outside this card:
1. STOP. Do not modify it.
2. Output a `[Boundary Exception Request]` block describing the file and reason.
3. Wait for explicit human approval before extending the boundary.

## Validation hook
- `python3 .claude/scripts/gates/scope_guard.py --focus-card <this file>`
  Run before declaring Implement complete.
