using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.enums;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.video;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;

namespace ReviewAnalysis.api
{
    public class VideoApi
    {

        public static volatile bool sendErrorVideoToServerFlag = false;

        /// <summary>
        /// 根据视频id获取视频信息-异步
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        public static async Task<VideoEntity> GetVideoByVideoId(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/clientGetVideoByVideoId", param);

            if(!string.IsNullOrEmpty(dataStr))
            {
                VideoEntity videoEntity = JsonConvert.DeserializeObject<VideoEntity>(dataStr);
                return videoEntity;
            }

            return null;
        }

        /// <summary>
        /// 根据 batchNumber 查询整场直播的第一条记录（paragraph=0），用于获取场次原始开始时间
        /// </summary>
        /// <param name="batchNumber">直播场次 room_id</param>
        /// <returns>场次第一条视频信息，含 startTime</returns>
        public static async Task<VideoEntity> GetLiveSessionByBatchNumber(string batchNumber)
        {
            var param = new Dictionary<string, object> { { "batchNumber", batchNumber } };
            string dataStr = await HttpUtils.SendServerGetAsync(
                ReplayHttpUtils.BaseUrl + "/AnchorVideo/clientGetLiveSessionByBatchNumber", param);
            if (!string.IsNullOrEmpty(dataStr))
                return JsonConvert.DeserializeObject<VideoEntity>(dataStr);
            return null;
        }

        /// <summary>
        /// 根据视频id获取视频信息-同步
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        public static VideoEntity GetVideoByVideoIdSync(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/AnchorVideo/clientGetVideoByVideoId", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                VideoEntity videoEntity = JsonConvert.DeserializeObject<VideoEntity>(dataStr);
                return videoEntity;
            }

            return null;
        }
        public async static Task<VideoEntity>  GetVideoByVideoIdAsync(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = await HttpAsyncUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/clientGetVideoByVideoId", param).ConfigureAwait(false);

            if (!string.IsNullOrEmpty(dataStr))
            {
                VideoEntity videoEntity = JsonConvert.DeserializeObject<VideoEntity>(dataStr);
                return videoEntity;
            }

            return null;
        }

        /// <summary>
        /// 根据视频id集合获取视频列表-同步
        /// </summary>
        /// <param name="videoIds">视频id集合</param>
        /// <returns></returns>
        public static List<VideoEntity> ListVideoByVideoIdsSync(List<string> videoIds)
        {

            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/AnchorVideo/clientListVideoByVideoIds", videoIds);

            if (!string.IsNullOrEmpty(dataStr))
            {
                List<VideoEntity> videoEntityList = JsonConvert.DeserializeObject<List<VideoEntity>>(dataStr);
                return videoEntityList;
            }

            return null;
        }

        /// <summary>
        /// 根据视频id集合获取视频列表-异步
        /// </summary>
        /// <param name="videoIds">视频id集合</param>
        /// <returns></returns>
        public static async Task<List<VideoEntity>> ListVideoByVideoIdsAsync(List<string> videoIds)
        {

            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/clientListVideoByVideoIds", videoIds);

            if (!string.IsNullOrEmpty(dataStr))
            {
                List<VideoEntity> videoEntityList = JsonConvert.DeserializeObject<List<VideoEntity>>(dataStr);
                return videoEntityList;
            }

            return null;
        }

        /// <summary>
        /// 获取未分析的视频
        /// </summary>
        /// <returns></returns>
        public static async Task<List<VideoEntity>> ListByNotAnalysis()
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("analysisStatus", 0);
            param.Add("isRecording", 0);
            param.Add("videoSliceType", 0);
            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/listUserVideo", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                List<VideoEntity> videoList = JsonConvert.DeserializeObject<List<VideoEntity>>(dataStr);
                return videoList;
            }

            return null;
        }

        /// <summary>
        /// 查询最近 N 天内开播的直播场次（主播下播后平台数据补采集专用）。
        /// 后端已按 batchNumber 分组去重，每场返回一条代表视频。
        /// </summary>
        /// <param name="days">场次窗口天数（按 startTime 过滤）</param>
        /// <returns>场次代表视频列表，接口异常或为空时返回 null</returns>
        public static async Task<List<VideoEntity>> ListRecentLiveSessions(int days)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("startTimeGe", DateTime.Now.AddDays(-days).ToString("yyyy-MM-dd HH:mm:ss"));
            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/listRecentLiveSessions", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                List<VideoEntity> videoList = JsonConvert.DeserializeObject<List<VideoEntity>>(dataStr);
                return videoList;
            }

            return null;
        }

        /// <summary>
        /// 修改视频的分析状态
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="analysisStatus">分析状态 0：未分析 1：分析中 2：分析完成 3：分析失败</param>
        /// <param name="errorReason">分析失败原因</param>
        /// <returns></returns>
        public static async Task UpdateVideoAnalysisStatus(string videoId, int analysisStatus, string errorReason)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            param.Add("analysisStatus", analysisStatus);
            param.Add("errorReason", errorReason);

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/updateVideoAnalysisStatus", param);

            // 状态变更后通知前端进度：1 分析中 → started，3 分析失败 → failed
            if (analysisStatus == 1)
            {
                FrontNotice.NoticeAnalysisProgress(videoId, "started");
            }
            else if (analysisStatus == 3)
            {
                FrontNotice.NoticeAnalysisProgress(videoId, "failed", errorReason);
            }
        }
        /// <summary>
        /// 修改视频的分析状态-同步
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="analysisStatus">分析状态 0：未分析 1：分析中 2：分析完成 3：分析失败</param>
        /// <param name="errorReason">分析失败原因</param>
        /// <returns></returns>
        public static void UpdateVideoAnalysisStatusSync(string videoId, int analysisStatus, string errorReason)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            param.Add("analysisStatus", analysisStatus);
            param.Add("errorReason", errorReason);

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/AnchorVideo/updateVideoAnalysisStatus", param);
        }
        /// <summary>
        /// 修改视频的分析状态
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="analysisStatus">分析状态 0：未分析 1：分析中 2：分析完成 3：分析失败</param>
        /// <param name="errorReason">不生成原文，2.5.0及之前的版本默认生成，以后的版本不生成, 1：不生成，其他值为生成</param>
        /// <returns></returns>
        public static async Task UpdateVideoAnalysisStatusSuccess(string videoId, int analysisStatus, int noVideoContent)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            param.Add("analysisStatus", analysisStatus);
            param.Add("noVideoContent", noVideoContent);

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/updateVideoAnalysisStatus", param);

            // 分析成功，通知前端进度：completed
            FrontNotice.NoticeAnalysisProgress(videoId, "completed");
        }

        /// <summary>
        /// 修改视频的上传状态
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="uploadStatus">视频上传状态 0：未上传 1：已上传 2：上传中</param>
        /// <param name="shareUrl">分享地址</param>
        /// <param name="videoOnlinePayUrl">在线视频播放地址</param>
        /// <returns></returns>
        public static async Task UpdateVideoUploadStatus(string videoId, int uploadStatus, string shareUrl = "", string videoOnlinePayUrl = "")
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            param.Add("uploadStatus", uploadStatus);
            param.Add("shareUrl", shareUrl);
            param.Add("videoOnlinePayUrl", videoOnlinePayUrl);

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/updateVideoUploadStatus", param);

        }
        /// <summary>
        /// 修改视频的上传状态-同步
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="uploadStatus">视频上传状态 0：未上传 1：已上传 2：上传中</param>
        /// <param name="shareUrl">分享地址</param>
        /// <param name="videoOnlinePayUrl">在线视频播放地址</param>
        /// <returns></returns>
        public static void UpdateVideoUploadStatusSync(string videoId, int uploadStatus, string shareUrl = "", string videoOnlinePayUrl = "")
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            param.Add("uploadStatus", uploadStatus);
            param.Add("shareUrl", shareUrl);
            param.Add("videoOnlinePayUrl", videoOnlinePayUrl);

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/AnchorVideo/updateVideoUploadStatus", param);

        }

        /// <summary>
        /// 根据录制状态获取视频列表
        /// <param name="isRecording">是否正在录制 0否 1是</param>
        /// </summary>
        /// <returns></returns>
        public static async Task<List<VideoEntity>> ListByRecordStatus(int isRecording)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("isRecording", isRecording);
            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/listUserVideo", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                List<VideoEntity> videoList = JsonConvert.DeserializeObject<List<VideoEntity>>(dataStr);
                return videoList;
            }

            return null;
        }

        /// <summary>
        ///批量删除视频-异步
        /// </summary>
        /// <param name="videoIdList">视频id集合</param>
        /// <returns></returns>
        public static async Task DelVideoByIds(List<string> videoIdList)
        {

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/clientDeleteVideo", videoIdList);
        }
        /// <summary>
        /// 批量删除视频-同步
        /// </summary>
        /// <param name="videoIdList">视频id集合</param>
        /// <returns></returns>
        public static void DelVideoByIdsSync(List<string> videoIdList)
        {

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/AnchorVideo/clientDeleteVideo", videoIdList);
        }

        /// <summary>
        ///删除视频
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        public static async Task DelVideoById(string videoId)
        {
            List<string> videoIdList = new List<string>();
            videoIdList.Add(videoId);
            await DelVideoByIds(videoIdList);

        }

        /// <summary>
        /// 批量修改视频的大小时长
        /// </summary>
        /// <param name="updateVideoSizeDurationBos">视频大小时长集合</param>
        /// <returns></returns>
        public static async Task UpdateVideoSizeDurationList(List<UpdateVideoSizeDurationBo> updateVideoSizeDurationBos)
        {

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/updateVideoSizeDuration", updateVideoSizeDurationBos);

        }

        /// <summary>
        /// 修改视频的大小时长
        /// </summary>
        /// <param name="updateVideoSizeDurationBo">视频大小时长</param>
        /// <returns></returns>
        public static async Task<bool> UpdateVideoSizeDuration(UpdateVideoSizeDurationBo updateVideoSizeDurationBo)
        {
            try
            {
                List<UpdateVideoSizeDurationBo> updateVideoSizeDurationBos = new List<UpdateVideoSizeDurationBo>();
                updateVideoSizeDurationBos.Add(updateVideoSizeDurationBo);
                await UpdateVideoSizeDurationList(updateVideoSizeDurationBos);
                return true;
            }catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"修改单个视频的大小时长请求发生异常");
            }
            
            return false;
        }

        /// <summary>
        /// 初始化视频表和文件，将分析中的改成分析失败
        /// </summary>
        public static async Task InitVideoAndFileAnalysisStatus()
        {
            await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/initVideoAndFileAnalysisStatus", null);
        }

        /// <summary>
        /// 保存或修改视频信息-同步
        /// </summary>
        /// <param name="videoEntity">视频信息</param>
        /// <returns></returns>
        public static void SaveOrUpdateVideo(VideoEntity videoEntity)
        {

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/AnchorVideo/saveOrUpdateVideo", videoEntity);

        }
        /// <summary>
        /// 保存或修改视频信息-异步
        /// </summary>
        /// <param name="videoEntity">视频信息</param>
        /// <returns></returns>
        public static async Task SaveOrUpdateVideoAsync(VideoEntity videoEntity)
        {

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/saveOrUpdateVideo", videoEntity);

        }

        /// <summary>
        /// 批量保存巨量拉取的视频记录
        /// </summary>
        public static async Task SavePulledVideos(List<VideoEntity> videoList)
        {
            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/savePulledVideos", videoList);
        }

        /// <summary>
        /// 检查用户是否已经同步过本地数据到服务器
        /// </summary>
        /// <returns></returns>
        public static bool CheckUserSyncLocalData()
        {

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/v2300/checkUserSyncLocalData", null);

            if(!string.IsNullOrEmpty(dataStr))
            {
                int code = int.Parse(dataStr);
                if(code == 1)
                {
                    return true;
                }
            }

            return false;
        }
        /// <summary>
        /// 同步本地视频、文件、对比到服务器
        /// </summary>
        /// <param name="param">id参数集合</param>
        /// <returns></returns>
        public static void SyncLocalDataToServer(Dictionary<string, object> param)
        {

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/openapi/v2300/syncLocalDataToServer", param);

        }

        /// <summary>
        /// 将断网导致同步失败的视频定时重新发送到服务器
        /// </summary>
        public static async Task SendErrorVideoToServer()
        {
            // 如果已经在定时处理，返回
            if (sendErrorVideoToServerFlag)
            {
                FileUtils.LogRecrd($"已经在定时处理，返回", $"录制结束后的视频处理===定时重新发送到服务器===");
                return;
            }

            sendErrorVideoToServerFlag = true;
            new Thread(async () =>
            {
                while (sendErrorVideoToServerFlag)
                {
                    try
                    {
                        List<VideoEntity> videoEntities = VideoCacheManager.ListVideoByRecordStatus(2);
                        if (videoEntities != null && videoEntities.Count > 0)
                        {
                            foreach (var video in videoEntities)
                            {
                                UpdateVideoSizeDurationBo updateVideoSizeDurationBo = new UpdateVideoSizeDurationBo();
                                updateVideoSizeDurationBo.videoId = video.videoId;
                                updateVideoSizeDurationBo.duration = long.Parse(video.duration);
                                updateVideoSizeDurationBo.fileSize = long.Parse(video.vedioSizie);
                                updateVideoSizeDurationBo.endTime = video.endTime;
                                updateVideoSizeDurationBo.isRecording = 0;
                                bool isSuccess = await UpdateVideoSizeDuration(updateVideoSizeDurationBo);
                                if (isSuccess)
                                {
                                    FileUtils.LogRecrd($"{video.storagePath}", $"录制结束后的视频处理===定时重新同步视频数据到服务器===成功");
                                    video.isRecording = 0;
                                    VideoCacheManager.SetVideoToCache(video);

                                    // 检查是否需要停止分析，修改视频分析状态
                                    AnchorRecordBll.checkStopAnalysis(video);

                                    // 判断是否需要生成数据看盘
                                    try
                                    {
                                        AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(video.secUid);
                                        if (ReplayHttpUtils.UserClientVersion.Equals(ClientVersion.Replay) && anchorInfo.platform == 0 && anchorInfo.IsDataViewing == 1 
                                            && anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth && anchorInfo.isAutoAnalysis != null && anchorInfo.isAutoAnalysis == 1)
                                        {
                                            // 生成数据看盘
                                            int onlineStatus = DouYinAnchorBll.GetOnlineStatus(anchorInfo.AnchorUserId);
                                            int anchorOnlineStatus = onlineStatus != 1 ? 1 : 0;
                                            DataViewingApi.CreateDataViewing(video.videoId, anchorOnlineStatus);
                                        }
                                    }
                                    catch (Exception e)
                                    {
                                        FileUtils.LogRecrd($"{e}", $"定时重新同步视频信息==生成数据看盘发生错误=={video.videoId}");
                                    }
                                }
                            }
                        }
                        else
                        {
                            sendErrorVideoToServerFlag = false;
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex}", $"将断网导致同步失败的视频定时重新发送到服务器失败");
                    }

                    Thread.Sleep(10000);
                }
            }).Start();
            
        }

        /// <summary>
        /// 根据视频id集合删除视频-仅删除视频
        /// <param name="ids">视频id集合</param>
        /// </summary>
        public static void DeleteLocalVideoByIds(List<string> ids)
        {
            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/anchorVideo/deleteLocalVideoByIds", ids);
        }

        /// <summary>
        /// 获取视频详情
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static AnchorVideoDetailVo infoDetailByVideoId(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/words/anchorVideoDetail/infoByVideoId", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<AnchorVideoDetailVo>(dataStr);
            }

            return null;
        }
        
        /// <summary>
        /// 更新数据诊断配置
        /// </summary>
        /// <param name="bo">参数</param>
        /// <returns></returns>
        public static DataDiagnosisConfigVo updateDataDiagnosisConfig(DataDiagnosisConfigVo bo)
        {
            var r = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/AnchorVideo/updateDataDiagnosisConfig", bo);
            if (r.success() && !string.IsNullOrEmpty(r?.data))
            {
                return JsonConvert.DeserializeObject<DataDiagnosisConfigVo>(r?.data ?? "");
            }
            else
            {
                FileUtils.LogError($"msg = {r?.msg ?? ""}", "getAiAnchorPrompt接口报错");
                throw new CustomException(r?.msg ?? "");
            }
            return null;
        }
        /// <summary>
        /// 获取视频详情-异步
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public async static Task<AnchorVideoDetailVo>  infoDetailByVideoIdAsync(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = await HttpAsyncUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/words/anchorVideoDetail/infoByVideoId", param).ConfigureAwait(false);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<AnchorVideoDetailVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 根据视频名称获取视频信息-异步
        /// </summary>
        /// <param name="videoName">视频名称</param>
        /// <returns></returns>
        public static async Task<VideoEntity> GetVideoByVideoName(string videoName)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoName", videoName);
            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/anchorVideo/getByVideoName", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                VideoEntity videoEntity = JsonConvert.DeserializeObject<VideoEntity>(dataStr);
                return videoEntity;
            }

            return null;
        }

        /// <summary>
        /// 分析视频到云空间
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="onlineFileUrl">视频播放地址</param>
        /// <returns></returns>
        public static async Task shareVideoToCloud(string videoId, string onlineFileUrl)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            param.Add("onlineFileUrl", onlineFileUrl);
            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/anchorVideo/shareVideoToCloud", param);
        }

        /// <summary>
        /// 根据secUid统计该主播在服务器上的视频数量（当前用户+租户范围内，不含已删除）
        /// </summary>
        /// <param name="secUid">主播sec_uid</param>
        /// <returns>视频数量，请求失败返回 -1</returns>
        public static int CountBySecUid(string secUid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("secUid", secUid);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/AnchorVideo/countVideoBySecUid", param);

            if (!string.IsNullOrEmpty(dataStr) && int.TryParse(dataStr, out int count))
            {
                return count;
            }

            return -1;
        }

        /// <summary>
        /// 分页获取本地源视频列表（自动删除本地视频专用）。
        /// 服务端按固定过滤条件（当前用户+租户、本地未删、分析完成、源视频）只查删除逻辑需要的字段，
        /// 逐页返回；客户端循环翻页直到 currPage 达到 totalPage。
        /// </summary>
        /// <param name="page">页码（从 1 开始）</param>
        /// <param name="limit">每页条数</param>
        /// <returns>本地源视频精简分页，请求失败返回 null</returns>
        public static async Task<PageResult<VideoEntity>> ListLocalSourceVideo(int page, int limit)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("page", page);
            param.Add("limit", limit);
            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/AnchorVideo/listLocalSourceVideo", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                PageResult<VideoEntity> pageResult = JsonConvert.DeserializeObject<PageResult<VideoEntity>>(dataStr);
                return pageResult;
            }

            return null;
        }
    }
}
