using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.Asr
{
    public class RecordNeedsWordVo
    {
        /// <summary>
        /// 记录要标注的词语位置，比如[0, 2]则表示标注当前段落第0个、第2个词语
        /// </summary>
        [JsonProperty("recordNeedsNum")]
        public int RecordNeedsNum { get; set; }

        /// <summary>
        /// 匹配中的限定词
        /// </summary>
        [JsonProperty("restrictWord")]
        public string RestrictWord { get; set; }

        /// <summary>
        /// 限定词范围
        /// </summary>
        [JsonProperty("restrictRange")]
        public int RestrictRange { get; set; }


    }
}
