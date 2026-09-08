---
date: 2026-05-29
feature: svs_ffmpeg_filter_tune
type: rules
run_id: 20260529_111437_svs_ffmpeg_filter_tune
related_specs:
  - .claude/runs/20260529_111437_svs_ffmpeg_filter_tune/openspec.md
related_adr: []
related_wal:
  - [[20260526_audio_voice_enhancement_rules]]
  - [[20260529_svs_ffmpeg_filter_tune_architecture]]
---

# Rules WAL — SVS FFmpeg 滤镜调优规则（2026-05-29 路线 B）

> **覆写声明**：本 WAL 覆写 [[20260526_audio_voice_enhancement_rules]] 中 CPU 开销表与滤镜链完整参数部分。历史 WAL 保持不变（wal-policy §6 anti-pattern: 不修改历史 WAL）。所有新实施应以本文为准。

## Change Summary

FFmpeg 音频前处理滤镜链定版（路线 B 完工）：200Hz 段方向反转 (-2dB → +3dB) 补人声基频；新增 6kHz highshelf +1.5dB 提升辅音；dynaudnorm 温和化 (f=500→200 / g=15→5 / r=0.5→0.7) 补轻声 + 抑制噪声放大。同时 `ErrorDataReceivedHandler` 新增真错误关键字白名单（boundary exception）。

## Impact

- 三大消费方（AnchorReplay / UserUpload / ShortVideo）共享同一 `SlicingAudioLegacy` 路径，本规则自动生效，无需分别适配
- 4 视频 Windows smoke 实测：常规口播 3/3 无回归（字符密度 1.92-3.5 char/s），badcase 1（粤语唱歌）字符密度 0.4 char/s 改善有限
- badcase 1 改善有限由路线 A（SVS 质量评估 + Tencent 自动降级）接管度量；阈值参考 §5 CTC collapsed/total 比例表
- 覆写 [[20260526_audio_voice_enhancement_rules]] 滤镜参数部分（旧 200Hz -2dB / dynaudnorm f=500:g=15 失效）；BGM 保留规则 [[20260526_asr_bgm_preservation_rules]] 不变

---

## 1. 当前定版滤镜链完整字符串（2026-05-29）

```
aresample=async=1:min_hard_comp=0.100:first_pts=0,
highpass=f=80,
equalizer=f=200:t=q:w=1:g=3,
equalizer=f=2800:t=q:w=1:g=2,
equalizer=f=6000:t=h:w=1:g=1.5,
dynaudnorm=f=200:g=5:r=0.7:n=1
```

**落地位置**：`Utils/AudioUtils.cs:150`（`SlicingAudioLegacy` 中 `arguments` 变量）。

[Confidence: HIGH]
[Evidence: Utils/AudioUtils.cs:150]

---

## 2. 各参数 badcase 对应关系

| 参数 | 变更 | 针对 badcase | 机理 |
|---|---|---|---|
| `equalizer=f=200:g=3` | -2dB → +3dB（方向反转） | badcase 1 轻声/唱歌，badcase 3 多人轻声 | 200Hz 是人声基频区（男 85-180Hz，女 165-255Hz），旧 -2dB 削弱轻声基频；+3dB 补偿先天不足 |
| `equalizer=f=2800:g=2` | 不变（g=2） | badcase 2 多人混说 | 保持咝音/辅音能量提升（AC3 要求 ≥+2dB 下界） |
| `equalizer=f=6000:t=h:g=1.5` | 新增 highshelf | badcase 2 多人混说，badcase 1 轻声唱歌 | 6kHz+ 辅音/齿音带（s/sh/ch 等）提升辨别能力 |
| `dynaudnorm=f=200` | f=500→200 | badcase 3 多人轻声断续 | 缩短帧窗，200-500ms 话轮切换内轻声不再落入"分析盲窗" |
| `dynaudnorm=g=5` | g=15→5 | badcase 3，噪声放大抑制 | 高斯窗收窄，低能量背景噪声段不被整体大幅拉升 |
| `dynaudnorm=r=0.7` | r=0.5→0.7 | badcase 1/3 轻声识别 | 目标 RMS 提高约 +2.9dB，轻声内容更大概率超过 SVS 端点检测阈值 |

[Confidence: HIGH]
[Evidence: Utils/AudioUtils.cs:141-145, openspec.md §5.3]

---

## 3. 200Hz 方向判据 — 何时正增益、何时负增益

| 场景 | 推荐方向 | 理由 |
|---|---|---|
| **口播为主（无轻声/唱歌 badcase）** | g=0 ~ g=-2（旧值） | 200Hz 负增益减低频闷腔感，提升咬字清晰度 |
| **轻声/混响唱歌/多人轻声（badcase 1/3）** | g=+2 ~ g=+3（新值） | 补偿人声基频能量，使 SVS 端点检测更易触发 |
| **smoke test 显示常规口播闷腔感增加** | 折中至 g=0 ~ g=+1 | openspec §5.2 QA 阶段备选，无需走 Phase 2 |

[Confidence: HIGH]
[Evidence: openspec.md §5.1 D-0, openspec.md §5.3 变更点(a), Utils/AudioUtils.cs:142-143]

---

## 4. 禁用滤镜静态白名单（AC1 grep 保证）

```bash
grep -E "(afftdn|arnndn|silenceremove|lowpass=f=[1-7]|anlmdn|afir)" Utils/AudioUtils.cs | grep -vE "^\s*//"
# 必须返回 0 行（注释行不计）
```

| 禁用滤镜 | 禁用原因 |
|---|---|
| `afftdn` | 2026-05-26 实测：将 AI 配音/TTS 学习为噪声并整段抹除 |
| `arnndn` | 同类频域降噪 + CPU 3-5x 实时超出 30min 超时预算 |
| `silenceremove` | 切掉无人声区间 = 切掉 BGM 歌词，违反 bgm_preservation_rules |
| `lowpass=f=[1-7]` | 截断辅音能量带（<8kHz），中文同音字辨别退化 |
| `anlmdn` | 自适应非局部均值降噪，同类频域降噪问题 |
| `afir` | FIR 滤波器通用性过强，可能误配置为低通 |

[Confidence: HIGH]
[Evidence: openspec.md §5.5, Utils/AudioUtils.cs:146]

---

## 5. CTC collapsed/total 比例 — 唱歌/低质量判据

| collapsed/total 比例 | 解读 | 建议行动 |
|---|---|---|
| < 90% | 正常说话段 | 无需干预 |
| 90%–95% | 边界区，可能含轻声/混响 | 路线 A 记录 metric，观察 |
| > 95% | 唱歌段或极低质量音频 | 路线 A 可触发 Tencent 降级或跳过 |

**来源**：视频 3 花生酱酱粤语唱歌 232s，collapsed/total=91-98%，字符密度 0.4 char/s（正常 ~2 char/s）。

[Confidence: MEDIUM]
[Evidence: VERBATIM:"""视频3 花生酱酱粤语唱歌 232s → 字符密度 0.4 char/s, collapsed/total=91-98%"""]

---

## 6. Smoke 实测字符密度对照表（2026-05-29，DirectML EP）

| 视频 | 时长 | 内容 | 字符密度 | blank% | 结论 |
|---|---|---|---|---|---|
| 蓝叙女装 | 130s | 单主播口播 | 1.92 char/s | ~30% | 无回归 ✅ |
| 交个朋友 | 208s | 单主播口播 | 2.18 char/s | ~28% | 无回归 ✅ |
| 花生酱酱 | 232s | 粤语唱歌 | 0.4 char/s | collapsed ~95% | badcase 1：滤镜调优改善有限（fundamental CTC 行为）⚠️ |
| 小七天青色 | 250s | 单主播口播 | 3.5 char/s | — | 无回归 ✅ |

FFmpeg 处理速度：~1220x 实时（130s 音频约 106ms 处理完）。

[Confidence: HIGH]
[Evidence: VERBATIM:"""视频1-4 smoke 实测数据（2026-05-29 DirectML EP）"""]

---

## 7. FFmpeg stderr 日志分级规则（Boundary Exception，2026-05-29）

`ErrorDataReceivedHandler` 新增 `FFmpegRealErrorMarkers` 白名单：`Error opening` / `Error while` / `Invalid argument` / `Invalid data` / `Permission denied` / `No such file` / `Could not find` / `Could not open` / `Failed to` / `Conversion failed`。命中才标"FFmpeg错误"，其余标普通"FFmpeg"。

**原因**：FFmpeg 版本信息、stream info、进度均打到 stderr，退出码 0 时均为正常 info，不应标错误。

[Confidence: HIGH]
[Evidence: Utils/AudioUtils.cs:297-318]
