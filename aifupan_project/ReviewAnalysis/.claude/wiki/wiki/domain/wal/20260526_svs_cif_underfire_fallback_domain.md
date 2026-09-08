# Domain WAL — SVS CIF Under-fire 降级行为 (2026-05-26)

> 关联 spec: [20260526_svs_cif_underfire_fallback.md](../../specs/20260526_svs_cif_underfire_fallback.md)
> 关联权威文档: [asr_engine_pipeline.md §4.1](../asr_engine_pipeline.md)

## 新增状态/行为

### `ComputeTimestamps` 的双分支语义（精确表述）

| 条件 | 路径 | 时间戳精度 |
|---|---|---|
| `cifPeak == null \|\| Length == 0` | Path B (CTC frame index) | LFR 60ms |
| `cifPeak != null && firePoints.Count >= n` | Path A (CIF fire points) | CIF 60ms（声学对齐） |
| `cifPeak != null && firePoints.Count < n` | **Path B 降级**（本次新增） | LFR 60ms |

**关键不变量**：返回数组长度恒等于 `frameIndices.Length`。所有 content token 都有有效时间戳，**不会**再出现 `[-1, -1]` 占位。

### 失败模式：CIF Under-fire

**触发场景**（已在生产观测）：
- 量化模型 (`model_quant.onnx` INT8) cif_peak 数值整体偏低
- 高语速直播解说（解说员 8-12 字/秒）
- 强背景音（游戏枪声/爆炸/BGM）扰动 cif_peak

**症状**：GreedyDecode 出 N 个 content token，CIF 只 fire 出 M 个（M << N），多出的 N-M 个被旧实现赋 `[-1,-1]`，在 `MergeWords` (line 640) 被静默丢弃 → 表现为"识别出来又消失"的大段丢字。

**示例（生产复现）**：游戏直播 60s 段，content=300+ token，fire=80~120，损失率 60%+。

## 后处理调用方契约

`MergeWords` 中 `if (tokenStart < 0 || tokenEnd < 0) continue;` (line 640) 仍保留作为**合法防御边界**，但在正常使用下不会被 ComputeTimestamps 触发（仅在未来 Path B 自身出现极端边界情况才可能命中）。
