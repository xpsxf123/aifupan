# CLAUDE.md — back-fupan-server

直播复盘后端服务。Java 21 / Spring Boot 3 / MyBatis-Plus / MySQL + MongoDB / RocketMQ。
AI 编码代理的唯一入口；每次会话先读这份，其余文件按需 lazy-load。

---

## 1. 行为四原则（每次输出前自检）

**1. 想清楚再写。** 不假设、不掩饰困惑。多种解读时摆出来让人选，别默默挑一个。看不懂就停下问。

**2. 简单优先。** 最少能解决问题的代码。不写未要求的功能、抽象、配置开关。200 行能写成 50 行 → 重写。

**3. 外科手术式改动。** 只动必动。不顺手改风格、不重构没坏的东西、不删非自己引入的死代码。每行变更都能追到用户请求。

**4. 目标驱动执行。** 把任务转成可验证目标（写测试→让它通过 / 复现 bug→让用例修复 / 跑 `mvn compile`→零错误）。多步任务先列步骤 + 每步验证条件。

---

## 2. 硬约束（违反 = 立刻 FAIL）

| 约束 | 触发后果 |
|---|---|
| 反死循环：gate/linter 重试 ≤ 3 次，`mvn compile` ≤ 2 次。超过 → STOP 问人。 | 防爆 |
| Scope Guard：不改 ACTIVE `<run_dir>/focus_card.md` Allowed Scope 之外的业务文件（`.claude/*` 在 HARNESS 白名单内免约束）；需要时发 `[Boundary Exception Request]` 等批准。 | `scope_guard.py` exit 2 |
| 不许自动 commit / push；不许 `--no-verify`、`--no-gpg-sign` 等安全绕过；除非用户明说。 | review BLOCK |
| 不许 commit 的目录/文件：`.claude/runs/`、`target/`、`build/`、`.idea/`、`.vscode/`、`__pycache__/`、`.DS_Store`、`.qoder`、`.env`、`credentials.*`。 | `secrets_linter.py` FAIL |
| **租户隔离**：列表查询、`LambdaUpdateWrapper` 写库必带 `tenantId` + `isRemoveRecord/isDeleted` 过滤（即使 current 已校验）；Controller / BO 禁收外部 `userId`（从 JWT 取）；SQL 占位 `#{}` 禁 `${}`；敏感字段不入日志 | review FAIL（越权/注入/泄露） |
| **数据完整性**：Feign 写方法返 boolean 必接收，false 抛 `RuntimeException` 触发 `@Transactional` 回滚；`switch(Integer)` 前必校 null + 范围（防 NPE）| review FAIL（脏写/崩溃）|
| **新代码守规范**：仅"已存在且本次未改动"的违规可暂不纠正（外科手术 + 登记 TECH-DEBT）；存量先例不构成保留新违规的依据 | review FAIL |
| **Review + Test 强制 dispatch**：PATCH / Standard 写代码任务 Implement 完后**必须** dispatch `@code-reviewer` + `@test-engineer` sub-agent；inline self-check / 仅 `mvn compile` 通过 → 不算闭环 | 视为未完成，Archive 拒收 |

完整安全与提交策略 → @.claude/rules/policy.md

---

## 3. 工作流锚点

**`@` 前缀 = 每会话自动加载**（session 启动就在上下文里，不用 Read）。
**无 `@` 前缀 = 按需 lazy-load**（用 `Read` 工具，只在该 phase / 该场景需要时拉）。

| 主题 | 文件 | 加载方式 |
|---|---|---|
| 路由（Vibe/Standard 模式）+ 阶段 + Hook + 风险分级 | @.claude/rules/lifecycle.md | 自动（每会话用） |
| 技能挂载顺序（哪个 phase 用哪个 skill） | @.claude/rules/skill-precedence.md | 自动（Implement 频繁查 Zone A） |
| 子代理分发模板（必须严格按它发，否则 ESCALATE） | .claude/rules/dispatch-template.md | 按需（只在 dispatch sub-agent 前 Read） |
| TaskList 使用规则（4 个 Pattern + 不重叠机制） | .claude/rules/tasklist-usage.md | 按需（Implement Phase 4 / `/h-fix-bug` / `/h-pr` 强制 Read；其他场景按 Pattern A 决定） |
| OpenSpec 模板（Standard 模式 Propose 阶段交付物） | .claude/llm_wiki/schema/openspec_schema.md | 按需（只在 Propose phase Read） |
| 知识库根 + 下钻 | .claude/llm_wiki/KNOWLEDGE_GRAPH.md | 按需（探索 wiki 时 Read） |
| Agent 角色目录（Explorer / Implement / Review / QA / Archive / Maintenance） | .claude/agents/ | 按需（dispatch 前 Read 具体 agent.md） |
| Skill 索引 | .claude/skills/trae-skill-index/SKILL.md | 按需（不确定哪个 skill 时 Read） |

**三档（粗分类）+ 每 phase 内部弹性裁剪**（完整 Phase Decision Matrix → @.claude/rules/lifecycle.md Part 1）：

| 档 | 触发判定 | 写代码 | openspec | Archive |
|---|---|---|---|---|
| **Vibe** | LEARN / 解释 / typo / 单行修 / 配置改一行 | ≤1 行 | 否 | 否 |
| **PATCH** (中间档) | 写代码 ∧ ≤3 文件 ∧ ≤50 行 ∧ 单模块 ∧ **不动公共面** (API / DB / auth / 错误码) | 是 | 否 | 1 行 changelog |
| **Standard** | 动公共 API / DB schema / auth / 错误码 / 核心业务主路径 / 跨 ≥3 模块 | 是 | 是 | 完整归档 |

**Shortcuts:** `@vibe` / `@patch` / `@standard` 强制对应档。`@patch` **不再是 Vibe 别名**——它是独立的中间档（focus_card + scope_guard + secrets_linter + 1 行 changelog，但不写 openspec）。

**会话第一行必须是路由说明**（无 shortcut 时由主 agent 自动判定）：
```
[Route] Mode=<Vibe|PATCH|Standard> | Risk=<MEDIUM|HIGH>(Standard only) | Phases={...} | Reason: <一句话>
```

**Mid-flight 升降级允许** —— 发现意图复杂度跳档，emit `[Mode Change] <from> → <to>` 补/省 phase，**已写代码不丢弃**。

---

## 4. 模块拓扑

```
replay-api (入口, 端口 6606)
  ├─ replay-power     用户/租户/角色/菜单/销售统计
  ├─ replay-words     直播分析 / 词规则引擎 / 敏感词 / 音频
  ├─ replay-ai        AI 诊断 / 提示词模板 (MongoDB)
  ├─ replay-order     商品/套餐/订单/支付/邀请码
  ├─ replay-agent     代理渠道/佣金/邀请奖励
  ├─ replay-activity  邀请活动/进度奖励
  ├─ replay-reward    奖励记录
  ├─ replay-third     微信支付/支付宝/七牛/字节AI/短信
  ├─ replay-video     视频热搜/达人/群组 (MongoDB)
  ├─ replay-system    字典/系统配置
  ├─ replay-common    AOP/Redis/MQ/分布式锁/防重复
  └─ replay-generic   R<T> / PageUtils / Feign / 枚举 / 异常
```

依赖方向：`replay-api → 业务模块 → replay-common → replay-generic`。
**跨模块调用走 `replay-generic` 的 Feign 接口**，禁止跨模块直连 Dao。
历史遗留分层 `Bll / Producer / Rse`（仅 words/order/agent）：读懂时遵循，新代码默认 **Controller → Service → Mapper** 三层，不要扩散。

技术栈具体版本以 `pom.xml` 为准。架构细节 → @.claude/llm_wiki/wiki/architecture/。

---

## 5. Java 项目特异约束（Claude 猜不到的项目个性）

| 约束                                                                                    | 违反后果 |
|---------------------------------------------------------------------------------------|---|
| DI 用 构造器注入，禁 `@Autowired` ,`@Resource`                                                | checkstyle FAIL |
| Controller 返回 `R<T>`（`com.jiuyu.replay.generic.vo.common.R`），禁裸返业务对象                  | api-standard FAIL |
| 分页用 `PageUtils<T>` 包装 `IPage`                                                         | review FAIL |
| ID 用 `SnowflakeManager.nextValue()`；实体 `@TableId(type = IdType.INPUT)`                | review FAIL |
| 软删除手动设 `isDeleted` 字段，禁 `@TableLogic`                                                 | review FAIL |
| 时间戳手动 `LocalDateTime.now()` 设 `createDate / updateDate`                               | review FAIL |
| 查询用 `LambdaQueryWrapper`（类型安全）                                                        | review WARN |
| 写方法加 `@Transactional(rollbackFor = Exception.class)`                                  | review FAIL |
| Controller 必须严格按照标准的Javadoc生成注释                                                       | review WARN |
| `IN (...)` 列表 > 500 用 `Lists.partition(ids, 500)` 分批 或使用 BatchQuery分批查询工具类            | review FAIL（性能） |
| Bean 拷贝用 Spring `BeanUtils.copyProperties`                                            | review WARN |
| 命名：`XxxEntity / XxxBo / XxxVo / XxxDao / XxxService / XxxServiceImpl / XxxController` | review WARN |
| **🚨 代码标识符禁中文（绝对红线）**：class / interface / method / field / parameter / local / constant / enum / package 名必须全英文；中文仅允许出现在注释 / Javadoc / 字符串字面量 / `@DisplayName/@Schema/@Operation` 等注解 String 参数内。测试方法名范式：`methodUnderTest_scenario_expected` + 配 `@DisplayName("中文场景")`。详 `memory/feedback_no_chinese_identifiers.md` | **review CRITICAL** |
| **跨模块走 `replay-generic` Feign SPI**，禁 Controller 直注其他模块 Service / 直 import `repository.service.*` / `entity.*`（先例不豁免）| review FAIL |
| **错误码 enum 名先 Read `StatusCode.java`**，不凭命名推断（70005 是"请先确认标准稿"，70014 才是"功能未上线"）| review WARN |
| **`tb_commodity_type` 新加 code 必配套主账号 `tb_user_property_type` 占位 INSERT** → `wiki/data/commodity_type_migration.md` | review FAIL（子账号开通爆炸） |
| **MQ `syncSendNormalMessage*` 系列是"伪同步"**（只写本地表 + XXL-Job 5min 扫表接力）；新代码用 `syncSendAndDeliverToTopic`（业务直发 + 兜底）→ `wiki/architecture/mq_dual_track.md` | review WARN |
| **`AnchorUrlUserEntity` 软删除用 `isRemoveRecord`**（业务从列表移除）而非 `isDeleted`（MP 逻辑删除）| review FAIL |

L1–L7 完整编码标准 → `.claude/skills/java-engineering-standards/SKILL.md`（按需 Read；及 skill-precedence.md 的 Zone A 路由）

---

## 6. 会话起步

1. 读本文（你正在读）。
2. 有 `.claude/runs/launch_spec_*.md`？恢复未完成任务，从其 Phase 接着走。
3. 用户给了具体路径 / 代码片段？直接读，跳过 wiki 漫游。
4. 意图不明 → 问**一个**澄清问题再动手。

---

## 7. 知识下钻索引

| 找什么 | 看哪 |
|---|---|
| 业务术语 / 状态机 / 流程 | `.claude/llm_wiki/wiki/domain/` |
| API 接口表 / 鉴权 / 路径 | `.claude/llm_wiki/wiki/api/` |
| DB 表 / 索引 / ER | `.claude/llm_wiki/wiki/data/` |
| 架构 / ADR / 定时任务 | `.claude/llm_wiki/wiki/architecture/` |
| 红线 / 反模式 / 安全基线 | `.claude/llm_wiki/wiki/preferences/` |
| 进行中 / 近期归档 spec | `.claude/llm_wiki/wiki/specs/` |
| 调研 / 可行性报告（Scenario F，`/h-research` 产） | `.claude/llm_wiki/wiki/research/` |
| 已归档 spec（traceability） | `.claude/llm_wiki/archive/` |

---
