using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.plugins.dataPullers;
using ReviewAnalysis.juliang;
using ReviewAnalysis.Model;
using ReviewAnalysis.enumeration.anchor;
using douyin.Utils;

namespace ReviewAnalysis.plugins.collectors
{
    /// <summary>
    /// 巨量数据采集器（简化版）
    /// 职责单一：只负责调用API获取数据，返回原始JSON
    /// </summary>
    public class JuliangedCollector : IPlatformCollector
    {
        public string PlatformId => "juliang";
        public string PlatformName => "巨量百应";

        private readonly JuliangedDataPuller _dataPuller;

        public JuliangedCollector()
        {
            _dataPuller = new JuliangedDataPuller();
        }

        /// <summary>
        /// 检查是否有采集条件
        /// 包括：巨量授权状态 + Cookie有效性
        /// </summary>
        public bool CanCollect(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null) return false;

            // 1. 检查巨量授权状态
            if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
            {
                FileUtils.LogRpa($"巨量采集跳过：主播未授权，juliangAuthStatus={anchorInfo.juliangAuthStatus}", "巨量采集器");
                return false;
            }

            // 2. 检查本地Cookie有效性
            int cookieStatus = JuliangUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
            if (cookieStatus != JuliangAuthStatusEnum.auth)
            {
                FileUtils.LogRpa($"巨量采集跳过：Cookie无效或过期，cookieStatus={cookieStatus}", "巨量采集器");
                return false;
            }

            return true;
        }

        /// <summary>
        /// 采集数据
        /// 使用JuliangedDataPuller拉取数据
        /// </summary>
        public async Task<string> CollectAsync(AnchorInfo anchorInfo, string roomId, string videoId = null)
        {
            try
            {
                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa($"巨量采集失败：roomId为空，主播【{anchorInfo?.AnchorName}】", "巨量采集器");
                    return null;
                }

                FileUtils.LogRpa($"开始采集巨量数据，主播【{anchorInfo?.AnchorName}】，roomId={roomId}, videoId={videoId}", "巨量采集器");

                // 使用传入的 anchorInfo
                var json = await _dataPuller.PullDataAsync(anchorInfo, roomId, videoId);

                return json;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"巨量采集异常：{ex.Message}，主播【{anchorInfo?.AnchorName}】", "巨量采集器");
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
                    var cookies = _dataPuller.GetType()
                        .GetMethod("GetCookies", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance)
                        ?.Invoke(_dataPuller, new object[] { secUid }) as Dictionary<string, string>;
                    return cookies != null && cookies.Count > 0 && cookies.ContainsKey("COMPASS_LUOPAN_DT");
                }
                catch
                {
                    return false;
                }
            });
        }
    }
}
