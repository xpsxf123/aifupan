#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Gate Runner (Role-aware)

This tool mounts roles dynamically by (intent, profile, phase) using:
- .claude/workflow/role_matrix.json

It runs deterministic gates and writes a markdown report to:
- .claude/runs/gates_report_<timestamp>.md

Exit codes:
- 0: PASS
- 1: WARN
- 2: FAIL
"""

import argparse
import json
import os
import re
import subprocess
import sys
import signal
from datetime import datetime

EXIT_WARN = 1
EXIT_FAIL = 2

_PHASE_CANONICAL = ("Explorer", "Propose", "Review", "Implement", "QA", "Archive")
_PHASE_PREFIX_RE = re.compile(r"^\d+(?:\.\d+)?_(.+)$")


def _normalize_phase(raw: str) -> str:
    """Accept both bare ('QA') and launch_spec/command form ('5_QA' / '3.5_Approval').
    Normalize to bare form used by role_matrix.json. Raise ArgumentTypeError on
    unknown phase so argparse surfaces a clear message instead of choices= mismatch.

    Approval (3.5_Approval) is a yield point, not a gate-runnable phase — rejected here.
    """
    s = (raw or "").strip()
    m = _PHASE_PREFIX_RE.match(s)
    if m:
        s = m.group(1)
    if s not in _PHASE_CANONICAL:
        valid_forms = sorted({*_PHASE_CANONICAL, *(f"{i+1}_{p}" for i, p in enumerate(_PHASE_CANONICAL))})
        raise argparse.ArgumentTypeError(
            f"--phase {raw!r}: accepted forms are {valid_forms}"
        )
    return s


def _load_json(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def _render_arg(token: str, ctx: dict) -> str:
    for k, v in ctx.items():
        token = token.replace("{" + k + "}", str(v))
    return token


def _run_script(script: str, args: list[str]) -> tuple[int, str]:
    cmd = [sys.executable, script] + args
    try:
        out = subprocess.check_output(cmd, stderr=subprocess.STDOUT, text=True)
        return 0, out
    except subprocess.CalledProcessError as e:
        code = e.returncode
        out = e.output or ""
        if code not in (0, 1, 2):
            code = EXIT_FAIL
        return code, out


def _discover_active_run_dir() -> str:
    """Discover the active run directory when `--run-dir` is not provided.

    Resolution order:
      1. `CLAUDE_ACTIVE_RUN_DIR` env var (explicit pinning, useful in CI / harness drivers).
      2. Most recently mtime'd `.claude/runs/<intent>__*/` that contains `openspec.md`.
      3. Empty string — caller's conditional mounts will fail-safe-skip.

    Mirrors the discovery contract used by find_active_focus_card.py, but anchored
    on openspec.md (the document that conditions are evaluated against) rather than
    focus_card.md, since Vibe-mode runs may have a focus_card but no openspec.
    """
    override = os.environ.get("CLAUDE_ACTIVE_RUN_DIR", "").strip()
    if override:
        if os.path.isdir(override) and os.path.isfile(os.path.join(override, "openspec.md")):
            return override
        return ""

    runs_root = ".claude/runs"
    if not os.path.isdir(runs_root):
        return ""

    candidates: list[tuple[float, str]] = []
    for name in os.listdir(runs_root):
        if "__" not in name:
            continue
        full = os.path.join(runs_root, name)
        if not os.path.isdir(full):
            continue
        if not os.path.isfile(os.path.join(full, "openspec.md")):
            continue
        try:
            candidates.append((os.path.getmtime(full), full))
        except OSError:
            continue
    if not candidates:
        return ""
    candidates.sort(reverse=True)
    return candidates[0][1]


def _coerce_scalar(raw: str):
    """Convert a raw string scalar from openspec frontmatter / condition RHS into a typed value."""
    s = raw.strip().strip('"').strip("'")
    if s.lower() == "true":
        return True
    if s.lower() == "false":
        return False
    if s.lower() in ("none", "null"):
        return None
    return s


def _read_openspec_frontmatter(run_dir: str) -> dict:
    """Read top-of-file `key: value` lines from <run_dir>/openspec.md.

    Supports both YAML-frontmatter form (`---\\nkey: value\\n---`) and the project's
    convention of bare top-of-file lines (e.g. `spec_mode: STANDARD`).
    Hyphens in keys are normalized to underscores so `frontend-facing` is
    addressable as `openspec.frontend_facing` in mount conditions.
    Returns empty dict when run_dir is missing/empty or openspec.md absent.
    """
    fields: dict = {}
    if not run_dir:
        return fields
    spec_path = os.path.join(run_dir, "openspec.md")
    if not os.path.isfile(spec_path):
        return fields

    with open(spec_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    in_frontmatter = False
    started = False
    for idx, raw in enumerate(lines):
        line = raw.rstrip("\n")
        stripped = line.strip()
        if idx == 0 and stripped == "---":
            in_frontmatter = True
            started = True
            continue
        if in_frontmatter and stripped == "---":
            break
        if not in_frontmatter:
            if stripped.startswith("#"):
                break
            if not stripped:
                if started:
                    break
                continue
            if ":" not in stripped or stripped.startswith("-"):
                # First non-keyvalue line ends the bare prefix.
                break
            started = True
        else:
            if not stripped or stripped.startswith("#"):
                continue
        if ":" in stripped:
            key, _, val = stripped.partition(":")
            key = key.strip().replace("-", "_")
            fields[key] = _coerce_scalar(val)
    return fields


_CONDITION_RE = re.compile(r"^\s*openspec\.([\w]+)\s*(==|!=)\s*(.+?)\s*$")


def _evaluate_condition(cond: str, openspec: dict):
    """Evaluate `openspec.<key> (==|!=) <value>`.

    Returns True/False on success, None when the expression cannot be parsed
    (caller should treat None as fail-safe-skip).
    """
    if not cond:
        return True
    m = _CONDITION_RE.match(cond)
    if not m:
        return None
    key, op, rhs_raw = m.group(1), m.group(2), m.group(3)
    actual = openspec.get(key)
    expected = _coerce_scalar(rhs_raw)
    if op == "==":
        return actual == expected
    if op == "!=":
        return actual != expected
    return None


def _resolve_roles(matrix: dict, intent: str, profile: str, phase: str, openspec: dict) -> tuple[list[str], list[str]]:
    """Resolve mounted roles for the given (intent, profile, phase) tuple.

    Returns (roles, condition_notes). condition_notes lists human-readable
    diagnostics about conditional mounts that were skipped or had unparseable
    conditions — surfaced in the gate report for transparency.
    """
    roles: list[str] = []
    notes: list[str] = []
    for m in matrix.get("mounts", []):
        if m.get("intent") != intent:
            continue
        if m.get("profile") != profile:
            continue
        if m.get("phase") != phase:
            continue
        cond = m.get("condition")
        if cond:
            result = _evaluate_condition(cond, openspec)
            if result is None:
                notes.append(f"SKIP mount (unparseable condition `{cond}`) → roles {m.get('roles', [])}")
                continue
            if not result:
                notes.append(f"SKIP mount (condition `{cond}` evaluated False) → roles {m.get('roles', [])}")
                continue
            notes.append(f"ALLOW mount (condition `{cond}` evaluated True) → roles {m.get('roles', [])}")
        roles.extend(m.get("roles", []))
    return roles, notes


def _resolve_scenario_roles(matrix: dict, scenario: str) -> list[str]:
    if not scenario:
        return []
    for s in matrix.get("scenarios", []) or []:
        if (s.get("scenario") or "").upper() == scenario.upper():
            return list(s.get("roles", []) or [])
    return []


def _safe_key(text: str) -> str:
    return (text or "").replace("\\", "/")


def _load_retry_state(path: str) -> dict:
    if not os.path.exists(path):
        return {}
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
            return data if isinstance(data, dict) else {}
    except Exception:
        return {}


def _save_retry_state(path: str, state: dict) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(state, f, ensure_ascii=False, indent=2)


def _clear_task_state(path: str, state: dict, task_id: str) -> None:
    if task_id in state:
        state.pop(task_id, None)
        _save_retry_state(path, state)


def _build_escalation_card(
        out_path: str,
        task_id: str,
        args,
        blocked: list[str],
        task_state: dict,
        report_path: str
) -> None:
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(f"# Escalation Card - {datetime.now().strftime('%Y%m%d_%H%M%S')}\n\n")
        f.write(f"- task-id: {task_id}\n")
        f.write(f"- intent: {args.intent}\n")
        f.write(f"- profile: {args.profile}\n")
        f.write(f"- phase: {args.phase}\n")
        f.write(f"- topic: {args.topic}\n")
        f.write(f"- date: {args.date}\n")
        f.write(f"- verify-level: {args.verify_level}\n")
        f.write(f"- report: `{report_path}`\n\n")
        f.write("## Goal\n")
        f.write("- Complete gate validation and proceed to next lifecycle phase.\n\n")
        f.write("## Current blockers\n")
        f.write("- Per-script failure cap exceeded (>3) in this task.\n")
        for s in blocked:
            f.write(f"- `{s}`: {task_state.get(s)} failures\n")
        f.write("\n## What I tried (with evidence)\n")
        f.write(f"- Re-ran mounted gates; latest evidence in `{report_path}`.\n")
        f.write(f"- Retry state snapshot: `.claude/runs/gate_retry_state.json`.\n\n")
        f.write("## What I need from human\n")
        f.write("- Clarify scope/intent ambiguities or adjust constraints for blocked scripts.\n")
        f.write("- Confirm whether to reset retry counter for this task after intervention.\n")


def _parse_artifact_tags(raw: str) -> set[str]:
    if not raw:
        return set()
    return {x.strip().lower() for x in raw.split(",") if x.strip()}


def _should_run_gate(script: str, rendered_args: list[str], verify_level: str, artifact_tags: set[str], ctx: dict) -> tuple[bool, str]:
    base = os.path.basename(script or "")

    # 1) Intensity-based filtering
    if verify_level == "quick" and base in {"wiki_linter.py", "secrets_linter.py", "comment_linter_java.py"}:
        return False, "skip by verify-level=quick"

    # 2) Artifact-based filtering
    if base == "delivery_capsule_gate.py" and not ctx.get("delivery_file"):
        return False, "skip: delivery file not provided"

    if base == "writeback_gate.py" and artifact_tags:
        req_types = []
        for i, x in enumerate(rendered_args):
            if x == "--require" and i + 1 < len(rendered_args):
                req_types.append(rendered_args[i + 1].strip().lower())
        if req_types:
            if not any(rt in artifact_tags for rt in req_types):
                return False, f"skip by artifact-tags={sorted(artifact_tags)}"

    # 3) Scenario-gated scripts: only run when the matching scenario tag is present
    scenario_gates = {
        "migration_gate.py": "scenario_b",
        "api_breaking_gate.py": "scenario_c",
        "dependency_gate.py": "scenario_e",
    }
    if base in scenario_gates and scenario_gates[base] not in artifact_tags:
        return False, f"skip: requires artifact-tag '{scenario_gates[base]}'"

    # 4) Strict mode runs mounted gates as-is
    return True, ""


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--intent", default="Change")
    parser.add_argument("--profile", required=True, choices=["PATCH", "STANDARD"])
    parser.add_argument("--phase", required=True, type=_normalize_phase,
                        help="Explorer|Propose|Review|Implement|QA|Archive (also accepts launch_spec form like 5_QA)")
    parser.add_argument("--topic", required=True)
    parser.add_argument("--date", required=True, help="YYYYMMDD")
    parser.add_argument("--intent-text", default="")
    parser.add_argument("--anchors-file", default="")
    parser.add_argument("--delivery-file", default="")
    parser.add_argument("--changed-files", default="", help="comma-separated changed files for scope guard")
    parser.add_argument("--artifact-tags", default="", help="comma-separated tags: domain,api,rules,data,architecture,skill,workflow")
    parser.add_argument("--verify-level", default="standard", choices=["quick", "standard", "strict"])
    parser.add_argument("--task-id", default="", help="stable task id for retry counting; default=intent:profile:topic:date")
    parser.add_argument("--max-failures-per-script", type=int, default=3, help="per task/script failure cap")
    parser.add_argument("--end-task", action="store_true", help="clear retry state for this task after run (success/fail)")
    parser.add_argument("--matrix", default=".claude/workflow/role_matrix.json")
    parser.add_argument("--run-dir", default="", help="active run directory; enables mount condition evaluation by reading <run-dir>/openspec.md frontmatter")
    parser.add_argument("--scenario", default="", help="scenario tag (e.g. DEBUG) to add scenario-mounted roles on top of phase mounts")
    args = parser.parse_args()

    matrix = _load_json(args.matrix)
    task_id = args.task_id.strip() or f"{args.intent}:{args.profile}:{args.topic}:{args.date}"
    retry_state_file = ".claude/runs/gate_retry_state.json"
    retry_state = _load_retry_state(retry_state_file)
    task_state = retry_state.get(task_id, {})

    # Nested closure captures retry_state_file/retry_state/task_id from main()'s
    # scope. Safe here because retry_state is only mutated below (line ~485) via
    # `retry_state[task_id] = ...`, never re-bound — closure sees the same dict
    # at signal time. If refactoring this main(), keep retry_state as a single
    # binding or hoist the handler to module scope with explicit state passing.
    def _handle_signal(signum, frame):
        _clear_task_state(retry_state_file, retry_state, task_id)
        print(f"INTERRUPTED: task state cleared for {task_id}")
        raise SystemExit(130)

    signal.signal(signal.SIGINT, _handle_signal)
    signal.signal(signal.SIGTERM, _handle_signal)

    ctx = {
        "intent_text": args.intent_text,
        "anchors_file": args.anchors_file,
        "topic": args.topic.strip().lower().replace(" ", "_"),
        "date": args.date,
        "delivery_file": args.delivery_file,
        "changed_files": args.changed_files,
        # `focus_card` and `run_dir` are filled in below after run_dir discovery.
        "focus_card": "",
        "run_dir": "",
    }
    artifact_tags = _parse_artifact_tags(args.artifact_tags)

    run_dir_source = "cli"
    if not args.run_dir:
        discovered = _discover_active_run_dir()
        if discovered:
            args.run_dir = discovered
            run_dir_source = (
                "env(CLAUDE_ACTIVE_RUN_DIR)"
                if os.environ.get("CLAUDE_ACTIVE_RUN_DIR", "").strip()
                else "auto-discovered"
            )
            print(f"INFO: run_dir {run_dir_source} → {discovered}", file=sys.stderr)
        else:
            run_dir_source = "none"

    # Now that run_dir is known, fill the placeholders so role_matrix args
    # like `{focus_card}` / `{run_dir}` interpolate correctly.
    if args.run_dir:
        ctx["run_dir"] = args.run_dir
        candidate_fc = os.path.join(args.run_dir, "focus_card.md")
        if os.path.isfile(candidate_fc):
            ctx["focus_card"] = candidate_fc

    openspec_fm = _read_openspec_frontmatter(args.run_dir)
    roles, condition_notes = _resolve_roles(matrix, args.intent, args.profile, args.phase, openspec_fm)
    scenario_roles = _resolve_scenario_roles(matrix, args.scenario)
    if scenario_roles:
        seen = set(roles)
        for r in scenario_roles:
            if r not in seen:
                roles.append(r)
                seen.add(r)
    if not roles:
        print("WARN: no roles mounted for this phase")
        return EXIT_WARN

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_path = f".claude/runs/gates_report_{timestamp}.md"
    os.makedirs(os.path.dirname(report_path), exist_ok=True)

    overall = 0
    with open(report_path, "w", encoding="utf-8") as rep:
        rep.write(f"# Gates Report - {timestamp}\n\n")
        rep.write(f"- intent: {args.intent}\n")
        rep.write(f"- profile: {args.profile}\n")
        rep.write(f"- phase: {args.phase}\n")
        rep.write(f"- topic: {ctx['topic']}\n")
        rep.write(f"- date: {ctx['date']}\n\n")
        rep.write(f"- task-id: {task_id}\n")
        rep.write(f"- max-failures-per-script: {args.max_failures_per_script}\n")
        rep.write(f"- verify-level: {args.verify_level}\n")
        rep.write(f"- artifact-tags: {sorted(artifact_tags)}\n")
        rep.write(f"- run-dir: {args.run_dir or '(none)'} (source: {run_dir_source})\n")
        rep.write(f"- scenario: {args.scenario or '(none)'}\n\n")
        if openspec_fm:
            rep.write("## Openspec Frontmatter Snapshot\n")
            for k, v in sorted(openspec_fm.items()):
                rep.write(f"- {k}: {v!r}\n")
            rep.write("\n")
        if condition_notes:
            rep.write("## Mount Condition Evaluation\n")
            for n in condition_notes:
                rep.write(f"- {n}\n")
            rep.write("\n")
        rep.write("## Mounted Roles\n")
        for r in roles:
            rep.write(f"- {r}\n")
        rep.write("\n## Gate Results\n")

        for role in roles:
            role_def = matrix.get("roles", {}).get(role, {})
            for gate in role_def.get("gates", []):
                script = gate.get("script")
                raw_args = gate.get("args", [])
                rendered_args = []
                for a in raw_args:
                    if a is None:
                        continue
                    rendered = _render_arg(a, ctx).strip()
                    if rendered == "":
                        continue
                    rendered_args.append(rendered)
                run_it, reason = _should_run_gate(script, rendered_args, args.verify_level, artifact_tags, ctx)
                if not run_it:
                    rep.write(f"### {role}: {script}\n")
                    rep.write(f"- exit: SKIP\n")
                    rep.write(f"- reason: {reason}\n\n")
                    continue

                script_key = _safe_key(script)
                current_failures = int(task_state.get(script_key, 0))
                if current_failures > args.max_failures_per_script:
                    overall = EXIT_FAIL
                    rep.write(f"### {role}: {script}\n")
                    rep.write("- exit: BLOCKED\n")
                    rep.write(
                        f"- reason: failure count exceeded {args.max_failures_per_script}; human help required\n\n"
                    )
                    continue

                code, out = _run_script(script, rendered_args)
                overall = max(overall, code)
                if code == EXIT_FAIL:
                    task_state[script_key] = current_failures + 1
                rep.write(f"### {role}: {script}\n")
                rep.write(f"- exit: {code}\n")
                rep.write(f"- failure-count: {task_state.get(script_key, current_failures)}\n")
                rep.write("\n```text\n")
                rep.write(out.strip() + "\n")
                rep.write("```\n\n")

    retry_state[task_id] = task_state
    _save_retry_state(retry_state_file, retry_state)

    print(f"Report: {report_path}")
    # Auto clear when task naturally ends (Archive phase), or explicitly requested.
    if args.phase == "Archive" or args.end_task:
        _clear_task_state(retry_state_file, retry_state, task_id)
        print(f"TASK_STATE_CLEARED: {task_id}")

    if overall == 0:
        print("OK: all gates pass")
        return 0
    if overall == EXIT_WARN:
        print("WARN: gates produced warnings")
        return EXIT_WARN
    print("FAIL: gates failed")
    blocked = [k for k, v in task_state.items() if int(v) > args.max_failures_per_script]
    if blocked:
        escalation_path = f".claude/runs/escalation_card_{timestamp}.md"
        _build_escalation_card(escalation_path, task_id, args, blocked, task_state, report_path)
        print("HUMAN_HELP_REQUIRED: some scripts exceeded failure cap")
        for b in blocked:
            print(f"- {b}: {task_state.get(b)} failures")
        print(f"ESCALATION_CARD: {escalation_path}")
    return EXIT_FAIL


if __name__ == "__main__":
    raise SystemExit(main())
