# CRM Spec-A 查询 API（已归档）

**状态：** Phase 6: Archive（2026-05-19 完成）

**目标：** 为 CRM 项目落地第一批对智能体开放的查询接口 — 批量聚合查询和单手机号订单查询两条只读链路。

## 关键接口

- `POST /internal/crm/customer/batch-aggregate-query` — 批量聚合查询（≤50 手机号；客户+订单+销售+跟进）
- `GET /internal/crm/customer/order-by-phone` — 按手机号查订单

## 主要文件

- `replay-power`: `CrmBatchAggregateQueryBo`, `CrmBatchAggregateQueryVo`, `CrmOrderQueryByPhoneVo`
- `replay-api`: `CrmIntegrationController`, `CrmIntegrationService/Impl`（含 `batchAggregateQuery`、`queryOrderByPhone`）

## 归档位置

完整实现计划已于 2026-05-20 harvest 提取并删除（archive/20260514_crm_spec_a_query_apis.md）。
稳定知识已合并至 [wiki/api/crm_api.md](../api/crm_api.md)。
原始文件可通过 `git log --diff-filter=D --follow -- .claude/llm_wiki/archive/20260514_crm_spec_a_query_apis.md` 恢复。
