using Newtonsoft.Json;

namespace ReviewAnalysis.WeChatChannels.Common
{
    public class WeChatChannelsBusinessInfo
    {
        [JsonProperty("open_pay")]
        public int OpenPay { get; set; }

        [JsonProperty("open_shake")]
        public int OpenShake { get; set; }

        [JsonProperty("open_scan")]
        public int OpenScan { get; set; }

        [JsonProperty("open_card")]
        public int OpenCard { get; set; }

        [JsonProperty("open_store")]
        public int OpenStore { get; set; }
    }
}
