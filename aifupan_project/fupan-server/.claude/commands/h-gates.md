---
description: Phase + Scenario 感知的 gate 套件；FAIL 时起 incident 草稿（用户确认才写盘）。
argument-hint: --phase <1_Explorer|2_Propose|3_Review|4_Implement|5_QA|6_Archive> [--scenario A|B|C|D|E|DEBUG] [--run-dir <path>]
allowed-tools: Read, Write, Bash, Grep, Glob
---

跑 `run.py`（role-aware aggregator） + phase/scenario 触发的确定性 gate。本命令不跑主观 review。

## 步骤

### 1. 解析参数

必填 `--phase`。可选 `--scenario` / `--run-dir` / `--topic` / `--date` / `--profile` / `--intent`。

- `--run-dir` 缺省：`python3 .claude/scripts/harness/find_active_focus_card.py` 取父目录；找不到 → 报错停。
- `--topic` 缺省：openspec.md frontmatter `module` 或 run_dir slug。
- `--date` 缺省：今天 `YYYYMMDD`。
- `--profile` 缺省：读 `<run_dir>/openspec.md` frontmatter `spec_mode` 推断（`SLIM` → `PATCH`、`STANDARD` → `STANDARD`）；openspec 缺则 fallback `STANDARD`。
- `--intent` 缺省：`Change`。

### 2. 主入口

```bash
python3 .claude/scripts/gates/run.py \
    --intent <Change|...> \
    --profile <PATCH|STANDARD> \
    --phase <1_Explorer|2_Propose|3_Review|4_Implement|5_QA|6_Archive> \
    --topic <module> \
    --date <YYYYMMDD> \
    [--run-dir <run_dir>] \
    [--scenario <DEBUG|...>]
```

退出码：0 PASS / 1 WARN / 2 FAIL。报告写到 `.claude/runs/gates_report_<timestamp>.md`。

禁绕过 `run.py` 直拼单 gate。

### 3. Scenario-specific 叠加

按 `lifecycle.md` Part 6：

| Scenario | 命令 |
|---|---|
| B (DB Migration) | `python3 .claude/scripts/gates/migration_gate.py --sql-dir sql/` |
| C (Breaking API) | `python3 .claude/scripts/gates/api_breaking_gate.py --openspec <run_dir>/openspec.md` |
| E (Dependency) | `python3 .claude/scripts/gates/dependency_gate.py --pom pom.xml` |
| 任意 Java 改动 | `python3 .claude/scripts/gates/comment_linter_java.py --path <touched dir>` (advisory) |

QA phase 追加（`--paths` nargs=+ glob list，不能引号包整串）：

```bash
git diff --name-only HEAD~1 HEAD | xargs -r python3 .claude/scripts/gates/secrets_linter.py --paths
```

### 4. 汇总

```
[Gates] phase=<…> | scenario=<…|none>
  - run.py: PASS|WARN|FAIL (report: <path>)
  - secrets_linter: PASS|WARN|FAIL (touched: N files)
  - <scenario gate>: PASS|WARN|FAIL
Verdict: PASS | WARN | FAIL
```

### 5. FAIL → incident 建议块

不自动重跑、不代调 `/h-incident`。

提示判定：

| 信号 | 提示 |
|---|---|
| 规则缺口 + 未来可能重蹈 | 是 |
| 单次 typo / 笔误 | 否 |

提示时输出：

```
[Gates FAIL → Incident Candidate]
Gate 失败暴露的规则缺口：<一句话>
Gate 输出摘录（3-5 行）：
  <stderr/stdout 关键行>

severity=P3 / source=qa-found / area=<module>
Run: /h-incident "<gate 失败摘要 3-5 行，含 gate 名 + 关键报错>"
```

### 6. WARN

WARN 不阻塞推进，列给用户决定。不写 incident。

## 硬约束

- 单次执行禁自动重跑同 gate。
- 入口必须 `run.py`。
- 不起 incident、不模拟 `/h-incident`，FAIL 仅输出建议块。
- WARN 不输出 incident 建议块。
