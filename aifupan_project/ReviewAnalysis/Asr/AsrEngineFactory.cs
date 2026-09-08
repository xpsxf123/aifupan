using douyin.Utils;
using ReviewAnalysis.Asr.Local;
using System;
using System.Collections.Generic;
using System.Linq;

namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// Engine factory. Pre-creates pooled engine instances at construction time.
    ///
    /// Build(language):               default routing — SVS-supported → Composite(SVS→Tencent); else → Tencent only.
    /// Build(language, engineList):   backend-directed routing — list order = priority; empty/null → default routing.
    /// Engine identifiers:            "sense-voice" → SenseVoiceSmallEngine, "tencent" → TencentASREngine.
    /// </summary>
    public class AsrEngineFactory
    {
        private static readonly Dictionary<string, string> EngineIdToPoolKey =
            new Dictionary<string, string>
            {
                ["sense-voice"] = "svs",
                ["tencent"]     = "tencent"
            };

        // 引擎按需创建：SenseVoiceSmallEngine 构造会跑 GpuProbe（DML/CUDA 探测），
        // 在不支持的机器上可能抛 AccessViolationException 导致整个工厂初始化失败。
        // 用 Lazy 延迟到首次真正用到该引擎才实例化——「只走腾讯」路径永不触碰 SVS 构造。
        private readonly Dictionary<string, Lazy<IASREngine>> _pool;

        public AsrEngineFactory()
        {
            _pool = new Dictionary<string, Lazy<IASREngine>>
            {
                ["tencent"] = new Lazy<IASREngine>(() => new TencentASREngine()),
                ["svs"]     = new Lazy<IASREngine>(() => new SenseVoiceSmallEngine())
            };
        }

        /// <summary>
        /// Build the engine pipeline for a normalized language code.
        /// When <paramref name="engines"/> is null or empty, falls back to default routing.
        /// </summary>
        /// <param name="language">Normalized Tencent language code (e.g. "16k_zh").</param>
        /// <param name="engines">
        /// Ordered list of engine identifiers returned by the backend
        /// (e.g. ["sense-voice","tencent"]). Null → default routing.
        /// </param>
        public IASREngine Build(string language, IReadOnlyList<string> engines = null)
        {
            if (string.IsNullOrEmpty(language))
                language = AsrLanguageCode.Default;

            var entries = ResolveEntries(language, engines);

            FileUtils.LogAnalysis(
                $"[ASR] 构建引擎管道: [{string.Join("→", entries.Select(e => e.Engine.GetType().Name))}], language=\"{language}\"");

            return new CompositeASREngine(entries);
        }

        // ── private ─────────────────────────────────────────────────────────

        /// <summary>
        /// 路由入口：优先使用后端指定列表，列表为空则回退默认路由。
        /// </summary>
        private List<EngineEntry> ResolveEntries(string language, IReadOnlyList<string> engines)
        {
            if (engines != null && engines.Count > 0)
                return BuildEntriesFromList(language, engines);

            return BuildDefaultEntries(language);
        }

        /// <summary>
        /// 默认路由：SenseVoiceSmall 支持的语言 → [SVS, Tencent]（本地优先，云端兜底）；
        /// 不支持的语言（如越南语、法语等）→ [Tencent]（直接走云端）。
        /// </summary>
        private List<EngineEntry> BuildDefaultEntries(string language)
        {
            var entries = new List<EngineEntry>();
            if (AsrLanguageCode.IsSvsSupported(language))
                TryAddEngine(entries, "svs", language);
            // Tencent 始终兜底：本地 SVS 实例化失败时仍保证云端可用
            TryAddEngine(entries, "tencent", language);
            return entries;
        }

        /// <summary>
        /// 实例化指定引擎并加入链；构造失败（缺 native dll、GPU 探测崩溃等）则记录并跳过，
        /// 不让单个引擎的初始化异常拖垮整条链 —— 保证可用引擎（如 Tencent）仍能兜底。
        /// </summary>
        private void TryAddEngine(List<EngineEntry> entries, string poolKey, string language)
        {
            try
            {
                IASREngine engine = _pool[poolKey].Value; // 首次访问触发实例化，可能抛
                entries.Add(new EngineEntry(engine, language));
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis(
                    $"[ASR] 引擎 \"{poolKey}\" 实例化失败，已从引擎链剔除（降级其它引擎兜底）: {ex.GetType().Name} - {ex.Message}");
            }
        }

        /// <summary>
        /// 按后端返回的有序引擎标识列表构建 EngineEntry，保留列表顺序作为优先级。
        /// 遇到未知标识跳过并记日志；若全部跳过则回退默认路由。
        /// </summary>
        private List<EngineEntry> BuildEntriesFromList(string language, IReadOnlyList<string> engines)
        {
            var entries = new List<EngineEntry>();
            foreach (string engineId in engines)
            {
                if (EngineIdToPoolKey.TryGetValue(engineId, out string poolKey))
                    TryAddEngine(entries, poolKey, language);
                else
                    FileUtils.LogAnalysis($"[ASR] 未知引擎标识 \"{engineId}\", 已跳过");
            }

            return entries.Count > 0 ? entries : BuildDefaultEntries(language);
        }

        /// <summary>Direct access to pooled engines for testing.</summary>
        public IASREngine this[string key] =>
            _pool.TryGetValue(key, out var engine) ? engine.Value : null;
    }
}
