using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Dto;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Controller
{
    [RestAsyncController("关于主播和录屏的控制器", "api/anchorvideo")]
    public class AnchorVideoAsyncController
    {
        /// <summary>
        /// 异步查看视频分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        [HttpGet("查看视频分析", "/lockanalysis")]
        public async Task<SentenceMarkDto> LockAnalysis(string videoId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            return await anchorVideoBll.LockVideoSisAnalyAsync(videoId);
        }
    }
}
