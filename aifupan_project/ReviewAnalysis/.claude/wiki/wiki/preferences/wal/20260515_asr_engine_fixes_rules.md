---
name: asr-engine-fixes-rules-20260515
description: ASR引擎代码审查提炼的规则 — HttpClient单例、Thread.Sleep、Word字段污染、死代码
metadata:
  type: feedback
---

## 本次修复提炼的工程规则 (2026-05-15)

### 规则 1: HttpClient 必须单例复用

**Why**: `ModelManager` 原在构造函数中 `new HttpClient`，`WarmupAsync` 内 `new ModelManager()` 造成 HttpClient 泄漏。

**How to apply**: 持有 HttpClient 的类，将其声明为 `private static readonly HttpClient`，或实现 `IDisposable`。WarmupAsync 等静态辅助方法不应 new 持有资源的实例。

### 规则 2: Thread.Sleep 禁止出现在 Task.Run 内部

**Why**: `Task.Run(() => methodWithThreadSleep())` 使调用方获得 async 外观，但实际仍占用线程池线程。高并发场景下多个 5s sleep 累积可触发线程池饥饿。

**How to apply**: 同步重试循环改为 async 方法 + `await Task.Delay`，直接 `await` 而无需 `Task.Run` 包装。

### 规则 3: 禁止 in-place 修改 DTO 字段用于格式化

**Why**: `AsrUtils` 原代码 `item.Word = item.Word + "  "` 永久污染了 `ASRWordEntity.Word`，下游（UI 展示、服务端接口、时间戳对齐）均读取到被污染的数据。

**How to apply**: 格式化拼接使用本地 `StringBuilder`，输出赋给独立字符串变量（如 `Result`），绝不修改传入的实体字段。

### 规则 4: CIF 时间戳对齐基准必须是 filtered token 顺序

**Why**: SenseVoiceSmall CIF fire point k 对应 filtered content token k，lang/textnorm token 不消耗 CIF fire。以 raw token 位置作为时间戳索引会造成系统性偏移。

**How to apply**: `ComputeTimestamps` 入参为 `filteredCount`，输出 `timestamps[i]` 与 `filteredTokens[i]` 直接对应，MergeWords 用 `i` 而非 original raw index 查找时间戳。

### 规则 5: 空 catch 至少记录完整异常

**Why**: 裸 `catch { return false; }` 隐藏异常来源，SHA256 校验失败无法区分"文件不存在"还是"JSON 解析错误"。

**How to apply**: `catch (Exception ex) { FileUtils.LogAnalysis($"...{ex.Message}"); }` — 至少记录 Message，必要时记录完整 ex。

### 规则 6: SemaphoreSlim 在串行 foreach+await 中无效

**Why**: `foreach + await` 本身串行执行，`SemaphoreSlim(1,1)` 每轮无竞争立即获得，净效果为零开销。

**How to apply**: 真正并发（多 Task 并行）场景才使用 SemaphoreSlim 限流；串行循环直接 await 即可。

### 规则 7: 权重/排序装饰在等效时应移除

**Why**: `EngineEntry.Weight = 10 - i*3` 降序排序产生与插入顺序完全相同的结果，引入了"权重可影响行为"的误导性认知。

**How to apply**: 排序/权重字段应有真实的分叉行为（不同 weight 产生不同顺序），否则移除，直接保持插入顺序。
