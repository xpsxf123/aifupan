---
name: asr-gapms-offset-rules
description: ASR 工程约束 — Sum 累加防多 entity、单例引擎避免构造函数耦合 per-call 配置、SvsLang 常量化
metadata:
  type: feedback
---

## 约束/反模式（来自 2026-05-19 改动）

### [MUST] 跨片偏移累加用 Sum，不用 Max

**Why:** `cumulativeOffsetMs += entities.Max(AudioDuration)` 依赖"每文件单 entity"的隐式假设，未来若引擎返回多个不相交时间段的 entity 会偏移不足。Sum 在单 entity 下与 Max 等价，多 entity 下才是正确语义。

**How to apply:** 凡是把 `List<entity>` 的某个时间字段聚合用于跨片偏移的累加，默认用 `Sum`；除非有明确语义（如取最末位置）才用 `Max`。

### [NEVER] 把 per-call 配置耦合进单例引擎构造函数

**Why:** 现实场景：`AsrEngineFactory.cs:31` 将 SVS 池化为进程级单例（持有 200MB+ ONNX session）。若把 per-call 配置（如语言相关阈值）放进构造函数，要么破坏池化、要么需要按语言重建引擎复制 ONNX session，内存浪费。本次 `MaxGroupGapMs` 阈值参数化时就遇到了原始 spec 与池化结构的冲突。

**How to apply:**
- 单例 + per-call 配置 → 在 `Recognize/Process` 入口现算并下传参数
- 多语言派生项 → 集中在 `ParseConfig` 之类的单点函数返回元组，单点解析单点消费
- 仅当配置是 per-instance 生命周期（如模型路径、设备 ID）才放构造函数

### [MUST] 跨模块字典 key 必须用常量，不用 magic string

**Why:** `GapMsByLang` 初版用 `"zh"`/`"en"` 等裸字符串作为 key，与 `AsrLanguageCode.ToSvsLang` 返回值耦合但无编译期约束，拼写/大小写漂移会静默退化为 `DefaultGapMs`。

**How to apply:**
- 任何 `Dictionary<string, T>` 若 key 是另一处函数的返回值，必须在源头暴露 `public const string`（或嵌套常量类）
- 本仓库 SVS 内部 token：`AsrLanguageCode.SvsLang.{Auto,Zh,En,Yue,Ja,Ko}`
- 若 key 集合稳定且有限，更进一步可改 enum + switch

### [MUST] 语言派生项集中在 ParseConfig 解析

**Why:** 多个步骤分别调 `AsrLanguageCode.Normalize/ToSvsLang` 推导各自需要的派生项，会重复计算且分散关注点。改成 `ParseConfig` 返回元组后，未来加新派生项（标点风格、置信度阈值、批大小）只改一处。

**How to apply:**
- 凡 `RecognizeAsync` 中需要"按语言变化"的配置项，先看 `ParseConfig` 是否能扩；不能扩才单独算
- `ParseConfig` 元组顺序保持 `(语言相关数字 ID..., 文本规范化, 阈值..., 其它)` 的语义分组

### 相关
- 决策记录：[[asr-gapms-offset-domain]]
- 历史阈值：[[asr-merge-words-v2-rules]]
