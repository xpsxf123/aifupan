spec_mode: STANDARD

# 本地 ASR 引擎集成 — OpenSpec

> 日期: 2026-05-14 | 作者: @System Architect
> 基于: [白皮书](20260513_asr_local_engine_whitepaper.md) · [统一 PRD](20260513_asr_local_engine_unified.md)

---

## 1. Context

- **Business goal**: 集成 SenseVoiceSmall ONNX 本地引擎，使 ReviewAnalysis 在不依赖腾讯云 API 的情况下完成语音识别，同时保持现有腾讯云路径零改动，支持双引擎自动降级。
- **Scope of change**:
  - 新增: `Asr/IASREngine.cs`, `Asr/CompositeASREngine.cs`, `Asr/TencentASREngine.cs`, `Asr/SenseVoiceSmallEngine.cs`, `Asr/WavFrontend.cs`, `Asr/OfflineModel.cs`, `Asr/ModelManager.cs`, `Asr/GpuProbe.cs`, `Asr/AsrEngineFactory.cs`
  - 修改: `Asr/AsrUtils.cs` (注入 IASREngine), `Utils/AnalysisUtils.cs` (追加错误码映射)
  - 不改: `Asr/MyCallableTask.cs`, `Asr/ASRHttpUtils.cs`, `Asr/ASRResultEntity.cs`, BLL 层全部文件
- **Dependencies**:
  - `depends_on: [20260513_asr_local_engine_whitepaper.md, 20260513_asr_local_engine_unified.md, ../domain/asr_module.md, ../domain/asr_sensevoice_small.md, ../domain/asr_local_engine_evaluation.md]`
  - NuGet: `Microsoft.ML.OnnxRuntime` (≥1.17), `NAudio` (≥2.2)
  - 外部代码复用: FunASR `runtime/csharp/AliParaformerAsr/` — `WavFrontend.cs`, `OfflineModel.cs`, `OfflineProjOfSenseVoiceSmall.cs`

---

## 2. Domain Model

### 2.1 新增术语

| 术语 | 定义 |
|------|------|
| **IASREngine** | 引擎统一接口，定义 `RecognizeAsync(FileInfo, string) → Task<Dictionary<string, object>>` |
| **CompositeASREngine** | 组合器，按权重降序遍历子引擎列表，任一成功即返回，全失败才报错 |
| **TencentASREngine** | 腾讯云适配器，包装现有 `MyCallableTask.StartAsTask()`，零改动 |
| **SenseVoiceSmallEngine** | 本地 ONNX 推理引擎，完整管道: NAudio 解码 → FBank+LFR → ONNX 推理 → 后处理 |
| **AsrEngineFactory** | 引擎工厂，启动时预创建实例，`Build(engSerViceType)` 解析配置返回对应引擎 |
| **ModelManager** | 模型生命周期管理：下载、SHA256 校验、断点续传、原子替换、损坏自愈 |
| **GpuProbe** | 启动时探测可用硬件加速器，优先级: CUDA > DirectML > CoreML > CPU |

### 2.2 引擎配置语法

```
<engine>[:<config>][|<engine2>...]

engine:  "tencent" | "svs"
svs config:  <lang>:<itn>
  lang:  auto | zh | en | yue | nospeech
  itn:   withitn | woitn
```

示例:
- `"16k_zh"` → TencentASREngine（向后兼容）
- `"svs:auto:withitn"` → SenseVoiceSmallEngine only
- `"svs:auto:withitn|tencent"` → 本地优先，腾讯云兜底

### 2.3 新增错误码枚举

```csharp
// 新增到 AsrErrorCode 枚举
ModelNotReady = 702,   // ONNX 模型文件缺失或损坏
AudioDecodeFailed = 703, // MP3→PCM 转换异常
InputTooLong = 704       // 音频超过 60s
```

### 2.4 状态机: ModelManager

```
Uninitialized → Downloading → Ready
                           ↘ DownloadFailed → (next restart) → Downloading
```

---

## 3. API Contract (Handoff)

本次变更不涉及 HTTP API。核心契约是 `IASREngine` 接口（进程内调用）。

### 3.1 接口定义

```csharp
namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// ASR 引擎统一接口。所有引擎（本地/云端/组合）必须实现。
    /// </summary>
    public interface IASREngine
    {
        /// <param name="audioFile">已切片的 MP3 文件 (16kHz, mono, ≤59s)</param>
        /// <param name="engSerViceType">引擎配置字符串, 见 2.2 语法</param>
        /// <returns>
        /// Dictionary with:
        ///   "code" → int     (0=成功, 500=失败, 702=模型未就绪, 703=解码失败, 704=输入超限)
        ///   "data" → List&lt;ASRResultEntity&gt;  (code=0 时有值)
        /// </returns>
        Task<Dictionary<string, object>> RecognizeAsync(FileInfo audioFile, string engSerViceType);
    }
}
```

### 3.2 返回格式（与腾讯云完全一致）

```csharp
// 成功
{ "code": 0, "data": [ ASRResultEntity, ... ] }

// 失败
{ "code": 500, "data": null }
```

### 3.3 AsrEngineFactory.Build 签名

```csharp
public class AsrEngineFactory
{
    public AsrEngineFactory(); // 预创建 tencent + svs 实例，不阻塞
    public IASREngine Build(string engSerViceType); // 解析配置，返回单引擎或 Composite
}
```

### 3.4 ASRResultEntity 结构（不改）

```csharp
public class ASRResultEntity
{
    public string Text { get; set; }          // 识别文本
    public List<ASRWordEntity> WordList { get; set; } // 分词列表，每条目 1-12 字
    public double Confidence { get; set; }     // 置信度 (SenseVoiceSmall 固定 0.95)
    // ... 其余现有字段不变
}
```

---

## 4. Data Model

None。本次变更不新增数据库表，不修改现有表结构。模型文件存储在文件系统：

```
%LocalAppData%/ReviewAnalysis/models/sensevoice-small/
├── manifest.json    # 文件清单 + SHA256
├── model.onnx       # ~150 MB
└── tokens.json      # ~50 KB
```

---

## 5. Business Logic

### 5.1 识别主流程（CompositeASREngine）

```
CompositeASREngine.RecognizeAsync(file, engSerViceType):
  foreach engine in _engines (按权重降序):
    result = await engine.RecognizeAsync(file, engSerViceType)
    if result["code"] == 0:
      return result
    else:
      log: "引擎 {name} 失败, code={code}, 降级到下一个"
  // 所有引擎均失败
  return {"code": 500, "data": null}
```

### 5.2 SenseVoiceSmallEngine 推理管道

```
SenseVoiceSmallEngine.RecognizeAsync(file, type):
  1. 检查 ModelManager.IsModelReady()
     → false: return {"code": 702}
  2. NAudio Mp3FileReader → float[] pcm (16kHz mono)
     → 异常: return {"code": 703}
  3. 检查音频时长 ≤ 60s
     → 超限: return {"code": 704}
  4. WavFrontend: PCM → FBank(80维) → LFR拼接 → float[T, 560]
  5. 解析 type 获取 lang ID 和 textnorm ID
  6. ONNX 推理:
     inputs = {speech, speech_lengths, language, textnorm}
     outputs = model.Run(inputs)
     → 异常: retry ≤2 次; 仍失败 return {"code": 500}
  7. GreedyDecode(outputs[0]) → int[] tokenIds
  8. TimestampLfr6(outputs[3], tokenIds) → int[][] [startMs, endMs]
  9. Token → 文本 + 词汇合并 (1-12字窗口)
     - 标点依附前词，继承前词结束时间
     - 语种标签切换 → 强制合并
  10. 组装 List<ASRResultEntity> → return {"code": 0, "data": [...]}
```

### 5.3 TencentASREngine 适配器

```
TencentASREngine.RecognizeAsync(file, type):
  1. 解析 type 中的原始腾讯云 EngSerViceType (如 "16k_zh")
  2. AsrApi.GetTempToken() → stsToken
  3. new MyCallableTask(file, stsToken, dir).StartAsTask(stsToken, modelType)
     → 返回 Dictionary<string, object>（格式不变）
  4. 直接返回结果
```

### 5.4 模型下载流程（ModelManager）

```
IsModelReady():
  1. 检查 %LocalAppData%/ReviewAnalysis/models/sensevoice-small/ 目录存在
     → 不存在: 触发异步下载, return false
  2. 读取 manifest.json → 逐文件 SHA256 校验
     → 任一失败: 删除损坏文件, 触发异步下载, return false
     → 全部通过: return true

DownloadAsync():
  1. HTTP GET manifest.json → 获取远端文件清单 + 最新 SHA256
  2. 逐文件:
     a. 检查本地 .part 文件 → 有则从已下载字节续传 (HTTP Range)
     b. 下载至 .part 临时文件
     c. SHA256 校验 → 通过则 rename .part → 正式文件; 失败则删除 .part 重试 (≤3次)
  3. 全部文件就绪 → 临时目录 rename → 正式目录 → 写入本地 manifest.json
  4. OnModelReady: 加载 ONNX Session
```

### 5.5 错误处理与降级分支

| 失败场景 | SenseVoiceSmallEngine 返回 | Composite 行为 |
|---------|:-------------------------:|---------------|
| 模型未就绪 (702) | `{code: 702}` | 降级到下一个引擎 |
| 音频解码失败 (703) | `{code: 703}` | 降级到下一个引擎 |
| 输入超限 (704) | `{code: 704}` | 降级到下一个引擎 |
| ONNX 推理异常 | `{code: 500}` | 降级到下一个引擎 |
| 所有引擎失败 | — | 返回 `{code: 500}` |

### 5.6 GPU 探针（应用启动时执行一次）

```
GpuProbe.Detect():
  providers = OrtEnv.Instance().GetAvailableProviders()
  if "CUDAExecutionProvider" in providers → HasCuda = true
  if "DmlExecutionProvider" in providers → HasDirectML = true
  if "CoreMLExecutionProvider" in providers → HasCoreML = true
  优先级: CUDA > DirectML > CoreML > CPU
  创建 SessionOptions 时 AppendExecutionProvider 按优先级
```

---

## 6. Non-Functional Constraints (Hard Constraints)

### 6.1 架构约束

- [MUST] 现有 `Asr/MyCallableTask.cs` 零改动
- [MUST] 现有 `Asr/ASRHttpUtils.cs` 零改动
- [MUST] BLL 层 (`AnchorVideoBll`, `UploadFileBll`) 零改动
- [MUST] `ASRResultEntity` 结构不变
- [MUST] 返回 `Dictionary<string, object>` 格式与腾讯云完全一致

### 6.2 并发约束

- [MUST] `SenseVoiceSmallEngine` 内部使用 `SemaphoreSlim(1)` 串行推理
- [MUST] 模型下载与推理互斥（下载完成前不加载 Session，推理中不触发下载）

### 6.3 安全约束

- [MUST] 模型 SHA256 校验不通过 → 删除文件，禁止加载
- [MUST] 模型下载必须 HTTPS
- [MUST] 禁止硬编码模型下载 URL 或密钥，必须从配置读取

### 6.4 禁止模式

- [NEVER] 子引擎内部调用其他引擎（引擎间互不感知）
- [NEVER] 同步阻塞 `.Result` / `.Wait()` 调用 ONNX 推理
- [NEVER] 在 `AsrEngineFactory` 构造函数中执行网络请求或模型加载（启动不阻塞）
- [NEVER] 修改 `MyCallableTask.StartAsTask()` 签名

### 6.5 回滚步骤

1. 配置 `engSerViceType` 改回 `"16k_zh"`
2. 无需重启，下次 `AnalysisVideo()` 自动使用纯腾讯云路径
3. 可选: 从 NuGet 移除 `Microsoft.ML.OnnxRuntime`（对现有功能无影响）

---

## 7. Acceptance Criteria (Testing)

### 7.1 Happy Path

**AC-1: 纯腾讯云模式（向后兼容）**
- Given: `engSerViceType = "16k_zh"`，一段 59s MP3 音频
- When: 调用 `AsrUtils.AsrByDirectoryPath(dir, token, "16k_zh")`
- Then: 走 `TencentASREngine`，返回结果与现有代码完全一致，`WordList` 可被 `WordApi.WordsMark()` 正常消费

**AC-2: 纯本地引擎模式**
- Given: 模型已就绪，`engSerViceType = "svs:auto:withitn"`，一段 59s 中文 MP3
- When: `SenseVoiceSmallEngine.RecognizeAsync(file, "svs:auto:withitn")`
- Then: 返回 `code=0`，`data` 包含有效 `ASRResultEntity` 列表，`WordList` 每条目 1-12 字

**AC-3: 本地优先+腾讯云兜底**
- Given: 模型已就绪，`engSerViceType = "svs:auto:withitn|tencent"`，一段 59s MP3
- When: `CompositeASREngine.RecognizeAsync(file, type)`
- Then: SenseVoiceSmall 成功 → 直接返回结果，`TencentASREngine` 不被调用

**AC-4: 自动降级（模型未就绪）**
- Given: 模型未下载，`engSerViceType = "svs:auto:withitn|tencent"`
- When: 触发识别
- Then: SenseVoiceSmall 返回 702 → Composite 降级调用 TencentASREngine → 返回腾讯云结果，业务层无感知

**AC-5: 模型首次下载**
- Given: `%LocalAppData%/ReviewAnalysis/models/sensevoice-small/` 目录不存在
- When: 首次调用 `SenseVoiceSmallEngine.RecognizeAsync()`
- Then: ModelManager 异步下载 model.onnx + tokens.json，下载完成 SHA256 校验通过，下次识别走本地引擎

### 7.2 Edge Cases

**AC-6: 空音频（纯静音）**
- Given: 一段 10s 完全静音的 MP3
- When: SenseVoiceSmallEngine 推理
- Then: 返回 `code=0`，`data` 为空列表或 `Text=""` 的条目，不抛异常

**AC-7: 超长音频（>60s）**
- Given: 一段 65s 的 MP3
- When: SenseVoiceSmallEngine.RecognizeAsync()
- Then: 返回 `code=704`，不执行 ONNX 推理

**AC-8: 损坏的 MP3**
- Given: 文件头损坏、NAudio 无法解码的 MP3
- When: SenseVoiceSmallEngine 解码步骤
- Then: 返回 `code=703`，不抛未捕获异常

**AC-9: 纯音乐（无人声）**
- Given: 一段 30s 纯背景音乐 MP3
- When: SenseVoiceSmallEngine 推理
- Then: 返回 `code=0`，`data` 为空列表或极短文本，不崩溃

**AC-10: 下载中断后续传**
- Given: model.onnx 下载到 50%，进程被 kill
- When: 下次启动，触发模型下载
- Then: ModelManager 检测到 .part 文件 → HTTP Range 从 50% 续传，不从头下载

**AC-11: SHA256 校验失败**
- Given: model.onnx 存在但内容损坏（SHA256 不匹配）
- When: ModelManager.IsModelReady()
- Then: 删除损坏文件 → 触发重新下载 → 下载完成校验通过 → 引擎就绪

**AC-12: 双引擎全部失败**
- Given: 本地模型损坏 + 同时断网，`engSerViceType = "svs:auto:withitn|tencent"`
- When: CompositeASREngine 遍历所有引擎
- Then: 最终返回 `code=500`，日志记录每个引擎的失败原因

### 7.3 性能验收

- [ ] GPU 环境: 单段 59s 推理 ≤ 2 秒
- [ ] CPU 环境: 单段 59s 推理 ≤ 25 秒
- [ ] 模型加载（首次 ONNX Session 创建）≤ 5 秒
- [ ] 进程内存增量（模型加载后）≤ 600 MB
- [ ] 应用启动时间不受模型是否就绪的影响（懒加载）

### 7.4 准确率验收

- [ ] 20 段测试音频（中/粤/英混合），SenseVoiceSmall vs 腾讯云，字错率 ≤ 5%
- [ ] 时间戳偏移 ≤ 100ms（与腾讯云对比）
