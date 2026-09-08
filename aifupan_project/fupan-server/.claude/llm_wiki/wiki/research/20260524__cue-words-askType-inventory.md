# Research Companion — 16 种 askType 提示词全图鉴 & 存储分布

**Date:** 2026-05-24
**Status:** 调研完成（基于 DB SELECT 结果 1273+41=1314 行 + 代码 grep）
**Parent:** [20260524__ai-script-assistant-business-flow.md](./20260524__ai-script-assistant-business-flow.md)
**Sibling:** [20260524__ai-script-assistant-cue-words-sample.md](./20260524__ai-script-assistant-cue-words-sample.md)（话术助手 askType=6 单独专题）
**数据源：**
- `/Users/hehui/Downloads/执行结果3.txt`（41 行 cueType=6 话术助手）
- `/Users/hehui/Downloads/执行结果4.txt`（1273 行 cueType ∈ {0,1,2,3,4,5,7,8,12,13}）
- 代码 grep 验证缺失 5 个 askType (9/10/11/14/15) 的存储位置

---

## 1. 数据规模综述

- **总 1314 条 prompt** 跨 11 个 cue_type
- **54 个行业**（trade_id unique 49 + 话术助手数据 5）
- **45 条租户私有 prompt**（27 + 18 — `tenant_id ≠ 0`），其余系统级
- **2 个 cue_type 未在 tb_cue_words 出现**：9 / 10 / 11 / 14 / 15 → 走 systemKv 或代码硬编码（详见 §2）

---

## 2. 16 askType × 存储分布全图鉴

按"存储位置"分组（所有 askType 0-15 一次性覆盖）：

### 2.1 存 `tb_cue_words` 表（10 种 cue_type，共 1314 条）

| askType | 枚举 | tb_cue_words 条数 | 主要 trade 覆盖 | 备注 |
|---|---|---|---|---|
| 0 | OPERATION 运营助手 | **1071** | 49 个行业 | 占总量 81%；最大宗 |
| 1 | VIOLATION 违规助手 | 34 | 多行业 | |
| 2 | BARRAGE 弹幕助手 | 72 | 多行业 | |
| 3 | SCREENSHOT 截图助手 | 2 | 通用 | 仅 2 条，可能 cue_word 多以 systemKv 复用 |
| 4 | BOARD 看板助手 | 5 | 通用 | |
| 5 | IMPORTANT_SCREENSHOT 重要弹幕 | 3 | 通用 | 配合 systemKv `important_barrage_ai_code`（AnchorVideoLogicImpl:1415） |
| **6** | **AI_SCRIPT_ASSISTANT 话术助手** | **41** | 4 行业 | 详见 [cue-words-sample.md](./20260524__ai-script-assistant-cue-words-sample.md) |
| 7 | NATURAL_ORIGINAL_TEXT 自然原文 | 3 | 通用 | |
| 8 | OPTIMIZE_ORIGINAL_TEXT 优化原文 | 2 | 通用 | |
| 12 | AI_HTML_PROMPT HTML 转换 | **1** | 通用 | 单条；配合 systemKv `html_ai_model_default` 选模型 |
| 13 | DATA_DIAGNOSIS 数据诊断 | 80 | 多行业 | 配合 `diagnosis_ai_model_default` |

### 2.2 存 systemKv（3 种）

| askType | 枚举 | systemKv key | 调用点 |
|---|---|---|---|
| 9 | TRADE_RECOMMEND 行业推荐 | `suggest_trade_ai_model`（仅存模型 code；prompt 由 `anchorVideoBll.getIndustryPrompt` 动态拼装含视频内容 + 行业树） | `AnchorUrlLogicImpl.java:1021-1034` |
| 14 | CHECK_AI_CORRECT 检查 AI 内容 | `check_ai_content_prompt`（prompt 文本） + `check_ai_content_model`（模型 code）+ `check_ai_content_prompt_confirm`（确认） | `AiRelatedLogicImpl.java:731-783` |
| 15 | AI_CORRECT_CONTENT AI 纠正内容 | `correct_ai_content_prompt`（prompt 文本） + `correct_ai_content_model`（模型 code） | `ConversationBll.java:848` + `correct_generate_expiration_time` 超时控制 |

### 2.3 代码硬编码 system prompt（1 种）

| askType | 枚举 | 硬编码位置 | system prompt 文本 |
|---|---|---|---|
| 11 | ANCHOR_KEYWORD 主播关键词获取 | `AnchorUrlLogicImpl.java:978` | `"你是一个顶尖关键词内容提炼专家"` — user prompt 由代码动态从视频内容拼装 |

### 2.4 死代码（1 种）

| askType | 枚举 | 状态 |
|---|---|---|
| 10 | EXTRACT_VIDEO 提取文案优化 | **全代码 0 引用** — `AiEnums` 中定义但无任何 grep 命中（既不 setUseSourceType / 也不 setAssistantType / 也不 tb_cue_words 数据）。要么是**未上线功能**，要么是**已废弃保留枚举**。建议产品 / 开发确认 |

### 2.5 复合（按情况兜底）

- askType=12 (HTML) — `tb_cue_words` 仅 1 条 + systemKv `html_ai_model_default` 选模型 + `html_system_extra_prompt` 拼接段
- askType=14/15 — 完全走 systemKv，无 tb_cue_words 数据（一次性单 prompt，不需按行业 / 场景多版本）

---

## 3. 字段语义二级分类（基于 cue_word 文本对比推断）

以下字段在伴生文档 `cue-words-sample.md` 中因 cueType=6 全为 0 无法判断；从全量数据反推：

### 3.1 `account_type` — **不是租户隔离，疑似平台账号变体**

- 分布：739 (acc=0) / 534 (acc=1)，几乎对半
- 证据：cueType=0 中**完全相同的 cue_word 文本**（"帮我拆解这场直播的框架结构..."）在 acc_type=0 和 acc_type=1 同时存在，id 不同
- 推断：**抖音账号 vs 其他平台（快手 / 视频号）账号 prompt 变体**（待 PM 确认）

### 3.2 `sync_scene` — **同步分析场景**

| 值 | 条数 | 推断含义 | 证据 |
|---|---|---|---|
| 1 | 1224 | 默认对话型（单视频或单文件） | 大部分对话型 prompt |
| 2 | 38 | **跨场对比分析** | cue_word 含 "相同和不同的地方、并举例（框架话题）" |
| 3 | 11 | **同直播间不同场次对比** | cue_word 含 "对比诊断相同直播间不同场次..." |

对应代码 `sourceType=2 SYNC_ANALYSIS`（对比分析对话），但 `sync_scene` 进一步细分对比类型。

### 3.3 `apply_to` — **是否多场聚合**

| 值 | 条数 | 推断含义 |
|---|---|---|
| 0 | 1220 | 单视频 / 单文件场景 |
| 1 | 53 | 多场对比聚合（与 sync_scene=2/3 强相关） |

### 3.4 `scope` — **来源类型补充（直播 vs 短视频）**

| 值 | 条数 | 推断含义 | 证据 |
|---|---|---|---|
| 0 | 1253 | 直播场景 | 大部分 |
| 1 | 20 | **短视频场景** | cue_word 含 "（2小时内视频效果最佳）" |

### 3.5 字段 `resource_type` — 全 0（无差异化语义，可能死字段）

### 3.6 字段 `scene` — 几乎全 0（7 条 = 1），用途不明（待开发确认）

### 3.7 字段 `outline` / `remarks` — 未在本次扫描验证

---

## 4. 与父报告 / sample 的交叉关系

| 文档 | 角色 | 关注点 |
|---|---|---|
| 父报告 `...business-flow.md` §2.1 | 16 askType 总览 + 装配器映射 | 代码层面 |
| 伴生 `cue-words-sample.md` | askType=6 话术助手专题 | 12 子能力 + 4 行业 |
| **本文档** | 全 16 askType × 存储分布 × 字段语义 | DB 层面 + 缺失位置 |

合并视角：父报告 + 伴生 sample + 本文档 = 完整的话术助手 + 全 AI 问答 prompt 体系。

---

## 5. 重要发现：wiki 偏差补充

本数据进一步坐实 [父报告 G4](./20260524__ai-script-assistant-business-flow.md)：

1. **`tb_cue_words` 跨 tenant**：实际 45 条 `tenant_id ≠ 0`（不只 cueType=6 的 1 条），更确认 wiki `ai_domain.md` §六.1 描述不全
2. **askType 缺位**：wiki 仅列 16 个枚举值，未提**只有 11 种走 tb_cue_words，5 种走 systemKv，1 种是死代码** — 这是 askType 体系的关键信息，建议补到 wiki `ai_domain.md` §四
3. **5 个非 tb_cue_words 字段语义**：`account_type / sync_scene / apply_to / scope / scene` 在 wiki / 代码注释中均无定义 — 建议补到 `wiki/data/words_data.md`（`tb_cue_words` 字段表）

---

## 6. 待澄清（升 G3.3 / G3.4 / G3.5）

| # | 问题 | 受众 |
|---|---|---|
| G3.3 | `account_type` 0/1 实际语义是什么？是否抖音 vs 快手账号变体？ | PM + 开发 |
| G3.4 | `EXTRACT_VIDEO` (askType=10) 是未上线功能还是已废弃？ | PM + 开发 |
| G3.5 | `scene` 字段 7 条 =1 的实际用途？`outline` 字段语义？ | 开发 |

---

## Source Material

- 用户提供的 `tb_cue_words` 全量 SELECT 结果（执行结果3.txt + 执行结果4.txt，共 1314 行 × 18 列）
- 代码 grep：`AnchorUrlLogicImpl.java:978, 1021` / `AiRelatedLogicImpl.java:731-783` / `ConversationBll.java:848` / `AnchorVideoLogicImpl.java:1415`
- 父报告 §2.1 16 askType 枚举表

---

**报告完。本伴生文档不进一步迭代 — G3.3 / G3.4 / G3.5 跟进结果回填父报告。**
