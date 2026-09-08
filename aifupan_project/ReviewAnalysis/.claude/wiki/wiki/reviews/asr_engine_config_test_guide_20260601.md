# ASR 引擎配置测试指南（交付测试团队）

**日期**：2026-06-01
**适用范围**：ReviewAnalysis 客户端 ASR 模块 + back-fupan-server `asr_engine_config` 表配置
**面向**：测试团队
**作者**：开发组

---

## 0. 一句话定位

测试团队 **只通过修改 `asr_engine_config` 表的数据** 就能控制每个用户 / 租户 / 语种用哪个 ASR 引擎、按什么顺序走，并且决定**质量降级（FALLBACK）兜底机制是否生效**。

本文回答四件事：

1. 表里怎么配？4 档优先级是什么？哨兵值是什么？
2. ⚠️ 如果配置里**没有 `tencent`**，会发生什么？（答：兜底机制完全失效）
3. 怎么看客户端日志确认实际走了什么引擎、一段视频里多少 chunk 走了 Tencent？
4. Tencent 支持哪些语言、SenseVoice（SVS）支持哪些、不被 SVS 支持的怎么处理、表里没配怎么办？

---

## 1. 整体链路

```
客户端                                 服务端
─────────────────────────             ──────────────────────────
ReviewAnalysis                        replay-api / replay-third
─────────────────────────             ──────────────────────────
AsrUtils.AsrByDirectoryPath  ─POST→   /replay/audio/asr-engine
                                          ↓
                                      AudioDiscernController.asrEngine
                                          ↓
                                      AsrEngineConfigServiceImpl
                                        .resolveEngines(tenantId, userId, language)
                                          ↓
                                      asr_engine_config 表（4 档优先级）
                                          ↓
                              ←─返回 List<String>，如 ["sense-voice", "tencent"]
AsrEngineFactory.Build(language, engines)
        ↓
CompositeASREngine（按列表顺序串联引擎）
        ↓
对每个 audio chunk（59s 硬切）：
  1) 首选引擎识别
  2) 若返回 code != 0 → 降级到下一个引擎
  3) 若返回 code = 0 且 IsLowQuality(svsResult) → 触发 FALLBACK 找链中 Tencent 复识别
```

**关键事实**：客户端**完全信任**服务端返回的引擎列表顺序。表里写什么，客户端就按什么顺序串联。

---

## 2. `asr_engine_config` 表配置规则

### 2.1 表关键字段

| 字段 | 含义 |
|---|---|
| `tenant_id` | 租户 ID |
| `user_id` | 用户 ID。**`0` = 租户级默认配置**（哨兵值） |
| `language` | 语种码（如 `16k_yue`）。**空串 `""` = 不限语言**（哨兵值） |
| `engines` | 逗号分隔的引擎标识，按出现顺序作为优先级。例如 `sense-voice,tencent` |
| `is_deleted` | `0` 才生效 |

### 2.2 ⚙ 4 档优先级（服务端 `resolveEngines` SSOT）

服务端在表里命中**最高优先级**的一条记录返回：

| 档位 | user_id | language | 语义 |
|---|---|---|---|
| **P1**（最高） | 当前用户 ID | 精确匹配请求 language | 该用户在该语种的专属配置 |
| **P2** | 当前用户 ID | `""`（空串） | 该用户的兜底（不限语言） |
| **P3** | `0` | 精确匹配请求 language | 租户级该语种专属 |
| **P4**（最低） | `0` | `""` | 租户级兜底 |

> 找到 P1 就不看 P2，找到 P2 就不看 P3，以此类推。

### 2.3 表里没有任何匹配行 → 默认行为

服务端的 hardcoded 兜底（`AudioDiscernController.asrEngine` line 124-128）：

| 请求 language | 返回 engines |
|---|---|
| `16k_zh`（中文通用） | `["sense-voice", "tencent"]` |
| `16k_zh-PY`（中英粤） | `["sense-voice", "tencent"]` |
| **其他任何语种** | `["tencent"]` ← **注意只有 Tencent，没有 SVS** |

### 2.4 未登录用户

`localUser == null` → 直接返回 `["sense-voice"]`（**只有 SVS，没有 Tencent**）。⚠️ 这种情况下 FALLBACK 也失效，但通常测试场景不会未登录。

### 2.5 `engines` 字段的解析规则

服务端 `parseEngines`：
- 按 `,` 切分
- `trim()` 去空白
- 保留**首次出现顺序**
- 去重
- 例：`"sense-voice, tencent, sense-voice"` → `["sense-voice", "tencent"]`
- 例：`""` 或空白 → 走 2.3 默认行为

支持的引擎标识只有两个：
- `sense-voice` → 客户端本地 SenseVoiceSmallEngine（ONNX 模型）
- `tencent` → 腾讯云 ASR
- 未知字符串（如 `xunfei`）→ 客户端日志 `[ASR] 未知引擎标识 "xunfei", 已跳过`，跳过；若全部不识别则回退默认路由

---

## 3. ⚠️ 关键警告：配置不含 `tencent` → 打分兜底机制完全失效

### 3.1 什么是"打分兜底（FALLBACK）"

客户端在每个 chunk 识别成功后，会对 SVS 结果**打质量分**，分两条规则（基于事实 token 统计）：

| 规则 | 触发条件 |
|---|---|
| **rule1 collapsed_ratio** | CTC 折叠率 `CollapsedTokens / DecodedTokens > 0.85` → 唱歌段特征 |
| **rule2 char_density** | 字符密度 `Result.Length / (AudioDuration秒) < 0.5` → 低质段特征 |

任一规则命中即视为低质，自动走 **Tencent 复识别** 替换 SVS 结果。这就是为什么口播主播看不到 FALLBACK 干扰、而唱歌主播被 Tencent 救回的关键机制。

### 3.2 ⚠️ 如果 `engines` 配置里没有 `tencent`

客户端 `CompositeASREngine.TryTencentFallbackAsync` 在链中找不到 TencentASREngine 时：

```text
[Composite-FALLBACK] chunk=audio_023.wav rule=no_tencent_engine action=KEPT_SVS
```

→ 即使 SVS 打分判定低质，**也只能保留低质 SVS 结果**，无法复识别。

### 3.3 哪些配置会让 FALLBACK 失效？

| 配置 | 是否能 FALLBACK | 备注 |
|---|---|---|
| `["sense-voice", "tencent"]` | ✅ 能 | 推荐配置 |
| `["tencent"]` | ✅ 能（首选就 Tencent，无需 fallback） | 但慢、贵 |
| `["sense-voice"]` | ❌ **不能** | ⚠️ 低质 chunk 会被吃掉，无救援 |
| `[]`（空） / 表里没配 + 非中文 | 走 2.3 默认 → `["tencent"]` | 能识别但单引擎不容错 |

### 3.4 测试动作：FALLBACK 用例必须确保配置含 `tencent`

如果测试目标是验证 FALLBACK 的触发率（如粤语场景的 chunk-level fallback），**必须**在 `asr_engine_config` 里给目标 user/tenant 配 `engines = "sense-voice,tencent"`。

---

## 4. Tencent 支持的语言（共 19 个语种 + 多方言）

| 语言码 | 说明 |
|---|---|
| `16k_zh` | 中文通用 |
| `16k_zh-PY` | 中英粤 |
| `16k_zh_medical` | 中文医疗 |
| `16k_en` | 英语 |
| `16k_yue` | 粤语 |
| `16k_ja` | 日语 |
| `16k_ko` | 韩语 |
| `16k_vi` | 越南语 |
| `16k_ms` | 马来语 |
| `16k_id` | 印度尼西亚语 |
| `16k_fil` | 菲律宾语 |
| `16k_th` | 泰语 |
| `16k_pt` | 葡萄牙语 |
| `16k_tr` | 土耳其语 |
| `16k_ar` | 阿拉伯语 |
| `16k_es` | 西班牙语 |
| `16k_hi` | 印地语 |
| `16k_fr` | 法语 |
| `16k_de` | 德语 |
| `16k_zh_dialect` | 多方言（23 种：上海、四川、武汉、贵阳、昆明、西安、郑州、太原、兰州、银川、西宁、南京、合肥、南昌、长沙、苏州、杭州、济南、天津、石家庄、黑龙江、吉林、辽宁） |

客户端归一化逻辑：以上 20 个语言码（含多方言）都是合法的；其他任意字符串自动归一化为 `16k_zh`。

---

## 5. SenseVoice（SVS）支持的语言（共 7 个）

| 语言码 | 说明 | SVS 内部 `lid` |
|---|---|---|
| `16k_zh` | 中文通用 | `zh` (3) |
| `16k_zh-PY` | 中英粤 | `zh` (3) |
| `16k_zh_medical` | 中文医疗 | `zh` (3) |
| `16k_en` | 英语 | `en` (4) |
| `16k_yue` | 粤语 | `yue` (7) |
| `16k_ja` | 日语 | `ja` (11) |
| `16k_ko` | 韩语 | `ko` (12) |

> SSOT：客户端 `Asr/AsrLanguageCode.cs` `SvsSupportedCodes` HashSet。

## 6. 不被 SVS 支持的语言（其他 13 个）怎么处理？

包括但不限于：越南语、马来语、印尼语、菲律宾语、泰语、葡萄牙语、土耳其语、阿拉伯语、西班牙语、印地语、法语、德语、`16k_zh_dialect`。

客户端 `AsrEngineFactory.BuildDefaultEntries` 的**默认路由**（表里没配且不进 2.3 中文兜底时）：

```
非 SVS 支持语言 → 引擎链 = [Tencent]（只走 Tencent，没有 fallback 机制）
```

**这是合理设计**：SVS 物理上不支持，无法本地识别；只能走 Tencent。但代价是单引擎依赖，Tencent 偶发失败就只能拿到错误码。

**如果在 `asr_engine_config` 表里手动给非 SVS 语言配 `sense-voice`** → 客户端 `AsrLanguageCode.ToSvsLang` 会把语言降级为 `"auto"` 传给 SVS 模型，SVS 自动检测语种识别（识别质量不保证）。**不推荐这么配**。

---

## 7. 怎么看日志确认实际走了什么引擎

ReviewAnalysis 客户端把所有 ASR 日志写到 `analysis.log`（统一通过 `FileUtils.LogAnalysis`）。

### 7.1 引擎管道构建（每个视频开始时打 1 次）

```text
[ASR] 构建引擎管道: [SenseVoiceSmallEngine→TencentASREngine], language="16k_yue"
```

或表配置了 `["tencent"]` 时：

```text
[ASR] 构建引擎管道: [TencentASREngine], language="16k_yue"
```

→ 直接看出**当前 user/tenant 在该语种下表配置返回了什么**。

### 7.2 每个 chunk 走向（每个 chunk 打 1-3 次）

```text
[ASR] Composite引擎启动, 链: SenseVoiceSmallEngine → TencentASREngine
[ASR] 引擎 SenseVoiceSmallEngine 识别成功
```

如 SVS 引擎失败降级：

```text
[ASR] 引擎降级: SenseVoiceSmallEngine 返回 code=500, 尝试下一个引擎
[ASR] 引擎 TencentASREngine 识别成功
```

### 7.3 FALLBACK 触发日志（最关键，看 chunk 级是否走 Tencent）

每次 SVS 打分判定低质都会打一行：

```text
[Composite-FALLBACK] chunk=audio_023.wav rule=collapsed_ratio ratio=0.992 tencent_code=0 action=REPLACED
```

字段含义：

| 字段 | 含义 |
|---|---|
| `chunk` | 当前 chunk 文件名（视频被 59s 硬切后的文件） |
| `rule` | 触发的判定规则：`collapsed_ratio` / `char_density` |
| `ratio` | 实际的 CTC 折叠率（仅 `collapsed_ratio` 触发时有意义） |
| `tencent_code` | Tencent ASR 返回码：`0`=成功，其他=失败 |
| `action` | `REPLACED`=Tencent 接管，`KEPT_SVS`=保留 SVS（Tencent 也失败或链中没 Tencent） |

**特殊 action**：

| log | 含义 |
|---|---|
| `rule=no_tencent_engine action=KEPT_SVS` | ⚠️ 配置没有 Tencent，FALLBACK 失效 |
| `rule=... tencent_code=-1 action=KEPT_SVS ex=...` | Tencent 调用异常抛错 |

### 7.4 视频级汇总（每个视频结束打 1 次）

```text
语音识别完成一共: 55段, FALLBACK: 12段 (21.8%)
```

→ 一眼看出这个视频**多少 chunk 走了 Tencent**。

### 7.5 命令行快速过滤建议

```bash
# 看本次视频构建了什么引擎链
grep "构建引擎管道" analysis.log

# 看每个 chunk 的 FALLBACK 动作
grep "Composite-FALLBACK" analysis.log

# 看视频级 FALLBACK 比例
grep "语音识别完成" analysis.log

# 找出 FALLBACK 失效的 chunk
grep "rule=no_tencent_engine" analysis.log
```

---

## 8. 推荐测试场景矩阵

| # | 场景 | 配置（user/tenant + lang + engines） | 预期日志 |
|---|---|---|---|
| T1 | 默认中文 | 表里无配 + `language=16k_zh` | 构建 `[SVS→Tencent]`；FALLBACK 触发率随 chunk 类型变化 |
| T2 | 默认粤语 | 表里无配 + `language=16k_yue` | 构建 `[Tencent]` 单引擎（走 2.3 默认） |
| T3 | 配置粤语双引擎 | engines=`sense-voice,tencent` lang=`16k_yue` | 构建 `[SVS→Tencent]`，唱歌段大量 FALLBACK |
| T4 | 配置只 SVS | engines=`sense-voice` lang=`16k_zh` | ⚠️ 构建 `[SVS]`；唱歌段日志 `rule=no_tencent_engine action=KEPT_SVS` |
| T5 | 配置只 Tencent | engines=`tencent` lang=`16k_zh` | 构建 `[Tencent]`，无 FALLBACK 行（不需要） |
| T6 | 用户级覆盖租户级 | user_id=具体ID 配 `tencent`；user_id=0 配 `sense-voice,tencent` | P1 优先 → 构建 `[Tencent]` |
| T7 | 不限语言兜底 | user_id=ID lang=`""` engines=`sense-voice,tencent` | 任意语言都走 `[SVS→Tencent]`（若 SVS 不支持则降为 auto） |
| T8 | 错误引擎标识 | engines=`xunfei,tencent` | 日志 `[ASR] 未知引擎标识 "xunfei", 已跳过`，最终 `[Tencent]` |
| T9 | 不被 SVS 支持的语种 | lang=`16k_th` engines=`sense-voice,tencent` | SVS 走 auto；建议复核结果质量 |
| T10 | 多方言 | lang=`16k_zh_dialect` engines 任意 | SVS 不支持，强制只走 Tencent 才合理 |

---

## 9. 配置变更后的快速验证流程

1. 在 `asr_engine_config` 表里改完目标用户的 engines
2. 客户端**不需要重启**（每个视频开始时都重新请求 `/replay/audio/asr-engine`）
3. 触发一次新视频识别
4. 在 `analysis.log` 里找：
   - `[ASR] 构建引擎管道: [...], language="..."` → 确认链构建正确
   - `语音识别完成一共: X段, FALLBACK: Y段 (Z%)` → 确认 FALLBACK 比例符合预期
   - 如配置含 `sense-voice` 但不含 `tencent`，确认有 `rule=no_tencent_engine` 行（这是预期）

---

## 10. 联系开发

- 客户端 SSOT：`Asr/CompositeASREngine.cs`（FALLBACK 决策）、`Asr/AsrEngineFactory.cs`（引擎链构建）、`Asr/AsrLanguageCode.cs`（语言归一化与 SVS 支持表）
- 服务端 SSOT：`AudioDiscernController.asrEngine`（POST 入口）、`AsrEngineConfigServiceImpl.resolveEngines`（4 档优先级）
- 阈值常量：`CompositeASREngine.CtcCollapseThreshold = 0.85` / `CharDensityThreshold = 0.5`（2026-06-01 版本，会随 smoke 数据继续调整）
- 异常情况（如 Tencent 凭证失败、SVS 模型未就绪）请把 `analysis.log` 对应时段截取交开发组排查
