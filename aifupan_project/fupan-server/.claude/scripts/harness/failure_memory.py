#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Failure-memory reflex injector.

Two data sources, merged by date, top-N surfaced to stderr on UserPromptSubmit:

  1. `.claude/llm_wiki/archive/index.md` — rows whose summary matches failure
     keywords (Fix / Hotfix / 修复 / 回炉 / Bugfix / Regression). Source of
     internally-discovered & archived bugs.

  2. `.claude/llm_wiki/incidents/*.md` — standalone markdown files with
     frontmatter (date / area / severity / source / status) + an H1 title.
     Source of EXTERNAL truth (prod alerts, customer reports, QA-found,
     post-mortems). Closes the self-selection bias of source #1.
     Convention: see `.claude/llm_wiki/incidents/README.md`.

Both sources filtered to a 14-day window. Top 3 most-recent entries surfaced.

Implements behavior declared in `.claude/rules/lifecycle.md` Part 6:
"injects recurring failure patterns into context (silent if none)".

Silent on:
  - CLAUDE_FAILURE_MEMORY_QUIET=1
  - No matching entries
  - Fired within last 30 minutes (cooldown — avoids re-injection in same session)
  - archive/index.md missing / unreadable (degrades to incidents-only)
  - incidents/ missing (degrades to archive-only)
  - State file unreadable (degrades silently)

Token budget: ≤ ~200 tokens per emit (3 entries × ~50 + 2 lines header/footer).
Importable: call `emit_if_due()` from sibling hook scripts.
"""
from __future__ import annotations

import json
import os
import re
import sys
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
ARCHIVE_INDEX = REPO_ROOT / ".claude" / "llm_wiki" / "archive" / "index.md"
INCIDENTS_DIR = REPO_ROOT / ".claude" / "llm_wiki" / "incidents"
STATE_FILE = REPO_ROOT / ".claude" / "runs" / ".failure_memory_state.json"

LOOKBACK_DAYS = 14
MAX_ENTRIES = 3
COOLDOWN_SECONDS = 30 * 60  # 30 min — silent within an active session, re-fires on fresh session

# Failure-class keywords. "PATCH" deliberately excluded (workflow ops, not bug fixes).
# `\b` boundary keeps Chinese keywords clean against following CJK punctuation.
FAILURE_PATTERN = re.compile(
    r"(?:^|[\s\*`])(Fix|Hotfix|修复|回炉|Bugfix|Regression)\b[:：]?",
    re.IGNORECASE,
)


def _parse_date(s: str) -> datetime | None:
    s = s.strip()
    for fmt in ("%Y-%m-%d", "%Y%m%d"):
        try:
            return datetime.strptime(s, fmt).replace(tzinfo=timezone.utc)
        except ValueError:
            continue
    return None


def _within_cooldown() -> bool:
    if not STATE_FILE.exists():
        return False
    try:
        last = float(json.loads(STATE_FILE.read_text(encoding="utf-8")).get("last_fired_ts", 0))
    except Exception:
        return False
    return (time.time() - last) < COOLDOWN_SECONDS


def _update_state() -> None:
    try:
        STATE_FILE.parent.mkdir(parents=True, exist_ok=True)
        STATE_FILE.write_text(
            json.dumps({"last_fired_ts": time.time()}, ensure_ascii=False),
            encoding="utf-8",
        )
    except Exception:
        pass  # advisory — state-write failure must not break the hook


def _scan_archive(cutoff: datetime) -> list[tuple[datetime, str, str, str]]:
    """Scan archive/index.md. Returns (date_obj, date_str, source_tag, summary)."""
    if not ARCHIVE_INDEX.is_file():
        return []
    try:
        text = ARCHIVE_INDEX.read_text(encoding="utf-8")
    except Exception:
        return []

    out: list[tuple[datetime, str, str, str]] = []
    for line in text.splitlines():
        if not line.startswith("|"):
            continue
        cols = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cols) < 3:
            continue
        date_str, summary = cols[0], cols[1]
        d = _parse_date(date_str)
        if d is None or d < cutoff:
            continue
        if not FAILURE_PATTERN.search(summary):
            continue
        out.append((d, date_str, "archive", summary))
    return out


def _parse_frontmatter(text: str) -> dict[str, str]:
    """Minimal YAML frontmatter parser — extracts top-level `key: value` lines.
    Pure stdlib (no PyYAML dep). Returns {} if no frontmatter detected.

    Strips surrounding single/double quotes from values so `date: "2026-05-22"`
    yields `2026-05-22` (raw form expected by downstream `_parse_date`)."""
    if not text.startswith("---"):
        return {}
    end = text.find("\n---", 3)
    if end < 0:
        return {}
    block = text[3:end]
    out: dict[str, str] = {}
    for line in block.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or ":" not in line:
            continue
        k, _, v = line.partition(":")
        v = v.strip()
        # Strip matching surrounding quotes (single or double).
        if len(v) >= 2 and v[0] == v[-1] and v[0] in ("'", '"'):
            v = v[1:-1]
        out[k.strip()] = v
    return out


def _scan_incidents(cutoff: datetime) -> list[tuple[datetime, str, str, str]]:
    """Scan incidents/*.md. Returns (date_obj, date_str, source_tag, title).
    source_tag format: 'incident@<area>'. Files starting with '_' or README are skipped."""
    if not INCIDENTS_DIR.is_dir():
        return []
    out: list[tuple[datetime, str, str, str]] = []
    for path in INCIDENTS_DIR.glob("*.md"):
        name = path.name
        if name.startswith("_") or name.lower() == "readme.md":
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except Exception:
            continue
        fm = _parse_frontmatter(text)
        d = _parse_date(fm.get("date", ""))
        if d is None or d < cutoff:
            continue
        area = fm.get("area", "unknown")
        # First H1 line after frontmatter = title
        body = text[text.find("\n---", 3) + 4 :] if text.startswith("---") else text
        m = re.search(r"^#\s+(.+)$", body, re.MULTILINE)
        title = m.group(1).strip() if m else path.stem
        out.append((d, fm["date"], f"incident@{area}", title))
    return out


def get_recent_failures() -> list[tuple[str, str, str]]:
    """Return up to MAX_ENTRIES (date_str, source_tag, summary) tuples, newest first.
    Merges archive Fix rows + incidents/*.md."""
    cutoff = datetime.now(timezone.utc) - timedelta(days=LOOKBACK_DAYS)
    merged = _scan_archive(cutoff) + _scan_incidents(cutoff)
    merged.sort(key=lambda x: x[0], reverse=True)
    return [(date_str, source, summary) for _, date_str, source, summary in merged[:MAX_ENTRIES]]


def _clean(summary: str, max_len: int = 100) -> str:
    """Strip markdown noise and truncate for stderr display."""
    s = re.sub(r"`([^`]+)`", r"\1", summary)
    s = re.sub(r"\*\*([^*]+)\*\*", r"\1", s)
    s = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", s)
    return s if len(s) <= max_len else s[: max_len - 1] + "…"


def emit_if_due() -> bool:
    """
    Emit failure-memory block to stderr if not in cooldown and matches exist.
    Returns True if anything was emitted (caller can chain). Never raises.
    """
    if os.environ.get("CLAUDE_FAILURE_MEMORY_QUIET") == "1":
        return False
    if _within_cooldown():
        return False
    entries = get_recent_failures()
    if not entries:
        return False

    print(
        f"[failure-memory] Recent mistakes to reflex-check (last {LOOKBACK_DAYS} days, top {len(entries)}):",
        file=sys.stderr,
    )
    for date_str, source, summary in entries:
        print(f"  - {date_str} [{source}]: {_clean(summary)}", file=sys.stderr)
    print(
        "[failure-memory] If your task touches the same area, verify the prior fix still holds. "
        "Incident details: .claude/llm_wiki/incidents/<file>.md",
        file=sys.stderr,
    )
    _update_state()
    return True


def main() -> int:
    emit_if_due()
    return 0


if __name__ == "__main__":
    sys.exit(main())
