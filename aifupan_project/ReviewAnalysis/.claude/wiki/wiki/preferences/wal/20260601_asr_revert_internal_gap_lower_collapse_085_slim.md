---
date: 2026-06-01
feature: asr_revert_internal_gap_lower_collapse_085
type: slim
supersedes: 20260601_asr_composite_internal_gap_rule_slim
---

# Slim WAL — 撤 rule3 internal_gap + CtcCollapseThreshold 0.90 → 0.85

`Asr/CompositeASREngine.cs` 撤销前序 commit `d3f34583` 引入的 rule3 internal_gap：移除 `MaxInternalGapMs` 常量、`HasLongInternalGap` helper、`IsLowQuality` 的 rule3 OR 项、`DetermineRuleName` 的 `"internal_gap"` 分支。同步把 `CtcCollapseThreshold` 由 0.90 下调到 0.85 作为兜底。

**设计原则确立**：FALLBACK 低质判定**必须基于事实 token 统计**（rule1 `CollapsedTokens/DecodedTokens`、rule2 字符密度），**不允许基于 WordList 间隙**等输出层时间推断量。原因：WordList 间隙无法区分"模型识别失败（应 fallback）"与"真没人说话（不应 fallback）"，对真静默 chunk 会产生 false positive，触发 Tencent 同样空响应，徒增 API 成本无实际改善。粤语 5–6s 无推理场景应靠**下调既有阈值**兜底，而非加输出层间隙规则。

阈值演进：0.95 (initial) → 0.90 (Task E `9bc9bf4d`) → 0.85 (this run)。生产 smoke 双峰分布（口播 0.05–0.30 / 唱歌 0.99）之间空挡较大，0.85 仍远高于口播上限，不误伤纯口播 chunk；同时把"高折叠但未到 0.90"的粤语边缘 chunk 拉进 FALLBACK 范围。

EPIC '粤语 5–6s 无推理修复' Task A 修正版，前序完整 commit 链：`9bc9bf4d` (E 0.95→0.90) `5a0d65db` (C yue 0.20) `d4d83713` (D CTC 折叠保护) `d3f34583` (A 误用 WordList-gap, 已撤) `0ba2389d` (B VAD 诊断) → (this) Task A 修正。

[Confidence: HIGH]
[Evidence: Asr/CompositeASREngine.cs:18-27, Asr/CompositeASREngine.cs:85-97, Asr/CompositeASREngine.cs:142-147, run_id=20260601_224515_asr_revert_internal_gap_lower_collapse_085]
