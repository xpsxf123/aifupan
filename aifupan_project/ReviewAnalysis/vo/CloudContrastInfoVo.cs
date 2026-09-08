using Newtonsoft.Json.Converters;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.vo
{
    public class CloudContrastInfoVo
    {
        /// <summary>
        /// 用户id
        /// </summary>
        [JsonProperty("userId")]
        public long UserId { get; set; }

        /// <summary>
        /// 视频1的视频ID
        /// </summary>
        [JsonProperty("videoOneId")]
        public string VideoOneId { get; set; }

        /// <summary>
        /// 视频2的视频ID
        /// </summary>
        [JsonProperty("videoTwoId")]
        public string VideoTwoId { get; set; }

        /// <summary>
        /// 对比时间
        /// </summary>
        [JsonProperty("contrastTime")]
        public string ContrastTime { get; set; }

        /// <summary>
        /// 主播1的secUid
        /// </summary>
        [JsonProperty("anchorOneId")]
        public string AnchorOneId { get; set; }

        /// <summary>
        /// 主播2的secUid
        /// </summary>
        [JsonProperty("anchorTwoId")]
        public string AnchorTwoId { get; set; }

        /// <summary>
        /// 文件1的文件id
        /// </summary>
        [JsonProperty("fileOneId")]
        public string FileOneId { get; set; }

        /// <summary>
        /// 文件2的文件id
        /// </summary>
        [JsonProperty("fileTwoId")]
        public string FileTwoId { get; set; }

        /// <summary>
        /// 对比的唯一标识
        /// </summary>
        [JsonProperty("contrastId")]
        public string ContrastId { get; set; }

        /// <summary>
        /// 是否已分享 0：否 1：是
        /// </summary>
        [JsonProperty("isShard")]
        public int IsShard { get; set; }

        /// <summary>
        /// 在线复盘的url
        /// </summary>
        [JsonProperty("shareUrl")]
        public string ShareUrl { get; set; }

        /// <summary>
        /// 删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除
        /// </summary>
        [JsonProperty("deleteStatus")]
        public int DeleteStatus { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        [JsonProperty("tenantId")]
        public long TenantId { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        [JsonProperty("createDate")]
        public string CreateDate { get; set; }

        /// <summary>
        /// 最后修改时间
        /// </summary>
        [JsonProperty("updateDate")]
        public string UpdateDate { get; set; }
    }
}
