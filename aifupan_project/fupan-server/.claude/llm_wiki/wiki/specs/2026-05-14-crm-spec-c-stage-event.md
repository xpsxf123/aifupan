# CRM Spec-C 阶段事件回传（已归档）

**状态：** Phase 6: Archive（2026-05-19 完成）

**目标：** 落地阶段事件回传能力，完成 `tb_crm_stage_event` 入库，并在受控条件下刷新 `tb_user_business` 销售快照。

## 关键接口

- `POST /internal/crm/customer/stage-event` — 回传客户阶段事件

## 业务规则

- 默认只入库，不直接覆盖当前销售状态
- 命中白名单 `stageCode`（`FOLLOW_UP_DONE`、`WAIT_NEXT_CONTACT`）时，受控刷新 `tb_user_business` 的 `according_status/content/date`

## 关键数据表

- `tb_crm_stage_event`（新增，按 `event_id` 幂等写入）
- `tb_user_business`（受控刷新快照字段）

## 主要文件

- `replay-power`: `CrmStageEventEntity`, `CrmStageEventMapper`, `CrmStageEventService/Impl`
- `replay-api`: `CrmIntegrationService/Impl`（含 `saveStageEvent`、`resolveBusinessSnapshot`）

## 归档位置

完整实现计划已于 2026-05-20 harvest 提取并删除（archive/20260514_crm_spec_c_stage_event.md）。
稳定知识（快照刷新白名单）已合并至 [wiki/data/crm_data.md §五](../data/crm_data.md)。
原始文件可通过 `git log --diff-filter=D --follow -- .claude/llm_wiki/archive/20260514_crm_spec_c_stage_event.md` 恢复。
