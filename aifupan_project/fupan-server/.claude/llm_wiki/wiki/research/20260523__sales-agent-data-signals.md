# Research — 销售智能体行为信号数据可行性

**Date:** 2026-05-23
**Question source:** User via /h-brief（误用，本应走研究路径，详见末尾"Process Note"）
**Status:** 调研完成 → 已生成对外交付版 `20260523__sales-agent-data-spec-handoff.md`；本文件保留为内部 working notes，**§2.5 / Rule 2 / 跨租户语义部分已被 errata 覆盖，见末尾 Errata 段**。

---

## 1. 问题陈述（用户原文）

> 找到 wiki CRM 的内容；基于这个业务背景再从 wiki 或代码中查找并分析销售智能体所需的数据支持我们能给到什么程度。智能体现在缺关键行为没有，销售去 push；关键行为有了，是后续聊天的话术切口。大概需要：
>
> 1. 添加账号多少个，包含自有账号多个；
> 2. 运营助手、对比复盘 这两个是否使用是关键；
> 3. 分析时长消耗多少；算力消耗多少。

**核心诉求：** 给 SalesCoach Agent（企微侧边栏 AI 销售助手）补"行为信号"，让它能按"行为有 / 没有"分流话术——**无 → push 销售触达；有 → 作为聊天切口**。

---

## 2. CRM 当前已经提供给智能体的数据（基线）

`replay-api/.../crm/impl/CrmIntegrationServiceImpl.java` 是当前对智能体的唯一集成接口，已暴露：

| 接口 | 返回 |
|---|---|
| `POST /internal/crm/customer/batch-aggregate-query` | userId / customerName / sales（归属销售）/ orders（订单列表） |
| `POST /internal/crm/customer/profile-sync` | 写：客户标签 / 行业 / 意向度等画像字段（`tb_user_details`） |
| `POST /internal/crm/customer/ai-profile` | 读 / 写：AI 画像 JSON（`tb_crm_ai_profile`） |
| `POST /internal/crm/customer/stage-event` | 写：阶段事件（`tb_crm_stage_event`） |
| `POST /internal/crm/customer/order-events`（出站） | 推：订单生命周期事件 |

**鉴权：** `@APIKey` + `APIKeyInterceptor`（ADR-CRM-003），无 Session。
**匹配键：** `phone → user_id`（ADR-CRM-004）。
**已可问出的语义：** "你是谁 / 跟进到哪一步 / 买了什么"。
**缺失的语义：** "你**用**了什么 / **用**了多少"——这就是用户要补的部分。

---

## 3. 用户三类信号 — 现状 + Gap 一览

### 3.1 信号速查表

| 用户原文信号 | 真实落库 | 模块 | 现有查询能力 | Gap | 工作量 |
|---|---|---|---|---|---|
| 添加账号数 | `tb_anchor_url_user`（按 `user_id` count，filter `is_remove_record=0 AND is_deleted=0`） | replay-words | `AnchorUrlUserFeign#getAnchorCountByUserIdsAndTenantId` 已存在 → `UserAnchorCountDto.anchorCount` | 直接可用 | — |
| 其中"自有账号"数 | 同上 + 加 filter `account_type=0` | replay-words | 无（现有 Feign 不分组） | 扩 1 个 Feign 方法 + 1 段 GROUP BY SQL + DTO 加 `ownAnchorCount` 字段 | **S** |
| 运营助手是否使用 | `tb_ai_token_use_record.assistant_type = 0`（OPERATION） | replay-order | 无跨模块 Feign | 新增 Feign + Api：`hasUsedOpAssistantByUserIds(userIds, tenantId) → Set<Long>` | **S** |
| 对比复盘是否使用 | `tb_sync_contrast` 存在记录 + `delete_status < 2`（未彻底删）+ `is_deleted=0` | replay-words | 无 Feign，无 Bll 方法 | 新增 Feign + Api + Bll + Rse + Mapper：`hasUsedContrastByUserIds(userIds, tenantId) → Set<Long>` | **M**（5 个新文件） |
| 服务时长（注册至今） | `tb_user.create_date` → `now()` 差值（小时） | replay-power | `AnchorVideoBll#getUserServiceDuration` 仅单用户 | 单 user 已可，批量需在 Service 层循环（≤50 用户可接受） | **XS**（在编排层算即可） |
| 已消耗语音分析时长 | `tb_user_property_*`（属性表） | replay-order | `UserPropertyFeign` 已存在，但无 `getUseAiAnalysisTime` 单方法 | 扩 1 个 Feign 方法 + Api 实现 | **S** |
| 算力消耗（全量） | `SUM(tb_ai_token_use_record.total_tokens)` group by user_id | replay-order | `AiTokenUseRecordLogic#queryPage` 只有分页，无聚合 | 新增 Feign + Api + Dao + Mapper：`sumTokensByUserIdsAndTenantId` | **S** |
| 算力消耗（近 30 天） | 同上 + `create_date >= NOW() - INTERVAL 30 DAY` | replay-order | 无 | 同上方法的兄弟方法 | **XS**（与全量同 Mapper 多写一个 method） |

### 3.2 工作量汇总

- 现成可用：**1 项**（总账号数）
- XS 扩展：**2 项**（服务时长在编排层算、近 30 天 token = 全量 token 的兄弟方法）
- S 扩展：**4 项**（自有账号 / 运营助手判断 / 语音分析时长 / 全量 token 聚合）
- M 新增：**1 项**（对比复盘——`tb_sync_contrast` 此前完全没暴露给跨模块）

**预估文件触动数：跨 5 模块（api / power / generic / words / order）26 个文件**。其中：
- 新增文件 9 个（4 新 Feign 接口 + 2 新 Api 类 + 1 新 Rse 接口 + 1 新 Rse impl + 3 个 BO/VO）
- 修改文件 17 个（既有 Controller / Service / Bll / Dao / Mapper / DTO 扩字段）

详见已生成的 focus_card.md（在 `.claude/runs/Change__2026-05-23_00-41-30/focus_card.md`，若决定转 Change 可直接复用）。

### 3.3 几个隐藏决策点

**a. "运营助手 / 对比复盘是否使用"的判定窗口**
当前定义"历史上是否有过一条记录"——只要曾用过就 `true`。如果业务想区分"曾用 vs 近期活跃"（即"用过但已沉睡"也要 push），需要加时间窗口字段。**建议：先按"历史是否用过"上线，话术策略迭代不用回改后端。**

**b. `tb_ai_token_use_record.assistant_type` 枚举完整含义**
`OPERATION=0` 对应"运营助手"已确认。同表还有 `useSourceType` 字段，对比复盘对应的是 `useSourceType=SYNC_ANALYSIS=2`，但实际"对比复盘"业务数据落在 `tb_sync_contrast`，**两个表互不依赖，独立查**。

**c. 服务时长 vs 配额消耗的语义区别**
用户原文"分析时长消耗多少"两种合理解读：
- 服务时长：`tb_user.create_date` → 注册至今小时数（"用了我们多久"）
- 配额消耗：`UserPropertyFeign.getUseAiAnalysisTime` → 实际跑了多少分析任务的时长配额（"真消耗了多少"）

**用户已决议：两个都返回**（这是真正反映"关键行为深度"的更有效信号；服务时长只反映"在册时间"）。

**d. 算力消耗的时间窗口**
全量 SUM 反映"总投入"，近 30 天反映"近期活跃"。**用户已决议：两个都返回**（智能体侧自行解读"沉睡 vs 活跃"）。

**e. 跨租户语义**
同一 `user_id` 可能跨多租户使用，但安全基线（CLAUDE.md §2）要求"列表查询必须带 tenantId 过滤"。**默认按 `user.activeTenantId` 单租户隔离**，不跨租户汇总。

---

## 4. 接口形态建议

两条路：

**A. 新增独立端点 `POST /internal/crm/customer/behavior-signals`**
- 优点：契约隔离，不污染 `batch-aggregate-query` 的 VO；智能体侧也能按需调用
- 缺点：智能体获取"完整客户画像"时要调 2 个接口
- 推荐度：⭐⭐⭐⭐⭐（已在调试用 focus_card 锁定此方案）

**B. 扩展现有 `/batch-aggregate-query` VO，加 `behaviorSignals` 字段块**
- 优点：智能体一次调用拿全部数据
- 缺点：现有 VO 字段表膨胀；既有调用方需要兼容；后端查询路径强耦合（必查 4-5 路 Feign 才能装配完整 VO，无法按需）
- 推荐度：⭐⭐

**唯一不推荐：** GraphQL / 字段按需选择——内部 server-to-server 调用，过度工程。

---

## 5. 推荐路径

按工作量 + 智能体话术策略价值排优先级：

| 优先级 | 范围 | 工作量 | 话术价值 |
|---|---|---|---|
| **P0** | 总账号 + 自有账号 + 运营助手 + 对比复盘 + 服务时长 | S + S + S + M + XS = 中等 | **核心切口**，"用没用 / 用了几个" 是 push 决策的二值信号 |
| **P1** | 算力消耗（全量 + 近 30 天） | S + XS = 小 | 区分"沉睡 vs 活跃"，对续费 / 复购话术有用 |
| **P2** | 已消耗语音分析时长 | S | 与算力 token 重合度高，可后续迭代 |

**推荐：P0 + P1 一次性做完**（边际成本低，已在同接口聚合），P2 视上线后智能体话术效果决定是否补。

---

## 6. 待澄清 / 待用户决策

1. **本调研报告的下一步：**
   - (a) 直接转 Change，立即派 Implement（focus_card 已 ready）
   - (b) 给智能体团队看，征询字段够不够再定
   - (c) 先观望，存档不动
2. **接口形态：** 推荐 A（独立端点），如有不同意见请说
3. **P2（已消耗语音分析时长）做不做：** 多 1 个 Feign 方法的成本，看你

---

## Process Note（流程反思，给主 agent / 未来读者）

本次任务**原本应当走 research 路径而非 `/h-brief`**——用户原文"能给到什么程度"是教科书级别的 feasibility 信号。但当前命令矩阵缺一档"研究 / 可行性"模式，主 agent 误把研究意图当成 Change 意图，跑完了 `/h-brief` + `/h-design` 全套仪式，多派了一次 `@system-architect`（~108k token 浪费）。

实际产出的有用资产：
- 本研究报告 ✓
- `focus_card.md`（如转 Change 可直接复用，含 26 文件 Allowed Scope）✓

被丢弃的资产：
- `openspec.md`（设计契约——内容正确但不该这时写）
- `launch_spec_20260523_004130.md`（Change 状态机）

**后续改进提议：** 加 `/h-research` 命令 + `@research` shortcut + 升级 `ambiguity-gatekeeper` 在动词为"分析/调研/评估/可行性"时 BLOCK 并提示走 research 路径。具体方案待用户确认后落地。

---

## Source Material（已读 wiki + 已 grep 代码）

**Wiki:**
- `.claude/llm_wiki/wiki/domain/crm_domain.md`
- `.claude/llm_wiki/wiki/architecture/crm_architecture.md`
- `.claude/llm_wiki/wiki/specs/2026-05-14-crm-spec-c-stage-event.md`
- `.claude/llm_wiki/wiki/domain/power_domain.md`
- `.claude/llm_wiki/wiki/data/power_data.md`

**代码（grep 验证的关键路径）：**
- `replay-api/.../crm/impl/CrmIntegrationServiceImpl.java`（现有集成层）
- `replay-power/.../vo/crm/CrmCustomerAggregateItemVo.java`（现有 VO）
- `replay-generic/.../dto/words/UserAnchorCountDto.java`（要扩 `ownAnchorCount`）
- `replay-power/.../bll/UserBll.java:1545`（已调用 `getAnchorCountByUserIdsAndTenantId`，扩展时需同步）
- `replay-api/.../logic/words/AnchorVideoLogicImpl.java`（写 `tb_ai_token_use_record` 时设 tenantId）
- `replay-generic/.../vo/power/SubUserListVo.java:70`（使用 `UserAnchorCountDto.anchorCount`，扩 `ownAnchorCount` 字段时确认兼容）

调用链已 grep 验证，无遗漏的隐式 caller / dependent。

---

## Errata（2026-05-23 晚，用户复核后更正）

### E-1：聚合范围模型更正（**重要**）

**原文（已废弃）：**
> Rule 2: tenantId 强制使用 UserDto.activeTenantId，不接受调用方传入 tenantId，**不跨租户汇总**。
> 数据查询模型为 "以 phone→userId 反查的那一个 user 为查询主体"。

**正确语义：**
"客户"在销售视角下 = **一个团队** = **主账号 + 该主账号名下的所有子账号**。所有计数 / 累计 / 是否使用类信号应按**团队整体**聚合，而非单个登录账号。

**技术实现的影响：**

1. phone→userId 反查后，需先沿 `parentId` 链回溯到主账号（项目已有 `power_architecture.md` ADR-006 父子链建模，深度 ≤2）。
2. 列出主账号 + 所有子账号的 user_id 集合（已有 `getSubUserCountByUserId` 同源能力，需补一个 `listSubUserIdsByParentId` 类的 Feign 方法）。
3. 所有跨模块聚合 SQL 的 `WHERE user_id = #{userId}` 改为 `WHERE user_id IN (#{userIdSet})`，并加 `GROUP BY 客户单元` 或在内存层 SUM。
4. `tb_anchor_url_user` / `tb_ai_token_use_record` / `tb_sync_contrast` 的查询过滤改 IN 集合。
5. 例外：`serviceDurationHours`（服务时长）仍按主账号 `tb_user.create_date` 取——业务语义是"团队成立时长"，子账号是后续加入。
6. 单租户隔离的安全约束依然成立：主账号的 `activeTenantId` 限定整个团队的查询租户范围（子账号继承父账号 activeTenantId，见 power_architecture ADR-004）。

**受影响的字段（聚合范围全部改为团队合计）：**
`addedAnchorCount` / `ownAnchorCount` / `hasUsedOpAssistant` / `hasUsedCompareReplay` / `aiAnalysisTimeConsumed` / `totalAiTokensUsed` / `recentAiTokensUsed30d`。

**新增字段：** `customerUnitSize`（团队账号总数 = 1 主账号 + N 子账号），让智能体侧能区分"个体客户 vs 团队客户"。

### E-2：工作量重新估算

聚合范围改成"主账号 + 子账号 IN 查询"后：

- 新增 1 项："列子账号 user_id 集合" 的 Feign 方法 — **XS**（power 模块已有相邻能力）
- 既有 5 项 Feign 方法的入参从 `userId` 改 `List<Long> userIds` — **XS 每项**（增加 IN 分批保护）
- SQL 改 `IN` + 内存层加 group 装配 — **S**（在原工作量基础上 +1 档）
- 整体规模仍可控，跨 5 模块、文件触动数 +1（多一个 list-sub-users Feign），不改路由级别（仍属 Standard MEDIUM 范畴）

### E-3：上线建议补强

智能体侧调用前**强烈建议**用真实客户（含子账号场景）做联调，验证：

- 子账号身份的 phone 是否正确回溯到主账号
- 团队 size = 1（仅主账号，无子账号）的客户是否符合预期
- 仅子账号有使用记录、主账号无使用记录的场景

这是因为 jiuyu 的 B2B 客户中，主账号普遍只是签约人不参与操作，单元测试容易漏掉这类边界。
