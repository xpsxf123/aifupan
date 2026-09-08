# Domain WAL — ASR 输入侧人声增强 (2026-05-26)

> 关联权威文档: [asr_engine_pipeline.md §1](../asr_engine_pipeline.md)
> 关联前置修复: [CIF under-fire fallback](./20260526_svs_cif_underfire_fallback_domain.md)
>
> **修订历史**：当日初版含 `afftdn=nr=12` 段，实测在含 AI/TTS 配音的素材上误杀 AI 解说（见本文 §3）；定版去掉 afftdn，留 3 段链。

## 1. 当前管道阶段（2026-05-26 F1+F3 试错调优后）

`AudioUtils.SlicingAudioLegacy` 的 FFmpeg `-af` 滤镜链：

```
aresample=async=1:min_hard_comp=0.100:first_pts=0    ← 异步重采样
  ↓
highpass=f=80                                        ← 砍 sub-bass
  ↓
equalizer=f=200:width_type=q:width=1.5:g=-2          ← F3: 轻减 200Hz 减少低频闷腔感
  ↓
equalizer=f=2800:width_type=q:width=1.5:g=2          ← F3: 轻增 2800Hz 提升咝音/辅音清晰度
  ↓
dynaudnorm=f=500:g=15:r=0.5:n=1                      ← F1: 调宽帧 + 高斯窗 + RMS + 独立 channel
```

历史变迁：
- 初版（2026-05-26 11:00）：`aresample,highpass,dynaudnorm=f=200:g=5` + 短暂的 afftdn（已撤回）
- F1+F3（2026-05-26 调优）：加 EQ 整形 + dynaudnorm 参数放宽
- 触发：用户产线 A/B 数据显示 SVS 内容完整度 98% 但同音字错较多，尝试输入侧最后一刀边际优化

## 2. 失败模式定位（已修复）

| 失败现象 | 根因 | 本次修复 |
|---|---|---|
| 59s 段尾部 24s 完全无 content token | 后半段是游戏 BGM/音效，输入电平偏低，模型输出 Music/Sound meta token | dynaudnorm 把人声拉到可识别电平 |
| 解说员喊叫段识别破碎 | 输入电平时高时低，FBank 特征失稳 | dynaudnorm 平滑动态范围 |
| 爆炸/枪声瞬间识别中断 | sub-bass 能量进入 FBank 80 维 mel 滤波器组的低端 bin | highpass=80 切除低于 80Hz 内容 |

## 3. 反面案例（已撤回）：afftdn 杀 AI 配音

**初版尝试**：`afftdn=nr=12` 频域自适应降噪。

**失败机理**：afftdn 持续学习"稳态频谱"作为噪声基线。AI 配音 / TTS 的频谱极其稳态（缺少人类自然呼吸 / 共振抖动），被 afftdn 学习成噪声并从信号中减除，整段 AI 解说丢失。

**结论**：**任何在 ASR 输入侧加频域自适应降噪的方案都必须先验证 TTS/AI 音色保留**。本项目业务包含人 + AI 混合素材，频域降噪不可用；若未来需要抑制 BGM，应走 VAD 端点检测或人声分离（vocal separation），不走频域 stationary noise reduction。

## 4. 设计决策

### 4.1 为什么不加 lowpass
SVS 在 16kHz 全频带训练，4-8kHz 咝音（s/sh/f/v 等无声辅音）对中文识别必要。`lowpass=3400` 是早期电话带宽 ASR 习惯做法，对现代神经网络模型有害。

### 4.2 为什么用 dynaudnorm 而非 loudnorm
`loudnorm` 是 EBU R128 广播级响度归一，最佳效果需要双 pass（先测量再应用）。`dynaudnorm` 是动态 AGC，单 pass 实时处理，适合流式切片管道。识别精度差异可忽略。

### 4.3 为什么不加 afftdn / arnndn 等频域降噪
见 §3。频域 stationary noise reduction 与 TTS/AI 音色不兼容。未来 BGM 抑制方案走 P2/P3（VAD 或人声分离），不走滤镜级降噪。
