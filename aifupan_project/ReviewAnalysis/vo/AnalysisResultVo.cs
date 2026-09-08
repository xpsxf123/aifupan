using ReviewAnalysis.Asr;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo
{
    public class AnalysisResultVo
    {
        /// <summary>
        /// 段落分析信息
        /// </summary>
        public List<SentenceMarkVo> sentenceMarkVos { get; set; }

        /// <summary>
        /// 分析结果的关键词类型信息
        /// </summary>
        public List<AnalysisResultCruxTypeVo> cruxTypeList { get; set; }

        /// <summary>
        /// 词语汇总列表
        /// </summary>
        public List<WordsMarkVo> wordsCollect { get; set; }

        /// <summary>
        /// 词语tab列表
        /// </summary>
        public List<WordsTabVo> wordsTabList { get; set; }
    }
}
