---
name: "技能图谱治理器"
description: "维护 skills 的可发现性：强制更新 skills/INDEX.md、补齐关联技能与写回记录，避免技能体系失控。"
---

# 技能图谱治理器

## 技能目的
- 让新增/修改的技能能被快速找到：索引、关联、入口齐全
- 降低“技能越来越多但没人用”的熵增
- 将结构性变更记录到 CHANGE_LOG.md，便于审计与回滚

## 输入参数
- `skill_changes`：必填，列表。新增/修改的 skill 信息（名称、目录、用途）
- `index_text`：选填，文本。当前 `skills/INDEX.md` 内容（未提供则读取）
- `agent_index_text`：选填，文本。当前 `INDEX.md` 内容（用于挂载入口，按需）

## 输出
- `skills_index_patch`：对 `skills/INDEX.md` 的增量补丁建议
- `skill_cross_links`：每个 skill 应补充的“相关技能”列表（双向）
- `agent_index_links`：是否需要在 `INDEX.md` 增加/更新关系边
- `changelog_entry`：是否需要新增 CHANGE_LOG 条目（结构/规则变更时必出）

## 治理规则（强制）
- 新增 skill 必须：
  - 在 `skills/INDEX.md` 注册
  - 在 skill 文档内写清：输入/输出/边界/失败处理
- 结构性变更（新增分组/新增目录约定/新增写回流程）必须：
  - 写入 `CHANGE_LOG.md`
- 若新增 skill 与既有 skill 存在组合关系，必须：
  - 双向补齐“相关技能”

## 适用场景
- 新增 skills、重组 skills 分组、引入新的写回流程（WAL/归档/质量门禁）

## 边界
- 不擅自改动业务代码结构；只对智能体资料（skills/wiki/index 等）做治理
- 涉及安全边界/禁止行为/权限边界的规则修改必须显式确认后才能落盘
