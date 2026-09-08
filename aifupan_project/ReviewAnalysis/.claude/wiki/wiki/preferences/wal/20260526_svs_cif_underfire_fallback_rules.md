# Rules WAL — CIF/CTC 时间戳混用禁令 (2026-05-26)

> 关联 spec: [20260526_svs_cif_underfire_fallback.md](../../specs/20260526_svs_cif_underfire_fallback.md)
> 关联反模式清单: [asr_engine_pipeline.md §7.3](../../domain/asr_engine_pipeline.md)

## 新增反模式

### [SVS] [NEVER] 禁止在同一 chunk 内混用 CIF 与 CTC 时间戳源

**反模式**：当 CIF fire 数 < content token 数时，"前 fire_count 个 token 用 CIF 时间戳，剩余 token 用 CTC frame index 时间戳"。

**原因**：CIF firePoints 与 CTC frameIndices 是两个独立的时间序列，不保证对齐：
- CIF 末端时间可能 > CTC 首端时间 → 破坏 `WordList[i].StartTime` 单调递增不变量
- 下游 `MergeWords` / `subSentence` 假设词条按时间单调排序，违反会引发 CJK 分组错乱、字幕跳跃

**正确做法**：欠 fire 时**整段**降级到 Path B（全部用 CTC frame index），不做"前 CIF + 后 CTC"混合。

```csharp
// ❌ 错误 — 混用时间戳源
for (int i = 0; i < n; i++)
{
    if (i < firePoints.Count)
        ts[i] = CIF 时间戳;
    else
        ts[i] = CTC frame index 时间戳;  // ← 与前面不可比
}

// ✅ 正确 — 整段单一源
if (firePoints.Count >= n)
    ts = 全部用 CIF;
else
    ts = 全部用 CTC frame index;
```

### [SVS] [NEVER] 禁止用 `[-1, -1]` 作为时间戳占位再期望被静默丢弃

**反模式**：在 ComputeTimestamps 中给无效 token 赋 `[-1, -1]`，依赖下游 `MergeWords` 用 `if (start < 0) continue` 丢弃。

**原因**：
1. 在高语速/量化模型/噪声场景下，"无效" token 可能占总数 60%+，导致大段丢字
2. 时间戳生产端（`ComputeTimestamps`）应保证返回值全部有效，不应把"无效"语义转嫁给消费端

**正确做法**：生产端必须给每个 content token 找到一个合理的时间戳；下游的 `[-1, -1]` 检查仅作为防御性兜底，不应是常规流量。

## 关联已有规则

- 复用 [asr_engine_pipeline.md §7.1](../../domain/asr_engine_pipeline.md) CIF 时间戳对齐约束：`timestamps[i] ↔ filteredToken[i]`（filtered 索引直接对应）—— 本次修复**不破坏**该规则
- 强化 [coding_standards.md](../coding_standards.md) 的"非关键路径异常应记录完整信息"原则：CIF under-fire 是可恢复异常，仅记 1 行警告，不抛
