#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Harvest Threshold Scanner.

For each tracked module, count archive entries newer than the module's
`.last_harvest.json` timestamp. If the count >= THRESHOLD (default 3),
recommend the module for a `@harvest` run.

Module discovery: any name X such that `wiki/<area>/X_<area>.md` exists for
at least one of {api, data, domain, architecture}.

Archive → module match: case-insensitive substring of the module name in the
archive filename (e.g., `20260514_crm_spec_a_query_apis.md` → matches `crm`).

State file: `.claude/llm_wiki/wiki/.last_harvest.json`. If absent, treated as
"never harvested" → every archive entry counts.

Exit codes:
- 0: no module meets threshold
- 1: at least one module meets threshold (recommend `@harvest <module>`)
- 2: error (bad args, IO failure)

Usage:
    harvest_threshold.py --all
    harvest_threshold.py --module crm
    harvest_threshold.py --module crm --threshold 5
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import date
from pathlib import Path

WIKI_ROOT = Path(".claude/llm_wiki/wiki")
ARCHIVE_DIR = Path(".claude/llm_wiki/archive")
STATE_FILE = WIKI_ROOT / ".last_harvest.json"
AREAS = ("api", "data", "domain", "architecture")
DEFAULT_THRESHOLD = 3

DATE_RE = re.compile(r"^(\d{8}|\d{4}-\d{2}-\d{2})")


def _discover_modules() -> set[str]:
    """Modules = names X such that wiki/<area>/X_<area>.md exists for any area."""
    modules: set[str] = set()
    for area in AREAS:
        area_dir = WIKI_ROOT / area
        if not area_dir.is_dir():
            continue
        suffix = f"_{area}.md"
        for f in area_dir.glob(f"*{suffix}"):
            name = f.name[: -len(suffix)]
            if name and name != "index":
                modules.add(name)
    return modules


def _load_state() -> dict[str, str]:
    if not STATE_FILE.is_file():
        return {}
    try:
        return json.loads(STATE_FILE.read_text(encoding="utf-8"))
    except Exception:
        return {}


def _normalize_date(token: str) -> str | None:
    """Convert 20260514 or 2026-05-14 to YYYY-MM-DD."""
    if not token:
        return None
    m = DATE_RE.match(token)
    if not m:
        return None
    raw = m.group(1)
    if "-" in raw:
        return raw
    return f"{raw[0:4]}-{raw[4:6]}-{raw[6:8]}"


def _archive_entries() -> list[tuple[str, str]]:
    """Returns list of (normalized_date, filename) for all archive entries."""
    if not ARCHIVE_DIR.is_dir():
        return []
    entries: list[tuple[str, str]] = []
    for f in ARCHIVE_DIR.glob("*.md"):
        if f.name == "index.md":
            continue
        d = _normalize_date(f.name)
        if d:
            entries.append((d, f.name))
    return entries


def _module_count(module: str, since: str | None, entries: list[tuple[str, str]]) -> list[str]:
    """Return archive filenames matching module name, newer than `since`."""
    needle = module.lower()
    matched: list[str] = []
    for d, name in entries:
        if needle not in name.lower():
            continue
        if since and d <= since:
            continue
        matched.append(name)
    return sorted(matched)


def _print_module_report(module: str, since: str | None, matched: list[str], threshold: int) -> bool:
    """Print report for one module. Return True if it meets threshold."""
    count = len(matched)
    meets = count >= threshold
    flag = "≥ threshold ✦" if meets else "below threshold"
    last = since or "never"
    print(f"[{module}] last_harvest={last} new_archives={count} (threshold={threshold}) → {flag}")
    if meets:
        for name in matched:
            print(f"    - {name}")
    return meets


def main() -> int:
    parser = argparse.ArgumentParser()
    g = parser.add_mutually_exclusive_group(required=True)
    g.add_argument("--all", action="store_true", help="scan every discovered module")
    g.add_argument("--module", help="scan one specific module")
    parser.add_argument("--threshold", type=int, default=DEFAULT_THRESHOLD,
                        help=f"min new-archives to recommend harvest (default {DEFAULT_THRESHOLD})")
    args = parser.parse_args()

    state = _load_state()
    entries = _archive_entries()

    if args.module:
        modules = {args.module}
        discovered = _discover_modules()
        if args.module not in discovered:
            print(f"WARN: module '{args.module}' not in discovered set {sorted(discovered)} — "
                  "scanning anyway by filename substring", file=sys.stderr)
    else:
        modules = _discover_modules()
        if not modules:
            print("No modules discovered under wiki/<area>/*.md", file=sys.stderr)
            return 0

    any_meets = False
    for module in sorted(modules):
        since = state.get(module)
        matched = _module_count(module, since, entries)
        meets = _print_module_report(module, since, matched, args.threshold)
        any_meets = any_meets or meets

    if any_meets:
        print("\n→ Recommend: dispatch @knowledge-harvester for the flagged module(s).")
        return 1
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        print(f"ERROR: {type(e).__name__}: {e}", file=sys.stderr)
        sys.exit(2)
