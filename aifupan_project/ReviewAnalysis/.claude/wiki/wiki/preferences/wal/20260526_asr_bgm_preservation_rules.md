# Rules WAL — BGM 内容保留策略 (2026-05-26)

> 关联反面案例: [EOS-break 撤回](./20260526_svs_eos_break_rules.md)
> 关联 feedback memory: `[[asr-bgm-preservation-policy]]`

## 新增策略规则

### [ASR] [MUST] BGM 文字内容默认保留，不允许无条件抑制

**策略**：

| 场景 | 处理 |
|---|---|
| **无人声 + BGM 含可识别文字（歌词等）** | **必须保留** BGM 文字内容 |
| 人声 + BGM 叠加 | 可优先保留人声，允许抑制 BGM 干扰 |
| 纯人声 | 保留人声 |

**原因**：

业务场景里 BGM 段的合法文字内容（歌词、节目提示音文字、AI 配音段、字幕同步歌词）是有价值的输出。"无人声时也抹掉 BGM 文字" 等于丢失合法识别内容，不符合用户对 ASR 系统的预期。

**禁止**：

- ❌ 全局无条件丢弃低置信度 token（可能误伤 BGM 歌词）
- ❌ 整段抑制 non-speech 区间（如基于 `<\|Music\|>` 事件 token 把整段 content 丢弃）
- ❌ EOS-break 类截断（语音末尾 EOS 会丢失后续 BGM 段，见 [eos_break_rules.md](./20260526_svs_eos_break_rules.md)）

**允许**：

- ✅ token 级软过滤（如置信度门），但阈值要保守，确保 BGM 歌词通常能通过
- ✅ 人声增强滤镜（dynaudnorm 等响度归一）—— 提升人声而不破坏 BGM 文字
- ✅ 未来若实现 speaker-aware 处理：在人声 + BGM 叠加段优先保人声

---

### [ASR] [SHOULD] 候选 A（事件 token VAD）必须遵守此策略

未来若实现"事件 token VAD"（用 `<\|Music\|>` 等事件 token 做区间过滤），**必须不能**简单"把音乐区间所有 content 抹掉"。

正确做法（待实施）：
- 检测 `<\|Music\|>` 区间，但只**降低**而不**清零**该区间 content 的可信度
- 或：仅在 `<\|Speech\|>` 事件 token 也同时出现时（说明人声重叠），才适度抑制 BGM 部分
- **默认输出 BGM 文字**，让消费方在业务层决定是否使用

## 已对照检查的现有改动

| 改动 | 是否符合策略 |
|---|---|
| CIF under-fire fallback | ✅ 符合（仅改时间戳） |
| FFmpeg highpass + dynaudnorm | ✅ 符合（增强不抑制） |
| 10KB→1KB 阈值 | ✅ 符合（保留更多内容） |
| 诊断日志 | ✅ 符合 |
| 置信度门 0.30 | ⚠️ **需复核**——阈值是否过激导致 BGM 浑浊歌词被砍。当前保留观察 |
| EOS-break | ❌ **违反，已撤回** |

## 与短视频验证规则的关系

补充 [asr_short_video_validation_rules.md](./20260526_asr_short_video_validation_rules.md) 第 5 类样本：
- **新增第 6 类必测样本**：**短视频 — 纯 BGM 含歌词（无人声片段）** —— 验证 BGM 文字内容不被抹掉
