using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.Asr
{
    public class SyncVideoAnalysisBo
    {
        /// <summary>
        /// 视频uuid
        /// </summary>
        [JsonProperty("videoId")]
        public string VideoId {  get; set; }
        /// <summary>
        /// 行业id
        /// </summary>
        [JsonProperty("tradeId")]
        public string TradeId { get; set; }
        /// <summary>
        /// 分析数据
        /// </summary>
        [JsonProperty("sentenceMarkVoList")]
        public List<SentenceMarkVo> SentenceMarkVoList { get; set; }
    }
}
