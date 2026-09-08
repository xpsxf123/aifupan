# CRM Spec-B 画像写回（已归档）

**状态：** Phase 6: Archive（2026-05-19 完成）

**目标：** 为 CRM 项目落地画像写回能力 — 结构化字段同步到 `tb_user_details`，新增 `tb_crm_ai_profile` 持久化 AI 全量画像。

## 关键接口

- `POST /internal/crm/customer/profile-sync` — 同步客户画像结构化字段
- `POST /internal/crm/ai-profile/upsert` — 写入 AI 全量画像 JSON

## 关键数据表

- `tb_user_details`（更新 `user_ambition`、`user_belong_type`）
- `tb_crm_ai_profile`（新增，按 `profile_id` 覆盖写）

## 主要文件

- `replay-power`: `CrmAiProfileEntity`, `CrmAiProfileMapper`, `CrmAiProfileService/Impl`
- `replay-api`: `CrmIntegrationService/Impl`（含 `profileSync`、`upsertAiProfile`）

## 归档位置

完整实现计划已于 2026-05-20 harvest 提取并删除（archive/20260514_crm_spec_b_profile_writeback.md）。
稳定知识已合并至 [wiki/api/crm_api.md](../api/crm_api.md) 和 [wiki/data/crm_data.md](../data/crm_data.md)。
原始文件可通过 `git log --diff-filter=D --follow -- .claude/llm_wiki/archive/20260514_crm_spec_b_profile_writeback.md` 恢复。
