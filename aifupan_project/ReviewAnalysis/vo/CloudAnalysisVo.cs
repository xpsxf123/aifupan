using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Model;
using ReviewAnalysis.vo.barrage;
using ReviewAnalysis.vo.oceanEngineData;

namespace ReviewAnalysis.vo
{
    public class CloudAnalysisVo
    {
        /// <summary>
        /// 主播信息
        /// </summary>
        [JsonProperty("anchorInfo")]
        public AnchorInfoVo AnchorInfo { get; set; }

        /// <summary>
        /// 视频信息
        /// </summary>
        [JsonProperty("videoInfo")]
        public AnchorVideoInfoVo VideoInfo { get; set; }

        /// <summary>
        /// 视频/音频播放地址
        /// </summary>
        [JsonProperty("playUrl")]
        public string PlayUrl { get; set; }

        /// <summary>
        /// 段落词语信息列表
        /// </summary>
        [JsonProperty("analysisList")]
        public List<OnlineAnalysisItemVo> AnalysisList { get; set; }

        /// <summary>
        /// 在线人数列表
        /// </summary>
        [JsonProperty("onlineNumList")]
        public List<OnlineNumVo> OnlineNumList { get; set; }

        /// <summary>
        /// 弹幕标注
        /// </summary>
        public List<BarrageData> barrageDataList { get; set; }

        /// <summary>
        /// 总弹幕数量
        /// </summary>
        public int? totalBarrageNum { get; set; }

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
