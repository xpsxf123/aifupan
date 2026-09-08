# ASR 引擎管道架构 (2.6.0-for-local-asr 起)

> 本文档是 ASR 子系统的**权威参考**。涵盖引擎抽象、工厂配置、本地推理管道、云端适配、结果组装全流程，以及历次代码审查沉淀的约束与反模式。
>
> **关联文档**: [asr_module.md](asr_module.md)（旧单引擎架构） | [asr_sensevoice_small.md](asr_sensevoice_small.md)（模型细节） | [WAL/api](../api/wal/20260515_asr_engine_fixes_api.md) | [WAL/rules](../preferences/wal/20260515_asr_engine_fixes_rules.md) | [NAudio禁用规则](wal/20260515_naudio_wasapi_missing_rules.md)

---

## 1. 整体架构

```
调用入口
  AnchorVideoBll / UploadFileBll
        │
        ▼
  AsrUtils.AsrByDirectoryPath(dir, token, engSerViceType)
        │  Lazy<AsrEngineFactory> 进程级单例
        │
        ├─ factory.Build(engSerViceType)
        │       │
        │       ├─ "svs:auto:withitn"          → SenseVoiceSmallEngine (直接返回池实例)
        │       ├─ "16k_zh" / "tencent"        → TencentASREngine     (直接返回池实例)
        │       └─ "svs:auto:withitn|tencent"  → CompositeASREngine
        │               ├─ SVS (priority 0, 先尝试)
        │               └─ Tencent (priority 1, 降级)
        │
        ▼
  foreach file → engine.RecognizeAsync(file, engSerViceType)
        │
        ├─ SenseVoiceSmallEngine  ──→  本地推理管道 (见第3节)
        └─ TencentASREngine       ──→  StartAsTaskAsync → 腾讯云 API
        └─ CompositeASREngine     ──→  按优先级顺序逐引擎尝试，首个 code=0 即返回
        │
        ▼
  结果组装 (AsrUtils)
        │  按文件名排序 → Paragraph 赋序号 → StringBuilder 重建 Result
        ▼
  WordApi.WordsMark(asrResultEntities)  → 关键词/敏感词匹配
```

---

## 2. 引擎抽象层

### 2.1 `IASREngine` 接口

```csharp
Task<Dictionary<string, object>> RecognizeAsync(FileInfo audioFile, string engSerViceType);
```

**返回约定**（所有引擎必须遵守）:

| key | 类型 | 说明 |
|-----|------|------|
| `"code"` | `int` | 0=成功；见错误码表 |
| `"data"` | `List<ASRResultEntity>` | 成功时包含识别结果；失败时包含错误信息 |

**错误码**:

| code | 含义 |
|------|------|
| 0 | 成功 |
| 500 | 通用错误 / 所有引擎失败 |
| 601 | 本地时间不正确（签名过期） |
| 602 | 获取临时凭证失败 |
| 603 | QPS 超限 |
| 701 | 音频文件不存在 |
| 702 | 模型文件未就绪 |
| 703 | 音频解码失败 / 特征提取失败 |
| 704 | 音频时长超过 60 秒限制 |

### 2.2 `ASRResultEntity` 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `Result` | `string` | 完整识别文本（由 AsrUtils 重建，英文词后单空格） |
| `WordList` | `List<ASRWordEntity>` | 词级别时间戳列表 |
| `FileName` | `string` | 对应的音频文件名 |
| `Paragraph` | `int` | 段落序号（由 AsrUtils 赋值，1-based） |
| `Code` | `int` | 识别结果状态码 |
| `AudioDuration` | `long` | 音频时长（ms），SVS 引擎填充 |

### 2.3 `ASRWordEntity` 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `Word` | `string` | 词文本（**不含**尾部空格，由引擎填充，AsrUtils 不修改） |
| `StartTime` | `long` | 词开始时间（ms） |
| `EndTime` | `long` | 词结束时间（ms） |

> ⚠️ **重要约束**: 禁止在 `AsrUtils` 或下游代码中修改 `ASRWordEntity.Word`。格式化拼接使用本地变量，不污染实体字段。

---

## 3. AsrEngineFactory — 工厂与配置

### 3.1 引擎池

```csharp
_pool = {
    ["tencent"] = new TencentASREngine(),   // 进程生命周期单例
    ["svs"]     = new SenseVoiceSmallEngine() // 进程生命周期单例
}
```

### 3.2 `engSerViceType` 配置语法

```
<engine-spec> [| <engine-spec> ...]

engine-spec:
  svs:<lang>:<textnorm>   →  本地 SenseVoiceSmall
  tencent                  →  腾讯云 ASR (默认 16k_zh)
  16k_zh / 8k_zh / ...    →  腾讯云 ASR 指定模型
```

**内置示例**:

| `engSerViceType` | 行为 |
|-----------------|------|
| `""` 或 `null` | 等同 `"svs:auto:withitn|tencent"`（本地优先，云端降级） |
| `"svs:auto:withitn"` | 仅本地引擎 |
| `"tencent"` | 仅腾讯云 |
| `"16k_zh"` | 腾讯云，16kHz 中文模型 |
| `"svs:zh:withitn\|tencent"` | 本地优先（中文），腾讯云降级 |
| `"tencent\|svs:auto:withitn"` | 腾讯云优先，本地降级 |

**lang 参数**: `auto`(默认) / `zh` / `en` / `yue` / `ja` / `ko` / `nospeech`

**textnorm 参数**: `withitn`(启用标点/数字归一化) / `woitn`(不启用)

### 3.3 多引擎管道（Composite）

- 按配置字符串从左到右为优先级顺序
- `CompositeASREngine` 保持插入顺序（无 weight 排序）
- 首个返回 `code=0` 的引擎结果直接返回，后续引擎跳过
- 全部失败才返回 `code=500`

> ⚠️ **约束**: `CompositeASREngine.RecognizeAsync` 的 `engSerViceType` 参数被**忽略**，实际使用各子引擎在 `Build()` 时固化的 `EngineEntry.Config`。

---

## 4. SenseVoiceSmallEngine — 本地推理管道

### 4.1 完整管道

```
WAV 音频文件（由 AudioUtils.SlicingAudio 预处理，16kHz mono）
  其他格式（极少） → FFmpeg 临时转 WAV → 读取后删除临时文件
    │
    ▼  WavFrontend.DecodeMp3()       纯 C# BinaryReader 解析 WAV header + 读 PCM
                                     非 WAV 格式 → ProcessStartInfo("tools\ffmpeg") 转码
                                     ⚠️ NAudio 已彻底移除（依赖链问题，见 7.3）
PCM float[] (16kHz mono)
    │
    ▼  时长校验                       < 400 采样 → 703; > 60s → 704
    │
    ▼  WavFrontend.GetFbank()        80维 FBank，25ms窗/10ms步移，Hamming窗，FFT=512
float[nFrames * 80]
    │
    ▼  WavFrontend.ApplyLfr()        LFR: m=7帧拼接, n=6步移 → 560维
float[nLfrFrames * 560]
    │
    ▼  ParseConfig(engSerViceType)   解析 lang_id / textnorm_id
    │
    ▼  EnsureModelLoaded()           双重检查锁，首次加载 ONNX Session
    │
    ▼  RunInference()                ONNX 推理（_inferenceLock 实例级串行）
    │    输入: speech[1,nFrames,560] / speech_lengths / language / textnorm
    │    输出: resultsArray[0]=logits[1,seqLen,vocabSize] (float32)
    │           resultsArray[1]=cif_peak[1,nAcousticFrames] (float32)
    │    ⚠️ 无独立序列长度输出；seqLen = logits.Dimensions[1]
    │    → 深拷贝 logits → Dispose OrtValues
    │
    ▼  GreedyDecode()                逐时间步取 argmax，非法索引回退到 0（blank）
int[] tokenIds (raw)
    │
    ▼  FilterSpecialTokens()         过滤 blank(≤2) / lang tokens / textnorm tokens
int[] filteredTokens
    │
    ▼  ComputeTimestamps(cifPeak, filteredCount)
    │    CIF 累积 (while >= 1.0f → fire); firePoint[k] → filteredToken[k]
    │    timestamps[i] = {start_ms, end_ms}，按 filtered 顺序建立
int[][filteredCount] timestamps
    │
    ▼  MergeWords(filteredTokens, timestamps)
    │    标点附着到前词关闭；句首/连续标点独立输出；12字拆分
    │    timestamps[i] 直接对应 filteredTokens[i]（无 raw index 偏移）
    │    BuildWord: Regex.Replace(word, @"<\|[^|]*\|>", "")  ← 清理情感/事件标签
List<ASRWordEntity>                  Word 字段已无 <|HAPPY|> / <|Speech|> 等残余标签
    │
    ▼  BuildResultText()             英文词末字符判断，单空格分隔
    │                                Regex.Replace(text, @"<\|[^|]*\|>", "")  ← 兜底清理
string Result
    │
    ▼  ASRResultEntity               FileName / Result / WordList / AudioDuration
```

### 4.2 模型管理（ModelManager）

| 关键点 | 说明 |
|--------|------|
| 存储路径 | `%LocalAppData%/ReviewAnalysis/models/sensevoice-small/` |
| 模型文件 | `model_quant.onnx` + `tokens.json` |
| 下载 URL 配置 | `%LocalAppData%/ReviewAnalysis/Config/svs_model_config.json`（可覆盖默认 URL） |
| 完整性校验 | SHA256 + `manifest.json`，首次慢路径校验，通过后快路径（文件存在检查） |
| 下载策略 | 支持断点续传（`Range` 请求头），重试 3 次，超时 30 分钟 |
| 启动预热 | `Program.cs` 调用 `ModelManager.WarmupAsync()`，后台静默下载 |
| 静态共享状态 | `_httpClient` / `_downloadTask` / `_downloadLock` / `_modelVerified` 均为 `static`，多实例共享 |

### 4.3 GPU 探测（GpuProbe + OfflineModel）

- 启动时探测顺序：CUDA → DirectML → CPU
- `AppendExecutionProvider_*` 成功不代表推理可用；实际在 `OfflineModel` 构造 `InferenceSession` 时验证
- 失败时通过 `catch (Exception ex) when (probe.BestProvider != Cpu)` 自动降级 CPU，记录日志，**不崩溃**

### 4.4 并发模型

```
_inferenceLock  实例级 SemaphoreSlim(1,1)   → 同一引擎实例的推理串行化
_initLock       实例级 object               → 模型/词表首次加载的双重检查锁
```

> ⚠️ `AsrEngineFactory._pool["svs"]` 是进程级单例，所有并发 ASR 请求共享同一 `SenseVoiceSmallEngine`，通过 `_inferenceLock` 串行推理。

---

## 5. TencentASREngine — 云端适配

### 5.1 调用链

```
TencentASREngine.RecognizeAsync()
    │
    ▼  ResolveModelType(config)   "tencent" → "16k_zh"; "16k_zh" → 原样传递
    │
    ▼  MyCallableTask.StartAsTaskAsync(null, modelType)
    │    ├─ AsrApi.GetTempToken("")    获取 STS 临时凭证
    │    ├─ ASRHttpUtils.DoRequest()   TC3-HMAC-SHA256 签名 → 腾讯云 SentenceRecognition
    │    │    VoiceFormat = "wav"  ← 2026-05-15 由 "mp3" 改为 "wav"（与 AudioUtils 输出对齐）
    │    └─ await Task.Delay(5000)     重试等待（不阻塞线程池，最多 3 次）
    │
    ▼  规范化返回值
        ASRResultEntity → List<ASRResultEntity>（与 IASREngine 契约对齐）
```

### 5.2 重试策略

| 条件 | 行为 |
|------|------|
| 识别结果为 null | 重试，最多 3 次（`asrCount`），每次 `Task.Delay(5000)` |
| `AuthFailure.SignatureExpire` | 立即返回 code=601，不重试 |
| `RequestLimitExceeded` | 重试，最多 300 次（`qpsCount`），每次 `Task.Delay(5000)` |
| 其他 API 错误 | 重试，最多 3 次 |

---

## 6. AsrUtils — 入口与结果组装

### 6.1 `AsrByDirectoryPath` 流程

```csharp
// 1. 枚举目录下所有音频文件（递归）
// 2. factory.Build(engSerViceType) 构建引擎（一次性，循环外）
// 3. foreach 串行识别每个文件（无并发控制，天然串行）
// 4. 首个失败立即返回（code != 0），已识别结果作为 data 返回
// 5. 按文件名排序，赋 Paragraph 序号（1-based）
// 6. StringBuilder 重建 Result（不修改 WordList.Word）
```

### 6.2 Result 重建规则

```
foreach word in WordList:
    sb.Append(word.Word)
    if word.Word 末字符是 ASCII 字母 / ' / .:
        sb.Append(' ')       // 单空格，与 BuildResultText 一致
Result = sb.ToString().TrimEnd()
```

> ⚠️ **反模式（已修复）**: 禁止 `item.Word += "  "`。`Word` 字段需保持原始词文本，格式化在 `Result` 字符串中完成。

---

## 7. 关键约束与反模式

### 7.1 时间戳对齐约束（CIF）

```
✅ 正确: timestamps[i] ↔ filteredToken[i]  (filtered 索引直接对应)
❌ 错误: timestamps[rawIndex] ↔ filteredToken[i]  (raw index 偏移，导致系统性错位)
```

CIF fire point k 对应 **filtered content token k**。lang/textnorm 前缀 token 不消耗 CIF fire。

### 7.2 线程安全

| 资源 | 保护机制 | 作用域 |
|------|---------|--------|
| ONNX 推理 | `_inferenceLock` (`SemaphoreSlim(1,1)`) | 实例级 |
| 模型/词表加载 | `_initLock` (`object` + 双重检查) | 实例级 |
| 模型下载 | `_downloadLock` (`static object` + 双重检查) | 类级 |
| 下载状态 | `_modelVerified` (`static volatile bool`) | 类级 |

### 7.3 反模式清单

| 反模式 | 原因 | 正确做法 |
|--------|------|---------|
| `new HttpClient()` 在构造函数/方法中 | Socket 泄漏 | `static readonly HttpClient` |
| `Thread.Sleep` 在 `Task.Run` 内 | 线程池耗尽 | `await Task.Delay` |
| 修改 `ASRWordEntity.Word` 做格式化 | 污染词表数据 | 本地 `StringBuilder` |
| `SemaphoreSlim(1,1)` 在串行 foreach 中 | 无效代码 | 直接 await |
| 裸 `catch { }` 不记录异常 | 吞噬错误，无法排查 | `catch (Exception ex) { LogAnalysis(...) }` |
| timestamps 用 raw index 查询 | CIF 对齐错误 | 用 filtered 顺序索引 |
| 引入 NAudio.dll | 依赖链：NAudio.Wasapi→WinMM→Asio→Midi→WinForms，缺一崩溃 | FFmpeg + 纯 C# BinaryReader |
| `resultsArray[1].AsEnumerable<long>()` 读 seqLen | 模型第2输出是 cif_peak(float32)，不是 int64 | `logits.Dimensions[1]` 直接从 tensor shape 取 |
| tokens.json 假设是 JSON 格式 | 文件可能是 JSON array / JSON dict / 纯文本 | 三格式依次尝试解析 |
| BuildResultText / BuildWord 不清理标签 | `<\|HAPPY\|>` `<\|Speech\|>` 等情感/事件标签透出到输出文本 | `Regex.Replace(text, @"<\|[^|]*\|>", "")` |

---

## 8. 文件清单

| 文件 | 职责 |
|------|------|
| `Asr/IASREngine.cs` | 统一引擎接口 |
| `Asr/AsrEngineFactory.cs` | 引擎池 + 配置解析 + 管道构建 |
| `Asr/CompositeASREngine.cs` | 多引擎优先级降级管道；各子引擎 try/catch 隔离 |
| `Asr/TencentASREngine.cs` | 腾讯云引擎适配器；try/catch + 日志 |
| `Asr/MyCallableTask.cs` | 腾讯云 ASR 重试逻辑（async） |
| `Asr/AsrUtils.cs` | 入口、串行编排、结果排序/重建 |
| `Asr/ASRHttpUtils.cs` | 腾讯云 API 请求封装；VoiceFormat="wav" |
| `Asr/Local/SenseVoiceSmallEngine.cs` | 本地推理主引擎 |
| `Asr/Local/WavFrontend.cs` | FBank+LFR 特征提取；音频解码（FFmpeg+BinaryReader，无 NAudio） |
| `Asr/Local/ModelManager.cs` | 模型下载/校验/缓存（static 共享状态） |
| `Asr/Local/OfflineModel.cs` | ONNX Session 封装 + GPU 降级 |
| `Asr/Local/GpuProbe.cs` | 执行提供程序探测（CUDA/DirectML/CPU） |
| `Asr/ASRResultEntity.cs` | 识别结果实体 |
| `Asr/ASRWordEntity.cs` | 词级时间戳实体 |
| `Utils/AudioUtils.cs` | 视频→WAV 音频切片（16kHz mono，59s 分段，FFmpeg） |

---

## 9. 配置速查

```jsonc
// svs_model_config.json (可选覆盖，位于 %LocalAppData%/ReviewAnalysis/Config/)
{
  "version": "1.0",
  "files": {
    "model_quant.onnx": "https://your-cdn/model_quant.onnx",
    "tokens.json": "https://your-cdn/tokens.json"
  }
}
```

```
// AnchorEntity.engSerViceType 推荐配置
svs:auto:withitn|tencent    // 本地优先，云端降级（默认）
svs:zh:withitn              // 纯本地，中文
tencent                     // 纯腾讯云
16k_zh                      // 腾讯云中文模型（显式）
```

---

## 10. 变更历史

| 版本 | 日期 | 变更摘要 |
|------|------|---------|
| 2.6.0-for-local-asr (init) | 2026-05-14 | 集成 SenseVoiceSmall 本地 ASR，IASREngine 抽象，Composite 降级管道 |
| 2.6.0-for-local-asr (fix1) | 2026-05-15 | CIF 时间戳对齐修复；GPU fallback；HttpClient 单例；Thread.Sleep→Task.Delay；Word 字段污染修复；Weight 死代码清理；ReplayHttpUtils1.cs 删除 |
| 2.6.0-for-local-asr (fix2) | 2026-05-15 | NAudio 全量移除→FFmpeg+BinaryReader；tokens.json 三格式兼容；三层 try/catch 健壮性（SVS/Composite/Tencent）；`<\|...\|>` 标签正则清理（BuildResultText+BuildWord）；音频切片 mp3→wav；腾讯 VoiceFormat=wav；CefSharp localhost 视频白名单（复盘播放修复） |
