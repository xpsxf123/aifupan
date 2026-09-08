using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.Asr
{
    public class SentenceMarkVo
    {
        /// <summary>
        /// 视频id
        /// </summary>
        [JsonProperty("videoId")]
        public string VideoId { get; set; }

        /// <summary>
        /// 当前是第几段，从1开始
        /// </summary>
        [JsonProperty("currentSort")]
        public int CurrentSort { get; set; }

        /// <summary>
        /// 文字内容
        /// </summary>
        [JsonProperty("content")]
        public string Content { get; set; }

        /// <summary>
        /// 词语列表
        /// </summary>
        [JsonProperty("items")]
        public List<WordListItemVo> Items { get; set; }

        /// <summary>
        /// 关键词/敏感词列表
        /// </summary>
        [JsonProperty("wordsList")]
        public List<WordsMarkVo> WordsList { get; set; }
    }
}
