using douyin.Utils;
using ReviewAnalysis.Ai.config;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.shortVideo;
using ReviewAnalysis.ShortVideo.DouYin;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.ShortVideo.Enums;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.hotSearch;
using ReviewAnalysis.vo.shortVideo;
using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;

namespace ReviewAnalysis.ShortVideo
{
    public class HotSearchHandle
    {
        public static volatile ThreadSafeList<HotSearchModel> dataQueue = new ThreadSafeList<HotSearchModel>();
        public static volatile String taskId = null;
        private static readonly object _lock = new object();
        private static int[] intervalHot = new int[] { 10, 50 };

        /// <summary>
        /// 项目启动后，登录后执行
        /// </summary>
        public static void init()
        {

            taskId = Guid.NewGuid().ToString();
            Task.Run(async () =>
            {
                string tempTaskId = taskId;
                while (true)
                {
                    if (tempTaskId == taskId)
                    {
                        try
                        {
                            await HotSearchHandle.startOne();
                        }
                        catch (Exception ex)
                        {
                            await FileUtils.LogErrorAsync(ex.Message, "订阅爆款错误。error");
                        }
                        await Task.Delay(1000 * 30);
                    }
                    else
                    {
                        break;
                    }
                    await Task.Delay(10000);
                }
            });

        }

        /// <summary>
        /// 获取要同步的爆款
        /// </summary>
        public static void GetServerDataList()
        {
            try
            {
                List<VideoUserHotSubscriptionAllVo> list = ShortVideoApi.hotSearchSubscriptionList();

                if (list == null || list.Count == 0)
                {
                    FileUtils.log($"获取要同步的爆款 size = 0", "获取要同步的爆款");
                    return;
                }

                List<HotSearchModel> influencerModels = list
                    .Select(item =>
                    {
                        return new HotSearchModel()
                        {
                            hotSearchId = item.id ?? 0,
                            searchKeyword = item.keyword,
                            platformType = item.platformType ?? 1,
                            isEnabled = item.isEnabled,
                            likeCountThreshold = item.likeCountThreshold,
                            updateTimeCondition = item.updateTimeCondition,
                            status = 0
                        };
                    }).ToList();
                FileUtils.log($"获取要同步的爆款 size = {influencerModels.Count}", "获取要同步的爆款");
                dataQueue.AddAll(influencerModels);
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "爆款-GetServerDataList方法报错");
            }
        }

        private static async Task startOne()
        {
            lock (_lock)
            {
                try
                {
                    // 检查队列是否为空
                    if (!dataQueue.IsNotEmpty())
                    {
                        return;
                    }

                    // 获取队列中的数据
                    List<HotSearchModel> videoList = dataQueue.GetAll();
                    if (videoList == null || videoList.Count <= 0)
                    {
                        return;
                    }

                    // 查找extractStatus=1的数据
                    var pendingVideos = videoList.Where(item => item.status == 0).ToList();
                    if (pendingVideos == null || pendingVideos.Count <= 0)
                    {
                        return;
                    }

                    // 检查是否有正在处理的任务（同一时间只能有1个VideoModel对象执行）
                    var processingVideos = videoList.Where(item => item.status == 1).ToList();
                    if (processingVideos != null && processingVideos.Count >= 1)
                    {
                        FileUtils.log("已有订阅爆款在处理中，等待处理完成");
                        return;
                    }

                    // 获取第一个待处理的视频
                    HotSearchModel hotSearch = pendingVideos.First();

                    FileUtils.log($"开始处理订阅爆款: ID = {hotSearch.hotSearchId}， searchKeyword = {hotSearch.searchKeyword}");

                    // 异步执行run方法
                    _ = Task.Run(async () => await run(hotSearch));
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"startOne方法执行异常: {ex.Message}", "HotSearchHandle.startOne");
                }
            }
        }

        /// <summary>
        /// 启动
        /// 状态修改，和抓取爆款的视频
        /// </summary>
        /// <param name="model"></param>
        private static async Task run(HotSearchModel model)
        {
            int status = model.status;
            string erreorStr = "";
            bool isSuccess = false;
            try
            {
                // 获取

                await FileUtils.LogAsync($"开始执行爆款搜索: id = {model.hotSearchId}, searchKeyword = {model.searchKeyword}");

                // 修改本地文案提取状态
                if (model.status == 0)
                {
                    dataQueue.Update(item => model.hotSearchId == item.hotSearchId, item => item.status = 1);
                }

                // 抓取数据后获取待提取的视频
                isSuccess = await SyncAndextractAsync(model);

                // 获取爆款的停止时间 默认15分钟
                int stopTime = KvHelper.GetIntKvByKey("hotSearch_sync_time_interval", 15 * 60);

                // 睡3分钟
                await Task.Delay(stopTime * 1000);

                // 修改成已完成
                dataQueue.Update(item => model.hotSearchId == item.hotSearchId, item => item.status = (int)ExtractStatus.Completed);
                await FileUtils.LogAsync($"爆款视频提取完成: id = {model.hotSearchId}, searchKeyword = {model.searchKeyword}");
            }
            catch (CustomException ex)
            {
                model.status = (int)ExtractStatus.Failed; // 设置为失败状态
                model.errorReason = ex.Message;
                await FileUtils.LogErrorAsync($"爆款视频提取异常: Message = {ex.Message}, StackTrace = {ex.StackTrace}", "HotSearchHandle.run");
            }
            catch (Exception ex)
            {
                model.status = (int)ExtractStatus.Failed; // 设置为失败状态
                model.errorReason = ex.Message;
                await FileUtils.LogErrorAsync($"爆款视频提取异常: Message = {ex.Message}, StackTrace = {ex.StackTrace}", "HotSearchHandle.run");
            }
            finally
            {
                // 只有是成功或者失败才重队列中删除
                if (model.status == (int)ExtractStatus.Completed || model.status == (int)ExtractStatus.Failed)
                {
                    dataQueue.DeleteIf(item => item.hotSearchId == model.hotSearchId);
                }
                await FileUtils.LogAsync($"同步爆款视频完成 还有{(dataQueue?.GetAll()?.Count ?? 0)}在同步 searchKeyword = {model.searchKeyword}, status = {model.status}", "同步爆款视频完成-完成");

                // 如果没有完成就把他加到队列的最后，在来一次
                if (!isSuccess)
                {

                }
            }
        }

        /// <summary>
        /// 抓取数据后获取待提取的视频
        /// </summary>
        /// <param name="searchKeyword"></param>
        /// <param name="platformType"></param>
        /// <param name="hotSearchId"></param>
        public static async Task<bool> syncAndextract(HotSearchModel model)
        {
            if (model == null)
            {
                return false;
            }
            // 同步爆款的视频
            VideoHotSearchSyncDataBo syncInfluencerInfo = await syncHotSearchVideo(model.searchKeyword, model.platformType, new int[] { 1500, 3000 });

            if ((syncInfluencerInfo?.videoList?.Count ?? 0) == 0)
            {
                return false;
            }

            // 检查是否要提取视频
            List<VideoInfoVo> autoList = new List<VideoInfoVo>();
            // 自动提取文案
            if (model.isEnabled != null && model.isEnabled == 1)
            {
                long nowTime = ServerTimeUtils.getServerCurrentTime();

                for (int i = 0; i < syncInfluencerInfo.videoList.Count; i++)
                {
                    VideoInfoVo item = syncInfluencerInfo.videoList[i];

                    // 有点赞数的筛选，要大于这个点赞数的
                    if ((model?.likeCountThreshold ?? 0) > 0 && model.likeCountThreshold >= (item.likeCount ?? 0))
                    {
                        continue;
                    }

                    // 自动提取文案作品更新时间阈值
                    if ((model?.updateTimeCondition ?? 0) > 0 && !ShortVideoUtils.isUpdateTimeCondition(nowTime, item.publicDateTime, model.updateTimeCondition ?? 0))
                    {
                        continue;
                    }
                    autoList.Add(item);
                }
            }
            // 自动提取后的视频处理
            ShortVideoHandle.autoExtract(autoList, 4, model.hotSearchId, model.platformType);
            return true;
        }

        /// <summary>
        /// 抓取数据后获取待提取的视频-异步
        /// </summary>
        /// <param name="searchKeyword"></param>
        /// <param name="platformType"></param>
        /// <param name="hotSearchId"></param>
        public static async Task<bool> SyncAndextractAsync(HotSearchModel model)
        {
            if (model == null)
            {
                return false;
            }
            // 同步爆款的视频
            VideoHotSearchSyncDataBo syncInfluencerInfo = await SyncHotSearchVideoAsync(model.searchKeyword, model.platformType);

            if ((syncInfluencerInfo?.videoList?.Count ?? 0) == 0)
            {
                return false;
            }

            // 检查是否要提取视频
            List<VideoInfoVo> autoList = new List<VideoInfoVo>();
            // 自动提取文案
            if (model.isEnabled != null && model.isEnabled == 1)
            {
                long nowTime = ServerTimeUtils.getServerCurrentTime();

                for (int i = 0; i < syncInfluencerInfo.videoList.Count; i++)
                {
                    VideoInfoVo item = syncInfluencerInfo.videoList[i];

                    // 有点赞数的筛选，要大于这个点赞数的
                    if ((model?.likeCountThreshold ?? 0) > 0 && model.likeCountThreshold >= (item.likeCount ?? 0))
                    {
                        continue;
                    }

                    // 自动提取文案作品更新时间阈值
                    if ((model?.updateTimeCondition ?? 0) > 0 && !ShortVideoUtils.isUpdateTimeCondition(nowTime, item.publicDateTime, model.updateTimeCondition ?? 0))
                    {
                        continue;
                    }
                    autoList.Add(item);
                }
            }
            // 自动提取后的视频处理
            ShortVideoHandle.autoExtract(autoList, 4, model.hotSearchId, model.platformType);
            return true;
        }


        /// <summary>
        /// 同步爆款的视频
        /// </summary>
        /// <param name="searchKeyword">关键词</param>
        /// <param name="platformType">平台</param>
        public static async Task<VideoHotSearchSyncDataBo> syncHotSearchVideo(string searchKeyword, int platformType, int[] next = null)
        {
            VideoHotSearchSyncDataBo result = new VideoHotSearchSyncDataBo();
            result.keyword = searchKeyword;
            result.platformType = platformType;
            result.videoList = null;
            if (platformType == 1)
            {
        //        List<DouYinVideoInfo> videoList = Task.Run(async () => await new SearchVideoByAweme().AppSearchAsync(searchKeyword, 0, 100)).ConfigureAwait(false) // 关键：不切换回原同步上下文
        //.GetAwaiter()
        //.GetResult();
                List<DouYinVideoInfo> videoList = await new SearchVideoByAweme().AppSearchAsync(searchKeyword, 0, 100); // 关键：不切换回原同步上下文

                if (videoList != null && videoList.Count > 0)
                {
                    result.videoList = videoList.Select(item =>
                    {
                        return new VideoHotSearchSyncVideoVo()
                        {
                            platformType = platformType,
                            platformVideoId = item?.aweme_id ?? "",
                            videoHash = item?.video?.play_addr?.file_hash,
                            title = item.desc,
                            description = item.desc,
                            coverUrl = item?.video?.cover?.url_list?[0] ?? "",
                            videoUrl = $"https://www.douyin.com/video/{item.aweme_id}",
                            authorId = item?.author?.sec_uid ?? "",
                            authorName = item?.author?.nickname ?? "",
                            likeCount = item?.statistics?.digg_count ?? 0,
                            commentCount = item?.statistics?.comment_count ?? 0,
                            shareCount = item?.statistics?.share_count ?? 0,
                            collectCount = item?.statistics?.collect_count ?? 0,
                            duration = item?.video?.duration ?? 0,
                            publicDateTime = item.create_time != null ? (long)item.create_time * 1000 : 0,
                            publishTime = item.create_time != null ? ServerTimeUtils.getTimeStrByTime((long)item.create_time * 1000) : null,
                            influencerPlatformType = platformType,
                            influencerPlatformUserId = item?.author?.sec_uid ?? "",
                            influencerNickname = item?.author?.nickname ?? "",
                            influencerAvatar = item?.author?.avatar_thumb?.url_list?[0] ?? "",
                            influencerFollowersCount = item?.author?.follower_count ?? 0
                        };
                    }).ToList();
                }
            }

            bool save = ShortVideoApi.syncVideoHotSearchData(result);
            FileUtils.log($"搜爆款视频服务器保存成功，searchKeyword = {searchKeyword}， platformType = {platformType}，同步视频数： {result?.videoList?.Count ?? 0}", "搜爆款视频-服务器保存成功");
            if ((result?.videoList?.Count ?? 0) == 0)
            {
                var param = new ReportFailureVo()
                {
                    accountId = Convert.ToInt64(SearchVideoByAweme._emailAccount?.accountId),
                    failureReason = "邮箱查不到数据",
                    email = SearchVideoByAweme._emailAccount?.email,
                    errorMessage = "使用此邮箱查不到数据！",
                    failureType = 5 //其他
                };
                HotSearchApi.HotSearchReportFailure(param);
                FileUtils.LogError($"{SearchVideoByAweme._emailAccount?.email},使用此邮箱查不到数据！", "爆款");
                return null;
            }
            if (save)
            {
                return result;
            }
            return null;
        }
   
        /// <summary>
        /// 同步爆款的视频 -异步方式
        /// </summary>
        /// <param name="searchKeyword">关键词</param>
        /// <param name="platformType">平台</param>
        public static async Task<VideoHotSearchSyncDataBo> SyncHotSearchVideoAsync(string searchKeyword, int platformType)
        {
            VideoHotSearchSyncDataBo result = new VideoHotSearchSyncDataBo();
            result.keyword = searchKeyword;
            result.platformType = platformType;
            result.videoList = new List<VideoHotSearchSyncVideoVo>();
            var targetApi = HotSearchApiConfig.GetNextApiForKeyword(searchKeyword);
            if (platformType == 1)
            {
                List<DouYinVideoInfo> videoList = null;
                if (targetApi == null)
                {
                    return null;
                }
                switch (targetApi.ApiId)
                {
                    case "api_phone":
                        videoList = await SearchVideoByAweme.SearchDouYinHotVideosAsync(searchKeyword);
                        break;
                    case "api_pc":
                        videoList = await SearchVideoByPc2.SearchDouyinVideo(searchKeyword);
                        //if(videoList == null)
                        //videoList = SearchVideo3.SearchDouyinVideo(searchKeyword);
                        //if(videoList == null)
                        //videoList = SearchVideoByPc.SearchDouyinVideo(searchKeyword);
                        break;
                    case "api_h5":
                        videoList = await SearchVideoByH5HttpClient.SearchDouyinVideoAsync(searchKeyword);
                        //if (videoList == null)
                        //    videoList = SearchVideoByH5.SearchDouyinVideo(searchKeyword);
                        break;
                    default:
                        videoList = await SearchVideoByAweme.SearchDouYinHotVideosAsync(searchKeyword);//兜底
                        break;
                }
                if (videoList != null && videoList.Count > 0)
                {
                    foreach (var item in videoList)
                    {
                        var videoh = new VideoHotSearchSyncVideoVo()
                        {
                        };
                        videoh.platformType = platformType;
                        videoh.platformVideoId = item?.aweme_id ?? "";
                        videoh.videoHash = item?.video?.play_addr?.file_hash;
                        videoh.title = item?.desc;
                        videoh.description = item?.desc;
                        videoh.coverUrl = item?.video?.cover?.url_list?[0] ?? "";
                        videoh.videoUrl = $"https://www.douyin.com/video/{item?.aweme_id}";
                        videoh.authorId = item?.author?.sec_uid ?? "";
                        videoh.authorName = item?.author?.nickname ?? "";
                        videoh.likeCount = item?.statistics?.digg_count ?? 0;
                        videoh.commentCount = item?.statistics?.comment_count ?? 0;
                        videoh.shareCount = item?.statistics?.share_count ?? 0;
                        videoh.collectCount = item?.statistics?.collect_count ?? 0;
                        videoh.duration = item?.video?.duration ?? 0;
                        videoh.publicDateTime = item?.create_time != null ? (long)item.create_time * 1000 : 0;
                        videoh.publishTime = item?.create_time != null ? ServerTimeUtils.getTimeStrByTime((long)item?.create_time * 1000) : null;
                        videoh.influencerPlatformType = platformType;
                        videoh.influencerPlatformUserId = item?.author?.sec_uid ?? "";
                        videoh.influencerNickname = item?.author?.nickname ?? "";
                        videoh.influencerAvatar = item?.author?.avatar_thumb?.url_list?[0] ?? "";
                        videoh.influencerFollowersCount = item?.author?.follower_count ?? 0;
                        result.videoList.Add(videoh);
                    }
                }
            }

            bool save = ShortVideoApi.syncVideoHotSearchData(result);
            FileUtils.log($"搜爆款视频服务器保存成功，searchKeyword = {searchKeyword}， platformType = {platformType}，apiType = {targetApi.ApiId}，同步视频数： {result?.videoList?.Count ?? 0}", "搜爆款视频-服务器保存成功");
            if (save)
            {
                return result;
            }
            return null;
        }
    }
}
