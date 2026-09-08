using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.socketCollectMessage
{
    /// <summary>
    /// 直播消息收集视图对象 
    /// 对应Java类: SocketCollectMessageVo
    /// </summary>
    public class SocketCollectMessageVo
    {
        /// <summary>
        /// ID
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 用户id 
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId { get; set; }

        /// <summary>
        /// 主播secUid 
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 直播场次号 
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 视频唯一标识 
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 文件地址
        /// </summary>
        public string fileAddress { get; set; }

        /// <summary>
        /// cos文件的key 
        /// </summary>
        public string cosKey { get; set; }

        /// <summary>
        /// 开始时间
        /// </summary>
        public string startDate { get; set; }

        /// <summary>
        /// 结束时间 
        /// </summary>
        public string endDate { get; set; }

        /// <summary>
        /// 累计观看人数 
        /// </summary>
        public string totalOnlineNum { get; set; }

        /// <summary>
        /// 场观人数
        /// </summary>
        public string observationNum { get; set; }

        /// <summary>
        /// 弹幕总数
        /// </summary>
        public int? totalBarrageNum { get; set; }

        /// <summary>
        /// 最高在线人数
        /// </summary>
        public int? onlineMaxNum { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate { get; set; }

        /// <summary>
        /// 最后修改时间
        /// </summary>
        public string updateDate { get; set; }
    }
}
