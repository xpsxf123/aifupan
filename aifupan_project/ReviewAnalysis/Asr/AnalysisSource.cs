namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// 录播分析的触发来源。用于在 ASR 入口区分不同来源以差异化引擎路由。
    /// 目前仅 ManualRetry 需要特殊处理（强制走 Tencent），其余来源走默认路由。
    /// </summary>
    public enum AnalysisSource
    {
        /// <summary>自动分析队列 / 手动首次分析：走默认引擎路由（SVS → Tencent 兜底）。</summary>
        Auto,

        /// <summary>手动「重新分析」(/reanalysis)：强制只走 Tencent，失败即失败。</summary>
        ManualRetry
    }
}
