---
intent: Change
profile: STANDARD
risk: MEDIUM
frontend-facing: false
module: replay-ai + replay-words + replay-generic
slug: script-monitor-prompt-by-trade
created: 2026-06-04
---

# OpenSpec — 话术质检按直播间行业取提示词

## 1. Context

话术质检（ScriptMonitor）异步生成报告流程当前通过 `CueWordsFeign.getCueWordsByType(cueType)`
按"全局首条"取提示词，未区分行业。线上规划要求按**直播间所属行业**（即对应录播视频的
`tb_anchor_video.trade_id`）取行业定制的质检提示词，并支持行业父链回溯。

现有优化原文/自然原文（`AnchorVideoDetailBll.toSelectTrade` → `CueWordsProducerImpl.queryPage`）
已实现"按 tradeId + cueType + 行业父链回溯 + 兜底 trade_id=1"的范式，且不清空 `problem` 字段
（绕开 `pageCueWords` C2 坑）。本次改造复用该范式，新增一个面向"行业+cueType"的轻量 SPI。

## 4. Data Model

无 DDL 变更。仅查询逻辑由"按 cueType 全表第一条"改为"按 tradeId 父链回溯 + cueType"。

涉及表：
- `tb_anchor_video.trade_id`（已存在；本次只读）
- `tb_cue_words(trade_id, cue_type, apply_to, tenant_id, sort)`（已存在；查询路径变化）

## 5. Business Logic

### 5.1 新增 SPI

`CueWordsFeign.getCueWordByTradeAndType(Long tradeId, Integer cueType): CueWordsInfoVo`

实现位 `CueWordsApi`：
1. 组装 `CueWordsListBo`：`applyTo=0`（单条分析）、`cueType` 入参、`queryType=0`（仅取通用 tenant_id=0）。
2. 调 `TradeProducer.listParentsByTradeId(tradeId, GENERAL_NO)` 取行业父链；按 `Comparator.reverseOrder()`
   排序后塞入 `tradeIds`（子→父优先）。
3. `page=1, limit=1` 调 `cueWordsProducer.queryPage`（**不清空 problem** —— 复用现有优化原文链路）。
4. 命中返第一条转 `CueWordsInfoVo`；未命中返 `null`。

行业兜底语义由 `CueWordsProducerImpl.getClientWrapper` 现有逻辑保证：
- tradeIds 中按顺序找第一个有数据的；
- 若全部无数据 → 兜底 `trade_id=1`（通用行业）。

### 5.2 ScriptMonitorGenerateBll 改造

- 新增依赖：`AnchorVideoFeign anchorVideoFeign`。
- 新增 helper `resolveTradeId(ScriptMonitorReportEntity report): Long`：
  - `sourceType=0` 录制视频 → `anchorVideoFeign.GetByVideoId(sourceId).getTradeId()`；
    取不到（video 不存在或 tradeId 为空）→ 兜底 `1L`。
  - `sourceType=1` 上传文件 → 直接兜底 `1L`（TECH-DEBT 登记：`UploadFileSimpleInfoVo`
    暂未暴露 trade_id，待 `SensitiveWordsFeign` 增补字段后回填）。
- 改 `loadSingleCueWord` 签名加 `Long tradeId`，内部改调
  `cueWordsFeign.getCueWordByTradeAndType(tradeId, cueType)`；返回 null 时抛 BusinessException
  （文案携带 tradeId 便于排查）。
- `generate()` 入口在取 ASR 后、取生成提示词前调 `tradeId = resolveTradeId(report)`，同一个
  tradeId 传给生成提示词（cueType=16）和合并提示词（cueType=17）。

### 5.3 删除旧路径（一刀干净，无 deprecated）

- `CueWordsFeign.getCueWordsByType(Integer cueType)` 接口删除。
- `CueWordsApi.getCueWordsByType` 实现删除。
- `CueWordsProducer.listByCueType / CueWordsProducerImpl.listByCueType` 接口 + 实现删除。
- `ScriptMonitorGenerateBllTest` 中 6 处 `when(cueWordsFeign.getCueWordsByType(...))` 改为
  新 SPI 的 stub；test javadoc / 注释同步更新。

唯一调用方均在本次 Allowed Scope 内（grep verify 已完成）。

## 6. Non-Functional Constraints

- **不变量**：保持 `problem` 字段不被清空（沿用 queryPage 路径）。
- **租户隔离**：新 SPI `queryType=0` 写死，仅取通用提示词（tenant_id=0），与现状一致；
  租户定制本次不开放（已与产品确认）。
- **accountType**：本次不参与查询（默认 null）；质检不区分自有/同行账号。
- **性能**：行业父链查询 + 单条提示词查询，最多 2 次 SQL；同 generate() 内 cueType=16/17
  各调一次共 4 次，与改造前（2 次）相比每报告 +2 次轻量查询，可接受。
- **回滚**：仅代码改动，无 DDL；git revert 即可。

## 7. Acceptance Criteria

- **AC-1**：Given 一条录制视频源（sourceType=0，对应 `tb_anchor_video.trade_id=X`，且该行业
  在 `tb_cue_words` 配置了 cueType=16/17 提示词），when 触发质检 generate，then 生成
  与合并阶段均使用 `trade_id=X` 对应的提示词 problem 文本调 AI。
- **AC-2**：Given 录制视频对应 trade_id=X，但 X 在 `tb_cue_words` 无 cueType=16 配置，X 的父行业
  Y 有配置，when 触发 generate，then 回退使用 trade_id=Y 的提示词（行业父链回溯）。
- **AC-3**：Given 文件源（sourceType=1）或 video.tradeId 为空，when 触发 generate，then
  按 trade_id=1（通用行业）取提示词；通用行业未配置 → 抛 BusinessException 文案包含
  "未配置（tradeId=1）"。
- **AC-4**：Given 旧 SPI `getCueWordsByType`、`listByCueType` 已删，when `mvn compile`，
  then 编译通过；全仓 grep `getCueWordsByType|listByCueType` 仅匹配 git 历史。
- **AC-5**：Given 单测 `ScriptMonitorGenerateBllTest`，when `mvn -pl replay-ai test
  -Dtest=ScriptMonitorGenerateBllTest`，then 全部用例通过（6 处旧 stub 已迁移到新 SPI）。

## Allowed Scope

见 `focus_card.md`。

## Plan Deviation Reflection

**偏差 1：实现策略调整（首版 → reviewer MAJOR-1 修复后）**
- 首版 `CueWordsApi.getCueWordByTradeAndType` 复用 `cueWordsProducer.queryPage`（继承优化原文范式），但该路径**不带 isDeleted=0 过滤**——是从旧 `listByCueType` 迁移过来的一处回归。
- 修复：新增 `CueWordsProducer.listByTradeIdsAndCueType` 方法显式带 `isDeleted=0 + tenant_id=0 + applyTo=0`，行业父链回溯算法从"DB 端 getClientWrapper 批量 count 找命中"改为"API 内存端按 tradeIds 顺序取分组 sort 最小"。

**偏差 2：兜底 trade_id=1 触发时机扩大**
- 首版 Javadoc 写"tradeId=null 时走 trade_id=1"，但实现里 null 时只是跳过 tradeIds 装填，导致 getClientWrapper 返回无 trade_id 约束的 wrapper（reviewer MAJOR-2 指出契约与实现不符）。
- 修复：CueWordsApi 现在**永远在 tradeIds 末尾追加 1L**（除非已含）。Javadoc 同步更新为"末尾追加兜底"语义。

**偏差 3：测试覆盖延伸**
- 首版仅迁移 6 处旧 SPI stub 到新 SPI，无新增用例。
- test-engineer 评估指出 AC-3 sourceType=1 兜底路径零覆盖。新增 `testGenerate_fileSource_useFallbackTradeId` 用例（验证 `anchorVideoFeign` 不被调用 + 按 tradeId=1 取提示词），单测从 11/11 提升至 12/12。

**偏差 4：登记两条新 TECH-DEBT（非本 PR scope，按外科手术原则）**
- DEBT-020：`buildTokenRecord.useSourceType` 硬编码（reviewer MAJOR-4，本 PR 未触及该方法行）。
- DEBT-021：`CueWordsApi` 层无单测基建，AC-2 父链回溯 / AC-3 SPI 层 null 防护无回归网。
