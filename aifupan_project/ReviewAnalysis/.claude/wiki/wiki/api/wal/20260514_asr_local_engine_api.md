# API WAL: 本地 ASR 引擎集成

**Date**: 2026-05-14
**Source**: Phase 2 implementation
**Type**: api

## New Interfaces

### IASREngine (`Asr/IASREngine.cs`)
```csharp
public interface IASREngine
{
    Task<Dictionary<string, object>> RecognizeAsync(FileInfo audioFile, string engSerViceType);
}
```
统一引擎接口，返回格式: `{"code": int, "data": List<ASRResultEntity>}`

### Implementations

| Class | File | Role |
|---|---|---|
| SenseVoiceSmallEngine | `Asr/Local/SenseVoiceSmallEngine.cs` | 本地 ONNX 推理引擎 |
| TencentASREngine | `Asr/TencentASREngine.cs` | 腾讯云 ASR 适配器 (包装 MyCallableTask) |
| CompositeASREngine | `Asr/CompositeASREngine.cs` | 权重降级编排器 |

### AsrEngineFactory (`Asr/AsrEngineFactory.cs`)
预创建引擎实例池，`Build(engSerViceType)` 解析管道语法:
- `"16k_zh"` → TencentASREngine
- `"svs:auto:withitn"` → SenseVoiceSmallEngine
- `"svs:zh:withitn|tencent"` → CompositeASREngine(SVS[10] + Tencent[7])
- `""` 或未指定 → `"svs:auto:withitn|tencent"` (本地优先, 云端降级)

## Modified Callers

### AsrUtils.AsrByDirectoryPath()
- 移除直接 `new MyCallableTask()` + `StartAsTask()` 调用
- 改为 `_engineFactory.Value.Build(engSerViceType).RecognizeAsync(file, engSerViceType)`
- `token` 参数保留签名但不使用 (向后兼容)
- 默认参数 `engSerViceType` 从 `"16k_zh"` 改为 `"svs:auto:withitn|tencent"`

### AnalysisUtils.checkAsrError()
- 新增错误码映射: 702 → "模型文件未就绪，请先下载模型", 703 → "音频解码失败", 704 → "音频超过60秒限制"

## New Files (Phase 1-3)

| File | Lines | Purpose |
|---|---|---|
| `Asr/Local/WavFrontend.cs` | ~280 | FBank + LFR 特征提取 (纯 C#) |
| `Asr/Local/OfflineModel.cs` | ~30 | ONNX Session 管理 (IDisposable) |
| `Asr/Local/SenseVoiceSmallEngine.cs` | ~560 | 完整推理管道 |
| `Asr/Local/ModelManager.cs` | ~270 | 模型下载 + SHA256 + manifest |
| `Asr/Local/GpuProbe.cs` | ~60 | GPU 硬件探测 |
| `Asr/IASREngine.cs` | ~30 | 引擎接口 |
| `Asr/TencentASREngine.cs` | ~55 | 腾讯云适配器 |
| `Asr/CompositeASREngine.cs` | ~55 | 降级编排 |
| `Asr/AsrEngineFactory.cs` | ~80 | 工厂 + 管道解析 |
