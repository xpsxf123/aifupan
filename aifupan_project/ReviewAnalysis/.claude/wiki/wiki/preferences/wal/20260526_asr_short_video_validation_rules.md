# Rules WAL — ASR 改动必须包含短视频 A/B 验证 (2026-05-26)

> 关联消费方映射: [asr_consumers.md](../../domain/asr_consumers.md)
> 关联反面案例: [audio_voice_enhancement_domain.md §3 (afftdn 撤回)](../../domain/wal/20260526_audio_voice_enhancement_domain.md)

## 新增规则

### [ASR] [MUST] 任何 ASR 管道改动必须用短视频样本 A/B 验证

**适用范围**：触及以下任一文件的改动都必须执行短视频 A/B 验证：

- `Utils/AudioUtils.cs`（音频切片 / 滤镜链）
- `Asr/Local/SenseVoiceSmallEngine.cs`（本地推理 / 解码 / 后处理）
- `Asr/Local/WavFrontend.cs`（特征提取）
- `Asr/AsrUtils.cs`（入口 / 跨段合并）
- `Asr/AsrEngineFactory.cs`（引擎选择）
- `Asr/CompositeASREngine.cs`（降级管道）

**原因**：

短视频是 ASR 三大消费方之一（详见 [asr_consumers.md](../../domain/asr_consumers.md)），且具备所有消费方中**最极端**的音频特征：
- BGM 几乎默认存在
- AI 配音 / TTS 占比极高
- 字幕同步语音对边界要求高
- 平台压缩 + 复杂混音

历史上多次出现"对主播回放有益但对短视频灾难性"的改动，最典型是 2026-05-26 afftdn 撤回案例。**短视频不验证 = 一定会回归**。

---

### [ASR] [MUST] 最低验证集（5 类，缺一不可）

| # | 样本类型 | 验收要点 |
|---|---|---|
| 1 | 主播游戏直播片段（基线） | 内容数 / 时间戳对齐与上次持平或更好 |
| 2 | 用户上传清晰人声（基线） | 识别率不退化 |
| 3 | **短视频 — 带 BGM 的人类配音**（生活/带货类） | 人声部分保留 |
| 4 | **短视频 — 带 BGM 的纯 AI 配音**（外推/科普/带货 AI） | **AI 配音必须完整保留**（afftdn 案例的根本检查） |
| 5 | **短视频 — 字幕同步语音类**（短句快切类） | 边界字不大量丢失 |
| 6 | **短视频 — 纯 BGM 含歌词（无人声片段）** | **BGM 文字内容必须保留**（详见 [BGM 保留策略](./20260526_asr_bgm_preservation_rules.md)） |

**任何 ≥ 1 项退化即视为回归**，须修正或回退。

---

### [ASR] [SHOULD] PR 描述模板

涉及 ASR 改动的 PR / spec 描述应包含：

```markdown
## ASR 验证

| 样本类型 | 改前 words/last_end | 改后 words/last_end | 结论 |
|---|---|---|---|
| 主播游戏直播 | XX / XXms | XX / XXms | ✅/⚠️/❌ |
| 用户上传清晰人声 | ... | ... | ... |
| 短视频 + BGM + 人声 | ... | ... | ... |
| 短视频 + BGM + AI 配音 | ... | ... | ... |
| 短视频 + 字幕同步 | ... | ... | ... |
```

利用现有 `[SVS] 诊断` 日志（见 [diagnostics rules](./20260526_asr_diagnostics_and_threshold_rules.md)）填充。

## 关联已有规则

- 与 [audio_voice_enhancement_rules.md](./20260526_audio_voice_enhancement_rules.md) §3 互补：那里规定"禁用 afftdn"；本规则规定"为什么——以及如何提前发现这类问题"
- 与 [asr_diagnostics_and_threshold_rules.md](./20260526_asr_diagnostics_and_threshold_rules.md) 互补：诊断日志是 A/B 验证的数据来源
