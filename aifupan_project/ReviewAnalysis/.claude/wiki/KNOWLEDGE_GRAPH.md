# LLM Wiki Knowledge Graph (Root Index)

This file is the root of the wiki. Use it to navigate by drilling down through indexes. Do not guess paths.

## Hard Rules (MUST)
- *Note: All paths are relative to the directory of this KNOWLEDGE_GRAPH.md file.*
- You MUST start navigation from this file, then drill down via `index.md` files. Read maximum 1-2 index files per step, analyze, and then decide the next exact file to read.
- You MUST NOT jump directly to random documents by guessing paths.
- Any new stable knowledge MUST be attached to this tree (via the correct domain). When updating knowledge, the agent MUST update `KNOWLEDGE_GRAPH.md` or the relevant domain `index.md` first to ensure no orphan docs are created.


## 1. Philosophy & Templates
- **[Purpose](purpose.md)**: why this system exists and what it optimizes for.
- **[OpenSpec Schema](schema/openspec_schema.md)**: the contract template for proposals and designs.
- **Skills**: 可用的专家 skills 位于 `.claude/skills/`，每个子目录含自己的 SKILL.md。

## 2. Active Domains (Drill-down Indexes)
- **[Domain](wiki/domain/index.md)**: 业务领域词汇表 — 主播、平台、录制、诊断等核心概念定义
  - **[ASR 引擎管道架构](wiki/domain/asr_engine_pipeline.md)** ⭐ 权威文档 — 双引擎架构/配置语法/推理管道/约束反模式（2.6.0-for-local-asr起）
  - **[ASR 消费方映射](wiki/domain/asr_consumers.md)** — 主播回放/上传文件/短视频 3 大消费方的引擎配置/音频特征/改动风险矩阵
- **[API](wiki/api/index.md)**: 内部 API、Controller 路由、平台对接接口
- **[Data](wiki/data/index.md)**: SQLite 数据表结构、缓存策略、存储路径
- **[Architecture](wiki/architecture/index.md)**: 架构总览 & ADR 决策记录 (含 [项目总览](wiki/architecture/project_overview.md))
- **[Specs](wiki/specs/index.md)**: 活跃的 openspec.md 设计提案
- **[Testing](wiki/testing/index.md)**: 测试标准和验证要求
- **[Reviews](wiki/reviews/index.md)**: 评审产物、PR 设计评审、审计报告
- **[Preferences](wiki/preferences/index.md)**: 项目约束、安全规则、反模式清单
  - [coding_standards.md](wiki/preferences/coding_standards.md) — C# 编码规范 (命名/异步/异常/资源管理)
  - [architecture_rules.md](wiki/preferences/architecture_rules.md) — 分层规则、DI 模式、类设计、线程安全
  - [platform_module_rules.md](wiki/preferences/platform_module_rules.md) — 平台模块基类模式、新增平台 Checklist
  - [security_rules.md](wiki/preferences/security_rules.md) — SQL 注入防护、敏感数据处理
  - [performance_rules.md](wiki/preferences/performance_rules.md) — HttpClient 复用、线程规范、缓存策略

## 3. WAL 碎片索引 (2026-05-15)
- **[Domain WAL — ASR后处理修复](wiki/domain/wal/20260515_asr_engine_fixes_domain.md)**: CIF时间戳对齐、标点修复、FilterSpecialTokens简化
- **[API WAL — ASR引擎API变更](wiki/api/wal/20260515_asr_engine_fixes_api.md)**: MyCallableTask签名/StartAsTaskAsync/EngineEntry简化
- **[Rules WAL — ASR代码审查规则](wiki/preferences/wal/20260515_asr_engine_fixes_rules.md)**: 7条工程规则（HttpClient/Thread.Sleep/DTO污染/CIF对齐等）

## 3.4 WAL 碎片索引 (2026-05-29，addOrUpdateAnchor 错误码透传)
> 来源：话术监控 addOrUpdateAnchor 改造（run 20260529_155758），关联 ADR-002
- **[Domain WAL — addOrUpdateAnchor 结果/错误回传机制](wiki/domain/wal/20260529_addoranchor_result_notify_domain.md)**: 两段式返回（HTTP ack ≠ 结果；FrontNotice `notifyFromCSharp` action=`addAnchorEnd`）+ MethodCache 反射分发与异常→{code,msg}
- **[Rules WAL — HTTP 错误透传与缓存 3 坑](wiki/preferences/wal/20260529_http_error_passthrough_rules.md)**: HttpUtils 吞码 vs HttpAsyncUtils 抛；GetAnchorByIdFromCache 活引用需深拷贝；透传开关禁 `?? 0`

## 3.3 WAL 碎片索引 (2026-05-29)
> 来源：`/h-distill-from-code` 代码→wiki 对账，对 `platform_integration.md` 全文与平台模块代码对齐
- **[Domain WAL — 平台授权状态枚举补全 (Life/Enterprise)](wiki/domain/wal/20260529_platform_auth_status_enums_domain.md)**: Life/Enterprise 实为 7 态枚举（authFailed/authing/accountMismatched/subNoPermission），wiki 原仅记 AnchorLive 4 态
- **[Domain WAL — plugins 采集层修正 + 调度层补全](wiki/domain/wal/20260529_plugins_collector_layer_correction.md)**: Collector 段无 Full 模式、JuliangApiCollector 孤立、实际 5 类均 IPlatformCollector；补全 CollectionScheduler + BaseDataPuller
- **[Domain WAL — Collector 表 form-B 订正审计](wiki/domain/wal/20260529_plugins_collector_table_replacement_audit.md)**: platform_integration.md Collector 表直接订正的 before/after/reason 审计记录
- **[Domain WAL — douyin Cookie 判据修正 + juliangApi DataHandle 补全](wiki/domain/wal/20260529_douyin_cookie_drift_correction.md)**: douyin 有效性仅看含 sessionid 的 cookie（passport_csrf_token/odin_tt 无引用）；补 JuliangApiDataHandle；含 index.md:17 form-B 订正记录

## 3.2 WAL 碎片索引 (2026-05-28)
- **[Architecture WAL — 工作流重构（借鉴 java-harness-agent）](wiki/architecture/wal/20260528_workflow_refactor_java_harness_architecture.md)**: `.claude/` 目录单 SSOT、双入口符号链接、Skill Zone 分组、5 项 java 高收益设计植入、资产保护 14 AC
- **[Rules WAL — 工作流重构新规则](wiki/preferences/wal/20260528_workflow_refactor_java_harness_rules.md)**: R1-R8 新规则（Triage Probe / 4 级 Risk / Zone 互斥 / dispatch 5-section / WAL Confidence+Evidence / Wiki Anti-Bloat / csproj 注册等）+ AP1-AP5 新反模式
- **[ADR-001 — 工作流重构架构决策](wiki/architecture/adr/ADR-001-workflow-refactor-java-harness.md)**: HIGH 风险架构决策记录，含 Alternatives Considered（渐进/中度/完全照搬）+ 14 AC 验证结果 + Rollback 方案

## 3.1 WAL 碎片索引 (2026-05-26)
- **[Domain WAL — SVS CIF Under-fire 降级行为](wiki/domain/wal/20260526_svs_cif_underfire_fallback_domain.md)**: ComputeTimestamps 双分支语义、CIF 欠 fire 失败模式描述
- **[API WAL — SVS ComputeTimestamps 行为变更](wiki/api/wal/20260526_svs_cif_underfire_fallback_api.md)**: 私有方法前置/后置条件、新增 under-fire 日志
- **[Rules WAL — CIF/CTC 时间戳混用禁令](wiki/preferences/wal/20260526_svs_cif_underfire_fallback_rules.md)**: 新增 2 条 SVS 反模式（禁混用时间戳源、禁 [-1,-1] 占位）
- **[Domain WAL — ASR 输入侧人声增强](wiki/domain/wal/20260526_audio_voice_enhancement_domain.md)**: FFmpeg 3 段滤镜链（aresample + highpass + dynaudnorm），保留 AI/TTS 配音；含 afftdn 被撤回的反面案例
- **[Rules WAL — ASR 输入侧 FFmpeg 滤镜规则](wiki/preferences/wal/20260526_audio_voice_enhancement_rules.md)**: 禁 lowpass < 8kHz、**禁 afftdn/arnndn 频域降噪（杀 TTS）**、推 dynaudnorm、A/B 验证集要求
- **[Rules WAL — ASR 诊断日志规约与切片阈值](wiki/preferences/wal/20260526_asr_diagnostics_and_threshold_rules.md)**: SVS 每段诊断格式 `decoded/content/words/last_end/chunk/cif_peak` + 字段排查表；禁用文件大小做静音检测；切片阈值 10KB→1KB
- **[Rules WAL — ASR 改动必须包含短视频 A/B 验证](wiki/preferences/wal/20260526_asr_short_video_validation_rules.md)**: 5 类最低验证集（含纯 AI 配音必测）；PR 描述模板；适用文件清单
- **[Rules WAL — SVS 解码置信度门](wiki/preferences/wal/20260526_svs_confidence_threshold_rules.md)**: GreedyDecode 后 softmax<0.30 强置 blank；log-sum-exp 数值稳定；调参指南；与 CIF/滤镜规则协同关系
- **[Rules WAL — EOS-break 撤回反面案例](wiki/preferences/wal/20260526_svs_eos_break_rules.md)**: 故意不照搬 FunASR 的 EOS-break；保留为反面案例防止未来重新引入
- **[Rules WAL — BGM 内容保留策略](wiki/preferences/wal/20260526_asr_bgm_preservation_rules.md)**: BGM 文字默认保留，仅在与人声叠加时可抑制；候选 A 实施必遵守；新增第 6 类必测样本
- **[Rules WAL — 跨段拼合规则 + Word 修改边界](wiki/preferences/wal/20260526_asr_cross_chunk_merge_rules.md)**: 5 层保守过滤；明确 Word 字段允许"语义合并"禁止"格式污染"的精确边界
- **[Rules WAL — VAD 智能切片 Phase 1 架构规约](wiki/preferences/wal/20260526_vad_phase1_architecture_rules.md)**: IVadEngine 接口 / SmartSlicer 算法 / 路由 + 降级 + 开关 (AsrVadEnabled)；Stub 实现，Phase 2 替换为真实引擎

## 4. Cold Storage
- **[Archive](archive/index.md)**: extracted or obsolete documents kept for traceability.

---

## 5. Anti-Bloat 规则 (新增 2026-05-28，借鉴 java-harness-agent)

### 5.1 文件行数硬约束

| 类型 | 默认上限 | 例外目录上限 |
|---|---|---|
| Wiki 文档（business/api/data/preferences） | **3000 行** | — |
| 报告类（`wiki/reviews/`、`wiki/architecture/reports/`） | — | **10000 行** |

超限触发：
```bash
python3 .claude/scripts/wiki/anti_bloat_check.py --root .claude/wiki/
```

FAIL 时 @knowledge-architect 角色动态挂载，拆分大 index 为子文档 + 更新路由链接 + 跑 `wiki_linter.py` 验证无死链。

### 5.2 WAL 自动索引规则

新 WAL 写入 **必须** 同步更新本文件的 §3.X 章节（按日期分组）：

```markdown
## 3.X WAL 碎片索引 (YYYY-MM-DD)
- **[Domain WAL — <Title>](wiki/<domain>/wal/<file>.md)**: <一句话摘要>
- **[Rules WAL — <Title>](wiki/preferences/wal/<file>.md)**: <一句话摘要>
```

详细 WAL 写回规则见 [.claude/rules/wal-policy.md](../../.claude/rules/wal-policy.md)。

### 5.3 双入口（新增 2026-05-28）

本 wiki 现有两个等效访问入口：

- 原物理路径：`.claude/wiki/`
- 新符号链接入口：`.claude/wiki/` → `../.claude/wiki/`

物理文件唯一存放在原路径下。新入口仅为统一 java-harness 风格的 `.claude/` 目录结构。

## 6. 元工作流入口（新增 2026-05-28）

- **[CLAUDE.md](../../CLAUDE.md)** — 单一会话入口（合并自 AGENTS.md）
- **[.claude/rules/](../../.claude/rules/)** — 规则 SSOT（lifecycle / policy / risk-profiles / skill-precedence / dispatch-template / wal-policy）
- **[.claude/agents/](../../.claude/agents/)** — 角色定义 9 文件（含新增 sqlite-reviewer）
- **[.claude/commands/](../../.claude/commands/)** — Phase 触发命令（h-explore / h-propose / h-implement / h-qa / h-archive）
- **[.claude/scripts/triage_probe.py](../scripts/triage_probe.py)** — 5 信号客观风险打分
