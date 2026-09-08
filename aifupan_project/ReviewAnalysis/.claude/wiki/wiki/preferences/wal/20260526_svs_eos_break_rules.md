# Rules WAL — 故意不用 FunASR 的 EOS-break 截断 (2026-05-26 撤回)

> **状态**：本规则于当日上线后**立刻撤回**。本文档保留为"为何不用"的反面案例，防止未来重新引入。
>
> 关联：FunASR 参考实现解读（memory · 跨机链接已失效，去链）、[BGM 内容保留策略](./20260526_asr_bgm_preservation_rules.md)

## 反面案例

### [SVS] [NEVER] 禁止在 FilterBlanksAndMetas 中加 `if (tid == 2) break;`

**FunASR 参考实现**（`OfflineRecognizer.cs:156`）使用此截断：
```csharp
if (token == 2) break;   // FunASR 的做法
```

**本项目曾尝试照搬，2026-05-26 实施后立即撤回**。

**撤回原因**：

| 维度 | FunASR 场景 | 本项目场景 |
|---|---|---|
| 输入音频 | 短的预分段干净语音 (chunk ≈ speech) | 59s 切片，常含"前段语音 + 后段 BGM/音效" |
| EOS 含义 | 真实序列结束 | 可能是"语音段结束"，但 BGM 还在继续 |
| EOS 后内容 | padding/garbage | **可能是合法的 BGM 歌词/音效文本** |
| 截断代价 | 几乎零 | **丢失 BGM 后段的合法 content** |

**用户策略**（[BGM 内容保留](./20260526_asr_bgm_preservation_rules.md)）：BGM 文字内容默认保留，仅在与人声叠加时可抑制。EOS-break 与此策略**直接冲突**。

**正确做法**：保持原行为 `if (tid <= 2) continue;`，把 EOS 当普通 blank 跳过，不截断后续 token。

## 经验教训

参考实现的设计假设必须先用本项目数据特征验证再照搬。FunASR/SVS 上游主要服务"已 VAD 切分的短语音"，与我们"59s 硬切的长混合段"分布不同。

未来若引入 EOS 相关处理，必须先验证：**模型在哪些情况下输出 token 2，token 2 出现在哪些位置，截断会丢什么**。
