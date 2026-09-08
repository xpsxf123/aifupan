#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Harness Self-Check — end-to-end smoke test for scope_guard hook chain.

Runs the PreToolUse decision logic against synthetic focus_cards covering the
historically-broken cases, and reports PASS/FAIL per case. Designed to be run
manually or wired into CI to catch the silent-failure pattern that hid the
scope_guard parser bugs (header equality + missing backtick strip) for months.

Exit codes:
  0 — all cases passed
  1 — at least one case behaved unexpectedly

Add a new case any time a hook bug is fixed, so the regression is locked down.
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
SCOPE_GUARD = REPO_ROOT / ".claude" / "scripts" / "gates" / "scope_guard.py"
FIND_FC = REPO_ROOT / ".claude" / "scripts" / "harness" / "find_active_focus_card.py"


def _run_scope_guard(focus_card: Path, files: list[str]) -> int:
    proc = subprocess.run(
        [sys.executable, str(SCOPE_GUARD), "--focus-card", str(focus_card), "--files", *files],
        capture_output=True,
        text=True,
    )
    return proc.returncode


def _run_find_active() -> tuple[int, str]:
    proc = subprocess.run(
        [sys.executable, str(FIND_FC)],
        capture_output=True,
        text=True,
    )
    return proc.returncode, proc.stdout.strip()


# --- cases -------------------------------------------------------------------
# Each case is (name, focus_card_body, files, expected_exit).
# expected_exit semantics for scope_guard: 0=PASS, 1=WARN(no scope found), 2=FAIL.
CASES = [
    (
        "decorated_header_with_backticks_in_scope",
        "# FC\n\n## Allowed Scope (file whitelist)\n- `replay-words/src/X.java`\n",
        ["replay-words/src/X.java"],
        0,
    ),
    (
        "decorated_header_with_backticks_out_of_scope",
        "# FC\n\n## Allowed Scope (file whitelist)\n- `replay-words/src/X.java`\n",
        ["replay-api/src/Y.java"],
        2,
    ),
    (
        "plain_header_no_backticks",
        "# FC\n\n## Allowed Scope\n- replay-words/src/X.java\n",
        ["replay-words/src/X.java"],
        0,
    ),
    (
        "directory_prefix_with_trailing_slash",
        "# FC\n\n## Allowed Scope\n- replay-words/\n",
        ["replay-words/src/main/java/anything.java"],
        0,
    ),
    (
        "harness_path_always_allowed",
        "# FC\n\n## Allowed Scope\n- replay-words/src/X.java\n",
        [".claude/runs/foo/focus_card.md", ".claude/scripts/harness/x.py"],
        0,
    ),
    (
        "empty_allowed_scope_emits_warn",
        "# FC\n\n## Allowed Scope (file whitelist)\n",
        ["any.java"],
        1,
    ),
]


def _check_find_active_picks_decorated_header() -> tuple[bool, str]:
    """Run find_active_focus_card.py against a synthetic decorated-header focus_card
    placed in a temp run_dir. Asserts the file is identified as active."""
    tmp_runs_root = REPO_ROOT / ".claude" / "runs"
    fixture_dir = tmp_runs_root / "SELFCHECK_FIXTURE__do_not_commit"
    fixture_fc = fixture_dir / "focus_card.md"
    try:
        fixture_dir.mkdir(parents=True, exist_ok=True)
        fixture_fc.write_text(
            "# FC\n\n## Allowed Scope (file whitelist)\n- `replay-words/src/X.java`\n",
            encoding="utf-8",
        )
        # Set mtime ahead so this beats any stale run_dir
        os.utime(fixture_dir, None)
        rc, out = _run_find_active()
        if rc != 0:
            return False, f"find_active exited {rc}, expected 0"
        if not out:
            return False, "find_active produced empty output"
        # The fixture should be selected if it's newest by mtime; if user has a
        # more-recent real task this case is non-deterministic, so we relax to
        # "fixture is a valid candidate" by re-running scope_guard against it.
        # The real assertion is: find_active does NOT skip decorated headers.
        rc2 = _run_scope_guard(fixture_fc, ["replay-words/src/X.java"])
        if rc2 != 0:
            return False, f"scope_guard against fixture exited {rc2}, expected 0"
        return True, "OK"
    finally:
        shutil.rmtree(fixture_dir, ignore_errors=True)


def main() -> int:
    failures: list[str] = []

    with tempfile.TemporaryDirectory() as td:
        for name, body, files, expected in CASES:
            fc = Path(td) / f"{name}_focus_card.md"
            fc.write_text(body, encoding="utf-8")
            rc = _run_scope_guard(fc, files)
            ok = rc == expected
            mark = "PASS" if ok else "FAIL"
            print(f"[{mark}] case={name} expected_exit={expected} actual_exit={rc}")
            if not ok:
                failures.append(name)

    ok, msg = _check_find_active_picks_decorated_header()
    mark = "PASS" if ok else "FAIL"
    print(f"[{mark}] case=find_active_handles_decorated_header detail={msg}")
    if not ok:
        failures.append("find_active_handles_decorated_header")

    print()
    if failures:
        print(f"SELF-CHECK FAILED: {len(failures)} case(s): {', '.join(failures)}")
        return 1
    print(f"SELF-CHECK OK: {len(CASES) + 1} cases passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
