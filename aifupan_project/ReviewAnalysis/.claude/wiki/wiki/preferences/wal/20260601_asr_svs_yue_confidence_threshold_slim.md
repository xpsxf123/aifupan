---
date: 2026-06-01
feature: asr_svs_yue_confidence_threshold
type: slim
---

# Slim WAL — 粤语 ConfidenceThreshold 0.30 → 0.20 动态化

`Asr/Local/SenseVoiceSmallEngine.cs` 把单常量 `ConfidenceThreshold = 0.30f` 拆为 `DefaultConfidenceThreshold (0.30) + LanguageConfidenceThresholds dict { 7: 0.20 }` + `GetConfidenceThreshold(int langId)` helper；`GreedyDecode` 签名加 `int languageId` 形参，调用点（RecognizeAsync line 213）直接传 `languageId`。粤语声调密集 / 入声 / 塞音过渡帧 logits 分布平坦，0.30 阈值会把大量正常发音帧打成 blank，造成段内 5–6s 静默；0.20 让粤语帧通过率提高。BGM 幻觉风险由下游 chunk-level FALLBACK（Task E 已下调 collapsed_ratio=0.90）兜底。日志加 `langId` 字段便于复盘。EPIC '粤语 5–6s 无推理修复' Task C，前序 Task E commit `9bc9bf4d`，后续 Task D（CTC 折叠保护）。

[Confidence: HIGH]
[Evidence: Asr/Local/SenseVoiceSmallEngine.cs:501-511, run_id=20260601_222934_asr_svs_yue_confidence_threshold]
