---
date: 2026-06-01
feature: asr_svs_ctc_collapse_protection
type: slim
---

# Slim WAL — CTC 折叠保护：同 token > 180ms 重新放行

`Asr/Local/SenseVoiceSmallEngine.cs` `FilterBlanksAndMetas` 在原 CTC 折叠状态机基础上加 `sameRunLength` 计数器与 `MaxCollapseFrames = 3` 私有常量。逻辑：同 token 连续命中 ≤ 3 帧（180ms）保持原 CTC 折叠语义；> 3 帧时认定为长持音/拖音，重置 run 为 1 并 fall-through 到 blank/meta/emit 链路，每 ~180ms 放行一份。覆盖场景：粤语长元音 / 拖音被压成单字导致 5s 持音 → 1 个 word entry，UI 音字同步僵硬。trace 验证 "A blank A" → 2 个 A（CTC 标准保留）、"AA" → 1 个 A、7 帧 "AAAAAAA" → 3 个 A（0/3/6 帧位）。BGM 误识风险由 Task E `CompositeASREngine collapsed_ratio=0.90` 兜底。EPIC '粤语 5–6s 无推理修复' Task D，前序 commits `9bc9bf4d` (E) `5a0d65db` (C)。

[Confidence: HIGH]
[Evidence: Asr/Local/SenseVoiceSmallEngine.cs:642-682, run_id=20260601_223324_asr_svs_ctc_collapse_protection]
