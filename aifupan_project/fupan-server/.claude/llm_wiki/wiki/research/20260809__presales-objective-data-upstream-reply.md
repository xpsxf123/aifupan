# 售前试用租户客观数据 — 上游（爱复盘）现状盘点与回复稿

- 日期：2026-08-09
- 输入：`售前试用租户客观数据-上游接口沟通稿-2026-08-09.md`（Business Data Hub 团队）
- 我方角色：上游 —— 爱复盘产品后台（back-fupan-server）+ AI 智能体（ifupan-anchor-agent）
- 结论性质：基于代码盘点（未联调验证）；负责人 / 排期留待人工填写

---

## Part A — 总体结论（内部视角）

1. **api-key 开放接口体系已存在，直接复用**：`@APIKey` 注解 + `APIKeyInterceptor`，Header `x-jiuyu-client-id` + `api-key`，密钥按 app-id 配置在 `jiuyu.oauth.client.api-key-secret`。现有消费方 `salescoach-agent`（`/internal/crm/**`）、`anchor-agent`（`/internal/ai-agent/**`）。**建议为 Business Data Hub 新发一个独立 app-id**，新接口统一挂 `/internal/bdh/**` 或并入 `/internal/crm/**`。
2. 对方文档判断为"缺失"的 4 项里，**3 项其实数据都在库里，缺的只是接口**：
   - 成员名单：`tb_user.active_tenant_id` + `userType/parentId`，治理端已有 `getAllUserList(tenantId)` 现成实现；
   - 登录状态：`tb_user_login_log`（每次登录一行）+ `tb_user_details.is_logged_in`，可聚合出首次/最近/次数；
   - 巨量授权：`tb_anchor_url_user.auth_jlby_status(+time)` / `auth_qc_status` / `auth_channel_status` / `auth_life_status`；
   - 账号分析：`tb_anchor_video(tenant_id, sec_uid, analysis_status, analysis_time)` 可聚合出按业务账号的分析状态/次数/时间。
3. **真正的硬缺口只有 2 个**（已确认：**直播/短视频智能体无需区分**，open-stats 现状混合总量口径直接可用，原 agent_type 改造项取消）：
   - CRM 客户 → 产品租户映射接口（数据可推导：手机号→`tb_user`→`active_tenant_id`，一租户一主账号已确认成立，做接口即可）；
   - 订单退款/取消：`tb_order_pay` 退款字段是**纯占位，全仓无任何写入代码**，退款实际走线下手工；订单取消无时间字段；CRM 事件推送只覆盖支付成功（eventType 硬编码 `PAY_SUCCESS`），无退款/取消/过期事件，无重试无 outbox。**"当前有效成交"的完整口径本期只能部分满足**。

   会话数为轻量补充项：`chat_session` 数据齐备（含预建索引），open-stats 补一条 SQL 即可返回 `conversation_count`。

### 内部风险（不进对外回复稿，需单独处理）

| # | 风险 | 位置 |
|---|---|---|
| 1 | `SignatureInterceptor` 从未在 `InterceptorConfig` 注册，`@FeatureSignature` 全部失效 → `/replay/openapi/governance/**`（改手机号/改密码/发短信/绑解绑子账号/**user-list 全量成员**）当前**应用层无鉴权** | `replay-api/.../interceptor/InterceptorConfig.java:48-51` |
| 2 | 各环境 `api-key-secret` / `signature.apps` / `crm.agent.api-key` 明文提交在 git（含 prod） | `application-{dev,test,yz,prod}.yml` |
| 3 | `APIKeyInterceptor` 鉴权失败日志打印客户端传来的 secret 明文 | `APIKeyInterceptor.java:124` |
| 4 | `UserPropertyDetailsDao` 求和 SQL 未使用 `sinceCreateDate` 参数 → behavior-signals 的 `totalAiTokensUsed / recentAiTokensUsed30d / recentAiTokensUsed7d` 三值恒等（已在给对方的口径说明中如实标注） | `UserPropertyDetailsDao.xml:5-10`、`CrmIntegrationServiceImpl.java:609-613` |
| 5 | 若复用 governance user-list 给 BDH，必须改挂 `@APIKey`，不能沿用名义签名 | GovernanceOpenAccountController |

---

## Part B — 对外回复稿（可直接发 Business Data Hub）

> 以下按贵方沟通稿第八节格式逐项回复。通用说明：
> - **鉴权**：统一使用现有 api-key 机制 —— Header `x-jiuyu-client-id: <app-id>` + `api-key: <secret>`，将为 Business Data Hub 单独分配 app-id 与密钥（服务端间调用，不下发前端）。
> - **成功语义**：HTTP 恒为 200，以响应体 `code` 判断（`200` 成功；鉴权失败返回 `code=403`）。
> - **稳定 ID**：租户/用户/订单 ID 均为雪花 Long，新接口一律按字符串序列化返回（存量接口逐个核对后确认）。
> - **无数据语义**：新接口按贵方要求区分「不存在 / 未发生 / 查询失败」，不会把未映射身份返回为 0/false。
> - 下表「预计时间」为工作量档位（小 ≤2 人日 / 中 3–5 人日 / 大 ≥1 周），具体排期与负责人另行确认。

### 回复总表

| 数据能力 | 当前是否已有 | 接口或文档地址 | 已有字段 | 当前环境状态 | 缺失字段能否补充 | 预计时间 | 负责人 | 备注 |
|---|---|---|---|---|---|---|---|---|
| CRM 客户 → 产品租户映射 | 部分已有（可推导，无正式接口） | 现有 `POST /internal/crm/customer/batch-aggregate-query`（按手机号返回 userId） | phone→userId、客户名、销售、订单 | 生产可用 | **能**：新增映射接口返回 `product_tenant_id`、`relation_status` | 小 | 待定 | CRM userId 即产品 user_id；详见 B.1 |
| 产品租户成员名单 | 已有数据 + 内部实现，无对外接口 | 内部：治理端 user-list（将以 api-key 形式重新暴露） | userId/tenantId/parentId/userType(主/子)/昵称/手机号/注册时间/冻结状态/是否登录过 | 数据生产真实 | **能**：补 joined_at（绑定时间）、left_at（解绑时间）、批量 tenant_id 查询 | 小–中 | 待定 | 详见 B.2 |
| 成员登录状态 | 已有数据（登录日志表），无接口 | — | 每次成功登录一行（时间/IP/用户），另有"是否登录过"标记 | 数据生产真实 | **能**：聚合出 has_logged_in / first / last / count | 中 | 待定 | 登录失败当前不记录；详见 B.3 |
| 抖音/视频号业务账号状态 | 已有数据，统计接口需新增 | 现有 behavior-signals 有团队级总数 | 平台(抖音/快手/视频号)/添加时间/添加人/移除状态 | 数据生产真实 | **能**：按租户×平台计数 + 明细 | 小–中 | 待定 | 详见 B.4 |
| 巨量授权状态 | **已有数据**（贵稿判断"缺失"，实际存在），无接口 | — | 巨量百应/千川/视频号/抖音来客四类授权状态 + 状态变更时间，粒度=租户×成员×业务账号 | 数据生产真实 | 部分：**无授权到期时间、无历史流水**（当前模型不支持，如需要属新建设） | 小（现状暴露）/ 大（补 expires_at+流水） | 待定 | 详见 B.5 |
| 账号分析状态 | 已有数据，无按账号聚合接口 | — | 分析状态(未/中/完成/错误)/分析时间/发起人/租户/业务账号 | 数据生产真实 | **能**：按租户×业务账号聚合次数与首末时间 | 中 | 待定 | "账号分析"与"累计转写时长"确认为两个口径；详见 B.6 |
| 智能体成员/租户用量（直播+短视频合并口径，经双方确认无需区分） | 已有（open-stats） | `open-stats-api.md`（5 端点） | used / userMessageCount / billedTokens，租户级+用户级，时间窗 | 已发布 | 无需改造（合并口径下现状即满足） | — | 待定 | 口径说明见 B.7 |
| 智能体会话数 | 数据已有，接口缺字段 | 同上 | 会话表含租户/时间/消息数，索引已预建 | 数据生产真实 | **能**：低成本补 conversation_count | 小 | 待定 | 会话定义见 B.7 |
| 正式成交及订单生命周期 | 部分已有 | `GET /internal/crm/order/query-by-phone` + 订单事件推送（PAY_SUCCESS） | 订单id/类型/试用标记/状态/支付时间/实付金额/服务起止 | 生产可用 | **部分**：支付成功事实完整；**退款/取消当前无线上化能力**，需按二期建设 | 中（口径补齐）/ 大（退款取消事件） | 待定 | 详见 B.8，含贵方全部枚举问题的逐条回答 |

### B.1 CRM 客户与产品租户映射

- **CRM `userId` 就是产品 `user_id`**：`batch-aggregate-query` 返回的 userId 即爱复盘 `tb_user.id`，无需再做 ID 映射。
- 映射规则：产品侧一个用户有唯一「当前激活租户」（`active_tenant_id`）；**一个租户固定一个主账号**（租户即由主账号创建，`tb_tenant.user_id` = 主账号）。因此：
  - 一个 CRM 客户（手机号→user）当前只对应一个产品租户；
  - 一个产品租户可对应多个成员（主账号 + 子账号），若贵方 CRM 联系人分别登记了子账号手机号，会映射到同一租户 —— 属正常情况，建议贵方以租户为聚合键；
  - 成员换租户（子账号被绑定/解绑）时 `active_tenant_id` 会变化，映射接口返回的是**当前关系**；历史关系可从绑定/解绑时间（`binding_date`/`unbind_date`）推导，首期先提供当前关系 + `source_updated_at`；
  - 注意边界：**每个用户注册时都会自动生成一个自己的租户**，子账号被绑定后激活租户切换为主账号租户，其个人空租户仍存在但不活跃。映射接口只返回当前激活租户，不会把空租户误报给贵方。
- 交付形式：新增 `POST /internal/crm/customer/tenant-mapping`（批量 `crm_customer_id` 或 phone 入参），返回 `crm_customer_id / product_tenant_id(字符串) / relation_status / source_updated_at`；`valid_from/valid_to` 首期不提供（无历史关系表），如实返回说明。
- 手机号问题：认可贵方观点，映射建立后请贵方持久化 `product_tenant_id`，长期不再依赖手机号查询。换手机号后 user_id / tenant_id 不变。

### B.2 产品租户成员名单

- 数据全部存在，内部已有按 `tenantId` 查全量成员的实现（治理后台在用），字段覆盖贵方最低要求的大部分：
  - `product_user_id`（tb_user.id）、`account_role`（userType：0 主账号 / 2 子账号）、`display_name`（昵称，可按需脱敏手机号）、`account_status`（正常/冻结/已删除，`status` + `is_deleted`）、`created_at`（注册时间）；
  - `joined_at` = 绑定记录 `binding_date`；`left_at` = 解绑记录 `unbind_date`（解绑成员通过绑定关系表可识别，不会静默丢失）；
- 需新开发的部分：批量 `product_tenant_id` 入参、joined_at/left_at 拼装、api-key 鉴权暴露。
- **返回的 `product_user_id` 可直接用于 open-stats 用户级查询**（两侧同一套 user_id / tenant_id），满足贵方验收口径第 4 条。

### B.3 成员登录状态

- 数据来源：登录日志表（每次**成功**登录一行，含时间与 IP）+「是否登录过」冗余标记。可提供：
  - `has_logged_in`：已有现成标记；
  - `first_login_at` / `last_login_at` / `login_count`：由日志表聚合（MIN/MAX/COUNT）；
- 口径回答：`login_count` 可同时支持累计与传入时间窗（左闭右开，与 open-stats 对齐）；**登录失败当前不落日志，无法提供失败次数**；日志表存量数据自上线起完整。
- 工作量：新增聚合接口 + 日志表按 (user_id, opera_type, create_date) 补索引，中等。

### B.4 业务账号（抖音/视频号等）状态

- 数据模型：业务账号 = 「租户添加的直播间账号」，平台枚举 0 抖音 / 1 快手 / 2 视频号（比贵稿预期多一个快手）。每条记录含：租户、**添加人 user_id（即贵稿的 `added_by_user_id`，仅表示操作者）**、添加时间、移除状态与移除时间。
- 可提供：
  - 租户汇总：`platform × total_count`（在册口径 = 未从列表移除）+ `observed_at`（实时查询时间）；`active_count` 建议等同在册数，我方无另一层"有效"概念，如实说明；
  - 明细：`business_account_id`（平台侧稳定 ID sec_uid，字符串）、账号昵称、平台、添加时间、当前状态、added_by_user_id。
- 现状差异说明：behavior-signals 里的 `addedAnchorCount/ownAnchorCount` 是**用户维度**总数且不分平台；本期将按贵方要求改为**租户 × 平台**口径新出接口。

### B.5 巨量授权状态

- **纠正贵稿排查结论：授权状态数据是存在的**，挂在「租户 × 成员 × 业务账号」维度上，共四类：
  - 巨量百应：状态（0 未授权 / 1 已授权 / 2 授权过期 / 3 授权失败 / 4 授权中 / 5 授权抖音号不匹配）+ 状态变更时间；
  - 千川：同上编码 + 变更时间；
  - 微信视频号：0 未授权 / 1 已授权 / 2 微信后台取消 / 3 用户取消（无变更时间字段）；
  - 抖音来客：同巨量编码 + 变更时间。
- 与贵方字段的对齐与限制：
  - `authorization_status` ✅（枚举将正式化提供）；`business_account_id` ✅（授权粒度就是业务账号级）；`updated_at` ✅（=状态变更时间）；`operator_user_id` ✅（记录归属成员）；
  - `authorized_at`：**当前只有"状态变更时间"**，状态=已授权时它近似等于最近授权时间，但历史上的首次授权时间不可回溯；
  - `expires_at`：**当前模型没有到期时间**，过期只能由状态=2 表达。如贵方强需要到期时间与授权流水，属于新建设（需新增授权流水表），请在回复中确认优先级。
- 未授权/未知区分：账号未做过授权返回状态 0，不会返回"不适用"。

### B.6 账号分析状态

- 口径确认（回答贵稿问题）：**"账号分析"与"直播录制/视频分析时长"是两个不同口径**——
  - 贵方要的「分析行为状态」对应我方**每场直播录制视频的智能分析记录**：租户 × 业务账号 × 场次，含分析状态（0 未分析 / 1 分析中 / 2 完成 / 3 错误）、分析时间、发起人；
  - behavior-signals 里的 `aiAnalysisTimeConsumed` 是**累计音视频转写时长（秒）**，是资源消耗口径，确实不能当作分析状态用，贵稿判断正确；
  - 另有短视频侧的「短视频/达人分析」（独立链路，状态 0–4），如贵方需要短视频分析状态请单独确认，本期默认只提供直播侧。
- 可提供（按租户 × 业务账号聚合）：`analysis_status`（是否发生过成功分析）、`analysis_count`（累计或时间窗成功分析次数）、`first_analyzed_at` / `last_analyzed_at`、`product_user_id`（明细级可给发起人）。
- 工作量：中（新聚合 SQL + 接口；分析时间字段需做类型清洗）。

### B.7 智能体用量与会话数（逐条回答贵稿 5.7 的确认问题）

> 前提更新：经双方确认，**第一期不区分直播智能体与短视频智能体**，统一按"智能体用量"合并口径提供。原 `agent_type` 需求取消。

1. **当前 open-stats 的数据是否只来自直播智能体？** —— 不是。直播智能体与短视频（达人）智能体是**同一个服务、同一套会话/消息/计费表**，open-stats 返回的是两类场景的**合并总量**。在"无需区分"的前提下，该口径即为最终口径，现有接口直接可用。
2. **短视频智能体是否已有独立统计接口？** —— 没有独立服务、没有独立接口；其用量已包含在上述合并总量中，不会漏统计。
3. **`agent_type`** —— 本期不提供（需求已取消）。备忘：底层表当前无场景字段，若未来恢复该需求属于新建设，且存量数据无法回溯拆分——届时请尽早提出。
4. **会话数的正式定义** —— 一次对话线程（用户新开一个聊天窗口）为一个会话；会话数 = 时间窗内**创建**的会话数（另可提供"窗口内活跃会话数"口径，请贵方二选一或都要）。数据与索引已具备，`conversation_count` 可低成本加入现有端点返回结构，工作量：小。
5. **Token 计费口径** —— open-stats 的 `billedTokens` 与爱复盘算力扣费**同一口径**：`(总token − 缓存token + 缓存token × 缓存折扣) × 1.2 × 模型倍率`，系数由爱复盘统一下发。即它是"计费 token"不是"原始 token"，单位是 Token 不是金额，与贵稿理解一致。
6. 附加口径说明（贵稿未问但必须声明）：open-stats **不包含爱复盘 App 内旧版 AI 助手**（运营助手/弹幕助手等）的用量，那是另一套数据；behavior-signals 里的 `totalAiTokensUsed` 系列取的正是旧版 App 助手的台账，两者不可相加也不可互替。另：智能体会话表的租户字段在早期版本为空，已做回刷，个别无法归属租户的早期数据不计入任何租户。

### B.8 正式成交订单事实（逐条回答贵稿 5.8 的确认问题）

先答枚举与字段问题：

1. **"明确支付成功"的权威依据**：支付记录 `pay_status=1` + 订单 `pay_date`（支付成功业务时间）。事件侧对应 `PAY_SUCCESS`。注意：后台手工开通的订单（source=1）不产生支付流水与 pay_date，属"非支付成交"，需要贵方确认这类订单算不算成交（建议算，判定字段我方可另给 source）。
2. **拉取与事件是否同一套枚举**：**当前不是**——拉取接口返回内部 int 状态，事件推送返回映射后的字符串枚举（`ACTIVE/EXPIRED/REFUNDED/CANCELED/PAID` 等）。本期整改：以事件侧字符串枚举为准，拉取接口对齐输出同一套枚举并提供完整对照表。
3. **`totalPrice / priceCent / realPrice` 哪个是实付金额**：**实付金额 = `totalPrice`（单位分）**，事件里的 `priceCent` 就是它。`realPrice` 是商品挂牌价（未扣升级抵扣），`originalPrice` 是原价，均不能当实付用。币种：无字段，恒为人民币分，接口文档中明示。
4. **试用订单**：`isTrial` = 订单级 `trial_order` 标记，试用订单不会被计为正式成交（贵方口径 `is_trial=false` 过滤即可）。
5. **首购与续费/升级/增购区分**：事件含 `orderType` 枚举（`FREE / UPGRADE / RENEWAL / INCREMENT / ACTIVITY`）；"首次付费成交"建议以贵方侧"该客户第一笔 is_trial=false 且支付成功"判定，我方枚举辅助分类。
6. **幂等**：事件带 `eventId = order_evt_{orderId}_{时间戳}`，订单 ID 唯一且字符串化，请按 `order_id` 去重累计。
7. **退款、取消（本期最大缺口，如实说明）**：
   - 退款：**线上退款流程当前未实现**，实际退款走线下人工处理，因此系统内退款状态数据不可靠，也**没有退款时间、没有部分/全额退款区分、没有退款事件推送**。
   - 取消：有取消状态，但**没有独立取消时间字段**，且当前无取消事件推送。
   - 事件推送现状：仅支付成功链路触发，且为进程内直发、无重试补偿——历史遗漏需以拉取接口对账兜底。
   - **因此本期我方能可靠提供的是"曾发生正式支付"完整口径；"当前有效成交"中的退款/取消扣减，需要二期建设**（补退款线上化或至少退款/取消事实登记 + 事件推送 + 补查接口）。建议贵方一期先按"曾正式支付 + 订单当前状态（拉取对账）"落地，我方把退款/取消事实建设列入排期后再切换到完整口径。这与贵稿"不能删除原支付事实、分别保留两个状态"的原则一致，我方支付事实不会被退款覆盖。
8. **订单归属租户（口径更新，2026-08 起）**：`tb_order` 与资产消耗明细表已增加 `tenant_id` 列——订单事实**直接携带 `product_tenant_id`**（业务发生时点的租户快照），不再需要经"用户→当前激活租户"推导归属。两点口径声明：
   - 新数据为**时点快照口径**：订单/消耗永久归属发生时所在租户，成员后续换租户不影响历史归属（优于按当前关系推导）；
   - **存量数据的 `tenant_id` 为回填值**，回填只能按用户当前租户关系执行，换过租户的用户其历史数据会归入现租户；回填不到的（用户已注销等）保持空值、不计入任何租户，不会伪装成 0。切换时间点将在接口文档中明示。

### 对贵稿第六节 Contract 共同口径的回复

| # | 贵方要求 | 我方回复 |
|---|---|---|
| 1 | 雪花 ID 字符串 | 新接口一律字符串序列化；订单事件已如此；存量接口逐个核对 |
| 2 | 成员行为归属租户 | 均带 tenant_id；成员当前/历史租户关系见 B.1 |
| 3 | 统计时间 | 统一"左闭右开可传时间窗"，不传按近一年，响应回显实际窗口（与 open-stats 现行为一致）；behavior-signals 的内置 7/30 天窗口保持现状 |
| 4 | 数据新鲜度 | 快照类返回 `observed_at`（实时查询即查询时刻）；状态类返回 `source_updated_at` |
| 5 | 无数据语义 | 新接口区分：身份不存在（明确 missingReason）/ 未发生（0/false）/ 参数或系统错误（非 200 code）；不混用 |
| 6 | 成功语义 | HTTP 恒 200，以 body `code` 判断；鉴权失败 `code=403` |
| 7 | 批量能力 | 现行：behavior-signals ≤50 手机号、open-stats ≤200 id；新接口默认 ≤200，超限报错不静默截断；限流与重试建议随接口文档给出 |
| 8 | 数据修订 | 智能体租户回刷类修订会书面通知；常规无追溯修订 |
| 9 | 安全边界 | api-key 仅服务端间使用；将为贵方独立发 app-id，可独立轮换与吊销 |
| 10 | 手机号 | 认可；映射建立后请贵方改用 tenant_id/user_id 为主键，手机号仅入口查询 |
| 11 | 订单幂等 | eventId + order_id 去重口径已提供（见 B.8.6） |

---

## Part C — 内部待开发清单（按优先级）

| P | 事项 | 模块 | 档位 |
|---|---|---|---|
| P0 | CRM客户→租户映射接口（B.1） | replay-api /internal/crm | 小 |
| P0 | 租户成员名单接口：批量 tenantId + joined_at/left_at + api-key 暴露（B.2）；**不得复用无鉴权的 governance 通道** | replay-api + replay-power | 小–中 |
| P1 | 成员登录状态聚合接口 + 登录日志索引（B.3） | replay-power | 中 |
| P1 | 业务账号租户×平台统计 + 明细接口（B.4） | replay-words | 小–中 |
| P1 | 授权状态查询接口（现状暴露，B.5） | replay-words | 小 |
| P1 | 账号分析状态聚合接口（B.6） | replay-words | 中 |
| P1 | open-stats 补 conversation_count（B.7.4） | ifupan-anchor-agent | 小 |
| P2 | 订单枚举正式化 + 拉取/事件口径对齐（B.8.2） | replay-order + replay-api | 中 |
| P2 | 退款/取消事实登记 + 事件推送 + 补查对账（B.8.7） | replay-order | 大 |
| P0 前置 | `tb_order` / `tb_user_property_details` 已加 `tenant_id`（DB 侧），但 `OrderEntity` / `UserPropertyDetailsEntity` 及查询代码尚未同步该字段；新接口开发时补实体字段 + 写入点赋值 + 按租户聚合 SQL，并确认存量回填规则与切换时间点 | replay-order | 小 |
| 并行 | 内部安全整改：governance 无鉴权、密钥入库 git、secret 入日志（Part A 风险表） | replay-api | 中 |

### 二次确认清单（2026-08-09 对方二轮回复）产生的增量行动项

对方二次清单 → 我方二次回复：`docs/售前试用租户客观数据-上游二次回复-爱复盘-2026-08-09.md`。核实结论与新增事项：

| # | 事项 | 结论/动作 |
|---|---|---|
| 1 | 成功码 | **一轮回复写 `code=200` 是笔误**，实际框架 `R.isSuccess()` 判 `code==0`；二次回复已更正为统一 `code=0` |
| 2 | open-stats 端点数 | 现行文档就是 5 个（对方持旧版）；第 5 个 `POST /open/stats/user-tenant/usage` 入参形状不同（`pairs`） |
| 3 | **密钥泄露确认** | `ifupan-anchor-agent/docs/api/open-stats-api.md` §2.1 含 `fupan-server` / `salescoach-agent` 两 appId 的 dev+prod 共四把**现行有效**明文密钥 → 需轮换 + 从文档移除（已在二次回复中承诺） |
| 4 | eventId 不稳定 | `CrmOrderChangedEventListener.java:73-76`：`order_evt_{orderId}_{发布时刻毫秒}`，补发即变 → 整改为按业务时间（paid_at）构造；同时补 `source_updated_at` + 单调序号 |
| 5 | 订单 source | `OrderEntity.source`（:129）：0 正常下单 / 1 手动添加 / 2 邀请成功 / 3 邀请码赠送；`source≠0` 无支付流水，不计"曾正式支付" |
| 6 | 新增开发项：订单对账拉取接口 | 按 tenant_id + updated_at 增量 + 游标分页（现有 query-by-phone 不满足对账）；档位：中 |
| 7 | 新增开发项：`tenant_attribution` 质量标记 | `SNAPSHOT / BACKFILLED / UNKNOWN` 三值，随订单事实返回 |
| 8 | 会话数定稿 | 两字段：`created_conversation_count`（窗口内新建）+ `active_conversation_count`（窗口内有消息，按消息去重会话）；档位：小 |
| 9 | 待办（书面补充） | 登录日志最早可信日期（查生产库）；退款/取消线上化立项；租户快照切换时间点；密钥轮换完成通知；各项负责人+日期 |

### 对方终审确认（2026-08-09 三轮）与交付跟踪

对方已接受全部业务口径，转入合成数据内部开发；**在下列 6 项交付前不连接真实系统**：

| # | 交付项 | 状态 | 下一步 |
|---|---|---|---|
| 1 | 排期会后的负责人 + 完成日期 | ⏳ 待排期会 | 人工：开排期会，按 Part C 清单分配 |
| 2 | 各接口正式文档 + 测试环境 | ⏳ 随开发交付 | 依赖 #1 排期 |
| 3 | 含第 5 端点的最新版 open-stats 文档 | ✅ **已发送并获确认**；后补 §3.5 统计范围声明（合并口径/不含旧助手/早期数据/计费token） | 随 v1.2 定稿 |
| 4 | open-stats 区分未知 ID 与真实 0/false 的改造结论 | ✅ **已定稿：确认改造**，items[] 加可空 `missingReason`（null=真实值 / NOT_FOUND / NOT_MAPPED）；存在性校验复用 fupan-server `/internal/ai-agent/account/user-name` 同型能力 | 并入 v1.2 |
| 5 | 密钥轮换完成通知 | ⏳ 待运维执行 | 人工：轮换 open-stats 四把密钥（灰度窗口协调 fupan-server / salescoach-agent 两个调用方），并从 anchor-agent 源文档移除明文 |
| 6 | 登录日志最早可信日期 / 订单租户快照切换时间 / 退款取消建设计划 | ⏳ 待查证+立项 | 人工：查生产库 `tb_user_login_log` 最早记录；DDL 配套代码排期后定切换时间；退款取消立项 |

### Contract 定稿轮（2026-08-09 四轮）：open-stats v1.2 范围锁定

对方文档确认后追加 7 问（编号 2–8）→ 我方回复：`docs/售前试用租户客观数据-Contract补充确认-爱复盘-2026-08-09.md`

| 类别 | 内容 | 状态 |
|---|---|---|
| **v1.2 发版范围**（ifupan-anchor-agent，一次交付） | ① items[] + `missingReason`（null/NOT_FOUND/NOT_MAPPED，存在性校验走 fupan-server 内部接口）② 接口 5 每 pair + `used` ③ 5 端点 + `created/active_conversation_count` ④ 文档 §3.5 统计范围（已写入对外发送版） | 待排期 |
| **口径定稿** | 多租户语义 = 单一激活租户 + 历史换租户，无同时多租户；端点 2 是跨历史租户合计，按租户口径一律用端点 5 | ✅ 已定稿 |
| **待书面补充** | v1.2 上线日期；测试环境 HTTPS/网络边界（本机 DNS 走代理 fake-ip 无法判定，需运维）；v1.1 生产部署准确时间（代码 08-08 合入、文档 08-09，部署时间查发布记录）；密钥轮换完成通知 | ⏳ 人工 |

### 实现轮（2026-08-10）：fupan-server 侧 7 个新接口落地

域名确认：测试 `https://testapi.aifupan.com.cn` / 生产 `https://api.aifupan.com.cn`。

| 交付物 | 内容 |
|---|---|
| 对外 API 文档 | `docs/爱复盘-售前客观数据-开放接口文档-2026-08-10.md`：v1.0=存量 3 接口 + 事件推送（含 code=-9 真实错误码、Long 精度警示、refund 占位字段警示）；v1.1=新增 §12–§18 七接口全契约 |
| 新接口代码（全部编译通过） | `/internal/data-hub` 7 端点：customer/tenant-mapping、tenant/members、user/login-stats、tenant/business-accounts、tenant/authorizations、tenant/analysis-stats、order/reconciliation。Controller+Service 在 replay-api（`controller/openapi/DataHubOpenController`、`service/datahub/`），数据层走各模块 Producer 新方法（power: UserProducer.listDataHubMember* / UserLoginLogProducer.aggregateSuccessLogin / BindingAccountProducer.listByParentUserIds；words: AnchorUrlUserProducer.listDataHubAccountsByTenantIds / AnchorVideoProducer.aggregateDataHubAnalysisStats；order: OrderProducer.listDataHubOrders 键集游标） |
| SQL | `sql/replay-44.sql`：tb_order 加 (update_date,id) 与 (tenant_id,update_date) 索引（对账接口前置，需 DBA 执行） |
| 关键实现口径 | 成员=active_tenant_id（含冻结/删除）+ 绑定表补解绑成员；登录=opera_type=0 且 opera_status=0；tenantAttribution 由配置 `jiuyu.datahub.order-tenant-snapshot-since` 推导（未配置=BACKFILLED 保守标记，UNKNOWN=tenant_id 0）；响应 Long 一律 ToStringSerializer |
| 待办 | 部署测试环境 + 为 BDH 发独立 app-id（配置 `jiuyu.oauth.client.api-key-secret`）；DBA 执行 replay-44.sql；排期会给日期；登录日志最早可信日期仍待查证 |
