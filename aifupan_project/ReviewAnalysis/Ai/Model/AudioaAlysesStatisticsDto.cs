using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.Model
{
    public class AudioaAlysesStatisticsDto
    {

        /// <summary>
        /// 视频开始时间 从0开始
        /// </summary>
        public long startTempTime {  get; set; }

        /// <summary>
        /// 视频结束时间 从0开始
        /// </summary>
        public long endTempTime { get; set; }

        /// <summary>
        /// 视频结束时间 从0开始
        /// </summary>
        public long endTempTimeTwo { get; set; }

        /// <summary>
        /// 视频开始时间 自然时间戳
        /// </summary>
        public long startVideoTime { get; set; }

        /// <summary>
        /// 视频结束时间 自然时间戳
        /// </summary>
        public long endVideoTime { get; set; }

        /// <summary>
        /// 视频的自然开始时间
        /// </summary>
        public DateTime? startTime { get; set; }

        /// <summary>
        /// 视频的自然结束时间
        /// </summary>
        public DateTime? endTime { get; set; }

        /// <summary>
        /// 视频的开始时间 HH:mm:ss
        /// </summary>
        public string textStart { get; set; }

        /// <summary>
        /// 视频的结束时间 HH:mm:ss
        /// </summary>
        public string textEnd { get; set; }

        /// <summary>
        /// 一段的在线人数
        /// </summary>
        public string renShu { get; set; }

        /// <summary>
        /// 一段的内容
        /// </summary>
        public string content {  get; set; }

    }
}
