#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Find the active focus_card.md for the current task.

Resolution order:
1. `CLAUDE_ACTIVE_FOCUS_CARD` env var (explicit override)
2. Latest `.claude/runs/<intent>__*/focus_card.md` by run_dir mtime (Standard/PATCH mode)

A focus_card is considered ACTIVE iff:
  - It contains a `## Allowed Scope` section
  - At least one `-` entry in that section is concrete (not a `<...>` placeholder)
  - It does not contain the literal template markers `<path or prefix>` or
    `<file path 1>` (template state)

Prints absolute path to stdout, or empty + exit 1 if none found.
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
RUNS_DIR = REPO_ROOT / ".claude" / "runs"
TEMPLATE_MARKERS = ("<path or prefix>", "<file path 1>", "<file path 2>")


def _is_active_focus_card(path: Path) -> bool:
    """True iff focus_card has at least one concrete entry under `## Allowed Scope`
    AND lacks template placeholder markers."""
    try:
        text = path.read_text(encoding="utf-8")
    except Exception:
        return False
    if any(marker in text for marker in TEMPLATE_MARKERS):
        return False
    in_section = False
    for line in text.splitlines():
        stripped = line.strip()
        # Accept decorated headers like "## Allowed Scope (file whitelist)".
        if stripped == "## Allowed Scope" or stripped.startswith("## Allowed Scope "):
            in_section = True
            continue
        if in_section and line.startswith("## "):
            break
        if in_section:
            if stripped.startswith("-"):
                # Strip leading "-", surrounding whitespace, and Markdown backticks.
                entry = stripped.lstrip("-").strip().strip("`").strip()
                # Reject placeholder entries like "<path>" or empty
                if entry and not (entry.startswith("<") and entry.endswith(">")):
                    return True
    return False


def _find_standard_run_dir_focus_card() -> Path | None:
    """Newest `.claude/runs/<intent>__*/focus_card.md` by run_dir mtime."""
    if not RUNS_DIR.is_dir():
        return None
    candidates: list[tuple[float, Path]] = []
    for child in RUNS_DIR.iterdir():
        if not child.is_dir():
            continue
        fc = child / "focus_card.md"
        if fc.is_file() and _is_active_focus_card(fc):
            candidates.append((child.stat().st_mtime, fc))
    if not candidates:
        return None
    candidates.sort(reverse=True)
    return candidates[0][1]


def find_active() -> Path | None:
    # Tier 1: explicit env var override
    override = os.environ.get("CLAUDE_ACTIVE_FOCUS_CARD")
    if override:
        p = Path(override)
        return p if p.exists() else None

    # Tier 2: Standard/PATCH mode run_dir
    fc = _find_standard_run_dir_focus_card()
    if fc is not None:
        return fc

    return None


def main() -> int:
    fc = find_active()
    if fc is None:
        return 1
    print(str(fc))
    return 0


if __name__ == "__main__":
    sys.exit(main())
