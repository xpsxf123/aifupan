# Rules WAL — SVS 解码置信度门 (2026-05-26)

> 关联 spec: [svs_confidence_threshold.md](../../specs/20260526_svs_confidence_threshold.md)

## 新增规则

### [SVS] [SHOULD] GreedyDecode 后置信度过滤是减少幻觉的首选手段

**适用场景**：解决"BGM/噪声段 logits 分布平坦 → argmax 选出垃圾 token → 输出幻觉文字"。

**机制**：每帧 argmax 后计算 softmax(maxToken)；< 阈值则强置 blank。

**与其它方案的取舍**：

| 方案 | 优势 | 短视频 AI 配音风险 | 评级 |
|---|---|---|---|
| **本方案（softmax 门）** | token 级精细控制；AI/TTS 高置信度天然安全 | 低 | ✅ 首选 |
| 频域降噪（afftdn） | 滤镜级简单 | **极高**（吃 TTS） | ❌ 禁用（见 [audio_voice_enhancement_rules.md](./20260526_audio_voice_enhancement_rules.md)） |
| 事件 token VAD（区间级） | 利用模型自身事件分类 | 中-高（AI 配音段可能被判 Music） | ⚠️ 慎用，必须可配 |

**结论**：在 token 级过滤优先于区间级或滤镜级，因为 token 级保留了对个别清晰发音的尊重。

---

### [SVS] [MUST] softmax 计算必须用 log-sum-exp 数值稳定技巧

**反模式**：直接 `Math.Exp(logits[v])`。

**原因**：SVS logits 范围可达 ±30；`Math.Exp(30) ≈ 1e13`，多个相加可能溢出 float。

**正确做法**：
```csharp
double expSum = 0;
for (int v = 0; v < vocabSize; v++)
    expSum += Math.Exp(logits[offset + v] - maxVal);  // ← 减 maxVal
float prob = (float)(1.0 / expSum);
```

减 maxVal 后所有指数 ≤ 0，`exp(x) ∈ (0, 1]`，求和稳定。

---

### [SVS] [SHOULD] 性能优化：只对非 blank 候选计算 softmax

**理由**：argmax 出 blank（id ≤ 2）的帧反正会被 FilterBlanksAndMetas 丢弃，无需算 softmax。可省 60-80% 的 exp 调用（典型音频帧 blank 占多数）。

---

### [SVS] [SHOULD] 置信度阈值参数 0.30 是经验起步值

**调参指南**：

| 阈值 | 行为 | 适用 |
|---|---|---|
| 0 | 等同禁用 | 回退/A/B 对照 |
| 0.15 | 极宽松 | 仅过滤明显垃圾 |
| **0.30** | **保守起步** | **默认** |
| 0.40 | 偏严 | 高 BGM 重场景 |
| 0.50+ | 激进 | 实测会误杀清晰人声，**不建议** |

**未来演进**：若需按消费方差异化（如短视频更激进、上传更保守），应迁移为 DI 注入或配置项。当前 const 设计已留出零成本回退路径。

## 关联已有规则

- 与 [svs_cif_underfire_fallback_rules.md](./20260526_svs_cif_underfire_fallback_rules.md) **协同**：本规则降低 content token 总数 → fire/content 比率更易满足 → CIF Path A 更可能触发
- 与 [audio_voice_enhancement_rules.md](./20260526_audio_voice_enhancement_rules.md) **互补**：前者管输入端（不破坏 TTS），本规则管解码端（不破坏 TTS）
- 与 [asr_short_video_validation_rules.md](./20260526_asr_short_video_validation_rules.md) **配合**：5 类样本验证是本规则上线的强制门
