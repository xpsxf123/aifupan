using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class AnchorVideoPageQueryDto
    {
        /// <summary>
        /// PageIndex
        /// </summary>
        public int PageIndex { get; set; }

        /// <summary>
        /// PageSize
        /// </summary>
        public int PageSize { get; set; }

        /// <summary>
        /// 主播Id
        /// </summary>
        public int AnchorId { get; set; }

        /// <summary>
        /// 主播secId
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 分析状态 analysis_status -1：全部 0：未分析 1：分析中 2：分析完成 3：分析失败
        /// </summary>
        public int AnalysisStatus { get; set; }

        /// <summary>
        /// 分析时间范围-开始
        /// </summary>
        public string AnalysisStartDate { get; set; }

        /// <summary>
        /// 分析时间范围-结束
        /// </summary>
        public string AnalysisEndDate { get; set; }

        /// <summary>
        /// 录制时间范围-开始
        /// </summary>
        public string RecordStartDate { get; set; }

        /// <summary>
        /// 录制时间范围-结束
        /// </summary>
        public string RecordEndDate { get; set; }

        /// <summary>
        /// 用户Id
        /// </summary>
        public string UserId { get; set; }
        /// <summary>
        /// 分享状态  1：已分享
        /// </summary>
        public int IsShare { get; set; }
        /// <summary>
        /// 删除状态 -1：不限 0：未删除 1：已从复盘列表删除 2：已从复盘列表和云空间删除
        /// </summary>
        public int DeleteStatus { get; set; }

    }
}
