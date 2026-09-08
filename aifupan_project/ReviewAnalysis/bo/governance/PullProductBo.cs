using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.governance
{
    /// <summary>
    /// 拉取商品数据请求参数（统一采集上传，同时上报 fupan-governance-server 和 fupan-server）
    /// </summary>
    public class PullProductBo
    {
        /// <summary>
        /// 主播SecUid
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 直播批次号
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 视频唯一标识（fupan-server 上传必须）
        /// </summary>
        public string videoId { get; set; }
    }
}
