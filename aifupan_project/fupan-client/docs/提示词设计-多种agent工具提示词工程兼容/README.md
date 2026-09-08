# 提示词设计：多 Agent 兼容补丁

本目录用于沉淀“多 Agent 共用一套提示词工程单一来源”的设计方案、落地方式与迁移清单。

## 文档清单

- `多Agent兼容与访问补丁设计.md`
  - 说明为什么会出现“入口文件被读了，但后续规则没有继续读取”的问题
  - 定义统一单一来源、桥接层、自动加载层、hooks 注入层的职责边界
- `Claude-Code兼容补丁落地说明.md`
  - 说明本项目如何为 Claude Code 建立 `根目录 CLAUDE.md + .claude/CLAUDE.md + .claude/rules + SessionStart/PostToolUse hooks` 桥接层
- `多Agent迁移清单.md`
  - 提供可交给其他项目的迁移步骤、最小复制清单与验证方式
- `补丁包与适配器抽象方法.md`
  - 抽象出跨工具通用的“适配器方法”，指导任何 Agent 工具自己自检入口规则并落地兼容层
- `patch/`
  - 可复制的补丁包模板：桥接清单示例、适配器检查表

## 本方案的核心目标

- 保持 `agnet/` 为单一来源
- 允许 Claude Code、Trae、Codex 等工具共用同一套提示词工程资产
- 避免每个 Agent 都各自维护一套 `rules/wiki/memory/skills`
- 通过“根入口摘要 + 工具目录详细配置 + rules + hooks”，将“基础规则自动加载”变成确定性行为

## 推荐阅读顺序

1. `多Agent兼容与访问补丁设计.md`
2. `Claude-Code兼容补丁落地说明.md`
3. `多Agent迁移清单.md`
