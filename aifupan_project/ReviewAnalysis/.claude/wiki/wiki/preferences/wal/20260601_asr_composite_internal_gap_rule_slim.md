---
date: 2026-06-01
feature: asr_composite_internal_gap_rule
type: slim
status: SUPERSEDED
superseded_by: 20260601_asr_revert_internal_gap_lower_collapse_085_slim
superseded_at: 2026-06-01
---

> **⚠️ SUPERSEDED 2026-06-01**：本 WAL 描述的 rule3 (WordList 间隙判定) 在用户 review 后撤销。
> 根因：WordList 间隙是输出层推断，无法区分"模型识别失败"与"真没人说话"，对真静默场景产生 false positive
> 与无意义 Tencent API 调用。撤销 + 改为下调事实型 rule1 阈值，详见 [[20260601_asr_revert_internal_gap_lower_collapse_085_slim]]。
> 本文件保留作为"为什么不能这样做"的反面教材。

# Slim WAL — IsLowQuality rule3 internal_gap（段内长静默触发整 chunk FALLBACK）[SUPERSEDED]

`Asr/CompositeASREngine.cs` IsLowQuality 加第 3 条规则 `internal_gap`：新增 `MaxInternalGapMs = 5000` 常量与 `HasLongInternalGap(ASRResultEntity)` 私有 helper，扫描 WordList 首词前 + 相邻词之间 + 末词到 chunkDuration 的最大间隙，> 5000ms 时返回 true 触发整 chunk Tencent fallback。DetermineRuleName 同步加 "internal_gap" 分支（在 collapsed_ratio 之后、char_density 之前判定）。直接对症用户报告："粤语测试 5–6s 无推理 + 无 FALLBACK" — 因为 rule1/rule2 都是 chunk 级聚合，对"前段静默 + 后段正常说话"的混合 chunk 不敏感。**Scope reduction**：本 run 仅做检测触发整 chunk FALLBACK，**没有**做真正的子区间 Tencent 音频切片 / WordList 时间戳合并（那是未来 EPIC 独立 task）。末词 EndTime 已被 SVS 延展到 chunkDuration，tail 检查自然为 0；首词前 5s+ 静默是粤语 badcase 主场景。EPIC '粤语 5–6s 无推理修复' Task A，前序 commits `9bc9bf4d` (E) `5a0d65db` (C) `d4d83713` (D)。

[Confidence: HIGH]
[Evidence: Asr/CompositeASREngine.cs:27, Asr/CompositeASREngine.cs:85-117, Asr/CompositeASREngine.cs:162-168, run_id=20260601_223646_asr_composite_internal_gap_rule]
