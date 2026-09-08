using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class VideoContrastPageQueryDto
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
        /// 主播SecUid
        /// </summary>
        public string AnchorId { get; set; }

        /// <summary>
        /// 对比时间范围-开始
        /// </summary>
        public string ContrastStartDate { get; set; }

        /// <summary>
        /// 对比时间范围-结束
        /// </summary>
        public string ContrastEndDate { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public string UserId { get; set; }
        /// <summary>
        /// 租户id
        /// </summary>
        public long TenantId { get; set; }

        /// <summary>
        /// 已分享
        /// </summary>
        public int IsShare { get; set; }

        /// <summary>
        /// 删除状态 -1：不限 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除
        /// </summary>
        public int DeleteStatus { get; set; }
    }
}
