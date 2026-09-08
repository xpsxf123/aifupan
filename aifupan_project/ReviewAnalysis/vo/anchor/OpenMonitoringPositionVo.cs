using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.anchor
{
    public class OpenMonitoringPositionVo
    {

        /// <summary>
        /// 是否开启弹幕监控 0否， 1是
        /// </summary>
        public int? isBarrageMonitoring {  get; set; }

        /// <summary>
        /// 是否开启数据看板 0：否 1：是
        /// </summary>
        public int? isDataViewing { get; set; }


    }
}
