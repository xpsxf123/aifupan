---
date: 2026-05-29
feature: asr_svs_quality_fallback
type: rules
run_id: 20260529_124517_asr_svs_quality_fallback
related_specs:
  - .claude/runs/20260529_124517_asr_svs_quality_fallback/openspec.md
related_adr: []
related_wal:
  - [[20260529_svs_ffmpeg_filter_tune_rules]]
  - [[20260529_svs_ffmpeg_filter_tune_architecture]]
  - [[20260529_asr_svs_quality_fallback_architecture]]
---

# Rules WAL — SVS 质量评估 + Chunk-level Tencent 降级规则（2026-05-29 路线 A）

## Change Summary

在 `CompositeASREngine` 引入 chunk-level 质量评估：SVS 识别成功（code==0）但 `IsLowQuality` 返回 true 时，自动调用 `TencentASREngine` 作为 fallback。`ASRResultEntity` 新增 3 个诊断字段，由 SVS 引擎出口赋值，判定逻辑唯一集中在 Composite 内。

## Impact

- 路线 A 与路线 B（FFmpeg 滤镜调优）协同：路线 B 改输入侧（无法根治 CTC 唱歌 badcase），路线 A 改输出侧（根治）
- 三大消费方（AnchorReplay / UserUpload / ShortVideo）无感知，`CompositeASREngine` 对上层永远返回 `code=0`（除非全部引擎失败）
- 生产实测（637s 唱歌+说话混合直播，11 chunks）：12.3% 触发降级，说话段正确保留 SVS，唱歌段全部 REPLACED

---

## 1. ASRResultEntity 3 个诊断字段定义

`ASRResultEntity` 追加 3 个 `public int` property：

| 字段 | 含义 | 默认值 |
|---|---|---|
| `DecodedTokens` | SVS CTC 解码后总 token 数（含 blank/meta/content） | 0 |
| `ContentTokens` | 经 `FilterBlanksAndMetas` 过滤后的有效内容 token 数 | 0 |
| `CollapsedTokens` | 折叠 token 数 = `DecodedTokens - ContentTokens` | 0 |

`DecodedTokens == 0` 时规则 1 自动跳过（除零保护）。其他引擎（Tencent 等）不填写这 3 个字段，默认 0，不参与 `IsLowQuality` 判定。

[Confidence: HIGH]
[Evidence: Asr/ASRResultEntity.cs:53-69]

---

## 2. SVS 出口：仅赋值，不判定

`SenseVoiceSmallEngine.RecognizeAsync` 在构造 `ASRResultEntity` 时赋值 3 个字段，变量直接复用 line 238 诊断日志中已存在的 `tokenIds` 和 `contentTokens`：

```csharp
DecodedTokens   = tokenIds.Length,
ContentTokens   = contentTokens.Length,
CollapsedTokens = tokenIds.Length - contentTokens.Length
```

约束：SVS 内部不调用任何阈值常量，不导入 `IsLowQuality` 引用，不向 Tencent 发起请求。判定职责单向上移到 Composite。

[Confidence: HIGH]
[Evidence: Asr/Local/SenseVoiceSmallEngine.cs:250-252]

---

## 3. IsLowQuality SSOT：2 条 OR 规则 + 阈值常量

判定 SSOT 唯一在 `CompositeASREngine`（`private static bool IsLowQuality`）：

```csharp
private const double CtcCollapseThreshold = 0.95;   // 规则 1 上限
private const double CharDensityThreshold = 0.5;    // 规则 2 下限（char/s）

bool rule1 = result.DecodedTokens > 0
    && (result.CollapsedTokens / (double)result.DecodedTokens) > CtcCollapseThreshold;

bool rule2 = result.AudioDuration > 0
    && ((result.Result?.Length ?? 0) / (result.AudioDuration / 1000.0)) < CharDensityThreshold;

return rule1 || rule2;
```

- 规则 1 针对唱歌段（CTC blank/meta 高度折叠）；规则 2 针对极低字符密度的任意低质片段
- 两条规则 OR 组合；不启用规则 3（content/total < 5%，与规则 1 高度重叠，YAGNI）
- 参考阈值来源：[[20260529_svs_ffmpeg_filter_tune_rules]] §5 CTC collapsed/total 比例表（> 95% = 唱歌段）

[Confidence: HIGH]
[Evidence: Asr/CompositeASREngine.cs:19-20, Asr/CompositeASREngine.cs:78-89]

---

## 4. 生产触发率基线（2026-05-29 Windows smoke）

| 测试视频 | 总 chunks | 降级触发 | 触发率 | 规则 |
|---|---|---|---|---|
| 唱歌+说话混合直播 637s | 81 | 10 | 12.3% | 规则 1（collapsed_ratio）|
| 规则 2 char_density | — | 0 | 0% | 本次 cover 不足，需继续观察 |

说话段代表性样本：`audio_008`（collapsed/decoded = 0.867）不触发，SVS 结果保留，识别正常。

[Confidence: HIGH]
[Evidence: VERBATIM:"""2026-05-29 Windows smoke: 637s 11 chunks, 10/81 REPLACED (12.3%), Tencent 10/10 code=0"""]

---

## 5. 禁用模式（v2 设计决策）

| 禁止行为 | 原因 |
|---|---|
| 抽独立 `QualityAssessor.cs` | 1 行 OR 逻辑不值得 60 行独立类（YAGNI 反模式） |
| 在 `SenseVoiceSmallEngine` 内部判定阈值或调用 Tencent | 职责混淆，破坏引擎中立性 |
| 引入 `svs:auto:withitn\|tencent` 类配置串解析 | 后端配置串已过时，v2 明确废除 |
| 新增 `.cs` 文件 | 逻辑体量不足，全部追加到现有 3 文件 |
| 在 `Composite.RecognizeAsync` 中 `.Result`/`.Wait()` 阻塞 | async 链完整传递约束 |

[Confidence: HIGH]
[Evidence: .claude/runs/20260529_124517_asr_svs_quality_fallback/openspec.md §6]

---

## 6. Metric 日志格式（AC9）

降级必须输出：

```
[Composite-FALLBACK] chunk=<file> rule=<collapsed_ratio|char_density|no_tencent_engine> ratio=<R:F3> tencent_code=<c> action=<REPLACED|KEPT_SVS>
```

`action=KEPT_SVS` 含义：Tencent 失败或无 Tencent 引擎，保留 SVS 原结果，整段识别不中断，返回 `code=0`。

[Confidence: HIGH]
[Evidence: Asr/CompositeASREngine.cs:98, Asr/CompositeASREngine.cs:117-125]
