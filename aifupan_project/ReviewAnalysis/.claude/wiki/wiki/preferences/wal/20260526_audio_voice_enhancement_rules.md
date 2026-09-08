# Rules WAL — ASR 输入侧 FFmpeg 滤镜规则 (2026-05-26)

> 关联前置: [audio_voice_enhancement_domain.md](../../domain/wal/20260526_audio_voice_enhancement_domain.md)
>
> **修订历史**：当日初版含"FFmpeg ≥ 4.0 / afftdn 可用"规则，后实测发现 afftdn 杀 AI 配音，规则改为禁用 afftdn / arnndn 等频域降噪。

## 新增规则

### [Audio] [NEVER] 禁止在 ASR 预处理链加 lowpass < 8kHz

**反模式**：`lowpass=f=3400` 或更低（仿照电话带宽 ASR 习惯）。

**原因**：现代神经网络 ASR（含 SenseVoiceSmall）在 16kHz 全频带训练。砍 4-8kHz 会切掉咝音（s/sh/f/v/c 等无声辅音的能量），中文识别尤其受影响（"是"vs"次"、"师"vs"诗" 区分丢失）。

**正确做法**：仅做 `highpass` 砍 sub-bass，保留人声完整带宽。

---

### [Audio] [NEVER] 禁止在 ASR 输入侧用频域 stationary noise reduction (afftdn / arnndn / rnnoise)

**反模式**：`afftdn=nr=12`、`arnndn=m=...`、其他基于"学习稳态频谱当噪声"的滤镜。

**原因**：本项目业务包含 AI 配音 / TTS 素材（带货外推、视频解说、产品讲解等）。TTS 频谱稳态度远高于人声 → 这类滤镜会把 AI 配音学习成噪声并整段抹除。

**实测案例（2026-05-26）**：`afftdn=nr=12` 加入后，含 AI 配音的视频整段 AI 解说从识别结果中消失。

**正确做法**：
- 不抑制 BGM：仅做 highpass + dynaudnorm
- 必须抑制 BGM 时：走 VAD 端点检测（silero-vad / fsmn-vad）或人声分离（Demucs / UVR），**不**用频域降噪
- 任何降噪方案上线前**必须**用含 AI 配音的素材做 A/B 验证

---

### [Audio] [SHOULD] ASR 预处理优先 dynaudnorm 而非 loudnorm

**理由**：流式切片管道无法做 loudnorm 二 pass 测量；dynaudnorm 单 pass 实时处理，效果对识别精度足够。

---

### [Audio] [MUST] FFmpeg 滤镜链改动后必须用混合素材 A/B 验证

**最低验证集**：
1. 纯人类解说（基线，确保不退化）
2. 纯 AI 配音 / TTS（**必测**，验证不被吃掉）
3. 人 + AI 混合
4. 含强 BGM / 游戏音效（验证降噪/响度增强效果）

任何 ≥ 1 项退化即视为回归。

---

### [Audio] [MUST] FFmpeg 滤镜链 CPU 开销评估

当前滤镜链 CPU 开销（`highpass + dynaudnorm`）：约 1.1x 实时倍率，30 分钟 ffmpeg 超时可处理 ~27 分钟视频。

未来若引入更重滤镜需重新评估：

| 滤镜 | 实时倍率（叠加）| 30min 超时上限 |
|---|---|---|
| 当前定版（highpass + dynaudnorm） | ~1.1x | ~27 min 视频 |
| + afftdn（**已禁用**） | ~2.5x | ~12 min |
| + arnndn（**已禁用**） | ~3-5x | ~6-10 min |
| + silero-vad（独立进程） | 与本链路无关 | — |

## 关联已有规则

- 与 [asr_engine_pipeline.md §7.3](../../domain/asr_engine_pipeline.md) NAudio 禁用规则正交
- 与 [CIF under-fire fallback rules](./20260526_svs_cif_underfire_fallback_rules.md) 互补：本规则约束**输入端**，前者约束**解码端**
