using ReviewAnalysis.Asr;
using ReviewAnalysis.Model;
using ReviewAnalysis.vo;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public class AnalysisResultTxtVo
    {
        /// <summary>
        /// 段落词语信息-录制的视频
        /// </summary>
        public List<AudioaAlysis> audioaAlyses { get; set; }

        /// <summary>
        /// 段落词语信息-上传的文件
        /// </summary>
        public List<UploadFileAlysis> fileAudioaAlyses { get; set; }

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
