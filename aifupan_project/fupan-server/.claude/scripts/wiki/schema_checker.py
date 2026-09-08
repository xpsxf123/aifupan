#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
契约防腐体检工具 (Schema Checker)
Agent 在生成 openspec.md 后，可选调用此脚本检查关键结构是否缺失。

校验逻辑（与 openspec_schema.md 对齐）：
- SLIM 模式：4 个固定标题必须出现。
- STANDARD 模式：
    * 始终必填：§1 Context / §5 Business Logic / §6 Non-Functional / §7 Acceptance Criteria
    * 触发必填：frontmatter 中 `triggers: [...]` 声明的每一项必须对应章节存在
    * HIGH 风险强制：`risk: HIGH` 自动追加 §9 ADRs 必填（不依赖 triggers 声明）
- WARN（不阻塞）：FULL 模式建议出现 ```json 代码块以示例 API 响应
"""

import os
import sys
import re

EXIT_FAIL = 2

SLIM_MARKER_PATTERN = re.compile(r"^\s*spec_mode\s*:\s*SLIM\s*$", re.IGNORECASE | re.MULTILINE)
RISK_PATTERN = re.compile(r"^\s*risk\s*:\s*(LOW|MEDIUM|HIGH)\s*$", re.IGNORECASE | re.MULTILINE)
TRIGGERS_PATTERN = re.compile(r"^\s*triggers\s*:\s*\[(.*?)\]\s*$", re.IGNORECASE | re.MULTILINE)

# Always-required sections in STANDARD mode.
FULL_REQUIRED_HEADERS = [
    (re.compile(r"#+\s*1\.\s+Context", re.IGNORECASE), "§1 Context (必填)"),
    (re.compile(r"#+\s*5\.\s+Business Logic", re.IGNORECASE), "§5 Business Logic (必填)"),
    (re.compile(r"#+\s*6\.\s+Non-Functional", re.IGNORECASE), "§6 Non-Functional Constraints (必填)"),
    (re.compile(r"#+\s*7\.\s+Acceptance Criteria", re.IGNORECASE), "§7 Acceptance Criteria (必填)"),
]

# Trigger-conditional sections — checked only when frontmatter `triggers:` declares them.
TRIGGER_SECTION_MAP = {
    "domain":         (re.compile(r"#+\s*2\.\s+Domain Model", re.IGNORECASE),               "§2 Domain Model"),
    "business-arch":  (re.compile(r"#+\s*2\.5\s+Business Architecture", re.IGNORECASE),      "§2.5 Business Architecture"),
    "api":            (re.compile(r"#+\s*3\.\s+API Contract", re.IGNORECASE),                "§3 API Contract"),
    "data":           (re.compile(r"#+\s*4\.\s+Data Model", re.IGNORECASE),                  "§4 Data Model"),
    "tech-arch":      (re.compile(r"#+\s*5\.5\s+Technical Architecture", re.IGNORECASE),     "§5.5 Technical Architecture"),
    "design-pattern": (re.compile(r"#+\s*6\.5\s+Design Patterns", re.IGNORECASE),            "§6.5 Design Patterns"),
    "adr":            (re.compile(r"#+\s*9\.\s+Architecture Decision Records", re.IGNORECASE),"§9 ADRs"),
}

SLIM_REQUIRED_HEADERS = [
    (re.compile(r"#+\s+.*(变更摘要|Change Summary).*", re.IGNORECASE), "变更摘要"),
    (re.compile(r"#+\s+.*(影响面|Scope of Change).*", re.IGNORECASE), "影响面清单"),
    (re.compile(r"#+\s+.*(风险|回滚|Rollback).*", re.IGNORECASE), "风险与回滚"),
    (re.compile(r"#+\s+.*(验证|证据|Verification|Evidence).*", re.IGNORECASE), "验证与证据"),
]


def _parse_triggers(content: str) -> list[str]:
    """Extract triggers list from frontmatter, e.g. triggers: [api, data, tech-arch]."""
    m = TRIGGERS_PATTERN.search(content)
    if not m:
        return []
    raw = m.group(1)
    return [t.strip() for t in raw.split(",") if t.strip()]


def _parse_risk(content: str) -> str:
    m = RISK_PATTERN.search(content)
    return m.group(1).upper() if m else ""


def check_schema(file_path):
    if not os.path.exists(file_path):
        print(f"❌ 文件不存在: {file_path}")
        sys.exit(1)

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    print(f"📊 === 契约文件体检: {file_path} ===")

    is_slim = bool(SLIM_MARKER_PATTERN.search(content))
    fail_items: list[str] = []
    warn_items: list[str] = []

    if is_slim:
        print("模式: SLIM")
        for pattern, name in SLIM_REQUIRED_HEADERS:
            if not pattern.search(content):
                fail_items.append(name)
    else:
        print("模式: STANDARD")
        risk = _parse_risk(content)
        triggers = _parse_triggers(content)
        if risk:
            print(f"风险等级: {risk}")
        if triggers:
            print(f"声明 triggers: {triggers}")

        # Always-required
        for pattern, name in FULL_REQUIRED_HEADERS:
            if not pattern.search(content):
                fail_items.append(name)

        # Trigger-conditional
        effective_triggers = set(triggers)
        if risk == "HIGH":
            effective_triggers.add("adr")  # ADRs hard floor for HIGH

        for trigger in effective_triggers:
            entry = TRIGGER_SECTION_MAP.get(trigger)
            if entry is None:
                warn_items.append(f"未知 trigger: {trigger}（不在 schema 已知列表中）")
                continue
            pattern, name = entry
            if not pattern.search(content):
                fail_items.append(f"{name}（trigger '{trigger}' 已声明但章节缺失）")

        # WARN if FULL and no JSON example anywhere
        if "```json" not in content:
            warn_items.append("未发现 ```json 代码块（建议在 §3 API Contract 提供 JSON 示例）")

    print()
    if fail_items:
        print("发现项 (FAIL):")
        for item in fail_items:
            print(f"  - [FAIL] 缺失: {item}")
    if warn_items:
        print("发现项 (WARN):")
        for item in warn_items:
            print(f"  - [WARN] {item}")
    if not fail_items and not warn_items:
        print("关键结构: ✅ OK")

    print()
    if fail_items:
        print("结论: ❌ FAIL")
        return EXIT_FAIL
    if warn_items:
        print("结论: ⚠️ WARN")
        return 0
    print("结论: ✅ OK")
    return 0


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python schema_checker.py <path_to_openspec.md>")
        sys.exit(1)
    raise SystemExit(check_schema(sys.argv[1]))
