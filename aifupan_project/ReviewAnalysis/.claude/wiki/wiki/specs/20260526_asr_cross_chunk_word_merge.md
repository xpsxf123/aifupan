spec_mode: SLIM

# Change Summary
- What changed: `AsrUtils.RunLocalEngineAsync` 在 Sort 后新增跨段 Word 拼合（修复 59s 硬切导致单字被切成两半，如 "好家"+"伙" → "好家伙"）。
- Why: 候选 6 / D — CPU 友好环境下唯一零成本的质量优化（前 5 项已实施，CIF/滤镜/阈值/日志/置信度门）。

# Scope of Change
- `Asr/AsrUtils.cs` —— 新增 `TryMergeChunkBoundaries` / `IsAllCjk` 私有方法 + 3 const；`RunLocalEngineAsync` 拆分循环以插入 1 次调用

# Risk & Rollback
- Why LOW: 单文件单点改动；5 层保守启发式（时间边界 / 纯 CJK / 句末标点 / 长度上限 / Count 检查）；BGM 长歌词不可能命中；回退仅需删除 1 行调用。
- Rollback: `git checkout HEAD -- Asr/AsrUtils.cs`

# Verification & Evidence
- Mac 不可编译；csharp-code-review 11 项 PASS（1 处 ⚠️ ASRWordEntity.Word 修改属于已有规则的语义级破例，本 spec 记录边界）
- Windows 端 5 类样本 A/B（per [短视频验证规则](../preferences/wal/20260526_asr_short_video_validation_rules.md)）：
  1. 主播游戏直播 → 看 `[ASR] 跨段拼合: "xxx"` 日志出现次数 + 拼合质量
  2. 用户清晰人声 → 边界字（如分句句首/末）正确拼合
  3. 短视频 + BGM + 人声 → BGM 歌词不被误拼（保守阈值生效）
  4. 短视频 + 纯 AI 配音 → AI 解说连贯保留
  5. 短视频 + 字幕同步 → 短句末尾字归位
  6. **短视频 + 纯 BGM 含歌词**（per BGM 保留策略） → 长歌词不被误合并
- 量化指标：日志中 `[ASR] 跨段拼合共 X 处`，X 占总词数 < 5% 是健康（不应大量触发，否则说明阈值太宽）
- 失败模式：若 X > 10% 或出现明显错误拼合 → 收紧阈值（`MaxMergedCjkLen 4→3` 或 `ChunkBoundaryWindowMs 200→100`）
