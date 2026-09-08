using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.Bll.Anchor.Entity
{
    /// <summary>
    /// 获取抖音主播是否在线返回的数据实体
    /// </summary>
    public class DouYinIsOnlieResultEntity
    {
        [JsonProperty("status_code")]
        public int StatusCode { get; set; }

        [JsonProperty("data")]
        public List<DataItem> Data { get; set; }

        [JsonProperty("extra")]
        public ExtraInfo Extra { get; set; }
    }

    public class DataItem
    {
        [JsonProperty("scene_id")]
        public int SceneId { get; set; }

        [JsonProperty("user_live")]
        public List<UserLive> UserLive { get; set; }
    }

    public class UserLive
    {
        [JsonProperty("room_id")]
        public long RoomId { get; set; }
        [JsonProperty("user_id")]
        public long UserId { get; set; }
        [JsonProperty("live_status")]
        public long LiveStatus { get; set; }
    }

    public class ExtraInfo
    {
        [JsonProperty("now")]
        public long Now { get; set; }
    }
}
