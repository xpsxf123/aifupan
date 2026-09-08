using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.anchor
{
    public class AnchorListBo
    {
        /// <summary>
        /// 当前页
        /// </summary>
        public int? pageIndex {  get; set; }
        /// <summary>
        /// 分页大小
        /// </summary>
        public int? pageSize { get; set; }
        /// <summary>
        /// 主播名称
        /// </summary>
        public string anchorName { get; set; }
        /// <summary>
        /// 录制状态 0未开始录播，1正在录播 2手动停止录播，3录播完成
        /// </summary>
        public int? recordStatus { get; set; }
        /// <summary>
        /// 是否从录制列表移除了  0：未移除 1：已移除
        /// </summary>
        public int? isRemoveRecord { get; set; }
        /// <summary>
        /// 行业id
        /// </summary>
        public string tradeId { get; set; }
        /// <summary>
        /// 账号归属类型 0：自有账号 1：同行账号
        /// </summary>
        public int? accountType { get; set; }

    }
}
