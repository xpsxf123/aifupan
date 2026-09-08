using System;
using System.Threading.Tasks;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.plugins.dataPullers;
using ReviewAnalysis.anchorLive;
using ReviewAnalysis.Model;
using douyin.Utils;

namespace ReviewAnalysis.plugins.collectors
{
    /// <summary>
    /// 主播后台数据采集器（简化版）
    /// 职责单一：只负责调用API获取数据，返回原始JSON
    /// </summary>
    public class AnchorLiveCollectorSimple : IPlatformCollector
    {
        public string PlatformId => "anchorLive";
        public string PlatformName => "主播后台";

        private readonly AnchorLiveDataPuller _dataPuller;

        public AnchorLiveCollectorSimple()
        {
            _dataPuller = new AnchorLiveDataPuller();
        }

        /// <summary>
        /// 检查是否有采集条件
        /// 包括：主播后台授权状态 + Cookie有效性
        /// </summary>
        public bool CanCollect(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null) return false;

            // 1. 检查主播后台授权状态（暂时注释，后续恢复）
            // TODO: 恢复授权状态检查
            // if (anchorInfo.anchorLiveAuthStatus != (int)AnchorLiveAuthStatusEnum.auth)
            // {
            //     FileUtils.LogRpa($"主播后台采集跳过：主播未授权，anchorLiveAuthStatus={anchorInfo.anchorLiveAuthStatus}", "主播后台采集器");
            //     return false;
            // }

            // 2. 检查本地Cookie有效性
            int cookieStatus = AnchorLiveUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
            if (cookieStatus != (int)AnchorLiveAuthStatusEnum.auth)
            {
                FileUtils.LogRpa($"主播后台采集跳过：Cookie无效或过期，cookieStatus={cookieStatus}", "主播后台采集器");
                return false;
            }

            return true;
        }

        /// <summary>
        /// 采集数据
        /// </summary>
        public async Task<string> CollectAsync(AnchorInfo anchorInfo, string roomId, string videoId = null)
        {
            try
            {
                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa($"主播后台采集失败：roomId为空，主播【{anchorInfo?.AnchorName}】", "主播后台采集器");
                    return null;
                }

                FileUtils.LogRpa($"开始采集主播后台数据，主播【{anchorInfo?.AnchorName}】，roomId={roomId}", "主播后台采集器");

                var json = await _dataPuller.PullDataAsync(anchorInfo, roomId, videoId);

                return json;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"主播后台采集异常：{ex.Message}，主播【{anchorInfo?.AnchorName}】", "主播后台采集器");
                return null;
            }
        }

        /// <summary>
        /// 检查Cookie是否有效
        /// </summary>
        public async Task<bool> CheckCookieAsync(string secUid)
        {
            return await Task.Run(() =>
            {
                try
                {
                    var cookies = AnchorLiveDataHandle.GetCookiesFromLocal(secUid);
                    return cookies != null && cookies.Count > 0;
                }
                catch
                {
                    return false;
                }
            });
        }
    }
}
