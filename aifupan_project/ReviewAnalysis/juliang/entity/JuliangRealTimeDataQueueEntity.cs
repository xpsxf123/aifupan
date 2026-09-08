using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang.entity
{
    public class JuliangRealTimeDataQueueEntity
    {

        /// <summary>
        /// 直播场次号
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 直播的唯一标识
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 巨量实时数据
        /// </summary>
        public JuliangRealTimeDataEntity juliangRealTimeData { get; set; }
    }
}
