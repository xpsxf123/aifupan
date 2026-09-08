using System.Collections.Generic;

namespace ReviewAnalysis.Bll.VideoPull.Models
{
    /// <summary>
    /// 巨量历史直播单场记录
    /// </summary>
    public class LiveSessionInfo
    {
        /// <summary>直播标题</summary>
        public string LiveRoom { get; set; }

        /// <summary>直播封面图URL</summary>
        public string CoverImgUri { get; set; }

        /// <summary>直播ID（19位数字字符串）</summary>
        public string LiveId { get; set; }

        /// <summary>开播时间, 如 "2026-07-08 16:33:00"</summary>
        public string StartTime { get; set; }

        /// <summary>结束播放时间, 如 "2026-07-08 17:54:05"</summary>
        public string EndTime { get; set; }

        /// <summary>视频时长（秒）</summary>
        public long DurationSec { get; set; }

        /// <summary>分段列表，非空时表示该场次已被拆分为多个时间段</summary>
        public List<VideoSegmentInfo> Segments { get; set; }
    }
}
