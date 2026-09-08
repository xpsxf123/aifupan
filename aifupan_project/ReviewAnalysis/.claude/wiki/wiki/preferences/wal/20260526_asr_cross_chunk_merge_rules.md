# Rules WAL — 跨段 Word 拼合规则 + ASRWordEntity.Word 修改边界 (2026-05-26)

> 关联 spec: [cross-chunk word merge](../../specs/20260526_asr_cross_chunk_word_merge.md)
> 关联反模式: [asr_engine_pipeline §2.2 / §7.3 (Word 字段污染)](../../domain/asr_engine_pipeline.md)

## 新增规则

### [ASR] [SHOULD] AsrUtils 跨段拼合限制必须保守

**实施位置**：`AsrUtils.TryMergeChunkBoundaries`

**必须满足全部 5 个条件才合并**：

| # | 条件 | 阈值 const | 目的 |
|---|---|---|---|
| 1 | curr 末词 `EndTime ≥ chunkDur - 200ms` | `ChunkBoundaryWindowMs=200` | 仅边界字 |
| 2 | next 首词 `StartTime ≤ 200ms` | 同上 | 仅边界字 |
| 3 | curr 末词不以 `。？！，、.?!,` 结尾 | `SentencePuncts` | 句号已结束的不合并 |
| 4 | 两词都是纯 CJK（U+4E00..U+9FFF） | `IsAllCjk` | 不混 ASCII/英文 |
| 5 | 合并后长度 ≤ 4 字 | `MaxMergedCjkLen=4` | MergeWords 3 字 word + 1 字溢出空间 |

**禁止**：放宽任一阈值导致 BGM 长歌词被误拼。BGM 歌词受 [BGM 保留策略](./20260526_asr_bgm_preservation_rules.md) 保护，5 层过滤是其在 AsrUtils 层的具体落地。

---

### [ASR] [MUST] 明确 ASRWordEntity.Word 字段的修改边界

**背景**：[asr_engine_pipeline §2.2](../../domain/asr_engine_pipeline.md) 原文：
> 禁止在 AsrUtils 或下游代码中修改 ASRWordEntity.Word。格式化拼接使用本地变量，不污染实体字段。

**实际现状（2026-05-26 起精确定义）**：

| 修改类型 | 允许 | 示例 | 位置 |
|---|---|---|---|
| ❌ **格式化污染** | 禁止 | 追加尾随空格 `word + "  "` | 历史 bug，2026-05-15 修复 |
| ❌ **逐字标记** | 禁止 | 加 `<highlight>` HTML 标签 | 应在视图层做 |
| ✅ **语义内容合并** | 允许 | 跨段拼合 `"好" + "家伙" → "好家伙"` | `AsrUtils.TryMergeChunkBoundaries` |
| ✅ **去标签清理** | 允许 | `Regex.Replace(word, @"<\|[^|]*\|>", "")` | `SenseVoiceSmallEngine.BuildWord`、`AsrUtils.RebuildResultText` 已存在 |
| ⚠️ **现有非理想行为** | 兼容容忍 | `RebuildResultText` 给 ASCII 词追加空格 | 历史腾讯兼容写法，未来重构时改 |

**判定标准**：修改是否**保留或恢复原始声学意图**？
- 是 → 允许（拼合是恢复被切散的词）
- 否 → 禁止（污染、标记是叠加额外信息到内容字段）

## 关联

- [BGM 保留策略](./20260526_asr_bgm_preservation_rules.md) — 5 层过滤是其落地
- [短视频验证规则](./20260526_asr_short_video_validation_rules.md) — 必须用 6 类样本验证不误拼
- [诊断日志规约](./20260526_asr_diagnostics_and_threshold_rules.md) — `[ASR] 跨段拼合: ...` / `[ASR] 跨段拼合共 X 处` 是新增观测点
