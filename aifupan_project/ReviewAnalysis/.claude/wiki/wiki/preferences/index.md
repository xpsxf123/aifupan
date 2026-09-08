# Preferences Index (Constraints & Anti-Patterns)

This domain stores project-specific constraints, preferences, and do-not-do rules.

Hard rules:
- Before any architecture design or code change, you MUST read the relevant rules in this domain.
- These rules are enforced via hooks. If violated during `Propose` or `Implement`, the workflow MUST fail and roll back.

## Core Preferences
- **[Coding] [MUST] C# 编码规范**: 命名、async/await、异常处理、资源管理、空安全 → [coding_standards.md](coding_standards.md)

## Constraints (Security, Architecture, Performance)

### Security Baseline
- No secrets in code: DO NOT hardcode API keys, secrets, or passwords in code or `application.yml`. Use environment variables or a config center.
- Authorization first: every API MUST enforce tenant/user authorization by default. DO NOT allow ID enumeration unless explicitly public.
- SQL injection: all queries MUST use parameterized queries. No string concatenation for SQL.
- Detailed rules: [security_rules.md](security_rules.md)

### Architecture Baseline
- Thin Controllers: Controllers MUST NOT contain business logic, thread creation, or direct cache/API access.
- No BLL-to-UI: BLL/Service MUST NEVER create UI elements (Form, Dialog, Browser control).
- Dependency Injection: new services MUST use constructor injection with interfaces. No `new XxxBll()`.
- No God Classes: max 500 lines per class, max 80 lines per method.
- No public static mutable fields.
- Detailed rules: [architecture_rules.md](architecture_rules.md)

### Platform Module Baseline
- All platform modules MUST inherit from base classes. No copy-paste of boilerplate.
- Unified `PlatformAuthStatus` enum. No per-platform auth enums.
- New platform: max 5 files, ~400 lines.
- Detailed rules: [platform_module_rules.md](platform_module_rules.md)

### Performance Baseline
- No N+1: DO NOT run DB queries or RPC calls inside loops. Use batch queries and in-memory assembly.
- Avoid full table scans: all query patterns MUST hit indexes.
- HttpClient singleton: NEVER create HttpClient per request.
- No sync-over-async: NEVER use `.Result` / `.Wait()` on Tasks.
- Detailed rules: [performance_rules.md](performance_rules.md)

## WAL (Write-Ahead Log)

- [2026-05-23] SVS GPU EP (DirectML) 配置规则 + 进程级 sticky CPU 兜底 + ORT 升级 bin/obj 清理 SOP: `[wal/20260523_asr_gpu_ep_fallback_rules.md]`
- [2026-05-21] ASR 上报字段名 PascalCase + JsonProperty 显式契约 + 诊断日志清理: `[wal/20260521_asr_serialization_rules.md]`
- [2026-05-19] ASR gapMs 阈值化 + Sum 偏移累加 + 单例引擎配置耦合反模式: `[wal/20260519_asr_gapms_offset_rules.md]`
- [2026-05-14] ASR 本地引擎规则与经验教训: `[wal/20260514_asr_local_engine_rules.md]`

## Archive Extraction SOP
During `Archive`, the Agent MUST ask the human for a 1–10 rating.
- If rating <= 5, extract the root cause as an anti-pattern and append it here.
- If rating >= 8, extract the praised practice and append it to Core Preferences.

### Append Template
```markdown
- **[{Tags}] [{Level}] {short rule}**: {what to do / what not to do, and why}
```
*Note: `[Tags]` can be `[Security]`, `[Performance]`, `[DB]`, etc. `[Level]` must be `MUST`, `SHOULD`, or `NEVER`.*
