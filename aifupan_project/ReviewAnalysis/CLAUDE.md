# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ReviewAnalysis 是一个 Windows 桌面客户端（.NET Framework 4.7.2 + WinForms），用于 **直播 / 短视频复盘分析**：录制主播直播与短视频，经 ASR 语音识别 + AI 分析处理，采集电商平台数据（巨量 / 千川 / 本地生活 / 企业 / 抖音直播 / 视频号），并展示复盘结果。CefSharp（内嵌 Chromium）负责各平台的授权登录与 Cookie 捕获。

核心业务管线：
1. 主播录播采集 → ASR 识别 → AI 分析 → 结果展示
2. 短视频上传 → ASR 识别 → AI 配音检测
3. 平台数据采集 → 实时监控 → 告警
4. 主播授权 → Cookie 管理 → 数据拉取

## Build, Run & Test

- 解决方案 `ReviewAnalysis.sln`（单一项目 `ReviewAnalysis.csproj`，旧式 csproj + `packages.config`）；输出 `WinExe`，x64，`AssemblyName=ReviewAnalysis`。
- **仅能在 Windows 上构建与运行。** .NET Framework 4.7.2 + WinForms + CefSharp + 原生 DLL（`librecord.dll`、`libcrypto-3-x64.dll` 等）无法在 macOS 开发机上 build 或运行；作者在 macOS 开发、在独立 Windows 机器上构建与测试。
- 构建（Windows）：`dotnet build ReviewAnalysis.sln`（lifecycle.md QA 阶段的门禁命令），或在 VS 开发者命令行执行 `msbuild ReviewAnalysis.sln /p:Configuration=Debug /p:Platform=x64`。
- 运行：启动构建产物 `ReviewAnalysis.exe`（通过命名 `Mutex` 强制单实例；入口 `Program.Main` → `FormMain`）。
- 测试：**当前无测试项目。** 新增测试遵循 `csharp-unit-testing` 技能：xUnit + NSubstitute + FluentAssertions。

## Architecture

入口 `Program.cs`（`Main` → `Application.Run(new FormMain())`）；`FormMain` 定义在 `Form1.cs` 中（约 3,100 行的 God Class，待拆分）。

四层架构 —— 调用方向严格向下：

```
UI/Form 层    (Form1.cs=FormMain、各平台 *Form.cs、SplashScreen)
   ↓
Controller/   (18 个 Controller；仅做委托；*AsyncController 为 async 变体)
   ↓
Bll/          (业务逻辑 / 状态；God Class：AnchorVideoBll、Bll/Anchor/*)
   ↓
api/ (后端 HTTP 客户端) + Db/ (SQLiteHelper* —— 按表/领域各一个)
```

关键子系统：
- **Asr/** —— 语音识别管线：音频上传 → 云存储（Tencent COS/VOD、QiNiu）→ ASR HTTP → `ASRResultEntity`/`SentenceMarkVo`。被直播录播、短视频、AI 配音检测三方消费。
- **Ai/** —— AI 分析提供商抽象：`IAiChatService` + `AiFactory`（provider 模式）+ `impl/`；`auto/AiAutoTimer` 调度自动分析。
- **Websocket/** —— 直播数据接入：`WebsocketConnection` + `WebsocketDataHandle`，抖音直播间弹幕 / 在线人数。
- **平台模块**（`juliang` / `qianchuan` / `life` / `enterprise` / `anchorLive` / `WeChatChannels`）—— 每个模块经 CefSharp `*Form` 授权、`CookiePersistenceHelper` 持久化 Cookie，并通过 `*DataCollectionManager` / `*DataPoller` / `*DataApi` 轮询平台接口。新增或修改模块 **必须** 继承 `PlatformDataCollectionManagerBase<TPoller>`（见 platform_module_rules.md）；`juliang` 早于基类、体量最大。
- **BackgroundWork/** —— `DetectionBackgroundManager`（定时检测主播开播状态）、`FristPageIni`（启动期长任务）。
- **Db/** —— 基于 `System.Data.SQLite`；`SQLiteHelper*` 按关注点拆分（audio / online-num / upload-file / video-record …）；`Model/` 为行实体；`DataTableToEntityMapper` 负责 `DataTable`→实体映射。
- **HttpServer/** —— 本地 `HttpListener` 服务（`HttpListenerAsyncServer`、`HttpResourceFileServer`）。

重型原生 / 三方依赖：CefSharp 135（内嵌 Chromium）、`librecord.dll`（录制）、`System.Data.SQLite`、`Newtonsoft.Json`。

---

## Hard Rules

1. **Anti-loop**: max 3 retries per gate/linter, max 2 for `dotnet build`. Exceed → STOP, ask human.
2. **中文优先**：面向用户的提问、TaskList 标题、进度更新、总结一律用中文。代码、路径、命令、日志、commit message 保持英文。
3. **注释强制**：所有新建/修改的类、方法、属性 **必须** 写详细注释。注释格式按语言区分：
   - **C#**：XML 文档注释 `/// <summary>`、`/// <param>`、`/// <returns>`、`/// <remarks>`
   - **Java**：Javadoc `/**  */` 含 `@param`、`@return`、`@throws`
   - **TypeScript / JavaScript**：JSDoc `/**  */` 含 `@param`、`@returns`、`@remarks`
   - **Python**：docstring `"""  """`（Google style 或 reStructuredText）
   - **Vue / React 组件**：Props 接口 JSDoc + 组件顶部功能性注释
   
   注释覆盖范围：每个类（职责说明）、每个 public/internal 方法（功能 + 参数 + 返回值）、每个属性（含义 + 约束）、关键算法步骤（为什么这样做）。违反 → Phase 5 QA code-review 直接 FAIL。

<HARD-GATE id="design-before-code">
STANDARD: do NOT Edit/Write `**/*.cs`, `*.csproj`, `*.sln`, `App.config`, `Web.config` until `.claude/runs/<run_id>/openspec.md` §2 Scope AND §5 ACs filled AND Phase 3 Review passed. Violation → STOP, emit `[Plan Invalidation]`, roll back to Propose.
</HARD-GATE>

<HARD-GATE id="csproj-register-new-cs">
Every new `.cs` MUST be registered as `<Compile Include="..." />` in `ReviewAnalysis.csproj` in the same edit. Violation → STOP before `[Status]: PASS`.
</HARD-GATE>

<HARD-GATE id="evidence-before-archive">
Archive requires per-AC mapping: `AC-id → command → output → PASS|FAIL`. No mapping → do NOT mv to archive, do NOT mark `Archived`. PATCH inline; STANDARD in `delivery_capsule.md`.
</HARD-GATE>

Full safety/commit/artifact policy: [.claude/rules/policy.md](.claude/rules/policy.md).

## Behavior

- Think before coding: state assumptions, surface tradeoffs, ask if unclear.
- Simplicity first: minimum code that solves the problem; no speculative features.
- Surgical changes: touch only what the task requires; match existing style.
- Goal-driven: define success criteria before acting; loop until verified.
- Skills are reference, not preflight: open `SKILL.md` only on specific decisions.

## TaskList

- Use TaskList for ≥ 3 distinct steps or plan-mode runs; skip for single trivial step.
- Granularity: PATCH 2-4 tasks per Phase; STANDARD 6-10; HIGH 10-15 by Wave.
- Mark `in_progress` BEFORE acting; `completed` immediately after; never batch.
- Format `<Phase>: <verb> <object>`; subject ≤ 60 chars, imperative.

## Modes

| Mode | Profile | Flow |
|---|---|---|
| **Vibe** | LEARN / PATCH(TRIVIAL) | Act directly. No spec, no Explorer, no WAL. |
| **Patch** | PATCH(LOW) | Slim Spec (scope + AC) → Implement → QA → Archive. |
| **Research** | RESEARCH | Investigate → Synthesize → Archive. Risk-orthogonal. Produces `research_report.md`. |
| **Standard** | STANDARD (MEDIUM \| HIGH) | Explorer → Propose → Review → [Approval if HIGH] → Implement → QA → Archive |

Risk assessment (3-dimension impact: blast radius × breaking change × business impact) + attention signals + shortcut DSL: [.claude/rules/risk-profiles.md](.claude/rules/risk-profiles.md). Force mode with `@vibe`/`@patch`/`@standard`/`@learn`/`@research`. Probe-red signal under `@vibe`/`@patch` → emit `[Probe Override]`. **Slim Spec ≠ Slim WAL**: Phase 2 design contract vs Phase 6 stable knowledge — see [slim_spec_schema](.claude/wiki/schema/slim_spec_schema.md) + [wal-policy §7](.claude/rules/wal-policy.md).

## Session Start

1. Read this file. `lifecycle.md` is `@import`-loaded; other `.claude/rules/*.md` lazy-load.
2. Resume interrupted: read `.claude/runs/_active.json` then restore from Phase.
3. Concrete paths/snippets in prompt: Read directly.
4. Ambiguous intent: ask one question, then proceed.
5. No `[Triage Probe]` block this turn: act directly.

Vibe → act, no classification line. Patch → emit one-paragraph Slim Spec first. Standard → emit `[Risk: HIGH | Scenario: B] → openspec.md required` before any output.

## C# / .NET Framework 4.7.2 Constraints

Wiki (`preferences/*_rules.md`) is SSOT; table below is fast-lookup mirror. Violations trigger `<HARD-GATE>` or block Approval Gate.

| Constraint | Rule |
|---|---|
| Naming | PascalCase public members; no `updateXxx`/`getXxx` lowercase Java-style |
| Async | `async Task` only; `async void` forbidden outside WinForms event handlers; never `.Result` / `.Wait()` / `.GetAwaiter().GetResult()` |
| Resources | Every `IDisposable` in `using`; `Timer` stored as field + disposed |
| Threading | `new Thread()` forbidden — use `Task.Run`; lock = `private static readonly object`; `ConcurrentDictionary<K, List<T>>` value type must be thread-safe |
| Architecture | 4-layer (UI → Controller → Service/BLL → Api/Db); Controller delegation-only; BLL must NOT instantiate WinForms; class ≤ 500 lines, method ≤ 80 lines |
| DI | Constructor injection with interfaces; no `new XxxBll()` in business logic |
| HTTP | `HttpClient` as static singleton; never `using (new HttpClient())` |
| SQL | Parameterized only (`@param`); dynamic table/column names require whitelist |
| Secrets | No hardcoded API keys / tokens / passwords / URLs; secrets via `ConfigManager.GetSecret()` |
| Platform modules | 6 modules MUST inherit `PlatformDataCollectionManagerBase<TPoller>`; ≤ 5 files / ~400 lines per new platform |
| External data | `JObject.TryParse` not `Parse`; null-check before index; collections check `Count > 0` before `[0]` |

Full rules: [coding_standards](.claude/wiki/wiki/preferences/coding_standards.md) · [architecture_rules](.claude/wiki/wiki/preferences/architecture_rules.md) · [security_rules](.claude/wiki/wiki/preferences/security_rules.md) · [performance_rules](.claude/wiki/wiki/preferences/performance_rules.md) · [platform_module_rules](.claude/wiki/wiki/preferences/platform_module_rules.md).

## SSOT

| Topic | File |
|---|---|
| Phase + Roles + Approval Gate | [lifecycle.md](.claude/rules/lifecycle.md) |
| Hooks + budgets + dispatch + anti-bloat + commit | [policy.md](.claude/rules/policy.md) |
| 3-dimension impact assessment + risk tiers + Triage Probe | [risk-profiles.md](.claude/rules/risk-profiles.md) |
| 5-section dispatch contract | [dispatch-template.md](.claude/rules/dispatch-template.md) |
| Skill Zones (A/B/C/D/N/U/M) | [skill-precedence.md](.claude/rules/skill-precedence.md) |
| WAL dimensions + tags + paths | [wal-policy.md](.claude/rules/wal-policy.md) |
| Agents · Skills · Wiki | [.claude/agents/](.claude/agents/) · [.claude/skills/](.claude/skills/) · [KNOWLEDGE_GRAPH](.claude/wiki/KNOWLEDGE_GRAPH.md) |
| Schemas (openspec / Slim / ADR / subagent return) | [.claude/wiki/schema/](.claude/wiki/schema/) |

@.claude/rules/lifecycle.md
