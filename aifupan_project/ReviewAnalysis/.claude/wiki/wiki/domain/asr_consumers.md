# ASR 消费方映射 (Consumer Map)

> 本文档枚举所有调用 `AsrUtils.AsrByDirectoryPath` 的业务方，及其各自音频特征与改动风险。
>
> 关联权威文档: [asr_engine_pipeline.md](./asr_engine_pipeline.md)
> 关联约束: [必须用短视频样本 A/B 验证](../preferences/wal/20260526_asr_short_video_validation_rules.md)

---

## 1. 三大消费方一览

| # | 业务方 | 调用点 | engSerViceType 来源 | 典型音频特征 |
|---|---|---|---|---|
| 1 | **主播视频回放分析** | `Bll/AnchorVideoBll.cs:602` | `anchorInfo.engSerViceType`（DB，每主播一份） | 直播解说 + 游戏/带货/生活 |
| 2 | **用户上传文件分析** | `Bll/UploadFileBll.cs:206` | `uploadFile.engSerViceType`（DB） | 用户任意上传，分布最广 |
| 3 | **短视频采集** | `ShortVideo/ShortVideoHandle.cs:566` | **硬编码 `"svs:auto:withitn\|tencent"`** | 抖音类内容：高 BGM + AI 配音 + 字幕同步语音 |

## 2. 共享基础设施

所有消费方走同一管道：

```
{各调用点}
    ↓
AudioUtils.SlicingAudio        ← FFmpeg 滤镜链 + 59s 切片（共享）
    ↓
AsrUtils.AsrByDirectoryPath    ← 入口 + 结果组装（共享）
    ↓
AsrEngineFactory.Build         ← Composite 或单引擎（共享）
    ↓
SenseVoiceSmallEngine / TencentASREngine    ← 引擎池单例（共享）
```

**结论**：任何对 `AudioUtils.SlicingAudio` 或 `SenseVoiceSmallEngine` 的改动**同时影响 3 个业务方**。

## 3. 各消费方的差异化关切

### 3.1 主播视频回放
- **时长**：通常长（数十分钟至数小时）
- **风险**：CPU 开销叠加敏感（30 分钟 ffmpeg 超时上限）
- **音质**：解说员有麦克风，质量相对稳定
- **AI 含量**：低-中

### 3.2 用户上传文件
- **时长**：任意
- **风险**：最不可预测（用户素材千差万别）
- **音质**：从专业录音到电话录音都有
- **AI 含量**：低-中

### 3.3 短视频采集 ⚠️
- **时长**：通常短（< 60s 居多）
- **风险**：硬编码引擎配置，无 DB 切换逃生口；切错只能改代码
- **音质**：平台压缩 + 复杂混音
- **AI 含量**：**极高**——抖音生态大量带货/科普/外推/解说类 AI 配音
- **特殊**：**BGM 几乎是默认存在**；字幕同步语音对模型边界要求高

## 4. 改动风险矩阵

| 类型改动 | 主播 | 上传 | 短视频 | 备注 |
|---|---|---|---|---|
| 切片时长/边界 | 中 | 中 | **高** | 短视频偏短，每段占比高，丢一段就丢很多 |
| FFmpeg 滤镜链 | 中 | 中 | **高** | 短视频音频混音复杂，对滤镜更敏感 |
| 频域降噪（afftdn 类） | 低-中 | 中 | **极高** | 杀 AI 配音案例（2026-05-26） |
| GreedyDecode / FilterMetas | 中 | 中 | **高** | 短视频高 BGM → meta token 多 |
| MergeWords | 中 | 中 | 中 | 影响普遍 |
| 跨段合并 | 中 | 中 | 低-中 | 短视频段少，影响小 |
| 引擎选择策略 | 中 | 中 | **不可改（硬编码）** | 改动需先解耦短视频引擎配置 |

## 5. 历史教训

### 2026-05-26 afftdn 撤回案例
**事件**：滤镜链加入 `afftdn=nr=12` 频域降噪。
**意外**：含 AI 配音的短视频整段 AI 解说被吃掉。
**根因**：afftdn 学习"稳态频谱"为噪声，TTS/AI 频谱稳态度极高 → 被误杀。
**教训**：**短视频是 ASR 改动的关键测试场景，必须前置 A/B 验证**。详见 [validation rules](../preferences/wal/20260526_asr_short_video_validation_rules.md)。

## 6. 工程债务（已记录，暂不处理）

- ShortVideoHandle.cs:566 引擎配置硬编码 → 应迁移到配置或 DB
- 短视频路径缺少独立的优化开关 → 当前所有优化"齐头并进"，无法只对短视频/只对回放调参
