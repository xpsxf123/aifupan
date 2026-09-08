---
date: 2026-06-01
feature: asr_vad_diagnostic_logging
type: slim
---

# Slim WAL — VAD 路由诊断日志收尾（EPIC 完结）

`Utils/AudioUtils.cs` `CanUseSmartSlicing` 在路由决策日志中加入 `VadEngine.GetType().Name`（null-safe：先判 `== null` 后取类型名），新增"启用智能切片"路径的成功 log。便于未来真 `FsmnVadEngine` 接入后从生产日志判断当前实例。

VAD 接口架构（`IVadEngine` / `FsmnVadEngineStub` / `VadSegment` / `SmartSlicer` / `AudioUtils.VadEngine` 静态挂载点 / `AsrVadEnabled` App.config 开关）在历史 run 已全部就位，Stub 返回 `IsReady=false` → `CanUseSmartSlicing` 返回 false → 始终降级 `SlicingAudioLegacy` 硬切。真 ONNX FsmnVadEngine 接入留作未来独立 run（模型源用 [[feedback_asr_vad_model_china_access]] 推荐的 ModelScope `iic/speech_fsmn_vad_zh-cn-16k-common-onnx`）。

**EPIC '粤语 5–6s 无推理修复' 完结**。5 task 完整序列 E→C→D→A→B 提交链：
- Task E `9bc9bf4d`: FALLBACK collapsed_ratio 0.95 → 0.90
- Task C `5a0d65db`: 粤语 SVS ConfidenceThreshold 动态化 0.30 → 0.20
- Task D `d4d83713`: CTC 折叠保护（> 180ms 重新放行）
- Task A `d3f34583`: IsLowQuality rule3 internal_gap（>5s 静默触发整 chunk FALLBACK）
- Task B (this run): VAD 路由诊断日志收尾

四路防线：(a) 帧级置信度门按语种放宽 (Task C) → (b) CTC 折叠保护 (Task D) 让长持音不被压成单字 → (c) 段内静默检测触发 chunk-level FALLBACK (Task A) → (d) FALLBACK 阈值整体下调 (Task E) 让边界 chunk 更易走 Tencent。

[Confidence: HIGH]
[Evidence: Utils/AudioUtils.cs:48-69, run_id=20260601_223900_asr_vad_diagnostic_logging, EPIC commit chain 9bc9bf4d/5a0d65db/d4d83713/d3f34583]
