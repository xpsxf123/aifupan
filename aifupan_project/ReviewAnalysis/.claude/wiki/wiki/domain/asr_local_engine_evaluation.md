# 本地 ASR 引擎集成可行性评估

本文档评估将 SenseVoiceSmall 本地 ONNX 模型作为腾讯云 ASR 替代方案的工程可行性与复杂度。

数据来源: 现有 `Asr/` 源码分析 + [SenseVoiceSmall 推理架构方案](asr_sensevoice_small.md)

---

## 1. 现有架构耦合度分析

当前 ASR 调用链：

```
AnchorVideoBll / UploadFileBll
  → AsrUtils.AsrByDirectoryPath(dir, token, engSerViceType)
    → MyCallableTask(file, token, dir).StartAsTask(stsToken, modelType)
      → ASRHttpUtils.DoRequest(secretId, secretKey, token, body, modelType)
    → 返回 Dictionary<string, object> {"code": int, "data": List<ASRResultEntity>}
  → WordApi.WordsMark()
  → AnalysisUtils.saveVideoLocalAnalysisData()
```

**唯一替换点是 `MyCallableTask.StartAsTask()`**。BLL 层完全不感知底层是腾讯云还是本地模型。

---

## 2. 结论：可行，复杂度中等

| 维度 | 评估 |
|------|------|
| 架构侵入性 | **低** — 仅需抽象 `MyCallableTask` 为接口，BLL 零改动 |
| 数据模型兼容性 | **已对齐** — SenseVoiceSmall 输出可直接映射到 `ASRResultEntity` + `ASRWordEntity` |
| 音频切片 | **零改动** — FFmpeg 59s MP3 分段流程完全复用 |
| 主要工程量 | 5 个新增类 + 1 个接口 + 2 处修改 |
| 风险点 | ONNX Runtime 依赖、MP3→PCM 解码、FBank 特征提取 |

---

## 3. 推荐架构设计

```
                    ┌─────────────────────┐
                    │    IASREngine       │  (新增接口)
                    │  RecognizeAsync()   │
                    └──────┬──────────────┘
           ┌──────────────┴──────────────┐
           │                             │
  ┌────────┴──────────┐    ┌─────────────┴──────────┐
  │ TencentASREngine  │    │ SenseVoiceSmallEngine  │  (新增包装类)
  │ (包装现有逻辑)      │    │ (ONNX 本地推理)         │
  └────────┬──────────┘    └─────────────┬──────────┘
           │                             │
  MyCallableTask               ONNX Runtime
  ASRHttpUtils                 NAudio (MP3解码)
  (现有代码不变)                  FBank/LFR 特征提取
```

### 3.1 接口定义

```csharp
public interface IASREngine
{
    /// <summary>
    /// 对单个音频片段执行语音识别
    /// </summary>
    /// <param name="audioFile">已切片的 MP3 音频文件 (≤59s, 16kHz mono)</param>
    /// <param name="engSerViceType">引擎模型类型标识 (如 "16k_zh")</param>
    /// <returns>code + ASRResultEntity 的字典结构，对齐现有 AsrUtils 约定</returns>
    Task<Dictionary<string, object>> RecognizeAsync(FileInfo audioFile, string engSerViceType);
}
```

### 3.2 包装策略

| 引擎 | 实现方式 | 对现有代码影响 |
|------|---------|:---:|
| `TencentASREngine` | 适配器模式包装 `MyCallableTask`，调用路径不变 | 零改动 |
| `SenseVoiceSmallEngine` | 全新实现：NAudio 解码 → FBank → ONNX 推理 → 后处理 → `ASRResultEntity` | 零影响（独立文件） |

---

## 4. 对现有流程的影响矩阵

| 现有流程步骤 | 是否变化 | 说明 |
|-------------|:---:|------|
| FFmpeg 音频切片 (59s MP3) | ❌ 不变 | 两种引擎共用同一 MP3 输入 |
| STS 临时凭证获取 | ⚠️ 跳过 | 本地引擎不需网络凭证，包装类内部直接跳过 |
| 逐段识别 (SemaphoreSlim) | ❌ 不变 | 串行控制保留在 AsrUtils |
| 段落排序 + Paragraph 赋值 | ❌ 不变 | AsrUtils 逻辑不动 |
| `WordApi.WordsMark()` 关键词匹配 | ❌ 不变 | 下游消费 `List<ASRResultEntity>` |
| 结果持久化 (JSON + SQLite) | ❌ 不变 | 同上 |
| 重试策略 | ⚠️ 简化 | 本地引擎无网络重试，仅保留模型异常重试 (2次) |
| 错误码映射 | ⚠️ 追加 | 601/603 等网络错误码不再产生，新增模型加载失败等错误码 |

---

## 5. 工程量分解

| # | 任务 | 文件 | 工作量 | 说明 |
|---|------|------|:---:|------|
| 1 | 定义 `IASREngine` 接口 | `Asr/IASREngine.cs` | 小 | 单方法接口，约 15 行 |
| 2 | `TencentASREngine` 包装类 | `Asr/TencentASREngine.cs` | 小 | 适配 `MyCallableTask`，现有逻辑零改动 |
| 3 | `SenseVoiceSmallEngine` | `Asr/SenseVoiceSmallEngine.cs` | 中 | 集成官方 ONNX 推理 + 后处理，约 200 行 |
| 4 | FBank/LFR 特征提取 | 复用官方 `WavFrontend.cs` | **小** | FunASR 官方 C# 实现已存在，直接集成 |
| 5 | MP3→PCM 解码 | NAudio 集成 | 中 | `Mp3FileReader` → `float[]`，约 30 行 |
| 6 | 时间戳对齐 + 词汇合并 | `Asr/SenseVoiceSmallEngine.cs` 内 | 中 | 1-12 字窗口合并，约 80 行 |
| 7 | 修改 `AsrUtils` | `Asr/AsrUtils.cs` | 小 | 注入 `IASREngine`，替换 `new MyCallableTask()` |
| 8 | 引擎选择配置 | 配置机制 | 小 | 开关控制用哪个引擎，约 10 行 |
| 9 | ONNX 模型文件管理 | 资源分发 | 中 | `model.onnx` (~200MB，新版模型无需 embed.onnx) |

**总工时估算: 4-6 天**（较原估算减少 2-3 天）

---

## 6. 关键技术风险

### 6.1 FBank/LFR 特征提取 (已解决 ✅)

~~最高风险~~ → **低风险**。2026-05-13 已确认 FunASR 官方 GitHub (`alibaba-damo-academy/FunASR`) 的 `runtime/csharp/AliParaformerAsr/AliParaformerAsr/WavFrontend.cs` 包含完整的 C# FBank+LFR 实现，由 `manyeyes` 贡献。同目录下 `OfflineProjOfSenseVoiceSmall.cs` 包含完整的 ONNX 推理封装。可直接复用，无需从 Python 手工移植。

**复用策略**：集成 `WavFrontend.cs` + `OfflineProjOfSenseVoiceSmall.cs` 到项目 `Asr/` 目录，参考其接口适配 `IASREngine`。

### 6.2 MP3→PCM 解码

当前切片输出 MP3，SenseVoiceSmall 需要 PCM `float[]`。两种方案：

- **方案 A**：修改 `SlicingAudio` 输出 PCM（影响腾讯云路径，**不推荐**）
- **方案 B (推荐)**：`SenseVoiceSmallEngine` 内部用 NAudio 解码 MP3 → PCM `float[]`（隔离性好，零影响现有流程）

### 6.3 ONNX Runtime 部署

- NuGet: `Microsoft.ML.OnnxRuntime` (CPU) 或 `Microsoft.ML.OnnxRuntime.Gpu` (CUDA)
- GPU 包体积较大 (~1.2GB)，需确认部署环境
- CPU 推理单段 59s 音频预计耗时 3-8 秒
- GPU 推理预计 < 1 秒

### 6.4 错误码映射

SenseVoiceSmall 不会产生腾讯云特有错误码（601 签名过期、603 QPS 超限），但引入新错误类型：

| Code | 含义 | 触发条件 |
|:----:|------|---------|
| 0 | 成功 | 识别正常完成 |
| 500 | 识别失败 | ONNX 推理异常或重试耗尽 |
| 701 | 音频文件不存在 | 目录中无音频文件 (AsrUtils 层面，不变) |
| **702** | **模型加载失败** | ONNX 模型文件缺失或损坏 |
| **703** | **音频解码失败** | MP3→PCM 转换异常 |
| **704** | **输入超限** | 音频超过 60s 限制 |

需在 `AnalysisUtils.checkAsrError()` 中追加以上错误码的中文映射。

---

## 7. 推荐实施路径

> **2026-05-13 更新**：官方 FunASR `runtime/csharp/AliParaformerAsr/` 目录下已有完整 C# 实现（`WavFrontend.cs` + `OfflineProjOfSenseVoiceSmall.cs` + `OfflineModel.cs`），Phase 1 FBank 移植工作量归零。

```
Phase 1: POC 验证 (1天)  ← 较原估算减少 1 天
  ├── 集成 ONNX Runtime + 加载模型 (复用 OfflineModel.cs)
  ├── 集成 WavFrontend.cs + OfflineProjOfSenseVoiceSmall.cs
  ├── 单段音频推理 + 结果对比腾讯云
  └── 决策门: 识别准确率 ≥ 腾讯云 95% 则继续

Phase 2: 工程集成 (3-4天)
  ├── IASREngine 接口 + TencentASREngine 包装
  ├── SenseVoiceSmallEngine 完善 + 后处理
  ├── AsrUtils 改造 + 引擎切换配置
  └── 错误码映射 + 日志

Phase 3: 测试验证 (1-2天)
  ├── 端到端集成测试 (录播+上传)
  ├── 边界 case (空音频/超长/纯音乐)
  ├── 并发稳定性测试
  └── 准确率对比报告
```

---

## 关联文档

- [ASR 模块总览](asr_module.md) — 现有腾讯云 ASR 完整流程与架构
- [SenseVoiceSmall 推理架构方案](asr_sensevoice_small.md) — 本地模型技术方案详情
- [本地 ASR 引擎集成 PRD](../specs/20260513_asr_local_engine_prd.md) — 完整需求文档与架构设计
