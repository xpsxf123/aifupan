using ReviewAnalysis.plugins.models;

namespace ReviewAnalysis.plugins.interfaces
{
    /// <summary>
    /// 数据适配器接口
    /// 职责：将各平台原始JSON数据转换为统一数据模型
    /// </summary>
    public interface IDataAdapter
    {
        /// <summary>
        /// 平台ID
        /// </summary>
        string PlatformId { get; }

        /// <summary>
        /// 平台名称
        /// </summary>
        string PlatformName { get; }

        /// <summary>
        /// 将原始JSON适配为统一数据模型
        /// </summary>
        /// <param name="rawJson">原始JSON数据</param>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="roomId">直播间ID</param>
        /// <param name="videoId">视频ID（可选）</param>
        /// <returns>统一数据模型</returns>
        UnifiedDataModel Adapt(string rawJson, string secUid, string roomId, string videoId = null);
    }
}
