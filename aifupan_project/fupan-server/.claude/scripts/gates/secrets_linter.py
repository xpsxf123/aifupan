#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Secrets Linter (High-confidence scan)

Exit codes:
- 0: PASS
- 1: WARN
- 2: FAIL
"""

import argparse
import fnmatch
import glob
import os
import re
import sys

EXIT_WARN = 1
EXIT_FAIL = 2


# 密钥风格 token：必须看起来像凭据（无空格、含字母数字、长度 8-128），
# 避免命中文档里的占位符或注释（如 `password: yourPasswordHere`）。
_CRED_TOKEN = r"[A-Za-z0-9+/=._\-]{8,128}"

PATTERNS_FAIL = [
    re.compile(r"(?i)\bsecret[_-]?access[_-]?key\b\s*[:=]\s*[\"']?" + _CRED_TOKEN + r"[\"']?"),
    re.compile(r"(?i)\baccess[_-]?key[_-]?id\b\s*[:=]\s*[\"']?" + _CRED_TOKEN + r"[\"']?"),
    re.compile(r"(?i)\bapi[_-]?key\b\s*[:=]\s*[\"']?" + _CRED_TOKEN + r"[\"']?"),
    re.compile(r"(?i)\bpassword\b\s*[:=]\s*[\"']?" + _CRED_TOKEN + r"[\"']?"),
    re.compile(r"(?i)\btoken\b\s*[:=]\s*[\"']?" + _CRED_TOKEN + r"[\"']?"),
]

PATTERNS_WARN = [
    re.compile(r"AKIA[0-9A-Z]{16}"),
    re.compile(r"(?i)\bsecret\b\s*[:=]\s*[\"']?" + _CRED_TOKEN + r"[\"']?"),
]

# 注释行 / 占位符行白名单：满足 _CRED_TOKEN 之外，再放过明显示例值。
_COMMENT_PREFIXES = ("//", "#", "*", "<!--")
_PLACEHOLDER_TOKENS = (
    "your", "example", "placeholder", "xxxxx", "changeme", "<", "${", "{{",
)

IGNORE_GLOBS = [
    "**/target/**",
    "**/.git/**",
    "**/.idea/**",
    "**/.claude/runs/**",
]


def _is_ignored(path: str) -> bool:
    p = path.replace("\\", "/")
    for ig in IGNORE_GLOBS:
        if fnmatch.fnmatch(p, ig):
            return True
    return False


def _scan_file(path: str) -> tuple[list[str], list[str]]:
    fails: list[str] = []
    warns: list[str] = []
    try:
        with open(path, "r", encoding="utf-8") as f:
            lines = f.readlines()
    except (UnicodeDecodeError, PermissionError) as e:
        # Binary or unreadable file — skip silently, expected case.
        return fails, warns
    except FileNotFoundError:
        print(f"[secrets_linter] file vanished mid-scan: {path}", file=sys.stderr)
        return fails, warns
    except OSError as e:
        print(f"[secrets_linter] OS error reading {path}: {e}", file=sys.stderr)
        return fails, warns

    for i, line in enumerate(lines, start=1):
        stripped = line.lstrip()
        # 跳过注释行（典型示例：`// password: example`）
        if stripped.startswith(_COMMENT_PREFIXES):
            continue
        # 跳过显式占位符 / 模板值
        lower = stripped.lower()
        if any(tok in lower for tok in _PLACEHOLDER_TOKENS):
            continue
        for pat in PATTERNS_FAIL:
            if pat.search(line):
                fails.append(f"{path}:{i}:{line.strip()}")
        for pat in PATTERNS_WARN:
            if pat.search(line):
                warns.append(f"{path}:{i}:{line.strip()}")
    return fails, warns


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--paths", nargs="+", required=True, help="glob patterns")
    args = parser.parse_args()

    files: list[str] = []
    for g in args.paths:
        files.extend(glob.glob(g, recursive=True))
    files = [os.path.normpath(p) for p in files if os.path.isfile(p) and not _is_ignored(p)]

    all_fails: list[str] = []
    all_warns: list[str] = []
    for f in sorted(set(files)):
        fails, warns = _scan_file(f)
        all_fails.extend(fails)
        all_warns.extend(warns)

    if all_fails:
        print("FAIL: secrets linter hit high-confidence patterns")
        for x in all_fails[:100]:
            print(f"- {x}")
        if len(all_fails) > 100:
            print(f"... truncated ({len(all_fails)} total)")
        return EXIT_FAIL

    if all_warns:
        print("WARN: secrets linter hit suspicious patterns")
        for x in all_warns[:100]:
            print(f"- {x}")
        if len(all_warns) > 100:
            print(f"... truncated ({len(all_warns)} total)")
        return EXIT_WARN

    print("OK: secrets linter pass")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
