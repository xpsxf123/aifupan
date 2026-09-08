using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.Ai.config;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.ShortVideo.DouYin;
using ReviewAnalysis.ShortVideo.Enums;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.bo.shortVideo;
using ReviewAnalysis.api;
using ReviewAnalysis.vo.shortVideo;
using MediaInfo.Model;

namespace ReviewAnalysis.ShortVideo
{
    /// <summary>
    /// 对达人的处理
    /// </summary>
    public class InfluencerHandle
    {

        public static volatile ThreadSafeList<InfluencerModel> dataQueue = new ThreadSafeList<InfluencerModel>();
        public static volatile String taskId = null;
        private static readonly object _lock = new object();


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
                            await InfluencerHandle.startOne();
                        }
                        catch (Exception ex)
                        {
                           await FileUtils.LogErrorAsync(ex.Message, "订阅达人错误。error");
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
        /// 获取要同步的达人
        /// </summary>
        public static void GetServerDataList()
        {
            try
            {
                List<UserInfluencerSubscriptionVo> list = ShortVideoApi.influencerSubscriptionList(null);

                if (list == null || list.Count == 0)
                {
                    FileUtils.log($"获取要同步的达人 size = 0", "获取要同步的达人");
                    return;
                }

                List<InfluencerModel> influencerModels = list
                    .Where(x => x != null && x.videoInfluencerInfoVo != null)
                    .Select(item =>
                    {
                        VideoInfluencerInfoVo influencerInfoVo = item.videoInfluencerInfoVo;
                        return new InfluencerModel()
                        {
                            influencerId = influencerInfoVo.id,
                            platformType = influencerInfoVo.platformType ?? 1,
                            platformUserId = influencerInfoVo.platformUserId,
                            platformAccount = influencerInfoVo.platformAccount,
                            lastSyncTime = influencerInfoVo.lastSyncTime,
                            status = 0,
                            isEnabled = item.isEnabled ?? 0,
                            likeCountThreshold = item.likeCountThreshold,
                            updateTimeCondition = item.updateTimeCondition ?? 0,
                            monitorFrequency = item.monitorFrequency ?? 0
                        };
                    }).ToList();
                FileUtils.log($"获取要同步的达人 size = {influencerModels.Count}", "获取要同步的达人");
                dataQueue.AddAll(influencerModels);
            }
            catch(Exception ex)
            {
                FileUtils.LogError(ex.Message, "达人-GetServerDataList方法报错");
            }
        }

        /// <summary>
        /// 启动分析
        /// </summary>
        /// <returns></returns>
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
                    List<InfluencerModel> videoList = dataQueue.GetAll();
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
                        FileUtils.log("已有订阅达人在处理中，等待处理完成");
                        return;
                    }

                    // 获取第一个待处理的视频
                    InfluencerModel influencer = pendingVideos.First();

                    FileUtils.log($"开始处理订阅达人: secUid = {influencer.platformUserId}, ID = {influencer.influencerId}");

                    // 异步执行run方法
                    _ = Task.Run(async () => await run(influencer));
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"startOne方法执行异常: {ex.Message}", "ShortVideoHandle.startOne");
                }
            }
        }

        /// <summary>
        /// 抓取数据
        /// </summary>
        /// <param name="model"></param>
        /// <returns></returns>
        private static async Task run(InfluencerModel model)
        {
            int status = model.status;
            string erreorStr = "";
            try
            {
                // 获取
                FileUtils.log($"开始执行达人搜索: id = {model.influencerId}, secuid = {model.platformUserId}");

                // 修改本地文案提取状态
                if (model.status == 0)
                {
                    dataQueue.Update(item => model.influencerId == item.influencerId, item => item.status = 1);
                }

                // 处理达人
                syncAndextract(model);

                // 获取达人的停止时间 默认15分钟
                int stopTime = KvHelper.GetIntKvByKey("influencer_sync_time_interval", 15 * 60);

                // 停止
                await Task.Delay(stopTime * 1000);

                // 修改成已完成
                dataQueue.Update(item => model.influencerId == item.influencerId, item => item.status = (int)ExtractStatus.Completed);
                FileUtils.log($"视频提取完成: id = {model.influencerId}, secuid = {model.platformUserId}");
            }
            catch (CustomException ex)
            {
                model.status = (int)ExtractStatus.Failed; // 设置为失败状态
                model.errorReason = ex.Message;
                FileUtils.LogError($"视频提取异常: {ex.Message}", "InfluencerHandle.run");
            }
            catch (Exception ex)
            {
                model.status = (int)ExtractStatus.Failed; // 设置为失败状态
                model.errorReason = ex.Message;
                FileUtils.LogError($"视频提取异常: {ex.Message}", "InfluencerHandle.run");
            }
            finally
            {
                // 只有是成功或者失败才重队列中删除
                if (model.status == (int)ExtractStatus.Completed || model.status == (int)ExtractStatus.Failed)
                {
                    dataQueue.DeleteIf(item => item.influencerId == model.influencerId);
                }
            }
            FileUtils.log($"同步达人视频完成 还有{(dataQueue?.GetAll()?.Count ?? 0)}在同步 platformAccount = {model.platformAccount}, status = {model.status}", "同步达人视频-完成");
        }

        /// <summary>
        /// 同步视频和自动提取文案
        /// </summary>
        /// <param name="model"></param>
        public static void syncAndextract(InfluencerModel model)
        {
            if (model == null)
            {
                return;
            }
            // 处理达人
            SyncInfluencerInfoBo syncInfluencerInfo = syncInfluencerVideo(model.platformUserId, model.platformAccount, model.platformType);

            if ((syncInfluencerInfo?.videoInfoEntityList?.Count ?? 0) == 0)
            {
                return;
            }

            List<VideoInfoVo> autoList = new List<VideoInfoVo>();

            // 自动提取文案
            if (model.isEnabled == 1)
            {
                long nowTime = ServerTimeUtils.getServerCurrentTime();
                for (int i = 0; i < syncInfluencerInfo.videoInfoEntityList.Count; i++)
                {
                    VideoInfoVo item = syncInfluencerInfo.videoInfoEntityList[i];

                    // 有点赞数的筛选，要大于这个点赞数的
                    if ((model?.likeCountThreshold ?? 0) > 0 && model.likeCountThreshold >= (item.likeCount ?? 0))
                    {
                        continue;
                    }

                    // 自动提取文案作品更新时间阈值
                    if((model?.updateTimeCondition ?? 0) > 0 && !ShortVideoUtils.isUpdateTimeCondition(nowTime, item.publicDateTime, model.updateTimeCondition))
                    {
                        continue;
                    }
                    autoList.Add(item);
                }
            }

            // 自动提取后的视频处理
            ShortVideoHandle.autoExtract(autoList, 3, model.influencerId, model.platformType);
        }

        /// <summary>
        /// 同步达人信息和对应的视频信息
        /// </summary>
        /// <param name="platformUserId"></param>
        /// <param name="platformAccount"></param>
        /// <param name="platformType"></param>
        /// <exception cref="CustomException"></exception>
        public static SyncInfluencerInfoBo syncInfluencerVideo(string platformUserId, string platformAccount, int platformType = 1)
        {
            // 抓取达人
            FileUtils.log($"搜达人视频开始，platformAccount = {platformAccount}， platformType = {platformType}", "搜达人视频-开始");
            bool isUpdate =  ShortVideoApi.hasInfluencerSyncVideos(platformUserId, platformType);
            if(!isUpdate)
            {
                return null;
            }
            DouyinSpider douyinSpider = new DouyinSpider();

            DouyinUser douYinUser = douyinSpider.UserProfile(platformUserId);

            if (douYinUser == null)
            {
                FileUtils.LogError($"达人数据获取失败，platformAccount = {platformAccount}， platformType = {platformType}", "搜达人视频-达人数据获取失败");
                throw new CustomException("达人数据获取失败");
            }
            SyncInfluencerInfoBo result = new SyncInfluencerInfoBo()
            {
                platformType = platformType,
                platformUserId = platformUserId,
                platformAccount = platformAccount,
                nickname = douYinUser.nickname,
                avatar = douYinUser?.avatar_thumb?.url_list?[0] ?? null,
                description = douYinUser.signature,
                followersCount = douYinUser.follower_count,
                followingCount = douYinUser.following_count,
                videoCount = douYinUser.aweme_count,
                likeCount = douYinUser.total_favorited,
                verificationStatus = douYinUser.verificationStatus,
                verificationInfo = douYinUser.verificationInfo
            };

            // 抓取达人的视频
            if (platformType == 1)
            {
                // 获取主播视频列表
                result.videoInfoEntityList = syncInfluencerVideoDouYin(platformUserId);
            }
            else
            {
                throw new CustomException("平台类型未知");
            }
            // 同步
            DateTime now = DateTime.Now;
            bool v = ShortVideoApi.syncInfluencerInfo(result);
            if (!v)
            {
                throw new CustomException("视频数据保存失败");
            }
            FileUtils.log($"服务器保存成功，platformAccount = {platformAccount}， platformType = {platformType}，同步视频数： {result?.videoInfoEntityList?.Count ?? 0}", "搜达人视频-服务器保存成功");
            return result;
        }

        /// <summary>
        /// 同步抖音主播的视频
        /// </summary>
        /// <param name="secUid"></param>
        private static List<VideoInfoVo> syncInfluencerVideoDouYin(string secUid)
        {
            DouyinSpider douyinSpider = new DouyinSpider();

            // 获取要的作品数
            int numberMax = KvHelper.GetIntKvByKey("influencer_sync_video_numberMax", 150);

            // 要多少天内的视频
            int timeMax = KvHelper.GetIntKvByKey("influencer_sync_video_timeMax", 6 * 60);

            List<DouYinVideoInfo> list = douyinSpider.GetUserAwemeVideoList(secUid, numberMax, timeMax * 24 * 60 * 60);
            if (list == null || list.Count == 0)
            {
                return new List<VideoInfoVo>();
            }

            return list.Select(item =>
            {
                string coverUrl = null;
                string videoUrl = $"https://www.douyin.com/video/{item.aweme_id}";
                List<string> coverList = item?.video?.cover?.url_list ?? null;
                if (coverList != null && coverList.Count > 0)
                {
                    coverUrl = coverList[coverList.Count - 1];
                }

                string PublishTime = item.create_time != null ? ServerTimeUtils.getTimeStrByTime((long)item.create_time * 1000) : null;

                VideoInfoVo video = new VideoInfoVo()
                {
                    platformType = 1,
                    platformVideoId = item.aweme_id,
                    videoHash = item?.video?.play_addr?.file_hash,
                    title = item.desc,
                    description = item.desc,
                    coverUrl = coverUrl,
                    videoUrl = videoUrl,
                    authorId = null,
                    authorName = null,
                    likeCount = item?.statistics?.digg_count ?? 0,
                    commentCount = item?.statistics?.comment_count ?? 0,
                    shareCount = item?.statistics?.share_count ?? 0,
                    collectCount = item?.statistics?.collect_count ?? 0,
                    duration = item?.video?.duration ?? 0,
                    publishTime = PublishTime,
                    publicDateTime = (long)item.create_time * 1000
                };
                return video;
            }).ToList();
        }




    }
}
