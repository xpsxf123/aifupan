#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sub-agent return gate.

Validates the structured return block from a sub-agent dispatch (per
`.claude/rules/dispatch-template.md`). Catches the common failure mode where
[Status]: PASS is claimed but evidence ([Files Changed], [Commands Run],
[ACs Mapped]) is empty or contradicts the claim.

Exit codes (per linter-severity-standard):
  0 OK    — proceed
  1 WARN  — structurally valid but inconsistent — surface to user
  2 FAIL  — structurally broken or contradictory — re-dispatch
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REQUIRED_FIELDS = (
    "Status",
    "Files Changed",
    "Commands Run",
    "ACs Mapped",
    "Issues Found",
    "Next Step",
)
VALID_STATUS = {"PASS", "PARTIAL", "FAIL", "ESCALATE", "BOUNDARY_EXCEPTION"}

EXIT_OK = 0
EXIT_WARN = 1
EXIT_FAIL = 2

FIELD_RE = re.compile(r"^\[([^\]]+)\]\s*:\s*(.*)$", re.MULTILINE)


def parse(text: str) -> dict[str, str]:
    """Parse `[Field]: value` block headers. For each header, capture both the
    same-line value AND any continuation lines up to the next header (or EOF).
    This is the canonical contract for dispatch returns where `[Files Changed]`
    and `[ACs Mapped]` routinely use multi-line bullet lists."""
    out: dict[str, str] = {}
    matches = list(FIELD_RE.finditer(text))
    for i, m in enumerate(matches):
        key = m.group(1).strip()
        same_line_val = m.group(2).strip()
        block_end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        after_text = text[m.end():block_end]
        parts: list[str] = []
        if same_line_val:
            parts.append(same_line_val)
        for line in after_text.splitlines():
            stripped = line.strip()
            if stripped:
                parts.append(stripped)
        out[key] = "\n".join(parts).strip()
    return out


def _is_empty_evidence(val: str) -> bool:
    return val.lower() in {"", "none", "n/a", "-", "—"}


def validate(fields: dict[str, str], task_kind: str) -> tuple[int, list[str]]:
    msgs: list[str] = []

    # 1. Required headers present
    missing = [k for k in REQUIRED_FIELDS if k not in fields]
    if missing:
        return EXIT_FAIL, [f"Missing required field(s): {', '.join(missing)}"]

    # 2. Status is one of the canonical values
    parts = (fields["Status"] or "").upper().split()
    status = parts[0] if parts else ""
    if status not in VALID_STATUS:
        return EXIT_FAIL, [f"Invalid Status: {fields['Status']!r}. Expected one of {sorted(VALID_STATUS)}"]

    # 3. ESCALATE / BOUNDARY_EXCEPTION → must include Reason
    if status in {"ESCALATE", "BOUNDARY_EXCEPTION"} and "Reason" not in fields:
        return EXIT_FAIL, [f"Status={status} requires a [Reason]: line."]

    # 4. PASS contradicts empty evidence on implement/review tasks
    if status == "PASS" and task_kind in {"implement", "review"}:
        files_empty = _is_empty_evidence(fields["Files Changed"])
        cmds_empty = _is_empty_evidence(fields["Commands Run"])
        if task_kind == "implement" and files_empty:
            return EXIT_FAIL, ["Status=PASS for implement task but [Files Changed]=none — contradicts."]
        if task_kind in {"implement", "review"} and cmds_empty:
            msgs.append("Status=PASS but [Commands Run]=none — inconsistent.")

    # 5. ACs Mapped with FAIL row + Status=PASS → FAIL
    if status == "PASS" and re.search(r"\bFAIL\b", fields["ACs Mapped"], re.IGNORECASE):
        return EXIT_FAIL, ["Status=PASS but at least one AC row is marked FAIL — direct contradiction."]

    if msgs:
        return EXIT_WARN, msgs
    return EXIT_OK, []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--return-file", required=True, help="Path to the sub-agent return text")
    parser.add_argument(
        "--task-kind",
        choices=["implement", "review", "extract", "audit"],
        default="implement",
    )
    args = parser.parse_args()

    path = Path(args.return_file)
    if not path.exists():
        print(f"FAIL: return file not found: {path}", file=sys.stderr)
        return EXIT_FAIL

    text = path.read_text(encoding="utf-8")
    fields = parse(text)
    code, msgs = validate(fields, args.task_kind)

    label = {EXIT_OK: "OK", EXIT_WARN: "WARN", EXIT_FAIL: "FAIL"}[code]
    print(f"{label}: subagent_return_gate (task_kind={args.task_kind})")
    for m in msgs:
        print(f"  - {m}")
    return code


if __name__ == "__main__":
    sys.exit(main())
