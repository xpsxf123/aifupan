# ASR「加速」机制 — 内部设计与集成指南

> 适用版本：2.6.2.1-asr 分支起
> 关联：`docs/API-录播ASR加速-前端对接.md`（前端接口）、`.claude/runs/20260710_asr-accelerate-force-tencent/openspec.md`（设计契约）
> 读者：客户端 C# 研发 —— 维护本机制、或希望在其它业务（短视频 / 用户上传 / 未来新消费方）复用「在飞切腾讯」加速能力的开发者

---

## 1. 背景与动机

ASR 有两条链路，瓶颈性质不同（详见 `.claude/runs/20260710_asr-concurrency-analysis/research_report.md`）：

| | 本地 ASR (SenseVoiceSmall) | 腾讯云 ASR |
|---|---|---|
| 瓶颈 | CPU/GPU **计算密集** | **网络 IO 密集** |
| 并发 | 全进程单例 + `SemaphoreSlim(1,1)` 串行 | 引擎无共享态、可并发 |
| 分片处理 | `foreach + await` 串行，一段段跑 | 单请求即出结果，wall-clock 更短 |

**问题**：本地识别一个长录播是「分片串行」，用户等待期长。**加速**让用户在识别进行中一键把**剩余分片**改走腾讯云，用「网络并发快」换「本地计算慢」，快速出结果。

**核心设计约束（已固化）**：
- 只切**剩余未识别分片**，已完成分片结果保留（省腾讯额度、不浪费已完成工作）。
- **不中断**当前正在推理的分片（ONNX 中途取消支持有限，且无必要）。
- 信号有效期**严格锁定在 ASR 识别循环真正运行的窗口内**，杜绝僵尸信号污染后续重分析。

---

## 2. 架构总览

```
前端 (加速按钮)
   │  GET /api/anchorvideo/accelerate?videoId=X
   ▼
AnchorVideoController.Accelerate(videoId)          [UI/Controller 层：仅委托]
   │  return AnchorVideoBll.RequestAccelerate(videoId)
   ▼
AnchorVideoBll.RequestAccelerate(videoId)          [BLL 层：薄委托]
   │  return AsrAccelerateSignal.TryRequest(videoId)
   ▼
AsrAccelerateSignal   ◄──────────────────────────┐ [Asr 层：进程级信号注册表]
   │  _running / _signals (ConcurrentDictionary)   │
   │                                               │ MarkRunning / IsRequested /
   ▼                                               │ MarkStopped / Clear
AsrUtils.RunLocalEngineAsync(...,videoId)  ────────┘ [Asr 层：分片识别循环，信号生产/消费点]
```

调用方向严格向下，符合四层架构。

---

## 3. 核心组件：`AsrAccelerateSignal`

进程级、线程安全（全 `ConcurrentDictionary`）的信号注册表。位置：`Asr/AsrAccelerateSignal.cs`。

| 方法 | 语义 | 调用方 |
|---|---|---|
| `MarkRunning(videoId)` | 标记该 videoId 的 ASR 识别循环**已开始**（进入运行窗口）| `RunLocalEngineAsync` 进入分片循环前 |
| `MarkStopped(videoId)` | 标记识别循环**已结束**（退出运行窗口，先于 Clear，先拒后续请求）| `RunLocalEngineAsync` 的 `finally` |
| `TryRequest(videoId)` | **仅当处于运行窗口内**才登记加速信号并返回 `true`；否则 `false` 且不登记 | BLL（前端触发）|
| `IsRequested(videoId)` | 该 videoId 是否已请求加速 | `RunLocalEngineAsync` 分片循环顶部 |
| `Clear(videoId)` | 清除加速信号 | `RunLocalEngineAsync` 的 `finally` |

**为什么用「运行窗口」而不是上层 `isAnalysis` 标志**：`isAnalysis`/`currentAnalysisId` 在 ASR **之前**就置真、在 ASR 之后好几个 await（WordsMark、状态更新等）才置假。若用它把关，会在「ASR 已跑完但 isAnalysis 仍为真」的窗口里接受加速请求并登记信号，而识别循环的 `finally` 早已执行、清不到它 → **僵尸信号残留**，等该视频下次重分析时第一片就被静默强制走腾讯。用 `_running` 精确锁定 ASR 执行窗口，从根上杜绝此类残留（此为 QA 阶段发现并修复的缺陷，见 delivery_capsule.md）。

---

## 4. 消费点：`RunLocalEngineAsync` 分片循环

位置：`Asr/AsrUtils.cs`。伪码：

```csharp
AsrAccelerateSignal.MarkRunning(videoId);      // 进入运行窗口
try
{
    var currentEngine = engine;
    bool switched = false;
    foreach (var file in files)                // 分片已全部切好落盘
    {
        // 检测点在循环顶部 → 当前分片先自然跑完，下一分片才换引擎（等当前分片跑完再切）
        if (!switched && AsrAccelerateSignal.IsRequested(videoId))
        {
            currentEngine = _engineFactory.Value.Build(engSerViceType,
                                new List<string> { "tencent" });  // 仅腾讯链
            switched = true;                   // 一次锁定，避免每轮重建
            FileUtils.LogAnalysis($"[ASR] 加速触发，剩余分片切 Tencent, videoId={videoId}");
        }
        var taskResult = await currentEngine.RecognizeAsync(file, engSerViceType);
        // ... 已识别分片结果原样累加 ...
    }
}
finally
{
    AsrAccelerateSignal.MarkStopped(videoId);  // 先拒绝后续请求
    AsrAccelerateSignal.Clear(videoId);        // 再抹掉最后一刻挤进来的信号
}
```

关键点：
- **检测点在循环顶部**：当前 `await RecognizeAsync` 自然先完成，下一分片才看到信号 → 「等当前分片跑完再切」，无需中断 ONNX。
- **`switched` 一次锁定**：切换后不再每轮 `Build`。
- **`finally` 双清**：`MarkStopped` + `Clear` 覆盖成功/失败/异常三条路径。

---

## 5. 时序（正常加速）

```
识别循环开始 ──► MarkRunning(X)
   分片1 (SVS) ──完成──► 累加结果
   [前端点加速] ──► TryRequest(X): _running含X → 登记信号, accepted=true
   分片2 顶部: IsRequested(X)=true → 切腾讯, switched=true
   分片2 (腾讯) ──完成──► 累加结果
   分片3 (腾讯) ──完成──► ...
识别循环结束 ──► finally: MarkStopped(X) + Clear(X)
```

「过早」（识别未开始）或「过晚」（识别已结束）点加速：`_running` 不含 X → `TryRequest` 返回 `false`，不登记，无副作用。

---

## 6. 在其它业务复用加速（集成步骤）

任何走 `AsrUtils.AsrByDirectoryPath` 的消费方（短视频 / 用户上传 / 未来新消费方）都可复用，成本很低。

### 6.1 前置条件（是否值得加速）
- 该消费方走的是**含本地 SVS 的链路**（纯腾讯链本就快，无需加速）。
- 识别对象是**多分片长音频**（分片越多，剩余分片切换收益越大）。

### 6.2 三步接入

1. **透传 job 唯一标识**：调用 `AsrByDirectoryPath` 时传入该任务的唯一 id（录播用 `videoId`，其它业务可用自己的任务 id）：
   ```csharp
   await AsrUtils.AsrByDirectoryPath(audioDir, token, engType, consumer,
                                     forceTencentOnly: false,
                                     videoId: yourJobId);   // ← 传入唯一 id
   ```
   > `MarkRunning/MarkStopped/Clear` 已内建在 `RunLocalEngineAsync` 内，**无需额外处理运行窗口**。只要传了 id，运行窗口就自动管理。

2. **暴露一个触发端点**（Controller → BLL → `AsrAccelerateSignal.TryRequest`）：
   ```csharp
   // Controller（仅委托）
   [HttpGet("加速识别", "/accelerate")]
   public Dictionary<string, object> Accelerate(string jobId)
       => new Dictionary<string, object> { { "accepted", new YourBll().RequestAccelerate(jobId) } };

   // BLL（薄委托）
   public bool RequestAccelerate(string jobId) => AsrAccelerateSignal.TryRequest(jobId);
   ```

3. **前端在该业务的「识别中」状态调用**该端点，按 `accepted` 反馈。

### 6.3 就这些
`AsrAccelerateSignal` 是全局单例、按 id 键入，天然支持多消费方并存（各自 id 不冲突）。无需改动 `AsrAccelerateSignal` 或 `RunLocalEngineAsync`。

---

## 7. 约束与非目标（务必知悉）

| 项 | 说明 |
|---|---|
| **不重做已完成分片** | 只切剩余分片；已识别分片保留其原引擎结果 |
| **不中断在飞分片** | 生效延迟 ≤ 1 个分片（≤60s）|
| **进程内存态** | 信号不持久化，进程重启丢失（重启后按默认引擎路由）|
| **单 id 单在飞** | 同一 id 同一时刻只应有一个识别循环（录播天然单槽；其它业务若并发多任务，务必保证 id 唯一）|
| **结果引擎混排** | 加速后同一任务结果可能是「SVS 前段 + 腾讯后段」拼接；两引擎均返回 chunk-relative 时间戳，下游拼接逻辑一致，但需知悉质量/风格可能有细微差异 |
| **腾讯额度** | 切腾讯会占用单账号 QPS 额度；大量任务同时加速可能触发限流退避（见 research_report §F7）|
| **依赖运行窗口** | 若某消费方**不**经 `RunLocalEngineAsync`（例如自建识别循环），则不会自动 `MarkRunning`，`TryRequest` 恒为 false —— 需自行调用 `MarkRunning/MarkStopped` 包裹其循环 |

---

## 8. 相关文件

| 文件 | 角色 |
|---|---|
| `Asr/AsrAccelerateSignal.cs` | 信号注册表（本机制核心）|
| `Asr/AsrUtils.cs` | `AsrByDirectoryPath`（入口，`videoId` 参数）+ `RunLocalEngineAsync`（生产/消费点）|
| `Bll/AnchorVideoBll.cs` | `RequestAccelerate`（录播委托）+ 录播调用点透传 `video.videoId` |
| `Controller/AnchorVideoController.cs` | `Accelerate` 端点（录播）|

---

## 9. 扩展方向（备忘）

- **开始前点加速 = 整段走腾讯**：当前「识别未开始」返回 false；若产品希望切片阶段点加速就让全片走腾讯，可在 `RunLocalEngineAsync` 首片前也检测信号（需把运行窗口标记提前到切片阶段，或另设 pending 语义）。
- **key 泛化**：`AsrAccelerateSignal` 已按字符串 id 键入，与「videoId」无耦合，可直接承载任意业务 job id。
- **可观测性**：如需统计加速命中率，可在 `TryRequest`/切换点埋点。
