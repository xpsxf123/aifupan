---
name: asr-gpu-ep-fallback-rules
description: SVS 本地 ASR 的 ONNX Runtime GPU EP（DirectML）配置规则、进程级 sticky CPU 兜底设计与 ORT 升级部署陷阱
metadata:
  type: rules
---

## 规则：本地 ASR ONNX Runtime GPU EP 配置与兜底（2026-05-23）

**背景**：本地 SVS 引擎（SenseVoiceSmall）从 CPU-only ORT 切换到 DirectML EP 以利用客户端 GPU。本次实战发现 4 条非显然约束，覆盖客户端从 NVIDIA 独显到 Intel 集显到无 GPU 全场景。

---

### 规则 1：DirectML EP 必须显式禁用 MemoryPattern

**Why**：ORT 硬约束。`AppendExecutionProvider_DML` 后若 `EnableMemoryPattern=true`（默认），`new InferenceSession(...)` 直接抛 `Mem pattern should be disabled for using DirectML execution provider`，整个 session 构造失败。CPU 和 CUDA 不受影响。

**How to apply**：仅在 DirectML 分支显式关闭，避免影响 CPU 性能：
```csharp
case Provider.DirectML:
    options.EnableMemoryPattern = false;
    options.AppendExecutionProvider_DML(DeviceId);
    break;
```

实际位置：`Asr/Local/GpuProbe.cs::ApplyTo()`。

---

### 规则 2：DirectML EP 必须配 CPU 兜底 EP + 降图优化级别

**Why**：DirectML EP 对部分 fused 算子覆盖不全（实测 SenseVoice self-attention 的带 mask Softmax 在 DML 1.12 / 1.13 上仍可能失败）。ORT 默认行为：跑不动的算子**直接抛 RuntimeException**，不会自动 fallback CPU；同时 `GraphOptimizationLevel.ORT_ENABLE_ALL` 会触发末段融合（如 Attention+Softmax 融合算子），DML 兼容性更差。

**How to apply**：DirectML EP 永远配双保险——
```csharp
case Provider.DirectML:
    options.EnableMemoryPattern = false;
    // 降级图优化避免 DML 不兼容的最终融合
    options.GraphOptimizationLevel = GraphOptimizationLevel.ORT_ENABLE_EXTENDED;
    options.AppendExecutionProvider_DML(DeviceId);
    // CPU 兜底：节点级 fallback，DML 跑不动的算子静默落 CPU 而非抛异常
    options.AppendExecutionProvider_CPU();
    break;
```

**注**：这只解决节点级别 fallback。**Run() 整体抛异常**仍可能发生（如本次的 Softmax），需配合规则 3 的进程级 sticky 兜底。

---

### 规则 3：进程级 sticky GPU 禁用模式（双层兜底设计）

**Why**：客户机器五花八门（NVIDIA / AMD / Intel 集显 / 无 GPU / 老驱动），GPU 失败无法在编译期排除。但**第一次失败后**继续每次都"试 GPU → 失败 → 降 CPU" 每次浪费 1-3s 推理时间且产生大量误导性错误日志。设计：失败一次→本次进程内永远走 CPU；应用重启后再尝试一次（驱动更新 / 系统重启可自愈）。

**How to apply**：进程级原子标记 + 单例引擎重建模式。

```csharp
// GpuProbe.cs — 进程级闸门
public static class GpuRuntimeState
{
    private static int _disabled;
    public static bool GpuDisabled => Volatile.Read(ref _disabled) != 0;
    public static void DisableGpuForProcess(string reason)
    {
        if (Interlocked.Exchange(ref _disabled, 1) == 0)
            FileUtils.LogAnalysis($"[GpuProbe] GPU 已禁用 (本次进程内): {reason}");
    }
}

// GpuProbe ctor 第一行检查标记
public GpuProbe()
{
    if (GpuRuntimeState.GpuDisabled) { BestProvider = Provider.Cpu; return; }
    // ...正常探测
}

// 引擎层 RunInference 失败路径
catch (Exception ex)
{
    var failedProvider = _offlineModel.ActiveProvider;
    if (failedProvider != GpuProbe.Provider.Cpu)
    {
        GpuRuntimeState.DisableGpuForProcess(ex.Message);
        RebuildOfflineModelForcedCpu();  // 重建 model（新 GpuProbe → BestProvider=Cpu）
        // 本次再试一次 CPU
    }
}
```

**关键设计点**：
- **进程级**（非持久化）：重启自愈，避免把瞬时驱动故障变永久状态
- **幂等触发**：`Interlocked.Exchange` 保证日志只打一次
- **先建后销**：`RebuildOfflineModelForcedCpu` 先 new 新 model 再 dispose 旧，避免读者看到 null
- **必须持锁调用**：rebuild 必须在 `_inferenceLock` 内执行，否则与并发 RunInference 竞态

**搭配 OfflineModel 构造期内置兜底**：构造期 GPU session 失败（如驱动崩溃）由 `OfflineModel.cs:24` 的 catch 兜底重建 CPU；运行期 Run() 失败由本规则兜底。两层互不冲突。

---

### 规则 4：ORT NuGet 升级必须清理 bin/obj 后 rebuild

**Why**：ORT 的 native `onnxruntime.dll` 由 NuGet 包的 targets 文件复制到 `bin/Debug/`。MSBuild 在某些配置下"newer 文件不覆盖"，**升级 NuGet 后旧版 native dll 残留** → 新 managed binding (1.17.x) 加载旧 native (1.16.x) → `NativeMethods` 静态 ctor 在 `GetApi(ORT_API_VERSION)` 取不到匹配 API 表 → NRE → 整个 ASR 进程启动直接崩。栈顶特征：
```
System.NullReferenceException
  at Microsoft.ML.OnnxRuntime.NativeMethods..cctor()
  at Microsoft.ML.OnnxRuntime.SessionOptions..ctor()
```

**How to apply**：所有 ORT NuGet 包版本升级 SOP——
1. 关闭所有正在跑的 `*.exe`（防文件占用）
2. VS 菜单 **生成 → 清理解决方案**
3. **手动**删除项目根 `bin/` 和 `obj/` 整个文件夹（VS 的"清理"不删 native dll）
4. **工具 → NuGet 包管理器 → 程序包管理器控制台** 跑 `Update-Package Microsoft.ML.OnnxRuntime.DirectML -Reinstall`
5. **生成 → 重新生成解决方案**（必须 Rebuild，不是 Build）
6. 验证 `bin/Debug/onnxruntime.dll` 文件版本号、`DirectML.dll` 是否存在

**注**：`Microsoft.ML.OnnxRuntime.DirectML 1.17.x` 的传递依赖 `Microsoft.AI.DirectML 1.13.x` 必须随之 restore，否则 `DirectML.dll` 缺失运行时崩。

---

### 规则 5：客户端 GPU 包选型决策（DirectML 优先）

**Why**：项目客户机器覆盖面广（NVIDIA / AMD / Intel 集显 / 无 GPU），不能强制用户装 CUDA Toolkit。包选型对比：

| 包 | 覆盖 NVIDIA | 覆盖 AMD/Intel | 客户端额外安装 | 包体积 |
|---|---|---|---|---|
| `Microsoft.ML.OnnxRuntime` (CPU) | ❌ | ❌ | 无 | ~10MB |
| `Microsoft.ML.OnnxRuntime.Gpu` (CUDA) | ✅ | ❌ | CUDA 11.8 + cuDNN 8.x | ~250MB |
| `Microsoft.ML.OnnxRuntime.DirectML` | ✅ | ✅ | 无（Win10 1903+ 自带 DX12） | ~15MB + DML.dll 10MB |

**How to apply**：项目客户面向终端用户、设备无控制时 **必须用 DirectML**。仅在开发机或可控 GPU 环境才考虑 `.Gpu` (CUDA) 变体。三个包**互斥**（同一 native onnxruntime.dll 编译产物不同），同一项目只能装一个。

---

### 历史教训
- DirectML 在 SenseVoice 上的真实兼容性差于宣传：即使配齐 MemPattern + 降图优化 + CPU 节点 fallback，self-attn Softmax 仍可能整体抛异常 → 进程级 sticky CPU 兜底是必需，不是 nice-to-have
- ORT 1.16.3 → 1.17.3 升级**不会自动**覆盖 bin/ 的旧 native dll，导致 managed/native 版本错位 NRE。这是 ORT 用户手册没明确强调的部署陷阱
- 客户机器广覆盖 + GPU 不可控 → 默认策略应是"试 GPU 一次，失败立刻 sticky 到 CPU"，而非"每次都先试"

### 相关
- 编码规范：`coding_standards.md`
- 架构规则：`architecture_rules.md`
- ASR 域 WAL：[[asr-local-engine-rules]] / [[asr-engine-fixes-rules]]
- 实现入口：`Asr/Local/GpuProbe.cs`, `Asr/Local/OfflineModel.cs`, `Asr/Local/SenseVoiceSmallEngine.cs`
