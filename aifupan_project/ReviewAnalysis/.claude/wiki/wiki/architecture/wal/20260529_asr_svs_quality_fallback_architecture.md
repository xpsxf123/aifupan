---
date: 2026-05-29
feature: asr_svs_quality_fallback
type: architecture
run_id: 20260529_124517_asr_svs_quality_fallback
related_specs:
  - .claude/runs/20260529_124517_asr_svs_quality_fallback/openspec.md
related_adr: []
related_wal:
  - [[20260529_svs_ffmpeg_filter_tune_rules]]
  - [[20260529_svs_ffmpeg_filter_tune_architecture]]
  - [[20260529_asr_svs_quality_fallback_rules]]
---

# Architecture WAL — Composite 装饰器扩展：Chunk-level LowQuality 降级路径（2026-05-29 路线 A）

## Change Summary

`CompositeASREngine` 在原有"内层引擎失败切下一引擎"循环上，新增一条与之并存不互斥的"成功但 LowQuality → 直接定位 Tencent fallback"分支，实现 chunk-level 智能切换。路线 A 完整落地，与路线 B FFmpeg 滤镜改动协同构成 SVS badcase 完整解决方案。

## Impact

- 路线 B（滤镜调优）+ 路线 A（输出侧降级）双重防线：前者消除轻声/混响的信号层面劣化，后者在 CTC fundamental 行为触发时接管
- `CompositeASREngine` 对消费方合约不变（`IASREngine` 接口不变，`code=0` 语义兼容）
- 三大消费方无需修改；`TencentASREngine` 无需新注入

---

## 1. Composite 装饰器扩展模式：两条降级路径并存不互斥

既有路径（不修改）：`foreach (_entries)` 循环中，inner engine 抛异常或 `code≠0` → `continue` → 下一引擎。

新增路径（与既有并存）：inner engine 成功（`code==0`）但 `IsLowQuality` 为 true → 直接定位 `TencentASREngine` 实例调用，不进入下一轮循环迭代。

两者逻辑位置不同（`continue` 分支 vs `code==0` 分支后插入），设计上明确互斥语义：失败降级走循环自动切换，质量降级走显式定位。

[Confidence: HIGH]
[Evidence: Asr/CompositeASREngine.cs:48-60]

---

## 2. TencentASREngine 工厂注入关系

`AsrEngineFactory.BuildDefaultEntries` 对 SVS 支持语言返回 `[SenseVoiceSmallEngine, TencentASREngine]`，Tencent 已在 `_entries` 中。`TryTencentFallbackAsync` 通过 `_entries.Find(e => e.Engine is TencentASREngine)` 按类型定位，使用其 `Config`（即语言 code）直接调用，不新增注入、不新增构造参数。

若后端仅下发 `["sense-voice"]`（`_entries` 中无 Tencent），`tencentEntry == null`，记录 `rule=no_tencent_engine action=KEPT_SVS` 后保留 SVS 结果，不中断识别。

[Confidence: HIGH]
[Evidence: .claude/runs/20260529_124517_asr_svs_quality_fallback/openspec.md §1, Asr/CompositeASREngine.cs:94-100]

---

## 3. Chunk-level 智能切换实证（2026-05-29 Windows smoke）

637s 唱歌+说话混合直播录播（11 chunks，路线 B 滤镜已生效）：

| chunk | collapsed/decoded | 判定 | 结果 |
|---|---|---|---|
| audio_000–007, 009–010（唱歌段） | 0.988–0.995 | rule1 触发 | REPLACED（Tencent 接管） |
| audio_008（说话段） | 0.867 | rule1 未触发 | KEPT_SVS（SVS 保留，说话正常） |

Tencent 调用：10/10 成功（`code=0`），无一触发 `KEPT_SVS` 兜底。81 chunks 全局 12.3% 触发率（符合混合内容说话为主的预期）。规则 2 本次 0 次触发——规则 1 已充分 cover 唱歌段。

[Confidence: HIGH]
[Evidence: VERBATIM:"""2026-05-29 Windows smoke: audio_000-007/009/010 ratio 0.988-0.995 REPLACED; audio_008 ratio 0.867 KEPT_SVS; 10/10 Tencent code=0"""]

---

## 4. v2 设计简化反思：先工程后简化案例

**v1 过度工程化设计**（已废除）：
- 独立 `QualityAssessor.cs`（约 60 行类 + 接口）封装 2 条规则
- `AsrUtils.cs` 解析 `svs:auto:withitn|tencent` 配置串

**用户 review 纠错（2026-05-29）**：
1. 1 行 OR 判断不值得抽 60 行类（YAGNI 信号：trivial 逻辑不抽）
2. 后端配置串已过时，不应在 ASR 层重新引入配置语言

**v2 最终方案**：`IsLowQuality` 回归 `CompositeASREngine` 内部（`private static`，无状态，无锁），共 30 行；`ASRResultEntity` 加多指标字段；`AsrUtils.cs` 零改动；新增文件数 = 0。

这是"主动过设计 → 用户 review → 回归简单"的典型正反馈案例。YAGNI 约束在 BLL 层方法体量 trivial 时应优先于接口抽象。

[Confidence: HIGH]
[Evidence: .claude/runs/20260529_124517_asr_svs_quality_fallback/openspec.md §1, §6]

---

## 5. 路线 B + 路线 A 协同架构（完整解决方案）

| 层面 | 路线 B（已 Archived） | 路线 A（本 WAL） |
|---|---|---|
| 干预位置 | 输入侧：FFmpeg 滤镜链（`AudioUtils.cs:150`） | 输出侧：Composite 内 `IsLowQuality` 分支 |
| 根治目标 | 轻声/混响前处理信号劣化（badcase 2/3） | CTC fundamental 唱歌 blank 行为（badcase 1） |
| 对常规口播影响 | 字符密度 1.92-3.5 char/s，无回归 | `IsLowQuality=false`，零额外延迟 |
| 能否单独根治 badcase 1 | 否（CTC 模型行为无法滤镜修复） | 是（Tencent 接管唱歌段） |

两条路径无共享状态，可独立回滚。回滚判据：`action=REPLACED` 但最终 Result 质量下降（误触发口播段），或 Tencent 超时导致整段延迟 > 30s。

[Confidence: HIGH]
[Evidence: .claude/runs/20260529_124517_asr_svs_quality_fallback/openspec.md §1, §8]
