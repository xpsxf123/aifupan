using Newtonsoft.Json;

namespace ReviewAnalysis.WeChatChannels.Common
{
    /// <summary>
    /// 微信视频号返回的原始数据
    /// </summary>
    public class WeChatChannelsAccount
    {
        public string NickName { get; set; }

        public string HeadImg { get; set; }

        public int ServiceTypeInfo { get; set; }

        public int VerifyTypeInfo { get; set; }

        public string UserName { get; set; }

        public string PrincipalName { get; set; }

        public string QRCodeUrl { get; set; }

        public WeChatChannelsBusinessInfo BusinessInfo { get; set; }

        public int AccountStatus { get; set; }

        [JsonProperty("Channels_info")]
        public int ChannelsInfo { get; set; }
    }
}
