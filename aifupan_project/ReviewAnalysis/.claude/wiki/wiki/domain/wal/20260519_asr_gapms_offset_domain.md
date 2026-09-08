---
name: asr-gapms-offset-domain
description: ASR MergeWords 按语言阈值化 + 多文件偏移累加从 Max 改为 Sum
metadata:
  type: project
---

## 变更：ASR 阈值参数化 + 偏移累加防御（2026-05-19）

**文件**:
- `Asr/AsrUtils.cs`
- `Asr/Local/SenseVoiceSmallEngine.cs`
- `Asr/AsrLanguageCode.cs`

**Why:** ① 原 `MergeWords` 用固定 200ms 阈值判定语速断点，对慢语速主播（< 4 字/秒）会过度切碎 wordList；英文/日韩自然停顿分布与中文差异显著，固定阈值不合适。② 跨文件偏移累加用 `entities.Max(AudioDuration)`，依赖"每文件单 entity"的隐式假设，未来若引擎切换返回多 entity 则会偏移不足。

### 新行为（按语言）

| svsLang | gapMs | 业务含义 |
|---|---|---|
| zh / yue / auto | 200 | 与改前一致 |
| en | 150 | 英文自然音节间距更短 |
| ja / ko | 220 | 日韩停顿更长 |

未列语言（vi/fr/de 等）经 `ToSvsLang` 会被映射为 `auto` → 200ms。

### 偏移累加

`Asr/AsrUtils.cs` 中 `cumulativeOffsetMs += entities.Sum(e => e.AudioDuration)`，`Max → Sum` 防御未来引擎返回多 entity 的情形。今日 Sum 与 Max 等价（所有引擎均返回 ≤ 1 entity per chunk）。

### 新增结构（API）

- `AsrLanguageCode.SvsLang` 嵌套静态类：暴露 SVS 内部语言 token 常量（`Zh`/`En`/`Yue`/`Ja`/`Ko`/`Auto`），消除跨模块字典 key 的 magic string 耦合
- `SenseVoiceSmallEngine.ParseConfig` 签名：`(int languageId, int textnormId)` → `(int languageId, int textnormId, int gapMs)`，语言派生项单点解析单点消费
- `SenseVoiceSmallEngine.MergeWords` 签名：新增 `int gapMs` 参数

### How to apply
- 想调某语言的合并粒度：直接改 `GapMsByLang` 字典对应项
- 想加新语言：先在 `AsrLanguageCode.SvsLang` 加常量并扩 `ToSvsLang` switch，再扩 `GapMsByLang`
- 想加新的语言派生项（如标点风格、置信度阈值）：扩 `ParseConfig` 返回的元组，沿用同一 svsLang 计算路径，避免重复调用 `Normalize/ToSvsLang`

### 相关
- 历史阈值决策：[[asr-merge-words-v2-rules]]（2026-05-18 200ms 固定阈值的引入）
- 偏移修复缘起：commit `8d0518f1` 修分片时间戳偏移
- 派生约束：[[asr-gapms-offset-rules]]
