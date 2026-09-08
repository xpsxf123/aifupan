using System;
using System.Threading.Tasks;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.plugins.dataPullers;
using ReviewAnalysis.qianchuan;
using ReviewAnalysis.Model;
using douyin.Utils;

namespace ReviewAnalysis.plugins.collectors
{
    /// <summary>
    /// 千川数据采集器（简化版）
    /// 职责单一：只负责调用API获取数据，返回原始JSON
    /// </summary>
    public class QianchuanCollectorSimple : IPlatformCollector
    {
        public string PlatformId => "qianchuan";
        public string PlatformName => "千川";

        private readonly QianchuanDataPuller _dataPuller;

        public QianchuanCollectorSimple()
        {
            _dataPuller = new QianchuanDataPuller();
        }

        /// <summary>
        /// 检查是否有采集条件
        /// 包括：千川授权状态 + Cookie有效性
        /// </summary>
        public bool CanCollect(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null) return false;

            // 1. 检查千川授权状态
            //if (anchorInfo.qianchuanAuthStatus != (int)QianchuanAuthStatusEnum.auth)
            //{
            //    FileUtils.LogRpa($"千川采集跳过：主播未授权，qianchuanAuthStatus={anchorInfo.qianchuanAuthStatus}", "千川采集器");
            //    return false;
            //}

            // 2. 检查本地Cookie有效性
            int cookieStatus = QianchuanUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
            if (cookieStatus != (int)QianchuanAuthStatusEnum.auth)
            {
                FileUtils.LogRpa($"千川采集跳过：Cookie无效或过期，cookieStatus={cookieStatus}", "千川采集器");
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
                    FileUtils.LogRpa($"千川采集失败：roomId为空，主播【{anchorInfo?.AnchorName}】", "千川采集器");
                    return null;
                }

                FileUtils.LogRpa($"开始采集千川数据，主播【{anchorInfo?.AnchorName}】，roomId={roomId}", "千川采集器");

                // 使用传入的 anchorInfo
                var json = await _dataPuller.PullDataAsync(anchorInfo, roomId, videoId);

                return json;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"千川采集异常：{ex.Message}，主播【{anchorInfo?.AnchorName}】", "千川采集器");
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
                    var cookies = QianchuanDataHandle.GetCookiesFromLocal(secUid);
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
