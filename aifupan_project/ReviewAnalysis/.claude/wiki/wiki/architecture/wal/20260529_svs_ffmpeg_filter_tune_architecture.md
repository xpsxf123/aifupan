---
date: 2026-05-29
feature: svs_ffmpeg_filter_tune
type: architecture
run_id: 20260529_111437_svs_ffmpeg_filter_tune
related_specs:
  - .claude/runs/20260529_111437_svs_ffmpeg_filter_tune/openspec.md
related_adr: []
related_wal:
  - [[20260529_svs_ffmpeg_filter_tune_rules]]
---

# Architecture WAL — SVS Badcase 根因分析 + 路线 A 必要性论证（2026-05-29）

## Change Summary

基于路线 B 4 视频 smoke 实测（含 badcase 1 真实样本——花生酱酱粤语唱歌 232s），记录 SVS 三大 badcase 的架构层根因 + 路线 A（SVS 质量评估 + Tencent 自动降级）的必要性论证 + 路线 A 接入点 anchors。

## Impact

- 为路线 A 立项提供 architectural 依据：路线 A **不是可选优化**，是 badcase 1（CTC fundamental 行为）和 badcase 2（stereo→mono downmix）的唯一根治路径
- 路线 A 启动后须依据 [[20260529_svs_ffmpeg_filter_tune_rules]] §5 CTC collapsed/total 阈值表 + 本 WAL §4 接入点 anchors 直接定位修改面，无需重做 Phase 1 Explorer
- 路线 A 上线后须重新校准降级阈值（路线 B 滤镜改动可能改变 SVS 置信度分布）
- stereo→mono downmix 问题留给路线 A 在"SVS 质量低 + 原音频 stereo"场景下尝试分声道送 SVS

---

## 1. Badcase 1 根因 — SVS CTC 模型对非说话段的 fundamental 行为

**结论**：轻声/混响唱歌的低字符密度（0.4 char/s，collapsed/total=91-98%）不是 FFmpeg 滤镜参数问题，而是 SenseVoiceSmall 端到端 CTC 模型的 fundamental 行为——对非说话段（歌唱、BGM）大量输出 blank token。

**机理**：
- SVS 是端到端 CTC 解码，argmax 全帧，无 CIF 辅助峰
- 唱歌音频音高连续、共振峰不在日常语音分布范围内，模型大概率映射到 blank
- 这是 CTC 解码对"不认识的语音模式"的设计行为，不是参数退化

**推论**：路线 B（FFmpeg 滤镜调优）无法根治 badcase 1。根治路径需要在 SVS 输出侧加质量评估（路线 A），在唱歌段检测到 collapsed/total > 95% 时触发 Tencent 降级。

[Confidence: HIGH]
[Evidence: VERBATIM:"""视频3 花生酱酱粤语唱歌：字符密度 0.4 char/s, content/total=0.6%-3.6%, collapsed/total=91-98%"""]

---

## 2. Badcase 2 潜在源头 — FFmpeg -ac 1 强制 stereo→mono downmix

**结论**：多人说话字迹混乱（badcase 2）的根因之一是 `SlicingAudioLegacy` 对 stereo 源执行 `-ac 1` 强制 downmix（线性叠加左右声道）。

**机理**：若原视频"双人对话且左右分录"（主播左声道，连麦嘉宾右声道），downmix = 两路语音直接线性叠加，等同于强制制造"多人重叠"。SVS 作为单流模型，对此类叠加信号输出混乱。

**代码位置**：`Utils/AudioUtils.cs:150` 末端 `-ar 16000 -ac 1`（不在本任务 scope，focus_card 锁定 :150 单行字符串）。

**路线 A 后续可行方案**（不在本任务实施）：在 SVS 质量评估发现失败 + 原音频是 stereo 时，尝试分声道分别送 SVS，取置信度较高者；或保留 stereo 送支持多通道的 ASR 后端。

[Confidence: MEDIUM]
[Evidence: explore_report.md §6 CA-9, Utils/AudioUtils.cs:150]

---

## 3. 路线 A 必要性论证

**核心论据**：路线 B smoke 实测（4 视频）证明 FFmpeg 滤镜调优对常规口播无回归（4/4 ✅），但对 badcase 1（唱歌）改善有限（字符密度 0.4 char/s，滤镜调整无法改变 CTC 模型对歌唱信号的 blank 输出行为）。

| 场景 | 路线 B 效果 | 路线 A 需介入 |
|---|---|---|
| 常规口播 | 无回归，密度 1.9-3.5 char/s | 不需要 |
| 轻声/唱歌 | collapsed/total > 95%，无本质改善 | 必须：SVS 出口质量评估 + Tencent 降级 |
| 多人 stereo 分声道 | 无法在滤镜侧解决 | 必须：检测 stereo + 分声道路由 |

**结论**：路线 A 是必要项，不是可选优化。

[Confidence: HIGH]
[Evidence: VERBATIM:"""3 次 smoke 实测结论；badcase 1 字符密度 0.4 char/s"""]

---

## 4. 路线 A 接入点 anchors（供后续任务直接定位）

| 接入点 | 位置 | 用途 |
|---|---|---|
| SVS 推理出口 | `Asr/Local/SenseVoiceSmallEngine.cs:69` `RecognizeAsync` | 读置信度 / collapsed 比例，决定是否降级到 Tencent |
| 拼接前结果列表 | `Asr/AsrUtils.cs:92-116` `TryMergeChunkBoundaries` | Tencent 补段插入点，须兼容边界修复逻辑 |
| ShortVideo 配置串 | `ShortVideo/ShortVideoHandle.cs:571` `svs:auto:withitn|tencent` | 路线 A 通用降级策略上线前须确认是否被覆盖 |

**注意**：路线 B 的滤镜改动可能改变 SVS 置信度分布，路线 A 的降级阈值须在路线 B 完成后重新校准。

[Confidence: HIGH]
[Evidence: explore_report.md §7, openspec.md §5 补充说明]

---

## 5. 三大消费方一致性保证

`SlicingAudio` / `SlicingAudioLegacy` 是唯一入口，三大消费方均通过以下 caller 接入：

- `Bll/AnchorVideoBll.cs:629`（AnchorReplay）
- `Bll/UploadFileBll.cs:217`（UserUpload）
- `ShortVideo/ShortVideoHandle.cs:570-571`（ShortVideo）

路线 B 单点修改 `AudioUtils.cs:150` 滤镜字符串即对三条链路等效生效，无需分别适配。

[Confidence: HIGH]
[Evidence: explore_report.md §6 CA-4]
