# API WAL — SVS ComputeTimestamps 行为变更 (2026-05-26)

> 关联 spec: [20260526_svs_cif_underfire_fallback.md](../../specs/20260526_svs_cif_underfire_fallback.md)

## 接口变更

无对外接口变更。`IASREngine.RecognizeAsync` 签名、返回字段、错误码全部不变。

## 私有方法行为变更

### `SenseVoiceSmallEngine.ComputeTimestamps(float[] cifPeak, int[] frameIndices, int chunkDurationMs)`

**签名**：未变。

**前置/后置条件**：

| 条件 | 修改前 | 修改后 |
|---|---|---|
| 返回数组长度 | == frameIndices.Length | == frameIndices.Length（不变） |
| 元素值范围 | `[0, chunkDurationMs]` 或 `[-1, -1]` | `[0, chunkDurationMs]`（移除 [-1,-1] 占位） |
| StartTime 单调性 | 局部保证 | **全段保证**（不混用 CIF/CTC 源） |
| 性能（CIF 充足分支） | O(cifPeak.Length + n) | 字节级等价 |
| 性能（CIF 不足分支） | O(cifPeak.Length + n) | O(cifPeak.Length + n)（多一次 List 累积已存在） |

## 新增日志

| 触发条件 | 内容 | 频率上限 |
|---|---|---|
| CIF fire 数 < content token 数 | `[SVS] CIF under-fire: fire={X}/content={Y}, 降级 CTC 时间戳路径` | 每个音频文件 ≤ 1 行 |

## 调用方影响

`MergeWords` (line 612) 中 `if (tokenStart < 0 || tokenEnd < 0) continue;` 保留为防御性边界检查，但来自 `ComputeTimestamps` 的输入在正常路径下不再触发该分支。
