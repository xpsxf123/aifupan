using System;

namespace ReviewAnalysis.WeChatChannels.Models
{
    /// <summary>
    /// 在线直播心跳模型
    /// </summary>
    public class LiveHeartbeatModel
    {
        /// <summary>
        /// 直播 Id
        /// </summary>
        public string ExportId { get; set; }

        /// <summary>
        /// 是否已结束
        /// </summary>
        public bool Finish { get; set; }

        /// <summary>
        /// 结束事件
        /// </summary>
        public DateTime EndTime { get; set; }
    }
}
