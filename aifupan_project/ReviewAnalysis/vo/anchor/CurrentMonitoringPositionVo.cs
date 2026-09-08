using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.anchor
{
    public class CurrentMonitoringPositionVo
    {
        /// <summary>
        /// id
        /// </summary>
        public long? id {  get; set; }
        /// <summary>
        /// 用户id
        /// </summary>
        public long? userId { get; set; }
        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId { get; set; }
        /// <summary>
        /// 主播
        /// </summary>
        public string anchorUrlSecUid { get; set; }
        /// <summary>
        /// 弹幕监控位
        /// </summary>
        public int? isBarrageMonitoring { get; set; }
        /// <summary>
        /// 数据看板监控位
        /// </summary>
        public int? isDataViewing { get; set; }
    }
}
