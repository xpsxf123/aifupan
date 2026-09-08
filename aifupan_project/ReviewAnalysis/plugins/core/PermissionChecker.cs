using System;
using System.Threading.Tasks;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using douyin.Utils;

namespace ReviewAnalysis.plugins.core
{
    /// <summary>
    /// 权限检查器
    /// 检查是否允许采集主播后台数据
    /// 条件：有企业后台 AND 主播在线 AND 抖音平台 AND 有排班
    /// </summary>
    public static class PermissionChecker
    {
        /// <summary>
        /// 检查是否允许采集数据
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <returns>true=允许采集，false=不允许</returns>
        public static bool CanCollectData(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null)
            {
                FileUtils.LogRpa("权限检查失败：主播信息为空", "权限检查");
                return false;
            }

            // 1. 检查是否有企业后台（授权状态为1表示已授权）
            if (!HasEnterpriseBackend(anchorInfo))
            {
                FileUtils.LogRpa($"主播【{anchorInfo.AnchorName}】无企业后台授权，不允许采集", "权限检查");
                return false;
            }

            // 2. 检查主播是否在线（LiveStatus=2表示直播中）
            if (!IsAnchorOnline(anchorInfo))
            {
                FileUtils.LogRpa($"主播【{anchorInfo.AnchorName}】不在线，不允许采集", "权限检查");
                return false;
            }

            // 3. 检查是否是抖音平台
            if (!IsDouyinPlatform(anchorInfo))
            {
                FileUtils.LogRpa($"主播【{anchorInfo.AnchorName}】非抖音平台，不允许采集", "权限检查");
                return false;
            }

            // 4. 检查是否有排班
            if (!HasSchedule(anchorInfo))
            {
                FileUtils.LogRpa($"主播【{anchorInfo.AnchorName}】今天无排班，不允许采集", "权限检查");
                return false;
            }

            FileUtils.LogRpa($"主播【{anchorInfo.AnchorName}】权限检查通过，允许采集", "权限检查");
            return true;
        }

        /// <summary>
        /// 检查是否允许采集数据（异步版本）
        /// </summary>
        public static async Task<bool> CanCollectDataAsync(AnchorInfo anchorInfo)
        {
            return await Task.Run(() => CanCollectData(anchorInfo));
        }

        /// <summary>
        /// 检查是否有企业后台
        /// 通过接口或本地状态检查
        /// </summary>
        public static bool HasEnterpriseBackend(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null) return false;

            // enterpriseAuthStatus = 1 表示已授权
            // enterpriseAuthStatus = 0 表示未授权
            // enterpriseAuthStatus = 4 表示授权过期
            return anchorInfo.enterpriseAuthStatus == 1;
        }

        /// <summary>
        /// 检查主播是否在线
        /// </summary>
        public static bool IsAnchorOnline(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null) return false;

            // LiveStatus = 2 表示直播中
            // LiveStatus = 4 表示未直播
            // LiveStatus = 0 表示未检测
            return anchorInfo.LiveStatus == 2;
        }

        /// <summary>
        /// 检查是否是抖音平台
        /// </summary>
        public static bool IsDouyinPlatform(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null) return false;

            // AnchorPlatform = "DouYinLive" 表示抖音平台
            return !string.IsNullOrEmpty(anchorInfo.AnchorPlatform) 
                   && anchorInfo.AnchorPlatform.Equals("DouYinLive", StringComparison.OrdinalIgnoreCase);
        }

        /// <summary>
        /// 检查主播今天是否有排班
        /// </summary>
        public static bool HasSchedule(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid)) return false;

            return AnchorScheduleCacheManager.HasScheduleToday(anchorInfo.SecUid);
        }

        /// <summary>
        /// 检查主播今天是否有排班（重载）
        /// </summary>
        public static bool HasSchedule(string secUid)
        {
            if (string.IsNullOrEmpty(secUid)) return false;

            return AnchorScheduleCacheManager.HasScheduleToday(secUid);
        }

        /// <summary>
        /// 获取权限检查结果描述
        /// </summary>
        public static string GetCheckResultDescription(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null) return "主播信息为空";

            var results = new System.Collections.Generic.List<string>();

            if (!HasEnterpriseBackend(anchorInfo))
                results.Add("无企业后台授权");

            if (!IsAnchorOnline(anchorInfo))
                results.Add("主播不在线");

            if (!IsDouyinPlatform(anchorInfo))
                results.Add($"非抖音平台({anchorInfo.AnchorPlatform})");

            if (!HasSchedule(anchorInfo))
                results.Add("今天无排班");

            if (results.Count == 0)
                return "权限检查通过";

            return string.Join("，", results);
        }
    }
}
