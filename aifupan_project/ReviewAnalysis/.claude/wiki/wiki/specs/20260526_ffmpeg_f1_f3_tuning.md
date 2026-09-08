spec_mode: SLIM

# Change Summary
- What changed: AudioUtils SlicingAudioLegacy 的 FFmpeg 滤镜链插入轻量 EQ 整形（200Hz -2dB / 2800Hz +2dB）+ dynaudnorm 参数调宽（f=500:g=15:r=0.5:n=1）。
- Why: 用户产线 A/B 显示 SVS 内容完整度 98% 但有错字（"性发→罚款"类同音字混淆）、动态范围波动。F1+F3 是"音质上限"内仅剩可调项，试错性优化。

# Scope of Change
- `Utils/AudioUtils.cs` 仅 `SlicingAudioLegacy` 内 1 行 FFmpeg `-af` 字符串扩展 + 注释

# Risk & Rollback
- Why LOW: 单行字符串改动；纯频谱整形 + 动态归一，无内容过滤；不引入新依赖；与 BGM 保留策略不冲突。
- Rollback: 把 `-af` 字符串改回 `aresample=async=1:min_hard_comp=0.100:first_pts=0,highpass=f=80,dynaudnorm=f=200:g=5`（一行 git checkout 或手动）

# Verification & Evidence
- Mac 不可编译；Windows 端运行时验证按 [短视频验证规则](../preferences/wal/20260526_asr_short_video_validation_rules.md) 6 类样本 A/B
- **明确预期**：试错性优化，单段错字数 / 数字混乱 / 动态稳定度 改善幅度可能 < 5%
- 量化健康指标（对比改前同一素材）：
  - 同音字错（如"性发题"vs"罚款题"）出现次数下降 → 收益正
  - 整体 content/decoded 比率变化 < ±2% → 不破坏识别量
  - 任何样本类型出现明显退化 → 立即回退
- **失败回退条件（明确）**：
  - 任一短视频样本（尤其纯 BGM 含歌词 / AI 配音）出现内容丢失 → 回退
  - 错字密度 ≥ 改前 → 回退
- 业务方主观感觉"听起来不一样了"如果是变好就保留；变差或没变化就回退
