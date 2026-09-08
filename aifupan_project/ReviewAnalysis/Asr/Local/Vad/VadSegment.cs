namespace ReviewAnalysis.Asr.Local.Vad
{
    /// <summary>
    /// VAD 检测到的一段语音活动区间。时间以毫秒为单位，相对于音频起点。
    /// </summary>
    public class VadSegment
    {
        public long StartMs { get; set; }
        public long EndMs { get; set; }

        public long DurationMs => EndMs - StartMs;
    }
}
