# Rules WAL — ASR 诊断日志规约与切片阈值 (2026-05-26)

> 关联前置: [audio_voice_enhancement_domain.md](../../domain/wal/20260526_audio_voice_enhancement_domain.md)、[svs_cif_underfire_fallback_domain.md](../../domain/wal/20260526_svs_cif_underfire_fallback_domain.md)

## 新增规则

### [ASR] [SHOULD] SVS 引擎每段必输诊断行

格式（已在 `SenseVoiceSmallEngine.RecognizeAsync` 落地）：
```
[SVS] 诊断 {fileName}: decoded={T} content={C} words={W} last_end={L}ms chunk={D}ms cif_peak={P}
```

**字段解读规约**（排查丢字时按层判定）：

| 信号 | 解读 | 应去查 |
|---|---|---|
| `C << T`（content 远小于 decoded） | meta/blank token 占多数（BGM / 纯音效段） | 输入侧降噪 / VAD ；**结合下方"过滤分解"日志**判断 blank 还是 meta 主导 |
| `W << C`（words 远小于 content） | MergeWords 中标点附着或时间戳 [-1,-1] 被丢 | 检查同段 `CIF under-fire` 日志；review MergeWords |
| `last_end << chunk` | 末尾 N 秒未产 content token（典型 BGM 段） | VAD 前置或人声分离 |
| `cif_peak == 0` | 模型未输出 cif_peak 张量（**已知现象**，2026-05-26 生产日志显示恒为 0） | 用"ONNX 输出张量"日志确认输出名 vs 我们读取的 `r.Name == "cif_peak"` 是否对得上 |

### 辅助诊断 1：过滤分解日志

格式：`[SVS] 过滤分解: total=T blank=B meta=M content=C`

每 chunk 1 行，回答"内容稀少是因为模型沉默(blank) 还是输出事件标签(meta)"：

| 信号 | 解读 |
|---|---|
| `blank >> meta` | 模型大部分帧"无法识别"，可能音频质量不足或非语音 |
| `meta >> blank` | 模型识别为事件 token（音乐/笑声/掌声/语种标签等），按 [BGM 保留策略](./20260526_asr_bgm_preservation_rules.md) 评估是否应保留 |
| `blank ≈ meta` | 混合场景，模型在不确定与事件识别之间 |

### 辅助诊断 2：ONNX 输出张量名（一次性）

格式：`[SVS] ONNX 输出张量: [0]name1, [1]name2, [2]name3, ...`

每进程仅打 1 次。用于：
- 排查 `cif_peak == 0` 根因
- 确认模型版本变更后输出名/顺序是否仍兼容代码假设

**禁止**：将本诊断日志改为 DEBUG 级别后默认关闭。SVS 是离线管道，日志频率 = 1 行 / chunk，开销可忽略，长期保留以便快速排查。

---

### [Audio] [NEVER] 禁止用文件大小阈值做静音检测

**反模式**：`if (fileSize < 10 * 1024) File.Delete(tempFile);` 当作"无声音段过滤"。

**原因**：
1. 文件大小与声音内容**无直接关系**（PCM 即使全静音也占 32 字节/ms 空间）
2. 业务上的"无声音"应由 dB 测量、VAD 或上游检测完成
3. 用大小阈值会**误杀视频末尾的短有效尾段**（10KB ≈ 0.3s @ 16kHz mono PCM）

**正确做法**：
- 文件大小检查**仅做最弱兜底**——过滤 ffmpeg 偶发产出的空文件 / 头损坏文件
- 阈值 ≤ 1KB（≈32ms）：低于此长度的 wav 实际无法承载任何有意义内容
- 真正的静音过滤交给 VAD 模块或 dB 测量（未来 T2.1）

**已修复**：`AudioUtils.cs:131` 阈值 10KB → 1KB。

## 关联已有规则

- 与 [audio_voice_enhancement_rules.md](./20260526_audio_voice_enhancement_rules.md) 互补：前者约束输入流的人声增强；本规则约束切片产物的过滤兜底与运行时观测
- 与 [svs_cif_underfire_fallback_rules.md](./20260526_svs_cif_underfire_fallback_rules.md) 互补：前者修复"识别出又丢"，本规则提供观测来定位"还没识别"
