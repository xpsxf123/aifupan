using System;
using Jint.Runtime;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.juliang;
using ReviewAnalysis.life;
using ReviewAnalysis.ShortVideo.Servier;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using douyin.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.Websocket;
using ReviewAnalysis.WeChatChannels.Services;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using ReviewAnalysis.Ai;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Bll.VideoPull;
using ReviewAnalysis.Bll.VideoPull.Juliang;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.plugins.core;

namespace ReviewAnalysis.Controller
{
    [RestController("配置相关的接口", "api/test")]
    public class TestController
    {
        private AnchorInstance instance = null;


        [HttpGet("执行拉数据", "/StartAnchorCollection")]
        public void StartAnchorCollection(string secUid, string roomId)
        {
            var anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if (anchorInfo == null)
            {
                throw new Exception("没找到secUid");
            }
            var instance = new AnchorInstance(anchorInfo, roomId, "");
            instance.Start();
            instance.CollectOnceAsync();
        }
        
        [HttpGet("执行场景切片", "/SceneSliceExecute")]
        public void SceneSliceExecute(String videoId)
        {
            SceneSliceBll.Execute(videoId);
        }

        [HttpGet("打印成pdf", "/testQian")]
        public void testQian()
        {
            var anchorByIdFromCache = AnchorCacheManager.GetAnchorByIdFromCache(
                "MS4wLjABAAAAWPO-GDD0RHvsPzhJUQGQAR6MIByVMY9bUv-_W8bVQaWM58ck3bmrIk7BTM-c8Gps");
            instance = new AnchorInstance(anchorByIdFromCache, "7636605517158173483", "sdfsdfdsfsd");
            instance.Start();
        }
        
        [HttpGet("打印成pdf", "/testQianStop")]
        public void testQianStop()
        {
            instance?.Stop();
        }

        [HttpGet("打印成pdf", "/test1111")]
        public void test1111(string videoId)
        {
            DiagnosisAuto.generateDiagnosis(new VideoEntity(){videoId = videoId});
        }
        
        [HttpGet("打印成pdf", "/openDiagnosis")]
        public void openDiagnosis(string videoId, string cueId)
        {

        }

        [HttpGet("dataUpload", "/dataUpload")]
        public void dataUpload(string batchNumber, string videoId, string secUid)
        {
            JuliangDataHandle.dataUpload(batchNumber, videoId, secUid);
            WeChatChannelsDashboardService.UploadDashboardData(videoId);
        }

        [HttpGet("getSessionId", "/getSessionId")]
        public string getSessionId()
        {
            return DouYinSession.GetValidSessionId();
        }


        [HttpPost("测试视频号", "/testWeChatChannels")]
        public async Task<string> TestWeChatChannels(SecUidModel model)
        {
            await Task.Run(async () => await WeChatChannelsSerivce.UnbindWeChatChannels(int.Parse(model.SecUid)));

            return "ok1111";
        }

        [HttpPost("测试获取视频号直播信息", "/QueryLiveInfo")]
        public string QueryLiveInfo(SecUidModel model)
        {
            var taskResult = Task.Run(async () => await WeChatChannelsSerivce.QueryLiveInfo(int.Parse(model.SecUid)));
            var result = taskResult.Result;

            return "ok1111";
        }

        [HttpPost("测试获取视频号直播心跳", "/QueryLiveHeartbeat")]
        public string QueryLiveHeartbeat(ExportIdModel model)
        {
            var taskResult = Task.Run(async () => await WeChatChannelsSerivce.QueryLiveHeartbeat(model.ExportId));
            var result = taskResult.Result;

            return "ok1111";
        }

        [HttpPost("微信视频号下载文件", "/DownloadFile")]
        public void DownloadFile()
        {
            TencentCosUtils.downloadCosFile("socketMessageFile/2025/12/04/b8264da1-35a6-4379-b219-c50db1e988b5.zip", "d:\\t", "1.zip");
            var s = "";
        }

        [HttpGet("测试巨量拉取直播列表", "/testJuliangVideoPull")]
        public async Task TestJuliangVideoPull(string secUid)
        {
            var puller = new VideoPullManager();
            await puller.PullAndSave(secUid);
        }

        /// <summary>
        /// 测试接口：拉取视频的完整数据包（在线人数曲线 + 弹幕 + 巨量平台数据）。
        /// 根据 videoId 从数据库查询 VideoEntity，然后调用 AnchorVideoBll.TryDownloadDataVideo 执行数据拉取。
        /// </summary>
        /// <param name="videoId">视频 ID</param>
        /// <param name="maxCommentCount">弹幕拉取上限，0 表示不限制</param>
        [HttpGet("测试拉取视频数据", "/testDownloadDataVideo")]
        public async Task TestDownloadDataVideo(string videoId, int maxCommentCount = 0)
        {
            VideoEntity video = VideoApi.GetVideoByVideoIdSync(videoId);
            if (video == null)
                throw new Exception($"视频不存在: {videoId}");

            var bll = new AnchorVideoBll();
            await bll.TryDownloadDataVideo(video, maxCommentCount);
        }

        [HttpGet("测试oss上传后的html打开", "/testHtml")]
        public string testHtml()
        {
            // 保存到oss中和本地中
            SignUploadUrlVo sv = AiRelatedApi.getHtmlSignUploadUrl();

            if (sv == null)
            {
                throw new CustomException("上传数据失败", 7005);
            }
            
            bool uploadFlag = UploadUtils.UploadFileAsync(sv.signedUrl, "D:\\project\\ReviewAnalysis\\bin\\Debug\\dataCollect\\test\\ai-html\\2026\\01\\06\\4529008538848104448.html", "text/html");
            if (!uploadFlag)
            {
                throw new CustomException("上传数据到服务器失败");
            }
            return sv.ossKey;
        }


        [HttpGet("测试来客GetAccountDetail", "/testLifeAccountDetail")]
        public async Task<string> TestLifeAccountDetail()
        {
            var cookies = new Dictionary<string, string>
            {
                ["sid_tt_ls"] = "c8e310dae10137d29fca3144c108a454",
                ["sessionid_ls"] = "c8e310dae10137d29fca3144c108a454",
                ["sessionid_ss_ls"] = "c8e310dae10137d29fca3144c108a454"
            };

            FileUtils.LogRpa("测试接口: 开始调用 GetAccountDetail", "来客测试");
            var result = await LifeDataApi.GetAccountDetail(cookies);
            if (result != null)
            {
                string json = JsonConvert.SerializeObject(result);
                FileUtils.LogRpa($"测试接口: GetAccountDetail 成功, result={json}", "来客测试");
                return json;
            }
            else
            {
                FileUtils.LogRpa("测试接口: GetAccountDetail 返回 null", "来客测试");
                return "{\"error\": \"GetAccountDetail returned null\"}";
            }
        }

        /// <summary>
        /// 测试接口：手动触发一轮主播下播补采集，跳过 4 小时定时等待。
        /// 复用 SupplementalDataCollectionScheduler.TriggerRoundAsync，与定时器串行、不重入。
        /// </summary>
        [HttpGet("手动触发补采集", "/TriggerSupplementCollect")]
        public async Task<string> TriggerSupplementCollect()
        {
            var scheduler = JuliangUtils.formMain?.SupplementCollectScheduler;
            if (scheduler == null)
            {
                throw new Exception("补采集调度器未启动（未登录或补采集总开关关闭）");
            }

            await scheduler.TriggerRoundAsync();
            return "补采集一轮已触发完成";
        }

        /// <summary>
        /// 测试接口：手动执行一轮删除本地视频，跳过 24 小时定时等待。
        /// 复用 <see cref="AnchorVideoBll.DeleteAgoLocalVideoOnce"/>，与定时循环同一逻辑、不进入 24 小时循环。
        /// </summary>
        [HttpGet("手动执行删除本地视频", "/TriggerDeleteLocalVideo")]
        public async Task<string> TriggerDeleteLocalVideo()
        {
            await AnchorVideoBll.DeleteAgoLocalVideoOnce();
            return "删除本地视频一轮已执行完成";
        }

        public class SecUidModel
        {
            public string SecUid { get; set; }
        }

        public class ExportIdModel
        {
            public string ExportId { get; set; }
        }
    }
}
