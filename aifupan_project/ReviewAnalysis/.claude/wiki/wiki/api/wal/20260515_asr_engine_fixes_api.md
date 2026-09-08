---
name: asr-engine-fixes-api-20260515
description: ASR引擎管道API变更 — EngineEntry签名、MyCallableTask构造函数、StartAsTaskAsync
metadata:
  type: project
---

## ASR 引擎 API 变更 (2026-05-15)

### MyCallableTask 构造函数

**Before**: `MyCallableTask(FileInfo file, string token, string directoryPath)`
**After**: `MyCallableTask(FileInfo file, string directoryPath)`

`token` 参数始终为 null，内部通过 `AsrApi.GetTempToken("")` 自行获取，死参数已移除。

### MyCallableTask.StartAsTask → StartAsTaskAsync

**删除**: `StartAsTask(AudioTempTokenEntity, string)` — 同步方法含 Thread.Sleep，已无外部调用方。

**新增**: `StartAsTaskAsync(AudioTempTokenEntity, string)` — 完全异步版本，`Thread.Sleep` 替换为 `await Task.Delay(5000)`，调用方无需 `Task.Run` 包装。

**外部影响**: 零外部调用方（grep 确认），仅 `TencentASREngine` 使用。

### EngineEntry 构造函数

**Before**: `EngineEntry(IASREngine engine, int weight, string config)`
**After**: `EngineEntry(IASREngine engine, string config)`

Weight 字段移除。原权重计算 `10 - i*3` 降序排序后结果与插入顺序完全一致，无实际功能，复杂度移除。

### CompositeASREngine 排序行为

**Before**: `entries.OrderByDescending(e => e.Weight)` — 按权重降序（等效于插入顺序）
**After**: `new List<EngineEntry>(entries)` — 保持插入（优先级）顺序

无功能变化，语义更清晰。

### AsrEngineFactory.ResolveType 未知配置日志

未知引擎 config 字符串现在输出 warning 日志再降级到 Tencent，便于排查配置错误。
