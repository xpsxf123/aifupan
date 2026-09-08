using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using MediaInfo;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.bo.shortVideo;
using ReviewAnalysis.ShortVideo;
using ReviewAnalysis.ShortVideo.Enums;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.shortVideo;

namespace ReviewAnalysis.Controller
{
    [RestController("短视频相关的api", "api/shortVideo")]
    public class ShortVideoController
    {

        [HttpGet("选择上传的本地视频", "/selectLocalVideo")]
        public LocalVideoInfoVo selectLocalVideo()
        {
            ShortVideoBll shortVideoBll = new ShortVideoBll();
            return shortVideoBll.selectLocalVideo();
        }


        [HttpPost("立即提取文案", "/immediatelyLocalVideo")]
        public void immediatelyLocalVideo(ImmediatelyLocalVideoBo localVideoBo)
        {
            ShortVideoBll shortVideoBll = new ShortVideoBll();
            shortVideoBll.immediatelyLocalVideo(localVideoBo);
        }


        [HttpPost("重新提取文案", "/reExtract")]
        public void reExtract(ReExtractBo bo)
        {
            ShortVideoBll shortVideoBll = new ShortVideoBll();
            shortVideoBll.reExtract(bo);
        }
        
        [HttpPost("搜达人", "/captureInfluencerInfo")]
        public long? captureInfluencerInfo(CaptureInfluencerInfoBo bo)
        {
            if (string.IsNullOrEmpty(bo?.searchKeyword?.Trim() ?? null))
            {
                throw new CustomException("关键词不能为空");
            }
            InfluencerSearchBo temp = new InfluencerSearchBo()
            {
                searchKeyword = bo.searchKeyword.Trim(),
                platformType = 1
            };
            ShortVideoBll shortVideoBll = new ShortVideoBll();
            return shortVideoBll.captureInfluencerInfo(temp);
        }

        [HttpPost("同步达人的视频信息", "/syncInfluencerVideo")]
        public async Task syncInfluencerVideo(SyncInfluencerVideoBo bo)
        {
            ShortVideoBll shortVideoBll = new ShortVideoBll();
            await Task.Run(() => shortVideoBll.syncInfluencerVideo(bo)).ConfigureAwait(false);
        }

        [HttpPost("添加订阅达人", "/addInfluencerInfo")]
        public void addInfluencerInfo(CaptureInfluencerInfoBo bo)
        {
            if (string.IsNullOrEmpty(bo?.searchKeyword?.Trim() ?? null))
            {
                throw new CustomException("抖音号不能为空");
            }
            ShortVideoBll shortVideoBll = new ShortVideoBll();
            bo.searchKeyword = bo.searchKeyword.Trim();
            shortVideoBll.addInfluencerInfo(bo);
            //Task.Run(() => shortVideoBll.addInfluencerInfo(bo));
        }

        [HttpPost("搜爆款", "/captureHotSearch")]
        public long? captureHotSearch(CaptureInfluencerInfoBo bo)
        {
            if (string.IsNullOrEmpty(bo?.searchKeyword?.Trim() ?? null))
            {
                throw new CustomException("关键词不能为空");
            }
            InfluencerSearchBo temp = new InfluencerSearchBo()
            {
                searchKeyword = bo.searchKeyword.Trim(),
                platformType = 1
            };
            ShortVideoBll shortVideoBll = new ShortVideoBll();
            return shortVideoBll.captureHotSearch(temp);
        }


        [HttpPost("添加订阅爆款", "/addHotSearch")]
        public VideoUserHotSubscriptionVo addHotSearch(VideoHotSubscriptionAddBo bo)
        {
            if (string.IsNullOrEmpty(bo?.keyword?.Trim() ?? null))
            {
                throw new CustomException("关键词不能为空");
            }
            ShortVideoBll shortVideoBll = new ShortVideoBll();
            bo.keyword = bo.keyword.Trim();
            return shortVideoBll.addHotSearch(bo);
        }


        [HttpPost("批量创建提取文案任务", "/batchCreateExtract")]
        public void batchCreateExtract(Dictionary<string, object> param)
        {
            ShortVideoHandle.batchCreateExtract(param);
        }

        [HttpPost("手动更新订阅爆款数据", "/updateHotSearchData")]
        public async Task updateHotSearchData(VideoHotSubscriptionAddBo bo)
        {
            if (string.IsNullOrEmpty(bo?.keyword?.Trim() ?? null))
            {
                throw new CustomException("关键词不能为空");
            }
            ShortVideoBll shortVideoBll = new ShortVideoBll();
            bo.keyword = bo.keyword.Trim();
          Task.Run(async() => await shortVideoBll.UpdateHotSearchDataAsync(bo));
        }
    }
}
