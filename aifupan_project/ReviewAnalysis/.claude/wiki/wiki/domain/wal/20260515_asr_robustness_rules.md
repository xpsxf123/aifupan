---
name: asr-engine-exception-boundaries
description: 本地 ASR 引擎三层异常边界规则：SVS 后处理、Composite 子引擎调用、Tencent 整体方法均须 try/catch
metadata:
  type: rules
---

本地 ASR 管道必须在三处设置异常边界，防止异常穿透影响主程序：

1. **SenseVoiceSmallEngine.RecognizeAsync 步骤9-11**（GreedyDecode/FilterSpecialTokens/ComputeTimestamps/MergeWords/BuildResultText）无原生保护，需 try/catch → CreateErrorResult(500)。
2. **CompositeASREngine** 的每个子引擎 RecognizeAsync 调用需 try/catch → log + continue，引擎抛异常时自动降级到下一个引擎，而非穿透。
3. **TencentASREngine.RecognizeAsync** 整体需 try/catch → 返回 {code:500, data:[ASRResultEntity]}，防止 MyCallableTask 以外的意外异常逃逸。

**Why:** CompositeASREngine 承担降级职责，子引擎抛异常而非返回非零 code 时，若无 catch 则降级链中断，所有引擎失效。

**How to apply:** 新增 IASREngine 实现时，RecognizeAsync 必须自行 try/catch 全部异常并返回标准错误字典，不得向上抛出。
