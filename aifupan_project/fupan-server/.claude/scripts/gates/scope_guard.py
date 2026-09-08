#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Scope Guard (Anti-drift)

This gate prevents the change set from drifting outside allowed scope.
It reads Allowed Scope prefixes from a focus card and compares them against changed files.

Exit codes:
- 0: PASS
- 1: WARN
- 2: FAIL
"""

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

EXIT_WARN = 1
EXIT_FAIL = 2

# Harness infrastructure paths are implicitly allowed regardless of focus_card.
# Rationale: scope_guard is meant to constrain BUSINESS code changes against the
# active task's Allowed Scope. .claude/* are workflow artifacts (run_dirs,
# scripts, rules, agents, skills) — restricting them creates bootstrap deadlocks
# (e.g., cannot write a new run_dir's focus_card.md while a stale one is active).
# Listed prefixes match the harness layout in this repo.
HARNESS_PREFIXES = (
    ".claude/runs/",
    ".claude/scripts/",
    ".claude/rules/",
    ".claude/agents/",
    ".claude/skills/",
    ".claude/commands/",
    ".claude/workflow/",
    ".claude/llm_wiki/",
    ".claude/settings.json",
    ".claude/settings.local.json",
)


def _repo_root() -> str:
    """Return repo root absolute path with forward slashes, or '' if not in a git repo."""
    try:
        out = subprocess.check_output(
            ["git", "rev-parse", "--show-toplevel"],
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
        return out.replace("\\", "/")
    except Exception:
        return ""


def _to_repo_relative(p: str, repo_root: str) -> str:
    """Normalize a file path to repo-relative form (forward slashes).
    Accepts either absolute paths (from PreToolUse hook) or relative paths
    (from `git diff --name-only`); returns repo-relative either way."""
    p = p.replace("\\", "/")
    if repo_root and os.path.isabs(p):
        rr = repo_root.rstrip("/") + "/"
        if p.startswith(rr):
            return p[len(rr):]
    return p


def _read_allowed_prefixes(focus_card_path: str) -> list[str]:
    with open(focus_card_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    in_section = False
    prefixes: list[str] = []
    for line in lines:
        stripped = line.strip()
        # Accept decorated headers like "## Allowed Scope (file whitelist)".
        if stripped == "## Allowed Scope" or stripped.startswith("## Allowed Scope "):
            in_section = True
            continue
        if in_section and line.startswith("## "):
            break
        if in_section:
            if stripped.startswith("-"):
                v = stripped.lstrip("-").strip()
                # 兼容多种写法：`- `path``、`- `path` — 说明`、`- path`、`- path — 说明`
                # 有反引号则取反引号内路径，否则去掉行尾 ` — 说明`
                m = re.search(r"`([^`]+)`", v)
                if m:
                    v = m.group(1).strip()
                else:
                    v = v.split(" — ")[0].strip()
                if v:
                    prefixes.append(v)
    return prefixes


def _git_changed_files() -> list[str]:
    try:
        out = subprocess.check_output(["git", "diff", "--name-only"], text=True).strip()
        if out:
            return [x.strip() for x in out.splitlines() if x.strip()]
    except Exception:
        pass
    try:
        out = subprocess.check_output(["git", "diff", "--name-only", "--cached"], text=True).strip()
        if out:
            return [x.strip() for x in out.splitlines() if x.strip()]
    except Exception:
        return []
    return []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--focus-card", required=True)
    parser.add_argument("--files", nargs="*", default=None, help="explicit file list; if omitted, uses git diff")
    parser.add_argument("--allow-prefix", action="append", default=[], help="additional allowed prefixes")
    args = parser.parse_args()

    if not os.path.exists(args.focus_card):
        print(f"FAIL: focus card not found: {args.focus_card}")
        return EXIT_FAIL

    raw_allowed = _read_allowed_prefixes(args.focus_card) + args.allow_prefix
    allowed_prefixes: list[str] = []
    allowed_exact: set[str] = set()
    for a in raw_allowed:
        a = a.strip()
        if not a:
            continue
        # Trailing slash → directory prefix.
        # No trailing slash → exact match (whether the entry contains `/` or not).
        # Fixes Bug 2: single-file scope (e.g., `.claude/agents/foo.md`) is now
        # expressible. Previously the entry was rewritten to `.claude/agents/foo.md/`
        # — a non-existent directory prefix that matched nothing, so no file-level
        # scope could ever pass.
        if a.endswith("/"):
            allowed_prefixes.append(a)
        else:
            allowed_exact.add(a)

    if not allowed_prefixes and not allowed_exact:
        print("WARN: no allowed scope prefixes found; cannot enforce scope")
        return EXIT_WARN

    files = args.files if args.files is not None and len(args.files) > 0 else _git_changed_files()
    repo_root = _repo_root()
    normalized_files: list[str] = []
    for item in files:
        if item is None:
            continue
        parts = [p.strip() for p in item.split(",")] if "," in item else [item.strip()]
        for p in parts:
            if p:
                # Bug 3 fix: PreToolUse hook passes absolute paths from
                # Claude Code's tool_input.file_path; git diff yields relative.
                # Normalize both to repo-relative so startswith() compares apples
                # to apples against focus_card prefixes (which are relative).
                normalized_files.append(_to_repo_relative(p, repo_root))
    files = normalized_files
    if not files:
        print("OK: no changed files detected")
        return 0

    violations = []
    for f in files:
        if f.startswith(HARNESS_PREFIXES):
            continue
        if f in allowed_exact:
            continue
        # 兼容 focus_card 写 module 相对路径（如 src/main/java/...），
        # 而 changed file 为 repo 相对（如 replay-words/src/main/java/...）：后缀匹配
        if any(f == a or f.endswith("/" + a) for a in allowed_exact):
            continue
        if any(f.startswith(p) for p in allowed_prefixes):
            continue
        violations.append(f)

    if violations:
        print("FAIL: scope guard detected out-of-scope changes")
        print("Allowed exact:")
        for p in sorted(allowed_exact):
            print(f"- {p}")
        print("Allowed prefixes:")
        for p in allowed_prefixes:
            print(f"- {p}")
        print("Violations:")
        for v in violations:
            print(f"- {v}")
        return EXIT_FAIL

    print("OK: scope guard pass")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
