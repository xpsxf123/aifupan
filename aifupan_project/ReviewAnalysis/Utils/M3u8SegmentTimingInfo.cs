namespace ReviewAnalysis.Utils
{
    /// <summary>
    /// M3U8 分片及其时间信息，由 FetchAndParseM3u8Async 解析 #EXTINF 标签填充
    /// </summary>
    public class M3u8SegmentTimingInfo
    {
        /// <summary>分片序号（从 0 开始）</summary>
        public int Index { get; set; }

        /// <summary>分片 URL（绝对地址）</summary>
        public string Url { get; set; }

        /// <summary>本段时长（秒），来自 #EXTINF 标签</summary>
        public double DurationSec { get; set; }

        /// <summary>累计时长偏移（秒），即本段开始时间相对 M3U8 起点的偏移</summary>
        public double StartOffsetSec { get; set; }

        /// <summary>累计时长结束偏移（秒）</summary>
        public double EndOffsetSec { get; set; }
    }
}
