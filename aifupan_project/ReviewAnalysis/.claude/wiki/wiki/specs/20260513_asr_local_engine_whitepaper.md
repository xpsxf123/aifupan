# 本地语音识别引擎 — 方案设计与技术白皮书

> 2026-05-13 · v1.0

---

## 一句话概述

我们计划在 ReviewAnalysis 中集成阿里巴巴达摩院的 SenseVoiceSmall 语音识别模型，让应用在没有网络连接的情况下也能完成语音转文字。本文档面向产品、架构、工程团队，全面阐述为什么做、怎么做、以及做完之后会带来什么改变。

---

## 阅读导航

如果你是以下角色，建议按标注的优先级阅读：

| 章节 | 产品 | 架构 | 开发 | 测试 |
|------|:--:|:--:|:--:|:--:|
| [一、我们要解决什么问题](#一我们要解决什么问题) | ★★ | ★★ | ★ | ★ |
| [二、做完之后有什么变化](#二做完之后有什么变化) | ★★ | ★★ | ★ | ★ |
| [三、整体方案长什么样](#三整体方案长什么样) | ★ | ★★ | ★★ | ★ |
| [四、引擎如何编排与降级](#四引擎如何编排与降级) | ★ | ★★ | ★★ | |
| [五、本地引擎内部怎么工作](#五本地引擎内部怎么工作) | | ★★ | ★★ | ★ |
| [六、模型文件怎么下载与管理](#六模型文件怎么下载与管理) | ★ | ★ | ★★ | |
| [七、不同硬件环境下表现如何](#七不同硬件环境下表现如何) | ★★ | ★ | ★ | ★ |
| [八、可以配置哪些识别策略](#八可以配置哪些识别策略) | ★★ | ★ | ★★ | |
| [九、出错了怎么办——降级与回滚](#九出错了怎么办降级与回滚) | ★★ | ★★ | ★ | ★ |
| [十、要做多少工作，分几步走](#十要做多少工作分几步走) | ★★ | ★★ | ★★ | |
| [十一、有什么风险](#十一有什么风险) | ★★ | ★★ | | ★ |
| [十二、怎么才算做完](#十二怎么才算做完) | ★★ | | ★ | ★★ |

---

## 一、我们要解决什么问题

### 背景

ReviewAnalysis 目前的语音识别功能完全依赖腾讯云服务。每次分析视频时，系统先把视频切成几十段 MP3 音频片段（每段最长 59 秒），然后一段一段地通过 HTTP 请求发送到腾讯云的 `asr.tencentcloudapi.com` 接口，等待云端返回识别结果。

这条链路在过去几年运行得不错，但随着业务量增长，暴露了三个越来越尖锐的问题。

### 问题一：网络不可靠，识别就不可靠

```
应用服务器 ──HTTP──→ 腾讯云 ASR API
              ↑
         这中间任何环节出问题
         整段音频的识别就会失败
```

腾讯云 API 调用需要经过 DNS 解析、TCP 建连、TLS 握手、签名认证等至少 4 个网络环节。任何一个环节出现波动——比如客户机房的出口带宽被其他业务抢占、运营商的 DNS 间歇性超时、甚至腾讯云某个可用区的负载均衡器临时过载——都会导致本次识别调用失败。

现状是失败后会进入重试队列，但重试策略有上限（QPS 类错误最多重试 300 次，通用错误最多 3 次）。一旦重试耗尽，这段视频的分析结果就会带上一个 "status=3" 的失败标记，下游的敏感词匹配和持久化链路虽然能正常走下去，但用户在前端看到的就是这部分内容"识别失败"，而且系统不会自动重拾这些失败段。

### 问题二：QPS 配额是天花板

腾讯云的语音识别接口有每秒调用次数（QPS）上限。在平台同时分析多场直播回放的高峰期，多个视频的音频片段同时排队调用 API，很容易触发 QPS 限制。被限速的请求会收到错误码 603，然后进入长达 300 次的重试循环——每次重试间隔 5 秒。最坏情况下一个片段要等 25 分钟才能拿到结果（甚至拿不到）。

### 问题三：成本随业务量线性增长

目前按调用次数计费。每场直播回放会产生大量音频片段（一场 2 小时的直播 ≈ 120 个片段），每段都是一次 API 调用。随着接入的直播间数量增加，API 费用也在同步攀升。

### 我们的目标

用一个**本地的、离线可用的**语音识别引擎来替代（或部分替代）腾讯云 API。这个本地引擎运行在应用所在的机器上，不依赖任何外部网络服务，同时保持与腾讯云 API 完全一致的输入输出格式，让上层业务代码一行不改就能享受离线识别能力。

核心原则只有四条：

1. **现有腾讯云路径一行代码不改。** 我们不会去动 `MyCallableTask`、`ASRHttpUtils` 这些现有文件。
2. **上层业务不感知底层引擎。** `AnchorVideoBll` 和 `UploadFileBll` 不需要知道今天是谁在做识别。
3. **本地引擎挂了，自动切回腾讯云。** 不能让本地引擎成为新的单点故障。
4. **模型文件可以远程下载，支持断点续传和完整性校验。** 用户不需要手动拷贝几百兆的模型文件。

---

## 二、做完之后有什么变化

### 成本侧

| 项目 | 现在（纯腾讯云） | 改造后 | 
|------|:---:|:---:|
| 单段音频的 API 调用次数 | 1 次 | 0 次（走本地引擎时） |
| 音频数据上传带宽 | 每段上传 Base64 编码（约原始大小的 1.3 倍） | 0（仅在本地处理） |
| STS 临时凭证请求 | 每次识别都要获取 | 0（本地引擎不需要凭证） |
| 模型下载流量 | 0 | 首次约 150MB，后续仅增量 |

**简单来说：凡是走了本地引擎的识别，API 费用直接归零。** 如果业务配置为"本地优先，腾讯云兜底"，那么绝大部分识别会免费，只有本地引擎失败的极少数片段才会触发 API 调用。

### 稳定性侧

| 维度 | 现在 | 改造后 |
|------|------|------|
| 对互联网的依赖 | 完全依赖 | 仅模型首次下载和引擎退化为腾讯云时需要 |
| QPS 限制 | 有，高峰期被限 | 本地引擎无 QPS 概念 |
| 失败后的恢复能力 | status=3 即为终态，无自动恢复 | 本地引擎失败自动走腾讯云；模型损坏自动修复 |
| 单段识别延迟 | 取决于网络 RTT + 云服务排队 | GPU 环境通常 <2 秒；CPU 环境 8-25 秒 |

### 可扩展性

引擎接口设计为通用抽象。后续如果想接入其他语音识别模型（比如 OpenAI Whisper、阿里的 Paraformer），只需要写一个新的引擎类，实现同一个接口，然后加到配置里即可。所有的 BLL 层代码、音频切片、关键词匹配、结果持久化——全都不用改。

---

## 三、整体方案长什么样

### 从一个"黑盒"视角看

对上层业务来说，整个改造就是一个**替换操作**。原来调用 `MyCallableTask`，现在调用一个叫 `IASREngine` 的接口，仅此而已。

```
改造前:
  BLL → AsrUtils → MyCallableTask → 腾讯云 HTTP

改造后:
  BLL → AsrUtils → IASREngine ─┬─ TencentASREngine（包装原 MyCallableTask）
                               └─ SenseVoiceSmallEngine（本地 ONNX）
```

BLL 层的 `AnchorVideoBll.AnalysisVideo()` 和 `UploadFileBll` 完全不用改。它看到的仍然是 `Dictionary<string, object> {"code": int, "data": List<ASRResultEntity>}` 这个返回格式。

### 三个核心部件

我们用三个新增的 C# 类来承载整个引擎体系：

**① IASREngine — 引擎接口**

只定义了唯一一个方法：
```csharp
Task<Dictionary<string, object>> RecognizeAsync(FileInfo audioFile, string engSerViceType);
```

输入一个已经切好的 MP3 音频文件，输出腾讯云格式的标准结果字典。不管底层是本地 ONNX 推理还是 HTTP 调用，接口不变。

**② CompositeASREngine — 组合器（引擎编排）**

它自己也实现了 `IASREngine` 接口，但内部并不自己做识别。它持有一个有序的引擎列表（按权重从高到低排列），收到识别请求后依次尝试列表中的引擎，谁先成功就用谁的结果。

```
CompositeASREngine 内部逻辑:
  对列表中的每个引擎:
    调它的 RecognizeAsync()
    if 返回 code == 0:
      直接返回这个结果（不再尝试后面的）
    else:
      记一条日志，继续试下一个
  全部失败:
    返回 code=500
```

这个设计的关键在于：**每个子引擎只管做好自己的识别工作，不需要知道自己有没有"备胎"。** 降级逻辑全部内聚在 CompositeASREngine 里。

**③ AsrEngineFactory — 工厂（引擎制造）**

应用启动时，Factory 的构造函数会预先创建所有可用的引擎实例：

```csharp
public AsrEngineFactory()
{
    _enginePool["tencent"] = new TencentASREngine();    // 腾讯云适配器
    _enginePool["svs"]      = new SenseVoiceSmallEngine(); // 本地 ONNX 引擎
}
```

注意，`SenseVoiceSmallEngine` 的构造函数本身**不会立即加载 ONNX 模型**（模型文件可能还没下载）。它内部持有 `ModelManager`，模型下载和加载采用懒加载策略——在第一次真正需要做识别时才触发。

业务代码调用时，只需要告诉 Factory 你想要什么样的引擎组合：

```
Build("svs:auto:withitn|tencent")
  → 解析出两个子引擎: SenseVoiceSmall (权重10) + Tencent (权重7)
  → 从池中取出已创建实例
  → 包装成 CompositeASREngine 返回
```

至此，一次业务调用的完整链路就清晰了：

```
应用启动
  → AsrEngineFactory 构造函数（预创建引擎实例，不阻塞）
  
用户触发视频分析
  → AnchorVideoBll.AnalysisVideo()
  → FFmpeg 切片（不变）
  → AsrEngineFactory.Build("svs:auto:withitn|tencent")
    → 返回 CompositeASREngine
  → 对每段 MP3 调用 engine.RecognizeAsync(file)
    → SenseVoiceSmallEngine（先试）
    → 失败 → TencentASREngine（兜底）
  → 拿到 ASRResultEntity 列表
  → WordApi 关键词匹配（不变）
  → 持久化（不变）
```

---

## 四、引擎如何编排与降级

### 配置语法设计

我们复用现有的 `engSerViceType` 这个字符串参数来承载引擎选择逻辑，而不是新增一个配置项。这样对已有调用方的改动最小。

```
简单模式（向后兼容）:
  "16k_zh"            → TencentASREngine，行为和原来一模一样

单引擎模式:
  "svs:auto:withitn"  → 只用本地 SenseVoiceSmall，不降级

双引擎降级模式（推荐生产配置）:
  "svs:auto:withitn|tencent"
    → 先试 SenseVoiceSmall，失败自动走腾讯云

反转优先级:
  "tencent|svs:zh:withitn"
    → 腾讯云优先，本地兜底
```

管道符号 `|` 左侧的引擎权重更高（值为 10），右侧依次递减（值为 7、4、1...）。`svs` 后面的冒号分隔参数 `lang:itn` 是 SenseVoiceSmall 的专属配置，告诉模型用什么语种策略和是否输出标点。

### 降级是如何自动发生的

假设我们配置了 `"svs:auto:withitn|tencent"`，以下是三种典型场景：

**场景 A：一切正常（最常见的情况）**
```
CompositeASREngine
  → 试 SenseVoiceSmallEngine → 识别成功 → 返回结果
  → TencentASREngine 压根不会被调用
```

**场景 B：模型还没下载完成**
```
CompositeASREngine
  → 试 SenseVoiceSmallEngine → ModelManager 发现模型未就绪 → 返回 code=500
  → 记日志："svs engine not ready, downgrading to tencent"
  → 试 TencentASREngine → 识别成功 → 返回结果
```
这个过程对用户完全透明——他们不会看到"模型正在下载"的提示，视频分析正常完成。

**场景 C：模型已下载但推理过程出错**
```
CompositeASREngine
  → 试 SenseVoiceSmallEngine → ONNX 推理抛异常 → 返回 code=500
  → 记日志："svs engine inference failed, downgrading to tencent"
  → 试 TencentASREngine → 识别成功 → 返回结果
```

**场景 D：两个引擎都失败了**
```
CompositeASREngine
  → 试 SenseVoiceSmallEngine → 失败
  → 试 TencentASREngine → 失败（比如同时断网了）
  → 返回最终错误 code=500
```

### 为什么不把降级逻辑写在 SenseVoiceSmallEngine 内部

这是我们设计时的一个重要取舍。直觉上可能会在 SenseVoiceSmall 引擎内部 try-catch，失败了直接调腾讯云。但这样做有两个问题：

1. **耦合。** SenseVoiceSmall 需要知道 TencentASREngine 的存在，两个引擎互相依赖。以后加第三个引擎（比如 Whisper），需要同时修改 SenseVoiceSmall 和 TencentASREngine。
2. **不可扩展。** 如果未来想配置"先试 SenseVoiceSmall，再试 Whisper，最后才试腾讯云"这种三级降级链，硬编码逻辑完全无法支持。

Composite 模式把编排和降级抽出来作为独立职责，每个子引擎各司其职，组合方式完全由配置文件决定。

---

## 五、本地引擎内部怎么工作

SenseVoiceSmallEngine 是本地识别的核心实现。它不依赖任何网络请求，纯粹在应用进程内完成从音频到文字的转换。

### 处理管道（6 步）

```
MP3 音频文件
    │
    ▼
[步骤1] NAudio 解码
    Mp3FileReader 读取 MP3 文件
    → 输出: 16kHz 单声道 PCM 浮点数组
    │
    ▼
[步骤2] FBank 特征提取 (WavFrontend.cs)
    短时傅里叶变换(FFT) → 80个梅尔频率滤波器 → 对数幅度谱
    → LFR 拼接: 将相邻帧堆叠成 560 维特征向量
    → 输出: float[N, 560]
    │
    ▼
[步骤3] ONNX 推理 (model.onnx)
    将特征输入 ONNX 图，同时传入 language 和 textnorm 标量 ID
    → 模型输出 3-4 个张量:
      [0] token logits — 每个时间步的词汇概率
      [1] sequence lengths — 有效序列长度
      [3] CIF peaks — CIF 激活值（用于计算时间戳）
    │
    ▼
[步骤4] 贪婪解码
    对每个时间步取最大概率的 token ID
    → 输出: int[] tokenIds
    │
    ▼
[步骤5] 时间戳计算
    TimestampLfr6(cifPeaks, tokenIds)
    → 利用 CIF 峰值确定每个 token 的起止时间
    → 输出: int[][] [startMs, endMs]（帧率 60ms 粒度）
    │
    ▼
[步骤6] Token → 文本 + 词汇合并
    tokenizer 解码为文本
    → 按 1-12 字窗口合并为 WordList
    → 标点符号依附于前一个词
    → 输出: ASRResultEntity
```

### 关于 FBank 特征提取——一个差点踩到的坑

在早期的方案中，我们把 FBank 特征提取标记为"最高风险项"。原因很简单：SenseVoiceSmall 模型期望的输入不是原始 PCM 音频，而是经过 FBank 变换 + LFR 拼接后的 560 维特征向量。这个计算过程涉及 FFT、梅尔滤波器组、对数运算等多个数学步骤，如果实现精度和 Python 参考代码有差异，识别准确率会显著下降。

我们原本计划从 FunASR 的 Python 代码手工移植到 C#。但在调研过程中发现，阿里巴巴 FunASR 项目的 GitHub 仓库（`alibaba-damo-academy/FunASR`）的 `runtime/csharp/` 目录下已经有一个完整的 C# 实现了——包括 `WavFrontend.cs`（FBank 特征提取）和 `OfflineProjOfSenseVoiceSmall.cs`（ONNX 推理封装）。这些代码是由社区开发者 manyeyes 贡献并合入官方仓库的，已经过验证。

**这意味着我们不需要从零实现 FBank，直接复用即可。** 这是本次方案中最大的风险消除项。

### 关于模型文件——一个好消息

SenseVoiceSmall 的 ONNX 模型有两种导出方式：旧版需要单独的 `embed.onnx` 文件来生成 task embedding，新版将这些 embedding 逻辑内建到了主模型中，language 和 textnorm 以简单整数标量的方式直接传入 ONNX 图。

我们在 ModelScope 上找到的 `iic/SenseVoiceSmall-onnx` 属于**新版**。它的 ONNX 输入节点如下：

| 输入节点 | 维度 | 内容 |
|---------|------|------|
| `speech` | [1, T, 560] | FBank+LFR 特征 |
| `speech_lengths` | [1] | 有效帧数 |
| `language` | [1] | 语种 ID（整数: 0=auto, 3=zh, 4=en...） |
| `textnorm` | [1] | 文本规制 ID（14=withitn, 15=woitn） |

不用额外维护 `embed.onnx` 这个文件，模型部署清单更简单：只需要 `model.onnx`（~150MB）和 `tokens.json`（~50KB）。

### CIF 时间戳机制

SenseVoiceSmall 的时间戳不是简单的"第 N 个 token × 60ms"这种线性估算。它使用一种叫 CIF（Continuous Integrate-and-Fire）的机制：

模型在每个解码步输出一个 CIF 激活值 `cif_peak`。当累积激活值达到一定阈值时，模型"发射"一个 token。通过对所有时间步的 CIF 峰值进行检测，可以精确反推每个 token 对应的音频起止时间。

算法实现在 `TimestampLfr6()` 函数中，精度为帧级（60ms），比简单的索引乘法准确得多。

---

## 六、模型文件怎么下载与管理

150MB 的模型文件不可能打包在应用安装包里，也不能让用户手动下载放到某个目录。我们设计了一套自动化的模型生命周期管理系统。

### 存储位置

模型存放在用户级应用数据目录，不污染项目文件：

```
C:\Users\<用户名>\AppData\Local\ReviewAnalysis\
└── models\
    └── sensevoice-small\
        ├── manifest.json      ← 文件清单 + SHA256 哈希
        ├── model.onnx         ← 主模型（~150MB）
        └── tokens.json        ← 词汇表（~50KB）
```

### 懒加载策略

应用启动时不会下载模型，也不会加载 ONNX 引擎。模型下载和 Session 加载被推迟到**第一次真正需要识别**的时刻（也就是首次调用 `SenseVoiceSmallEngine.RecognizeAsync()` 时）。

这样做的优点：
- 应用启动速度不受影响
- 如果用户一直配置为纯腾讯云模式，模型永远不会被下载，不浪费磁盘和带宽
- SenseVoiceSmallEngine 在 Factory 中可以被预先创建（零依赖），但不会阻塞启动

### 下载流程详解

```
第一次需要本地识别时:
  │
  ├─ 检查本地模型目录是否存在
  │   └─ 不存在 → 触发下载
  │
  ├─ 检查 manifest.json 是否完整
  │   └─ 缺失 → 触发下载
  │
  ├─ 逐文件校验 SHA256
  │   ├─ 全部通过 → 加载 ONNX Session → 引擎就绪
  │   └─ 任一失败 → 删除损坏文件 → 触发下载
  │
  └─ 下载过程:
      ├─ 从服务端拉取 manifest.json（获取最新文件清单和哈希）
      ├─ 逐文件以 HTTP GET 请求下载，写入 .part 临时文件
      ├─ 如果下载中断 → 下次启动时通过 HTTP Range 头续传
      ├─ 下载完成后 SHA256 校验
      │   ├─ 通过 → .part 重命名为正式文件
      │   └─ 失败 → 删除 .part，重试（最多 3 次）
      ├─ 全部文件下载完毕 → 原子替换（临时目录 rename 为正式目录）
      └─ 写入本地 manifest.json → 触发 OnModelReady
```

### 下载期间的降级行为

这是设计中最巧妙的部分。模型下载是异步进行的，期间如果有 ASR 请求进来：

```
SenseVoiceSmallEngine.RecognizeAsync()
  → ModelManager.IsModelReady() 返回 false
  → 直接返回 {code: 500}
  → CompositeASREngine 收到失败 → 自动尝试下一个引擎
  → TencentASREngine 正常执行 → 返回结果
```

对用户来说，视频分析照常进行，他们完全感知不到"模型正在下载"这件事。下载完成后，下一个片段的识别就会自动走本地引擎。

### 损坏自愈

每次调用 `IsModelReady()` 都会逐文件做 SHA256 校验（不是简单的"文件是否存在"检查）。这意味着：

- 如果磁盘坏道损坏了某个文件 → 校验不通过 → 自动删除 → 触发重新下载
- 如果用户手动删除了 model.onnx → 下次识别时发现缺失 → 自动下载
- 如果 manifest 版本落后于服务端 → 自动增量更新

整个模型管理对外暴露的只有一个状态：引擎就绪 / 未就绪。上层代码完全不需要关心模型是怎么下载的、存在哪里、有没有损坏。

---

## 七、不同硬件环境下表现如何

SenseVoiceSmall 的 ONNX 运行时支持多种硬件后端。我们在应用启动时会执行一次 GPU 探针探测，自动选择当前机器上最优的推理加速器。

### 探针逻辑

```
启动时:
  → 查询 ONNX Runtime 可用的执行提供程序
  → 按优先级选择: CUDA > DirectML > CoreML > CPU
  → 创建对应的 SessionOptions，传入 InferenceSession
  → 记录日志: "ASR engine provider=CUDA, device=NVIDIA GeForce RTX 3060"
```

### 各硬件环境下的预期表现

下表数据基于参考实现的估算，实际表现以 POC 阶段的实测为准：

#### GPU 环境（推荐）

| 显卡 | 后端 | 单段(59s)推理耗时 | 显存占用 | 并发能力 |
|------|------|:---:|:---:|:---:|
| NVIDIA RTX 3060/4060 (12GB) | CUDA | 0.5 ~ 1.5 秒 | ~1.5 GB | 6-8 路 |
| NVIDIA GTX 1650 (4GB) | CUDA | 2 ~ 4 秒 | ~1.5 GB | 1-2 路 |
| Intel Arc / AMD Radeon | DirectML | 3 ~ 6 秒 | ~1.5 GB | 视显存 |
| Apple M1/M2/M3 | CoreML | 2 ~ 5 秒 | ~1.0 GB | 视统一内存 |

#### 纯 CPU 环境

| CPU | 后端 | 单段(59s)推理耗时 | 内存占用 |
|-----|------|:---:|:---:|
| Intel i7-12700 / i7-13700 | MLAS (CPU) | 8 ~ 15 秒 | ~600 MB |
| Intel i5-12400 | MLAS (CPU) | 15 ~ 25 秒 | ~600 MB |

### 对业务的影响

GPU 环境下延迟表现优秀（<2 秒），完全适合生产使用。

CPU 环境下延迟 8-25 秒相对较高，但注意这是**单段**的耗时。在实际的视频分析场景中，一段 2 小时的直播会产生约 120 个 MP3 片段，如果全部走 CPU 推理，总耗时可能达到 15-50 分钟。

**因此我们的建议是**：生产环境优先使用有 NVIDIA 显卡的机器运行，CPU 模式作为无 GPU 环境下的兜底方案（此时腾讯云可能更快，配置为 `"svs:auto:withitn|tencent"` 即可让腾讯云担主要负载）。

### 并发控制

无论哪种硬件环境，目前都采用 `SemaphoreSlim(1)` 的保守串行策略，即同一时刻只允许一段音频进行 ONNX 推理。GPU 环境如果后续发现有需求，可以放宽并发数（RTX 3060 12GB 显存理论上可支撑 6-8 路并发）。

---

## 八、可以配置哪些识别策略

SenseVoiceSmall 模型支持通过任务向量（Task Vector）来调整识别行为。在项目中，我们通过配置字符串来控制这些参数。

### 语种选择

| 配置值 | 效果 | 适用场景 |
|--------|------|---------|
| `auto` | 自动识别（支持中/粤/英混读） | **通用场景，推荐默认值** |
| `zh` | 强制中文普通话 | 纯中文直播，防止粤语/英语误判 |
| `en` | 强制英语 | 跨境电商、英文直播间 |
| `yue` | 强制粤语（输出繁体） | 粤语专用场景（需额外繁转简） |
| `nospeech` | 纯静音检测 | 判断音频是否含有效人声 |

### 文本输出格式

| 配置值 | 效果 |
|--------|------|
| `withitn` | **标点 + 数字格式化**（中文数字→阿拉伯数字，自动加标点） |
| `woitn` | 纯文本（无标点，保留原始数字表达） |

### 推荐组合

```
# 生产环境默认配置（对标腾讯云 16k_zh）
svs:auto:withitn

# 纯中文直播
svs:zh:withitn

# 需要原始数据的科研/分析场景
svs:auto:woitn

# 本地优先 + 腾讯云兜底
svs:auto:withitn|tencent
```

`withitn` 模式同时集成了文本规整化（日期、金额、百分比的标准化），无需额外配置。`event` 和 `emotion` 两个维度由模型内部硬编码处理，不可配置也不需配置。

---

## 九、出错了怎么办——降级与回滚

### 三层防护

本方案提供三个级别的故障应对机制：

**第一层：引擎内重试**

SenseVoiceSmallEngine 在遇到 ONNX 推理异常时会自动重试最多 2 次。这主要应对瞬时的内存分配失败或 GPU 驱动抖动。

**第二层：CompositeASREngine 自动降级**

如果本地引擎重试耗尽仍未成功，CompositeASREngine 自动切换到管道中的下一个引擎。这个过程对上层透明，用户看到的结果就是"识别正常完成"。

**第三层：配置级回滚**

运维人员（或自动化系统）可以通过修改配置，让整个应用回到纯腾讯云模式，无需重启：

```
"svs:auto:withitn|tencent"  →  改成 "16k_zh"

下一次视频分析时，AsrEngineFactory.Build("16k_zh") 
会直接返回 TencentASREngine 的单例，不再尝试本地引擎。
```

### 新错误码

本地引擎不会产生腾讯云特有的网络错误（601 签名过期、603 QPS 超限），但它引入了自己的错误类型：

| 错误码 | 含义 | 触发示例 |
|:---:|------|---------|
| 500 | 所有引擎都失败 | 本地推理异常 + 同时断网 |
| 702 | 模型文件问题 | model.onnx 缺失且下载失败 |
| 703 | 音频解码失败 | MP3 文件损坏，NAudio 无法解析 |
| 704 | 输入超出限制 | 音频超过 60 秒 |

### 回滚检查清单

如果真的需要完全回退到改造前的状态：

- [ ] 配置 `engSerViceType` 改为 `"16k_zh"`
- [ ] 验证视频分析流程正常完成
- [ ] 验证识别结果格式无变化（WordApi 能正常消费）
- [ ] 验证不产生 ONNX 相关的日志和错误

---

## 十、要做多少工作，分几步走

### 工作量

| 任务 | 说明 | 难度 |
|------|------|:--:|
| 定义 `IASREngine` 接口 | 约 15 行 | 小 |
| 实现 `CompositeASREngine` | 权重链式调用，约 50 行 | 小 |
| 实现 `TencentASREngine` | 包装现有 MyCallableTask，零改动 | 小 |
| 实现 `AsrEngineFactory` | 实例池 + Build 编排，约 60 行 | 小 |
| 集成 FBank 特征提取 | 复用 FunASR 官方 WavFrontend.cs | 小 |
| 集成 ONNX 模型加载 | 复用 FunASR 官方 OfflineModel.cs | 小 |
| 实现 `SenseVoiceSmallEngine` | ONNX 推理 + 后处理 + 词汇合并，约 200 行 | 中 |
| 实现 `ModelManager` | 下载/校验/续传/原子替换，约 150 行 | 中 |
| 实现 `GpuProbe` | 硬件加速器检测，约 70 行 | 小 |
| 集成 NAudio 音频解码 | MP3→PCM，约 30 行 | 中 |
| 修改 `AsrUtils` | 注入 IASREngine | 小 |
| 修改 `AnalysisUtils` | 追加 702/703/704 中文错误映射 | 小 |

**总代码量约 600 行（新增），4-6 个工作日**（比早期估算少 2-3 天，因为 FBank 特征提取可以直接复用官方代码）。

### 实施分三个阶段

```
Phase 1: 概念验证 (1天)
  └─ 目标: 证明"能跑通"
      ├── 引入 ONNX Runtime NuGet
      ├── 集成 WavFrontend.cs + OfflineProjOfSenseVoiceSmall.cs
      ├── 准备一段 59s MP3 测试片段
      ├── 跑通完整的"MP3→识别→ASRResultEntity"链路
      └── 和腾讯云返回结果做人工对比

      决策门: 识别准确率 ≥ 腾讯云 95% → 进入 Phase 2

Phase 2: 工程集成 (2-3天)
  └─ 目标: 把能力变成产品
      ├── 实现完整的接口体系（IASREngine / Composite / Factory）
      ├── 实现 ModelManager（下载 + 校验 + 生命周期）
      ├── 实现 GpuProbe（硬件检测）
      ├── 改造 AsrUtils（注入引擎）
      ├── 追加错误码映射
      └── 配置机制对接

Phase 3: 测试验证 (1-2天)
  └─ 目标: 确保不崩
      ├── 端到端集成测试（真实直播回放文件）
      ├── 边界测试（空音频 / 超长音频 / 纯音乐 / 纯噪音）
      ├── 降级测试（手动删除模型 → 走腾讯云）
      ├── 断点续传测试（下载中途杀进程 → 重启续传）
      └── 准确率对比报告（20 段 × 本地 vs 腾讯云）
```

---

## 十一、有什么风险

### 已经消除的风险

**FBank 特征提取精度问题**（原最高风险）—— FunASR 官方 C# 实现可直接复用，无需从 Python 手工移植。风险等级从 HIGH 降为 NONE。

### 仍需关注的风险

| 风险 | 严重度 | 为什么值得关注 | 应对 |
|------|:---:|------|------|
| ONNX Runtime 与客户环境不兼容 | 中 | 部分老旧 Windows 系统缺少 VC++ 运行时，ONNX Runtime 可能加载失败 | CPU 版本兜底（纯 C API）；加载失败自动降级腾讯云 |
| CPU 推理延迟偏高 | 中 | 纯 CPU 环境单段 15-25s，120 段累计耗时近 1 小时 | GPU 环境是推荐方案；CPU 环境通过配置多用腾讯云 |
| 内存增加约 600MB | 低 | 加载 ONNX 模型后进程内存从 ~400MB 涨到 ~1GB | 懒加载（不用不占）；32GB 内存已是主流配置 |
| GPU 显存不足以并发多路 | 低 | GTX 1650 仅 4GB，单模型占 1.5GB，剩余不足支撑第二路并发 | 维持 SemaphoreSlim(1) 串行策略，先保证稳定性 |
| 模型文件下载失败 | 中 | CDN 不可达、磁盘空间不足（~200MB 需求） | 下载前检查磁盘空间；下载失败自动降级；最多 3 次重试 |

---

## 十二、怎么才算做完

### 功能验收

- [ ] `SenseVoiceSmallEngine` 能成功识别一段 59 秒的 MP3（中英粤混合内容）
- [ ] 输出的 `ASRResultEntity` 能被下游 `WordApi.WordsMark()` 正常消费
- [ ] 重新配置引擎类型为 `16k_zh` 后，行为和改造前完全一致
- [ ] `TencentASREngine` 的端到端结果与现有代码无差异
- [ ] WordList 每条目字数在 1-12 之间，标点正确依附前词

### 降级验收

- [ ] 手动删除 `model.onnx` → 下一次识别自动降级到腾讯云，不报错
- [ ] 配置 `"svs:auto:withitn|tencent"` → 本地引擎正常时走本地，失败时走腾讯云
- [ ] 模型正在下载时触发识别 → 标记为未就绪，走腾讯云

### 模型管理验收

- [ ] 本地无模型文件 → 首次调用自动触发下载
- [ ] 下载中途模拟网络断开 → 重启后断点续传
- [ ] SHA256 校验不匹配 → 自动删除重新下载
- [ ] 手动删除单个文件 → 自动补全
- [ ] 应用退出时保留 `.part` 进度 → 下次继续

### 性能验收

- [ ] GPU 环境（如有）：单段 59s 推理 ≤ 2 秒
- [ ] CPU 环境：单段 59s 推理 ≤ 25 秒
- [ ] 模型加载时间（从磁盘读取到首 token 推理）≤ 5 秒
- [ ] 应用启动时间不受模型存在与否的影响
- [ ] 现有腾讯云路径的识别延迟无变化（TencentASREngine 零开销适配）

---

## 附录：技术选型说明

| 层次 | 我们选了 | 为什么没选别的 |
|------|:---:|------|
| 推理引擎 | ONNX Runtime | PyTorch 子进程 — 跨进程开销大，需装 Python 环境；TensorRT — 仅 NVIDIA，需额外转换 |
| 音频解码 | NAudio (NuGet) | FFmpeg Process — 进程启动开销；BASS — 商业授权限制 |
| FBank 特征 | 复用 FunASR 官方 C# | 手工移植 Python — 费时且精度风险高（已验证不必） |
| 模型下载 | HttpClient Range 续传 | 第三方下载器 — 引入额外依赖 |
| 配置机制 | 复用 `engSerViceType` 字段 | 新增配置项 — 增加调用方改动 |

---

## 相关文档

| 文档 | 定位 |
|------|------|
| [ASR 模块总览](../domain/asr_module.md) | 现有腾讯云 ASR 的完整流程文档 |
| [SenseVoiceSmall 推理架构](../domain/asr_sensevoice_small.md) | 模型推理的技术细节与 GPU 探针 |
| [可行性评估](../domain/asr_local_engine_evaluation.md) | 工程风险评估与迁移计划 |
| [统一 PRD+架构 (旧版)](20260513_asr_local_engine_prd.md) | 原始架构规格（较技术化） |
