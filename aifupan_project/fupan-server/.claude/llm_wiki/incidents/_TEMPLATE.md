---
date: YYYY-MM-DD
area: replay-<module>
severity: P0 | P1 | P2 | P3
source: prod-alert | customer-report | qa-found | post-mortem | rca-archive
status: fixed | mitigated | open
---

# 一句话标题（事故本质，≤50 字 — failure_memory 会读这行）

## Symptom
（1-3 句观察到的现象）

## Root cause
（1-3 句真实根因，不是"重启就好了"那种）

## Fix / mitigation
（做了什么，要带文件/类名定位）

## Reflex
（未来写代码时该 reflex 检查的具体动作 — 这条是给未来 LLM 看的）

<!-- 文件命名: YYYY-MM-DD__<area>__<short-slug>.md
     例:      2026-05-18__replay-words__anchor_task_stuck_running.md
     约定见:  ./README.md -->
