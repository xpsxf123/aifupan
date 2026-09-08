# Active Specs (OpenSpec)

This index lists `openspec.md` documents that are currently in progress or recently finished and still frequently referenced.

## Hard Rules (MUST)
- When a spec reaches `Archive` and its stable knowledge has been extracted, you MUST move it out of the active list.
- After extraction, long-term knowledge lives in `api/`, `data/`, and `domain/` indexes. The spec remains only for traceability.

## In Progress / Recent

| Feature | Status | Link |
|---|---|---|
| Codesign, Logger & UI Polish | `Phase 3: Review / WAITING_APPROVAL` | `[20260422_codesign_logger_ui_polish.md]` |
| 本地 ASR 引擎集成 (SenseVoiceSmall) | `Phase 2: Propose (来源)` | `[20260513_asr_local_engine_prd.md]` |
| 本地 ASR 引擎集成 — 统一文档 (PRD+架构) | `Phase 2: Propose (来源)` | `[20260513_asr_local_engine_unified.md]` |
| 本地 ASR 引擎集成 — 方案设计白皮书 | `Phase 2: Propose (来源)` | `[20260513_asr_local_engine_whitepaper.md]` |
| **本地 ASR 引擎集成 — OpenSpec** | `Phase 4: Archive (Implemented)` | `[20260514_asr_local_engine_openspec.md]` |
| **SVS CIF Under-fire → CTC Fallback (直播解说大段丢字修复)** | `Phase 4: Archive (Implemented, awaiting Windows runtime verification)` | `[20260526_svs_cif_underfire_fallback.md]` |
| **SVS GreedyDecode 置信度阈值过滤 (BGM 幻觉减少)** | `Phase 4: Archive (Implemented, awaiting Windows A/B)` | `[20260526_svs_confidence_threshold.md]` |
| **AsrUtils 跨段 Word 拼合 (修复 59s 硬切断字)** | `Phase 4: Archive (Implemented, awaiting Windows A/B)` | `[20260526_asr_cross_chunk_word_merge.md]` |
| **FSMN-VAD 智能切片 (替代 FFmpeg 59s 硬切)** | `Phase 4: Archive (Phase 1 架构落地 已实施，Phase 2 真正 VAD 引擎待新 spec)` | `[20260526_fsmn_vad_smart_slicing.md]` |
| **FFmpeg F1+F3 试错调优 (EQ 整形 + dynaudnorm 放宽)** | `Phase 4: Archive (Implemented, awaiting Windows A/B)` | `[20260526_ffmpeg_f1_f3_tuning.md]` |
| **ConvertToMP4 优先 -c copy remux (省 80% CPU 重编码)** | `Phase 4: Archive (Implemented, awaiting Windows A/B)` | `[20260526_video_convert_remux_first.md]` |

---

## Lifecycle SOP
- After `Propose`: add a new row with status `Phase 3: Review`.
- After `Archive`: remove it from the active list, and move it to "Recently Archived" (or fully transfer to `archive/index.md`).

### Append Template
```markdown
| {feature name} | `{current phase}` | `[{file_name.md}]` |
```

## Recently Archived
(Knowledge extracted into api/data/domain. This section is read-only traceability.)

### Append Template (Archived)
```markdown
- [{YYYY-MM-DD}] {Feature Name}: `[{spec_file.md}]`
```

- No entries
