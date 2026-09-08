# ASR 语音转文字模块

> ⚠️ **本文档记录 2.6.0-for-local-asr 之前的单引擎（腾讯云）架构，仅作历史参考。**
>
> **2.6.0-for-local-asr 起，请以 [asr_engine_pipeline.md](asr_engine_pipeline.md) 为权威参考**，该文档涵盖双引擎架构（本地 SVS + 腾讯云 Composite）、完整配置语法、推理管道、约束与反模式。

本文档描述 ReviewAnalysis 的 ASR（Automatic Speech Recognition）语音转文字模块的完整架构与流程。所有 AI Agent 在处理 ASR 相关代码时 **必须** 参考本文档。

数据来源: `Asr/` 23 个文件 / `Bll/AnchorVideoBll.cs:537` / `Bll/UploadFileBll.cs:161` / `Utils/AudioUtils.cs` / 对话梳理

---

## 1. 架构总览

```
┌──────────────────────────────────────────────┐
│                   调用入口                      │
│  AnchorVideoBll.AnalysisVideo()    录播视频     │
│  UploadFileBll.AnalysisVideoOrAudio() 上传文件  │
└──────────────────┬───────────────────────────┘
                   │
    ┌──────────────┴──────────────┐
    │  FFmpeg 音频预处理            │
    │  AudioUtils.SlicingAudio()   │
    │  → 提取音轨 → 16kHz 单声道     │
    │  → 59秒分段 MP3 → PTS 归零    │
    └──────────────┬──────────────┘
                   │
    ┌──────────────┴──────────────┐
    │  ASR 识别编排                 │
    │  AsrUtils.AsrByDirectoryPath()│
    │  → 获取 STS 临时凭证          │
    │  → 逐段串行提交识别            │
    └──────────────┬──────────────┘
                   │
    ┌──────────────┴──────────────┐
    │  腾讯云 ASR API              │
    │  ASRHttpUtils.DoRequest()   │
    │  → SentenceRecognition      │
    │  → TC3-HMAC-SHA256 签名      │
    │  → 同步等待返回               │
    └──────────────┬──────────────┘
                   │
    ┌──────────────┴──────────────┐
    │  关键词/敏感词匹配            │
    │  WordApi.WordsMark()        │
    │  → 发送到后端服务              │
    │  → 返回 SentenceMarkVo       │
    └──────────────┬──────────────┘
                   │
    ┌──────────────┴──────────────┐
    │  结果持久化                   │
    │  本地 .txt (JSON) + SQLite   │
    └──────────────────────────────┘
```

---

## 2. 文件清单

### [Module] 核心 Asr 命名空间（`Asr/` 目录）

| 文件 | 职责 |
|------|------|
| `AsrUtils.cs` | 主编排器 — 遍历音频目录，逐段提交 ASR 识别 |
| `MyCallableTask.cs` | 单段音频识别任务 — Base64 编码 + 调用 API + 重试 |
| `ASRHttpUtils.cs` | 腾讯云 API 客户端 — TC3 签名 + `SentenceRecognition` + `CreateRecTask` |
| `ASRResultEntity.cs` | 识别结果实体 — Paragraph, FileName, Result, WordList, AudioDuration, Error |
| `ASRResultErrorEntity.cs` | 错误实体 — Code, Message |
| `ASRWordEntity.cs` | 词语实体 — Word, StartTime(ms), EndTime(ms) |
| `AudioTempTokenEntity.cs` | 临时 STS 凭证 — Token, TempSecretId, TempSecretKey |
| `CheckSurplusEntity.cs` | QPS 配额检查结果 |
| `SentenceMarkVo.cs` | 段落分析结果 — content, word items, keyword/sensitive word lists |
| `WordListItemVo.cs` | 词级标记 — word, startTime, endTime |
| `WordsMarkVo.cs` | 敏感词/关键词信息 — type, level, name, position, count |
| `RecordNeedsWordVo.cs` | 需要高亮的词位置 |
| `SyncVideoAnalysisBo.cs` | 同步视频分析数据到服务端 |
| `ReplayHttpUtils.cs` | 服务端通信 — Token 管理、结果同步、QPS 管理 |
| `TencentCosUtils.cs` | 腾讯云 COS 上传/下载 |
| `TencentVodUtils.cs` | 腾讯云 VOD 上传 |
| `QiNiuHttpUtils.cs` | 七牛云上传 |

### [Module] API 层

| 文件 | 职责 |
|------|------|
| `api/AsrApi.cs` | `GetTempToken()` — 从后端获取临时 STS 凭证 |
| `api/WordApi.cs` | `WordsMark()` — 发送识别文本到后端做关键词/敏感词匹配 |

### [Module] 业务逻辑层

| 文件 | 方法 | 职责 |
|------|------|------|
| `Bll/AnchorVideoBll.cs:537` | `AnalysisVideo(VideoEntity)` | 录播视频 ASR 分析入口 |
| `Bll/UploadFileBll.cs:161` | `AnalysisVideoOrAudio(...)` | 上传文件 ASR 分析入口 |

### [Module] 工具层

| 文件 | 方法 | 职责 |
|------|------|------|
| `Utils/AudioUtils.cs:24` | `SlicingAudio(...)` | FFmpeg 音频提取 + 分段 |
| `Utils/AnalysisUtils.cs:202` | `checkAsrError(...)` | ASR 错误码 → 用户友好中文提示 |

### [Module] 数据模型

| 文件 | 表/数据库 |
|------|----------|
| `Model/Audio.cs` | `review_analysis_audio.db` → `audio` 表 |
| `Model/AudioaAlysis.cs` | `review_analysis_audio_analysis.db` → `audio_analysis` 表 |
| `Model/UploadFileAudio.cs` | `upload_file_audio.db` |
| `Model/UploadFileAlysis.cs` | `upload_file_analysis.db` |

---

## 3. 完整流程（分步详解）

### Step 1: 视频格式统一

**文件**: `Bll/AnchorVideoBll.cs:544-549`

```csharp
// 将 ts/flv 视频转成 mp4
if (!video.storagePath.EndsWith(".mp4"))
{
    await WaitForMp4ConvertComplete(video.videoId);
    mp4Path = VideoUtils.ConvertToMP4(video.storagePath, video.videoName, platformType);
    
    // 检查 ts 和 mp4 时长一致性
    int duration = VideoUtils.CkeckTsAndMp4Consistent(video.storagePath, mp4Path);
    if (duration >= 60) { /* 标记录制网络异常状态 */ }
}
```

- [ASR] [MUST] 非 mp4 格式（ts/flv）先转换为 mp4
- [ASR] [MUST] 转换后校验 ts 和 mp4 时长一致性
- 转换期间自旋等待当前 videoId 的 MP4 转换完成

### Step 2: 音频切片

**文件**: `Utils/AudioUtils.cs:24-175`

```bash
# FFmpeg 命令 (line 52)
ffmpeg -i "{videoPath}" -vn \
  -af "aresample=async=1:min_hard_comp=0.100:first_pts=0" \
  -ar 16k -ac 1 -ab 96k \
  -f segment -segment_time 59 -reset_timestamps 1 \
  -segment_format mp3 -c:a libmp3lame \
  "{tempOutputDir}\audio_%03d.mp3"
```

关键参数：

| 参数 | 值 | 作用 |
|------|-----|------|
| `-vn` | — | 丢弃视频轨道 |
| `-ar 16k` | 16000 Hz | 采样率（腾讯云 16k_zh 模型要求） |
| `-ac 1` | 单声道 | 降低数据量 |
| `-ab 96k` | 96 kbps | MP3 比特率 |
| `-segment_time 59` | 59 秒 | 每段最大 59 秒（一句话识别 API 上限约 60 秒） |
| `-reset_timestamps 1` | **PTS 归零** | 每段内部时间戳从 0 开始 |
| `-c:a libmp3lame` | MP3 | 编码器 |

后处理：

- [ASR] [MUST] 删除 < 10KB 的音频片段（静音段）
- 使用临时目录避免路径中含 `%` 字符导致 FFmpeg 解析错误
- 超时时间 30 分钟，超时后强杀进程并抛异常

### Step 3: 编排识别任务

**文件**: `Asr/AsrUtils.cs:25-132`

```csharp
public static async Task<Dictionary<string, object>> AsrByDirectoryPath(
    string directoryPath, string token, string engSerViceType = "16k_zh")
{
    // 1. 扫描目录获取所有音频文件
    var files = Directory.EnumerateFiles(directoryPath, "*", SearchOption.AllDirectories)
                        .Select(p => new FileInfo(p)).ToList();
    
    if (files == null || files.Count == 0)
        return { code: 701, data: [] };  // 701 = 音频文件不存在

    // 2. 获取 STS 临时凭证（所有段共用）
    AudioTempTokenEntity audioTempTokenEntity = AsrApi.GetTempToken("");

    SemaphoreSlim semaphore = new SemaphoreSlim(1, 1);  // ⚠️ 强制串行

    // 3. 逐段提交
    foreach (var file in files)
    {
        var task = new MyCallableTask(file, token, directoryPath);
        
        var future = Task.Run(async () =>
        {
            await semaphore.WaitAsync();
            try { return task.StartAsTask(audioTempTokenEntity, engSerViceType); }
            finally { semaphore.Release(); }
        });

        var taskResult = await future;  // 等当前段完成
        if ((int)taskResult["code"] != 0)
            return result;  // ⚠️ 任一段失败 → 立即返回，剩余段丢弃
    }

    // 4. 按文件名排序 + 组装段落序号和文本
    asrResultList.Sort((x, y) => x.FileName.CompareTo(y.FileName));
    for (int i = 0; i < asrResultList.Count; i++)
    {
        asrResultList[i].Paragraph = i + 1;
        // 拼接 WordList 为纯文本
        foreach (var item in asrResultList[i].WordList)
        {
            if (Regex.IsMatch(item.Word, @"^[a-zA-Z'.]+$"))
                item.Word = item.Word + "  ";  // 英文单词加空格
            asrResultList[i].Result += item.Word;
        }
    }
}
```

**并发设计缺陷**：

- `SemaphoreSlim(1, 1)` — 硬编码为 1，强制串行
- 每段必须等待前一段 HTTP 响应返回才开始下一段
- 某段失败 → 立即返回，剩余段全部丢弃不做任何处理
- `Task.WhenAll` 在代码中存在但完全冗余（所有 Task 在此之前已 await 完毕）

### Step 4: 单段识别 + 重试

**文件**: `Asr/MyCallableTask.cs:35-166`

```csharp
public Dictionary<string, object> StartAsTask(
    AudioTempTokenEntity audioTempTokenEntity, string engSerViceType = "16k_zh")
{
    int qpsCount = 300;   // QPS 专用重试计数器
    int asrCount = 3;     // 通用错误重试计数器

    while (asrCount > 0)
    {
        // 1. 获取 STS 凭证（首次调用传 null 则自动获取）
        if (audioTempTokenEntity == null)
            audioTempTokenEntity = AsrApi.GetTempToken("");

        // 2. 读取音频文件 → Base64 编码
        byte[] fileContent = File.ReadAllBytes(Path.Combine(_directoryPath, _file.Name));
        bodyJsonObject.DataLen = fileContent.Length;
        bodyJsonObject.Data = Convert.ToBase64String(fileContent);

        // 3. 调用腾讯云 ASR API
        ASRResultEntity asrResultEntity = ASRHttpUtils.DoRequest(
            audioTempTokenEntity.TempSecretId,
            audioTempTokenEntity.TempSecretKey,
            audioTempTokenEntity.Token,
            bodyJsonObject,
            engSerViceType
        );

        // 4. 分类处理结果
        if (asrResultEntity == null)          → asrCount--, 等 5s, continue
        if (签名过期 AuthFailure.SignatureExpire) → 立即返回 code=601
        if (QPS 超限 RequestLimitExceeded)     → qpsCount--, 等 5s, continue
        if (其他 API 错误)                      → asrCount--, 等 5s, continue
        if (成功)                               → 返回 code=0 + ASRResultEntity
    }
    return { code: 500, data: { Result: "识别失败" } };
}
```

### Step 5: 腾讯云 API 调用

**文件**: `Asr/ASRHttpUtils.cs:219-271`

```
API:     POST https://asr.tencentcloudapi.com
Action:  SentenceRecognition
Version: 2019-06-14
签名:    TC3-HMAC-SHA256（手动实现，非 SDK）
```

请求参数：

| 参数 | 值 | 含义 |
|------|-----|------|
| `EngSerViceType` | `16k_zh`（默认） | 中文 16kHz 模型 |
| `SourceType` | `1` | 数据在请求体中（Base64） |
| `VoiceFormat` | `mp3` | 音频格式 |
| `WordInfo` | `2` | 返回词级时间戳（StartTime/EndTime，毫秒） |
| `Data` | Base64 字符串 | 音频数据 |
| `DataLen` | 整数 | 原始字节数 |

响应映射到 `ASRResultEntity`：

```
Response.Result        → ASRResultEntity.Result (纯文本)
Response.AudioDuration → ASRResultEntity.AudioDuration (音频时长，毫秒)
Response.WordList[]    → ASRResultEntity.WordList
  └─ Word              → ASRWordEntity.Word
  └─ StartTime         → ASRWordEntity.StartTime (毫秒)
  └─ EndTime           → ASRWordEntity.EndTime (毫秒)
Response.Error         → ASRResultErrorEntity (如果出错)
Response.RequestId     → ASRResultEntity.RequestId
```

**调用方式**：同步阻塞 — `Client.SendAsync(request).Result`

**备用 API**：`CreateRecTask`（录音文件识别，异步轮询模式）定义在 `ASRHttpUtils.cs:40-145`，但使用硬编码的测试凭证和固定 COS URL，**未被主流程调用**。

### Step 6: 关键词/敏感词匹配

**文件**: `api/WordApi.cs:32-93`

每段独立发送到后端服务：

```
POST {BaseUrl}/openapi/v2000/wordsMark

{
    "platformType": 1,          // 0:全平台 1:抖音 2:快手 3:视频号 4:小红书
    "videoId": "...",
    "tradeId": "...",
    "content": "识别的完整文本",
    "currentSort": 1,           // 段落序号 (1-based)
    "isLast": 0,                // 是否最后一段
    "type": 0,                  // 0:视频 1:文件
    "items": [
        { "Word": "大家", "StartTime": 1000, "EndTime": 1800 },
        ...
    ]
}
```

- [ASR] [MUST] 当 WordList 为空时填充占位词: `{ Word: "-", StartTime: 0, EndTime: 20 }`
- [ASR] [MUST] 每段独立请求，非批量

### Step 7: 结果持久化

**文件**: `Bll/AnchorVideoBll.cs:625-626`, `Utils/AnalysisUtils.cs`

- **本地文件**: `analysisData/video/{date}/{videoId}.txt`（JSON）
- **SQLite**: `audio_analysis` 表 — video_id, paragraph, status, data_json, trade_id
- **服务端**: 通过 `VideoApi` 更新视频分析状态 (0→1→2 成功 / 0→1→3 失败)
- **资源扣减**: 扣减 `aiAnalysisTime` 或 `textExtractionNum`

---

## 4. 重试策略详解

### [Retry] 两级计数器

| 错误类型 | 判断条件 | 重试次数 | 等待间隔 | 耗尽行为 |
|---------|---------|:----:|:----:|---------|
| QPS 超限 | `Error.Code == "RequestLimitExceeded"` | 300 | 5s | 返回 code=603 |
| 签名过期 | `Error.Code contains "AuthFailure.SignatureExpire"` | 0 | — | 立即返回 code=601 |
| 凭证获取失败 | `audioTempTokenEntity == null` | 3 | 5s | 返回 code=602 |
| API 返回 null | `asrResultEntity == null` | 3 | 5s | 返回 code=500 |
| 其他 API 错误 | 其他 Error.Code | 3 | 5s | 返回 code=500 |
| Exception | `catch (Exception)` | 3 | 无显式 sleep | 返回 code=500 |

### [Retry] 失败后不会重新入队

- 重试仅发生在 `MyCallableTask.StartAsTask()` 内存循环中
- 耗尽后返回失败码 → `AsrUtils` 立即退出 → BLL 层设置 `analysisStatus=3` (永久失败)
- **没有外层重扫机制**，status=3 的视频不会被任何后台任务重新拾取

---

## 5. 时间戳模型

### [Timestamp] 段内相对时间

```
FFmpeg: -reset_timestamps 1  →  每段 PTS 从 0 开始
腾讯云: WordInfo=2            →  返回词级偏移 (相对于当前段首)
客户端: 不做全局偏移计算        →  仅通过 Paragraph 区分段落
```

```
完整视频:
├─ audio_001.mp3 (0 ~ 59s)  Paragraph=1
│   ├─ "欢迎" StartTime=1000 EndTime=1800   ← 段内偏移
│   └─ "大家" StartTime=2000 EndTime=2500
├─ audio_002.mp3 (0 ~ 59s)  Paragraph=2  ← PTS 独立归零
│   └─ "今天" StartTime=500  EndTime=1000   ← 也是段内偏移
└─ ...
```

### [Timestamp] [WARNING] 无全局时间戳

客户端未计算视频级全局时间戳。如需展示词在完整视频中的位置，需自行计算：

```
globalMs ≈ (Paragraph - 1) × 59,000 + word.StartTime
```

但存在两个精度问题：

1. `-segment_time 59` 不是精确切 59 秒，会在最近关键帧处截断，每段实际时长可能短于 59 秒
2. FFmpeg `-reset_timestamps 1` 使 `AudioDuration` 只反映段实际有效音频长度

更准确的计算方式：**逐段累加前段的 `AudioDuration`**（腾讯云返回的段实际音频时长毫秒数）。

---

## 6. 错误码速查

| Code | 含义 | 触发条件 |
|:----:|------|---------|
| 0 | 成功 | 识别正常完成 |
| 500 | 识别失败 | 重试耗尽或 API 返回 null |
| 601 | 本地时间不正确 | `AuthFailure.SignatureExpire` |
| 602 | 临时凭证获取失败 | `AsrApi.GetTempToken()` 返回 null 重试 3 次仍失败 |
| 603 | QPS 已满 | `RequestLimitExceeded` 重试 300 次仍失败 |
| 701 | 音频文件不存在 | 目录中无音频文件 |

`AnalysisUtils.checkAsrError()` 将以上错误码映射为用户可读的中文提示。

---

## 7. 已知设计与待改进项

### [Design] [ISSUE] 伪并发 — 信号量硬编码为 1

`SemaphoreSlim(1,1)` 使并行架构退化为串行。一段 1 小时视频切约 60 段，每段 API 耗时约 2-3 秒，串行浪费约 2 分钟等待时间。腾讯云 API 通常允许 30+ QPS，可安全提升并发至 5-10。

### [Design] [ISSUE] 部分失败即全弃

某段识别失败 → `AsrUtils` 立即返回，剩余段不做处理。应改为：失败段记录后继续处理后续段，最终汇总成功/失败结果。

### [Design] [ISSUE] 无重试队列

status=3（失败）的视频永不被重拾。应增加定时扫描机制或手动重试入口。

### [Design] [ISSUE] 无视频级全局时间戳

时间戳仅为段内偏移。前端如需在视频播放器中同步定位，需要客户端或后端额外计算全局偏移。

### [Design] [ISSUE] `.Result` 同步阻塞

`ASRHttpUtils.DoRequest()` 使用 `.Result` 同步阻塞，在 UI 线程上下文中可能导致死锁。虽然当前通过 `Task.Run` 在后台线程执行缓解了风险，但仍不符合项目的 `[Async] [NEVER] 禁止 .Result` 规范。

### [Security] [ISSUE] 硬编码 AK/SK

`RecTaskRequest()` 方法中包含硬编码的腾讯云 AK/SK（`ASRHttpUtils.cs:65-67`），违反 `[Secret] [NEVER] 禁止硬编码密钥` 规范。应清理这些测试凭证。

---

## 8. 关联文档

- [SenseVoiceSmall 推理架构方案](asr_sensevoice_small.md) — 本地 ONNX 模型技术方案，任务向量配置，时间戳对齐策略
- [本地 ASR 引擎集成可行性评估](asr_local_engine_evaluation.md) — IASREngine 抽象设计，工程量分解，风险评估，推荐实施路径
