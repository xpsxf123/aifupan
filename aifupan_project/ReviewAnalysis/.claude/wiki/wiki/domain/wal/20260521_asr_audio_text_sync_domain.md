---
name: asr-audio-text-sync-domain
description: ASR 音字同步契约 — 时间戳分层、CJK 分组、标点附着、字段名约定
metadata:
  type: project
---

## 变更：本地 ASR 引擎与服务端音字同步契约对齐（2026-05-21）

### 涉及文件
- `Asr/AsrUtils.cs`
- `Asr/Local/SenseVoiceSmallEngine.cs`
- `Asr/ASRWordEntity.cs`
- `api/WordApi.cs`

### 痛点
之前 SVS 输出在数据库里被切成 "P1=西 / P2=南 / P3=地..." 的单字段，且服务端报 `getEndTime() is null` NPE。读后端代码定位到 2 个真相后修复。

---

### 真相 1：时间戳契约是 **chunk-relative**，服务端自加偏移

后端 `SensitiveWordsBll.subSentence` 第 555-560 行：
```java
for (WordsMarkItemBo item : wordsMarkBo.getItems()) {
    item.setEndTime(item.getEndTime() + ((wordsMarkBo.getCurrentSort() - 1) * wordsProperties.getAudioLength()));
    item.setStartTime(item.getStartTime() + ((wordsMarkBo.getCurrentSort() - 1) * wordsProperties.getAudioLength()));
}
```

**客户端必须发 chunk-relative 时间戳**（每段 0..chunkDuration）。`AsrUtils` **禁止** 加 cumulativeOffset。

**绝对时间公式**：`abs = (Paragraph - 1) × audioLength + WordList[i].StartTime`

由服务端 subSentence 和前端音字同步层各自换算，不在客户端预处理。

---

### 真相 2：服务端按 `item.Word.endsWith("，"|"。"|...)` 切句

后端 `SensitiveWordsBll.subSentence` 第 618-630 行从后往前找带标点的 item 作为句子边界。**若 items 全无标点**，`index=0` 一直生效，每次只取 `items[0]`，剩下挤 Redis 等下次 paragraph 拼接 → "P1-P5 单字 + P6 全包"现象。

**修复**：SVS `MergeWords` 把标点**附加到前一个 Word 末尾**（不再丢弃）：
```csharp
if (PunctuationSet.Contains(token)) {
    FlushAsciiBuffer(...); FlushCjkBuffer(...);
    if (words.Count > 0) {
        var prev = words[words.Count - 1];
        prev.Word = prev.Word + token;  // "话" → "话，"
        if (tokenEnd > prev.EndTime) prev.EndTime = tokenEnd;
    }
}
```

---

### CJK 分组算法（按语速 + 硬上限）

SVS 不做 NLP 分词，按以下规则把单字组合成 1-3 字的 Word：
- `MaxCharsPerCjkWord = 3`（硬上限，避免长串高亮难追读）
- `CjkBreakThresholdMs = 300`（上一字 duration > 300ms 视为自然停顿，flush）

**为什么 300ms**：
- 紧凑连读字间距 60-200ms → 不 flush，连续累 3 字
- 自然字间停顿 200-300ms → 不 flush（节奏感）
- 词组 / 呼吸停顿 > 300ms → flush（语义边界）

时间戳来源：CTC 帧索引（cif_peak 当前模型不输出），`tokens[i].start = frameIndices[i] × 60ms`，末字 end 延伸到 chunkDurationMs，全部钳位到 `[0, chunkDurationMs]`。

---

### 字段名约定：PascalCase

后端 `WordsMarkItemBo` 用 `@JsonProperty("Word"/"StartTime"/"EndTime")` **强制 PascalCase**。  
项目里某处的 Newtonsoft 默认输出 camelCase（实测发现），缺 JsonProperty 时会 endTime=null → NPE。

**ASRWordEntity 必须显式声明 PascalCase JsonProperty**：
```csharp
[JsonProperty("Word")]
public string Word { get; set; }
[JsonProperty("StartTime")]
public long StartTime { get; set; }
[JsonProperty("EndTime")]
public long EndTime { get; set; }
```

Newtonsoft 反序列化默认大小写不敏感，Tencent 路径仍正常。

---

### How to apply
- 想调单 Word 最大字数：改 `MaxCharsPerCjkWord` 常量
- 想调 flush 停顿阈值：改 `CjkBreakThresholdMs`
- 想加新引擎：返回 `ASRResultEntity.WordList` 时间戳必须 chunk-relative（0..AudioDuration）
- 客户端 **禁止** 自己累加偏移转绝对时间

### 相关
- 服务端契约：`back-fupan-server/replay-words/.../SensitiveWordsBll.subSentence`
- 字段名约定：[[asr-serialization-rules]]
- 历史误诊（已撤销）：errone JsonProperty camelCase
