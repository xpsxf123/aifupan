#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PostToolUse hook for Claude Code.

Runs secrets_linter on changed files after every Edit/Write. Findings appear on
stderr but do NOT block (exit 0) — secrets are caught later by the
security-sentinel Archive gate. Goal here: surface fast feedback during edits.

Set CLAUDE_POST_HOOK_BLOCK=1 to upgrade findings to a hard block (exit 2).
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

HARNESS_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = HARNESS_DIR.parent
SECRETS_LINTER = SCRIPTS_DIR / "gates" / "secrets_linter.py"

# Only scan paths in these prefixes (avoid noisy false-positives on test fixtures)
SCANNED_PREFIXES = (
    "src/main/",
    "src/test/",
    ".claude/",
    "replay-",
    "sql/",
)


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
    edits = tool_input.get("edits")
    if isinstance(edits, list):
        for e in edits:
            if isinstance(e, dict):
                p = e.get("file_path")
                if isinstance(p, str) and p:
                    paths.append(p)
    return paths


def _should_scan(path: str) -> bool:
    # Match SCANNED_PREFIXES tokens as substrings; accept absolute, relative, or bare paths.
    normalized = path.replace("\\", "/")
    return any(tok in normalized for tok in SCANNED_PREFIXES)


def main() -> int:
    payload = _read_payload()
    file_paths = [p for p in _extract_file_paths(payload) if _should_scan(p)]
    if not file_paths:
        return 0

    try:
        result = subprocess.run(
            [sys.executable, str(SECRETS_LINTER), "--paths", *file_paths],
            capture_output=True,
            text=True,
            timeout=15,
        )
    except subprocess.TimeoutExpired:
        print("[post_tool_use_hook] secrets_linter timed out (>15s); skipping", file=sys.stderr)
        return 0
    except FileNotFoundError:
        print(f"[post_tool_use_hook] secrets_linter.py missing at {SECRETS_LINTER}; skipping", file=sys.stderr)
        return 0
    except Exception as e:
        print(f"[post_tool_use_hook] secrets_linter raised {type(e).__name__}: {e}; skipping", file=sys.stderr)
        return 0

    if result.returncode == 0:
        # Observability breadcrumb — see pre_tool_use_hook.py for rationale.
        print(f"[secrets-linter] PASS ({len(file_paths)} file(s) scanned)", file=sys.stderr)
        return 0

    # Surface findings on stderr
    if result.stdout:
        print(result.stdout, file=sys.stderr)
    if result.stderr:
        print(result.stderr, file=sys.stderr)

    if os.environ.get("CLAUDE_POST_HOOK_BLOCK") == "1" and result.returncode == 2:
        return 2
    # Default: do not block edits on secrets — Archive gate will catch.
    return 0


if __name__ == "__main__":
    sys.exit(main())
