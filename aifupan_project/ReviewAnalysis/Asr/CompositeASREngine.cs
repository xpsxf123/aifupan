using douyin.Utils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// Composite engine with priority-ordered degradation.
    /// Iterates sub-engines in insertion order (highest priority first). First success is returned.
    /// All engines must fail before an error is reported.
    ///
    /// 术语：日志中的 "FALLBACK" 指 **SVS 降级到 Tencent 兜底**（SVS 视角），
    /// 不是腾讯被降级。Tencent 不参与质量评分，code=0 且 Result 非空即视为成功。
    /// 质量评分仅作用于 SVS，规则见 <see cref="IsLowQuality"/>。
    /// </summary>
    public class CompositeASREngine : IASREngine
    {
        private readonly List<EngineEntry> _entries;

        // Route A v2: IsLowQuality 判定 SSOT 阈值常量
        // 判定必须基于**事实 token 统计**（rule1 折叠率、rule2 字符密度），不要基于 WordList
        // 间隙——后者无法区分"模型识别失败"与"真没人说话"，对真静默场景会产生 false positive
        // 与无意义 Tencent API 调用。粤语 5–6s 无推理场景由下调 rule1 阈值兜底。
        //
        // 2026-06-01 collapsed_ratio 演进：0.95 → 0.90 → 0.85。
        // boss-approved 接受更多 Tencent 调用；生产 smoke 双峰分布（口播 0.05–0.30 / 唱歌 0.99）
        // 之间空挡较大，0.85 仍远高于口播上限，不会误伤纯口播 chunk。
        private const double CtcCollapseThreshold = 0.85;   // 规则 1: CTC 折叠率 > 阈值视为低质
        private const double CharDensityThreshold = 0.5;    // 规则 2: 字符密度 < 0.5 char/s 视为低质

        public CompositeASREngine(IEnumerable<EngineEntry> entries)
        {
            _entries = new List<EngineEntry>(entries); // preserve insertion (priority) order
        }

        // engSerViceType is intentionally unused: each sub-engine uses its EngineEntry.Config
        // fixed at factory Build() time.
        public async Task<Dictionary<string, object>> RecognizeAsync(FileInfo audioFile, string engSerViceType)
        {
            FileUtils.LogAnalysis($"[ASR] Composite引擎启动, 链: {string.Join(" → ", _entries.ConvertAll(e => e.Engine.GetType().Name))}");

            foreach (var entry in _entries)
            {
                Dictionary<string, object> result;
                try
                {
                    result = await entry.Engine.RecognizeAsync(audioFile, entry.Config);
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"[ASR] 引擎 {entry.Engine.GetType().Name} 抛出异常: {ex.Message}, 尝试下一个引擎");
                    continue;
                }

                int code = (int)result["code"];

                if (code == 0)
                {
                    // Route A v2: chunk-level quality assessment + Tencent fallback。
                    // 单引擎链没有兜底候选，评分无意义（自己降级到自己也不会有改善），直接跳过。
                    var svsList = result["data"] as List<ASRResultEntity>;
                    var svsResult = svsList?.Count > 0 ? svsList[0] : null;
                    if (svsResult != null && _entries.Count > 1 && IsLowQuality(svsResult))
                    {
                        result = await TryTencentFallbackAsync(audioFile, svsResult);
                    }

                    FileUtils.LogAnalysis($"[ASR] 引擎 {entry.Engine.GetType().Name} 识别成功");
                    return result;
                }

                FileUtils.LogAnalysis(
                    $"[ASR] 引擎降级: {entry.Engine.GetType().Name} 返回 code={code}, 尝试下一个引擎");
            }

            return new Dictionary<string, object>
            {
                { "code", 500 },
                { "data", new List<ASRResultEntity>
                    {
                        new ASRResultEntity { Code = 500, Msg = "所有ASR引擎均失败" }
                    }
                }
            };
        }

        // Route A v2 — Quality assessment SSOT
        private static bool IsLowQuality(ASRResultEntity result)
        {
            // 规则 1 — CTC 折叠率：非内容 token 占比 > CtcCollapseThreshold（唱歌段特征）
            bool rule1 = result.DecodedTokens > 0
                && (result.CollapsedTokens / (double)result.DecodedTokens) > CtcCollapseThreshold;

            // 规则 2 — 字符密度过低：识别文本 < CharDensityThreshold char/s
            bool rule2 = result.AudioDuration > 0
                && ((result.Result?.Length ?? 0) / (result.AudioDuration / 1000.0)) < CharDensityThreshold;

            return rule1 || rule2;
        }

        private async Task<Dictionary<string, object>> TryTencentFallbackAsync(
            FileInfo audioFile, ASRResultEntity svsResult)
        {
            // 进入兜底分支即代表 SVS 判定低质 — 在最早位置标记 svsResult，
            // 确保即便走 no_tencent_engine 或 Tencent 异常路径也能正确计入 video 级低质汇总。
            svsResult.LowQuality = true;

            var tencentEntry = _entries.Find(e => e.Engine is TencentASREngine);
            if (tencentEntry == null)
            {
                FileUtils.LogAnalysis(
                    $"[Composite-FALLBACK] chunk={audioFile.Name} rule=no_tencent_engine action=KEPT_SVS");
                return BuildSvsResult(svsResult);
            }

            string ruleName = DetermineRuleName(svsResult);
            // 两条规则的实际数值都打出来，避免 rule=char_density 时打 collapse ratio 造成的指标错配。
            double collapse = svsResult.DecodedTokens > 0
                ? svsResult.CollapsedTokens / (double)svsResult.DecodedTokens : 0.0;
            double density = svsResult.AudioDuration > 0
                ? (svsResult.Result?.Length ?? 0) / (svsResult.AudioDuration / 1000.0) : 0.0;
            int tencentCode = -1;
            string action = "KEPT_SVS";

            try
            {
                var tencentResult = await tencentEntry.Engine.RecognizeAsync(audioFile, tencentEntry.Config);
                tencentCode = (int)tencentResult["code"];
                var tencentList = tencentResult["data"] as List<ASRResultEntity>;
                bool tencentOk = tencentCode == 0
                    && tencentList?.Count > 0
                    && !string.IsNullOrEmpty(tencentList[0].Result);
                action = tencentOk ? "REPLACED" : "KEPT_SVS";
                FileUtils.LogAnalysis(
                    $"[Composite-FALLBACK] chunk={audioFile.Name} rule={ruleName} collapse={collapse:F3} density={density:F3} tencent_code={tencentCode} action={action}");
                if (tencentOk)
                {
                    // 标记 Tencent 返回的 entity 来自 fallback + 保留 LowQuality 传递，供 AsrUtils video 级汇总
                    tencentList[0].FromFallback = true;
                    tencentList[0].LowQuality = true;
                    return tencentResult;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis(
                    $"[Composite-FALLBACK] chunk={audioFile.Name} rule={ruleName} collapse={collapse:F3} density={density:F3} tencent_code=-1 action=KEPT_SVS ex={ex.Message}");
            }

            return BuildSvsResult(svsResult);
        }

        private static string DetermineRuleName(ASRResultEntity r)
        {
            if (r.DecodedTokens > 0 && (r.CollapsedTokens / (double)r.DecodedTokens) > CtcCollapseThreshold)
                return "collapsed_ratio";
            return "char_density";
        }

        private static Dictionary<string, object> BuildSvsResult(ASRResultEntity svsResult)
        {
            return new Dictionary<string, object>
            {
                { "code", 0 },
                { "data", new List<ASRResultEntity> { svsResult } }
            };
        }
    }

    public class EngineEntry
    {
        public IASREngine Engine { get; }
        public string Config { get; }

        public EngineEntry(IASREngine engine, string config)
        {
            Engine = engine;
            Config = config;
        }
    }
}
