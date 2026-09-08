#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PreToolUse hook for Claude Code.

Claude Code invokes this script before each matched tool call (Edit, Write) and
passes a JSON payload on stdin. We:

  1. Parse the file_path from the tool input.
  2. Look up the active focus_card.md (silent skip if none — Vibe mode).
  3. Delegate to scope_guard.py with --focus-card + --files.
  4. Exit 0 (allow) or exit 2 (block + reason on stderr — Claude Code surfaces this).

Bypass: CLAUDE_SCOPE_GUARD_BYPASS=1 → always exit 0.

The hook never raises an exception; failures degrade to silent skip so a broken
hook cannot brick the workflow.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

HARNESS_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = HARNESS_DIR.parent
SCOPE_GUARD = SCRIPTS_DIR / "gates" / "scope_guard.py"
FIND_FC = HARNESS_DIR / "find_active_focus_card.py"


def _silent_pass() -> int:
    return 0


def _read_payload() -> dict:
    try:
        raw = sys.stdin.read()
        if not raw.strip():
            return {}
        return json.loads(raw)
    except Exception:
        return {}


def _extract_file_paths(payload: dict) -> list[str]:
    tool_input = payload.get("tool_input") or {}
    paths: list[str] = []
    fp = tool_input.get("file_path")
    if isinstance(fp, str) and fp:
        paths.append(fp)
    # Edit/MultiEdit also have file_path; future-proof for other tools
    edits = tool_input.get("edits")
    if isinstance(edits, list):
        for e in edits:
            if isinstance(e, dict):
                p = e.get("file_path")
                if isinstance(p, str) and p:
                    paths.append(p)
    return paths


def main() -> int:
    if os.environ.get("CLAUDE_SCOPE_GUARD_BYPASS") == "1":
        return _silent_pass()

    payload = _read_payload()
    file_paths = _extract_file_paths(payload)
    if not file_paths:
        return _silent_pass()

    # Find active focus_card; silent skip if none (Vibe mode)
    try:
        out = subprocess.run(
            [sys.executable, str(FIND_FC)],
            capture_output=True,
            text=True,
            timeout=5,
        )
    except subprocess.TimeoutExpired:
        print("[pre_tool_use_hook] find_active_focus_card timed out (>5s); allowing edit", file=sys.stderr)
        return _silent_pass()
    except FileNotFoundError:
        # find_active_focus_card.py was removed/renamed — degrade silently
        return _silent_pass()
    except Exception as e:
        print(f"[pre_tool_use_hook] find_active_focus_card raised {type(e).__name__}: {e}; allowing edit", file=sys.stderr)
        return _silent_pass()

    if out.returncode != 0 or not out.stdout.strip():
        # No active focus card — Vibe mode, silent skip
        return _silent_pass()

    focus_card = out.stdout.strip()

    try:
        result = subprocess.run(
            [sys.executable, str(SCOPE_GUARD), "--focus-card", focus_card, "--files", *file_paths],
            capture_output=True,
            text=True,
            timeout=10,
        )
    except subprocess.TimeoutExpired:
        print("[pre_tool_use_hook] scope_guard timed out (>10s); allowing edit", file=sys.stderr)
        return _silent_pass()
    except FileNotFoundError:
        print(f"[pre_tool_use_hook] scope_guard.py missing at {SCOPE_GUARD}; allowing edit", file=sys.stderr)
        return _silent_pass()
    except Exception as e:
        print(f"[pre_tool_use_hook] scope_guard raised {type(e).__name__}: {e}; allowing edit", file=sys.stderr)
        return _silent_pass()

    if result.returncode == 0:
        # Emit a one-line breadcrumb so the agent can see scope_guard is alive.
        # Without this, success is indistinguishable from a silently-skipped hook
        # — which is exactly how the parser bug (header equality + backticks)
        # hid for months. Cheap signal, huge observability win.
        try:
            fc_short = focus_card.split("/.claude/runs/", 1)[-1]
        except Exception:
            fc_short = focus_card
        print(f"[scope-guard] PASS ({len(file_paths)} file(s) ⊂ {fc_short})", file=sys.stderr)
        return 0
    if result.returncode == 2:
        # FAIL — block the Edit. Claude Code shows stderr to the agent.
        print(result.stdout, file=sys.stderr)
        print(result.stderr, file=sys.stderr)
        return 2
    # WARN (exit 1) → allow but surface notice
    print(result.stdout, file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
