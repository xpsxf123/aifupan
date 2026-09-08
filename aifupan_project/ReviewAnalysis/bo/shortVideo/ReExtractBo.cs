using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.shortVideo
{
    public class ReExtractBo
    {
        /// <summary>
        /// 主键id
        /// </summary>
        public long? id {  get; set; }

        /// <summary>
        /// 视频来源：1: 短视频URL 2:本地上传 3：搜达人  4：搜爆款
        /// </summary>
        public int sourceType { get; set; }
    }
}
