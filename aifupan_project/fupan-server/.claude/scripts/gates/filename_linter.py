#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
文件名规则 Linter (Cross-platform compatibility)

规则：项目内文件名只允许 [a-zA-Z0-9._-]（数字 / 字母 / 中划线 / 下划线 / 点）。
路径分隔符 '/' 不算文件名字符，由 OS 处理。

为什么：
- Windows 文件系统禁用 < > : " / \\ | ? *
- 跨平台开发（macOS/Linux/Windows）+ Git 仓库共享，
  含 ':' / 中文 / 空格的文件名在 Windows clone 时会失败
- 历史教训：archive/Change__YYYY-MM-DD_HH:MM:SS__*.md
  含 ':' 导致 Windows 用户无法 checkout，2026-06-16 统一改名

用法：
    # 扫描指定路径下所有文件
    python3 filename_linter.py --paths .claude/llm_wiki .claude/scripts

    # 仅检查 git staged 文件（pre-commit hook 用）
    python3 filename_linter.py --staged

    # 严格模式（默认禁中文 / 空格 / Windows 禁字符 / 全角字符）
    # --lax 模式仅禁 Windows 禁字符 < > : " | ? *（允许中文 / 空格等其他）

退出码：
    0  OK    所有文件名合规
    1  WARN  暂未使用
    2  FAIL  存在违规文件名（pre-commit 应拒绝提交）
"""

import argparse
import os
import re
import subprocess
import sys
from typing import List, Tuple

EXIT_OK = 0
EXIT_FAIL = 2

# 严格规则：文件名仅允许这些字符（不含路径分隔符 '/' '\'）
# . 用于扩展名 + 隐藏文件（如 .gitignore）
STRICT_PATTERN = re.compile(r'^[a-zA-Z0-9._-]+$')

# Windows 文件名禁字符（不含 '/' 因其是路径分隔符）
WINDOWS_FORBIDDEN = set('<>:"|?*')


def check_filename(fname: str, strict: bool = True) -> Tuple[bool, str]:
    """
    检查单个文件名是否合规。

    返回 (is_ok, reason)。
    """
    if not fname:
        return True, ""
    # 跳过 dotfiles 的特殊情况：如 .DS_Store / .gitignore / .gitkeep 等
    # 但仍要检查整体字符
    if strict:
        if STRICT_PATTERN.match(fname):
            return True, ""
        # 找出第一个非法字符以给出明确反馈
        for ch in fname:
            if not re.match(r'[a-zA-Z0-9._-]', ch):
                if ch in WINDOWS_FORBIDDEN:
                    return False, f"Windows 禁字符 {ch!r}"
                if '一' <= ch <= '鿿':
                    return False, f"中文字符 {ch!r}"
                if ch == ' ':
                    return False, "空格"
                return False, f"非法字符 {ch!r}"
        # 上面应该已经匹到，兜底
        return False, "不符合 [a-zA-Z0-9._-] 规则"
    else:
        # lax 模式：只禁 Windows 禁字符
        for ch in fname:
            if ch in WINDOWS_FORBIDDEN:
                return False, f"Windows 禁字符 {ch!r}"
        return True, ""


def scan_paths(paths: List[str], strict: bool = True) -> List[Tuple[str, str]]:
    """递归扫描指定路径，返回 [(file_path, reason)] 违规列表。"""
    violations = []
    for root_path in paths:
        if not os.path.exists(root_path):
            print(f"⚠️  路径不存在: {root_path}", file=sys.stderr)
            continue
        if os.path.isfile(root_path):
            fname = os.path.basename(root_path)
            ok, reason = check_filename(fname, strict)
            if not ok:
                violations.append((root_path, reason))
            continue
        for dirpath, dirnames, filenames in os.walk(root_path):
            # 检查目录名（path component）
            for d in dirnames:
                ok, reason = check_filename(d, strict)
                if not ok:
                    full = os.path.join(dirpath, d)
                    violations.append((full + os.sep, reason))
            # 检查文件名
            for f in filenames:
                ok, reason = check_filename(f, strict)
                if not ok:
                    violations.append((os.path.join(dirpath, f), reason))
    return violations


def scan_staged(strict: bool = True) -> List[Tuple[str, str]]:
    """扫 git diff --cached --name-only 列出的所有 staged 文件名。"""
    try:
        result = subprocess.run(
            ['git', 'diff', '--cached', '--name-only', '--diff-filter=AC'],  # A=added, C=copied
            capture_output=True, text=True, check=True
        )
    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        print(f"⚠️  git diff 调用失败: {e}", file=sys.stderr)
        return []
    files = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    violations = []
    for fpath in files:
        # 检查路径中的每段（不含根开头的 ''）
        parts = fpath.split('/')
        for i, part in enumerate(parts):
            if not part:
                continue
            ok, reason = check_filename(part, strict)
            if not ok:
                # 标出哪段违规
                display = '/'.join(parts[:i + 1])
                violations.append((display + ('/' if i < len(parts) - 1 else ''), reason))
                break  # 一个 path 只报第一段违规
    return violations


def main() -> int:
    parser = argparse.ArgumentParser(
        description="文件名规则 Linter — 检查文件名是否仅含 [a-zA-Z0-9._-]"
    )
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--paths", nargs='+', help="扫描的路径列表（递归）")
    group.add_argument("--staged", action='store_true', help="仅检查 git staged 文件（pre-commit 模式）")
    parser.add_argument("--lax", action='store_true',
                        help="宽松模式：仅禁 Windows 禁字符 < > : \" | ? *（默认严格模式禁中文 / 空格 / 其他特殊字符）")
    args = parser.parse_args()
    strict = not args.lax

    if args.staged:
        violations = scan_staged(strict)
        mode_desc = "git staged 文件"
    else:
        violations = scan_paths(args.paths, strict)
        mode_desc = f"路径 {args.paths}"

    print(f"📋 文件名 Linter — 扫描 {mode_desc}（{'严格' if strict else '宽松'}模式）")

    if not violations:
        print("✅ 所有文件名合规（仅含 [a-zA-Z0-9._-]）" if strict
              else "✅ 无 Windows 禁字符")
        return EXIT_OK

    print(f"❌ 发现 {len(violations)} 个违规：")
    for path, reason in violations:
        print(f"  - [FAIL] {path}  ({reason})")
    print()
    print("📌 规则：文件名仅允许 [a-zA-Z0-9._-]（数字 / 字母 / 中划线 / 下划线 / 点）")
    print("        Windows 文件系统禁字符：< > : \" / \\ | ? *")
    print("        含中文 / 空格的文件名在跨平台 Git 仓库中易踩 encoding 坑")
    return EXIT_FAIL


if __name__ == "__main__":
    raise SystemExit(main())
