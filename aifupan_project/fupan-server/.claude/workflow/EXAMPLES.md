# STANDARD Task — Multi-Turn Walkthrough

A concrete walk-through showing how a Standard task flows across turns, respecting Human-in-the-Loop gates.

---

## Scenario: 给主播管理新增一个客户端取消监控接口 `POST /replay/words/anchorUrl/cancelMonitor`

**Risk:** MEDIUM (new external API, core business path 主播监控)
**Mode:** Standard
**Approval Gate:** No (MEDIUM = FYI only)

---

## Turn 1 — Explorer

**User:** "新增取消监控的接口，登录用户对自己的主播可以执行；已经取消的不允许重复操作。"

**Agent actions:**
1. Read `.claude/llm_wiki/wiki/preferences/index.md` (security baseline)
2. Read `.claude/llm_wiki/wiki/domain/words_domain.md` to confirm vocabulary
3. Read `.claude/llm_wiki/wiki/api/words_api.md` to find sibling endpoint (`againAdd`, `thoroughlyDelete`)
4. **Specification Inference:**
   - Current: `AnchorUrlController` 有 `thoroughlyDelete` 和 `againAdd`，无单独 "取消监控" 动作；监控开关分散在 `tb_anchor_url_user` 的多个字段
   - Required: 新增动作 — 仅置 `monitoring=0`，保留主播记录
   - Delta: 新增 Controller 方法 + Service 方法 + 状态守卫（已 0 则报错）
5. **AC translation:**
   - AC-001: Given 用户对该主播有归属 + monitoring=1, when POST cancelMonitor, then monitoring=0 + 返回 R<Boolean>(true)
   - AC-002: Given 用户不归属该主播, when 调用, then 抛 BusinessException(AUTH_FAIL) → R<>(403)
   - AC-003: Given monitoring 已为 0, when 调用, then 抛 BusinessException(ANCHOR_NOT_CANCELLABLE) → R<>(409)

**Agent output:**
```
[Intent: Change | Mode: Standard | Risk: MEDIUM] → Phase: Explorer

Gap: AnchorUrl 缺独立"取消监控"动作。
Need: cancelMonitor(anchorUrlId, userId) 带 ownership + monitoring 状态守卫。
AC: 3 个可测准则。
Proceeding to Propose.
```

---

## Turn 2 — Propose + Review (FYI, no gate)

**Agent actions:**
1. Design — single approach: 在现有 AnchorUrlService 加 cancelMonitor 方法（避免引入新状态机），由 Controller 直接调用。
2. Emit Constraint List:
   - 写操作落在 Service 层 `@Transactional`
   - 错误统一抛 `BusinessException`
3. Write `<run_dir>/openspec.md` per template, with endpoint contract + AC + Allowed Scope
4. Write `<run_dir>/focus_card.md` with Allowed Scope locked

**Agent output (FYI):**
```
[Propose + Review complete]

Design: POST /replay/words/anchorUrl/cancelMonitor
- Service: AnchorUrlService.cancelMonitor(Long anchorUrlId, Long userId)
- Auth: 当前登录 userId 必须等于 tb_anchor_url_user.userId
- Guard: monitoring 必须为 1，否则 ANCHOR_NOT_CANCELLABLE
- Allowed Scope: AnchorUrlController.java, AnchorUrlService.java (+ Impl)

Spec: .claude/runs/Change__2026-05-19_2030/openspec.md
(FYI — proceeding to Implement unless objection)
```

---

## Turn 3 — Implement

**Agent actions:**
1. Read `<run_dir>/openspec.md` Machine Section
2. Implement: `AnchorUrlController.cancelMonitor()` → `AnchorUrlService.cancelMonitor()` 用 `LambdaUpdateWrapper` 更新单字段
3. Run `mvn compile -q` — PASS
4. **STOP (Yield)** — do not auto-test

**Agent output:**
```
[Implement complete — compile OK]
Modified: AnchorUrlController.java (+15/-0), AnchorUrlService.java (+1/-0), AnchorUrlServiceImpl.java (+22/-0)
Awaiting permission to proceed to QA.
```

---

## Turn 4 — Human: "ok 跑 QA"

**Agent actions (QA):**
1. `mvn clean compile` — PASS
2. (No test infra for this module) 手工列出验证步骤：
   - 启动 `ReplayApiApplication`，看日志无 BeanCreationException
   - curl 三个用例
3. Evidence Mapping Table (3 ACs):

```
| AC | Verification | Expected | Actual | Status |
| AC-001: owner + monitoring=1 | curl POST -H "token: xxx" | R{code:200, data:true} + DB monitoring=0 | (待用户验证) | PENDING |
| AC-002: 非归属 | curl POST 别人主播 | R{code:403, msg:无权限} | (待用户验证) | PENDING |
| AC-003: monitoring=0 | curl POST 已取消 | R{code:409, msg:不可取消} | (待用户验证) | PENDING |
```

(测试基础设施缺失 → 由用户接手手工 smoke test，签字后归档)

---

## Turn 5 — Archive

**Agent actions:**
1. Move `<run_dir>/openspec.md` → `.claude/llm_wiki/archive/20260519_anchor_url_cancel_monitor.md`
2. Append changelog: `| 2026-05-19 | STANDARD | 新增主播取消监控接口 | [link](./20260519_anchor_url_cancel_monitor.md) |`
3. Run `python3 .claude/scripts/wiki/harvest_threshold.py --module words` — if it reports ≥ threshold, mention to user (do NOT auto-dispatch harvester; harvest is user-initiated via `@harvest`).

**Agent output:**
```
→ Phase: Archive. Role: @documentation-curator
Archive: .claude/llm_wiki/archive/20260519_anchor_url_cancel_monitor.md
Changelog: +1
Threshold: words module 2/3 new archives — not yet ripe for @harvest.
Task complete.
```

---

## Key Observations

- **5 turns** for a MEDIUM task: Explorer → Propose/Review → Implement → **YIELD** → QA → Archive
- **1 explicit stop**: Implement Yield (Turn 3→4) — no Approval Gate for MEDIUM
- **Single artifact**: `openspec.md` is the task contract — no brake snapshot, no delivery capsule, no explore_report
- **Evidence Mapping Table** keeps QA objective even when tests are manual
- **HIGH risk** adds: Approval Gate after Propose (turn 2 STOPs at WAITING_APPROVAL), ≥2 ADR alternatives in openspec Human Section
