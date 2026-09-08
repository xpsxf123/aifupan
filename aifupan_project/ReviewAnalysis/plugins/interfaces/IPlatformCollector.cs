using System;
using System.Threading.Tasks;
using ReviewAnalysis.Model;

namespace ReviewAnalysis.plugins.interfaces
{
    /// <summary>
    /// 平台数据采集器接口（简化版）
    /// 职责单一：只负责从平台API获取数据，返回原始JSON
    /// 不负责数据解析、存储
    /// </summary>
    public interface IPlatformCollector
    {
        /// <summary>
        /// 平台ID（如：juliang, qianchuan, enterprise, life, anchorLive）
        /// </summary>
        string PlatformId { get; }

        /// <summary>
        /// 平台名称
        /// </summary>
        string PlatformName { get; }

        /// <summary>
        /// 检查是否有采集条件
        /// 包括：授权状态 + Cookie有效性
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <returns>true=可以采集，false=不满足条件</returns>
        bool CanCollect(AnchorInfo anchorInfo);

        /// <summary>
        /// 采集数据
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="roomId">直播间ID</param>
        /// <param name="videoId">视频ID（可选）</param>
        /// <returns>原始JSON字符串，失败返回null</returns>
        Task<string> CollectAsync(AnchorInfo anchorInfo, string roomId, string videoId = null);

        /// <summary>
        /// 检查Cookie是否有效
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>true=有效，false=无效</returns>
        Task<bool> CheckCookieAsync(string secUid);
    }

    /// <summary>
    /// 采集结果
    /// </summary>
    public class CollectResult
    {
        /// <summary>
        /// 是否成功
        /// </summary>
        public bool Success { get; set; }

        /// <summary>
        /// 原始JSON数据
        /// </summary>
        public string RawJson { get; set; }

        /// <summary>
        /// 错误消息
        /// </summary>
        public string ErrorMessage { get; set; }

        /// <summary>
        /// 采集时间
        /// </summary>
        public DateTime CollectTime { get; set; }

        /// <summary>
        /// 平台ID
        /// </summary>
        public string PlatformId { get; set; }

        /// <summary>
        /// 创建成功结果
        /// </summary>
        public static CollectResult Ok(string platformId, string json)
        {
            return new CollectResult
            {
                Success = true,
                RawJson = json,
                CollectTime = DateTime.Now,
                PlatformId = platformId
            };
        }

        /// <summary>
        /// 创建失败结果
        /// </summary>
        public static CollectResult Fail(string platformId, string error)
        {
            return new CollectResult
            {
                Success = false,
                ErrorMessage = error,
                CollectTime = DateTime.Now,
                PlatformId = platformId
            };
        }
    }
}
