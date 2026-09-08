# AI Agent 检索接口 —— 待优化项（索引 / 全文检索）

> **状态：待排期，未实施。** 本文档只做记录与方案，下述 DDL / 代码改造均**未执行**。
> 关联接口文档：[`ai-agent-replay-openapi.md`](./ai-agent-replay-openapi.md)。
> 关联代码：`AiAgentInfluencerVideoServiceImpl`、`VideoInfoMapper.xml`、`VideoInfluencerInfoMapper.xml`。

---

## 背景

达人短视频 / 全库检索 / 爆款系列接口改为 **keyset 游标流式分页**后：

- **准确性已保证**：keyset 用 `(排序列, id)` 复合定位，是严格全序，"取前 N"即全库真 top-N，无重复/漏行（比 offset 更准）。
- **性能取决于索引**，且有两个结构性瓶颈：
  1. keyset 的 `ORDER BY 排序列, id` + 范围过滤，需要**排序列上的复合索引**才能走索引区间扫描、`LIMIT` 早停；否则全表扫描 + filesort。
  2. `keyword` / `#标签` 用 `LIKE '%..%'` **前导通配，永远走不了普通 B-Tree 索引** → 大表全表扫描。这是文本检索"慢"的根因，准确性不受影响。

本文档列出为"既快又准"需要补的**索引（P0）**与**全文检索改造（P1）**。

> **实施前置**：先 `SHOW INDEX FROM <表>` 核对现状，避免与既有索引重复；大表用 online DDL（gh-ost / pt-osc）。

---

## 一、索引（P0 · 纯 DDL，可先上，不改代码）

| 表 | 建议索引 | 支撑的查询 / 接口 | 备注 |
|---|---|---|---|
| `tb_video_info` | `idx_like_id (like_count, id)` | 全库短视频搜索按点赞排序（§6.15 sortField=1）、爆款视频按点赞（§6.19） | keyset 主力，最常用 |
| `tb_video_info` | `idx_publish_id (publish_time, id)` | 按发布时间排序（sortField=5）+ 发布时间范围过滤 | 排序 + 范围双用 |
| `tb_video_info` | `idx_comment_id (comment_count, id)` / `idx_share_id (share_count, id)` / `idx_collect_id (collect_count, id)` | 对应指标排序（sortField=2/3/4） | **按实际用量再加**，非高频排序维度可暂缓 |
| `tb_video_info` | `idx_author (author_id)` | 达人视频过滤（§6.15 influencerIdList）、租户已采集 count/stats（§6.11/§6.14 的 `author_id IN`） | `author_id` 为字符串（= 达人 id） |
| `tb_video_influencer_info` | `idx_followers_id (followers_count, id)`、`idx_like_id (like_count, id)` | 全库达人搜索排序（§6.17 sortField=0/1） | keyset |
| `tb_video_hot_search` | `idx_videocount_id (video_count, id)`、`idx_sync_id (last_sync_time, id)` | 爆款关键词排序（§6.18 sortField=0/1）+ 同步时间范围 | keyset |
| ~~`tb_video_hot_search_video`~~ | ~~`idx_search_sort_video (search_id, sort_order, video_id)`~~ | 爆款视频默认排序（§6.19 sortField=0）+ §6.21 选题拆解样本扫描 | **✅ 无需新增（2026-08-18 核对线上 DDL）** —— 线上已有 `idx_snapshot_query (search_id, is_deleted, sort_order)`，形态比本条建议更好（多了 `is_deleted` 过滤列）。两个查询的 `WHERE hs.is_deleted=0 AND hs.search_id=?` 正好命中最左两列，走索引区间扫描。详见下方「已覆盖」小节 |
| `tb_video_user_video` | `idx_tenant_video (tenant_id, is_deleted, video_id)`（子账号常用则 `(tenant_id, user_id, is_deleted, video_id)`） | 租户已采集视频的 count/stats/list 半连接（§6.11/§6.12/§6.14） | 命中租户隔离 + video_id 半连接 |
| `tb_video_influencer_search_snapshot` | `idx_tenant_time (tenant_id, is_deleted, search_time)` | 达人列表租户过滤 + 采集时间范围（§6.11） | |
| `tb_video_influencer_search_relation` | `idx_snapshot_inf (snapshot_id, influencer_id, is_deleted)` | 达人列表 snapshot⋈relation 取 distinct influencer_id（§6.11） | 覆盖索引，避免回表 |

> 说明：`tb_video_info` / `tb_video_influencer_info` / `tb_video_hot_search` 为**无 tenant_id 的公共爬取全量表**，索引全局共享，优先级最高（全库检索直接压这几张表）。

### 已覆盖：`tb_video_hot_search_video`（2026-08-18 核对线上 DDL，无需 DDL）

线上现状：

```sql
KEY `idx_snapshot_query` (`search_id`, `is_deleted`, `sort_order`) USING BTREE
```

| 查询 | 命中情况 | 结论 |
|---|---|---|
| §6.21 选题拆解取样本（`listHotSearchClipMetrics`）：`WHERE hs.is_deleted=0 AND hs.search_id=?` | 命中最左两列 `(search_id, is_deleted)`，索引区间扫描；JOIN `tb_video_info` 走主键 | **完全够用**。排序 `(赞+评+享+藏) DESC` 是表达式，任何索引都救不了、必然 filesort，但参与排序的行数已被索引压到「该选题下的行数」，且 `LIMIT 20000` 兜底 |
| §6.19 爆款视频列表默认排序：`WHERE hs.is_deleted=0 AND hs.search_id=? ORDER BY hs.sort_order, hs.video_id` | 前三列全命中，`sort_order` 已在索引序 | 仅剩 `video_id` 这个并列打破键不在索引里，理论上仍有一次 filesort，但 `LIMIT pageSize+1 ≤ 201`、且已在 `search_id` 分区内，代价可忽略 |

**若将来确实要消掉 §6.19 那次残余 filesort**，正确形态是**扩展现有索引**为 `(search_id, is_deleted, sort_order, video_id)` 并删掉旧的，而不是新建本文档原先建议的 `(search_id, sort_order, video_id)` —— 后者丢了 `is_deleted` 过滤列，是退步。当前不建议做，收益不抵大表 DDL 成本。

---

## 二、全文检索（P1 · 需配套改代码，解决 `LIKE '%..%'` 瓶颈）

**问题**：`keyword` / `#标签` 目前是 `LIKE '%关键词%'`，前导通配符无法走索引，全库大表上慢查询风险高。

**方案**：对文本列建 **FULLTEXT 索引**并改用 `MATCH ... AGAINST`。

| 表 / 列 | 用于接口 | 改造点 |
|---|---|---|
| `tb_video_info(title, video_description)` FULLTEXT | §6.15 全库短视频搜索 `keyword`、§6.19 爆款视频 `keyword` | `VideoInfoMapper.xml` 的 `hotSearchVideoFilter` 与 `globalVideoSearch` 的关键词条件从 `LIKE` 改 `MATCH(title, video_description) AGAINST(#{keyword} IN BOOLEAN MODE)` |
| `tb_video_influencer_info(nickname, platform_account)` FULLTEXT | §6.17 全库达人搜索 `keyword` | 昵称/账号关键词从 `LIKE` 改 `MATCH ... AGAINST` |

**注意事项：**

1. **中文必须用 ngram 分词器**：`FULLTEXT ... WITH PARSER ngram`；确认 `ngram_token_size`（默认 2）符合关键词粒度。
2. **`#标签`（tagList）不适合直接 FULLTEXT**：`#` 会被分词器切掉。两种落地方式（择一，后续再定）：
   - 抽取标题里的 `#xxx` 到独立 `tags` 列（或标签关联表）再建索引 / FULLTEXT；
   - 或标签量不大时，维持 `title LIKE '%#标签%'`（前导通配，仅在已用 FULLTEXT 大幅缩小结果集后再叠加，代价可控）。
3. **`MATCH AGAINST` 与 keyset 排序的关系**：默认相关度排序与我们的"按指标排序"是两套。若要"关键词命中 + 按点赞排序"，keyword 走 FULLTEXT 做**过滤**（BOOLEAN MODE，不取 relevance 排序），排序仍用指标列 keyset —— 两者可共存，但要 EXPLAIN 确认优化器选对索引（FULLTEXT 过滤 vs 指标索引排序，通常二选一，取决于选择性）。
4. **大表建 FULLTEXT 成本高**：全量重建耗时、占空间，安排在低峰 + online 工具。
5. **替代方案**：文本检索量级/复杂度再上一个台阶时，考虑外接 **ES**，把全库短视频/达人做成检索索引，MySQL 只存明细。属更大改造，本文档不展开。

---

## 三、验收标准

- 对下列关键查询 `EXPLAIN`：
  - 全库短视频搜索（按点赞 / 发布时间排序，带 keyset 游标）→ 走 `idx_like_id` / `idx_publish_id`，**无 `Using filesort`**（keyset 已在索引序）。
  - 全库达人搜索、爆款关键词 → 走对应 keyset 复合索引。
  - 租户已采集 count/stats/list → `tb_video_user_video` 命中租户复合索引，无全表扫描。
- 文本检索接口改 FULLTEXT 后，`EXPLAIN` 的 `type` 为 `fulltext` 而非 `ALL`。
- 回归：keyset 翻页在补索引前后结果一致（准确性本就不依赖索引，索引只影响速度）。

---

## 四、优先级

| 项 | 优先级 | 依赖 | 说明 |
|---|---|---|---|
| 一、keyset / 过滤复合索引 | **P0** | 无（纯 DDL） | 上线即显著降低慢查询，代码零改动 |
| 二、FULLTEXT + `MATCH AGAINST` | **P1** | 改 mapper + ngram 配置 | 解决文本检索瓶颈，需代码配套 + 回归 |
| `#标签` 结构化 / ES | **P2** | 视数据量与需求 | 更大改造，按需评估 |
