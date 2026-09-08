using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class UploadFilePageQueryDto
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
        /// 分析状态 analysis_status -1：全部 0：未分析 1：分析中 2：分析完成 3：分析失败
        /// </summary>
        public int AnalysisStatus { get; set; }

        /// <summary>
        /// 文件名
        /// </summary>
        public string FileName { get; set; }

        /// <summary>
        /// 分析时间范围-开始
        /// </summary>
        public string AnalysisStartDate { get; set; }

        /// <summary>
        /// 分析时间范围-结束
        /// </summary>
        public string AnalysisEndDate { get; set; }

        /// <summary>
        /// 上传时间范围-开始
        /// </summary>
        public string UpdateStartDate { get; set; }

        /// <summary>
        /// 上传时间范围-结束
        /// </summary>
        public string UpdateEndDate { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public string UserId { get; set; }

    }
}
