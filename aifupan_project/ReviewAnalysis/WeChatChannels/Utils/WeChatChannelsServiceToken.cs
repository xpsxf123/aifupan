using System;

namespace ReviewAnalysis.WeChatChannels.Utils
{
    /// <summary>
    /// 微信视频号后端服务 Token
    /// </summary>
    public static class WeChatChannelsServiceToken
    {
        /// <summary>
        /// Token
        /// </summary>
        public static string Token { get; set; } = string.Empty;

        /// <summary>
        /// 密钥
        /// </summary>
        public static string Secret { get; set; }

        /// <summary>
        /// 密钥过期时间戳（秒）
        /// </summary>
        public static long SecretExpiryTime { get; set; }
    }
}
