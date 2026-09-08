spec_mode: STANDARD

# FSMN-VAD 智能切片（替代 FFmpeg 59s 硬切）

## 1. Context

- **Business goal**: 在不依赖腾讯 ASR 兜底的前提下，让 SVS 单引擎下的识别质量在 CPU 主导环境下达到最大上限——通过 VAD 智能切片，把 59s 硬切边界（每段丢 1-3 字 + 单字被切成两半）换成自然静音点切分，提升识别连贯性。
- **Scope of change**:
  - `Asr/Local/ModelManager.cs` — 扩展支持 VAD 模型下载（model.onnx + vad.yaml + vad.mvn）
  - `Asr/Local/FsmnVadEngine.cs` — **新建** — FSMN-VAD 推理封装（参考 FunASR `AliFsmnVadSharp`）
  - `Asr/Local/SmartSlicer.cs` — **新建** — 基于 VAD 段输出的智能切分算法
  - `Utils/AudioUtils.cs` — 改造 `SlicingAudio`：先解码全 PCM → VAD → 智能切分 → 输出 wav 文件
  - `Program.cs` / `ModelManager.WarmupAsync` — 启动时一并预热 VAD 模型
  - `wiki/preferences/wal/20260526_asr_bgm_preservation_rules.md` — 新增"VAD 不得做内容过滤，只可做切分"约束
- **Dependencies**:
  - `depends_on: [../domain/asr_engine_pipeline.md]`
  - `depends_on: [../domain/asr_consumers.md]`
  - `depends_on: [../preferences/wal/20260526_asr_bgm_preservation_rules.md]`
  - 外部参考: `modelscope/FunASR runtime/csharp/AliFsmnVad/` (Apache 2.0)
  - 模型: `https://modelscope.cn/models/iic/speech_fsmn_vad_zh-cn-16k-common-onnx`（**国内必达**）

## 2. Domain Model

新增术语：

| 术语 | 定义 |
|---|---|
| **FsmnVadEngine** | 阿里达摩院 FSMN-Monophone VAD 的 C# ONNX 推理封装；返回 speech segment 起止 ms |
| **VadSegment** | `{ StartMs: long, EndMs: long }` — 一段 VAD 检测出的语音活动区间 |
| **SmartSlicer** | 基于 VadSegment[] + 总时长 + 最大段长，规划音频切分点的算法组件 |
| **Silence Gap** | 两个相邻 VadSegment 之间的非语音区间（含 BGM/静音/音效） |
| **Smart Cut Point** | SmartSlicer 选定的切分点；尽量落在 Silence Gap 中点 |

状态机：无（无状态算法 + 单例 VAD 引擎）。

## 3. API Contract (Handoff)

无对外接口变更。`IASREngine.RecognizeAsync` / `AsrUtils.AsrByDirectoryPath` 签名不变。`AudioUtils.SlicingAudio(string videoPath, string outputDirectoryPath)` 签名不变，仅内部实现改造。

## 4. Data Model

无。模型文件本地落盘，不入 SQLite。新增本地路径：
- `%LocalAppData%/ReviewAnalysis/models/fsmn-vad/model.onnx`
- `%LocalAppData%/ReviewAnalysis/models/fsmn-vad/vad.yaml`
- `%LocalAppData%/ReviewAnalysis/models/fsmn-vad/vad.mvn`
- 总大小 ~5MB

## 5. Business Logic

### 5.1 总流程

```
SlicingAudio(videoPath, outputDirectoryPath):
  ① FFmpeg 解码视频 → 单文件 16kHz mono PCM WAV（不切片）
     滤镜链保持当前：aresample + highpass + dynaudnorm
  ② 加载 PCM float[] 到内存
  ③ FsmnVadEngine.GetSegments(pcm) → VadSegment[]
     - 若模型未就绪 → 降级到旧的 -f segment 59s 硬切（兼容路径）
     - 若 VAD 推理失败 → 同上
  ④ SmartSlicer.PlanCuts(vadSegments, totalDurationMs, maxChunkMs=58000)
       → List<(long startMs, long endMs)> 切分点列表
  ⑤ 按切分点用 FFmpeg 截取每段，写入 audio_000.wav, audio_001.wav, ...
  ⑥ 沿用现有 <1KB 文件丢弃逻辑（不变）
```

### 5.2 SmartSlicer 算法

```
PlanCuts(speechSegments, totalDurationMs, maxChunkMs=58000):
  chunks = []
  chunkStart = 0
  while chunkStart < totalDurationMs:
    targetEnd = chunkStart + maxChunkMs
    if targetEnd >= totalDurationMs:
      # 最后一段
      chunks.add((chunkStart, totalDurationMs))
      break

    # 找 [chunkStart+45s, chunkStart+58s] 区间内最大/最居中的 silence gap
    candidateGaps = silence gaps with midpoint ∈ [chunkStart+45000, chunkStart+58000]
    if candidateGaps is empty:
      # 无静音可切 → 硬切到 maxChunkMs (与当前行为一致)
      cutAt = chunkStart + maxChunkMs
    else:
      # 选距离 targetEnd 最近的 gap 中点
      cutAt = closest gap's midpoint to targetEnd

    chunks.add((chunkStart, cutAt))
    chunkStart = cutAt

  return chunks
```

**关键约束**：
- maxChunkMs = 58000（留 2s 余量给 SVS 60s 硬限）
- minCutWindow = 45000（不低于 45s，避免段过短导致 SVS 加载开销摊不开）
- 切点优先取 silence gap 中点（远离语音活动边缘）

### 5.3 BGM 保留合规（核心）

**SmartSlicer 只决定"在哪里切"，不决定"哪些段送 SVS"**。所有段都进 ASR 管道，包括纯 BGM 区间。

**禁止**在本 spec 实现中：
- ❌ 跳过被 VAD 标为非语音的段
- ❌ 把 VAD 输出当成 SVS 输入的"白名单"
- ❌ 任何基于 VAD 输出的内容过滤

VAD 仅用于切分位置决策。

### 5.4 降级路径（fallback）

| 触发条件 | 降级行为 |
|---|---|
| VAD 模型未下载完成 | 走旧 `-f segment 59s` 硬切 |
| VAD ONNX 加载失败 | 同上 |
| VAD GetSegments 抛异常 | 同上 + 记日志 |
| SmartSlicer 算法异常 | 同上 + 记日志 |
| 视频时长 < 60s | 直接走旧 `-f segment`（无需 VAD） |

所有降级都**不破坏**现有功能，最坏情况退化到 (a) 之前的行为。

### 5.5 模型分发

- 主源（默认）：`https://modelscope.cn/api/v1/models/iic/speech_fsmn_vad_zh-cn-16k-common-onnx/repo?Revision=master&FilePath=...`
- 配置覆盖：`%LocalAppData%/ReviewAnalysis/Config/fsmn_vad_config.json`（参考现有 `svs_model_config.json`）
- 下载流程：复用 `ModelManager` 模式（断点续传 + SHA256 + manifest）
- 启动预热：`Program.cs` 调用 `ModelManager.WarmupVadAsync()` 后台静默下载

### 5.6 日志

新增诊断日志（per [诊断规约](../preferences/wal/20260526_asr_diagnostics_and_threshold_rules.md)）：
- `[VAD] 模型加载完成, 耗时={ms}ms`
- `[VAD] 检测完成: 总时长={ms}ms, 语音段={N}个`
- `[Slicer] 智能切分: total={ms}ms → {N} 段, 切点 [{ms},{ms},...]`
- `[Slicer] 降级: 原因={reason}` （若 fallback 触发）

## 6. Non-Functional Constraints (Hard Constraints)

### 6.1 性能（CPU 主导环境）

| 项 | 预算 |
|---|---|
| VAD 推理 RTF | < 0.01（60s 音频 < 600ms） |
| VAD 模型内存 | < 50MB RAM |
| VAD 模型首次加载 | < 2s |
| 切片总耗时（含 VAD） | 比旧版增加 < 10% |

### 6.2 模型分发

- **必须**默认源国内可达（per [[asr-vad-model-china-access]]）→ ModelScope
- **禁止**单依赖 `huggingface.co` / `github.com/.../releases`
- 总下载量 < 10MB
- 必须支持断点续传（参考 ModelManager 现状）

### 6.3 BGM 保留合规

- **禁止**任何基于 VAD 标签的内容过滤（per [[asr-bgm-preservation-policy]]）
- VAD 仅用于切分位置；所有段必须送 SVS

### 6.4 并发与线程安全

- FsmnVadEngine 单例（类似 SenseVoiceSmallEngine）
- 内部 SemaphoreSlim(1,1) 串行推理
- ONNX session 在 _initLock 保护下首次加载

### 6.5 禁止模式

- ❌ 引入 NAudio（per 反模式清单）
- ❌ `Math.Exp` 在 inner loop（VAD 输出已经是 segment，无需 softmax）
- ❌ Thread.Sleep / .Result / 空 catch / async void

### 6.6 回滚

- 单次 commit 包含所有改动
- 回滚 = `git revert <commit>`
- 模型文件保留在磁盘（不删，下次启用直接用）
- 数据库无变化，无需迁移

## 7. Acceptance Criteria (Testing)

**Mac 环境约束**：不编译，QA 走 csharp-code-review；Windows 运行时验证移交用户。

### 7.1 静态审查 Checklist

- [ ] csharp-code-review 全 PASS（含反模式 / 资源管理 / 空安全）
- [ ] FsmnVadEngine 结构对照 FunASR `AliFsmnVadSharp` 一致
- [ ] SmartSlicer 算法纸面追踪 5 个边界用例：见 §7.3
- [ ] AudioUtils.SlicingAudio 降级路径不破坏旧行为
- [ ] ModelManager 新增 VAD 下载支持，主源是 modelscope.cn
- [ ] 无 BGM 保留违规（grep 确认无"vad.IsSpeech ? continue : process"类逻辑）

### 7.2 Windows 端 6 类样本 A/B（per [短视频验证规则](../preferences/wal/20260526_asr_short_video_validation_rules.md)）

| # | 样本 | 验收标准 |
|---|---|---|
| 1 | 主播游戏直播（基线） | 段数 ≤ 旧版；识别质量 ≥ 旧版；跨段拼合触发次数下降（说明边界改善） |
| 2 | 用户清晰人声 | 识别率不退化 |
| 3 | 短视频 BGM + 人声 | 人声完整保留 |
| 4 | **短视频 + 纯 AI 配音** | **AI 配音必须完整保留**（与 afftdn 撤回案例同等标准） |
| 5 | 短视频字幕同步 | 边界字归位 |
| 6 | **短视频 + 纯 BGM 含歌词**（无人声） | **BGM 歌词必须保留**（验证 SmartSlicer 没跳过非语音段） |

**任何 ≥ 1 项退化即视为回归**。

### 7.3 SmartSlicer 算法分支覆盖

| 用例 | 输入 | 预期 |
|---|---|---|
| 标准 | 120s 音频，语音段 [[0,55000],[58000,120000]] | 切点 ~56500ms 在 silence gap 中点；2 段 |
| 无静音 | 120s 音频，语音段 [[0,120000]] | 硬切到 58000，2 段 |
| 全静音 | 120s 音频，无语音段 | 硬切到 58000，2 段（BGM 仍送 SVS）|
| 多 gap | 多个 gap 在 45-58s 窗内 | 选最接近 55s 的 gap 中点 |
| 短音频 | < 60s 音频 | 不调 SmartSlicer，直接走旧硬切路径 |

### 7.4 不在本次范围（明确出界）

- ❌ VAD 内容过滤 / 非语音段跳过
- ❌ 候选 A（事件 token VAD）—— 已排除
- ❌ 候选 D（跨段拼合）调整 —— (a) 已实施，本次不动
- ❌ ModelManager 整体重构 —— 仅扩展支持 VAD 模型
- ❌ 替换 SVS 引擎或换其他 ASR 模型

### 7.5 量化健康指标（Windows 端 A/B 必出）

- `[Slicer] 智能切分: total=XXXms → N 段` —— N 应接近 `ceil(total/55000)`
- `[VAD] 检测完成: 语音段=Y 个` —— 长视频 Y ≥ 10 是健康
- `[Slicer] 降级: 原因=X` —— 健康样本下不应出现
- `[ASR] 跨段拼合共 X 处` —— 应比 (a) 单独时下降 50%+（说明边界更好）
- `[SVS] 诊断 ... content/decoded` —— 应持平或略升
