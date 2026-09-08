using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Forms;
using CefSharp;
using douyin.Utils;
using MediaInfo;
using Newtonsoft.Json;
using ReviewAnalysis.Ai;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.BackgroundWork;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.enums;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Global;
using ReviewAnalysis.HttpServer;
using ReviewAnalysis.LogConsole;
using ReviewAnalysis.Model;
using ReviewAnalysis.ShortVideo;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.config;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.vo.user;
using ReviewAnalysis.Websocket;
using ReviewAnalysis.Websocket.socketAddress;
using Swan;
using Swan.Parsers;

// 添加 Form1 的引用用于登录后启动检测
using Form1 = ReviewAnalysis.FormMain;

namespace ReviewAnalysis.Controller
{
    [RestController("配置相关的接口", "api/config")]
    public class ConfigController
    {

        private static FolderBrowserDialog _currentDialog;
        /// <summary>
        /// 是否开启定时获取用户信息
        /// </summary>
        public static bool getUserFlag = true;
        /// <summary>
        /// 获取用户失败的次数，超过10次通知前端弹窗
        /// </summary>
        public static volatile int getUserInfoErrorCount = 0;
        private readonly AsyncLock _settokenAsyncLock = new AsyncLock();

        /// <summary>
        /// 测试设置代理
        /// </summary>
        /// <returns></returns>
        [HttpGet("测试设置代理", "/testSetProxy")]
        public void testSetProxy()
        {
            new Thread(async () =>
            {
                DouyinHttpClient webClient = DouyinHttpClient.Instance;
                // 使用代理
                webClient.UseProxyIP();

            }).Start();
            
        }

        /// <summary>
        /// 测试通知前端抖音警告
        /// </summary>
        /// <returns></returns>
        [HttpGet("测试通知前端抖音警告", "/tesTdouyinWarnStopRecord")]
        public async Task tesTdouyinWarnStopRecord()
        {
            // 通知前端弹窗
            try
            {
                ConfigBll configBll = new ConfigBll();
                Config config = configBll.GetModel();
                if (config.IsRocord == 1)
                {
                    // 抖音已经发起警告，停止录制
                    await AnchorBll.StopDecectorAll();
                }

                // 通知前端上传
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "douyinWarnStopRecord";
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", $"自动上传到云空间发生异常");
            }
        }

        /// <summary>
        /// 测试通知前端打开更新弹窗
        /// </summary>
        /// <returns></returns>
        [HttpGet("测试通知前端打开更新弹窗", "/openUpdateVersion")]
        public async Task openUpdateVersion()
        {
            // 通知前端弹窗
            try
            {
                ConfigBll configBll = new ConfigBll();
                Config config = configBll.GetModel();
                if (config.IsRocord == 1)
                {
                    // 抖音已经发起警告，停止录制
                    await AnchorBll.StopDecectorAll();
                }
                // 通知前端上传
                FrontNotice frontNotice = new FrontNotice();
                Dictionary<string, object> result = new Dictionary<string, object>();
                result.Add("code", 0);
                result.Add("status", 200);
                result.Add("action", "openUpdate");
                Dictionary<string, object> data = new Dictionary<string, object>();
                data.Add("updateType", 0);
                data.Add("currentVersion", Constant.VERSION);
                data.Add("updateVersion", "2.4.0.test");
                data.Add("updateRemarks", "<p>1、新增弹幕录制功能；</p><p>2、优化录制功能；</p><p>3、xxxxxx；</p>");
                result.Add("data", data);
                frontNotice.NoticeJs(JsonConvert.SerializeObject(result));
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"测试通知前端打开更新弹窗发生异常");
            }
        }

        [HttpGet("打开更新弹窗", "/updateVersion")]
        public void updateVersion()
        {
            ReplayHttpUtils.openUpdateVersion(false);
        }

        [HttpGet("点击更新弹窗后打开更新弹窗", "/handUpdateVersion")]
        public void handUpdateVersion()
        {
            ReplayHttpUtils.allAreOpenUpdateVersion();
        }

        /// <summary>
        /// 获取当前系统时间是否准确
        /// </summary>
        /// <returns></returns>
        [HttpGet("获取当前系统时间是否准确", "/getTimeAccurate")]
        public bool getTimeAccurate()
        {
            ServerTimeUtils.checkTimeAccurate();
            return ServerTimeUtils.timeAccurate;
        }

        ///// <summary>
        ///// 获取当前系统时间是否准确
        ///// </summary>
        ///// <returns></returns>
        //[HttpGet("获取当前系统时间是否准确", "/getTimeAccurate")]
        //public async Task<bool> getTimeAccurate()
        //{
        //    ServerTimeUtils.checkTimeAccurate();
        //    return await Task.Run(()=> ServerTimeUtils.timeAccurate);
        //}


        /// <summary>
        /// 获取版本更新信息
        /// </summary>
        /// <returns></returns>
        [HttpGet("获取版本更新信息", "/getVersionUpdate")]
        public VersionUpdateVo getVersionUpdate()
        {
            //FileUtils.log(JsonConvert.SerializeObject(ReplayHttpUtils.VersionVo));
            if (ReplayHttpUtils.VersionVo != null && !Constant.VERSION.Equals(ReplayHttpUtils.VersionVo.VersionNum))
            {
                return ReplayHttpUtils.VersionVo;
            }
            return null;
        }

        [HttpGet("启动查询更新定时器", "/getCreateTime")]
        public VersionUpdateVo GetCreateTime()
        {
            FileUtils.log("启动定时版本更新检测");
            if (!ReplayHttpUtils.IsUpdateThread)
            {
                // 定时检测版本更新
                new Thread(() => ReplayHttpUtils.CheckVersionUpdate()).Start();
                ReplayHttpUtils.IsUpdateThread = true;
            }
            return this.getVersionUpdate();
        }

        [HttpGet("检查版本是否有更新", "/changeVersion")]
        public void changeVersion()
        {
            ReplayHttpUtils.VersionUpdate(0);
        }

        //[HttpGet("启动查询更新定时器", "/getCreateTime")]
        //public async Task<VersionUpdateVo> GetCreateTime()
        //{
        //    FileUtils.log("启动定时版本更新检测");
        //    if (!ReplayHttpUtils.IsUpdateThread)
        //    {
        //        // 定时检测版本更新
        //        new Thread(() => ReplayHttpUtils.CheckVersionUpdate()).Start();
        //        ReplayHttpUtils.IsUpdateThread = true;
        //    }
        //    return await Task.Run(()=> this.getVersionUpdate());
        //}

        /// <summary>
        /// getUrl
        /// </summary>
        /// <returns></returns>
        [HttpGet("getUrl", "/getUrl")]
        public string getUrl()
        {
            return Constant.GetApiBaseUrl() + Constant.GetWebServerUrl() + Constant.GetOnlineUrl();
        }

        /// <summary>
        /// 更新token
        /// </summary>
        /// <returns></returns>
        [HttpGet("更新token", "/settoken")]
        public async Task<SetTokenVo> SetToken(string token, string userId, long activeTenantId)
        {
            ReplayHttpUtils.Token = token;
            ReplayHttpUtils.UserId = userId;
            ReplayHttpUtils.ActiveTenantId = activeTenantId;

            // 初始化iis监听端口
            HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();
            httpResourceFileServer.StartResourceFileServer(config.SavePath + "\\");


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
            _ = Task.Run(() => WebsocketConnection.init());

            // 同步本地视频、文件、对比到服务器
            new Thread(() => SyncLocalDataToServer()).Start();

            // 同步服务器的主播列表到本地
            OperationAnchorBll operationAnchor = new OperationAnchorBll();
            operationAnchor.syncServerAnchor();

            // 清除视频缓存
            VideoCacheManager.VideoCachesClear();

            // 将当前用户的本地视频加载到缓存
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            //anchorVideoBll.LoadToCache();

            // 将未完成录制的视频的录制状态改为录制完成
            new Thread(() => anchorVideoBll.UpdateVideoRecordStatus()).Start();

            // 初始化视频表和文件表，将分析中的改成分析失败
            VideoApi.InitVideoAndFileAnalysisStatus();

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
            
            // 开启线程定时同步排班数据
            new Thread(() => AnchorBll.StartSyncScheduleTimer()).Start();

            // 开启自动执行的任务
            AiAutoTimer.start();

            // 福袋
            BlessBag.init();

            // 短视频视频线程启动
            ShortVideoHandle.init();

            // 登录成功后执行启动检查（上传未上传数据）
            Task.Run(async () =>
            {
                try
                {
                    var mainForm = FormUtils.GetForm() as Form1;
                    if (mainForm != null)
                    {
                        await mainForm.RunStartupCheckAfterLoginAsync();
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"登录后启动检查异常: {ex.Message}", "ConfigController");
                }
            });

            
            // 启动系统状态轮询定时器（网络检测 + 抖音授权状态）
            SystemStatusTimer.Start();

            _ = Task.Run(async () =>
            {
                await Task.Delay(5000);
                try
                {
                    // DeleteAgoLocalVideo 内部为 24 小时无限循环（永不返回），此处只需调用一次，无需外层循环
                    await AnchorVideoBll.DeleteAgoLocalVideo();
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", $"每天定时删除超过90天的本地mp4视频发生异常");
                }
            });

            // 开启线程同步主播排班信息
            // new Thread(() => AnchorBll.StartSyncScheduleTimer()).Start();

            // 启动抖音数据采集线程
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.StartDouyinScheduleDataCollectThread();

            // 获取用户对应的版本和是否显示版本选择
            var setTokenValues = await returnSetTokenValues();
            // 调用登录后的服务器接口
            _ = Task.Run(() => logoPostFun());
            return setTokenValues;
        }

        /// <summary>
        /// 返回setToken接口的值
        /// </summary>
        /// <returns></returns>
        private async Task<SetTokenVo> returnSetTokenValues()
        {
            SetTokenVo setTokenVo = new SetTokenVo();

            SetTokenVo clientVersionConfig = OrderApi.userClientVersionConfig();
            setTokenVo.clientVersion = clientVersionConfig == null ? ClientVersion.Replay : clientVersionConfig.clientVersion;
            setTokenVo.isVersionSelect = clientVersionConfig == null ? 0 : clientVersionConfig.isVersionSelect;
            
            // 如果是复盘版的客户端 and 用户是录制版，就手动设置为不弹出选择版本弹窗
            if (clientVersionConfig != null && Constant.CLIENT_VERSION == ClientVersion.Replay)
            {
                setTokenVo.isVersionSelect = 0;
            }
            
            // 之前选过了，就不选择了
            ConfigBll configBll = new ConfigBll();
            var userConfig = configBll.getUserConfig();
            if (userConfig.pureRecordingPopUp == 1)
            {
                setTokenVo.isVersionSelect = 0;
            }
            
            ReplayHttpUtils.UserClientVersion = setTokenVo.clientVersion;
            
            return setTokenVo;
        }

        /// <summary>
        /// 登录成功后启动主播上线检测
        /// </summary>
        private void StartDetectionAfterLogin()
        {
            try
            {
                FileUtils.log("登录成功，启动主播上线检测");
                
                // 获取 Form1 实例并调用检测方法
                var mainForm = FormUtils.GetForm() as Form1;
                if (mainForm != null)
                {
                    mainForm.Invoke(new Action(() =>
                    {
                        mainForm.StartAllDetectionOnStartup();
                    }));
                }
                else
                {
                    FileUtils.LogRpa("无法获取主窗体实例，检测未启动", "登录后检测");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"登录后启动检测异常: {ex.Message}", "登录后检测");
            }
        }
        
        private void logoPostFun()
        { 
            try
            {
                Dictionary<string, object> param = new Dictionary<string, object>();
                param.Add("clientVersion", ReplayHttpUtils.UserClientVersion);
                UserApi.clientLogoPost(param);
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "调用登录后的服务器接口错误");
            }
            
            // 上报电脑硬件配置到服务端
            try
            {
                var hw = ComputerInfoCollector.Collect();
                var param = new Dictionary<string, object>
                {
                    { "cpuId", ReplayHttpUtils.cpuid },
                    { "cpuModel", hw.cpuModel ?? "" },
                    { "cpuCores", hw.cpuCores },
                    { "cpuThreads", hw.cpuThreads },
                    { "cpuFrequencyMhz", hw.cpuFrequencyMhz },
                    { "ramGb", hw.ramGb },
                    { "gpuModel", hw.gpuModel ?? "" },
                    { "vramGb", hw.vramGb },
                    { "diskTotalGb", hw.diskTotalGb },
                    { "diskFreeGb", hw.diskFreeGb },
                    { "osVersion", hw.osVersion ?? "" },
                    { "clientVersion", Constant.VERSION },
                    { "macAddress", hw.macAddress ?? "" },
                    { "computerName", hw.computerName ?? "" },
                    { "ipAddress", hw.ipAddress ?? "" },
                    { "screenResolution", hw.screenResolution ?? "" }
                };
                UserApi.reportComputerConfig(param);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", "reportComputerConfig error");
            }
        }
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
        /// 同步服务器配置到客户端
        /// </summary>
        private void SyncServerConfigToClient()
        {
            try
            {

            }catch (Exception ex)
            {

            }
        }

        /// <summary>
        /// 同步本地视频、文件、对比到服务器
        /// </summary>
        private void SyncLocalDataToServer()
        {
            try
            {
                string dbPath = Path.GetFullPath(@"DbFile/review_analysis.db");
                if(!File.Exists(dbPath))
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
        /// 定时获取用户信息
        /// </summary>
        private void ScheduledGetUser()
        {
            while (getUserFlag)
            {
                try
                {
                    Asr.UserVo userVo = UserApi.GetUserLoginInfo();
                    if(userVo != null)
                    {
                        getUserInfoErrorCount = 0;
                        ReplayHttpUtils.UserInfo = userVo;

                        if(userVo.Status == 1)
                        {
                            // 用户已被冻结
                            FileUtils.log($"{userVo.NickName}", $"用户被冻结");
                            userFrozen(null);

                        }

                        // 检查是否更换租户，如果更换了，就弹出登录页
                        if (userVo.activeTenantId != null)
                        {
                            long tenantId = (long)userVo.activeTenantId;
                            if (ReplayHttpUtils.ActiveTenantId != tenantId)
                            {
                                FileUtils.log($"{userVo.NickName}，oldActiveTenantId= {ReplayHttpUtils.ActiveTenantId}，newtenantId = {tenantId}", $"用户已更换租户");
                                userFrozen("主账号发生变动，请重新登录");
                            }

                        }

                    }
                    else
                    {
                        ReplayHttpUtils.UserInfo = null;

                        getUserInfoErrorCount++;
                        if(getUserInfoErrorCount >= 10)
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
                            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                        }
                    }

                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", $"定时获取当前登录的用户信息请求失败");
                }
                Thread.Sleep(8000);
            }
        }

        /// <summary>
        /// 跳出到登录页
        /// </summary>
        /// <param name="msg"></param>
        public async Task userFrozen(string msg)
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
        /// 获取用户信息
        /// </summary>
        /// <returns></returns>
        [HttpGet("获取用户信息", "/getuserinfo")]
        public Asr.UserVo GetUserInfo()
        {

            Asr.UserVo userInfo = ReplayHttpUtils.UserInfo;
            if (userInfo == null || userInfo.Status == 1)
            {
                //new Thread(() =>
                //{
                //    // 停止自动分析
                //    AnchorVideoBll.autoAnalysis = false;
                //    // 停止继续获取用户信息
                //    ReplayHttpUtils.GetUserFlag = false;
                //    // 停止录制
                //    //OperationAnchorBll operationAnchorBll = FormUtils.GetOperationBll();
                //    //operationAnchorBll.StopDecector();
                //    AnchorBll.StopDecectorAll();
                //}).Start();

            }

            return userInfo;

        }

        ///// <summary>
        ///// 获取用户信息
        ///// </summary>
        ///// <returns></returns>
        //[HttpGet("获取用户信息", "/getuserinfo")]
        //public async Task<Asr.UserVo> GetUserInfo()
        //{

        //    Asr.UserVo userInfo = ReplayHttpUtils.UserInfo;
        //    if (userInfo == null || userInfo.Status == 1)
        //    {
        //        //new Thread(() =>
        //        //{
        //        //    // 停止自动分析
        //        //    AnchorVideoBll.autoAnalysis = false;
        //        //    // 停止继续获取用户信息
        //        //    ReplayHttpUtils.GetUserFlag = false;
        //        //    // 停止录制
        //        //    //OperationAnchorBll operationAnchorBll = FormUtils.GetOperationBll();
        //        //    //operationAnchorBll.StopDecector();
        //        //    AnchorBll.StopDecectorAll();
        //        //}).Start();

        //    }

        //    return userInfo;

        //}

        /// <summary>
        /// 获取配置
        /// </summary>
        /// <returns></returns>
        [HttpGet("获取客户端配置", "/getmodel")]
        public Config GetModel()
        {
            ConfigBll configBll = new ConfigBll();
            return configBll.GetModel();
        }

        ///// <summary>
        ///// 获取配置
        ///// </summary>
        ///// <returns></returns>
        //[HttpGet("获取客户端配置", "/getmodel")]
        //public async Task<Config>  GetModel()
        //{
        //    ConfigBll configBll = new ConfigBll();
        //    return await Task.Run(() => configBll.GetModel());
        //}

        /// <summary>
        /// 更新客户端配置
        /// </summary>
        /// <param name="config"></param>

        [HttpPost("更新客户端配置", "/updatemodel")]
        public void Update(Config config)
        {

            if (string.IsNullOrEmpty(config.SavePath))
            {
                throw new Exception("视频存储路径不能为空");
            }

            ConfigBll configBll = new ConfigBll();

            configBll.updateModel(config);
        }

        ///// <summary>
        ///// 更新客户端配置
        ///// </summary>
        ///// <param name="config"></param>

        //[HttpPost("更新客户端配置", "/updatemodel")]
        //public async Task Update(Config config)
        //{
        //    ConfigBll configBll = new ConfigBll();

        //    await Task.Run(()=> configBll.updateModel(config));
        //}

        [HttpGet("打开文件选择器", "/checkfilebox")]
        public string CheckFileSavePath()
        {
            _currentDialog = null; // 清除引用
            _currentDialog = new FolderBrowserDialog
            {
                Description = "请选择一个文件夹",
                ShowNewFolderButton = true
            };
            DialogResult result = _currentDialog.ShowDialog();
            if (result == DialogResult.OK && !string.IsNullOrWhiteSpace(_currentDialog.SelectedPath))
            {
                return _currentDialog.SelectedPath;
            }
            else
            {
                return null;
            }
        }

        [HttpGet("关闭文件选择器", "/closedialog")]
        public static void CloseDialog()
        {
            if (_currentDialog != null)
            {
                _currentDialog.Dispose(); // 关闭对话框
                _currentDialog = null; // 清除引用
            }
        }

        [HttpGet("获取当前磁盘大小", "/getdisksize")]
        public Dictionary<string, long> GetDiskSize()
        {
            //获取盘符
            // 获取当前执行文件的完整路径
            //string exePath = System.Reflection.Assembly.GetExecutingAssembly().Location;

            //// 获取当前执行文件所在的盘符，包括冒号
            //string driveWithColon = Path.GetPathRoot(exePath);

            //// 去除冒号，仅保留盘符字母
            //string driveLetter = driveWithColon.Substring(0, 1);

            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();
            string savePath = config.SavePath;
            string driveLetter = Path.GetPathRoot(savePath).Substring(0, 1);
            Dictionary<string, long> dictionary = FileUtils.GetDiskSize(driveLetter);
            if(dictionary == null || dictionary["driveTotalSize"] == 0)
            {
                throw new Exception("磁盘无法访问，请检查电脑磁盘是否松动。");
            }
            return dictionary;
        }

        [HttpGet("返回剩余空间容量最大的盘符", "/getmaxdisk")]
        public string GetMaxDisk()
        {
            // 获取所有逻辑驱动器
            DriveInfo[] drives = DriveInfo.GetDrives();

            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();

            if (config.SavePath.StartsWith("C"))
            {
                // 当前存储路径在C盘
                if (drives.Length > 1)
                {
                    // 有2个盘或以上，提示放在除C盘外最大的盘
                    var driveWith = drives
                        .Where(d => d.IsReady && d.Name != "C:\\") // 只考虑就绪的驱动器且不是C盘
                        .OrderByDescending(d => d.AvailableFreeSpace) // 根据可用空间降序排列
                        .FirstOrDefault(); // 获取第一个（也就是可用空间最大的）驱动器
                    if (driveWith == null) return "";
                    return "当前视频存储路径为系统盘C盘，是否前往设置更换成剩余容量最大的" + driveWith.Name.Substring(0, 1) + "盘？";
                }
            } else
            {
                // 当前存储在其他盘
                if (drives.Length > 2)
                {
                    // 有3个盘或以上，提示放在除C盘外最大的盘
                    var driveWith = drives
                        .Where(d => d.IsReady && d.Name != "C:\\") // 只考虑就绪的驱动器且不是C盘
                        .OrderByDescending(d => d.AvailableFreeSpace) // 根据可用空间降序排列
                        .FirstOrDefault(); // 获取第一个（也就是可用空间最大的）驱动器

                    if (driveWith == null) return "";
                    if (config.SavePath.Substring(0, 1) != driveWith.Name.Substring(0, 1))
                    {
                        return "当前视频存储路径所在盘剩余可用空间较小，是否前往设置更换成剩余容量最大的" + driveWith.Name.Substring(0, 1) + "盘？";
                    }
                }
            }

            return "";

        }

        [HttpGet("更新主程序", "/updateProgram")]
        public void UpdateProgram()
        {
            if (ReplayHttpUtils.mainUpdateDownload)
            {
                throw new Exception("程序正在下载最新的更新包，请稍后再更新");
            }
            FileUtils.log("启动更新程序", "启动更新程序");
            FileUtils.StartUpdate();
        }

        [HttpGet("更新客户端版本", "/setClientVersion")]
        public void setClientVersion()
        {
            ConfigBll configBll = new ConfigBll();
            configBll.setClientVersion();
        }

        [HttpGet("打开开发模式", "/openDevelopmentMode")]
        public int openDevelopmentMode()
        {
            ReplayHttpUtils.developmentMode = 0;
            return 0;
        }

        [HttpGet("关闭开发模式", "/closeDevMode")]
        public int closeDevMode()
        {
            ReplayHttpUtils.developmentMode = 1;
            return 1;
        }

        [HttpGet("获取客户端的模式", "/getClientMode")]
        public int getClientMode()
        {
            return ReplayHttpUtils.developmentMode;
        }
        //[HttpGet("获取客户端的模式", "/getClientMode")]
        //public async Task<int> getClientMode()
        //{
        //    return await Task.Run(()=> ReplayHttpUtils.developmentMode);
        //}

        [HttpGet("打开谷歌的F12", "/openDevTools")]
        public void openDevTools()
        {
            if (ReplayHttpUtils.developmentMode == 0)
            {
                FormMain.webBrower?.ShowDevTools();
            }
            LogForm.Instance.Show();

        }
        
        [HttpGet("获取账号密码", "/getUserObject")]
        public AccountPasswordVo getUserObject()
        {
            ConfigBll configBll = new ConfigBll();
            return configBll.getUserObject();
        }


        //[HttpGet("获取账号密码", "/getUserObject")]
        //public async Task<AccountPasswordVo> getUserObject()
        //{
        //    ConfigBll configBll = new ConfigBll();
        //    return await Task.Run(()=> configBll.getUserObject());
        //}

        ///// <summary>
        ///// 极简优化版（无DI/日志，优先解决核心问题）
        ///// </summary>
        ///// <param name="vo">用户账号密码信息</param>
        //[HttpGet("保存账号密码", "/putUserObject")]
        //public async Task PutUserObjectAsync(AccountPasswordVo vo) // 修正命名+Async
        //{
        //    // 基础参数校验
        //    if (vo == null)
        //        throw new ArgumentNullException(nameof(vo), "用户账号密码信息不能为空");
        //    if (string.IsNullOrWhiteSpace(vo.userName))
        //        throw new ArgumentException("用户不能为空", nameof(vo.userName));

        //    try
        //    {
        //        ConfigBll configBll = new ConfigBll();
        //        // 核心：若业务层无异步方法，IO操作封装为Task.Run（CPU密集型不推荐）
        //        await Task.Run(() => configBll.putUserObject(vo)).ConfigureAwait(false);
        //    }
        //    catch (Exception ex)
        //    {
        //        // 极简日志（写入文件）
        //        FileUtils.LogError($"保存用户信息失败：{ex.Message}", "PutUserObjectAsync");
        //        throw;
        //    }
        //}
        [HttpGet("保存账号密码", "/putUserObject")]
        public void putUserObject(AccountPasswordVo vo)
        {
            ConfigBll configBll = new ConfigBll();
            configBll.putUserObject(vo);
        }
        [HttpGet("设置获取websocket地址时是否使用代理", "/setSockectProxy")]
        public void setSockectProxy(int proxy)
        {
            SocketAddressInit.isProxy = proxy == 1;
        }
        
        [HttpGet("获取客户端版本配置", "/getClientVersionConfig")]
        public ClientVersionConfigVo getClientVersionConfig()
        {
            ClientVersionConfigVo configBll = new ClientVersionConfigVo();
            configBll.clientVersion = ReplayHttpUtils.UserClientVersion;
            return configBll;
        }
        
        [HttpPost("设置客户端版本配置", "/setClientVersionConfig")]
        public ClientVersionConfigVo setClientVersionConfig(ClientVersionConfigVo vo)
        {
            if(AnchorVideoBll.isAnalysis)
            {
                throw new CustomException("当前有视频正在分析中，不能切换");
            }
            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();
            if (config != null && config.IsRocord == 1)
            {
                throw new CustomException("当前正在录制中，不能切换");
            }
            configBll.setClientVersionConfig(vo);
            ReplayHttpUtils.UserClientVersion = vo.clientVersion;
            
            // 如果是在登录页后选择的版本，就记录下
            if ((vo.pageType??0) == 1)
            {
                var userConfig = configBll.getUserConfig();
                userConfig.pureRecordingPopUp = 1;
                configBll.setUserConfig(userConfig);
            }
            return getClientVersionConfig();
        }
    }
}
