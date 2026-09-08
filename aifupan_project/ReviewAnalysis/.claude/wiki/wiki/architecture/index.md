# Architecture Index (Baselines & ADRs)

This domain records architecture baselines and ADRs (Architecture Decision Records).

## Hard Rules (MUST)
- When making cross-cutting technical choices (module boundaries, middleware, global patterns), you MUST consult existing ADRs or add a new one.

## 项目总览
- **[项目总览](project_overview.md)**: 架构、技术栈、模块全景

## Baselines & Guards
- Security baseline: [../preferences/security_rules.md](../preferences/security_rules.md)
- Architecture rules: [../preferences/architecture_rules.md](../preferences/architecture_rules.md)
- Platform module rules: [../preferences/platform_module_rules.md](../preferences/platform_module_rules.md)
- Coding standards: [../preferences/coding_standards.md](../preferences/coding_standards.md)
- Performance rules: [../preferences/performance_rules.md](../preferences/performance_rules.md)

## ADR List

| ADR # | Title | Status | Decision Summary | Date | Doc Link |
|---|---|---|---|---|---|
| ADR-ARCH-01 | 混合桌面架构 | Active | WinForms + CefSharp 嵌入式浏览器，本地 HttpListener 提供 API 给前端页面 | 2026-04-23 | `[project_overview.md]` |
| ADR-DATA-01 | SQLite 数据持久化 | Active | SQLite + EF6 + 多 SQLiteHelper 分表管理，文件数据库免部署 | 2026-04-23 | `[project_overview.md]` |
| ADR-PLUGIN-01 | 插件化数据采集 | Active | PlatformDataManager → Collector → Adapter → Processor 统一多平台数据采集 | 2026-04-23 | `[project_overview.md]` |
| ADR-AI-01 | AI 分析集成 | Active | ChatCompletions + 腾讯云 ASR，支持直播内容智能诊断与语音转文字 | 2026-04-23 | `[project_overview.md]` |

---

## Archive Extraction SOP
If `Propose` makes a global architecture decision, you MUST write it back here during `Archive`.

### Append Template
```markdown
| ADR-{XXX} | {decision title} | Accepted | {one-line reason} | {YYYY-MM-DD} | `[{doc_link}]` |
```
