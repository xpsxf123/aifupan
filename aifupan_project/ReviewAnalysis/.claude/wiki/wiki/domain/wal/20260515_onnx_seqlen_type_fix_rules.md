---
name: onnx-seqlen-int64-rule
description: SenseVoiceSmall ONNX 输出 sequence length 是 int64，不是 int32，必须用 AsEnumerable<long>() 读取
metadata:
  type: rules
---

ONNX 模型（SenseVoiceSmall）的 sequence length 输出张量类型是 **int64**。

**Why:** ONNX 标准整数默认 int64，`AsEnumerable<int>()` 读 int64 张量时 .NET unboxing 抛 `InvalidCastException`。并发处理 N 个文件时触发 N×3 条异常风暴（每文件重试 3 次）。

**How to apply:** 所有读取 ONNX 整数型输出的代码，优先用 `AsEnumerable<long>()` 再显式 `(int)` 转换，不要假设是 int32。
