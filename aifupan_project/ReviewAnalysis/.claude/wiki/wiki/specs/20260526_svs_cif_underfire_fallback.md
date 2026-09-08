spec_mode: STANDARD

# SVS CIF Under-fire → CTC Fallback (修复直播解说大段丢字)

## 1. Context

- **Business goal**: 在 SenseVoiceSmall 本地引擎下，当 CIF fire 数 < content token 数时，自动降级到 CTC frame-index 时间戳路径，避免大段字被静默丢弃。
- **Scope of change**:
  - `Asr/Local/SenseVoiceSmallEngine.cs` — 仅修改 `ComputeTimestamps(cifPeak, frameIndices, chunkDurationMs)` 方法（line 502-572）
  - 不动 `IASREngine`、`AsrUtils`、`AsrEngineFactory`、`WavFrontend`、`OfflineModel`
- **Dependencies**:
  - `depends_on: [../domain/asr_engine_pipeline.md]`（权威架构文档）
  - `depends_on: [../preferences/coding_standards.md]`（异常/日志/资源规范）
  - `depends_on: [../preferences/wal/20260515_asr_engine_fixes_rules.md]`（CIF 对齐已修复规则）

## 2. Domain Model

- **新增约束**: `ComputeTimestamps` 的 Path A（CIF 路径）现在仅在 `firePoints.Count >= contentTokens.Length` 时使用；否则**完全降级**到 Path B（CTC frame-index 路径），不混用两种时间戳来源。
- **无新业务术语**；CIF / Path A / Path B 已在 [asr_engine_pipeline.md §4.1](../domain/asr_engine_pipeline.md) 定义。

## 3. API Contract (Handoff)

None. `ComputeTimestamps` 是私有方法，签名与返回类型不变（`int[][]`，长度始终 == `frameIndices.Length`）。

## 4. Data Model

None.

## 5. Business Logic

### 5.1 修改前行为（缺陷）

```text
if (cifPeak != null && cifPeak.Length > 0)
{
    fire = accumulate cif_peak, fire when >= 1.0
    for i in 0..n-1:
        if i < firePoints.Count:
            ts[i] = (firePoints[i], firePoints[i+1]) * 60ms
        else:
            ts[i] = (-1, -1)   ← 后续 MergeWords line 640 把它丢弃 → 大段丢字
}
```

### 5.2 修改后行为

```text
if (cifPeak != null && cifPeak.Length > 0)
{
    fire = accumulate cif_peak, fire when >= 1.0

    // 5.2.1 充足分支：fire 数足够覆盖所有 content token → 走 Path A
    if (firePoints.Count >= n) {
        for i in 0..n-1:
            ts[i] = (firePoints[i], firePoints[i+1]) * 60ms
        return ts
    }

    // 5.2.2 不足分支：fire 数 < content → 整段降级 Path B
    //        理由：CIF/CTC 时间戳源不可混用（前段 CIF 末端时间可能 > 后段 CTC 首端时间，
    //              违反 timestamps[i].StartTime 单调递增不变量，引发 MergeWords 错乱）
    FileUtils.LogAnalysis($"[SVS] CIF under-fire: fire={firePoints.Count}/content={n}, 降级 CTC 路径")
    // fallthrough 到原 Path B
}

// Path B (原代码，不变)
for i in 0..n-1:
    start = frameIndices[i] * 60ms
    end = (i+1 < n) ? frameIndices[i+1] * 60ms : chunkDurationMs
    clamp to [0, chunkDurationMs]
    ts[i] = (start, end)
```

### 5.3 错误处理

- `cifPeak` 为 null 或 length=0：保持原行为，直接走 Path B（已有兜底）。
- `n == 0`（无 content token）：保持原行为，返回 `new int[0][]`。
- `frameIndices == null`：保持原行为，`frameIndices?.Length ?? 0` 处理为 0。
- 不引入新异常类型，不新增 try/catch。

### 5.4 日志策略

仅在触发降级时记录**一行**警告级日志（`FileUtils.LogAnalysis`），含 fire 数 / content 数 / 比率。
**不**在每帧/每 token 加日志（避免直播长任务日志爆炸）。

## 6. Non-Functional Constraints (Hard Constraints)

### 6.1 性能
- Path A 充足分支的代码路径**不变**（fire >= n 时性能完全等同修改前）。
- Path B 降级分支新增 fire 累加（O(cifPeak.Length)），但只在欠 fire 时执行，且 cifPeak 长度 ≤ 1000（60s @ 60ms/LFR 帧）→ < 1ms 增量。

### 6.2 线程安全
- `ComputeTimestamps` 是无状态私有方法，受调用方 `_inferenceLock` 保护，无并发风险。

### 6.3 资源管理
- 不创建 IDisposable / Process / Timer，无释放问题。

### 6.4 禁止模式 (DO NOT)
- **禁止**混用 CIF 时间戳和 CTC 时间戳（不能"前 fire 数个用 CIF，剩下的用 CTC"），会破坏单调性。
- **禁止**修改 `MergeWords` 的 `if (tokenStart < 0 || tokenEnd < 0) continue;` —— 此分支是合法防御（Path B 也可能在极端情况下产生无效时间戳）。
- **禁止**改 `cifPeak >= 1.0f` 阈值或对 cif_peak 做 scale up（B/C 方案，不在本次范围）。
- **禁止**改 60s 硬限或音频切片逻辑（不在本次范围）。

### 6.5 回滚
- 单文件单方法改动，回滚 = `git checkout HEAD -- Asr/Local/SenseVoiceSmallEngine.cs`。
- 无数据库迁移、无配置变更、无外部依赖。

## 7. Acceptance Criteria (Testing)

**Mac 环境约束**: 本机为 macOS，**禁止**触发 .NET Framework 4.7.2 + WinForms 项目编译。QA 阶段以**静态代码审查 + 逻辑追踪**替代编译/单元测试。Windows 运行验证移交用户。

### 7.1 静态审查 Checklist (Implement 后 QA 阶段执行)

- [ ] 调用 csharp-code-review skill 复核 `ComputeTimestamps` diff
- [ ] 确认 Path A 充足分支与修改前**字节级等价**（fire >= n 路径不变）
- [ ] 确认 Path B 代码**完全不变**（fallthrough 落入原代码）
- [ ] 确认日志一行限定、不在循环内
- [ ] 确认无新增异常处理、无 new HttpClient / Thread.Sleep（CLAUDE.md 反模式）
- [ ] 确认返回数组长度恒等于 `frameIndices.Length`（与 MergeWords 索引契约对齐）

### 7.2 逻辑分支覆盖（伪测试，纸面追踪）

| 用例 | 输入 | 预期 |
|---|---|---|
| Happy path（CIF 充足） | cifPeak 正常，fire=100, content=100 | 走 5.2.1，与修改前完全一致 |
| **核心 bug 复现** | cifPeak 量化偏低，fire=30, content=300 | 走 5.2.2，270 个原本丢失的 token **现在保留**，时间戳为 CTC frame index |
| CIF 略不足 | fire=99, content=100 | 走 5.2.2（严格判定），1 个边界差异降级走 CTC（保守安全） |
| CIF 过 fire | fire=120, content=100 | 走 5.2.1，前 100 fire 被用（与原行为一致） |
| cifPeak 为 null | cifPeak=null | 跳过 Path A，直接 Path B（原行为） |
| 空内容 | content=0 | 返回 `int[0][]`（原行为） |

### 7.3 用户侧运行时验证（Windows 端，文档化交接）

用户在 Windows 机器测试 commit 后，需观察 `FileUtils.LogAnalysis` 输出：

1. **复现样本**：用诊断对话中的游戏直播解说视频跑一次 ASR
2. **预期日志**：能看到至少一行 `[SVS] CIF under-fire: fire=X/content=Y, 降级 CTC 路径`
3. **预期结果文本**：与腾讯 ASR 对比，原本大段丢失的内容现在出现（允许个别字识别错误，因为 SVS 模型质量本身限制）
4. **回归基线**：用一段已知能跑通的短音频（< 30s 清晰单人说话）跑，无 under-fire 日志，结果与修改前一致

### 7.4 不在本次范围的项目（明确出界）

- ❌ 音频切片重叠优化（前次诊断的方案 A）
- ❌ 10KB 小文件强删阈值调整
- ❌ CIF 自适应缩放（方案 B）
- ❌ 非量化模型替换（方案 D）
- ❌ MergeWords 内部逻辑改动

以上是后续独立的 Change 提案，不在本 spec 范围。
