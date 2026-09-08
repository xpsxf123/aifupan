using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.HttpServer;
using ReviewAnalysis.Model;
using ReviewAnalysis.ShortVideo;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Websocket;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.BackgroundWork
{
    public class FristPageIni
    {
        /// <summary>
        /// 是否开启定时获取用户信息
        /// </summary>
        public static volatile bool getUserFlag = true;
        /// <summary>
        /// 获取用户失败的次数，超过10次通知前端弹窗
        /// </summary>
        public static volatile int getUserInfoErrorCount = 0;
        // 任务控制变量
        public static bool _isRunning = false; // 标记任务是否运行
        public static readonly object _lockObj = new object(); // 线程安全锁
        public static CancellationTokenSource _cts; // 取消令牌源（控制任务停止）
        public static volatile string scheduledGetUserTaskId = Guid.NewGuid().ToString();
        /// <summary>
        /// 自动分析线程（静态变量防止被GC回收）
        /// </summary>
        private static Thread _autoAnalysisThread;
        /// <summary>
        /// 守护线程（监控自动分析线程）
        /// </summary>
        private static Thread _watchdogThread;
        /// <summary>
        /// 自动分析线程重启次数
        /// </summary>
        private static volatile int _autoAnalysisRestartCount = 0;
        private static volatile FormMain _formMain;
        /// <summary>
        /// 长时间处理任务（后台线程执行）
        /// </summary>
        /// <param name="token">取消令牌（用于接收停止信号）</param>
        public static void  LongRunningTask(CancellationToken token, FormMain formMain)
        {
            // 检查是否收到取消信号，若有则抛出取消异常
            token.ThrowIfCancellationRequested();
            _formMain = formMain;
            // 初始化iis监听端口
            HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();
            Task.Run(() => httpResourceFileServer.StartResourceFileServer(config.SavePath + "\\"));

            // 初始化所有的视频数据 将录制状态修改为未录制
            //AnchorVideo anchorVideo = new AnchorVideo();
            //List<AnchorVideo> videos = anchorVideo.GetIsRecordingIsOne();
            //if (videos != null && videos.Count > 0)
            //{
            //    foreach (var item in videos)
            //    {
            //        item.IsRecording = 0;
            //        item.update();

            //        // 合并视频
            //        VideoUtils.MegerVideo(item.StoragePath, item.VideoId);

            //        // 同步视频数据到服务器
            //        ReplayHttpUtils.UpdateVideoToServer(item);
            //    }
            //}

            // 同步服务器配置到客户端
            //new Thread(() => SyncServerConfigToClient()).Start();
            // 启动websocket数据采集
            //WebsocketConnection.init();
            

            // 同步本地视频、文件、对比到服务器
            Task.Run(() => SyncLocalDataToServer());

            // 同步服务器的主播列表到本地
            OperationAnchorBll operationAnchor = new OperationAnchorBll();
             Task.Run(() => operationAnchor.syncServerAnchor());

            // 清除视频缓存
             Task.Run(() => VideoCacheManager.VideoCachesClear());

            // 将当前用户的本地视频加载到缓存
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            //anchorVideoBll.LoadToCache();

            // 将未完成录制的视频的录制状态改为录制完成
             Task.Run(() => anchorVideoBll.UpdateVideoRecordStatus());

            // 初始化视频表和文件表，将分析中的改成分析失败
             Task.Run(() => VideoApi.InitVideoAndFileAnalysisStatus());

            // 异步修改所有租户id为0的数据
            //new Thread(() => UpdateTenantData()).Start();

            // 同步服务器的视频列表到本地
            //anchorVideoBll.syncServerVideo();

            // 同步服务器的文件列表到本地
            //UploadFileBll uploadFileBll = new UploadFileBll();
            //uploadFileBll.syncServerFile();

            // 同步服务器的对比数据到本地
            //anchorVideoBll.syncServerContrast();

            //// 将场观人次数据存到缓存
            //TotalOnlineNumCacheManager.Clear();
            //TotalOnlineNumBll totalOnlineNumBll = new TotalOnlineNumBll();
            //totalOnlineNumBll.LoadToCache();

            //// 将场观人次数据存到缓存
            //VideoViewershipNumCacheManager.Clear();
            //VideoViewershipNumBll videoViewershipNumBll = new VideoViewershipNumBll();
            //videoViewershipNumBll.LoadToCache();

            //// 将在线数据存到缓存
            //OnlineNumCacheManager.Clear();
            //OnlineNumBll onlineNumBll = new OnlineNumBll();
            //onlineNumBll.LoadToCache();

            ////同步服务器近两天最高累计观看人数
            //ReplayHttpUtils.GetChangGuan();

            // 开启自动分析
            AnchorVideoBll.autoAnalysis = true;
            // 启动自动分析线程
            StartAutoAnalysisThread();
            // 启动守护线程监控自动分析线程
            StartWatchdogThread();

            getUserFlag = true;
            // 开启定时向服务器获取用户信息，如果拿到null表示用户没有登录
            new Thread(() => ScheduledGetUser()).Start();

            // 开启直播检测，将已经完成录制的主播从录制中的集合里面移到待检测集合
            //new Thread(() => OperationAnchorBll.CheckOnlineBlls()).Start();

            // 记录用户的版本
            new Thread(() => configBll.setClientVersion()).Start();

            // 诊断问题自动分析
            //AiAutoTimer.diagnosisInit();

            // 原文自动分析
            //AiAutoTimer.VideoContentInit();

            // 福袋
            BlessBag.init();

            // 短视频视频线程启动
            ShortVideoHandle.init();
        }
        /// <summary>
        /// 启动自动分析线程
        /// </summary>
        private static void StartAutoAnalysisThread()
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            _autoAnalysisThread = new Thread(() =>  // 不用 async，避免 async void 陷阱
            {
                FileUtils.LogAnalysis($"自动分析线程已启动，ThreadId: {Thread.CurrentThread.ManagedThreadId}");
                try
                {
                    // 同步阻塞等待异步任务完成，保证 Thread 不会提前退出
                    anchorVideoBll.AutoAnalysis().GetAwaiter().GetResult();
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", $"自动分析线程异常退出");
                }
                FileUtils.LogAnalysis($"自动分析线程已结束，ThreadId: {Thread.CurrentThread.ManagedThreadId}");
            })
            {
                IsBackground = true,
                Name = "AutoAnalysisThread",
                Priority = ThreadPriority.Normal
            };
            _autoAnalysisThread.Start();
        }

        /// <summary>
        /// 启动守护线程，监控自动分析线程状态
        /// </summary>
        private static void StartWatchdogThread()
        {
            _watchdogThread = new Thread(() =>
            {
                FileUtils.LogAnalysis($"守护线程已启动，ThreadId: {Thread.CurrentThread.ManagedThreadId}");
                int checkCounter = 0;
                int time = 5 * 60 * 1000; // 每5分钟检查一次
                while (true)
                {
                    try
                    {
                        Thread.Sleep(time);
                        checkCounter++;

                        // 检查自动分析开关是否开启
                        if (!AnchorVideoBll.autoAnalysis)
                        {
                            continue;
                        }

                        // 检查自动分析线程状态
                        bool needRestart = _autoAnalysisThread == null ||
                                          !_autoAnalysisThread.IsAlive ||
                                          _autoAnalysisThread.ThreadState == ThreadState.Stopped ||
                                          _autoAnalysisThread.ThreadState == ThreadState.Aborted;

                        if (needRestart)
                        {
                            _autoAnalysisRestartCount++;
                            FileUtils.LogAnalysis($"守护线程检测到自动分析线程已停止，第{_autoAnalysisRestartCount}次重启");

                            // 重新启动自动分析线程
                            StartAutoAnalysisThread();

                            FileUtils.LogAnalysis($"自动分析线程已重新启动");
                        }
                        else if (checkCounter % 10 == 0) // 每10次检查（约5分钟）记录一次心跳
                        {
                            FileUtils.LogAnalysis($"守护线程心跳: 自动分析线程状态正常, ThreadState={_autoAnalysisThread.ThreadState}, 重启次数={_autoAnalysisRestartCount}");
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex}", $"守护线程发生异常");
                    }
                }
            })
            {
                IsBackground = true,
                Name = "WatchdogThread",
                Priority = ThreadPriority.BelowNormal
            };
            _watchdogThread.Start();
        }

        /// <summary>
        /// 同步本地视频、文件、对比到服务器
        /// </summary>
        private static void SyncLocalDataToServer()
        {
            try
            {
                string dbPath = Path.GetFullPath(@"DbFile/review_analysis.db");
                if (!File.Exists(dbPath))
                {
                    return;
                }

                // 查询当前用户是否已经同步过数据到服务器
                bool result = VideoApi.CheckUserSyncLocalData();
                if (!result)
                {
                    // 还没同步过数据
                    Dictionary<string, object> param = new Dictionary<string, object>();
                    List<string> videoIds = new List<string>();
                    List<string> fileIds = new List<string>();
                    List<string> contrastIds = new List<string>();

                    // 获取本地视频
                    AnchorVideo anchorVideo = new AnchorVideo();
                    anchorVideo.UserId = ReplayHttpUtils.UserId;
                    anchorVideo.TenantId = ReplayHttpUtils.ActiveTenantId;
                    List<AnchorVideo> anchorVideoList = anchorVideo.GetList();
                    if (anchorVideoList != null && anchorVideoList.Count > 0)
                    {
                        foreach (var video in anchorVideoList)
                        {
                            if (video.DeleteStatus == 0 || video.UploadStatus == 1)
                            {
                                videoIds.Add(video.VideoId);
                            }
                        }
                    }

                    // 获取本地文件
                    UploadFile uploadFile = new UploadFile();
                    uploadFile.UserId = ReplayHttpUtils.UserId;
                    List<UploadFile> uploadFiles = uploadFile.GetList();
                    if (uploadFiles != null && uploadFiles.Count > 0)
                    {
                        foreach (var file in uploadFiles)
                        {
                            fileIds.Add(file.FileId);
                        }
                    }

                    // 获取本地对比数据
                    VideoContrast videoContrast = new VideoContrast();
                    videoContrast.UserId = ReplayHttpUtils.UserId;
                    videoContrast.TenantId = ReplayHttpUtils.ActiveTenantId;
                    List<VideoContrast> videoContrasts = videoContrast.GetList();
                    if (videoContrasts != null && videoContrasts.Count > 0)
                    {
                        foreach (var item in videoContrasts)
                        {
                            if (videoContrast.DeleteStatus == 0 || videoContrast.IsShard == 1)
                            {

                                contrastIds.Add(item.ContrastId);
                            }
                        }
                    }

                    param.Add("videoIds", videoIds);
                    param.Add("fileIds", fileIds);
                    param.Add("contrastIds", contrastIds);

                    VideoApi.SyncLocalDataToServer(param);

                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"同步本地视频、文件、对比到服务器发生异常");
            }

        }
        /// <summary>
        /// 跳出到登录页
        /// </summary>
        /// <param name="msg"></param>
        public static async Task userFrozen(string msg)
        {
            try
            {
                // 停止自动分析
                AnchorVideoBll.autoAnalysis = false;
                // 停止继续获取用户信息
                getUserFlag = false;
                // 停止录制
                await AnchorBll.StopDecectorAll();

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"用户已经更换租户id，停止录制时发生异常");
            }

            // 退出登录
            try
            {
                UserApi.logout();
                ReplayHttpUtils.Token = null;
            }
            catch (Exception ex) { }
            // 通知前端弹窗
            FrontNotice frontNotice = new FrontNotice();
            var requestDataObj = new Dictionary<string, object>();
            requestDataObj["code"] = 0;
            requestDataObj["status"] = 200;
            requestDataObj["action"] = "userFrozen";
            requestDataObj["data"] = new { msg };
            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
        }
        /// <summary>
        /// 定时获取用户信息
        /// </summary>
        private static void ScheduledGetUser()
        {
            scheduledGetUserTaskId = Guid.NewGuid().ToString();
            Task.Run(async () =>
            {
                string tempTaskId = scheduledGetUserTaskId;
                while (getUserFlag)
                {
                    try
                    {
                        Asr.UserVo userVo = UserApi.GetUserLoginInfo();
                        if (userVo != null)
                        {
                            getUserInfoErrorCount = 0;
                            ReplayHttpUtils.UserInfo = userVo;

                            if (userVo.Status == 1)
                            {
                                // 用户已被冻结
                                await FileUtils.LogAsync($"{userVo.NickName}", $"用户被冻结");
                                userFrozen(null);

                            }

                            // 检查是否更换租户，如果更换了，就弹出登录页
                            if (userVo.activeTenantId != null)
                            {
                                long tenantId = (long)userVo.activeTenantId;
                                if (ReplayHttpUtils.ActiveTenantId != tenantId)
                                {
                                    await FileUtils.LogAsync($"{userVo.NickName}，oldActiveTenantId= {ReplayHttpUtils.ActiveTenantId}，newtenantId = {tenantId}", $"用户已更换租户");
                                    userFrozen("主账号发生变动，请重新登录");
                                }

                            }

                        }
                        else
                        {
                            ReplayHttpUtils.UserInfo = null;

                            getUserInfoErrorCount++;
                            if (getUserInfoErrorCount >= 10)
                            {
                                // 通知前端弹出断网提示
                                FrontNotice frontNotice = new FrontNotice();
                                var requestDataObj = new Dictionary<string, object>();
                                requestDataObj["code"] = 0;
                                requestDataObj["status"] = 200;
                                requestDataObj["action"] = "notice";
                                var data = new Dictionary<string, object>();
                                data["alertType"] = 1;
                                data["statusType"] = "warning";
                                data["title"] = "断网提示";
                                data["msg"] = $"网络已中断超过1分钟，可能会导致录制中断或其他故障，请确保网络的稳定性~提示时间：{ServerTimeUtils.getCurrentTimeStr()}";
                                data["duration"] = 0;
                                requestDataObj["data"] = data;
                                await frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                            }
                        }

                    }
                    catch (Exception ex)
                    {
                        await FileUtils.LogErrorAsync($"{ex}", $"定时获取当前登录的用户信息请求失败");
                    }
                    Thread.Sleep(8000);
                }
            });
            
        }
    }
}
