<!-- module: order -->
<!-- area: domain -->
<!-- generated-by: claude-code-agent -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-api/.../task/OrderScheduledTasks.java, replay-order/.../bll/OrderBll.java, replay-order/.../bll/OrderDetailBll.java, replay-order/.../producer/OrderProducerImpl.java, replay-order/.../bll/OrderPayBll.java -->

# 订单过期 — 业务流程与规则

> 覆盖两层过期：订单生命周期终结 + 周期性资产重置。附带支付超时、手动关单/取消路径。

---

## 一、触发机制总览

| 触发方式 | 状态 | 入口 |
|---|---|---|
| **XXL-JOB 定时任务** | ✅ 主力 | `OrderScheduledTasks.expiredOrder()`，调度平台配置 cron |
| 手动触发 HTTP | ✅ 备用 | `GET /replay/order/OrderScheduledTasks` |
| Redis Key 过期监听 | ❌ 已废弃 | `RedisTimeoutOrderListener` 整类注释 |
| 应用启动扫描 | ❌ 已废弃 | `StartupEventListener.checkOrderExpired()` 已注释 |
| MQ 消息 | ❌ 不涉及 | RocketMQ consumer 不处理过期逻辑 |

---

## 二、过期入口：expiredOrder() 主流程

### 2.1 查询待过期订单

```
WHERE tb_order.tenant_id = #{tenantId}
  AND tb_order.status IN (1, 2, 7)   -- 1=未开始, 2=生效中, 7=冻结
  AND tb_order.endDate <= #{expiredTime}
```

`expiredTime` 默认 = 当天 23:59:59，可通过 XXL-JOB 参数 `aboutToExpiredHours` / `expiredTime` 自定义。

查询采用游标分页（每批 1000 条，上限 10 万条，30 分钟超时），避免大结果集 OOM。

### 2.2 分批处理策略

```
每批 1000 条订单
  └─ 拆成 20 个子组 (每组 50 条)
       └─ 每个子组：手动事务 DataSourceTransactionManager
            └─ expiredOrderProcess(orderList, ...)
```

**规则**：50 条一组是为了控制事务粒度 — 单条失败不回滚整批 1000 条。

### 2.3 订单过期 → 资产重置（两阶段）

```
Phase 1: 订单生命周期过期
  └─ expiredOrderProcess() 逐批处理状态 IN (1,2,7) 的订单

Phase 2: 资产周期重置（Phase 1 全部完成后执行）
  └─ loadExpiredAllOrderDetail(expiredTime)
       └─ 每 100 个 orderDetail 为一组
            └─ executeOrderDetailExpired(list, batchNo)
```

**关键时序**：Phase 2 必须在 Phase 1 之后执行，因为 Phase 1 结束后用户的订单状态和资产快照才一致。

---

## 三、Phase 1 详解：订单生命周期过期

### 步骤 1 — 版本套餐额外清理（仅 commodityType=1 且 status=2）

```
条件: order.commodityType == 1 && order.status == 2
动作:
  a) 查询用户所有子账号 userIds
  b) 清除 anchorBarrageNum → 0 (用户 + 子账号)
     UPDATE tb_user_property SET anchorBarrageNum = 0 WHERE userId IN (...)
  c) 关闭所有相关主播弹幕监控
     anchorUrlBll.closeAnchorBarrageNum(allUserIds)
```

**业务规则**：版本套餐到期意味着弹幕监控功能一并失效，需收回所有子账号的监控位。

### 步骤 2 — 批量终止订单 (batchStopOrder)

```
UPDATE tb_order
   SET status = 5,           -- 5=已结束(升级)
       realEndDate = NOW()
 WHERE id IN (...)

UPDATE tb_order_detail
   SET status = 3            -- 3=已过期
 WHERE orderId IN (...)

UPDATE tb_type_surplus
   SET useStatus = 2,        -- 2=已过期
       timeStatus = 2         -- 2=已过期
 WHERE surplusId IN (SELECT ... FROM tb_order_detail WHERE orderId IN (...))
```

**对应实体状态变更**：

| 实体 | 变更字段 | 过期前值 | 过期后值 |
|---|---|---|---|
| `tb_order` | `status` | 1/2/7 | 5 |
| `tb_order` | `realEndDate` | null | now() |
| `tb_order_detail` | `status` | 0/1/4 | 3 |
| `tb_type_surplus` | `useStatus` | 0 | 2 |
| `tb_type_surplus` | `timeStatus` | 0/3 | 2 |

### 步骤 3 — 自动激活待生效订单

```
查询: 同一 userId 下 status = 1 (未开始) 的订单
存在 → batchUseOrder():
  UPDATE tb_order SET status = 2, startDate = NOW()
  UPDATE tb_order_detail SET status = 1
  UPDATE tb_type_surplus SET useStatus = 0, timeStatus = 0
```

**业务规则**：用户可能提前续费购买了新版本（status=1），旧版本过期后自动启用以保证服务不中断。

### 步骤 4 — 无待生效订单时：解冻 + 创建免费版

```
不存在 status=1 的订单 → batchUserUnfreezeOrders():
  a) 解冻冻结的资产: tb_type_surplus.timeStatus 3→0
  b) 检查是否有 status=1/2 的版本套餐在使用中
     无 → 创建免费版:
       createNewOrder() (commodityType=1, status=1)
       → successOrder() (status→2)
       → 标记 afterUpgrading = -1 (阻断后续升级链)
```

**业务规则**：用户到期后不能"裸奔"，系统兜底分配免费版本。`afterUpgrading=-1` 防止免费版被当作真正的升级链路起点。

### 步骤 5 — 重算用户资产统计

```
userPropertyProducer.batchStatisticsProperty(userIds)
userPropertyProducer.batchStatisticsChildProperty(parentUserIds)
```

汇总 `tb_user_property` 表中各用户的资产使用量/总量，保证统计口径与现实一致。

---

## 四、Phase 2 详解：周期性资产重置

### 触发条件

```
SELECT ... FROM tb_order_detail
 WHERE status = 1              -- 生效中的明细
   AND nextReset <= #{expiredTime}   -- 重置时间已到
```

### 步骤 1 — 过期当前周期资产 (setExpiration)

```
1. 查询关联 tb_type_surplus (timeStatus IN 0=active, 3=frozen)
2. 对每条 surplus:
   未用量 = totalNumber - useNumber
3. 写入资产流水:
   INSERT tb_user_property_details
     (quantity = 未用量, signs = 0=减, assetCreationType = 1=系统)
4. 批量更新:
   UPDATE tb_type_surplus SET useStatus = EXPIRED, timeStatus = EXPIRED
```

**业务规则**：周期结束时，未用完的额度要记录流水（表示"这些额度到期收回"），然后标记资产过期。

### 步骤 2 — 创建新周期资产 (setNextExpirationTime)

```
条件: commodityTypeReset = 1 AND status = 1
动作:
  a) 计算新 nextReset:
     newNextReset = 当前 nextReset + resetNum × resetUnit
     (例: resetNum=1, resetUnit=MONTH → 下月同日)
  b) INSERT 新的 tb_type_surplus:
     totalNumber = 重新计算的配额
     useStatus = 0, timeStatus = 0
  c) 更新 tb_order_detail.nextReset = newNextReset
```

**示例**：AI 分析时长每月 100 分钟 → 月底 100 分钟到期收回 → 系统写入新的 100 分钟额度到下月底。

### 步骤 3 — 重算统计 + 短信通知

```
1. batchStatisticsProperty() — 重算资产总量
2. 汇总每人 AI 分析重置时长 + AI token 重置数
3. 异步发送短信:
   模板: user_rest_ai_analysis_time_sms_code
   模板: user_rest_ai_token_num_sms_code
```

---

## 五、支付超时（独立子流程）

### 入口

目前**无自动触发** — Redis 监听和启动检查均已注释。仅可通过旧版 `batchSaveAudioLog()` 间接执行，或管理员手动调用 `closeOrder`。

### 流程

```
OrderPayBll.orderExpired(orderId):

  if payStatus == 0 (未支付) && order.status == 0 (未支付):
    UPDATE tb_order_pay SET payStatus = 2   -- 支付失败
    UPDATE tb_order SET status = 6          -- 超时关闭
```

| 实体 | 变更字段 | 变更前 | 变更后 |
|---|---|---|---|
| `tb_order_pay` | `payStatus` | 0 (未支付) | 2 (支付失败) |
| `tb_order` | `status` | 0 (未支付) | 6 (超时关闭) |

---

## 六、手动操作入口

### 6.1 POST `/replay/order/orderStop` — 取消订单

```
实现: OrderLogicImpl.orderStop(orderId) → OrderBll.orderStop(..., status=8)

流程与自动过期基本一致:
  a) order.status → 8 (手动取消)
  b) order_detail.status → 3
  c) type_surplus.useStatus/timeStatus → 2
  d) 清除弹幕监控配额
  e) 检查续费订单日期调整
  f) 无在用版本 → 解冻 + 创建免费版
```

### 6.2 GET `/replay/order/closeOrder` — 关闭订单

```
实现: OrderLogicImpl.closeOrder(orderId) → OrderProducer.closeOrder(orderId, isClosePay)
  - order.status → 6 (超时关闭)
  - isClosePay=true → payStatus → 4 (支付取消)
  - PayPal/WeChat 关单 API 代码已注释
```

**区别**：`closeOrder` 仅适用于 status=0 的未支付订单；`orderStop` 适用于任何状态的订单，强制取消。

### 6.3 GET `/replay/order/OrderScheduledTasks` — 手动触发过期

直接调用旧版 `batchSaveAudioLog()`，一次性遍历所有过期订单。不推荐生产使用（无分页/无超时控制）。

---

## 七、完整状态机

### 订单状态流转（过期相关路径）

```
                    ┌─────────────────┐
                    │  0 未支付        │── closeOrder ──→ 6 超时关闭
                    └───────┬─────────┘
                            │ 支付成功
                    ┌───────▼─────────┐
                    │  1 未开始        │────────────────────────────┐
                    └───────┬─────────┘                            │
                            │ 到达 startDate / 自动激活             │
                    ┌───────▼─────────┐                            │
        ┌───────────│  2 生效中        │────────────────────┐       │
        │           └───────┬─────────┘                    │       │
        │                   │ 管理员冻结                     │       │
        │           ┌───────▼─────────┐                    │       │
        │           │  7 冻结          │                    │       │
        │           └───────┬─────────┘                    │       │
        │                   │                              │       │
        │                   ▼ endDate <= expiredTime       │       │
        │           ┌─────────────────────────────┐        │       │
        │           │  expiredOrderProcess()       │        │       │
        │           │  或 orderStop()              │        │       │
        │           └─────────────┬───────────────┘        │       │
        │                         │                        │       │
        │                         ▼                        ▼       │
        │           ┌─────────────────┐           ┌───────────────┐│
        │           │  5 已结束(升级)   │           │  8 手动取消    ││
        │           └─────────────────┘           └───────────────┘│
        │                                                           │
        └───────────────────────────────────────────────────────────┘
                              batchUseOrder (自动激活)
```

- 状态 3（已过期）在代码中存在枚举值，但实际过期不走此状态，而是走 5
- 状态 4（已退款）是退款流程，与过期无关
- 状态 8 仅管理员手动操作

### OrderDetail 状态

```
0 未开始 → 1 生效中 → 3 已过期
                       ↑
                  expiredOrderProcess / orderStop
```

### TypeSurplus 状态

```
useStatus: 0 使用中 → 2 已过期
timeStatus: 0 活跃 / 3 冻结 → 2 已过期
```

---

## 八、关键业务规则汇总

| 规则 | 说明 |
|---|---|
| **过期时间判断** | 仅看 `endDate`，不看 `startDate`；`endDate <= expiredTime` 即过期 |
| **版本套餐连带清理** | commodityType=1 过期 → 清空该用户+子账号的弹幕监控配额 |
| **自动续期** | 有 status=1 待生效订单 → 自动激活；无 → 创建免费版兜底 |
| **免费版阻断** | 系统创建的免费版 `afterUpgrading=-1`，防止被当作升级链起点 |
| **资产流水可追溯** | 周期重置时，未用额度写入 `tb_user_property_details`(流水表)，signs=0 |
| **分期事务隔离** | 50 条一组手动事务，单组失败不回滚其他组 |
| **Phase 1 → Phase 2 顺序** | 先终结订单生命周期，再处理明细层周期重置，保证快照一致 |
| **CRM 事件不发** | 过期路径不发布 `CrmOrderChangedEvent`（仅支付成功时发） |
| **支付超时无自动触发** | Redis 监听 + 启动检查均已废弃；需靠 XXL-JOB 或手动 |
| **IN 分页** | 查询使用游标分页（id > lastMaxId），不是 IN 列表，无 500 条限制 |

---

## 九、关联文档

- 订单领域词汇表 → `order_domain.md`
- 订单数据模型 → `../data/order_data.md` / `../data/order_data_detail.md`
- 订单 API 接口 → `../api/order_api.md`
- 订单架构基线 → `../architecture/order_architecture.md`
