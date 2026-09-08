# ADR-002 — HttpUtils.SendServer* 全局恢复「非0码抛异常」

- Status: **全局方案(B)被 Approval Gate 驳回（改动过大）→ 采纳局部方案 A′′（MEDIUM）**
- Date: 2026-05-29
- Risk: HIGH（全局方案）→ 实际采纳的 A′′ 为 MEDIUM
- 关联 Spec: `.claude/runs/20260529_155758_sendserver_throw_global/openspec.md`（run 目录，临时产物）
- 关联审计: `.claude/runs/20260529_155758_sendserver_throw_global/explore_report.md`

## Context

`Utils/HttpUtils.cs` 的 4 个 `SendServer*` 方法在某次改动中，从「服务端 `code!=0` 抛异常」被改成「记日志 + return null/""」（注释里残留的 `//throw new Exception(...)` 是证据）。后果：服务端业务错误码（70007 账号切换保护、70002 授权量不足等）被**静默吞掉**，无法透传到前端；`addOrUpdateAnchor` 等接口甚至会向前端误推「成功」。

同项目的 `Utils/HttpAsyncUtils.cs`（独立类）**保留了正确行为**——空响应/反序列化失败/非 0 码均抛 `CustomException`，并由 `BeanCache/MethodCache.cs:199-231` 在请求路径捕获、转 `{code,msg}` 回前端。即「会抛」的机制本就存在且在生产中运行。

需求「话术智能监控 addOrUpdateAnchor 改造」§5.3 要求错误码透传到前端，触发本次决策。

## Decision

**全局**把 `HttpUtils` 的 4 个 `SendServer*` 方法恢复为「`code!=0` 抛 `CustomException(code,msg)`」，对齐 `HttpAsyncUtils`；并对审计出的 ~160 处调用方按 consumption 分类逐一处理（详见 openspec §5）：
- best-effort（计费/日志/状态回传/同步）→ 调用方 catch-log，**吞**，保持「失败不抛断」；
- 需透传错误的请求路径（addOrUpdateAnchor）→ 传播，经 MethodCache/FrontNotice 回前端；
- 读路径 → 请求侧传播、后台侧补 catch。

## Decision Update (2026-05-29，Approval Gate 后)

全局方案(B)在 Approval Gate 被驳回（用户判定 ~160 处改动过大）。**最终采纳方案 A′′**（见 openspec §5）：仅改 `AnchorApi.AddOrUpdateAnchor`（异步版，换用已抛异常的 `HttpAsyncUtils.SendServerPostAsync`）+ `AnchorBll.AddOrUpdateAnchor`（catch 识别 CustomException 推真实 code/msg；UPDATE 分支深拷贝避免活缓存污染）。同步版 `AddOrUpdateAnchorSync` 因有 5 个后台/CEF fire-and-forget 调用方而**保持不变**。blast=2 文件，风险 MEDIUM。其余接口的吞码问题留待日后按方案 C 逐个迁移。

## Alternatives Considered

1. **方案 A — 局部抛（仅 addOrUpdateAnchor）**：不动共享方法，只在 `AnchorApi.AddOrUpdateAnchor` 解析 code 并抛 `CustomException`，配合 AnchorBll/Controller 改可等待。blast=3，风险 MEDIUM，能满足 5.3。
   - 否决理由（用户决策）：治标不治本，其它接口的吞码问题仍在；用户选择系统性修复。
2. **方案 B — 全局抛**（本 ADR 采纳）：根因修复，行为与 HttpAsyncUtils 统一。代价：~45 处 FIRE_FORGET 须逐个包 try-catch、~95 处读路径须复核。
3. **方案 C — 新增会抛变体 `SendServer*OrThrow`，按需迁移**：老方法不变、需要的接口改用新变体。渐进可控，但留下两套语义、长期割裂。

## Consequences

**正面**：根因修复；错误码语义全局统一；与 HttpAsyncUtils 一致；为后续接口的错误透传扫清障碍。

**负面 / 风险**：
- 改动面大（~160 站点 / 30 文件），主要成本在「给 ~45 处 best-effort fire-and-forget 包 catch-log」。
- 后台 timer/录制/分析/`async void` 路径**无全局兜底**——任何遗漏的裸调用在改动后可能崩任务。需 100% 覆盖审计清单（AC-6）。
- 计费/Token 类（UserPropertyApi 等）语义敏感：本决策定为「catch-log 不回滚」，依赖服务端权威；若未来需强一致需服务端配合。
- 本地 HttpServer 行为契约变化：addOrUpdateAnchor 从「恒成功」变「可能返回 70007」，Web 前端需据此差异化提示。

## Rollback

- 引入开关常量 `HttpThrowOnError`（默认 true）；置 false 即恢复旧「return null」行为，秒级回退。
- 全程隔离在 `feat/sendserver-throw-on-error` 分支；未合并前基线 `2.6.0.3-test` 不受影响。
- 分 Wave 实施，每 Wave 可独立编译/验证；Wave 失败回滚该 Wave 即可。

## Validation Criteria

见 openspec §7 AC-1..AC-7（错误透传 / 计费不抛断 / 日志不抛断 / 后台不崩 / 读路径 / 全覆盖 / 回退）。
