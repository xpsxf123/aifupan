using ReviewAnalysis.Asr;
using ReviewAnalysis.Model;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.barrage;
using ReviewAnalysis.vo.oceanEngineData;
using ReviewAnalysis.vo.trade;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class SentenceMarkDto
    {
        /// <summary>
        /// 主播信息
        /// </summary>
        public AnchorInfo anchorInfo {  get; set; }
        /// <summary>
        /// 视频信息
        /// </summary>
        public AnchorVideo videoInfo { get; set; }
        /// <summary>
        /// 文件信息
        /// </summary>
        public UploadFile uploadFile { get; set; }
        /// <summary>
        /// 音频/视播放地址
        /// </summary>
        public string playUrl { get; set; }
        /// <summary>
        /// 段落词语信息-录制的视频
        /// </summary>
        public List<AudioaAlysis> audioaAlyses { get; set; }
        /// <summary>
        /// 段落词语信息-上传的文件
        /// </summary>
        public List<UploadFileAlysis> fileAudioaAlyses { get; set; }
        /// <summary>
        /// 在线人数列表
        /// </summary>
        public List<OnlineNum> onlineNumList { get; set; }

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

        /// <summary>
        /// 互动率
        /// </summary>
        public double? interactionPercent { get; set; }

        /// <summary>
        /// 销量
        /// </summary>
        public int? purchaseCountStart { get; set; }

        /// <summary>
        /// 销量
        /// </summary>
        public int? purchaseCountEnd { get; set; }

        /// <summary>
        /// 销量
        /// </summary>
        public int? purchaseCount { get; set; }

        /// <summary>
        /// 总观看人次
        /// </summary>
        public int? totalWatchNum { get; set; }

        /// <summary>
        /// 数据来源类型 0：蝉妈妈 1：巨量百应
        /// </summary>
        public int? dataSourceType { get; set; }

        /// <summary>
        /// 是否已推荐行业 0未推荐，1已推荐
        /// </summary>
        public int? suggestTrade { get; set; }

        /// <summary>
        /// uv价值区间范围-起始 
        /// </summary>
        public double? uvValueStart { get; set; }

        /// <summary>
        /// uv价值区间范围-结束 
        /// </summary>
        public double? uvValueEnd { get; set; }

        /// <summary>
        /// 销售额-起始 
        /// </summary>
        public double? volumeStart { get; set; }

        /// <summary>
        /// 销售额-结束 
        /// </summary>
        public double? volumeEnd { get; set; }

        /// <summary>
        /// 巨量数据列表
        /// </summary>
        public List<OceanEngineDataDto> juLiangDataList { get; set; }

        /// <summary>
        /// 视频行业信息
        /// </summary>
        public TradeVo tradeInfo { get; set; }

        /// <summary>
        /// 投放消耗金额列表（元）
        /// </summary>
        public List<BarrageDoubleData> qianchuanCostDataList { get; set; }

        /// <summary>
        /// 投放消耗总金额（元）
        /// </summary>
        public double? totalQianchuanCost { get; set; }

        /// <summary>
        /// 净成交ROI列表
        /// </summary>
        public List<BarrageDoubleData> netTransactionRoiDataList { get; set; }

        /// <summary>
        /// 净成交ROI
        /// </summary>
        public double? totalNetTransactionRoi { get; set; }
    }
}
