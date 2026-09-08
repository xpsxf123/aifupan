# WAL — ASR 语言归一化 + 后端调用

**日期**: 2026-05-18
**类型**: Domain WAL

## 新增概念

### AsrLanguageCode（`Asr/AsrLanguageCode.cs`）
- 19 个标准 Tencent 语言码白名单（16k_zh / 16k_en / 16k_yue / ...）
- `Normalize(code)`: 无效值归一化为 `"16k_zh"`
- `ToBackendEngine(code)`: SVS 支持语言 → `"sense-voice"`；其余 → `"tencent"`
- `ToSvsLang(code)`: Tencent 码 → SVS LidDict key（不支持的 → `"auto"`）

### SVS 支持的语言（可用 sense-voice）
`16k_zh`, `16k_zh-PY`, `16k_zh_medical`, `16k_en`, `16k_yue`, `16k_ja`, `16k_ko`

### SVS 不支持的语言（自动降级 auto，后端用 tencent）
`16k_vi`, `16k_ms`, `16k_id`, `16k_fil`, `16k_th`, `16k_pt`, `16k_tr`, `16k_ar`, `16k_es`, `16k_hi`, `16k_fr`, `16k_de`

## 状态变更

### `AsrUtils.AsrByDirectoryPath` 默认值
- 旧: `"svs:auto:withitn|tencent"`（混合 DSL）
- 新: `"16k_zh"`（纯语言码）

### 调用流程
1. `Normalize(engSerViceType)`
2. POST `/audio/asr-engine` `{ engine, language, directoryPath }`
3. 成功且 data 非空 → 返回后端结果
4. 任何失败 → 兜底本地 Composite(SVS + Tencent)

## 反模式（新增）
- 禁止向 `AsrUtils.AsrByDirectoryPath` 传入旧 DSL 格式（如 `"svs:zh:withitn|tencent"`），只传标准语言码
- 禁止绕过 `AsrLanguageCode.Normalize` 直接构造 `AsrEngineFactory.Build` 参数
