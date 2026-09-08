using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.vo
{
    public class CloudContrastAnalysisVo
    {
        /// <summary>
        /// 对比数据1
        /// </summary>
        [JsonProperty("sentenceMark1")]
        public CloudAnalysisVo SentenceMark1 { get; set; }
        /// <summary>
        /// 对比数据2
        /// </summary>
        [JsonProperty("sentenceMark2")]
        public CloudAnalysisVo SentenceMark2 { get; set; }
    }
}
