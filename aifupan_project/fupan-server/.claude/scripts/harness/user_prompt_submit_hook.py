#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
UserPromptSubmit hook for Claude Code.

Surfaces two tiny context blocks:
  1. Active intent/profile when a launch_spec exists (silent in Vibe mode).
  2. Failure-memory reflex reminders (delegated to failure_memory.py, silent
     when no matches or within 30-min cooldown).

Token budget: < 300 tokens combined, well under the 5-minute prompt-cache window.

Set CLAUDE_USER_PROMPT_HOOK_QUIET=1 to disable BOTH blocks.
Set CLAUDE_FAILURE_MEMORY_QUIET=1 to disable failure memory only.
"""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

# Sibling import: failure_memory.py lives in this same harness directory.
sys.path.insert(0, str(Path(__file__).resolve().parent))
try:
    from failure_memory import emit_if_due as _emit_failure_memory
except Exception:  # degrade silently if sibling missing/broken
    _emit_failure_memory = lambda: False  # noqa: E731

REPO_ROOT = Path(__file__).resolve().parents[3]
RUNS_DIR = REPO_ROOT / ".claude" / "runs"


def _latest_launch_spec() -> Path | None:
    if not RUNS_DIR.is_dir():
        return None
    candidates = sorted(
        (p for p in RUNS_DIR.rglob("launch_spec_*.md") if p.is_file()),
        key=lambda p: p.stat().st_mtime,
        reverse=True,
    )
    return candidates[0] if candidates else None


def _extract_active_row(spec: Path) -> tuple[str, str, str] | None:
    """Return (intent, status, phase) for first IN_PROGRESS or WAITING_APPROVAL row."""
    try:
        text = spec.read_text(encoding="utf-8")
    except Exception:
        return None
    for line in text.splitlines():
        if not line.startswith("|"):
            continue
        parts = [p.strip() for p in line.strip().strip("|").split("|")]
        if len(parts) < 3:
            continue
        intent, status, phase = parts[0], parts[1], parts[2]
        if status in ("IN_PROGRESS", "WAITING_APPROVAL"):
            return intent, status, phase
    return None


def main() -> int:
    if os.environ.get("CLAUDE_USER_PROMPT_HOOK_QUIET") == "1":
        return 0

    # Block 1 — active intent/profile (only when a launch_spec is in flight)
    spec = _latest_launch_spec()
    if spec is not None:
        row = _extract_active_row(spec)
        if row is not None:
            intent, status, phase = row
            rel = spec.relative_to(REPO_ROOT)
            print(
                f"[harness] active: intent={intent} status={status} phase={phase} ({rel})",
                file=sys.stderr,
            )
            if status == "WAITING_APPROVAL":
                print(
                    "[harness] WAITING_APPROVAL — present openspec.md Human Section to user and wait for explicit approval before any code edit.",
                    file=sys.stderr,
                )

    # Block 2 — failure memory (always evaluated; silent on no-match / cooldown / QUIET)
    try:
        _emit_failure_memory()
    except Exception:
        pass  # advisory — never break the hook

    return 0


if __name__ == "__main__":
    sys.exit(main())
