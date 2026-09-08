# Testing & Evidence

This domain defines testing expectations and how to store objective evidence after implementing a feature or fixing a bug.

## Core Testing Strategies
- Build verification: `msbuild ReviewAnalysis.sln /p:Configuration=Debug /p:Platform=x64` (MAX 2 次重试)
- 功能验证: 手动执行关键路径操作 (主播授权 → 录制 → 分析 → 报告), 检查 UI 行为和日志
- 门禁脚本: `python3 .claude/scripts/gates/run.py` (secrets_linter, scope_guard, writeback_gate 等)

## Test Evidence Schema (MUST)
At the end of `QA Test` (or after a bug fix), the Agent MUST generate an objective evidence file named:
- `test_evidence_{feature_name}.md`

This file is used by hooks for verification and is archived during `Archive`.

### Evidence Template
```markdown
# Test Evidence: {feature_or_bug_name}

## 1. Environment & Commands
- Build command: `msbuild ReviewAnalysis.sln /p:Configuration=Debug /p:Platform=x64`
- Test scope: `[classes/methods from the scope of change]`

## 2. Objective Logs (Snippets)
- Paste the minimal pass logs proving the compilation was green.
- Failed retries: `N`

## 3. Covered Edge Cases
- [Pass] Concurrent duplicate submission returns 409.
- [Pass] Cross-tenant query returns 403.
- [Pass] Happy path.

## 4. Coverage Metrics (Optional)
- Manual verification steps performed
- UI behavior screenshot evidence
```

## Archived Evidence Index
(This section is appended during `Archive`.)

### Append Template
```markdown
- [{YYYY-MM-DD}] {Feature/Bug Name}: `[{evidence_file.md}]`
```

### 2026
- No entries
