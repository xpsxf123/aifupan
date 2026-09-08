using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.shortVideo
{
    public class ImmediatelyLocalVideoBo
    {

        /// <summary>
        /// 来源类型：1: 短视频URL 2:本地上传 3：搜达人  4：搜爆款
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// 视频的URL，sourceType = 1：短视频URL，sourceType = 2：本地地址
        /// </summary>
        public string videoUrl { get; set; }

        /// <summary>
        /// 平台类型
        /// </summary>
        public int? platformType { get; set; }

        /// <summary>
        /// 视频标题
        /// </summary>
        //public string videoTitle { get; set; }
    }
}
