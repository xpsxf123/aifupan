# Rules WAL — VAD 智能切片 Phase 1 架构规约 (2026-05-26)

> 关联 spec: [fsmn-vad smart slicing](../../specs/20260526_fsmn_vad_smart_slicing.md)
> 关联策略: [BGM 保留策略](./20260526_asr_bgm_preservation_rules.md)
> 关联约束: VAD 模型国内可达（memory · 跨机链接已失效，去链）

## Phase 1 实施总结

### 新增组件

| 文件 | 行数 | 职责 |
|---|---|---|
| `Asr/Local/Vad/VadSegment.cs` | ~15 | 数据类：语音段起止 ms |
| `Asr/Local/Vad/IVadEngine.cs` | ~25 | VAD 引擎接口（IsReady / GetSegments）|
| `Asr/Local/Vad/FsmnVadEngineStub.cs` | ~25 | 桩实现：IsReady=false，触发降级 |
| `Asr/Local/Vad/SmartSlicer.cs` | ~80 | 切分计划算法（纯函数）|
| `Utils/AudioUtils.cs` | +60 改 | 顶层路由 + 开关 + Legacy 重命名 |

Phase 1 总计 ~205 行（spec 承诺"~250 行架构落地" → 实际收敛）。

### 行为不变保证

Phase 1 默认配置下：
- `AudioUtils.VadEngine = new FsmnVadEngineStub()` （默认）
- `Stub.IsReady = false`
- `CanUseSmartSlicing()` 返回 false
- 始终走 `SlicingAudioLegacy()`（原 SlicingAudio 完整代码）
- **结果**：与 Phase 1 之前的行为**字节级等价**

## 新增规则

### [Vad] [MUST] AudioUtils.VadEngine 不允许在业务代码中替换

**唯一合法替换点**：`Program.cs` 启动逻辑（或 `App.xaml.cs` 等单一进程入口）。

**禁止**：在 Controller / BLL / Form 中赋值 `AudioUtils.VadEngine = ...`。

**理由**：VadEngine 是进程级单例，类似 `AsrEngineFactory._pool["svs"]`。多点赋值会引发竞态 + 资源泄漏（旧引擎 ONNX session 未 Dispose）。

### [Vad] [MUST] IVadEngine 的实现必须遵守 BGM 保留契约

接口注释已规定：
> 注：本接口仅用于切片位置决策，不允许下游基于检测结果做内容过滤。

**禁止模式**：
```csharp
// ❌ 错误：根据 VAD 输出过滤内容
var segments = vadEngine.GetSegments(pcm);
foreach (var seg in segments) {
    SendToSvs(pcm[seg.StartMs..seg.EndMs]);   // 只送语音段，丢 BGM
}

// ✅ 正确：用 VAD 输出做切分位置决策
var segments = vadEngine.GetSegments(pcm);
var cuts = SmartSlicer.PlanCuts(segments, totalMs);
foreach (var cut in cuts) {
    SendToSvs(pcm[cut.StartMs..cut.EndMs]);   // 切点用 VAD 建议，但所有 PCM 都送
}
```

### [Vad] [SHOULD] VAD 开关 (AsrVadEnabled) 默认启用

`AudioUtils.IsVadEnabled()` 读取 `App.config` `<appSettings>` 的 `AsrVadEnabled` key：
- 缺失 / 空 → 默认 `true`
- 值为 `"false"`（大小写不敏感）→ 禁用
- 其他值 → 启用

**禁止**：在代码里 hardcode 关闭 VAD（如 `private static bool VadEnabled = false`）。让运维通过配置文件控制。

### [Vad] [MUST] 智能切片异常必须降级到 Legacy

`SlicingAudio` 顶层路由必须 try-catch SlicingAudioSmart 异常并 fallback 到 `SlicingAudioLegacy`。

**禁止**：异常向上抛，破坏切片流程导致整个 ASR 失败。

**理由**：VAD 是质量优化，不是必需路径。任何异常都应该退化到已知可工作的硬切。

## 教训：旧式 csproj 必须手动注册

.NET Framework 4.7.2 + 旧式 csproj 不像 SDK-style 项目那样自动包含源文件。**任何新增 `.cs` 必须在 `ReviewAnalysis.csproj` `<ItemGroup>` 加 `<Compile Include="..."/>`**，否则 Mac 端编辑看似成功，Windows 编译报"未能找到类型或命名空间名"。

Phase 1 落地时此步骤遗漏，2026-05-26 补上。Phase 2 起所有新 `.cs` 必须同步 csproj。

---

## 已知技术债（Phase 2 处理）

| 项 | 计划 |
|---|---|
| `public static IVadEngine VadEngine { get; set; }` | 违反 [架构规则 §3 NEVER public static 可变字段]。Phase 2 评估是否迁移到 DI 容器或服务定位器。当前以"基础设施单例"豁免。 |
| `SlicingAudioSmart` throw NotImplementedException | Phase 2 写入真实实现 |
| `FsmnVadEngineStub` 占位 | Phase 2 替换为真实 `FsmnVadEngine`（基于 ModelScope iic/speech_fsmn_vad_zh-cn-16k-common-onnx）|
| `ModelManager` 未扩展 VAD 下载 | Phase 2 加入 |
| `Program.cs` 未加 VAD warmup | Phase 2 加入 |

## Phase 2 入口指引

下次起 Phase 2 spec 时，**必读以下文档**避免重复研究：
- FunASR 参考实现解读（memory · 跨机链接已失效，去链）
- 本文档（Phase 1 已建好的接口和路由）
- spec [20260526_fsmn_vad_smart_slicing.md](../../specs/20260526_fsmn_vad_smart_slicing.md)

Phase 2 主要工作：
1. 决定实现策略（A 完整 port / B 离线简化 + KaldiNativeFbankSharp / C 纯 C# FBank）
2. 实施 `Asr/Local/Vad/FsmnVadEngine.cs`（替换 Stub）
3. 扩展 `ModelManager` 加 VAD 模型下载
4. 实施 `SlicingAudioSmart`（FFmpeg 解码 + VAD 调用 + SmartSlicer + FFmpeg 提取）
5. `Program.cs` 启动时 `AudioUtils.VadEngine = new FsmnVadEngine(...)`
6. 6 类 Windows A/B（含纯 AI 配音 + 纯 BGM 含歌词）
