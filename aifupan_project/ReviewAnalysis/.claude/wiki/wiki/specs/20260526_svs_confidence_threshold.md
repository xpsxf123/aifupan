spec_mode: SLIM

# Change Summary
- What changed: `SenseVoiceSmallEngine.GreedyDecode` 加入 0.30 softmax 置信度门，低置信度 token 强置 blank（被下游过滤）。
- Why: 减少 BGM/噪声段的幻觉 token（候选 B from 优化路线图）。

# Scope of Change
- `Asr/Local/SenseVoiceSmallEngine.cs`（新增 const + GreedyDecode 内增量；其他文件未动）

# Risk & Rollback
- Why LOW: 单方法局部改动；行为单调（阈值越高过滤越多）；阈值设为 0 等同禁用；GreedyDecode 返回契约不变（长度 == actualSeqLen）；不影响 CIF/timestamps 链路。
- Rollback: const 改为 0f → 等同禁用，无需重启或迁移。彻底回滚：`git checkout HEAD -- Asr/Local/SenseVoiceSmallEngine.cs`。

# Verification & Evidence
- Mac 不可编译；QA 走 csharp-code-review 静态审查，9 项 PASS（数值稳定/性能/契约/日志/反模式/3 消费方影响均验证）。
- Windows 端运行时验证按 [短视频 A/B 验证规则](../preferences/wal/20260526_asr_short_video_validation_rules.md) 5 类样本执行：
  1. 主播游戏直播 → 预期：BGM 段幻觉字减少（words 数下降但 last_end 接近 chunk）
  2. 用户清晰人声 → 预期：与改前持平（high-conf 不被过滤）
  3. 短视频 + BGM + 人声 → 预期：人声保留，BGM 杂字减少
  4. **短视频 + BGM + AI 配音** → 关键测试：AI 解说必须完整保留
  5. 短视频 + 字幕同步 → 预期：与改前持平
- 量化指标：每 chunk 日志看 `[SVS] 低置信度过滤: X/Y 帧`，比对 X/Y 比率
  - BGM 重段：X/Y > 30% 是合理（说明在过滤幻觉）
  - 清晰人声段：X/Y < 5% 是健康（说明没误杀）
  - 若 X/Y > 50% 持续出现 → 阈值过严，降到 0.2
- 阈值调参指南：默认 0.30 → 偏激进可 0.20 → 偏保守可 0.40
