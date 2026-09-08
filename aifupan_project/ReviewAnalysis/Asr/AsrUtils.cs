using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.api;

namespace ReviewAnalysis.Asr
{
    public class AsrUtils
    {
        private static readonly Lazy<AsrEngineFactory> _engineFactory = new Lazy<AsrEngineFactory>();

        /// <summary>
        /// 语音识别：先查询后端可用引擎列表，再本地识别；后端不可达则兜底默认 Composite(SVS + Tencent)。
        /// </summary>
        /// <param name="directoryPath">音频文件夹路径</param>
        /// <param name="token">
        /// 保留参数（历史兼容）。Tencent 临时凭证由 MyCallableTask 内部通过 AsrApi.GetTempToken 自取，
        /// 此处不再向下传递。
        /// </param>
        /// <param name="engSerViceType">
        /// 语言码，默认 16k_zh（中文通用）。支持：
        /// 16k_zh / 16k_zh-PY / 16k_zh_medical / 16k_en / 16k_yue / 16k_ja / 16k_ko /
        /// 16k_vi / 16k_ms / 16k_id / 16k_fil / 16k_th / 16k_pt / 16k_tr /
        /// 16k_ar / 16k_es / 16k_hi / 16k_fr / 16k_de。
        /// 非法值自动归一化为 16k_zh。
        /// </param>
        /// <param name="consumer">业务消费方，必填。后端据此差异化下发引擎优先级。</param>
        /// <param name="forceTencentOnly">
        /// 强制只走 Tencent 单引擎（跳过后端引擎查询与本地 SVS）。
        /// 仅录播「手动重新分析」场景传 true；其余消费方/来源保持默认（false）。
        /// 单引擎链下 CompositeASREngine 不做质量评分，Tencent 失败即整体失败。
        /// </param>
        /// <param name="videoId">
        /// 录播视频 id，用于「加速」在飞切换（识别中途把剩余分片切腾讯）。
        /// 仅录播消费方传入；其余消费方/来源传 null → 不参与加速，行为与改动前一致。
        /// </param>
        /// <returns>500：识别错误 601：本地时间不正确 602：获取临时调用凭证错误 603：QPS已满 701：音频文件不存在 702：模型未就绪 703：音频解码失败 704：音频超60秒</returns>
        public static async Task<Dictionary<string, object>> AsrByDirectoryPath(
            string directoryPath, string token, string engSerViceType, AsrConsumerType consumer,
            bool forceTencentOnly = false, string videoId = null)
        {
            try
            {
                engSerViceType = AsrLanguageCode.Normalize(engSerViceType);

                // 1. 确定引擎列表：手动重试强制 [tencent]；否则查询后端可用引擎，失败兜底默认 Composite
                List<string> availableEngines = null;
                if (forceTencentOnly)
                {
                    availableEngines = new List<string> { "tencent" };
                    FileUtils.LogAnalysis("[ASR] 手动重新分析，强制只走 Tencent 引擎");
                }
                else
                {
                    try
                    {
                        availableEngines = await AsrApi.GetAvailableEnginesAsync(engSerViceType, consumer);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogAnalysis($"[ASR] 查询可用引擎异常，兜底默认 Composite: {ex.Message}");
                    }
                }

                // 2. 按引擎列表（null → 默认路由）构建本地管道，然后识别
                var engine = _engineFactory.Value.Build(engSerViceType, availableEngines);
                return await RunLocalEngineAsync(directoryPath, engSerViceType, engine, videoId);
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", "语音识别失败");
                return new Dictionary<string, object>
                {
                    { "code", 500 },
                    { "data", new List<ASRResultEntity>() }
                };
            }
        }

        // ── private ─────────────────────────────────────────────────────────

        /// <summary>
        /// 使用指定引擎对目录内所有音频文件执行本地识别，并对结果排序、编号、重建 Result 文本。
        /// 任意文件识别失败（非 0 code）时立即返回，不继续处理后续文件。
        /// </summary>
        private static async Task<Dictionary<string, object>> RunLocalEngineAsync(
            string directoryPath, string engSerViceType, IASREngine engine, string videoId = null)
        {
            var asrResultList = new List<ASRResultEntity>();

            var files = Directory.EnumerateFiles(directoryPath, "*", SearchOption.AllDirectories)
                                 .Select(p => new FileInfo(p))
                                 .OrderBy(f => f.Name)
                                 .ToList();

            if (files.Count == 0)
                return MakeResult(701, asrResultList);

            // 标记该 videoId 的 ASR 循环已开始 —— 加速信号只在此窗口内被接受（见 AsrAccelerateSignal）。
            AsrAccelerateSignal.MarkRunning(videoId);
            try
            {
                // 「加速」在飞切换：识别中途前端可请求把剩余分片切腾讯。
                // 检测点在循环顶部 → 当前分片的 RecognizeAsync 先自然跑完，下一分片才换引擎
                //（等当前分片跑完再切，不中断在飞的 ONNX 推理）；已识别分片结果原样保留。
                var currentEngine = engine;
                bool switched = false;

                foreach (var file in files)
                {
                    if (!switched && AsrAccelerateSignal.IsRequested(videoId))
                    {
                        currentEngine = _engineFactory.Value.Build(
                            engSerViceType, new List<string> { "tencent" });
                        switched = true;
                        FileUtils.LogAnalysis($"[ASR] 加速触发，剩余分片切 Tencent, videoId={videoId}");
                    }

                    var taskResult = await currentEngine.RecognizeAsync(file, engSerViceType);
                    int code = Convert.ToInt32(taskResult["code"]);
                    if (code != 0)
                        return MakeResult(code, asrResultList);

                    var entities = (List<ASRResultEntity>)taskResult["data"];
                    if (entities != null)
                        asrResultList.AddRange(entities);

                    // Each engine returns WordList timestamps in chunk-relative milliseconds (0..chunkDuration).
                    // We DO NOT add any cumulative offset here — downstream consumers expect the contract
                    //   absolute_time = (Paragraph - 1) * audioLength + WordList[i].StartTime
                    // and apply the conversion themselves (server-side subSentence, front-end audio-text sync, etc.).
                }
            }
            finally
            {
                // 先 MarkStopped 拒绝后续加速请求，再 Clear 抹掉最后一刻可能挤进来的信号；
                // 无论成功/失败/异常都执行，防止残留影响该 videoId 的后续重分析。
                AsrAccelerateSignal.MarkStopped(videoId);
                AsrAccelerateSignal.Clear(videoId);
            }

            int fallbackCount = asrResultList.Count(r => r.FromFallback);
            int lowQualityCount = asrResultList.Count(r => r.LowQuality);
            int total = asrResultList.Count;

            // 单引擎链或全部高质场景：fallbackCount=0 时不打 FALLBACK 行（无意义噪声）。
            // 同理 lowQualityCount=0 时不打低质汇总。
            var summary = new System.Text.StringBuilder();
            summary.Append($"语音识别完成一共: {total}段");
            if (lowQualityCount > 0)
            {
                double lowRate = total > 0 ? lowQualityCount * 100.0 / total : 0.0;
                summary.Append($", LOW_QUALITY: {lowQualityCount}段 ({lowRate:F1}%)");
            }
            if (fallbackCount > 0)
            {
                double fallbackRate = total > 0 ? fallbackCount * 100.0 / total : 0.0;
                summary.Append($", FALLBACK: {fallbackCount}段 ({fallbackRate:F1}%)");
            }
            FileUtils.LogAnalysis(summary.ToString());

            asrResultList.Sort((x, y) => x.FileName.CompareTo(y.FileName));

            // 跨段 Word 拼合：修复 59s 硬切边界把单字断成两半的情况（如 "好家" + "伙"）。
            // 必须在 RebuildResultText 之前执行 — 后者会给 ASCII 词追加尾随空格污染 Word。
            TryMergeChunkBoundaries(asrResultList);

            for (int i = 0; i < asrResultList.Count; i++)
            {
                asrResultList[i].Paragraph = i + 1;
                RebuildResultText(asrResultList[i]);
            }

            return MakeResult(0, asrResultList);
        }

        // 跨段拼合的判定阈值，注释解释见 TryMergeChunkBoundaries：
        private const int ChunkBoundaryWindowMs = 200;   // 距离 chunk 起止 <200ms 视为边界字
        private const int MaxMergedCjkLen = 4;            // 拼合后最长 4 字（MergeWords 3 字+1 字溢出场景）
        private static readonly HashSet<char> SentencePuncts =
            new HashSet<char> { '。', '？', '！', '，', '、', '.', '?', '!', ',' };

        /// <summary>
        /// 修复硬切边界把单字断成两半的情况。保守启发式：
        ///   ① chunk N 末词 EndTime 贴近 chunkDuration（被切到末尾）
        ///   ② chunk N+1 首词 StartTime 贴近 0（被切到开头）
        ///   ③ 两词都是纯 CJK（避免 ASCII 词混淆）
        ///   ④ 拼合后长度 ≤ 4 字（MergeWords 默认 3 字 word，允许 +1 字溢出）
        ///   ⑤ N 末词**不以句末标点结尾**（句子已结束的不合并）
        /// 满足全部条件时：N 末词文本 = 拼接结果，N+1 首词删除；时间戳保持 chunk-relative（下游
        /// 用 paragraph * chunkDuration + WordList[i].StartTime 计算绝对时间，跨段字在 N 上轻微偏移
        /// 可接受，远好于"好" / "家伙" 错切体验）。
        /// </summary>
        private static void TryMergeChunkBoundaries(List<ASRResultEntity> entities)
        {
            int mergeCount = 0;
            for (int i = 0; i < entities.Count - 1; i++)
            {
                var curr = entities[i];
                var next = entities[i + 1];
                if (curr.WordList == null || curr.WordList.Count == 0) continue;
                if (next.WordList == null || next.WordList.Count == 0) continue;

                var currLast = curr.WordList[curr.WordList.Count - 1];
                var nextFirst = next.WordList[0];

                // ① 时间边界
                long currChunkDur = curr.AudioDuration;
                if (currChunkDur <= 0) continue;
                if (currLast.EndTime < currChunkDur - ChunkBoundaryWindowMs) continue;
                if (nextFirst.StartTime > ChunkBoundaryWindowMs) continue;

                // ⑤ N 末词不能以句末标点结尾
                if (string.IsNullOrEmpty(currLast.Word)) continue;
                char lastChar = currLast.Word[currLast.Word.Length - 1];
                if (SentencePuncts.Contains(lastChar)) continue;

                // ③ 纯 CJK
                if (!IsAllCjk(currLast.Word) || !IsAllCjk(nextFirst.Word)) continue;

                // ④ 拼合长度
                if (currLast.Word.Length + nextFirst.Word.Length > MaxMergedCjkLen) continue;

                // 执行拼合
                string merged = currLast.Word + nextFirst.Word;
                currLast.Word = merged;
                // EndTime 保持 currChunkDur（chunk-relative 上限），N+1 首词移除
                next.WordList.RemoveAt(0);
                mergeCount++;
                FileUtils.LogAnalysis($"[ASR] 跨段拼合: \"{merged}\" (段 {i + 1}↔{i + 2})");
            }
            if (mergeCount > 0)
                FileUtils.LogAnalysis($"[ASR] 跨段拼合共 {mergeCount} 处");
        }

        private static bool IsAllCjk(string s)
        {
            if (string.IsNullOrEmpty(s)) return false;
            foreach (var ch in s)
            {
                // 基本 CJK 统一表意文字区间（0x4E00..0x9FFF），覆盖绝大多数中日韩汉字
                if (ch < 0x4E00 || ch > 0x9FFF) return false;
            }
            return true;
        }

        /// <summary>
        /// 将 WordList 中的词条拼接为 Result 文本（与线上 Tencent 行为对齐）：
        ///   ① 强制清空 entity.Result 再重建（不论引擎是否已写入）
        ///   ② 对 ASCII 词（^[a-zA-Z'.]+$）就地追加 2 个空格到 item.Word，让服务端按词分词
        ///   ③ 把所有 item.Word 顺序拼接到 entity.Result
        /// 注意：这会就地修改 WordList 内每个 ASRWordEntity.Word，与线上一致。
        /// </summary>
        private static void RebuildResultText(ASRResultEntity entity)
        {
            if (entity.WordList == null || entity.WordList.Count == 0)
                return;

            entity.Result = "";
            foreach (var item in entity.WordList)
            {
                if (item.Word != null && Regex.IsMatch(item.Word, @"^[a-zA-Z'.]+$"))
                {
                    item.Word = item.Word + "  ";
                }
                entity.Result += item.Word;
            }
        }

        /// <summary>构造统一返回格式 {code, data}。</summary>
        private static Dictionary<string, object> MakeResult(int code, List<ASRResultEntity> data) =>
            new Dictionary<string, object> { { "code", code }, { "data", data } };
    }
}
