using ReviewAnalysis.Asr;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.analysis
{
    public class WordsMarkParagraphBo
    {

        /// <summary>
        /// 类型 0：视频 1：文件
        /// </summary>
        public int type { get; set; }

        /// <summary>
        /// 视频id/文件id
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public string tradeId { get; set; }

        /// <summary>
        /// 平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书
        /// </summary>
        public int platformType { get; set; }

        /// <summary>
        /// 当前是第几段，从1开始
        /// </summary>
        public int currentSort { get; set; }

        /// <summary>
        /// 文字内容
        /// </summary>
        public string content { get; set; }

        /// <summary>
        /// 是否是最后一段 0：否 1：是
        /// </summary>
        public int isLast { get; set; }

        /// <summary>
        /// 词语列表
        /// </summary>
        public List<WordListItemBo> items { get; set; }

    }
}
