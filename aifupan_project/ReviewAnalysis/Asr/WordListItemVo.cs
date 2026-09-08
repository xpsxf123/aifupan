using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.Asr
{
    public class WordListItemVo
    {
        /// <summary>
        /// 词语
        /// </summary>
        [JsonProperty("word")]
        public string Word { get; set; }

        /// <summary>
        /// 开始时间，毫秒
        /// </summary>
        [JsonProperty("startTime")]
        public long StartTime { get; set; }

        /// <summary>
        /// 结束时间，毫秒
        /// </summary>
        [JsonProperty("endTime")]
        public long EndTime { get; set; }
    }
}
