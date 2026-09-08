namespace ReviewAnalysis.Asr.Local.Vad
{
    /// <summary>
    /// 语音活动检测引擎接口。实现类负责加载 ONNX 模型并对 PCM 音频做语音段检测。
    /// 注：本接口仅用于切片位置决策，不允许下游基于检测结果做内容过滤
    /// （per [BGM 保留策略](.agents/llm_wiki/wiki/preferences/wal/20260526_asr_bgm_preservation_rules.md)）。
    /// </summary>
    public interface IVadEngine
    {
        /// <summary>模型是否已下载并就绪。未就绪时调用方应走降级路径。</summary>
        bool IsReady { get; }

        /// <summary>
        /// 对单声道 16kHz PCM 做语音活动检测，返回语音段（起止 ms）。
        /// 失败/无语音时返回空数组（不抛异常），由调用方决定降级策略。
        /// </summary>
        /// <param name="pcm16kMono">16kHz mono float PCM 数据</param>
        /// <returns>语音段数组（按起始时间排序）；非语音区间是相邻段之间的 gap</returns>
        VadSegment[] GetSegments(float[] pcm16kMono);
    }
}
