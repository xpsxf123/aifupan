using ReviewAnalysis.juliang;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.WeChatChannels.Services;
using System.Collections.Generic;

namespace ReviewAnalysis.Bll
{
    /// <summary>
    /// 数据看板相关的业务逻辑
    /// </summary>
    public static class DashboardBll
    {
        /// <summary>
        /// 获得数据看板的实时数据列表
        /// </summary>
        /// <param name="platform">平台类型 0：全平台 1：抖音 2：快手 3：视频号</param>
        public static List<JuliangRealTimeDataEntity> GetDashboardRealTimeDataList(string platform, string videoId)
        {
            switch (platform)
            {
                case "1":
                    return JuliangDataHandle.getJuliangRealTimeList(videoId);

                case "3":
                    return WeChatChannelsDashboardService.GetDashboardRealTimeDataList(videoId);
            }

            return new List<JuliangRealTimeDataEntity>();
        }
    }
}
