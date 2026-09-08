---
adr: 0002
title: 现网提示词更新 sql 策略 — UPDATE(replay-32.sql) vs 仅改 INSERT(replay-31.sql)
status: accepted
date: 2026-06-07
source: archive/20260608_patrol-summary-md-cast.md
---

<!-- Source: archive/20260608_patrol-summary-md-cast.md §9 ADR-2 -->

## Context

`sql/replay-31.sql` 已在生产环境执行，cueType=21 的 INSERT 带 `WHERE NOT EXISTS` 幂等保护。现网 `tb_cue_words` 中 `ask_type=21 AND trade_id=1` 行存储的是旧 JSON 格式提示词。若仅修改 `replay-31.sql` 的 INSERT 内容，已执行过的生产环境因 `WHERE NOT EXISTS` 保护跳过 INSERT，旧行不会更新。新代码部署后，AI 仍接收 JSON 格式提示词，用正则提取无标签，`summaryJson` 全为 null，产生**静默数据故障**（只有 log.warn，无显式报警）。

## Decision

新建 `sql/replay-32.sql`，使用 `UPDATE` 语句覆盖现网已执行行；同时修改 `replay-31.sql` 的 cueType=21 INSERT 内容为 Markdown 格式，保证新环境初始化正确。发版顺序：replay-32.sql 先于代码部署执行。

关键约束：`replay-32.sql` 文件头注释须写明"**必须在代码部署前执行**"，Release Notes 明文约定执行顺序。

## Alternatives

### Alternative A: 新建 replay-32.sql（UPDATE 覆盖现网行）+ 同步改 replay-31.sql INSERT（选定方案）

- **Pros:** 现网已执行环境可靠更新；新环境初始化正确；UPDATE 幂等（WHERE 条件限定，重复执行无副作用）；发版顺序清晰可操作。
- **Cons:** 需维护两个 sql 文件；执行顺序依赖人工保证（replay-32.sql 先于代码）。
- **Failure conditions:** DBA 未按顺序执行（代码先于 sql），窗口期内 summaryJson 全 null；UPDATE WHERE 条件漏掉 `is_deleted=0`，误更新已软删除行（风险低，但需检查）。
- **Estimated complexity:** S（1 条 UPDATE sql + 1 处 INSERT 文本修改）
- **Why chosen:** 唯一可靠覆盖现网已执行行的方案；UPDATE 语义清晰；发版顺序可在 Release Notes 明文约定。

### Alternative B: 仅修改 replay-31.sql INSERT 内容

- **Pros:** 操作简单，只改一个文件。
- **Cons:** 现网已执行环境因 `WHERE NOT EXISTS` 保护，INSERT 跳过，旧 JSON 格式提示词永远不更新；新代码上线后 summaryJson 全 null，静默故障，难以发现。
- **Failure conditions:** 100% 必然发生 — 所有已执行过 replay-31.sql 的环境（生产、预生产）均不受影响，旧行保留。
- **Estimated complexity:** XS（仅改文本）
- **Why not chosen:** 必然产生静默数据故障，不可接受。

### Alternative C: replay-32.sql 做 DELETE + INSERT（替换行）

- **Pros:** 语义上比 UPDATE 更彻底（重建行，含 id 和 create_date）。
- **Cons:** 删 INSERT 非幂等（DELETE 后再 INSERT 的 id 可能不同；Snowflake id 已被历史引用时有外键风险）；`WHERE NOT EXISTS` 的 INSERT 和 DELETE 组合如执行顺序出错会造成数据空窗。
- **Failure conditions:** DELETE 成功但 INSERT 失败 → cueType=21 行消失，AI 调用因 cueWord 为 null 导致任务失败（影响所有 tenant）。
- **Estimated complexity:** S（与 A 相当但语义风险高）
- **Why not chosen:** 原子性更差（DELETE + INSERT 非单语句事务）；`tb_cue_words` id 被其他表引用的风险未排查；UPDATE 语义更安全。

## Consequences

**Positive**

- 现网已执行环境可靠更新，消除静默数据故障窗口。
- UPDATE 幂等，DBA 重复执行无副作用。
- 新建文件 `replay-32.sql` 命名延续项目约定，便于追踪。

**Negative**

- 需要两个 sql 文件共同维护同一条 cueType=21 行（replay-31.sql 管新环境 INSERT，replay-32.sql 管现网 UPDATE），维护时需注意两者一致性。
- 若未来再次修改 cueType=21 提示词，需同时更新 INSERT 内容和新增 UPDATE sql。

## Risks

- **Risk:** 发版时 replay-32.sql 未先于代码执行，新代码上线后窗口期内 summaryJson 全 null。
  → **Mitigation:** Release Notes 明文约定"执行 replay-32.sql → 验证 SELECT → 部署代码"发版顺序；Implement 阶段在 replay-32.sql 文件头注释写"**必须在代码部署前执行**"。
- **Risk:** UPDATE 覆盖了 `tenant_id=0` 以外的租户私有 cueType=21 行。
  → **Mitigation:** UPDATE WHERE 条件含 `trade_id=1 AND is_deleted=0`；若生产存在租户私有 cueType=21 行（`tenant_id != 0`），不在 WHERE 命中范围，不受影响。需 Implement 阶段确认 WHERE 条件是否含 `tenant_id=0`。
