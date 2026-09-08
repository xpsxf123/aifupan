using juliang;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang
{
    public class DataCollectEventArgs : EventArgs
    {
        /// <summary>
        /// 主播名称
        /// </summary>
        public string anchorName { get; set; }
        /// <summary>
        /// 主播SecUid
        /// </summary>
        public string secUid { get; set; }
        /// <summary>
        /// 直播场次号
        /// </summary>
        public string batchNumber { get; set; }
        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }
        /// <summary>
        /// 数据key
        /// </summary>
        public string key { get; set; }
        /// <summary>
        /// 数据
        /// </summary>
        public string data { get; set; }
        /// <summary>
        /// 窗体对象
        /// </summary>
        [JsonIgnore]
        public JuliangForm juliangForm { get; set; }
    }
}
