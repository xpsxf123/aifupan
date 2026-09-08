#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
LLM Wiki 图谱体检工具 (Wiki Linter)
Agent 可以在修改了大规模目录或索引后，可选地调用此脚本。
该脚本只做诊断，绝不修改文件。
"""

import os
import re
import glob
import sys

WIKI_DIR = ".claude/llm_wiki"

EXIT_FAIL = 2

# 文件名规则：仅允许 [a-zA-Z0-9._-]（与 .claude/scripts/gates/filename_linter.py 一致）
# 详见 filename_linter.py 注释；这里复制规则避免子进程调用开销
FILENAME_PATTERN = re.compile(r'^[a-zA-Z0-9._-]+$')


def check_wiki():
    if not os.path.exists(WIKI_DIR):
        print(f"❌ 目录不存在: {WIKI_DIR}")
        return EXIT_FAIL

    all_md_files = glob.glob(f"{WIKI_DIR}/**/*.md", recursive=True)
    all_md_files = [os.path.normpath(f) for f in all_md_files]
    
    oversized_files = []
    dead_links = []
    referenced_files = set()
    bad_filenames = []  # [(path, reason)] — 文件名规则违规

    # 扫描所有目录 / 文件名（不仅 .md，也扫子目录名）
    for dirpath, dirnames, filenames in os.walk(WIKI_DIR):
        for d in dirnames:
            if not FILENAME_PATTERN.match(d):
                bad_filenames.append((os.path.join(dirpath, d) + os.sep,
                                      "目录名违规（仅允许 [a-zA-Z0-9._-]）"))
        for f in filenames:
            if not FILENAME_PATTERN.match(f):
                bad_filenames.append((os.path.join(dirpath, f),
                                      "文件名违规（仅允许 [a-zA-Z0-9._-]）"))

    # 匹配 [文本](链接) 和 [[链接]]
    link_pattern = re.compile(r'\[.*?\]\((.*?\.md)(?:#.*?)?\)')
    wikilink_pattern = re.compile(r'\[\[(.*?\.md)(?:\|.*?)?\]\]')

    for file_path in all_md_files:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        # Check size (500 行防膨胀预警)
        if len(lines) > 500:
            oversized_files.append((file_path, len(lines)))
            
        # Check links
        content = "".join(lines)
        links = link_pattern.findall(content) + wikilink_pattern.findall(content)
        
        base_dir = os.path.dirname(file_path)
        for link in links:
            if link.startswith('http'):
                continue
            
            # 解析相对路径
            target_path = os.path.normpath(os.path.join(base_dir, link))
            if not os.path.exists(target_path):
                # 尝试当作相对于 WIKI_DIR 的绝对路径
                alt_path = os.path.normpath(os.path.join(WIKI_DIR, link.lstrip('/')))
                if os.path.exists(alt_path):
                    target_path = alt_path
                else:
                    dead_links.append((file_path, link))
                    continue
            referenced_files.add(target_path)

    # Check orphans (没有被其他文件引用，且不是核心索引文件的 md)
    # WAL fragments are intentionally append-only and may be unreferenced until compaction.
    orphans = []
    core_files = ["KNOWLEDGE_GRAPH.md", "purpose.md", "index.md"]
    for f in all_md_files:
        if os.path.sep + "wal" + os.path.sep in f:
            continue
        is_core = any(f.endswith(core) for core in core_files)
        if not is_core and f not in referenced_files:
            orphans.append(f)

    print("📊 === LLM Wiki 图谱体检报告 ===")
    print(f"扫描文件总数: {len(all_md_files)}")
    
    print("\n⚠️  【超长文件预警】 (>500行，建议按业务拆分):")
    if oversized_files:
        for f, lines in oversized_files:
            print(f"  - [WARN] {f} ({lines} 行)")
    else:
        print("  ✅ 无超长文件")

    print("\n🔗  【死链检测】:")
    if dead_links:
        for src, link in dead_links:
            print(f"  - [FAIL] [{src}] 包含了无效链接 -> '{link}'")
    else:
        print("  ✅ 无死链")

    print("\n🏝️  【孤岛检测】 (未被任何 Index 引用的活跃知识):")
    if orphans:
        for o in orphans:
            print(f"  - [WARN] {o}")
    else:
        print("  ✅ 无孤岛文件")

    print("\n📂  【文件名规则检测】 (仅允许 [a-zA-Z0-9._-]，Windows 兼容):")
    if bad_filenames:
        for path, reason in bad_filenames:
            print(f"  - [FAIL] {path}  ({reason})")
    else:
        print("  ✅ 所有文件名合规")

    if dead_links or bad_filenames:
        print("\n结论: ❌ FAIL")
        return EXIT_FAIL
    if oversized_files or orphans:
        print("\n结论: ⚠️ WARN")
        return 0
    print("\n结论: ✅ OK")
    return 0

if __name__ == "__main__":
    raise SystemExit(check_wiki())
