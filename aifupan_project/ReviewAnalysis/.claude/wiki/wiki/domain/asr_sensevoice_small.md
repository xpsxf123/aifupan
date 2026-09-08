# SenseVoiceSmall C# 增强型推理架构方案

本文档描述基于 SenseVoiceSmall ONNX 模型的本地 ASR 推理架构，作为腾讯云 ASR 的本地替代方案。目标场景：**单次输入 ≤ 60s**，通过多任务 Embedding 注入实现中粤英混合识别 + 原生标点 + 时间戳对齐。

数据来源: 用户提供的 SenseVoiceSmall 技术调研方案 + FunASR 官方 C++/C# runtime 源码验证 (2026-05-13)

---

## 1. 架构总览

```
┌──────────────────────────────────────────────┐
│              输入网关 (Input Gateway)           │
│  PCM 16kHz 硬性校验 — 超 60s (960k采样点) 阻断  │
└──────────────────┬───────────────────────────┘
                   │
    ┌──────────────┴──────────────┐
    │ 前端特征提取器 (FBank/LFR)    │
    │ 80 维 Mel-FBank → LFR 拼接   │
    │ → 560 维特征向量              │
    └──────────────┬──────────────┘
                   │
    ┌──────────────┴──────────────┐
    │ 指令编码器 (embed.onnx)       │
    │ Language/Event/ITN/Norm ID   │
    │ → 4 帧引导向量               │
    └──────────────┬──────────────┘
                   │
    ┌──────────────┴──────────────┐
    │ 主推理引擎 (model.onnx)       │
    │ ASR 解码 + Token 级隐层对齐   │
    │ 输出 Token 流 + 时间索引       │
    └──────────────┬──────────────┘
                   │
    ┌──────────────┴──────────────┐
    │ 后处理逻辑控制器               │
    │ 繁简转换 / ITN 补强            │
    │ 1-12 字词汇合并 / 时间戳计算    │
    └──────────────┬──────────────┘
                   │
    ┌──────────────┴──────────────┐
    │ 标准化输出 (ApiResponse)      │
    │ 映射为 ASRResultEntity 结构   │
    └──────────────────────────────┘
```

---

## 2. 推理配置与引导逻辑 (Task Vector Settings)

> ✅ 已通过 `manyeyes/OfflineProjOfSenseVoiceSmall` 生产代码验证。

通过 `embed.onnx` 分别生成各维度引导向量，按固定顺序拼接到语音特征前面：

| 任务维度 | 选项 (Token) | ID | 生成方式 | 说明 |
| --- | --- | --- | --- | --- |
| **Language** | `auto` / `zh` / `en` / `yue` / `ja` / `ko` | 见 7.1 语种表 | `EmbedProj([langId])` → 1×560 维 | 动态识别或强制语种 |
| **Event+Emotion** | `[1, 2]` (硬编码) | 1, 2 | `EmbedProj([1, 2])` → 2×560 维 | 默认事件/情感标记，不可配置 |
| **ITN/文本标准化** | `withitn` / `woitn` | 14 / 15 | `EmbedProj([textnormId])` → 1×560 维 | `withitn`=标点+数字格式化；`woitn`=纯文本 |

**拼接顺序**（参考代码验证）：
```
[language(560)] + [event_emo(1120)] + [textnorm(560)] + [speech_fbank_lfr(N*560)]
                                                    ↑ 总共 4 帧 embedding, 共 2240 维
```

**模型双模式**（取决于 ONNX 模型导出方式）：
- **新版模型** ✅ (已确认 ModelScope `iic/SenseVoiceSmall-onnx`)：`model.onnx` 包含 `speech` + `speech_lengths` + `language` + `textnorm` 四个输入节点，language/textnorm 以标量 ID 直接传入，**无需 embed.onnx**
- **旧版模型**：如果不包含这些输入 → 使用 `embed.onnx` 生成 embedding 向量并拼接到 speech 前端

**ONNX 输入/输出 spec（已通过生产运行验证，2026-05-15）**：
| 节点 | 维度 | 类型 | 说明 |
|------|------|------|------|
| 输入 `speech` | [batch, T, 560] | float32 | FBank+LFR 特征（WavFrontend.cs 生成） |
| 输入 `speech_lengths` | [batch] | int32 | 有效帧数 |
| 输入 `language` | [batch] | int32 | 语种 ID 标量 |
| 输入 `textnorm` | [batch] | int32 | withitn=14 / woitn=15 |
| 输出 `logits` (index 0) | [batch, T, V] | float32 | token logits；seqLen = logits.Dimensions[1] |
| 输出 `cif_peak` (index 1) | [batch, T] | float32 | CIF peak 激活值（时间戳对齐） |

> ⚠️ **重要修正**：本模型只有 **2 个输出**（logits + cif_peak，均为 float32）。**不存在**独立的序列长度输出（旧文档描述的 `[batch] int` 是错误的）。有效序列长度应从 `logits.Dimensions[1]` 读取。`cif_peak` 的 ONNX 输出索引为 **1**，不是 3。

**关于 `norm`/`unorm`**：参考代码中 **不存在** 独立的 `norm(11)` / `unorm(12)` task ID。这些 ID (11, 12) 实际对应 `ja` / `ko` 语种。文本规整化由 `withitn`/`woitn` + 模型内部处理完成。

---

## 3. 时间戳对齐与词汇合并策略

### 3.1 时间戳计算逻辑（✅ CIF 机制）

> ❌ 废弃: `StartTime_ms = (TokenIndex - 4) × 60`（简单公式，不准确）
> ✅ 采用: CIF (Continuous Integrate-and-Fire) 峰值检测 + `TimestampLfr6` 算法

SenseVoiceSmall 模型第 4 个输出为 **CIF peak 激活值**（`cif_peak` tensor），帧率为 LFR6（每 6 个原始帧合并为 1 个 LFR 帧，每帧 60ms）。

```csharp
// 参考代码实现
Tensor<float> cifPeak = results[3].AsTensor<float>();        // 模型第4输出
List<int[]> timestamps = ComputeHelper.TimestampLfr6(        // CIF峰值→时间戳映射
    usCifPeak, tokenIdsList[i].ToArray());
// timestamps[i] = [startTime_ms, endTime_ms]  ← 帧对齐的真实时间戳
```

- **帧率基准**：LFR6 下每一帧对应 **60ms** 物理时长（`60ms = 6 × 10ms`，原始帧移 10ms）
- **CIF 机制**：模型在每个解码步输出一个 CIF 激活值，累积到阈值后"发射"一个 token
- **对齐精度**：帧级对齐（60ms 粒度），优于简单索引乘法

### 3.2 词汇合并约束 (1-12 字)

合并触发条件（满足任一即执行 `WordList` 条目合并）：

- 当前累积字数（含标点）达到 **12**
- 遇到强标点符号（`。` `？` `！`）
- 检测到语种标签切换（如从 `zh` 变为 `en`）

**标点归属**：标点符号不单独作为 `Word` 条目，强制依附于前一个词汇，并继承该词汇的结束时间。

---

## 4. 核心代码实现架构 (C#)

> ✅ 基于 `manyeyes/OfflineProjOfSenseVoiceSmall` 参考实现校正。

```csharp
public class SenseVoiceSmallEngine : IDisposable
{
    private InferenceSession _modelSession;   // model.onnx
    private InferenceSession _embedSession;   // embed.onnx (旧版模型需要)
    private ITokenizer _tokenizer;            // Textoken 类型
    private bool _useITN;                     // withitn / woitn
    private int _languageId;                  // 0=auto, 3=zh, 4=en, 7=yue, 11=ja, 12=ko

    public ASRResultEntity Recognize(float[] pcmData)
    {
        // 1. 入口硬校验
        int durationMs = (int)(pcmData.Length / 16.0);
        if (durationMs > 60000) throw new Exception("Audio exceeds 60s limit.");

        // 2. FBank + LFR 特征提取 → [N, 560]
        var speechFeatures = FeatureExtractor.ComputeFBankLFR(pcmData);

        // 3. 判断模型类型
        bool isNewModel = _modelSession.InputMetadata.ContainsKey("language");
        
        List<NamedOnnxValue> inputs;
        if (isNewModel)
        {
            // 新版模型: language/textnorm 作为标量直接输入
            inputs = CreateNewModelInputs(speechFeatures, _languageId, _useITN ? 14 : 15);
        }
        else
        {
            // 旧版模型: 用 embed.onnx 生成 embedding 向量，拼接到 speech 前方
            var langEmbed = EmbedProj(_languageId);          // 1×560
            var eventEmoEmbed = EmbedProj(1, 2);              // 2×560
            var textnormEmbed = EmbedProj(_useITN ? 14 : 15); // 1×560
            // 拼接: [lang(560)] + [event_emo(1120)] + [textnorm(560)] + [speech(N*560)]
            var mergedFeatures = ConcatAll(langEmbed, eventEmoEmbed, textnormEmbed, speechFeatures);
            inputs = CreateOldModelInputs(mergedFeatures);
        }

        // 4. ONNX 推理
        using var results = _modelSession.Run(inputs);
        var resultsArray = results.ToArray();

        // 5. 输出解析
        //    resultsArray[0] = logits [1, T, V] — token 概率 (float32)
        //    resultsArray[1] = cif_peak [1, T]  — CIF 激活值 (float32)
        //    ⚠️ seqLen 从 logits.Dimensions[1] 读取，无独立序列长度输出
        var logits = resultsArray[0].AsTensor<float>();
        int seqLen = logits.Dimensions[1];  // 从 tensor shape 获取，不从 resultsArray[1] 读
        var tokenIds = GreedyDecode(logits, seqLen);
        var cifPeak = resultsArray[1].AsTensor<float>();  // index=1，不是 3
        var timestamps = ComputeHelper.TimestampLfr6(cifPeak, tokenIds);

        // 6. Token → 文本解码 + 词汇合并
        var text = _tokenizer.Decode(tokenIds);
        var wordList = Aggregator.MergeToSentenceWords(text, tokenIds, timestamps, min: 1, max: 12);

        // 7. 构造标准返回
        return new ASRResultEntity
        {
            Result = text,
            AudioDuration = durationMs,
            WordSize = wordList.Count.ToString(),
            WordList = wordList
        };
    }

    private float[] EmbedProj(params long[] ids)
    {
        // embed.onnx 推理: 输入 x [1, N], 输出 [N * 560]
        var tensor = new DenseTensor<long>(ids, new[] { 1, ids.Length });
        var inputs = new List<NamedOnnxValue> { NamedOnnxValue.CreateFromTensor("x", tensor) };
        using var results = _embedSession.Run(inputs);
        return results.ToArray()[0].AsTensor<float>().ToArray();
    }
}
```

---

## 5. 输出数据模型 (符合业务规范)

```json
{
    "Response": {
        "RequestId": "41ed9283-0c09-46fb-917b-0b83fa95f0be",
        "Result": "识别结果汇总，支持中粤英混合内容。",
        "AudioDuration": 58000,
        "WordSize": 15,
        "WordList": [
            {
                "Word": "识别结果",
                "StartTime": 120,
                "EndTime": 840
            },
            {
                "Word": "汇总，",
                "StartTime": 840,
                "EndTime": 1200
            }
        ]
    }
}
```

该结构可直接映射到现有 `ASRResultEntity` + `ASRWordEntity`。

---

## 6. 针对混合场景的优化

### 6.1 年份与金额精准度

针对 `withitn` 模式可能出现的异常，在 `WordList` 生成后执行正则后验：
- 正则匹配：`[零一二三四五六七八九]{4}年` → 转换为 `2024年`

### 6.2 GPU 探针与推理加速

#### GPU 探针设计

启动时自动检测硬件加速能力，选择最优推理后端：

```csharp
public static class GpuProbe
{
    /// <summary>
    /// 检测结果，包含可用加速器和显存信息
    /// </summary>
    public class GpuInfo
    {
        public bool HasCuda { get; set; }
        public bool HasDirectML { get; set; }
        public bool HasCoreML { get; set; }
        public string SelectedProvider { get; set; } = "CPU";
        public long CudaMemoryMB { get; set; } = -1;
        public string DeviceName { get; set; } = "Unknown";
    }

    /// <summary>
    /// 探测可用 GPU 加速器，返回优先级: CUDA > DirectML > CoreML > CPU
    /// </summary>
    public static GpuInfo Detect()
    {
        var info = new GpuInfo();
        try
        {
            var providers = OrtEnv.Instance().GetAvailableProviders();
            info.HasCuda = providers.Contains("CUDAExecutionProvider");
            info.HasDirectML = providers.Contains("DmlExecutionProvider");
            info.HasCoreML = providers.Contains("CoreMLExecutionProvider");

            if (info.HasCuda)
            {
                info.SelectedProvider = "CUDA";
                info.DeviceName = TryGetCudaDeviceName();
                info.CudaMemoryMB = TryGetCudaMemory();
            }
            else if (info.HasDirectML)
            {
                info.SelectedProvider = "DirectML";
                info.DeviceName = "DirectML GPU";
            }
            else if (info.HasCoreML)
            {
                info.SelectedProvider = "CoreML";
                info.DeviceName = "Apple Neural Engine";
            }
        }
        catch
        {
            // ONNX Runtime 不可用 → 保持 CPU
        }
        return info;
    }

    /// <summary>
    /// 根据探测结果创建最优 SessionOptions（供 SenseVoiceSmall 和 Embed 两个 Session 共用配置）
    /// </summary>
    public static SessionOptions CreateOptimalOptions(GpuInfo? gpuInfo = null)
    {
        gpuInfo ??= Detect();
        var options = new SessionOptions();

        switch (gpuInfo.SelectedProvider)
        {
            case "CUDA":
                // CUDA: 最大显存复用，关闭 CPU arena 减少内存占用
                options.AppendExecutionProvider_CUDA();
                options.EnableCpuMemArena = false;
                break;
            case "DirectML":
                options.AppendExecutionProvider_DML();
                break;
            case "CoreML":
                options.AppendExecutionProvider_CoreML();
                break;
            // default: CPU (MLAS) — 无需额外配置
        }

        options.GraphOptimizationLevel = GraphOptimizationLevel.ORT_ENABLE_ALL;
        return options;
    }

    // 通过 WMI (Windows) 或 NVML 获取设备名
    private static string TryGetCudaDeviceName()
    {
        try { return System.Management... /* WMI Win32_VideoController */; }
        catch { return "NVIDIA GPU"; }
    }

    // 通过 NVML 或 WMI 获取可用显存
    private static long TryGetCudaMemory()
    {
        try { return ...; }
        catch { return -1; } // Unknown
    }
}
```

#### 推理 Session 初始化

```csharp
// SenseVoiceSmallEngine 构造函数中:
var gpuInfo = GpuProbe.Detect();
var options = GpuProbe.CreateOptimalOptions(gpuInfo);
_modelSession = new InferenceSession(modelPath, options);
if (embedPath != null)
    _embedSession = new InferenceSession(embedPath, options);
Logger.Info($"ASR engine: provider={gpuInfo.SelectedProvider}, device={gpuInfo.DeviceName}");
```

#### 预期推理延迟（基于参考实现估算）

| 硬件 | 后端 | 58s 音频推理延迟 | 显存占用 |
|------|------|:---:|:---:|
| NVIDIA RTX 3060+ (12GB) | CUDA | 0.5-1.5s | ~1.5GB |
| NVIDIA GTX 1650 (4GB) | CUDA | 2-4s | ~1.5GB |
| Intel Arc / AMD | DirectML | 3-6s | ~1.5GB |
| Apple M1/M2/M3 | CoreML | 2-5s | ~1.0GB |
| Intel i5-12400 (CPU) | MLAS | 15-25s | ~600MB RAM |
| Intel i7-12700 (CPU) | MLAS | 8-15s | ~600MB RAM |

> **结论**: GPU 环境推理延迟完全可接受（<5s）；纯 CPU 环境 15-25s 延迟偏高，需评估业务可接受度。

- **并发控制**：单 GPU 推理显存约 1.5GB，GTX 1650 (4GB) 可支持 2 路并发，RTX 3060+ (12GB) 可支持 6-8 路。本方案暂维持 `SemaphoreSlim(1)` 保守策略。

### 6.3 英语词汇处理

英文单词在合并计数时作为单个 Word 单元，防止因字母过多导致过早切分。

---

## 7. SenseVoiceSmall 任务维度全量可选项

### 7.1 语种策略 (Language)

> ✅ ID 映射已通过 `manyeyes` 参考代码校正。

| 选项 (Token) | ID | 解释 | 对业务的影响 |
| --- | --- | --- | --- |
| **`auto`** | **0** | 自动识别，支持中英粤混读 | 短音频/强噪音下可能误判 |
| **`zh`** | **3** | 强制中文普通话 | 防止误识别为粤语/英语 |
| **`en`** | **4** | 强制英语 | 适合纯英语场景 |
| **`yue`** | **7** | 强制粤语 | 输出方言字，通常为繁体 |
| **`ja`** | **11** | 日语 | 跨境电商/日语直播 |
| **`ko`** | **12** | 韩语 | 跨境娱乐/韩语直播 |
| **`nospeech`** | **13** | 纯静音检测，过滤非人声 | 用于判断音频是否含有效语音 |

### 7.2 文本标准化 (Text Norm) — 同一维度，互斥

| 选项 (Token) | ID | 解释 | 对业务的影响 |
| --- | --- | --- | --- |
| **`withitn`** | **14** | **标点 + 数字格式化**。强制输出标点，中文数字→阿拉伯数字 | 提升可读性，直播/字幕必备。同时包含文本规整化效果（日期、百分比、金额） |
| **`woitn`** | **15** | **纯文本转录**。不输出标点，数字保持发音对应汉字 | 保留原始发音，适合二次深度加工的科研数据 |

> **注意**：参考代码中不存在独立的 `norm(11)` / `unorm(12)` 配置项。文本规整化是 `withitn` 的内置行为，非独立 task ID。旧文档中 `norm=11` 与 `ja=11`、`unorm=12` 与 `ko=12` 存在 **ID 冲突**。

### 7.3 Event+Emotion (硬编码，不可配置)

| 选项 | ID | 解释 |
| --- | --- | --- |
| **event** | **1** | 默认事件标记（硬编码，不可更改） |
| **emotion** | **2** | 默认情感标记（硬编码，不可更改） |

> 这两个 ID 在 `EmbedProj([1, 2])` 调用中始终为固定值，业务侧不可配置。模型通过这两个 embedding 控制非人声事件和情感基调的感知。

---

## 8. 后处理约束逻辑（硬逻辑）

无论如何选择以上参数，架构侧将始终执行以下硬逻辑以对齐现有 API 规范：

1. **Token 解码**：通过 `Textoken` 类型 tokenizer 将 token ID 序列解码为文本字符串
2. **CIF 时间戳映射**：通过 `TimestampLfr6(cifPeak, tokenIds)` 将 CIF 激活值映射为 token 级 `[startMs, endMs]`，帧率 60ms
3. **1-12 字窗口合并**：无论模型输出多长，后处理逻辑在 12 字处或强标点处截断，生成一个 `WordList` 条目
4. **标点依附**：标点符号不单独作为 `Word` 条目，依附于前一个词汇并继承其结束时间

> **关于繁简转换**：参考代码中未集成 OpenCC。粤语场景 `auto` 模式即可处理，若需繁体→简体转换，应在上层 `Aggregator` 后处理中按需添加，不作为引擎内建逻辑。

---

## 关联文档

- [ASR 模块总览](asr_module.md) — 现有腾讯云 ASR 完整流程与架构
- [本地 ASR 引擎集成可行性评估](asr_local_engine_evaluation.md) — 基于本方案的工程可行性分析
- [本地 ASR 引擎集成 PRD](../specs/20260513_asr_local_engine_prd.md) — 完整需求文档与架构设计
