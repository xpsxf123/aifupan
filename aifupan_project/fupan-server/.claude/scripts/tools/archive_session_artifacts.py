#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import os
import shutil
from datetime import datetime


def _ensure_dir(path: str):
    os.makedirs(path, exist_ok=True)


def _move_if_exists(src: str, dst: str) -> bool:
    if not os.path.exists(src):
        return False
    _ensure_dir(os.path.dirname(dst))
    shutil.move(src, dst)
    return True


def _write_pointer(path: str, archived_path: str):
    _ensure_dir(os.path.dirname(path))
    with open(path, "w", encoding="utf-8") as f:
        f.write("# Archived\n\n")
        f.write("This file is archived. Do not use it as active working memory.\n\n")
        f.write(f"- Archived to: {archived_path}\n")


def _archive_file(runs_dir: str, archive_dir: str, prefix: str, slug: str, file_name: str, pointer: bool):
    src = os.path.join(runs_dir, file_name)
    dst = os.path.join(archive_dir, f"{prefix}__{slug}__{file_name}")
    moved = _move_if_exists(src, dst)
    if moved and pointer:
        _write_pointer(src, dst)
    return moved


def _list_launch_specs(runs_dir: str):
    if not os.path.isdir(runs_dir):
        return []
    return sorted([f for f in os.listdir(runs_dir) if f.startswith("launch_spec_") and f.endswith(".md")])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--slug", required=True, help="Feature slug, e.g. live_room_batch_schedule_query")
    parser.add_argument("--date", default=datetime.now().strftime("%Y%m%d"))
    parser.add_argument("--prefix", default=None, help="Archive prefix override, e.g. Change__2026-04-26_16-15-03")
    parser.add_argument("--run-dir", default=".claude/runs", help="Run directory, e.g. .claude/runs/Change__2026-04-26_16-15-03")
    args = parser.parse_args()

    runs_dir = args.run_dir
    archive_dir = ".claude/llm_wiki/archive"
    _ensure_dir(archive_dir)

    if args.prefix is not None:
        prefix = args.prefix
    else:
        base_name = os.path.basename(os.path.normpath(runs_dir))
        prefix = base_name if "__" in base_name else args.date

    moved_openspec = _archive_file(runs_dir, archive_dir, prefix, args.slug, "openspec.md", True)
    moved_focus = _archive_file(runs_dir, archive_dir, prefix, args.slug, "focus_card.md", True)
    moved_current_task = _archive_file(runs_dir, archive_dir, prefix, args.slug, "current_task.md", True)
    moved_explore_report = _archive_file(runs_dir, archive_dir, prefix, args.slug, "explore_report.md", True)

    moved_launch_specs = False
    for file_name in _list_launch_specs(runs_dir):
        moved_launch_specs = _archive_file(runs_dir, archive_dir, prefix, args.slug, file_name, False) or moved_launch_specs

    if not moved_openspec and not moved_focus and not moved_current_task and not moved_explore_report and not moved_launch_specs:
        raise SystemExit(f"No session artifacts found under {runs_dir}")


if __name__ == "__main__":
    main()
