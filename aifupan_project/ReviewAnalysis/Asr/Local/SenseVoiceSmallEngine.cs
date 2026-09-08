using douyin.Utils;
using Microsoft.ML.OnnxRuntime;
using Microsoft.ML.OnnxRuntime.Tensors;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr.Local
{
    /// <summary>
    /// SenseVoiceSmall local ONNX ASR engine.
    /// Complete pipeline: MP3 → PCM → FBank+LFR → ONNX inference → post-process → ASRResultEntity.
    /// </summary>
    public class SenseVoiceSmallEngine : IASREngine
    {
        private readonly ModelManager _modelManager;
        private readonly WavFrontend _wavFrontend;
        private GpuProbe _gpuProbe;
        private OfflineModel _offlineModel;
        private string[] _vocab;
        private HashSet<int> _metaTokenIds; // auto-scanned at vocab load
        private readonly object _initLock = new object();
        private readonly SemaphoreSlim _inferenceLock = new SemaphoreSlim(1, 1);
        private bool _loggedOutputNames = false; // 诊断：一次性打印 ONNX 输出名以确认 cif_peak 的真实名字
        private static readonly Dictionary<string, int> LidDict = new Dictionary<string, int>
        {
            { "auto", 0 }, { "zh", 3 }, { "en", 4 }, { "yue", 7 },
            { "ja", 11 }, { "ko", 12 }, { "nospeech", 13 }
        };

        private static readonly Dictionary<string, int> TextnormDict = new Dictionary<string, int>
        {
            { "withitn", 14 }, { "woitn", 15 }
        };

        // Special token sets for post-processing
        private static readonly HashSet<int> LanguageTokenIds = new HashSet<int>
            { 24884, 24885, 24888, 24892, 24896, 24992 };
        private static readonly HashSet<int> TextnormTokenIds = new HashSet<int>
            { 25016, 25017 };
        private static readonly HashSet<string> PunctuationSet = new HashSet<string>
            { "。", "？", "！", "，", "、", ".", "?", "!", "," };

        // CJK 分组参数 — 我们不做 NLP 分词，按语速 + 硬上限组合：
        //   ① 一个 Word 最多 3 个 CJK 字（避免长串高亮难追读）
        //   ② 当前字 duration（即与下一字之间的间隔）> 300ms 视为自然停顿，flush
        //   覆盖语速范围：慢速 333ms/字、正常 200ms/字、快速 143ms/字 — 300ms 阈值刚好在
        //   "正常字距" 与 "词组/呼吸停顿" 之间，不切碎主播慢速也不把短静音合进词组。
        private const int MaxCharsPerCjkWord = 3;
        private const int CjkBreakThresholdMs = 300;

        public SenseVoiceSmallEngine()
        {
            _modelManager = new ModelManager();
            _wavFrontend = new WavFrontend();
            _gpuProbe = new GpuProbe();
        }

        /// <summary>
        /// Main entry point. Takes an MP3 file and returns ASR result
        /// in the same format as Tencent Cloud: Dictionary{code, data: List&lt;ASRResultEntity&gt;}.
        /// </summary>
        public async Task<Dictionary<string, object>> RecognizeAsync(FileInfo audioFile, string engSerViceType)
        {
            var swTotal = Stopwatch.StartNew();
            FileUtils.LogAnalysis($"[SVS] 开始识别: {audioFile.Name}, 配置={engSerViceType}");

            // 1. Check model readiness, wait for download on first run
            if (!_modelManager.IsModelReady())
            {
                FileUtils.LogAnalysis("[SVS] 模型未就绪，开始下载...");
                var downloadTask = _modelManager.EnsureDownloadStartedAsync();
                var timeout = Task.Delay(TimeSpan.FromMinutes(10));
                var completed = await Task.WhenAny(downloadTask, timeout);
                if (completed == timeout || !_modelManager.IsModelReady())
                {
                    FileUtils.LogAnalysis("[SVS] 模型下载超时或失败");
                    return CreateErrorResult(702, "模型文件下载失败，请检查网络后重试");
                }
                FileUtils.LogAnalysis("[SVS] 模型下载完成，继续推理");
            }

            // 2. Decode MP3 to PCM (done once, reused for duration + features)
            float[] pcm;
            try
            {
                var swDecode = Stopwatch.StartNew();
                pcm = await Task.Run(() => WavFrontend.DecodeMp3(audioFile.FullName));
                FileUtils.LogAnalysis($"[SVS] MP3解码完成, PCM={pcm.Length}采样, 耗时={swDecode.ElapsedMilliseconds}ms");
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[SVS] MP3解码失败: {ex.Message}");
                return CreateErrorResult(703, "音频解码失败: " + ex.Message);
            }

            // 3. Check duration ≤ 60s
            double durationSec = (double)pcm.Length / 16000;
            FileUtils.LogAnalysis($"[SVS] 音频时长={durationSec:F1}s");
            if (durationSec > 60)
            {
                FileUtils.LogAnalysis($"[SVS] 音频超60秒限制, 时长={durationSec:F1}s");
                return CreateErrorResult(704, "音频时长超过60秒限制");
            }
            if (pcm.Length < 400)
            {
                FileUtils.LogAnalysis("[SVS] 音频过短, 无法提取特征");
                return CreateErrorResult(703, "音频过短，无法提取特征");
            }

            // 4. Extract FBank features + LFR (reuse decoded PCM)
            float[] features;
            try
            {
                var swFeat = Stopwatch.StartNew();
                float[] fbank = _wavFrontend.GetFbank(pcm);
                features = _wavFrontend.ApplyLfr(fbank);
                FileUtils.LogAnalysis($"[SVS] 特征提取完成, frames={features.Length / 560}, 耗时={swFeat.ElapsedMilliseconds}ms");
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[SVS] 特征提取失败: {ex.Message}");
                return CreateErrorResult(703, "音频特征提取失败: " + ex.Message);
            }

            // 5. Parse config
            var config = ParseConfig(engSerViceType);
            int languageId = config.languageId;
            int textnormId = config.textnormId;

            // 6. Ensure model is loaded
            try
            {
                var swLoad = Stopwatch.StartNew();
                EnsureModelLoaded();
                if (swLoad.ElapsedMilliseconds > 0)
                    FileUtils.LogAnalysis($"[SVS] 模型首次加载, 耗时={swLoad.ElapsedMilliseconds}ms");
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[SVS] 模型加载失败: {ex.Message}");
                return CreateErrorResult(500, "模型加载失败: " + ex.Message);
            }

            // 7. Run ONNX inference
            //    GPU 失败 → 进程级禁用 GPU + 重建 CPU 模型 + 重试一次
            //    CPU 失败 → 不重试（同输入同模型重试结果不变）
            ModelOutput output = null;
            Exception lastEx = null;
            var swInfer = Stopwatch.StartNew();
            await _inferenceLock.WaitAsync();
            try
            {
                try
                {
                    output = RunInference(features, languageId, textnormId);
                }
                catch (Exception ex)
                {
                    var failedProvider = _offlineModel.ActiveProvider;
                    FileUtils.LogAnalysis($"[SVS] ONNX 推理失败 (EP={failedProvider}): {ex.Message}");
                    lastEx = ex;

                    if (failedProvider != GpuProbe.Provider.Cpu)
                    {
                        GpuRuntimeState.DisableGpuForProcess($"SVS RunInference 失败: {ex.Message}");
                        try
                        {
                            RebuildOfflineModelForcedCpu();
                            output = RunInference(features, languageId, textnormId);
                            lastEx = null;
                            FileUtils.LogAnalysis("[SVS] CPU 兜底重试成功");
                        }
                        catch (Exception ex2)
                        {
                            FileUtils.LogAnalysis($"[SVS] CPU 兜底重试仍失败: {ex2.Message}");
                            lastEx = ex2;
                        }
                    }
                }
            }
            finally
            {
                _inferenceLock.Release();
            }
            if (lastEx != null)
            {
                FileUtils.LogAnalysis($"[SVS] ONNX 推理最终失败: {lastEx.Message}");
                return CreateErrorResult(500, "ONNX推理失败: " + lastEx.Message);
            }
            FileUtils.LogAnalysis($"[SVS] ONNX推理完成, EP={_offlineModel.ActiveProvider}, 耗时={swInfer.ElapsedMilliseconds}ms");

            // 8. Ensure vocab is loaded
            try
            {
                EnsureVocabLoaded();
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[SVS] 词汇表加载失败: {ex.Message}");
                return CreateErrorResult(702, "词汇表加载失败: " + ex.Message);
            }

            // 9. Greedy decode
            try
            {
                int[] tokenIds = GreedyDecode(output.LogitsCopy, output.VocabSize, output.SequenceLength, languageId);

                // 10. Filter blanks + meta tokens (language/textnorm/emotion/event) in one pass,
                //    while recording each surviving content token's original frame index in the
                //    decoder output sequence. In CTC-style decoding the frame index IS the time
                //    position (frame t → t × frame_stride_ms), used as a fallback when the ONNX
                //    model does not output cif_peak.
                var filtered = FilterBlanksAndMetas(tokenIds);
                int[] contentTokens = filtered.Tokens;
                int[] frameIndices  = filtered.FrameIndices;

                int chunkDurationMs = (int)(durationSec * 1000);
                int[][] timestamps = ComputeTimestamps(output.CifPeak, frameIndices, chunkDurationMs);

                // 11. Word merge + result text 单遍合并扫描
                var merged = MergeWords(contentTokens, timestamps);
                List<ASRWordEntity> wordList = merged.words;
                string resultText = merged.resultText;

                // 诊断日志：管道各阶段计数，便于定位"哪一层在丢内容"。
                // 解读：content << decoded → meta/blank 占多数（BGM/纯音效段）；
                //       words << content → MergeWords 标点附着或时间戳无效丢弃；
                //       last_end << chunk → 末尾 N 秒未产 content token（典型 BGM 段）。
                int lastWordEndMs = wordList.Count > 0 ? (int)wordList[wordList.Count - 1].EndTime : 0;
                int cifPeakLen = output.CifPeak?.Length ?? 0;
                FileUtils.LogAnalysis($"[SVS] 诊断 {audioFile.Name}: decoded={tokenIds.Length} content={contentTokens.Length} words={wordList.Count} last_end={lastWordEndMs}ms chunk={chunkDurationMs}ms cif_peak={cifPeakLen}");

                // 日志预览：长文本截断为前 30 字 + 省略号，避免每段刷屏；完整文本由下游消费方持久化。
                string textForLog = resultText ?? string.Empty;
                string preview = textForLog.Length <= 30 ? textForLog : textForLog.Substring(0, 30) + "…";
                FileUtils.LogAnalysis($"[SVS] 识别完成: \"{preview}\" len={textForLog.Length}, words={wordList.Count}, 总耗时={swTotal.ElapsedMilliseconds}ms");

                var entity = new ASRResultEntity
                {
                    FileName = audioFile.Name,
                    Result = resultText,
                    WordList = wordList,
                    WordSize = wordList.Count.ToString(),
                    AudioDuration = (long)(durationSec * 1000),
                    Code = 0,
                    Paragraph = 0,
                    // SVS 诊断指标（route A）— 仅赋值不判定，判定 SSOT 在 CompositeASREngine
                    DecodedTokens = tokenIds.Length,
                    ContentTokens = contentTokens.Length,
                    CollapsedTokens = tokenIds.Length - contentTokens.Length
                };

                return new Dictionary<string, object>
                {
                    { "code", 0 },
                    { "data", new List<ASRResultEntity> { entity } }
                };
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[SVS] 后处理异常: {ex.Message}");
                return CreateErrorResult(500, "后处理失败: " + ex.Message);
            }
        }

        /// <summary>
        /// POC test method — run from console or test harness.
        /// </summary>
        public static async Task TestAsync(string mp3Path)
        {
            var engine = new SenseVoiceSmallEngine();
            var result = await engine.RecognizeAsync(new FileInfo(mp3Path), "16k_zh");
            int code = (int)result["code"];
            if (code == 0)
            {
                var data = (List<ASRResultEntity>)result["data"];
                Console.WriteLine("Recognition successful:");
                Console.WriteLine("  Text: " + data[0].Result);
                Console.WriteLine("  Words: " + data[0].WordList.Count);
                Console.WriteLine("  Duration: " + data[0].AudioDuration + "ms");
            }
            else
            {
                var data = (List<ASRResultEntity>)result["data"];
                string msg = (data != null && data.Count > 0) ? data[0].Msg : "unknown";
                Console.WriteLine("Recognition failed: code=" + code + ", msg=" + msg);
            }
        }

        #region Private

        private void EnsureModelLoaded()
        {
            if (_offlineModel != null)
                return;

            lock (_initLock)
            {
                if (_offlineModel != null)
                    return;
                _offlineModel = new OfflineModel(_modelManager.GetModelPath(), _gpuProbe);
            }
        }

        /// <summary>
        /// GPU 推理失败后调用：用新建的 GpuProbe（此时 GpuRuntimeState.GpuDisabled=true → BestProvider=Cpu）
        /// 重建 _offlineModel，再 dispose 旧实例。先建后销，避免读者看到 null。
        /// 调用方必须已持有 _inferenceLock，否则与并发推理读 _offlineModel 有竞态。
        /// </summary>
        private void RebuildOfflineModelForcedCpu()
        {
            lock (_initLock)
            {
                var old = _offlineModel;
                _gpuProbe = new GpuProbe();
                _offlineModel = new OfflineModel(_modelManager.GetModelPath(), _gpuProbe);
                old?.Dispose();
            }
        }

        private void EnsureVocabLoaded()
        {
            if (_vocab != null)
                return;

            lock (_initLock)
            {
                if (_vocab != null)
                    return;

                string tokensPath = _modelManager.GetTokensPath();
                string json = File.ReadAllText(tokensPath);

                // Try array format: ["<blank>", "<s>", ...]
                try
                {
                    var arr = JsonConvert.DeserializeObject<string[]>(json);
                    if (arr != null && arr.Length > 0)
                        _vocab = arr;
                }
                catch (JsonException)
                {
                    // Not a valid array, try dict format below
                }

                if (_vocab == null)
                {
                    try
                    {
                        var dict = JsonConvert.DeserializeObject<Dictionary<string, int>>(json);
                        if (dict != null && dict.Count > 0)
                        {
                            int maxIdx = dict.Values.Max();
                            _vocab = new string[maxIdx + 1];
                            foreach (var kv in dict)
                                _vocab[kv.Value] = kv.Key;
                        }
                    }
                    catch (JsonException)
                    {
                        // Not a valid dict either; try plain-text format (one token per line, index = token ID)
                        var lines = json.Split(new[] { '\r', '\n' }, StringSplitOptions.None);
                        var tokens = new List<string>();
                        foreach (var line in lines)
                        {
                            // support optional "id token" or "token id" columns; fall back to whole line as token
                            var parts = line.Split(new[] { ' ', '\t' }, 2, StringSplitOptions.RemoveEmptyEntries);
                            if (parts.Length == 0) { tokens.Add(""); continue; }
                            // plain token-per-line
                            tokens.Add(parts[0]);
                        }
                        // trim trailing empty entries
                        int last = tokens.Count - 1;
                        while (last >= 0 && string.IsNullOrEmpty(tokens[last])) last--;
                        if (last >= 0)
                            _vocab = tokens.GetRange(0, last + 1).ToArray();
                    }

                    if (_vocab == null)
                        throw new InvalidDataException("无法解析tokens.json，格式不支持");
                }

                // Auto-collect every meta token ("<|...|>") so emotion/event/lid/textnorm
                // tokens are filtered uniformly. Falls back to the hard-coded sets above
                // if the vocab is missing or partially loaded.
                // 用字符判断替代 Regex：vocab ~25k 词条，Regex.IsMatch 慢 ~10x。
                var metaIds = new HashSet<int>();
                for (int i = 0; i < _vocab.Length; i++)
                {
                    string t = _vocab[i];
                    if (!string.IsNullOrEmpty(t) && t.Length >= 4
                        && t[0] == '<' && t[1] == '|'
                        && t[t.Length - 2] == '|' && t[t.Length - 1] == '>')
                    {
                        metaIds.Add(i);
                    }
                }
                _metaTokenIds = metaIds;
            }
        }

        private (int languageId, int textnormId) ParseConfig(string engSerViceType)
        {
            // Accept normalized Tencent language codes (e.g. "16k_zh", "16k_en").
            // Languages not natively supported by SVS fall back to "auto".
            string svsLang = AsrLanguageCode.ToSvsLang(
                AsrLanguageCode.Normalize(engSerViceType));
            int langId = LidDict.TryGetValue(svsLang, out int id) ? id : 0;
            return (langId, 14); // 14 = withitn (inverse text normalization on)
        }

        private ModelOutput RunInference(float[] features, int languageId, int textnormId)
        {
            int nFrames = features.Length / 560;
            int batchSize = 1;

            // Build input tensors
            var speechTensor = new DenseTensor<float>(features, new int[] { batchSize, nFrames, 560 }, false);
            var lengthsTensor = new DenseTensor<int>(new int[] { nFrames }, new int[] { batchSize }, false);
            var langTensor = new DenseTensor<int>(new int[] { languageId }, new int[] { batchSize }, false);
            var normTensor = new DenseTensor<int>(new int[] { textnormId }, new int[] { batchSize }, false);

            var inputs = new List<NamedOnnxValue>
            {
                NamedOnnxValue.CreateFromTensor("speech", speechTensor),
                NamedOnnxValue.CreateFromTensor("speech_lengths", lengthsTensor),
                NamedOnnxValue.CreateFromTensor("language", langTensor),
                NamedOnnxValue.CreateFromTensor("textnorm", normTensor)
            };

            // IMPORTANT: Do NOT wrap Run() in a using block here —
            // the returned tensor references native OrtValue memory.
            // Dispose results after GreedyDecode consumes the data.
            var results = _offlineModel.ModelSession.Run(inputs);
            try
            {
                var resultsArray = results.ToArray();
                // 诊断（一次性）：打印 ONNX 模型实际输出张量名与位置，用于排查 cif_peak 为空的根因。
                // 当前代码按 name=="cif_peak" 查找；若模型输出名不同（如 "predictor_peak"），需修改读取策略。
                if (!_loggedOutputNames)
                {
                    var names = string.Join(", ", resultsArray.Select((r, idx) => $"[{idx}]{r.Name}"));
                    FileUtils.LogAnalysis($"[SVS] ONNX 输出张量: {names}");
                    _loggedOutputNames = true;
                }
                var logits = resultsArray[0].AsTensor<float>();
                int seqLen = logits.Dimensions[1]; // model outputs: logits + cif_peak (no separate seqlen output)
                int vocabSize = logits.Dimensions[2];
                int timeSteps = logits.Dimensions[1];

                // 优先走 DenseTensor.Buffer.Span 批量拷贝（O(N) memcpy），失败兜底单元素遍历
                float[] logitsCopy = new float[timeSteps * vocabSize];
                if (logits is DenseTensor<float> denseLogits)
                {
                    denseLogits.Buffer.Span.CopyTo(logitsCopy);
                }
                else
                {
                    int idx = 0;
                    for (int t = 0; t < timeSteps; t++)
                        for (int v = 0; v < vocabSize; v++)
                            logitsCopy[idx++] = logits[0, t, v];
                }

                float[] cifPeak = null;
                var cifResult = results.FirstOrDefault(r => r.Name == "cif_peak");
                if (cifResult != null)
                {
                    var cifTensor = cifResult.AsTensor<float>();
                    cifPeak = cifTensor.ToArray();
                }

                return new ModelOutput
                {
                    LogitsCopy = logitsCopy,
                    VocabSize = vocabSize,
                    SequenceLength = seqLen,
                    CifPeak = cifPeak
                };
            }
            finally
            {
                results.Dispose();
            }
        }

        // 低置信度阈值：argmax token 的 softmax 概率 < 此值则强置 blank。
        // 0.30 是保守起步值（中文/英文/日韩），覆盖明显的 BGM / 噪声幻觉
        // （这些帧 logits 分布平坦，任何单一 token 概率都 < 0.3）；同时
        // 不会误杀正常人声（清晰发音通常 > 0.5）和 AI/TTS 配音（合成信号
        // 确定性强，通常 > 0.7）。设为 0 等同禁用（回退到纯 argmax 行为）。
        //
        // 2026-06-01 引入 per-language 阈值：粤语 (langId=7) 降到 0.20。
        // 原因：粤语声调密集 / 入声 / 塞音过渡帧 logits 分布更平，0.30 会把
        // 大量正常发音帧打成 blank，造成段内 5–6s 静默。下游 chunk-level
        // FALLBACK (CompositeASREngine collapsed_ratio=0.90) 兜底 BGM 误识。
        private const float DefaultConfidenceThreshold = 0.30f;
        private static readonly Dictionary<int, float> LanguageConfidenceThresholds = new Dictionary<int, float>
        {
            { 7, 0.20f }, // yue (Cantonese)
        };
        private static float GetConfidenceThreshold(int languageId) =>
            LanguageConfidenceThresholds.TryGetValue(languageId, out float v) ? v : DefaultConfidenceThreshold;

        private int[] GreedyDecode(float[] logits, int vocabSize, int seqLen, int languageId)
        {
            float confidenceThreshold = GetConfidenceThreshold(languageId);
            if (vocabSize <= 0 || logits.Length == 0)
                return new int[0];

            int timeSteps = logits.Length / vocabSize;
            int actualSeqLen = Math.Min(seqLen, timeSteps);

            var tokenIds = new int[actualSeqLen];
            int lowConfFiltered = 0;
            for (int t = 0; t < actualSeqLen; t++)
            {
                float maxVal = float.MinValue;
                int maxIdx = 0;
                int offset = t * vocabSize;
                for (int v = 0; v < vocabSize; v++)
                {
                    float val = logits[offset + v];
                    if (val > maxVal)
                    {
                        maxVal = val;
                        maxIdx = v;
                    }
                }
                // Validate: ensure maxIdx is within vocab range (NaN logits can produce garbage)
                if (maxIdx < 0 || maxIdx >= vocabSize)
                    maxIdx = 0; // fallback to blank token, filtered later

                // 置信度门：只对非 blank token 计算 softmax（blank 反正会被滤掉，省 25k 次 exp）。
                // softmax(max) = exp(maxLogit - maxLogit) / sum_v exp(logit_v - maxLogit) = 1 / sum
                // 减 maxLogit 是数值稳定性技巧，避免 exp 溢出。
                //
                // 早退优化：prob < threshold ⟺ 1/expSum < threshold ⟺ expSum > 1/threshold。
                // expSum 各项为正、随 v 单调递增，故一旦超过 1/threshold 即可确定 prob < threshold，
                // 提前 break 省去剩余 exp。低置信度（BGM/噪声）帧 logits 平坦、expSum 增长快，
                // 正是早退命中最多、原本最费的场景；判定结果与跑满全词表数值等价（边界 ULP 级差异可忽略）。
                if (maxIdx > 2 && confidenceThreshold > 0f)
                {
                    double expSumLimit = 1.0 / confidenceThreshold;
                    double expSum = 0;
                    bool lowConf = false;
                    for (int v = 0; v < vocabSize; v++)
                    {
                        expSum += Math.Exp(logits[offset + v] - maxVal);
                        if (expSum > expSumLimit)
                        {
                            lowConf = true;
                            break;
                        }
                    }
                    if (lowConf)
                    {
                        maxIdx = 0; // 强置 blank，下游 FilterBlanksAndMetas 丢弃
                        lowConfFiltered++;
                    }
                }
                tokenIds[t] = maxIdx;
            }

            if (lowConfFiltered > 0)
                FileUtils.LogAnalysis($"[SVS] 低置信度过滤: {lowConfFiltered}/{actualSeqLen} 帧 (threshold={confidenceThreshold:F2}, langId={languageId})");

            return tokenIds;
        }

        private int[][] ComputeTimestamps(float[] cifPeak, int[] frameIndices, int chunkDurationMs)
        {
            const int frameRateMs = 60; // FrameShift(10ms) * LfrN(6) = 60ms per LFR frame
            int n = frameIndices?.Length ?? 0;

            if (n == 0)
                return new int[0][];

            // Path A: CIF-enabled model — accumulate weights and align fire points 1:1 with content.
            if (cifPeak != null && cifPeak.Length > 0)
            {
                var firePoints = new List<int>();
                float accumulated = 0;
                for (int t = 0; t < cifPeak.Length; t++)
                {
                    accumulated += cifPeak[t];
                    while (accumulated >= 1.0f)
                    {
                        firePoints.Add(t);
                        accumulated -= 1.0f;
                    }
                }

                // CIF under-fire 保护：fire < content 时，旧实现给多余 token 赋 [-1,-1]，
                // 在 MergeWords (line 640) 被静默丢弃，造成高语速/量化模型场景大段丢字。
                // 整段降级到 Path B（不混用 CIF/CTC 时间戳源，避免破坏 StartTime 单调性）。
                if (firePoints.Count >= n)
                {
                    var ts = new int[n][];
                    for (int i = 0; i < n; i++)
                    {
                        int s = firePoints[i];
                        int e = (i + 1 < firePoints.Count) ? firePoints[i + 1] : s + 1;
                        ts[i] = new int[] { s * frameRateMs, e * frameRateMs };
                    }
                    return ts;
                }

                FileUtils.LogAnalysis($"[SVS] CIF under-fire: fire={firePoints.Count}/content={n}, 降级 CTC 时间戳路径");
                // fallthrough → Path B
            }

            // Path B: model without cif_peak — CTC-style timestamps from each token's frame index
            // in the decoder output. tokens at output time-step t correspond to LFR frame t (each
            // LFR frame = FrameShift(10ms) × LfrN(6) = 60ms of source audio).
            //
            // For audio-text sync (karaoke-style highlighting) we WANT each word to "linger"
            // through any trailing silence until the next word starts — that way during pauses
            // the most recently spoken word stays highlighted. The last word extends to the
            // end of the chunk so the highlight remains until the chunk boundary.
            //
            // Both start and end are clamped to [0, chunkDurationMs]: the ONNX model may emit
            // a few LFR frames beyond the audio's true span (LFR padding artifact), and we must
            // never let WordList[i].EndTime > chunkDurationMs — downstream paragraph bucketing
            // (e.g. packageParagraphWord) treats this as an invariant.
            var timestamps = new int[n][];
            for (int i = 0; i < n; i++)
            {
                int start = frameIndices[i] * frameRateMs;
                int end = (i + 1 < n)
                    ? frameIndices[i + 1] * frameRateMs   // 自然连接：长字 / 静音由前一字覆盖
                    : chunkDurationMs;                    // 末字延伸到 chunk 结尾

                // Clamp to chunk boundary
                if (start > chunkDurationMs) start = chunkDurationMs;
                if (end   > chunkDurationMs) end   = chunkDurationMs;
                if (end <= start)
                    end = Math.Min(start + frameRateMs, chunkDurationMs);

                timestamps[i] = new int[] { start, end };
            }
            return timestamps;
        }

        /// <summary>
        /// Remove blank tokens (id 0-2) AND meta tokens (language/textnorm/emotion/event)
        /// in one pass, returning each surviving content token together with its original
        /// frame index in the decoder output sequence. The frame index is needed for the
        /// CTC-style timestamp fallback when the ONNX model does not output cif_peak.
        /// </summary>
        // CTC 折叠保护：同 token 连续 > MaxCollapseFrames 帧 (180ms) 视为长持音/拖音，
        // 重置 run 并放行一份；避免粤语长元音 / 慢拖音被压成单字（5s 持音 → 1 个 word，
        // UI 音字同步会僵硬卡 5s）。frameRateMs = FrameShift(10ms) * LfrN(6) = 60ms / 帧。
        private const int MaxCollapseFrames = 3; // 180ms / 60ms = 3 frames

        private (int[] Tokens, int[] FrameIndices) FilterBlanksAndMetas(int[] tokenIds)
        {
            var filteredTokens  = new List<int>(tokenIds.Length);
            var filteredIndices = new List<int>(tokenIds.Length);
            var metaIds = _metaTokenIds;
            int blankCount = 0;
            int metaCount = 0;
            int collapsedCount = 0;
            // CTC 折叠状态机：跟踪上一原始 token id（含 blank/meta，因为它们是 CTC 分隔信号）。
            // CTC 标准规则：相邻相同 token 视为同一发音的持续，折叠为一份；blank 起分隔作用，
            // "A A" → "A"，"A blank A" 保留两份 "A"。必须在 blank/meta 过滤之前判断，
            // 否则失去分隔信号 → 长字/慢语速场景出现"多少少"这类单字重复（用户反馈根因）。
            //
            // 2026-06-01 加长持音保护：run > MaxCollapseFrames 时重置 run 并 fall-through，
            // 每 ~180ms 放行一份。BGM 误识风险由 chunk-level FALLBACK 兜底。
            int prevRawTokenId = -1;
            int sameRunLength = 0;
            for (int i = 0; i < tokenIds.Length; i++)
            {
                int tid = tokenIds[i];
                if (tid == prevRawTokenId)
                {
                    sameRunLength++;
                    if (sameRunLength <= MaxCollapseFrames)
                    {
                        collapsedCount++;
                        continue;
                    }
                    // 持音超过 180ms：重置 run 并继续走 blank/meta/emit 链路
                    sameRunLength = 1;
                }
                else
                {
                    prevRawTokenId = tid;
                    sameRunLength = 1;
                }
                // 注意：故意不使用 FunASR 的 EOS-break 行为（OfflineRecognizer.cs:156 `if (token == 2) break`）。
                // 原因：FunASR 用在短的预分段干净语音上，chunk ≈ speech；我们 59s chunk 常包含
                // "前段语音 + 后段 BGM/音效"，若在语音末尾的 EOS 处截断会丢失后续 BGM 文字内容
                // （含歌词识别等合法 content）。策略：tid==2 与其他 blank 一样 continue。
                if (tid <= 2) { blankCount++; continue; }
                if (metaIds != null && metaIds.Contains(tid)) { metaCount++; continue; }
                // Fallback for the rare case where vocab scan produced an empty set
                if (LanguageTokenIds.Contains(tid)) { metaCount++; continue; }
                if (TextnormTokenIds.Contains(tid)) { metaCount++; continue; }
                filteredTokens.Add(tid);
                filteredIndices.Add(i);
            }
            // 诊断：blank vs meta 分解 + CTC 折叠计数
            if (tokenIds.Length > 0)
                FileUtils.LogAnalysis($"[SVS] 过滤分解: total={tokenIds.Length} blank={blankCount} meta={metaCount} collapsed={collapsedCount} content={filteredTokens.Count}");
            return (filteredTokens.ToArray(), filteredIndices.ToArray());
        }

        // MergeWords: 组合 token 为可读的 Word 条目。
        //
        // CJK 字符：按"语速 + 硬上限"分组（参见 MaxCharsPerCjkWord / CjkBreakThresholdMs 注释）。
        //   - 累积进 cjkBuf，每个 Word 最多 3 个字
        //   - 上一字 duration > 300ms 视为自然停顿，触发 flush
        // ASCII：保持原 SentencePiece "▁" 词起始边界 → 完整英文词。
        // 标点：不丢弃 — 附加到上一个 Word 的末尾（与 Tencent 行为对齐，让服务端
        //   subSentence 能按 endsWith("，"|"。"|...) 切句）。
        // 时间戳缺失（[-1,-1]）的 token 直接丢弃，避免 chunk 起点附近的幻影词条。
        //
        // 同时构建 resultText（合并旧 BuildPunctuatedText 单遍扫描）：直接拼接 wordList
        // 末态的 Word 字符串（含标点），AsrUtils.RebuildResultText 之后可能再覆盖此值
        // 以加 ASCII 词后空格，不冲突。
        private (List<ASRWordEntity> words, string resultText) MergeWords(int[] tokenIds, int[][] timestamps)
        {
            var words = new List<ASRWordEntity>();
            if (tokenIds.Length == 0)
                return (words, string.Empty);

            var asciiBuf = new System.Text.StringBuilder();
            int asciiStart = -1;
            int asciiEnd = -1;

            var cjkBuf = new System.Text.StringBuilder();
            int cjkStart = -1;
            int cjkEnd = -1;
            int cjkLastCharStart = -1;
            int cjkCharCount = 0;

            for (int i = 0; i < tokenIds.Length; i++)
            {
                var detail = GetTokenTextDetailed(tokenIds[i]);
                string token = detail.text;
                bool isWordStart = detail.isWordStart;
                if (string.IsNullOrEmpty(token))
                    continue;

                int tokenStart = (i < timestamps.Length && timestamps[i]?.Length >= 2) ? timestamps[i][0] : -1;
                int tokenEnd   = (i < timestamps.Length && timestamps[i]?.Length >= 2) ? timestamps[i][1] : -1;

                // Drop tokens whose CIF fire point was unresolved — phantom output otherwise.
                if (tokenStart < 0 || tokenEnd < 0)
                    continue;

                // ① Punctuation: flush both buffers, then 挂到上一个 Word 末尾 (与 Tencent 行为对齐)
                if (PunctuationSet.Contains(token))
                {
                    FlushAsciiBuffer(words, asciiBuf, ref asciiStart, ref asciiEnd);
                    FlushCjkBuffer(words, cjkBuf, ref cjkStart, ref cjkEnd, ref cjkLastCharStart, ref cjkCharCount);
                    if (words.Count > 0)
                    {
                        var prev = words[words.Count - 1];
                        prev.Word = prev.Word + token;
                        if (tokenEnd > prev.EndTime)
                            prev.EndTime = tokenEnd;
                    }
                    continue;
                }

                bool tokenIsAscii = IsAsciiOnly(token);
                if (tokenIsAscii)
                {
                    // ASCII 进来 → 先 flush CJK 缓冲
                    FlushCjkBuffer(words, cjkBuf, ref cjkStart, ref cjkEnd, ref cjkLastCharStart, ref cjkCharCount);

                    // SentencePiece word-start marker indicates a new English word
                    if (isWordStart)
                        FlushAsciiBuffer(words, asciiBuf, ref asciiStart, ref asciiEnd);

                    if (asciiBuf.Length == 0)
                        asciiStart = tokenStart;
                    asciiBuf.Append(token);
                    asciiEnd = tokenEnd;
                    continue;
                }

                // CJK 进来 → 先 flush ASCII 缓冲
                FlushAsciiBuffer(words, asciiBuf, ref asciiStart, ref asciiEnd);

                foreach (var ch in token)
                {
                    // 决定要不要先 flush 当前 CJK 缓冲再追加新字
                    if (cjkBuf.Length > 0)
                    {
                        bool shouldFlush = false;
                        if (cjkCharCount >= MaxCharsPerCjkWord)
                            shouldFlush = true;
                        else
                        {
                            // 上一字 duration = 它与下一字（即当前字）之间的间隔
                            // > 阈值视为自然停顿，flush
                            int prevCharDuration = cjkEnd - cjkLastCharStart;
                            if (prevCharDuration > CjkBreakThresholdMs)
                                shouldFlush = true;
                        }
                        if (shouldFlush)
                            FlushCjkBuffer(words, cjkBuf, ref cjkStart, ref cjkEnd, ref cjkLastCharStart, ref cjkCharCount);
                    }

                    if (cjkBuf.Length == 0)
                        cjkStart = tokenStart;
                    cjkBuf.Append(ch);
                    cjkEnd = tokenEnd;
                    cjkLastCharStart = tokenStart;
                    cjkCharCount++;
                }
            }

            FlushAsciiBuffer(words, asciiBuf, ref asciiStart, ref asciiEnd);
            FlushCjkBuffer(words, cjkBuf, ref cjkStart, ref cjkEnd, ref cjkLastCharStart, ref cjkCharCount);

            // ITN 后处理：修补 FunASR WeTextProcessing FST 对 ordinal "第N" 的覆盖缺失。
            // 例 "第二7" → "第27"。仅处理"中文数字字符紧邻阿拉伯数字"，不处理位词
            // 十/百/千/万，避免误转 "二十" 这类需要 cardinal 转换的复杂场景。
            int itnFixed = NormalizeAdjacentDigits(words);

            // ITN 位词后处理：阿拉伯数字 + 中文位词 → 阿拉伯数字 × 位值
            // 例 "8十"→"80", "5百"→"500", "2千"→"2000", "3万"→"30000"
            // 仅处理"阿拉伯数字在左"模式；不处理"十5"或"二十5"等 cardinal mode。
            int positionalFixed = NormalizeArabicWithPositional(words);

            // 单遍拼接 resultText：词条 Word 直接 concat（标点已挂在词末尾）
            var resultSb = new System.Text.StringBuilder(words.Count * 3);
            foreach (var w in words)
                resultSb.Append(w.Word);
            if (itnFixed > 0 || positionalFixed > 0)
                FileUtils.LogAnalysis($"[SVS] ITN 修补: {itnFixed} 处单字数字, {positionalFixed} 处位词");
            return (words, resultSb.ToString());
        }

        // 中文数字 → 阿拉伯字符映射。仅覆盖单字数字（零/〇/一-九）；位词十/百/千/万/亿不处理。
        private static readonly Dictionary<char, char> ChineseDigitToArabic = new Dictionary<char, char>
        {
            { '零', '0' }, { '〇', '0' },
            { '一', '1' }, { '二', '2' }, { '三', '3' }, { '四', '4' }, { '五', '5' },
            { '六', '6' }, { '七', '7' }, { '八', '8' }, { '九', '9' }
        };

        // 中文位词 → 数值。仅由 NormalizeArabicWithPositional 使用（阿拉伯数字 × 位值场景）。
        // 不含"亿"：极少见且超 int 风险；如需可扩展为 long 路径。
        private static readonly Dictionary<char, int> ChinesePositionalValue = new Dictionary<char, int>
        {
            { '十', 10 }, { '百', 100 }, { '千', 1000 }, { '万', 10000 }
        };

        private static bool IsArabicDigit(char c) => c >= '0' && c <= '9';

        /// <summary>
        /// 扫描 wordList，若中文数字字符紧邻阿拉伯数字（同词内左右邻 / 跨词的相邻词末-首字），
        /// 将该中文数字转为阿拉伯。返回总修补字符数。
        /// 例："第二7" → "第27"；"三个9"（"三"紧邻"个"非阿拉伯）保持不变；
        ///     "三个九"（全无阿拉伯邻居）保持不变。
        /// 迭代到收敛以正确传播 "二二7" → "二2" → "22" 这类多步转换；
        /// 安全上限 5 轮（每轮至少修一个，长度有限）。
        /// </summary>
        private static int NormalizeAdjacentDigits(List<ASRWordEntity> words)
        {
            if (words == null || words.Count == 0) return 0;
            int totalFixed = 0;
            for (int pass = 0; pass < 5; pass++)
            {
                int passFixed = 0;
                for (int i = 0; i < words.Count; i++)
                {
                    var w = words[i];
                    if (string.IsNullOrEmpty(w.Word)) continue;
                    var chars = w.Word.ToCharArray();
                    bool changed = false;
                    for (int j = 0; j < chars.Length; j++)
                    {
                        if (!ChineseDigitToArabic.TryGetValue(chars[j], out char arabic)) continue;
                        bool prevIsArabic =
                            (j > 0 && IsArabicDigit(chars[j - 1]))
                            || (j == 0 && i > 0
                                && !string.IsNullOrEmpty(words[i - 1].Word)
                                && IsArabicDigit(words[i - 1].Word[words[i - 1].Word.Length - 1]));
                        bool nextIsArabic =
                            (j < chars.Length - 1 && IsArabicDigit(chars[j + 1]))
                            || (j == chars.Length - 1 && i < words.Count - 1
                                && !string.IsNullOrEmpty(words[i + 1].Word)
                                && IsArabicDigit(words[i + 1].Word[0]));
                        if (prevIsArabic || nextIsArabic)
                        {
                            chars[j] = arabic;
                            changed = true;
                            passFixed++;
                        }
                    }
                    if (changed) w.Word = new string(chars);
                }
                totalFixed += passFixed;
                if (passFixed == 0) break;
            }
            return totalFixed;
        }

        /// <summary>
        /// 阿拉伯数字 + 中文位词（十/百/千/万）→ 阿拉伯数字 × 位值。
        /// 处理同 word 内（"8十块"）和跨 word 边界（word1="8" + word2="十块"）两种情形。
        /// 仅当"阿拉伯数字"在位词左侧时触发；不处理 "十5"（位词在前）或 "二十5"（cardinal mode）。
        /// 例: "8十" → "80"; "5百多" → "500多"; "2千个" → "2000个"; "3万" → "30000"。
        /// 返回修补次数（每次合并一段记 1 次）。
        /// </summary>
        private static int NormalizeArabicWithPositional(List<ASRWordEntity> words)
        {
            if (words == null || words.Count == 0) return 0;
            int totalFixed = 0;

            for (int i = 0; i < words.Count; i++)
            {
                var w = words[i];
                if (string.IsNullOrEmpty(w.Word)) continue;
                string word = w.Word;
                var sb = new System.Text.StringBuilder(word.Length);
                int j = 0;

                while (j < word.Length)
                {
                    char c = word[j];
                    if (IsArabicDigit(c))
                    {
                        // 提取连续阿拉伯数字段
                        int start = j;
                        while (j < word.Length && IsArabicDigit(word[j])) j++;
                        string digitStr = word.Substring(start, j - start);

                        // 检测紧邻位词：同 word 内 OR 下一个 word 的首字
                        int positionalValue = 0;
                        bool positionalInThisWord = false;
                        if (j < word.Length && ChinesePositionalValue.TryGetValue(word[j], out positionalValue))
                        {
                            positionalInThisWord = true;
                        }
                        else if (j == word.Length && i < words.Count - 1
                                 && !string.IsNullOrEmpty(words[i + 1].Word)
                                 && ChinesePositionalValue.TryGetValue(words[i + 1].Word[0], out positionalValue))
                        {
                            // positionalValue 已设；位词在下一个 word 首字（跨 word 边界）
                        }

                        if (positionalValue > 0 && long.TryParse(digitStr, out long num))
                        {
                            sb.Append((num * positionalValue).ToString());
                            if (positionalInThisWord)
                            {
                                j++; // 跳过本 word 内的位词字符
                            }
                            else
                            {
                                // 删除下一个 word 的首字（位词），其余保留
                                words[i + 1].Word = words[i + 1].Word.Substring(1);
                            }
                            totalFixed++;
                        }
                        else
                        {
                            sb.Append(digitStr);
                        }
                    }
                    else
                    {
                        sb.Append(c);
                        j++;
                    }
                }

                if (sb.Length != word.Length || sb.ToString() != word)
                    w.Word = sb.ToString();
            }
            return totalFixed;
        }

        private static void FlushAsciiBuffer(List<ASRWordEntity> words, System.Text.StringBuilder buf, ref int startMs, ref int endMs)
        {
            if (buf.Length == 0)
                return;
            words.Add(new ASRWordEntity
            {
                Word = buf.ToString(),
                StartTime = Math.Max(0, startMs),
                EndTime = Math.Max(endMs, startMs + 1)
            });
            buf.Clear();
            startMs = -1;
            endMs = -1;
        }

        private static void FlushCjkBuffer(List<ASRWordEntity> words, System.Text.StringBuilder buf,
            ref int startMs, ref int endMs, ref int lastCharStart, ref int charCount)
        {
            if (buf.Length == 0)
                return;
            words.Add(new ASRWordEntity
            {
                Word = buf.ToString(),
                StartTime = Math.Max(0, startMs),
                EndTime = Math.Max(endMs, startMs + 1)
            });
            buf.Clear();
            startMs = -1;
            endMs = -1;
            lastCharStart = -1;
            charCount = 0;
        }

        private static bool IsAsciiOnly(string s)
        {
            for (int i = 0; i < s.Length; i++)
            {
                if (s[i] >= 128) return false;
            }
            return s.Length > 0;
        }

        private string GetTokenText(int tokenId)
        {
            return GetTokenTextDetailed(tokenId).text;
        }

        /// <summary>
        /// Returns the token text with its SentencePiece word-start flag.
        /// isWordStart is true when the raw token starts with U+2581 ("▁"), which marks
        /// the beginning of a new word in SenseVoice's BPE vocabulary. MergeWords uses this
        /// to group ASCII sub-words (e.g. "▁he","ll","o" → "hello") without merging across
        /// adjacent English words.
        /// </summary>
        private (string text, bool isWordStart) GetTokenTextDetailed(int tokenId)
        {
            if (_vocab == null || tokenId < 0 || tokenId >= _vocab.Length)
            {
                if (_vocab != null)
                    FileUtils.LogAnalysis($"tokenId {tokenId} 超出词汇表范围 (0..{_vocab.Length - 1})");
                return ("", false);
            }
            string token = _vocab[tokenId];
            if (string.IsNullOrEmpty(token))
                return ("", false);

            bool isWordStart = token.StartsWith("▁");
            if (isWordStart)
                token = token.Substring(1);

            return (token, isWordStart);
        }

        private Dictionary<string, object> CreateErrorResult(int code, string msg)
        {
            return new Dictionary<string, object>
            {
                { "code", code },
                { "data", new List<ASRResultEntity>
                    {
                        new ASRResultEntity { Code = code, Msg = msg }
                    }
                }
            };
        }

        #endregion

        private class ModelOutput
        {
            public float[] LogitsCopy;
            public int VocabSize;
            public int SequenceLength;
            public float[] CifPeak;
        }
    }
}
