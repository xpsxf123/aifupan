# Research Companion — `tb_cue_words.cueType=6` 实际数据样本（G3）

**Date:** 2026-05-24
**Status:** 调研完成（基于 DB SELECT 结果，41 行 cueType=6 数据）
**Parent:** [20260524__ai-script-assistant-business-flow.md](./20260524__ai-script-assistant-business-flow.md) (G3 解决)
**数据源：** 用户提供的 `tb_cue_words WHERE cue_type=6` SELECT 结果（共 41 行）

---

## 1. 数据规模与分布

- **总 41 行**，全部 `cue_type=6` (AI_SCRIPT_ASSISTANT)
- **40 行系统级**（`tenant_id=0` + `account_type=0`）+ **1 行租户私有**（`tenant_id=4399667611416133632` + `account_type=1`）
- 字段 `scope / scene / apply_to / resource_type / sync_scene / outline` 在 cueType=6 场景下全为 0（**这些字段对话术助手无差异化**）

## 2. 系统级 prompt 按行业分组（4 个 trade）

| trade_id | prompt 条数 | 含子类型（按 sort 序） |
|---|---|---|
| `1`（默认 / 通用） | 12 | sort 1=塑品 / 2=逼单 / 3=互动 / 4=引导关注 / 5=导流 / 6=人设 / 7=观点举例 / 8=幽默 / 9=金句 / 10=迎新 / 11=钩子 / 12=黑话 |
| `4352179920231727104` | 12 | 与 trade=1 完全对齐 1:1（subtype + sort 编号都一致），疑似从 trade=1 fork |
| `4324266648077860864` | 9 | 缺 sort 2/4/11/12（无逼单 / 无引导关注 / 无钩子 / 无黑话）；sort 8=表达观点和举例 / 9=迎新 / 10=钩子（与 trade=1 错位） |
| `4352181997137821696` | 7 | **子类型集与上面 3 个完全不同**：sort 1=迎新 / 2=互动 / 3=引导关注 / 4=人设 / 5=**情绪价值** / 6=**刷礼物** / 7=**回怼观众**（"情绪价值/刷礼物/回怼" 是该行业独有） |

**关键洞察：sort 字段不是稳定子类型编号。** 它只是每个行业内部的排序号，**同一 sort 在不同行业可能代表完全不同的子类型**：
- trade=1 的 sort=5 = 导流话术
- trade=4352181997137821696 的 sort=5 = **情绪价值话术**

子类型语义只能从 `cue_word`（短标题）/ `problem`（prompt 文本）字段反推，**代码层面没有 enum 规范化**。

## 3. 字段语义校正（与 wiki / 推理不符的点）

| 字段 | 推断值 | 实际值 | 影响 |
|---|---|---|---|
| `cue_word` | "prompt 文本" | **短标题**（13-32 字符，如 "总结这个直播的引导加粉丝灯牌话术，输出表格"） | wiki / 代码命名误导 — `cue_word` 不是真正的 prompt 文本 |
| `problem` | "用户提问？" | **真正的 prompt 文本主体**（约 500-2000 字符的"你是一个直播话术总结专家..."完整指令） | `problem` 字段名极度误导 |
| `tb_cue_words` 边界 | "仅系统提示词" | **也可装租户私有 prompt**（41 行中有 1 行 tenant_id≠0 + account_type=1） | wiki `ai_domain.md` §六.1 "USER → tb_cust_prompt" 不完整 — 实际有跨表的可能（详见 §4） |
| `account_type` | 未在 ai_data.md 列出 | **0=系统 / 1=个人账户 prompt**（推断） | 文档遗漏字段；建议补到 wiki/words_data.md |
| `apply_to` / `sync_scene` / `outline` | 未确认含义 | 在 cueType=6 场景下全为 0 | 至少话术助手不依赖；其他 cueType 是否启用待查 |

## 4. 跨 wiki 偏差：`tb_cue_words` vs `tb_cust_prompt` 边界模糊

**wiki [`ai_domain.md` §六.1 "提示词来源回退"](../domain/ai_domain.md) 描述：**

> 1. `cueWordsType == null` → 先查系统 `CueWordsFeign`，找不到回退查用户 `tb_cust_prompt`
> 2. `cueWordsType == 0`（SYSTEM）→ 仅查系统
> 3. `cueWordsType == 1`（USER）→ 仅查 `tb_cust_prompt`

**SELECT 结果反驳：** `tb_cue_words` 自身就有 `tenant_id≠0` 的非系统级 prompt（row 41）。这意味着：
- 要么 `cueWordsType=0` 实际查询条件不只 `tenant_id=0`，可能含 `tenant_id IN (0, currentTenantId)`
- 要么 row 41 是**误植 / 历史数据迁移产物**（生产环境的脏数据）
- 要么 `tb_cue_words` 设计上就**允许租户级 prompt**，而 `tb_cust_prompt` 只是用户级私有，二者是不同隔离粒度

**待开发确认**（升 G3.1 子问题）：
- 查 `CueWordsBll.getCueWords(cueWordsId)` 实际 WHERE 子句是否过滤 `tenant_id`
- 查 row 41 是否实际被某个用户 / 客户端访问到
- 若设计如此，wiki 描述需补"`tb_cue_words` 含系统级 + 租户级双隔离"

## 5. `#{trade}` 占位符使用

`problem` 字段大量出现 `#{trade}` 占位符（与 `#{reason}` 同机制，由 `AiRelatedBll.getAiPromptWord2` 处理）：
- 出现位置：`"这个直播间属于#{trade}行业的直播间"`
- 替换源：`SensitiveWordsFeign.getTradeId(sourceType, sourceId)` 拿到行业 ID → 字典反查行业名称
- 与 `#{reason}` 的差异：`#{reason}` 是上下文锚定（前后 4 字符抓取上一轮 realContent），`#{trade}` 是简单字段替换

## 6. 数据回填到主报告 §2.2 产品语义

基于本数据，§2.2"话术助手产品语义"中**"推断的产品语义"**可由"待 PM 确认"升级为"**部分已确认**"：

| 维度 | 之前推断 | 数据确认 |
|---|---|---|
| 是什么 | 基于视频脚本 + 实时数据生成话术建议 | ✅ 确认 — `problem` 字段开头一律 "你是一个直播话术总结专家..." |
| 子能力 | 未细分 | **新增**：12 种通用子能力（塑品 / 逼单 / 互动 / 引导关注 / 导流 / 人设 / 观点 / 幽默 / 金句 / 迎新 / 钩子 / 黑话）+ 行业差异化扩展（情绪价值 / 刷礼物 / 回怼） |
| 输出形式 | 未确认 | **新增**：`problem` 中大量要求"输出格式：表格 / 不低于 N 字 / 不要省略" → 客户端 UI 预期是富文本表格 |
| 行业感知 | 未确认 | ✅ 确认 — `#{trade}` 占位符 + 行业级 prompt 分组 |
| 跨行业 prompt 可移植 | 未确认 | ❌ **不可** — sort 编号在不同 trade 间错位，子类型分布也不对齐 |

仍待 PM 确认（数据无法回答）：
- 是否计划把 12 种子能力做成 enum 规范化（移除 sort 错位风险）？
- 行业 4352181997137821696 的"情绪价值 / 刷礼物 / 回怼"是产品方向还是临时实验？
- 是否所有租户都看见所有 12 种？还是某些子能力按订阅等级解锁？

## 7. 推荐后续动作

| # | 行动 | 优先级 |
|---|---|---|
| 1 | 主报告 §2.2 把"推断的产品语义"标记为"**部分已确认 — 见 cue-words-sample.md §6**" | P1（已完成本文档时同步） |
| 2 | 让开发确认 G3.1（`tb_cue_words` 是否支持租户级隔离 + row 41 来源） | P1 |
| 3 | 让 PM 确认 G3.2（12 种子能力是否需要 enum 规范化、行业差异化是产品方向还是实验） | P1 |
| 4 | 修订 `ai_domain.md` §六.1 "提示词来源回退"加注 `tb_cue_words` 实际含非系统级数据 | P2（攒到 G4 wiki 修订时一起） |
| 5 | 补 `wiki/data/words_data.md` 字段表（`tb_cue_words.account_type / apply_to / sync_scene / outline` 缺漏） | P2 |

## Source Material

- 用户提供的 `tb_cue_words WHERE cue_type=6` SELECT 结果（`/Users/hehui/Downloads/执行结果3.txt`，41 行 × 18 列）
- 父报告 [`20260524__ai-script-assistant-business-flow.md`](./20260524__ai-script-assistant-business-flow.md) §2.2 推断的产品语义
- wiki [`ai_domain.md` §四 askType 枚举 / §六.1 提示词来源回退](../domain/ai_domain.md)

---

**报告完。本伴生文档不进一步迭代 — 后续 G3.1 / G3.2 跟进结果回填主报告。**
