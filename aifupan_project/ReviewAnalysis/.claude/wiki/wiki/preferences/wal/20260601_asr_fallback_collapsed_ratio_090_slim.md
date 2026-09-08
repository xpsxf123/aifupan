---
date: 2026-06-01
feature: asr_fallback_collapsed_ratio_090
type: slim
---

# Slim WAL — FALLBACK collapsed_ratio 0.95 → 0.90

`Asr/CompositeASREngine.cs:22` `CtcCollapseThreshold` 由 `0.95` 降至 `0.90`，扩大 SVS chunk 级 FALLBACK 触发面，让"高折叠但未达 0.95"的边缘 chunk（粤语 / 半唱半说段）自动走 Tencent 复识别。Boss 已批准接受 Tencent 调用频率上升的成本。生产 smoke 双峰分布（0.05 口播 vs 0.99 唱歌）证明 0.95→0.90 主要影响中间过渡 chunk，不会把口播段误打。EPIC '粤语 5–6s 无推理修复' Task E（5 task 第 1 个），后续 Task C/D/A/B 处理 SVS 帧过滤/CTC 折叠/子区间 FALLBACK/VAD 接口。

[Confidence: HIGH]
[Evidence: Asr/CompositeASREngine.cs:22, run_id=20260601_222207_asr_fallback_collapsed_ratio_090]
