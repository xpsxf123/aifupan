using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using douyin.Utils;
using MediaInfo;
using Newtonsoft.Json;
using ReviewAnalysis.Ai;
using ReviewAnalysis.Ai.config;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.bo.ai;
using ReviewAnalysis.bo.shortVideo;
using ReviewAnalysis.Dto;
using ReviewAnalysis.enums;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.ShortVideo.Enums;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.ShortVideo.Servier;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;
using ReviewAnalysis.vo.shortVideo;

namespace ReviewAnalysis.ShortVideo
{
    public class ShortVideoHandle
    {
        private static volatile ThreadSafeList<VideoModel> dataQueue = new ThreadSafeList<VideoModel>();

        public static string savePath => new ConfigBll().GetModel().SavePath + "\\_本地短视频";
        public static string ffmpegPath = Path.GetFullPath("tools\\ffmpeg.exe");

        public static string webSocketUrl = "";
        public static volatile String taskId = null;
        public static volatile String syncTaskId = null;
        private static readonly object _ShortVideoLock = new object();

        /// <summary>
        /// 最近同步视频数据的时间戳
        /// </summary>
        private static long recentlySyncVideoTime = -1;
        private static string recentlySyncVideoTimePath = Path.GetFullPath("dataCollect\\config\\recentlySyncVideoTime");

        /// <summary>
        /// 项目启动后，登录后执行
        /// </summary>
        public static void init()
        {
            FileUtils.log("ShortVideoHandle.init() 被调用，启动提取循环", "短视频提取-初始化");

            taskId = Guid.NewGuid().ToString();
            startProjectInit();
            int workerCount = KvHelper.GetIntKvByKey("short_video_extract_concurrency", 1);
            for (int i = 0; i < workerCount; i++)
            {
                int workerId = i;
                Task.Run(async () =>
                {
                    string tempTaskId = taskId;
                    FileUtils.log($"StartOne提取循环已启动(worker-{workerId}), taskId={tempTaskId}", "短视频提取-初始化");
                    while (true)
                    {
                        if (tempTaskId == taskId)
                        {
                            try
                            {
                                await ShortVideoHandle.StartOne();
                            }
                            catch (Exception ex)
                            {
                               await FileUtils.LogErrorAsync($"worker-{workerId}: {ex.Message}", "短视频提取失败。error");
                            }
                        }
                        else
                        {
                            FileUtils.log($"StartOne提取循环退出(worker-{workerId}, taskId已变更), oldTaskId={tempTaskId}, newTaskId={taskId}", "短视频提取-初始化");
                            break;
                        }
                        await Task.Delay(10000);
                    }
                });
            }

            syncTaskId = Guid.NewGuid().ToString();
            Task.Run(async() =>
            {
                string tempTaskId = syncTaskId;
                FileUtils.log($"CronTimer定时同步循环已启动, syncTaskId={tempTaskId}", "短视频提取-初始化");
                while (true)
                {
                    if (tempTaskId == syncTaskId)
                    {
                        try
                        {
                            startCronTimer();
                        }
                        catch (Exception ex)
                        {
                            await FileUtils.LogErrorAsync(ex.Message, "短视频提取失败。error");
                        }
                        await Task.Delay(1000 * 30);
                    }
                    else
                    {
                        FileUtils.log($"CronTimer定时同步循环退出(syncTaskId已变更)", "短视频提取-初始化");
                        break;
                    }
                    await Task.Delay(10000);
                }
            });

            InfluencerHandle.init();
            HotSearchHandle.init();
        }

        /// <summary>
        /// 启动定时器
        /// </summary>
        public static void startCronTimer()
        {
            int interval = KvHelper.GetIntKvByKey("sync_short_video_interval", 12 * 60);
            if (isSyncShortVideo(interval)) //平均接口抓取数据 / HotSearchApiConfig.ApiConfigCount
            {
                try
                {
                    // 获取要同步的达人
                    InfluencerHandle.GetServerDataList();

                    // 获取要同步的爆款
                    HotSearchHandle.GetServerDataList();
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"Message = {ex.Message}, StackTrace = {ex.StackTrace}", "定时同步达人/爆款报错");
                }
                finally
                {
                    // 设置最近视频同步时间
                    setRecentlySyncVideoTime();
                }
            }
        }

        /// <summary>
        /// 是否同步短视频
        /// 1、先获取最近同步视频数据的时间戳
        /// 2、如果没有获取到最近同步时间，就返回true
        /// 3、对比现在时间戳，如果大于interval分钟，就返回true，否则false
        /// </summary>
        /// <param name="interval">最少的间隔时间多少分钟</param>
        /// <returns></returns>
        private static bool isSyncShortVideo(int interval)
        {
            try
            {
                // 1、先获取最近同步视频数据的时间戳
                long lastSyncTime = getRecentlySyncVideoTime();

                // 2、如果没有获取到最近同步时间，就返回true
                if (lastSyncTime == -1)
                {
                    FileUtils.log("未获取到最近同步时间，允许同步", "ShortVideoHandle.isSyncShortVideo");
                    return true;
                }

                // 3、对比现在时间戳，如果大于interval分钟，就返回true，否则false
                long currentTime = ServerTimeUtils.getCurrentTime();
                long timeDifferenceMinutes = (currentTime - lastSyncTime) / (1000 * 60); // 转换为分钟

                bool shouldSync = timeDifferenceMinutes >= interval;

                if (shouldSync)
                {
                    FileUtils.log($"同步检查 - 上次同步时间: {lastSyncTime}, 当前时间: {currentTime}, " +
                             $"时间差: {timeDifferenceMinutes}分钟, 间隔要求: {interval}分钟, 是否同步: {shouldSync}",
                             "ShortVideoHandle.isSyncShortVideo", true);
                }

                return shouldSync;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"检查是否同步短视频时发生异常: {ex.Message}", "ShortVideoHandle.isSyncShortVideo");
                // 发生异常时，为了保证系统正常运行，返回true允许同步
                return true;
            }
        }

        /// <summary>
        /// 最近同步视频数据的时间戳
        ///
        /// 步骤1、先获取recentlySyncVideoTime字段，如果后就返回
        /// 步骤2、在获取recentlySyncVideoTimePath文件路径中的时间戳，返回
        /// 都没有就返回-1
        /// </summary>
        /// <returns></returns>
        private static long getRecentlySyncVideoTime()
        {
            try
            {
                //步骤1、先获取recentlySyncVideoTime字段，如果有就返回
                //if (recentlySyncVideoTime != -1)
                //{
                //    return recentlySyncVideoTime;
                //}

                // 步骤2、在获取recentlySyncVideoTimePath文件路径中的时间戳，返回
                if (File.Exists(recentlySyncVideoTimePath))
                {
                    string timeContent = File.ReadAllText(recentlySyncVideoTimePath, Encoding.UTF8);

                    if (!string.IsNullOrWhiteSpace(timeContent) && long.TryParse(timeContent.Trim(), out long fileTime))
                    {
                        // 将文件中的时间戳缓存到内存中
                        recentlySyncVideoTime = fileTime;
                        return fileTime;
                    }
                    else
                    {
                        FileUtils.LogError($"文件内容无法解析为时间戳: {timeContent}", "ShortVideoHandle.getRecentlySyncVideoTime");
                    }
                }
                else
                {
                    FileUtils.log($"同步时间文件不存在: {recentlySyncVideoTimePath}", "ShortVideoHandle.getRecentlySyncVideoTime");
                }

                // 都没有就返回-1
                FileUtils.log("未找到最近同步时间，返回-1", "ShortVideoHandle.getRecentlySyncVideoTime");
                return -1;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"获取最近同步时间时发生异常: {ex.Message}", "ShortVideoHandle.getRecentlySyncVideoTime");
                return -1;
            }
        }

        /// <summary>
        /// 设置最近视频同步时间
        /// </summary>
        private static void setRecentlySyncVideoTime()
        {
            try
            {
                long currentTime = ServerTimeUtils.getCurrentTime();

                // 确保目录存在
                string dir = Path.GetDirectoryName(recentlySyncVideoTimePath);
                if (!Directory.Exists(dir))
                {
                    Directory.CreateDirectory(dir);
                }

                // 写入文件
                File.WriteAllText(recentlySyncVideoTimePath, currentTime.ToString(), Encoding.UTF8);

                // 同时更新内存中的值
                recentlySyncVideoTime = currentTime;

                FileUtils.log($"设置最近同步时间成功: {currentTime}", "ShortVideoHandle.setRecentlySyncVideoTime");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"设置最近同步时间时发生异常: {ex.Message}", "ShortVideoHandle.setRecentlySyncVideoTime");
            }
        }

        /// <summary>
        /// 自动提取后的视频处理
        /// </summary>
        /// <param name="autoList">视频列表</param>
        /// <param name="sourceType">视频来源 3达人，4爆款</param>
        /// <param name="sourceId">达人id或者爆款id</param>
        /// <param name="platformType">视频类型，1 抖音</param>
        public static void autoExtract(List<VideoInfoVo> autoList, int sourceType, long sourceId, int platformType)
        {
            if (autoList != null && autoList.Count > 0)
            {
                // 查询资产，是否足够提取
                Asr.UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
                long shortVideoNum = userPropertyEntity?.shortVideoNum ?? 0;
                if(shortVideoNum == 0)
                {
                    return;
                }

                // 资产不够了，资产多少提取多少
                if (autoList.Count > shortVideoNum)
                {
                    autoList = autoList.Take((int)shortVideoNum).ToList();
                }

                Dictionary<string, object> param = new Dictionary<string, object>();
                param["sourceType"] = sourceType;
                param["sourceId"] = sourceId;
                List<Dictionary<string, object>> videoInfoVos = new List<Dictionary<string, object>>();
                autoList.ForEach(item =>
                {
                    Dictionary<string, object> temp = new Dictionary<string, object>();
                    temp["platformType"] = platformType;
                    temp["platformVideoId"] = item.platformVideoId;
                    videoInfoVos.Add(temp);
                });
                param["videoInfoVos"] = videoInfoVos;

                // 批量创建提取文案任务
                ShortVideoHandle.batchCreateExtract(param);
            }
            else
            {
                FileUtils.log($"sourceType = {sourceType}, sourceId = {sourceId}, platformType = {platformType}", $"自动提取后的视频处理-没有视频");
            }
        }


        /// <summary>
        /// 手动批量创建提取文案任务
        /// </summary>
        /// <param name="param"></param>
        public static void batchCreateExtract(Dictionary<string, object> param)
        {
            // 批量创建提取文案任务
            List<VideoUserVideoVo> videoList = ShortVideoApi.batchCreateExtract(param);
            if (videoList != null && videoList.Count > 0)
            {
                // 过滤掉不是待提取的视频
                videoList = videoList.Where(x => x.extractStatus != null && x.extractStatus == (int)ExtractStatus.Pending).ToList();
                if (videoList != null && videoList.Count > 0)
                {
                    //添加加到队列中
                    addAll(JsonConvert.DeserializeObject<List<VideoModel>>(JsonConvert.SerializeObject(videoList)));
                }
            }
        }

        /// <summary>
        /// 查询服务器中待处理和处理中的短视频
        /// </summary>
        public static void startProjectInit()
        {

            List<VideoUserVideoVo> list = ShortVideoApi.getPendingQueue();
            if (list != null && list.Count > 0)
            {

                List<VideoModel> temp = JsonConvert.DeserializeObject<List<VideoModel>>(JsonConvert.SerializeObject(list));

                addAll(temp);
                FileUtils.log($"启动加载待提取视频: {temp.Count} 条", "短视频提取-启动加载");
            }
            else
            {
                FileUtils.log("启动加载待提取视频: 0 条", "短视频提取-启动加载");
            }

        }

        /// <summary>
        /// 停止线程
        /// </summary>
        public static void stop()
        {
            taskId = null;
        }

        /// <summary>
        /// 把全部要提取的数据放到队列中
        /// </summary>
        /// <param name="addList"></param>
        public static void addAll(List<VideoModel> addList)
        {
            dataQueue.AddAll(addList);
        }

        /// <summary>
        /// 获取一个要提取的数据
        /// </summary>
        public async static Task StartOne()
        {
            VideoModel videoToProcess = null;
            int totalCount = 0, pendingCount = 0, processingCount = 0, stuckCount = 0;
            string stuckInfo = null;

            lock (_ShortVideoLock)
            {
                if (!dataQueue.IsNotEmpty())
                    return;

                List<VideoModel> videoList = dataQueue.GetAll();
                if (videoList == null || videoList.Count <= 0)
                    return;

                var pendingVideos = videoList.Where(item => item.extractLocalStatus == 0).ToList();
                var processingVideos = videoList.Where(item => item.extractLocalStatus == 1).ToList();
                var stuckVideos = videoList.Where(item => item.extractLocalStatus != 0 && item.extractLocalStatus != 1).ToList();

                totalCount = videoList.Count;
                pendingCount = pendingVideos.Count;
                processingCount = processingVideos.Count;
                stuckCount = stuckVideos.Count;

                if (stuckVideos.Count > 0)
                    stuckInfo = string.Join(",", stuckVideos.Select(v => $"id={v.id},localStatus={v.extractLocalStatus},svrStatus={v.extractStatus}"));

                if (pendingVideos.Count <= 0)
                    return;

                videoToProcess = pendingVideos.First();
                videoToProcess.extractLocalStatus = 1;
            }

            try
            {
                await FileUtils.LogAsync($"队列状态: 总数={totalCount}, 待处理(0)={pendingCount}, 处理中(1)={processingCount}, 其他状态={stuckCount}", "短视频提取-队列检查", true);

                if (stuckCount > 0)
                    await FileUtils.LogAsync($"队列中有{stuckCount}条状态异常视频: {stuckInfo}", "短视频提取-异常状态");

                await FileUtils.LogAsync($"开始处理视频: {videoToProcess.videoTitle}, ID: {videoToProcess.id}, extractStatus={videoToProcess.extractStatus}");
                await Run(videoToProcess);
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync($"startOne方法执行异常: {ex.Message}", "ShortVideoHandle.startOne");
            }
        }

        /// <summary>
        /// 执行视频提取
        /// </summary>
        /// <param name="videoModel">要处理的视频模型</param>
        public static async Task Run(VideoModel videoModel)
        {
            int status = videoModel.extractStatus;
            try
            {
                // 获取
                await FileUtils.LogAsync($"开始执行视频提取: id = {videoModel.id}, videoTitle = {videoModel.videoTitle}");

                // 检查资产
                bool isCh = UserPropertyApi.checkUseProperty("shortVideoNum", -1);
                if (!isCh)
                {
                    throw new CustomException("提取文案条数不足！");
                }

                // 从服务器获取这个视频的平台类型
                int platformType = getPlatformType(videoModel);
                dataQueue.Update(item => videoModel.id == item.id, item => item.platformType = platformType);

                // 待处理
                if (videoModel.extractLocalStatus == 1 && (videoModel.extractStatus == (int)ExtractStatus.Pending || videoModel.extractStatus == (int)ExtractStatus.Processing))
                {
                    videoModel = await ProcessingHandle(videoModel);
                }

                await FileUtils.LogAsync($"视频提取完成: {videoModel.videoTitle}");
            }
            catch (CustomException ex)
            {
                videoModel.extractStatus = (int)ExtractStatus.Failed; // 设置为失败状态
                videoModel.extractErrorReason = ex.Message;
                FileUtils.LogError($"视频提取异常: {ex.Message}", "ShortVideoHandle.run");
                await SetFailed(videoModel);
            }
            catch (Exception ex)
            {
                videoModel.extractStatus = (int)ExtractStatus.Failed; // 设置为失败状态
                videoModel.extractErrorReason = ex.Message;
                FileUtils.LogError($"视频提取异常: {ex.Message}", "ShortVideoHandle.run");
                await SetFailed(videoModel);
            }
            finally
            {
                // 只有是成功或者失败才重队列中删除
                if (videoModel.extractStatus == (int)ExtractStatus.Completed || videoModel.extractStatus == (int)ExtractStatus.Failed)
                {
                    dataQueue.DeleteIf(item => item.id == videoModel.id);
                }

                await FileUtils.LogAsync($"短视频提取完成 还有{(dataQueue?.GetAll()?.Count ?? 0)}在提取 videoTitle = {videoModel.videoTitle}, extractStatus = {videoModel.extractStatus}", "短视频提取-完成一个短视频提取");
            }
        }

        /// <summary>
        /// 获取平台类型
        /// </summary>
        /// <param name="videoModel"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static int getPlatformType(VideoModel videoModel)
        {
            if(videoModel.videoId == null)
            {
                throw new CustomException("视频id为空");
            }
            VideoInfoVo video = ShortVideoApi.getVideoInfoByVideoId(videoModel.videoId);
            if (video == null || video.platformType == null)
            {
                throw new CustomException("视频查询失败");
            }

            return video.platformType ?? 0;
        }

        /// <summary>
        /// 设置为失败
        /// </summary>
        /// <param name="model"></param>
        /// <returns></returns>
        public static async Task SetFailed(VideoModel model)
        {
            if (model.extractLocalStatus == 1 && model.extractStatus == (int)ExtractStatus.Failed)
            {
                VideoExtractBo bo = new VideoExtractBo()
                {
                    id = model.id,
                    extractStatus = (int)ExtractStatus.Failed,
                    sourceType = model.sourceType ?? 0,
                    extractErrorReason = model.extractErrorReason
                };
                VideoUserVideoVo videoUserVideoVo = ShortVideoApi.urlOrLocal(bo);
            }
        }

        /// <summary>
        /// 待提取和提取中的处理
        /// </summary>
        /// <param name="model"></param>
        /// <returns></returns>
        public static async Task<VideoModel> ProcessingHandle(VideoModel model)
        {
            // 获取本地视频的地址
            await setLocalVideoFilePath(model);

            // 如果待提取的就调用接口设置成提取中
            if (model.extractStatus == (int)ExtractStatus.Pending)
            {
                // 获取视频时长
                setDuration(model);

                // 获取文件的hash值
                string fileHash = CalculateFileHash(model.localFileUrl);

                VideoExtractBo bo = new VideoExtractBo()
                {
                    id = model.id,
                    sourceType = model.sourceType ?? 0,
                    videoHash = fileHash,
                    duration = model.duration,
                    extractStatus = (int)ExtractStatus.Processing,
                };
                VideoUserVideoVo videoUserVideoVo = ShortVideoApi.urlOrLocal(bo);
                if (videoUserVideoVo != null)
                {
                    model.extractStatus = videoUserVideoVo.extractStatus ?? (int)ExtractStatus.Processing;
                    model.extractLocalStatus = videoUserVideoVo.extractStatus == (int)ExtractStatus.Processing ? 1 : 2;

                    // 服务端基于文件 hash 去重：若已存在相同文件的提取结果，会直接返回 Completed（跳过客户端处理）。
                    // 此时本地视频文件已无保留必要，需删除，否则「之前删除的视频重新生成」会残留旧文件。
                    await DeleteLocalVideoFileIfCompleted(model);
                }
                else
                {
                    await FileUtils.LogAsync($"状态更新失败(Pending→Processing): urlOrLocal返回null, id={model.id}, videoTitle={model.videoTitle}", "短视频提取-状态更新失败");
                }
            }

            // 处理提取中的调用分隔视频成音频，在把音频转义成文字
            if (model.extractLocalStatus == 1 && model.extractStatus == (int)ExtractStatus.Processing)
            {
                // 4.4 切割视频成音频
                string audioPath = $"{savePath}\\{model.getLocalSplitAudioPath()}";
                AudioUtils.SlicingAudio(model.localFileUrl, audioPath);
                await FileUtils.LogAsync("音频切割完成");

                // 4.5 调用语音识别
                await FileUtils.LogAsync("开始语音识别");
                Dictionary<string, object> result = await AsrUtils.AsrByDirectoryPath(
                    audioPath, ReplayHttpUtils.Token, "svs:auto:withitn|tencent", AsrConsumerType.ShortVideo);

                if (result == null)
                {
                    throw new CustomException("电脑网络不佳");
                }

                int code = (int)result["code"];
                List<ASRResultEntity> asrResultEntities = (List<ASRResultEntity>)result["data"];

                if (code == 500)
                {
                    throw new CustomException("网络不佳");
                }
                else if (code == 601)
                {
                    throw new CustomException("电脑时间不正确");
                }
                else if (code == 602)
                {
                    throw new CustomException("凭证无效");
                }
                else if (code == 603)
                {
                    throw new CustomException("QPS已满");
                }
                else if (code == 701)
                {
                    throw new CustomException("音频文件不存在");
                }
                else if (code == 702)
                {
                    throw new CustomException("模型文件未就绪，请先下载模型");
                }
                else if (code == 703)
                {
                    throw new CustomException("音频解码失败");
                }
                else if (code == 704)
                {
                    throw new CustomException("音频超过60秒限制");
                }

                await FileUtils.LogAsync("短视频语音识别完成");

                // 删除文件夹中的所以文件
                ShortVideoUtils.DeletedDirectory(audioPath);
                // 删除文件夹
                Directory.Delete(audioPath);

                string str = "";

                for (int i = 0; i < asrResultEntities.Count; i++)
                {
                    ASRResultEntity item = asrResultEntities[i];
                    str += item.Result;
                }

                // 原文
                model.originalExtractContent = string.IsNullOrEmpty(str) ? "无" : str ;
                // ai优化原文
                model.extractContent = getOptimizeExtractCopyContent(str, model);

                // 上传服务器，修改状态
                VideoExtractBo bo = new VideoExtractBo()
                {
                    id = model.id,
                    extractContent = model.extractContent,
                    originalExtractContent = model.originalExtractContent,
                    sourceType = model.sourceType ?? 0,
                    extractStatus = (int)ExtractStatus.Completed,
                };
                VideoUserVideoVo videoUserVideoVo = ShortVideoApi.urlOrLocal(bo);
                if (videoUserVideoVo != null)
                {
                    model.extractStatus = videoUserVideoVo.extractStatus ?? (int)ExtractStatus.Failed;
                    model.extractLocalStatus = 2;

                    // 提取成功后删除本地视频文件，不再保留
                    await DeleteLocalVideoFileIfCompleted(model);
                }
                else
                {
                    await FileUtils.LogAsync($"结果提交失败(Processing→Completed): urlOrLocal返回null, id={model.id}, videoTitle={model.videoTitle}", "短视频提取-结果提交失败");
                }
            }

            return model;
        }

        /// <summary>
        /// 提取完成后删除本地视频文件（不再保留）。
        /// 仅在提取状态为 <see cref="ExtractStatus.Completed"/> 且本地文件实际存在时执行；删除失败仅记录日志，不中断流程。
        /// </summary>
        /// <param name="model">视频模型</param>
        private static async Task DeleteLocalVideoFileIfCompleted(VideoModel model)
        {
            if (model.extractStatus != (int)ExtractStatus.Completed
                || string.IsNullOrEmpty(model.localFileUrl)
                || !File.Exists(model.localFileUrl))
            {
                return;
            }

            try
            {
                File.Delete(model.localFileUrl);
                await FileUtils.LogAsync($"视频文件已删除: {model.localFileUrl}", "短视频提取-删除视频");
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync($"删除视频文件失败: {model.localFileUrl}, {ex.Message}", "ShortVideoHandle.ProcessingHandle");
            }
        }

        /// <summary>
        /// 处理提取文案的占位符
        /// </summary>
        /// <param name="content"></param>
        /// <param name="map"></param>
        /// <returns></returns>
        private static string setPlaceholder(string content, Dictionary<string, string> map)
        {
            if (string.IsNullOrEmpty(content) || map == null || map.Count == 0)
            {
                return content;
            }
            // 处理占位符
            // #{行业}、#{违规原因}
            // 判断是否有行业，有要查询行业
            foreach (var key in map.Keys)
            {
                string replace = key;
                if (map.TryGetValue(key, out string value))
                {
                    if (content.IndexOf(replace) != -1)
                    {
                        content = content.Replace(replace, value);
                    }
                }
                
            }
            return content;
        }

        /// <summary>
        /// 获取ai优化后的提取文案
        /// </summary>
        /// <param name="content"></param>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static string getOptimizeExtractCopyContent(string content, VideoModel video)
        {
            string res = "无";
            if (string.IsNullOrEmpty(content))
            {
                return res;
            }
            ExtractPromptVo config = AiRelatedApi.getExtractPrompt();
            if (config == null || string.IsNullOrEmpty(config.cueWord))
            {
                throw new CustomException("获取提取文案优化的提示词失败");
            }

            // 设置占位符
            Dictionary<string, string> dictionary = new Dictionary<string, string>()
            {
                { "#{videoTitle}", video?.videoTitle ?? "暂无"},
                { "#{anchorName}", video?.authorName ?? "暂无"},
            };
            config.cueWord = setPlaceholder(config.cueWord, dictionary);

            // 把内容添加到提示词中
            config.cueWord = $"{config.cueWord}\n\n内容：\n{content}";

            // 获取临时token
            AiTempTokenDto token = AiRelatedApi.getAnalysisTempToken(config.aiModel ?? 0);
            AskRequestDto dto = new AskRequestDto()
            {
                identity = config.aiIdentity,
                thinkingType = "disabled"
            };
            var service = AiFactory.GetAiChatService(token.resourceType);
            CompletionsDto completions = service.ChatCompletion(token, dto, config.cueWord);

            if (completions.status == 0)
            {
                // 保存到aiToken使用记录中
                AiTokenUseRecordVo recordVo = new AiTokenUseRecordVo();
                recordVo.useSourceType = (int)AiUseSourceType.提取文案视频;
                recordVo.useSourceId = video.sourceId?.ToString() ?? "";
                recordVo.assistantType = (int)CueType.提取文案优化;
                recordVo.modelName = token.modelCode;
                recordVo.requestId = completions.choices.requestId;
                recordVo.finishReason = completions.choices.finishReason;
                recordVo.remarks = "提取文案优化";
                recordVo.promptTokens = completions.usage.promptTokens;
                recordVo.completionTokens = completions.usage.completionTokens;
                recordVo.reasoningTokens = completions.usage?.completionTokensDetails?.reasoningTokens ?? 0;
                recordVo.cachedTokens = completions.usage?.promptTokensDetails?.cached_tokens ?? 0;
                recordVo.totalTokens = completions.usage?.totalTokens ?? 0;
                AiTokenUseRecordVo result = AiTokenUseRecordApi.saveAiTokenUseRecord(recordVo);

                if (!string.IsNullOrEmpty(completions?.choices?.onlyMessage ?? ""))
                {
                    res = completions?.choices?.onlyMessage;
                }
            }
            else
            {
                throw new CustomException(completions.choices.message);
            }

            return res;
        }

        /// <summary>
        /// 获取视频的时长-校验视频超过10分钟就不提取
        /// </summary>
        /// <param name="model"></param>
        public static void setDuration(VideoModel model)
        {
            if (!string.IsNullOrEmpty(model.localFileUrl) && File.Exists(model.localFileUrl))
            {

                int duration = VideoUtils.getVideoDuration(model.localFileUrl);
                if(duration >= 0)
                {
                    model.duration = duration;
                    ShortVideoUtils.checkDuration(model.duration);
                }
            }
        }



        /// <summary>
        /// 设置本地视频地址
        /// </summary>
        /// <param name="videoModel"></param>
        public static async Task setLocalVideoFilePath(VideoModel videoModel)
        {
            if (videoModel.sourceType == 2)
            {
                // 本地上传，videoUrl就是文件夹中的视频地址
                videoModel.localFileUrl = videoModel.videoUrl;
                await FileUtils.LogAsync($"本地视频文件路径: {videoModel.localFileUrl}");
            }
            else if (videoModel.sourceType == 1 || videoModel.sourceType == 3 || videoModel.sourceType == 4)
            {
                ShortVideoService shortVideoService = new ShortVideoDouYinService();
                DouYinVideoInfo videoData = null;
                string error = "抖音短视频打开失败，请检查网络或者地址";
                try
                {
                    videoData = shortVideoService.GetVideoDetailAsync(videoModel.videoUrl);
                }
                catch (CustomException ex)
                {
                    await FileUtils.LogErrorAsync(ex.Message, "爬取抖音视频失败");
                    error = ex.Message;
                }
                catch (Exception ex)
                {
                    await FileUtils.LogErrorAsync(ex.Message, "爬取抖音视频失败");
                    error = ex.Message;
                }
                if (videoData == null || videoData.video == null)
                {
                    throw new CustomException(error);
                }

                List<string> playList = videoData.video.play_addr.url_list;
                if (playList == null || playList.Count == 0)
                {
                    await FileUtils.LogErrorAsync("获取videoData成功，但是video中的视频流地址获取失败"+JsonConvert.SerializeObject(videoData), "视频流地址获取失败");
                    throw new CustomException("抖音短视频打开失败，请检查网络或者地址");
                }
                string url = playList.FirstOrDefault();

                // 网络视频地址，需要下载到本地
                string downloadPath = videoModel.getLocalDownloadDepositPath();

                // 把视频从网络上下载下来
                await downloadVideo(url, downloadPath);

                videoModel.localFileUrl = downloadPath;
                await FileUtils.LogAsync($"视频下载完成: {downloadPath}");
            }
            else
            {
                // 网络视频地址，需要下载到本地
                string downloadPath = videoModel.getLocalDownloadDepositPath();

                // 把视频从网络上下载下来
                await downloadVideo(videoModel.videoUrl, downloadPath);

                videoModel.localFileUrl = downloadPath;
                await FileUtils.LogAsync($"视频下载完成: {downloadPath}");
            }
        }

        /// <summary>
        /// 下载视频到本地
        /// </summary>
        /// <param name="videoUrl"></param>
        /// <param name="downloadPath"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static async Task downloadVideo(string videoUrl, string downloadPath)
        {
            // 确保下载目录存在
            string downloadDir = Path.GetDirectoryName(downloadPath);
            if (!Directory.Exists(downloadDir))
            {
                Directory.CreateDirectory(downloadDir);
            }

            // 检查文件是否已经存在，如果存在就删除
            if (File.Exists(downloadPath))
            {
                try
                {
                    File.Delete(downloadPath);
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"下载文件前，删除存在的文件失败, downloadPath = {downloadPath}, msg = {ex.Message}", "下载文件前，删除存在的文件失败");
                    throw new CustomException($"视频下载失败，存在同名文件，请手动删除，地址：{downloadPath}");
                }
            }

            FileUtils.log($"开始下载网络视频: {videoUrl} -> {downloadPath}");

            // 使用ffmpeg下载视频
            bool downloadSuccess = await DownloadVideoWithFFmpeg(videoUrl, downloadPath);

            if (!downloadSuccess)
            {
                FileUtils.LogError($"视频下载失败: {videoUrl}", "ShortVideoHandle.downloadVideo");
                throw new CustomException("视频下载失败，请检查网络或视频地址");
            }
        }

        /// <summary>
        /// 使用FFmpeg下载视频
        /// </summary>
        /// <param name="videoUrl">视频URL</param>
        /// <param name="outputPath">输出路径</param>
        /// <returns>是否下载成功</returns>
        private static readonly int FFMPEG_DOWNLOAD_TIMEOUT_MS = 5 * 60 * 1000;

        private static async Task<bool> DownloadVideoWithFFmpeg(string videoUrl, string outputPath)
        {
            try
            {
                string arguments = $" -headers \"referer:{videoUrl}\" -y -i \"{videoUrl}\" -c copy \"{outputPath}\"";

                ProcessStartInfo startInfo = new ProcessStartInfo(ffmpegPath, arguments)
                {
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                using (Process process = new Process())
                {
                    process.StartInfo = startInfo;

                    FileUtils.log($"开始执行FFmpeg下载: {arguments}");

                    process.Start();

                    Task<string> outputTask = process.StandardOutput.ReadToEndAsync();
                    Task<string> errorTask = process.StandardError.ReadToEndAsync();
                    Task ioTask = Task.WhenAll(outputTask, errorTask);
                    Task timeoutTask = Task.Delay(FFMPEG_DOWNLOAD_TIMEOUT_MS);

                    Task completed = await Task.WhenAny(ioTask, timeoutTask);

                    if (completed == timeoutTask)
                    {
                        try { process.Kill(); } catch { }
                        FileUtils.LogError($"FFmpeg下载超时（{FFMPEG_DOWNLOAD_TIMEOUT_MS / 1000}秒）, 已kill进程: {videoUrl}", "DownloadVideoWithFFmpeg");
                        return false;
                    }

                    process.WaitForExit();

                    if (process.ExitCode == 0 && File.Exists(outputPath))
                    {
                        FileUtils.log($"FFmpeg下载成功: {outputPath}");
                        return true;
                    }
                    else
                    {
                        string error = errorTask.Result;
                        FileUtils.LogError($"FFmpeg下载失败，退出码: {process.ExitCode}, 错误信息: {error}", "DownloadVideoWithFFmpeg");
                        return false;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"FFmpeg下载异常: {ex.Message}", "DownloadVideoWithFFmpeg");
                return false;
            }
        }

        /// <summary>
        /// 计算文件的MD5哈希值
        /// </summary>
        /// <param name="filePath">文件路径</param>
        /// <returns>MD5哈希值</returns>
        private static string CalculateFileHash(string filePath)
        {
            try
            {
                if (!File.Exists(filePath))
                {
                    FileUtils.LogError($"文件不存在: {filePath}", "CalculateFileHash");
                    return string.Empty;
                }

                using (var md5 = MD5.Create())
                {
                    using (var stream = File.OpenRead(filePath))
                    {
                        byte[] hash = md5.ComputeHash(stream);
                        string hashString = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
                        FileUtils.log($"文件HASH计算完成: {filePath} -> {hashString}");
                        return hashString;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"计算文件HASH异常: {ex.Message}", "CalculateFileHash");
                return string.Empty;
            }
        }


        /// <summary>
        /// 异步提取视频首帧（返回操作状态）
        /// </summary>
        /// <param name="videoPath">视频文件路径</param>
        /// <param name="outputPath">输出图片路径（默认同目录下生成）</param>
        /// <returns>true表示成功，false表示失败</returns>
        public static async Task<bool> ExtractFirstFrameAsync(string videoPath, string outputPath = null)
        {
            if (!File.Exists(videoPath))
                throw new FileNotFoundException("视频文件不存在", videoPath);

            // 如果图片存在就删除图片
            File.Delete(outputPath);

            try
            {
                string arguments = $"-y -i \"{videoPath}\" -ss 00:00:00 -vframes 1 -q:v 2 \"{outputPath}\"";

                ProcessStartInfo startInfo = new ProcessStartInfo(ffmpegPath, arguments)
                {
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                using (Process process = new Process())
                {
                    process.StartInfo = startInfo;

                    FileUtils.log($"开始执行FFmpeg下载: {arguments}");

                    process.Start();

                    // 读取输出
                    string output = await process.StandardOutput.ReadToEndAsync();
                    string error = await process.StandardError.ReadToEndAsync();

                    process.WaitForExit();

                    if (process.ExitCode == 0 && File.Exists(outputPath))
                    {
                        FileUtils.log($"FFmpeg下载成功: {outputPath}");
                        return true;
                    }
                    else
                    {
                        FileUtils.LogError($"FFmpeg下载失败，退出码: {process.ExitCode}, 错误信息: {error}", "DownloadVideoWithFFmpeg");
                        return false;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"FFmpeg下载异常: {ex.Message}", "DownloadVideoWithFFmpeg");
                return false;
            }
        }
    }
}
