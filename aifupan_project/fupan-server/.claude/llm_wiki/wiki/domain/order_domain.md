<!-- module: order -->
<!-- area: domain -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-order/, replay-api/.../controller/order/ -->

# Order Domain — 业务概念与词汇表

> replay-order 模块核心业务概念定义。负责商品/套餐/订单/支付/邀请码/用户资产以及向 CRM 子系统出站订单生命周期事件。Agent 在 Explorer/Propose 阶段必须使用本术语表，避免领域漂移。

---

## 一、模块定位（一句话）

replay-order 是付费体系骨干模块：承载**商品（资源原子）→ 商品价格（计费单元）→ 套餐（版本组合）→ 订单（购买行为）→ 用户资产（运行时配额消费）**的完整链路，并通过 `tb_invitation_code*` 提供线下/活动渠道兑换能力，通过 `CrmOrderChangedEvent` 向 CRM 出站订单变更信号。

---

## 二、核心概念

| 概念 | 定义 | 关联概念 | 状态机 |
|---|---|---|---|
| **Commodity (商品)** | 资源原子：一个 commodity 绑定一个 `commodity_type`（如"分析时长"、"监控位"），用于组装成套餐/增量包 | CommodityType, CommodityPrice, Package | `status`: 0未上架/1已上架 |
| **CommodityType (商品类型)** | 资源类别枚举：定义资源 `code`（如 `aiAnalysisTime`、`monitorNum`、`anchorNum`）、单位、是否周期重置；详见 `OrderEnums.commodityTypeCode` | Commodity, TypeConsumption | — |
| **CommodityPrice (商品价格)** | 商品/套餐的可售价格档位：原价 + 折扣 + 真实价 + 有效期单位（小时/天/月/季/半年/年）；type=0 商品价格、type=1 套餐价格 | Commodity, Package | `showStatus`: 0官网隐藏/1官网显示；`trialVersion`: 0正式/1试用 |
| **Package (套餐 / 版本)** | 用户实际可购的商品组合（"会员版本"），含 `level` 用于升级判定、`packageType`（1主要 / 2次要，用户只能购买主要） | Commodity, CommodityPrice, Increment, PackageUser | `status`: 0未上架/1已上架；`customizeType`: 0系统/1自定义 |
| **PackageUser (自定义版本-用户关联)** | 当 `customizeType=1` 时，记录哪些 `userId / tenantId` 可见该自定义版本 | Package | — |
| **Increment (增量包)** | 在已有套餐基础上追加资源的"加购单元"，绑定 `commodityId / packageId / commodityPriceId` | Commodity, Package, CommodityPrice | `status`: 0未上架/1已上架 |
| **TypeConsumption (类型用量配置)** | 描述"一个商品/套餐/邀请码包含多少某资源类型"的配方表；订单创建时按此展开为用户资产 | CommodityType, Package, InvitationCodeBatch | `type`: 0商品/1套餐/2邀请码 |
| **Order (订单)** | 一次购买行为的主单：含价格、有效期、生效/过期时间、订单类型、商品类型、来源；价格单位**分** | OrderDetail, OrderPay, OrderExtend | `status` 9 态 / `orderType` 8 类 / `source` 4 类（详下） |
| **OrderDetail (订单详情/资源行)** | 订单展开后的"资源行"：每行=一个 `commodity_type`（如"分析时长 1000 分钟"），含开始/重置/过期时间 | Order, CommodityType, TypeSurplus | `status`: 0未开始/1生效中/3已失效/4冻结 |
| **OrderPay (订单支付)** | 订单的支付侧记录：支付状态、支付方式、第三方支付/退款单号、回调内容 | Order | `payStatus` 5 态 / `refundStatus` 4 态（详下） |
| **OrderExtend (订单扩展)** | 订单的运营态扩展：付费截图、备注（用于手工添加订单的凭证留痕） | Order | — |
| **InvitationCodeBatch (邀请码批次)** | 批量生成邀请码的来源凭证：批次绑定商品/价格/有效期，含 `type`（0机构码/1个人码/2激活码）、`isGratis`（免费）、`channelId`（渠道） | InvitationCode, Channel, Commodity | `status`: 0正常/1禁用 |
| **InvitationCode (邀请码)** | 单个可兑换码：使用后生成订单，与 `userId / orderId` 强关联 | InvitationCodeBatch, Order | `useStatus`: 0未使用/1已使用；`status`: 0正常/1禁用 |
| **InvitationUsageRecord (邀请码使用记录)** | 邀请码使用流水（含 `tenantId / orderId / userId`）；与 `tb_invitation_code` 解耦，便于审计 | InvitationCode | — |
| **UserProperty (用户资产 / 资产池)** | 用户的资源容器，一个 userId 可有多份；含 `parentUserId`（子账号场景），`isUse` 表示当前激活 | TypeSurplus, UserPropertyType, UserPropertyDetails | `type`: 0自有/1子用户共享；`isUse`: 0未启用/1启用 |
| **UserPropertyType (用户-资产类型总览)** | 用户在某一 `commodity_type` 下的总数 / 已用数 / 总累计 | UserProperty, CommodityType | — |
| **UserPropertyDetails (资产消费流水)** | 每次加/减资产的明细：含 `signs`（0减/1加）、`orderDetailId`、`typeSurplusId`、`assetCreationType`（0用户/1系统）；用于资产对账 | UserProperty, TypeSurplus | — |
| **TypeSurplus (资产剩余批次)** | 同一资产类型可按订单分批入账（如：本月套餐发的 1000 分钟，购买增量再加 200 分钟），按批次扣减 | UserProperty, OrderDetail | `useStatus` 5 态 / `timeStatus` 4 态（详下） |
| **AiTokenUseRecord (AI Token 消耗)** | 每次 AI 模型调用的 token 维度记账（输入/输出/图像/音频/视频/思维链 tokens、模型名、`finishReason`） | UserPropertyDetails, PropertyDetailsToken | — |
| **PropertyDetailsToken (资产-Token 关联)** | 把 `tb_user_property_details` 与 `tb_ai_token_use_record` 多对多绑定，用于追溯一次 AI 调用扣了哪些资产 | UserPropertyDetails, AiTokenUseRecord | — |
| **CrmOrderChangedEvent (CRM 订单出站事件)** | Spring `ApplicationEvent`：订单关键节点（支付成功/升级/续费/增购）发布给 CRM 子系统消费 | Order | `eventType` 见下 |

---

## 三、状态机与枚举

### 3.1 Order.status — 订单状态（9 态）

| 值 | 名称 | 说明 |
|---|---|---|
| 0 | UNPAID | 未支付 |
| 1 | NOT_STARTED | 未开始（已支付但未到生效时间） |
| 2 | EFFECTIVE | 生效中 |
| 3 | EXPIRED | 已过期 |
| 4 | REFUNDED | 已退款 |
| 5 | END_BY_UPGRADE | 已结束（升级后当前订单失效） |
| 6 | TIMEOUT_CLOSED | 超时未支付关闭 |
| 7 | FROZEN | 冻结 |
| 8 | MANUAL_CANCELED | 手动取消 |

### 3.2 Order.orderType — 订单类型（8 类）

| 值 | 名称 | 说明 |
|---|---|---|
| 0 | FREE | 免费订单 |
| 1 | UPGRADE | 升级订单 |
| 2 | FREE_TO_PAID | 免费版换收费版 |
| 3 | RENEWAL | 续费 |
| 4 | INCREMENT | 增量包订单 |
| 5 | VERSION_ACTIVITY | 版本活动订单 |
| 6 | EDIT | 编辑订单 |
| 7 | COMMODITY_ACTIVITY | 商品活动订单 |

### 3.3 Order.commodityType — 订单商品大类

| 值 | 含义 |
|---|---|
| 0 | 增量包 |
| 1 | 正常版本（月底资源重置） |
| 2 | 活动版本（月底资源不重置） |
| 3 | 邀请活动订单 |

### 3.4 Order.source — 订单来源

| 值 | 含义 |
|---|---|
| 0 | 正常下单 |
| 1 | 手动添加 |
| 2 | 邀请用户成功 |
| 3 | 邀请码赠送 |

### 3.5 OrderPay.payStatus / refundStatus

| payStatus | 含义 | | refundStatus | 含义 |
|---|---|---|---|---|
| 0 | 未支付 | | 0 | 未申请退款 |
| 1 | 已支付 | | 1 | 申请退款中 |
| 2 | 支付失败 | | 2 | 退款成功 |
| 3 | 已退款 | | 3 | 退款失败 |
| 4 | 取消支付 | | | |

`payType`: 0 微信支付 / 1 支付宝支付。

### 3.6 OrderDetail.status — 资源行状态

| 值 | 含义 |
|---|---|
| 0 | 未开始 |
| 1 | 生效中 |
| 3 | 已失效 |
| 4 | 冻结 |

### 3.7 TypeSurplus — 资产批次状态（`OrderEnums.UseStatus` / `OrderEnums.TimeStatus`）

| useStatus | 含义 | | timeStatus | 含义 |
|---|---|---|---|---|
| 0 | 正在使用 | | 0 | 生效中 |
| 1 | 已用完 | | 1 | 已过期 |
| 2 | 弃用（当前订单已升级） | | 2 | 停用 |
| 3 | 时间过期 | | 3 | 冻结 |
| 4 | 冻结 | | | |

### 3.8 OrderEnums.commodityTypeCode — 资源类型 code 枚举（核心）

| code | 含义 |
|---|---|
| `aiAnalysisTime` | AI 语音分析时长 |
| `textTaggingWordCount` | 文本标注字数 |
| `monitorNum` / `child_monitorNum` | 主账号 / 子账号监控位 |
| `anchorNum` / `child_anchorNum` | 主账号 / 子账号可添加主播数 |
| `storageNum` | 空间容量 |
| `subAccountCount` | 子账号数量 |
| `anchorBarrageNum` / `child_anchorBarrageNum` | 主 / 子账号主播弹幕监控位 |
| `barrageNum` | 弹幕显示条数 |
| `showBarrageBubble` | 点击弹幕显示气泡（0否/1是） |
| `aiTokenNum` | AI 分析文本数量 |
| `imgIdentifyNum` | 图片识别数量 |
| `dataBoardNum` | 数据看板 |
| `smsMessageNum` | 短信条数 |
| `textExtractionNum` | 文案提取时长 |
| `rpaAmountNum` / `child_rpaAmountNum` | 主 / 子账号巨量监控位 |
| `shortVideoNum` | 提取文案条数 |
| `searchInfluencerNum` / `child_searchInfluencerNum` | 主 / 子账号搜索达人数量 |
| `subscribeInfluencerNum` | 订阅达人数量（共享，短视频订阅资产） |
| `searchHotVideoNum` / `child_searchHotVideoNum` | 主 / 子账号搜索爆款数量 |
| `subscribeHotVideoNum` | 订阅爆款数量（共享，短视频订阅资产） |
| `userSubscribeInfluencerNum` / `child_userSubscribeInfluencerNum` | 主 / 子账号订阅达人数量（本地，与共享对应） |
| `userSubscribeHotVideoNum` / `child_userSubscribeHotVideoNum` | 主 / 子账号订阅爆款数量（本地） |
| `enterprisePersonNum` | 人员数量 |
| `enterpriseSubsidiariesNum` | 子公司数量 |

> 共享/本地区分由 `OrderEnums.commodityTypeCode#isUserShortVideoProperty(code)` 判定，仅订阅类资产存在共享池。

### 3.9 OrderEnums.clientVersion — 客户端版本

| code | 含义 |
|---|---|
| `record` | 纯录制版 |
| `replay` | 复盘版 |

### 3.10 CrmOrderChangedEvent.eventType — 出站事件类型

| 事件 | 触发判定（`OrderBll#resolveOrderEventType`） |
|---|---|
| `UPGRADE` | `orderType == 1` |
| `RENEWAL` | `orderType == 3` |
| `INCREMENT` | `orderType == 4` 或 `commodityType == 0`（增量包大类） |
| `PAY_SUCCESS` | 其他情况（默认） |

> CRM 文档中描述的 `ORDER_CREATED / EXPIRING / EXPIRED / REFUND / CANCELED` 当前在 `replay-order` 代码侧未观察到对应发布点；现阶段仅 `PAY_SUCCESS / UPGRADE / RENEWAL / INCREMENT` 4 类由 `OrderBll#publishCrmOrderChangedEvent` 在 `successOrder` 路径上发出。`<待补充>`：到期/退款/取消等事件是否要补发布点，需要业务侧确认。

---

## 四、核心工作流

### 4.1 创建订单 → 支付 → 资产入账

```
用户下单 (Controller /replay/order/createOrder | /createOnlineOrder)
  → OrderLogic / OrderBll.createOrder
  → 写 tb_order + tb_order_detail + tb_order_pay
  → 第三方支付（replay-third：微信 / 支付宝）拉起支付码
  → 支付回调 → OrderPayBll 标记 OrderPay.payStatus=1
  → OrderBll.successOrder
      ├── 处理升级：旧订单 status=5 (END_BY_UPGRADE)，资产 typeSurplus.useStatus=2 (DISABLED)
      ├── payOrderHandle: tb_order.status=2 (EFFECTIVE), startDate/endDate 写入
      ├── useOrder: 展开 OrderDetail → 写 tb_type_surplus 批次 → 重算 tb_user_property_type
      └── publishCrmOrderChangedEvent(eventType)
```

### 4.2 邀请码兑换流程

```
用户输入邀请码 → InvitationCodeLogic.checkAndUse
  → tb_invitation_code.useStatus=1, userId/orderId 落库
  → tb_invitation_usage_record 写流水
  → OrderBll.invitationCodeCreateOrder 创建 source=3 订单
  → 走 4.1 入账主流程
```

### 4.3 定时器：订单过期 / 资源重置

> 详细流程与业务规则见 **[order_expiration.md](order_expiration.md)**（两阶段过期：订单生命周期终结 + 周期性资产重置、支付超时、手动关单/取消）。

```
@XxlJob("batchSaveAudioLog") (OrderScheduledTasks#expiredOrder)
  Phase 1 — 订单生命周期过期:
    → orderBll.loadExpiredOrder(expiredTime, callback) 每批 50 条，手动事务
    → 版本套餐过期 → 清空弹幕监控配额 + 关闭主播监控位
    → batchStopOrder → order.status→5, detail.status→3, surplus.useStatus/timeStatus→2
    → 有 status=1 待生效订单 → 自动激活；无 → 创建免费版兜底
  Phase 2 — 资产周期重置 (Phase 1 完成后):
    → loadExpiredAllOrderDetail → 过期当前周期资产 → 写入流水
    → 计算 nextReset → 创建新周期 surplus → 异步短信通知
```

### 4.4 客户端版本切换（纯录制 ↔ 复盘）

```
ClientOrderController#setClientVersionConfig
  → 校验账号类型必须 CLIENT_USER (UserEnums.userType)
  → orderBll.setClientVersionConfig 写 Redis user_client_version_config
  → 后续 userClientVersionConfig 读取
```

### 4.5 CRM 出站事件

```
OrderBll.successOrder → publishCrmOrderChangedEvent
  → 构造 CrmOrderChangedBo(eventType, occurredAt, userId, orderId)
  → CrmOrderEventServiceImpl.publishOrderChangedEvent
  → ApplicationEventPublisher.publishEvent(CrmOrderChangedEvent)
  → CrmOrderChangedEventListener (位于 replay-api) HTTP POST 至 SalesCoach Agent
```

---

## 五、业务规则与不变量

1. **价格单位强制为分**：`originalPrice / realPrice / payMoney / totalPrice / discountRate` 全部以**分**存储；前端展示需 /100。
2. **租户隔离**：`tb_package_user / tb_invitation_usage_record / tb_ai_token_use_record` 持 `tenantId`；其余表通过 `userId → tb_user.tenant_id` 间接归属。**列表查询必须带租户过滤**（详见 CLAUDE.md §5）。
3. **订单状态转换是强单向的**：UNPAID → EFFECTIVE / TIMEOUT_CLOSED / MANUAL_CANCELED；EFFECTIVE → EXPIRED / END_BY_UPGRADE / REFUNDED / FROZEN。**禁止逆向**（如 EXPIRED → EFFECTIVE）。
4. **资产批次模型（双层）**：`tb_user_property_type` 是"总账"（缓存友好），`tb_type_surplus` 是"批次明细"（按 orderDetail 分批），扣减必须先扣 `typeSurplus` 再回写 `userPropertyType` 总数；Redis 缓存键由 `RedisCacheKey.userPropertyTypeCacheKey / userPropertyTypeTotalCacheKey` 维护。
5. **共享订阅资产语义**：`subscribeInfluencerNum / subscribeHotVideoNum` 在用户共享池中（一般是父账号或租户级），与本地 `userSubscribeInfluencerNum / userSubscribeHotVideoNum` 双写——使用时优先扣本地，溢出再扣共享（详见 `OrderUserPropertyController#checkUseProperty`）。
6. **升级订单（orderType=1）副作用**：升级后旧订单 `status=5`、`afterUpgrading=newOrderId`；新订单 `beforeUpgrading=oldOrderId`；旧订单的 `typeSurplus.useStatus=2 (DISABLED)`。
7. **邀请码批次的 4 个互斥维度**：`type`（机构/个人/激活）× `isInfinite`（无限/有限）× `isGratis`（免费/收费）× `resourceType`（后台/外部接入）。
8. **CRM 出站事件只在 `successOrder` 路径发布**：未支付订单的关闭/取消、定时器到期、退款回调当前路径**不会发布事件**——若 CRM 需要这些信号，需补 publish 点。
9. **`tb_invitation_usage_record` 使用了 `@TableLogic`**：这是项目内**唯一**使用 `@TableLogic` 的实体（违反 CLAUDE.md §5 "禁 @TableLogic"），属历史遗留，新代码不应模仿。详见 `## 六` 已知技术债。

---

## 六、已知技术债 / 注意点

- `InvitationUsageRecordEntity` 使用 `@TableLogic`（与项目"手动 isDeleted"约定冲突），新功能不要复用其样式。
- `RedisTimeoutOrderListener.java` 整文件已注释掉——原本用 Redis Key 过期事件触发订单超时关闭，现已下线，订单超时改由定时器扫描（详见 `## 4.3`）。
- `Order.payDate / startDate / endDate / realEndDate` 是 `java.util.Date`，其它新表是 `LocalDateTime`——混用是历史遗留，读写时注意时区/类型。
- `OrderEntity.realEndDate vs endDate`：`endDate` 是名义到期时间，`realEndDate` 是真实到期时间（用于升级订单回算，可能早于 endDate）。
- `tb_increment` 没有 `is_deleted / update_date` 字段——增量包配置历史上视为不可变；新业务若需删除，应走"下架"（`status=0`）。
- `CrmOrderChangedEvent` 只覆盖 4 种 eventType；CRM 文档列出的 9 种 eventType 中 `ORDER_CREATED / EXPIRING / EXPIRED / REFUND / CANCELED` 当前无发布点 `<待补充>`。

---

## 七、跨域引用

- 客户域：[[crm_domain]] — 客户主体（tb_user）通过 `userId` 关联订单；CRM 通过 `CrmOrderChangedEvent` 消费订单生命周期信号。
- 主播 / 视频域：[[words_domain]] — 订单生效后释放主播监控位 / 录制资源（`anchorNum / monitorNum / anchorBarrageNum`）；订单过期触发 `AnchorUrlBll.closeAnchorBarrageNum` 关闭弹幕监控。
- 用户体系：`replay-power` — `userId / tenantId / userType (UserEnums.userType.CLIENT_USER)` 来源；子账号体系直接影响资产 `parentUserId` 字段。
- 第三方支付：`replay-third` — 微信支付 / 支付宝 / 退款回调由该模块处理，回写 `tb_order_pay.thirdOrderNum / thirdCallbackContent`。
