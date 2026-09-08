using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.vo.shortVideo;

namespace ReviewAnalysis.bo.shortVideo
{
    public class SyncInfluencerInfoBo : InfluencerSearchInfoBo
    {

        /// <summary>
        /// 视频详情
        /// </summary>
        public List<VideoInfoVo> videoInfoEntityList {  get; set; }

    }
}
