# Domain WAL: 本地 ASR 引擎 (SenseVoiceSmall)

**Date**: 2026-05-14
**Source**: Phase 1-3 implementation, OpenSpec `20260514_asr_local_engine_openspec.md`
**Type**: domain

## New Concepts

| Term | Definition | Related |
|---|---|---|
| SenseVoiceSmall | 阿里达摩院 SenseVoiceSmall ONNX 量化模型 (model_quant.onnx, 241MB)，本地运行语音识别，无需联网 | FunASR, ONNX Runtime |
| FBank + LFR | 音频特征提取管道：16kHz PCM → 80维 Mel 滤波器组 (Hamming窗, 25ms窗长, 10ms移位) → LFR 拼接 (m=7, n=6) → 560维特征向量 | WavFrontend, FFT |
| ModelManager | 模型文件生命周期管理：下载 (HTTP Range 续传)、SHA256 完整性校验、本地 manifest.json、.part 临时文件原子替换 | manifest.json, svs_model_config.json |
| GpuProbe | GPU 硬件加速探测：CUDA > DirectML > CPU 优先级，通过 ONNX Runtime provider API 探测可用性 | ONNX Runtime, SessionOptions |
| Engine Pipeline | 多引擎编排管道：`"svs:auto:withitn\|tencent"` 语法解析为 CompositeASREngine，按权重降序尝试 | AsrEngineFactory, IASREngine |
| svs_model_config.json | 模型下载配置：`%LocalAppData%/ReviewAnalysis/Config/svs_model_config.json`，含 download_url 和 version 字段 | ModelManager, LoadDownloadUrl() |
| manifest.json | 本地模型文件清单 + SHA256 校验和，下载完成后由本地计算生成，后续 IsModelReady() 逐文件校验 | ModelManager, ComputeSha256() |
| 错误码 702/703/704 | 702=模型未就绪/词汇表加载失败, 703=音频解码/特征提取失败, 704=音频超60秒限制 | ASRResultEntity, checkAsrError() |

## State Transitions

- **模型就绪状态**: IsModelReady() → false 触发 EnsureDownloadStarted() → DownloadAsync() → 写入 manifest.json → IsModelReady() → true
- **引擎降级**: SenseVoiceSmall 失败 (code≠0) → CompositeASREngine 尝试 TencentASREngine → 全部失败返回 code=500
- **GPU 探测**: GpuProbe 构造时一次性探测，结果缓存于 BestProvider，OfflineModel 使用探测结果配置 SessionOptions

## Constraints

- ONNX 推理必须串行 (static SemaphoreSlim(1))
- 音频时长 ≤ 60 秒
- 模型文件无远端 manifest，SHA256 由本地下载后计算
- 下载 URL 可从 svs_model_config.json 覆盖，默认指向 ModelScope
