using System;

namespace ReviewAnalysis.Model
{
    /// <summary>
    /// 主播排班实体
    /// </summary>
    public class AnchorSchedule
    {
        /// <summary>
        /// 排班ID（后台生成的唯一标识）
        /// </summary>
        public string scheduleId { get; set; }

        /// <summary>
        /// 主播SecUid
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 开始时间 格式：yyyy-MM-dd HH:mm:ss
        /// </summary>
        public string startTime { get; set; }

        /// <summary>
        /// 结束时间 格式：yyyy-MM-dd HH:mm:ss
        /// </summary>
        public string endTime { get; set; }
    }
}