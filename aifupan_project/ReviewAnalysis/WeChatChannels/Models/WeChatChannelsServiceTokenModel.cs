namespace ReviewAnalysis.WeChatChannels.Models
{
    /// <summary>
    /// 微信视频号后端服务 Token 模型
    /// </summary>
    public class WeChatChannelsServiceTokenModel
    {
        public string AccessToken { get; set; }

        public int ExpiresIn { get; set; }

        public string Secret { get; set; }
    }
}
