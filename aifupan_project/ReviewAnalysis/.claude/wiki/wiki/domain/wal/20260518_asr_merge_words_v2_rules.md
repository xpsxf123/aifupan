---
name: asr-merge-words-v2-rules
description: MergeWords v2 重写决策：wordList 无标点、2字分组、语速断句、resultText 独立构建
metadata:
  type: project
---

## 变更：MergeWords 重写（2026-05-18）

**文件**: `Asr/Local/SenseVoiceSmallEngine.cs`

**Why:** wordList 用于播放时卡拉OK高亮，需要精细粒度（1-2字）且不含标点；resultText 需保留标点供展示。

### 新规则（按优先级）
1. **标点** → flush buffer，标点本身不进 wordList
2. **语速断句** → 相邻 token 时间间隔 > `MaxGroupGapMs`(200ms) → flush，首字跳过此检查
3. **2字上限** → charCount >= 2 立即 emit；加入新 token 会超 2 字则先 flush
4. **resultText 独立** → `BuildPunctuatedText(filteredTokens)` 直接遍历 token 流，与 wordList 完全解耦

### 关键常量
```csharp
private const int MaxGroupGapMs = 200;
```

### How to apply
- 修改分词粒度时，先理解语速断句是第②优先级（高于字数限制）
- `BuildResultText(wordList)` 已废弃为生产路径，保留仅供 debug
