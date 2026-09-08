# CRM Spec-D 订单事件出站（已归档）

**状态：** Phase 6: Archive（2026-05-19 完成）

**目标：** 在订单完成后发布 Spring 事件，由监听器聚合订单/客户/销售信息调用智能体侧 `POST /api/internal/agent/order/event`。

## 关键流程

```
OrderBll.successOrder()
  → publishEvent(CrmOrderChangedEvent)
    → CrmOrderChangedEventListener.handleCrmOrderChangedEvent()
      → 聚合 user/sales/order
        → CrmOrderEventPushService.pushOrderEvent()
          → POST /api/internal/agent/order/event
```

## 事件类型映射

| `orderType` / `commodityType` | eventType |
|---|---|
| orderType=1 | UPGRADE |
| orderType=3 | RENEWAL |
| commodityType=3 | INCREMENT |
| 其他 | PAY_SUCCESS |

## 配置

```yaml
crm:
  agent:
    base-url: https://salescoach.ifupan.com
    api-key: ${CRM_AGENT_API_KEY:}
    order-event-path: /api/internal/agent/order/event
```

## 主要文件

- `replay-order`: `CrmOrderChangedEvent`, `CrmOrderEventService`（发布器）, `OrderBll`（触发点）
- `replay-api`: `CrmOrderChangedEventListener`, `CrmOrderEventPushService`, `CrmAgentIntegrationProperties`

## 归档位置

完整实现计划已于 2026-05-20 harvest 提取并删除（archive/20260514_crm_spec_d_order_event.md）。
稳定知识（crm.agent 配置项）已合并至 [wiki/api/crm_api.md §三](../api/crm_api.md)。
原始文件可通过 `git log --diff-filter=D --follow -- .claude/llm_wiki/archive/20260514_crm_spec_d_order_event.md` 恢复。
