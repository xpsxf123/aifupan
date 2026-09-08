# CRM Domain — 业务概念与词汇表

> CRM 子系统核心业务概念定义。Agent 在进行 Explorer/Propose 阶段时必须使用此术语表，避免领域漂移。

---

## 核心概念

| 概念 | 定义 | 关联概念 | 状态机 |
|------|------|----------|--------|
| **Customer (客户)** | 被销售跟进的对象，以手机号为主体标识，拥有账号状态和租户归属 | UserDetails, Order, Sales | `status`: 0正常/1冻结 |
| **Sales (销售)** | 跟进动作的执行者，可被分配客户归属，支持轮询开关 | Customer, UserDetails | — |
| **Follow-up Record (跟进记录/明细)** | 销售每次跟进客户的结果留痕，含跟进类型、状态、内容、下次跟进时间 | Customer, Sales, BusinessSnapshot | — |
| **Business Snapshot (跟进快照)** | 以最新跟进记录为准自动同步的客户当前状态冗余，一个客户一条 | Follow-up Record, Customer | `accordingStatus` |
| **Next Follow-up Time (下次跟进时间)** | 下一次需要触达/跟进该客户的计划时间，用于待跟进列表筛选 | Follow-up Record, Business Snapshot | — |
| **UserDetails (客户详情)** | 客户扩展信息：行业、渠道、公司、意向度、线索类型、微信名等 | Customer, Sales | — |
| **Sales Statistics (销售看板)** | 按销售/团队维度的统计汇总：待跟进、到期、成交、意向分布 | Sales, Customer, Order | — |
| **AI Profile (AI 画像)** | 智能体为 CRM 客户生成的完整 AI 画像 JSON，独立存储于扩展表 | Customer | — |
| **Stage Event (阶段事件)** | 智能体在关键节点回传的阶段判断、关键事实、证据与摘要 | Customer, AI Profile | — |
| **Order Event (订单事件)** | CRM 向智能体推送的订单生命周期变化通知 | Customer, Order | 9 种 event_type |
| **SalesCoach Agent (销售智能体)** | 企微侧边栏中的 AI 销售助手，自动抽取客户画像和话术建议 | Customer, CRM | — |
| **CRM Integration (CRM 集成)** | 爱复盘 CRM 与销售智能体的双向数据通道 | SalesCoach Agent, Order Event | — |

---

## 跟进类型与状态（字典项）

### 跟进类型 (remarkType)
- 首次联系 / 日常跟进 / 演示跟进 / 成交跟进 / 续费跟进 / 唤醒跟进

### 跟进状态 (accordingStatus)
- 待跟进 / 跟进中 / 已演示 / 已成交 / 已流失 / 暂不需要

### 客户意向度 (userAmbition)
- S 待收款 / A 有付款意向 / B 感兴趣 / C 暂不需要

### 线索类型 (leadType)
- 个人 / 工作室 / 企业 / 品牌旗舰及定制

---

## 订单事件类型 (event_type)

| 事件类型 | 触发时机 | 说明 |
|----------|----------|------|
| `ORDER_CREATED` | 订单创建 | 含试用/正式订单 |
| `PAY_SUCCESS` | 支付成功 | 客户已付款 |
| `RENEWAL` | 续费 | 到期后续费 |
| `UPGRADE` | 升级 | 套餐升级 |
| `INCREMENT` | 增购 | 新增购买 |
| `EXPIRING` | 即将到期 | 到期前 N 天提醒 |
| `EXPIRED` | 已到期 | 到期未续费 |
| `REFUND` | 退款 | 已退款 |
| `CANCELED` | 取消 | 订单取消 |

---

## 核心工作流

### 客户入库 → 跟进流程
```
客户注册/入库 → 完善客户详情 → 分配归属销售 → 
首次跟进 → 多次跟进推进 → 成交/继续跟进/流失 → 持续维护
```

### 跟进记录写流程
```
销售填写跟进信息 → 提交保存 → 写入跟进明细(tb_user_remark) → 
刷新快照(tb_user_business) → 列表/看板立即可见最新状态
```

### 订单事件出站流
```
订单状态变更 → 发布 CrmOrderChangedEvent → 
CrmOrderChangedEventListener → HTTP POST → SalesCoach Agent
```

---

## 角色与权限

| 角色 | 权限范围 |
|------|----------|
| 销售人员 | 查看/跟进归属自身客户，维护跟进记录 |
| 销售管理员 | 查看团队/全量客户与统计，销售配置管理 |
| 系统管理员 | 维护字典项、系统配置 |
| SalesCoach Agent | 通过 API Key 调用集成接口，读写客户画像和阶段事件 |
