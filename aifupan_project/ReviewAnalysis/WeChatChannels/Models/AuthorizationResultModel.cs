using ReviewAnalysis.WeChatChannels.Common;

namespace ReviewAnalysis.WeChatChannels.Models
{
    /// <summary>
    /// 微信视频号授权结果
    /// </summary>
    public class AuthorizationResultModel
    {
        /// <summary>
        /// 爱复盘用户 Id
        /// </summary>
        public int UserChannelId { get; set; }

        /// <summary>
        /// 微信视频号后端接口授权唯一 Id
        /// </summary>
        /// <remarks>
        /// 多次授权跟多个不同账号授权唯一 Id 都不会变化。
        /// </remarks>
        public int AuthorizerInfoId { get; set; }

        /// <summary>
        /// 微信视频号账号信息
        /// </summary>
        public WeChatChannelsAccount AccountBody { get; set; }
    }
}
