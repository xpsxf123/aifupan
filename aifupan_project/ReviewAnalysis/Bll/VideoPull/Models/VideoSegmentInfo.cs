namespace ReviewAnalysis.Bll.VideoPull.Models
{
    /// <summary>
    /// 视频分段信息，表示一场直播中的一个时间段
    /// </summary>
    public class VideoSegmentInfo
    {
        /// <summary>分段序号（从 0 开始）</summary>
        public int SegmentIndex { get; set; }

        /// <summary>分段开始时间，格式 "yyyy-MM-dd HH:mm:ss"</summary>
        public string StartTime { get; set; }

        /// <summary>分段结束时间，格式 "yyyy-MM-dd HH:mm:ss"</summary>
        public string EndTime { get; set; }

        /// <summary>分段时长（秒）</summary>
        public long DurationSec { get; set; }

        /// <summary>分段相对于整场直播开始时间的偏移（秒）</summary>
        public long StartOffsetSec { get; set; }
    }
}
