using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.shortVideo
{
    public class VideoInfluencerSubscribeBo : InfluencerSearchInfoBo
    {
        /// <summary>
        /// 行业ID
        /// </summary>
        public long? industryId {  get; set; }
        /// <summary>
        /// 分组ID
        /// </summary>
        public long? groupId { get; set; }

    }
}
