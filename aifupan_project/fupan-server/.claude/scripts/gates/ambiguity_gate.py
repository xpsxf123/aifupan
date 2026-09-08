#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Ambiguity Gate (Deterministic)

Exit codes:
- 0: PASS
- 1: WARN
- 2: FAIL
"""

import argparse
import os
import re
import sys

EXIT_WARN = 1
EXIT_FAIL = 2


def _normalize(text: str) -> str:
    return re.sub(r"\s+", " ", (text or "").strip()).lower()


def _has_any(text: str, keywords: list[str]) -> bool:
    t = _normalize(text)
    return any(k in t for k in keywords)


_STOP_WORDS = {
    "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
    "of", "with", "by", "from", "is", "are", "was", "be", "this", "that",
    "it", "its", "we", "i", "you", "he", "she", "they", "our", "my",
    "all", "not", "no", "do", "does", "have", "has", "can", "will", "should",
    "如何", "怎么", "什么", "这个", "那个", "是否", "需要",
}


def _has_content_word(text: str) -> bool:
    """Return True if the text contains at least one content word (non-stop-word, len >= 4).
    This is an open-ended fallback so that business domain objects (table names, service
    names, feature names) are accepted without being hardcoded in this gate.
    """
    words = re.split(r"[\s\-_/.,;:!?()\[\]{}\"']+", text)
    return any(len(w) >= 4 and w not in _STOP_WORDS for w in words)


def _check_intent(intent: str) -> tuple[int, list[str]]:
    reasons: list[str] = []
    t = _normalize(intent)
    if not t:
        return EXIT_FAIL, ["intent is empty"]

    # Only abstract process-level artifacts are hardcoded.
    # Business domain objects (tables, APIs, services, permissions, etc.) are intentionally
    # excluded — the LLM reads the workspace code to identify them.
    process_object_signals = [
        # Workflow process artifacts
        "hook", "hooks", "gate", "gates", "lifecycle", "router", "workflow",
        "wiki", "wal", "launch_spec", "focus_card", "openspec", "explore_report",
        "skill", "skills", "role_matrix", "agent", "agents",
        # Chinese equivalents
        "流程", "门控", "生命周期",
    ]
    action_signals = [
        # English
        "implement", "add", "create", "fix", "refactor", "optimize", "design",
        "migrate", "update", "remove", "delete", "integrate", "deploy", "test", "review",
        "build", "generate", "write", "change", "modify",
        # Chinese
        "设计", "实现", "修复", "新增", "改造", "优化", "落地", "上线", "测试",
        # Research-family verbs — counted as valid actions (DoR passes), but trigger
        # a research-diversion hint on stderr (see _check_research_diversion below).
        "research", "investigate", "assess", "explore", "feasibility",
        "分析", "调研", "评估", "探索", "可行性", "现状",
    ]
    success_signals = [
        # English — Change-style success
        "pass", "deliver", "working", "verified", "evidence",
        "test case", "doc", "documented", "returns",
        # Chinese — Change-style success
        "验收", "通过", "可用", "跑通", "门禁", "测试用例", "接口返回", "文档",
        # Research-style success (Scenario F): the report itself is the deliverable
        "report", "feasibility", "gap analysis", "inventory",
        "报告", "清单", "可行性结论", "gap",
    ]

    has_process_object = _has_any(t, process_object_signals)
    has_action = _has_any(t, action_signals)
    has_success = _has_any(t, success_signals)

    # Fallback: if no hardcoded process object matched, check for any content word
    # (length ≥ 4, not a common stop word). Business domain objects are expected here —
    # the gate accepts them without knowing their meaning; the LLM handles semantics.
    has_object = has_process_object or _has_content_word(t)

    if not has_action:
        reasons.append("missing action signal (e.g. implement, fix, add, design)")
    if not has_object:
        reasons.append("missing object — intent appears to have no noun/target (very short or all stop-words)")
    if not has_success:
        reasons.append("missing success/evidence signal (e.g. verified, test case, deliver, doc)")

    if has_action and has_object:
        if has_success:
            return 0, []
        return EXIT_WARN, reasons
    return EXIT_FAIL, reasons


_RESEARCH_VERBS = [
    "research", "investigate", "assess", "explore", "feasibility",
    "分析", "调研", "评估", "探索", "可行性", "现状", "能给到什么程度",
]

_RESEARCH_OUTCOMES = [
    "报告", "清单", "gap", "数据现状", "能不能", "可不可行", "成本估算",
    "report", "feasibility", "gap analysis", "what data", "inventory",
]


def _check_research_diversion(intent: str) -> str | None:
    """Detect if intent shape suggests /h-research instead of /h-brief.

    Returns a hint string when verb+outcome both match research family,
    None otherwise. Does NOT change the exit code — DoR is honored as-is;
    this is purely a routing hint for the agent layer.
    """
    t = _normalize(intent)
    verb_hit = next((v for v in _RESEARCH_VERBS if v in t), None)
    outcome_hit = next((o for o in _RESEARCH_OUTCOMES if o in t), None)
    if verb_hit and outcome_hit:
        return (
            f"[research-diversion] verb='{verb_hit}' + outcome='{outcome_hit}' detected — "
            "intent looks like research; consider /h-research over /h-brief "
            "(see .claude/rules/lifecycle.md Scenario F)."
        )
    # Single verb hit alone is weaker — still emit but lower confidence.
    if verb_hit and not outcome_hit:
        return (
            f"[research-diversion?] verb='{verb_hit}' detected but no explicit research "
            "outcome shape — may be Change-with-analysis (e.g., 分析 bug 后修). "
            "Agent should disambiguate before /h-brief."
        )
    return None


def _check_anchors_file(path: str) -> tuple[int, list[str]]:
    if not path:
        return 0, []
    if not os.path.exists(path):
        return EXIT_FAIL, [f"anchors file not found: {path}"]

    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    if "## Core Context Anchors" not in content:
        return EXIT_FAIL, ["missing section: ## Core Context Anchors"]

    # Check for abstract anchor dimension headings, not business-specific terms.
    # The actual domain content (API names, table names, etc.) is written by the LLM
    # based on workspace analysis — we only verify the structural skeleton exists.
    anchor_dimension_markers = [
        # Process/engineering dimensions
        "Red Line", "red line", "Constraint", "constraint",
        "Risk", "risk", "Evidence", "evidence",
        "Rules", "rules", "规则", "风险", "证据", "约束",
    ]
    present = sum(1 for m in anchor_dimension_markers if m in content)
    if present < 2:
        return EXIT_WARN, ["anchors are too thin — include at least 2 of: Red Lines, Constraints, Risk, Evidence, Rules"]
    return 0, []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--intent", required=True)
    parser.add_argument("--anchors-file", default="")
    args = parser.parse_args()

    code1, reasons1 = _check_intent(args.intent)
    code2, reasons2 = _check_anchors_file(args.anchors_file)

    code = max(code1, code2)
    reasons = reasons1 + reasons2

    # Research diversion hint — emitted to stderr regardless of exit code so the agent
    # layer (ambiguity-gatekeeper.md) can surface it in [Issues Found] without blocking.
    research_hint = _check_research_diversion(args.intent)
    if research_hint:
        print(research_hint, file=sys.stderr)

    if code == 0:
        print("OK: ambiguity gate pass")
        return 0
    if code == EXIT_WARN:
        print("WARN: ambiguity gate")
        for r in reasons:
            print(f"- {r}")
        return EXIT_WARN
    print("FAIL: ambiguity gate")
    for r in reasons:
        print(f"- {r}")
    return EXIT_FAIL


if __name__ == "__main__":
    raise SystemExit(main())
