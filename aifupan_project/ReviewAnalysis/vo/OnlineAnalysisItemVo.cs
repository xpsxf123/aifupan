using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.vo
{
    public class OnlineAnalysisItemVo
    {
        /// <summary>
        /// ID
        /// </summary>
        [JsonProperty("id")]
        public long Id { get; set; }

        /// <summary>
        /// 文件/视频唯一标识uuid
        /// </summary>
        [JsonProperty("fileUuid")]
        public string FileUuid { get; set; }

        /// <summary>
        /// 当前段落 从1开始
        /// </summary>
        [JsonProperty("paragraph")]
        public int Paragraph { get; set; }

        /// <summary>
        /// 识别状态 0：成功 1：失败
        /// </summary>
        [JsonProperty("status")]
        public int Status { get; set; }

        /// <summary>
        /// 词语json字符串内容
        /// </summary>
        [JsonProperty("dataJson")]
        public string DataJson { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        [JsonProperty("tradeId")]
        public long TradeId { get; set; }
    }
}
