# Domain Index (Vocabulary & State)

This index defines the project's vocabulary. The Agent MUST use these terms during `Explorer` and `Propose` to avoid domain drift.

## 业务域文档

| 业务域 | 核心概念 | 详情文档 |
|---|---|---|
| 主播与直播 | Anchor, Recording, BarrageGrab, WebSocket | `[anchor_live.md]` |
| AI 与分析 | ChatCompletions, Diagnosis, ShortVideo | `[ai_analysis.md]` |
| ASR 语音转文字 | IASREngine抽象, AsrEngineFactory, CompositeASREngine, SenseVoiceSmall本地推理, TencentASR云端, engSerViceType配置语法, CIF/CTC帧索引时间戳兜底, FBank+LFR特征, 重试策略, chunk-relative时间戳契约, CJK语速分组(3字+300ms), 标点附着前词, 上报字段PascalCase | **权威**: `[asr_engine_pipeline.md]` / **消费方映射**: `[asr_consumers.md]` / 历史: `[asr_module.md]` / 模型细节: `[asr_sensevoice_small.md]` / 评估: `[asr_local_engine_evaluation.md]` / WAL: `[wal/20260521_asr_audio_text_sync_domain.md]` / `[wal/20260519_asr_gapms_offset_domain.md]` / `[wal/20260515_asr_engine_fixes_domain.md]` / `[wal/20260514_asr_local_engine_domain.md]` |
| 平台对接 | Juliang, Qianchuan, Life, Enterprise, WeChatChannels, PlatformDataManager | `[platform_integration.md]` |
| 基础设施 | CefSharp, HttpServer, SQLite, 启动流程, 云存储 | `[infrastructure.md]` |
| 业务逻辑层 | AnchorBll, AnchorVideoBll, DouYinAnchorBll, AiRelatedBll 等 20+ Bll 类 | `[business_logic.md]` |

---

## Archive Extraction SOP
If an `openspec.md` introduces new terms, roles, enum values, or state transitions, the Agent MUST extract them here during `Archive`.

### Append Template
```markdown
| {term} | {1–2 sentence definition and boundary} | {Related Concepts / Synonyms} | `[{details_doc}]` |
```

Anti-bloat rule: if the vocabulary exceeds 30 concepts, you MUST split into per-line dictionaries (example: `dictionary_xxx.md`) and keep this file as a router.
