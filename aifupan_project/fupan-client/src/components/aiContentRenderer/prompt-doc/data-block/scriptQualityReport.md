## type=scriptQualityReport

适用：话术质检报告类 JSON 数据（用于“去重合并/结构化展示/可缓存可渲染”）。

渲染方式：

- 当 `aifupan-data-block` 的 `type="scriptQualityReport"` 且 `visible=true` 时，前端会将解析出的 JSON 对象交给 `ScriptQualityReportContent` 进行结构化渲染（meta 声明 + summary 计数 + breakdown 文案 + 分维度明细）。

## 1. 输出方式（提示词应该怎么让模型输出）

推荐直接输出如下形式（注意：**data-block 内部是纯 JSON，不要 ` ```json `**）：

~~~text
<aifupan-data-block type="scriptQualityReport" visible="true" cacheKey="scriptQualityReport">
{
  "meta": {
    "statement": "以下是【爱复盘标准运营智能体1.0】AI拆解分析后的结果。您也可以找【爱复盘产品顾问】了解如何 <font>【定制运营分析智能体】</font>，以符合您实际的业务场景需求。",
    "roomName": "...",
    "duration": "...",
    "date": "...",
    "coverage": "...",
    "category": "..."
  },
  "summary": {
    "crashCount": 0,
    "slackCount": 0,
    "brandDamageCount": 0,
    "afterSalesCount": 0,
    "totalNegativeCount": 0,
    "totalSentencesAnalyzed": 0
  },
  "breakdown": {
    "crash": { "high": 0, "medium": 0, "low": 0, "text": "本场直播未发现该类问题话术" },
    "slack": { "high": 0, "medium": 0, "low": 0, "text": "本场直播未发现该类问题话术" },
    "brandDamage": { "high": 0, "medium": 0, "low": 0, "text": "本场直播未发现该类问题话术" },
    "afterSales": { "high": 0, "medium": 0, "low": 0, "text": "本场直播未发现该类问题话术" }
  },
  "details": {
    "crash": [],
    "slack": [],
    "brandDamage": [],
    "afterSales": []
  }
}
</aifupan-data-block>
~~~

补充说明：

- data-block 内部允许使用 ` ```json ` 代码块，但本 type 的推荐提示词规范要求“只输出严格 JSON”，因此默认不使用 fenced code block。
- 由于 `type=scriptQualityReport` 的内容类型要求为 JSON，未显式提供 `contentType` 时前端会默认按 `json` 解析；若你显式设置 `contentType="text"` 或使用 ` ```text ` 包裹，会导致不渲染（仅缓存）。
- **禁止在 data-block 外输出任何解释性文字**；否则会污染最终渲染内容。

## 2. 核心执行规则（必须严格执行）

1. 绝对禁止修改任何单条问题话术的原始内容：明细字段 `timeRange`、`originalText`、`issue`、`suggestion` 的值必须一字不差地来源于原报告；`severity` 仅做"高→high、中→medium、低→low"的等级映射，不改变判定。
2. 绝对禁止添加任何概要总结、整体评价或评分：严格遵循原提示词"输出完分维度分析后即结束"的要求。禁止输出任何形式的整体评估文字、质量评分、汇总型改进建议等原报告没有的内容（不得有 `summaryHtml`、`overallScore`、顶层 `improvementSuggestions` 等字段）。
3. 绝对禁止添加其它原报告中没有的内容：包括校验说明、不一致标注、额外分析。
4. 绝对禁止删除任何不重复的问题话术记录：所有在3份报告中唯一出现的问题话术必须完整保留为一条 JSON 记录。
5. 全程不进行任何形式的校验工作：不对比报告间的判定差异，不标注任何分类/风险等级不一致，不解决任何冲突，仅做完全重复记录的去重合并。
6. 开头标准声明与直播间基础信息照原样保留：开头声明一字不差填入 `meta.statement`；基础信息三份一致直接使用，不一致优先取第1份报告的内容，不做任何标注。
7. 最终只输出符合 Schema 的严格 JSON（位于 data-block 内部）：禁止任何前缀、后缀、解释性文字、Markdown 代码块标记（不要 ```json）、注释或差异说明。

## 3. 去重规则（唯一执行标准，仅针对完全相同的记录）

1. 以“时间节点(HH:MM:SS) + 话术原文”作为判断重复的唯一标识。
2. 若多条记录的时间节点完全相同且话术原文完全相同，视为重复记录，仅保留其中任意 1 份完整内容。
3. 若时间节点不同或话术原文有任何差异，均视为不同记录，全部保留。
4. 若同一时间节点 + 同一话术原文在不同报告中被归为不同分类、判定不同风险等级或有不同的影响分析/优化建议，均视为不同记录，全部保留为多条 JSON 记录，不做任何合并或取舍。

## 4. Schema（字段说明 + 映射规则）

### 4.1 meta（开头声明 + 直播间基础信息）

| 字段 | 类型 | 说明 |
|------|------|------|
| `meta.statement` | string | 原报告开头标准声明（一字不差，允许包含 `<font>` 等标记） |
| `meta.roomName` | string | 直播间名称 |
| `meta.duration` | string | 直播时长 |
| `meta.date` | string | 直播日期 |
| `meta.coverage` | string | 分析覆盖范围 |
| `meta.category` | string | 核心直播品类 |

### 4.2 summary（去重后计数）

| 字段 | 类型 | 说明 |
|------|------|------|
| `summary.crashCount` | number | 崩盘话术条数（必须等于 `details.crash.length`） |
| `summary.slackCount` | number | 摸鱼话术条数（必须等于 `details.slack.length`） |
| `summary.brandDamageCount` | number | 有损品牌条数（必须等于 `details.brandDamage.length`） |
| `summary.afterSalesCount` | number | 增加售后条数（必须等于 `details.afterSales.length`） |
| `summary.totalNegativeCount` | number | 四类之和 |
| `summary.totalSentencesAnalyzed` | number | 本场分析的话术总句数（来源优先级见核心规则第 6 条） |

### 4.3 breakdown（分维度整体判定）

`breakdown.crash/slack/brandDamage/afterSales` 均为同一结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| `high` | number | 该类去重后记录中 `severity="high"` 的条数 |
| `medium` | number | 该类去重后记录中 `severity="medium"` 的条数 |
| `low` | number | 该类去重后记录中 `severity="low"` 的条数 |
| `text` | string | 按规定句式生成的“整体判定”一句话；若无记录固定为「本场直播未发现该类问题话术」 |

### 4.4 details（明细字段映射）

分类归属映射：

- 崩盘话术 → `details.crash[]`
- 摸鱼话术 → `details.slack[]`
- 有损品牌形象话术 → `details.brandDamage[]`
- 增加售后压力话术 → `details.afterSales[]`

每条明细 item（必须保留 5 列全部信息）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `timeRange` | string | 时间节点（HH:MM:SS，一字不差） |
| `originalText` | string | 话术原文（一字不差） |
| `severity` | string | 风险等级映射：`high`/`medium`/`low`（仅映射，不改变判定） |
| `issue` | string | 实际影响分析（一字不差） |
| `suggestion` | string | 优化建议（一字不差） |

每个分类数组内处理流程：

1. 收集 3 份报告中归为该类的所有记录
2. 按“时间节点 + 话术原文”去重（仅去除完全重复记录）
3. 按 `timeRange`（HH:MM:SS）升序排列
4. 若该类无任何记录，输出 `[]`

## 5. 前端解析/渲染方式（数据块标签内如何解析）

解析与挂载链路：

1. `AiContentRenderer` 在渲染前会预处理 `<aifupan-data-block ...>...</aifupan-data-block>`
2. data-block 内部内容会被当作 JSON 文本解析（支持“纯 JSON”或 ` ```json ` 代码块两种写法）
3. 当 `type="scriptQualityReport"` 且 `visible=true`：
   - 会动态挂载 `ScriptQualityReportContent` 渲染
4. 同时该 data-block 会被缓存（按 `cacheKey` 存储整个 block 对象，包含 `raw/data/type/visible`）

渲染字段使用说明（当前前端渲染会用到的字段）：

- `meta.statement`：渲染为顶部声明（会做基础 HTML 清洗）
- `summary.*`：渲染为计数卡片
- `breakdown.*.text`：渲染为分维度整体判定文字
- `details.*[]`：渲染为分维度明细列表（展示 `timeRange/severity/issue/originalText/suggestion`）
