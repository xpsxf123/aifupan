using ReviewAnalysis.WeChatChannels.Common;
using System;

namespace ReviewAnalysis.WeChatChannels.Models
{
    public class LiveStatusModel
    {
        public int AuthorizerInfoId { get; set; }

        /// <summary>
        /// 直播状态, 0未开播 1 已开播未截流 3 已截流 4 直播结束
        /// </summary>
        public int LiveStatus { get; set; }

        public bool OpenLive { get; set; }

        public string RecentlyExportId { get; set; }

        public DateTime RecentlyLiveTime { get; set; }

        public DateTime NextMonitorTime { get; set; }

        public WeChatChannelsLiveStream Live { get; set; }
    }
}
