#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Hard Constraints Generator — pattern-match Allowed Scope paths to relevant
jiuyu invariants from CLAUDE.md §2 / §5.

WHY:
  `dispatch-template.md` requires the main agent to hand-pick relevant Hard
  Constraints rows from CLAUDE.md §5 when authoring a dispatch prompt. Manual
  selection is error-prone — easy to omit ("forgot SQL #{} rule when scope
  includes Mapper.xml") or over-include ("listed Controller rules for a pure
  Service dispatch"). This script automates the selection.

USAGE:
  # Author mode: print the Hard Constraints markdown block for a scope.
  python3 .claude/scripts/harness/generate_hard_constraints.py \\
      --files replay-words/.../AnchorServiceImpl.java \\
              replay-words/.../AnchorController.java \\
              sql/tb_anchor_url.sql

  # SSOT validation: verify every constraint in this script also lives in
  # CLAUDE.md §2 / §5. Exits non-zero if drift detected.
  python3 .claude/scripts/harness/generate_hard_constraints.py --validate-ssot

OUTPUT (author mode):
  A markdown block ready to paste into the `**Hard Constraints**` section of a
  dispatch prompt, with a trailing comment showing which categories fired.

EXIT CODES:
  0 — OK (or validate-ssot passed)
  1 — no files passed in author mode (nothing to generate)
  2 — SSOT drift detected in --validate-ssot mode
"""
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
# Multi-SSOT design: Java invariants live in CLAUDE.md §5; SQL DDL column
# conventions live in openspec_schema.md §4 (the contract template architects
# fill in). Both are checked — a constraint's token must appear in at least one.
SSOT_FILES = (
    REPO_ROOT / "CLAUDE.md",
    REPO_ROOT / ".claude" / "llm_wiki" / "schema" / "openspec_schema.md",
)

# --- Constraint catalog ------------------------------------------------------
# Each entry: (category_tag, constraint_bullet, ssot_substring)
#   - category_tag drives the path → category mapping below.
#   - constraint_bullet is what gets emitted in the dispatch prompt.
#   - ssot_substring is a stable token expected to appear in CLAUDE.md §2 / §5.
#     If CLAUDE.md is edited and the token disappears, --validate-ssot fails so
#     drift is caught at lint time rather than silently rotting.
CONSTRAINTS: list[tuple[str, str, str]] = [
    # Universal — applies to any .java file
    ("universal", "DI 用构造器注入，禁 `@Autowired` / `@Resource`（新代码强制；改 legacy 时镜像周围风格，详见 dispatch-template.md）", "DI 用 构造器注入"),
    ("universal", "命名规范：`XxxEntity / XxxBo / XxxVo / XxxDao / XxxService / XxxServiceImpl / XxxController`", "XxxEntity / XxxBo / XxxVo"),
    ("universal", "🚨 **代码标识符禁中文（绝对红线，CRITICAL）**：class / interface / method / field / parameter / local / constant / enum / package 名必须全英文；中文仅注释 / Javadoc / 字符串字面量 / `@DisplayName/@Schema/@Operation/@Tag` 等注解 String 参数内允许。测试方法名范式：`methodUnderTest_scenario_expected` + `@DisplayName(\"中文场景\")` 双重表达。详 `memory/feedback_no_chinese_identifiers.md`", "代码标识符禁中文"),

    # Controller
    ("controller", "Controller 必须返回 `R<T>`（`com.jiuyu.replay.generic.vo.common.R`），禁裸返业务对象", "Controller 返回 `R<T>`"),
    ("controller", "Controller 加 `@CrossOrigin` / `@Tag` / `@Operation`", "Controller 加 `@CrossOrigin`"),

    # Service write methods
    ("service", "写方法加 `@Transactional(rollbackFor = Exception.class)`", "@Transactional(rollbackFor = Exception.class)"),
    ("service", "ID 用 `SnowflakeManager.nextValue()`", "SnowflakeManager.nextValue()"),
    ("service", "Bean 拷贝用 Spring `BeanUtils.copyProperties`", "BeanUtils.copyProperties"),
    ("service", "分页用 `PageUtils<T>` 包装 `IPage`", "PageUtils<T>"),

    # Entity / persistence shape
    ("entity", "实体 `@TableId(type = IdType.INPUT)`，禁 `@TableLogic`", "@TableId(type = IdType.INPUT)"),
    ("entity", "软删除手动设 `isDeleted` 字段", "软删除手动设 `isDeleted` 字段"),
    ("entity", "时间戳手动 `LocalDateTime.now()` 设 `createDate / updateDate`", "时间戳手动 `LocalDateTime.now()`"),

    # SQL / persistence access
    ("sql", "列表查询必须带 `tenantId` 过滤（多租户隔离）", "列表查询必须带 `tenantId` 过滤"),
    ("sql", "SQL 占位用 `#{}` 禁 `${}`", "SQL 占位用 `#{}` 禁 `${}`"),
    ("sql", "查询用 `LambdaQueryWrapper`（类型安全）", "LambdaQueryWrapper"),
    ("sql", "`IN (...)` 列表 > 500 用 `Lists.partition(ids, 500)` 分批", "Lists.partition(ids, 500)"),

    # DDL / schema files
    ("ddl", "新表必含 `tenant_id BIGINT NOT NULL`、`is_deleted TINYINT NOT NULL DEFAULT 0`、`create_date DATETIME NOT NULL`、`update_date DATETIME NOT NULL`", "is_deleted"),
    ("ddl", "新索引按 leftmost-prefix 设计；`(tenant_id, ...)` 优先（多租户高选择性列在前）", "tenant_id"),

    # Cross-module
    ("cross_module", "跨 jiuyu 模块调用必须走 `replay-generic` 的 Feign 接口，禁跨模块直连 Dao", "跨模块调用走 `replay-generic` 的 Feign 接口"),

    # Logging / security
    ("security", "敏感字段（密码 / 手机 / 身份证）不入日志", "敏感字段（密码/手机/身份证）不入日志"),
]


# --- Path → category mapping -------------------------------------------------
def _categories_for(path: str) -> set[str]:
    """Determine which constraint categories apply to a given file path."""
    p = path.replace("\\", "/").lower()
    cats: set[str] = set()

    is_java = p.endswith(".java")
    is_xml = p.endswith(".xml")
    is_sql = p.endswith(".sql")

    if is_java:
        cats.add("universal")
        cats.add("security")  # logging concern applies everywhere
        if "/controller/" in p or p.endswith("controller.java"):
            cats.add("controller")
        if "/service/" in p or p.endswith("service.java") or p.endswith("serviceimpl.java"):
            cats.add("service")
            cats.add("sql")        # service often constructs queries
            cats.add("cross_module")  # service is where Feign calls live
        if "/dao/" in p or p.endswith("dao.java"):
            cats.add("sql")
        if "/entity/" in p or p.endswith("entity.java"):
            cats.add("entity")

    if is_xml and "mapper" in p:
        cats.add("sql")

    if is_sql:
        cats.add("ddl")
        # New tables also need tenant_id filtering at read time — surface SQL rule
        cats.add("sql")

    return cats


# --- Emission ----------------------------------------------------------------
def _emit_block(files: list[str]) -> str:
    fired: set[str] = set()
    for f in files:
        fired |= _categories_for(f)

    if not fired:
        return (
            "**Hard Constraints** (none auto-detected for this scope — confirm manually against CLAUDE.md §2 / §5):\n"
            "- (none)\n"
        )

    lines: list[str] = []
    lines.append("**Hard Constraints** (auto-generated by .claude/scripts/harness/generate_hard_constraints.py)")
    lines.append("")
    seen: set[str] = set()
    for cat, bullet, _ in CONSTRAINTS:
        if cat in fired and bullet not in seen:
            lines.append(f"- {bullet}")
            seen.add(bullet)
    lines.append("")
    lines.append(f"<!-- categories fired: {sorted(fired)} -->")
    lines.append(f"<!-- SSOT: CLAUDE.md §2 / §5 (verify with `--validate-ssot`) -->")
    return "\n".join(lines)


# --- SSOT validation ---------------------------------------------------------
def _validate_ssot() -> int:
    sources: list[tuple[Path, str]] = []
    for f in SSOT_FILES:
        if not f.is_file():
            print(f"FAIL: SSOT source not found at {f}")
            return 2
        sources.append((f, f.read_text(encoding="utf-8")))

    missing: list[tuple[str, str]] = []
    for cat, _bullet, token in CONSTRAINTS:
        if not any(token in body for _path, body in sources):
            missing.append((cat, token))

    if missing:
        print("FAIL: SSOT drift — the following constraint tokens no longer appear in any SSOT source:")
        for cat, token in missing:
            print(f"  - [{cat}] token not found: {token!r}")
        print("")
        print("SSOT sources checked:")
        for f, _ in sources:
            print(f"  - {f.relative_to(REPO_ROOT)}")
        print("")
        print("Either restore the missing rows in the relevant SSOT file, or update this script's CONSTRAINTS table.")
        return 2
    print(f"OK: SSOT validated — all {len(CONSTRAINTS)} tokens present across {len(sources)} source(s)")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--files", nargs="*", default=[], help="space-separated Allowed Scope file paths")
    parser.add_argument("--validate-ssot", action="store_true", help="check that every constraint token still appears in CLAUDE.md")
    args = parser.parse_args()

    if args.validate_ssot:
        return _validate_ssot()

    if not args.files:
        print("FAIL: no --files provided (use --validate-ssot for SSOT check mode)", file=sys.stderr)
        return 1

    print(_emit_block(args.files))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
