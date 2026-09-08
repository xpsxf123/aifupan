using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
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
using ReviewAnalysis.Global;
using ReviewAnalysis.HttpServer;
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


namespace ReviewAnalysis.Controller
{
    [RestAsyncController("配置相关的接口", "api/config")]
    public class ConfigAsyncController: IDisposable
    {
        #region 字段定义（线程安全+取消令牌）
        // 文件对话框线程安全锁
        private static readonly object _dialogLock = new object();
        private static FolderBrowserDialog _currentDialog;
        // 定时获取用户信息的取消令牌
        private CancellationTokenSource _getUserCts;
        // 异步锁（防止重复启动任务）
        private readonly AsyncLock _setTokenAsyncLock = new AsyncLock();
        // 线程安全字段（原子操作）
        private static int _getUserFlag = 1; // 1=开启，0=关闭
        private static int _getUserInfoErrorCount = 0;

        // 属性封装（线程安全访问）
        public static bool GetUserFlag
        {
            get => Interlocked.CompareExchange(ref _getUserFlag, 1, 1) == 1;
            set => Interlocked.Exchange(ref _getUserFlag, value ? 1 : 0);
        }

        public static int GetUserInfoErrorCount
        {
            get => Interlocked.CompareExchange(ref _getUserInfoErrorCount, 0, 0);
            set => Interlocked.Exchange(ref _getUserInfoErrorCount, value);
        }
        #endregion

        #region 接口实现（全异步改造）
        /// <summary>
        /// 打印成PDF（异步）
        /// </summary>
        [HttpGet("打印成pdf", "/pin")]
        public async Task PinAsync()
        {
            if (FormMain.webBrower == null)
            {
                throw new InvalidOperationException("浏览器控件未初始化");
            }

            var pdfSettings = new PdfPrintSettings
            {
                PaperWidth = 11.69,
                PaperHeight = 8.27
            };
            await FormMain.webBrower.PrintToPdfAsync("D:\\AiFuPan\\output.pdf", pdfSettings)
                .ConfigureAwait(false);
        }

        /// <summary>
        /// 测试设置代理（异步）
        /// </summary>
        [HttpGet("测试设置代理", "/testSetProxy")]
        public async Task TestSetProxyAsync()
        {
            var webClient = DouyinHttpClient.Instance;
            // 异步调用代理设置（若UseProxyIP有异步版本优先用，无则包装为Task）
            await Task.Run(() => webClient.UseProxyIP())
                .ConfigureAwait(false);
        }

        /// <summary>
        /// 测试通知前端抖音警告（异步）
        /// </summary>
        [HttpGet("测试通知前端抖音警告", "/tesTdouyinWarnStopRecord")]
        public async Task TestDouyinWarnStopRecordAsync()
        {
            try
            {
                var configBll = new ConfigBll();
                var config =  configBll.GetModel();

                if (config.IsRocord == 1)
                {
                    await AnchorBll.StopDecectorAll();
                }

                var frontNotice = new FrontNotice();
                var requestData = new Dictionary<string, object>
                {
                    ["code"] = 0,
                    ["status"] = 200,
                    ["action"] = "douyinWarnStopRecord"
                };
                await Task.Run(() => frontNotice.NoticeJs(JsonConvert.SerializeObject(requestData)))
                    .ConfigureAwait(false);
            }
            catch (Exception e)
            {
                await FileUtils.LogAsync($"{e}", $"自动上传到云空间发生异常")
                    .ConfigureAwait(false);
                throw;
            }
        }

        /// <summary>
        /// 测试通知前端打开更新弹窗（异步）
        /// </summary>
        [HttpGet("测试通知前端打开更新弹窗", "/openUpdateVersion")]
        public async Task OpenUpdateVersionTestAsync()
        {
            try
            {
                var configBll = new ConfigBll();
                var config = configBll.GetModel();

                if (config.IsRocord == 1)
                {
                    await AnchorBll.StopDecectorAll();
                }

                var frontNotice = new FrontNotice();
                var result = new Dictionary<string, object>
                {
                    ["code"] = 0,
                    ["status"] = 200,
                    ["action"] = "openUpdate",
                    ["data"] = new Dictionary<string, object>
                    {
                        ["updateType"] = 0,
                        ["currentVersion"] = Constant.VERSION,
                        ["updateVersion"] = "2.4.0.test",
                        ["updateRemarks"] = "<p>1、新增弹幕录制功能；</p><p>2、优化录制功能；</p><p>3、xxxxxx；</p>"
                    }
                };
                await Task.Run(() => frontNotice.NoticeJs(JsonConvert.SerializeObject(result)))
                    .ConfigureAwait(false);
            }
            catch (Exception e)
            {
                await FileUtils.LogErrorAsync($"{e}", $"测试通知前端打开更新弹窗发生异常")
                    .ConfigureAwait(false);
                throw;
            }
        }

        [HttpGet("打开更新弹窗", "/updateVersion")]
        public async Task OpenUpdateVersionAsync()
        {
            await Task.Run(() => ReplayHttpUtils.openUpdateVersion(false))
                .ConfigureAwait(false);
        }

        [HttpGet("点击更新弹窗后打开更新弹窗", "/handUpdateVersion")]
        public async Task HandUpdateVersionAsync()
        {
            await Task.Run(() => ReplayHttpUtils.allAreOpenUpdateVersion())
                .ConfigureAwait(false);
        }

        /// <summary>
        /// 获取当前系统时间是否准确（异步）
        /// </summary>
        [HttpGet("获取当前系统时间是否准确", "/getTimeAccurate")]
        public async Task<bool> GetTimeAccurateAsync()
        {
            await Task.Run(ServerTimeUtils.checkTimeAccurate)
                .ConfigureAwait(false);
            return ServerTimeUtils.timeAccurate;
        }

        /// <summary>
        /// 获取版本更新信息（异步）
        /// </summary>
        [HttpGet("获取版本更新信息", "/getVersionUpdate")]
        public async Task<VersionUpdateVo> GetVersionUpdateAsync()
        {
            return await Task.Run(() =>
            {
                if (ReplayHttpUtils.VersionVo != null && !Constant.VERSION.Equals(ReplayHttpUtils.VersionVo.VersionNum))
                {
                    return ReplayHttpUtils.VersionVo;
                }
                return null;
            }).ConfigureAwait(false);
        }

        /// <summary>
        /// 启动查询更新定时器（异步）
        /// </summary>
        [HttpGet("启动查询更新定时器", "/getCreateTime")]
        public async Task<VersionUpdateVo> GetCreateTimeAsync()
        {
            await FileUtils.LogAsync("启动定时版本更新检测", "ConfigController")
                .ConfigureAwait(false);

            if (!ReplayHttpUtils.IsUpdateThread)
            {
                // 异步启动版本检测（避免阻塞）
                _ = Task.Run(() => ReplayHttpUtils.CheckVersionUpdate())
                    .ContinueWith(async t =>
                    {
                        if (t.Exception != null)
                        {
                            await FileUtils.LogErrorAsync(t.Exception.Flatten().Message, "版本检测异常")
                                .ConfigureAwait(false);
                        }
                    }, TaskContinuationOptions.OnlyOnFaulted);

                ReplayHttpUtils.IsUpdateThread = true;
            }

            return await GetVersionUpdateAsync().ConfigureAwait(false);
        }

        /// <summary>
        /// 获取拼接URL（异步）
        /// </summary>
        [HttpGet("getUrl", "/getUrl")]
        public async Task<string> GetUrlAsync()
        {
            return await Task.Run(() =>
                Constant.GetApiBaseUrl() + Constant.GetWebServerUrl() + Constant.GetOnlineUrl())
                .ConfigureAwait(false);
        }

        /// <summary>
        /// 更新Token（异步+取消支持）
        /// </summary>
        [HttpGet("更新token", "/settoken")]
        public async Task SetTokenAsync(string token, string userId, long activeTenantId)
        {
            ReplayHttpUtils.Token = token;
            ReplayHttpUtils.UserId = userId;
            ReplayHttpUtils.ActiveTenantId = activeTenantId;

            // 线程安全检查：避免重复启动
            using (await _setTokenAsyncLock.LockAsync().ConfigureAwait(false))
            {
                if (FristPageIni._isRunning) return;
                FristPageIni._isRunning = true;
            }

            // 初始化取消令牌
            FristPageIni._cts?.Dispose();
            FristPageIni._cts = new CancellationTokenSource();
            var cancellationToken = FristPageIni._cts.Token;

            try
            {
                // 异步执行长时间任务
                //await Task.Run(() => FristPageIni.LongRunningTask(cancellationToken), cancellationToken)
                //    .ConfigureAwait(false);
            }
            catch (OperationCanceledException ex)
            {
                await FileUtils.LogErrorAsync($"{ex}", $"FristPageIni任务取消（正常停止）")
                    .ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync($"{ex}", $"FristPageIni执行异常")
                    .ConfigureAwait(false);
                throw;
            }
            finally
            {
                // 清理资源+重置状态
                using (await _setTokenAsyncLock.LockAsync().ConfigureAwait(false))
                {
                    FristPageIni._isRunning = false;
                }
                FristPageIni._cts?.Dispose();
                FristPageIni._cts = null;
            }
        }

        /// <summary>
        /// 同步本地数据到服务器（异步）
        /// </summary>
        private async Task SyncLocalDataToServerAsync()
        {
            try
            {
                string dbPath = Path.GetFullPath(@"DbFile/review_analysis.db");
                if (!File.Exists(dbPath)) return;

                // 异步检查同步状态
                bool hasSynced = await Task.Run(VideoApi.CheckUserSyncLocalData)
                    .ConfigureAwait(false);
                if (hasSynced) return;

                var param = new Dictionary<string, object>();
                var videoIds = new List<string>();
                var fileIds = new List<string>();
                var contrastIds = new List<string>();

                // 获取本地视频（异步）
                var anchorVideo = new AnchorVideo
                {
                    UserId = ReplayHttpUtils.UserId,
                    TenantId = ReplayHttpUtils.ActiveTenantId
                };
                var anchorVideoList = await Task.Run(anchorVideo.GetList)
                    .ConfigureAwait(false);
                if (anchorVideoList?.Count > 0)
                {
                    videoIds.AddRange(anchorVideoList
                        .Where(v => v.DeleteStatus == 0 || v.UploadStatus == 1)
                        .Select(v => v.VideoId));
                }

                // 获取本地文件（异步）
                var uploadFile = new UploadFile { UserId = ReplayHttpUtils.UserId };
                var uploadFiles = await Task.Run(uploadFile.GetList)
                    .ConfigureAwait(false);
                if (uploadFiles?.Count > 0)
                {
                    fileIds.AddRange(uploadFiles.Select(f => f.FileId));
                }

                // 获取本地对比数据（异步）
                var videoContrast = new VideoContrast
                {
                    UserId = ReplayHttpUtils.UserId,
                    TenantId = ReplayHttpUtils.ActiveTenantId
                };
                var videoContrasts = await Task.Run(videoContrast.GetList)
                    .ConfigureAwait(false);
                if (videoContrasts?.Count > 0)
                {
                    contrastIds.AddRange(videoContrasts
                        .Where(c => c.DeleteStatus == 0 || c.IsShard == 1)
                        .Select(c => c.ContrastId));
                }

                param.Add("videoIds", videoIds);
                param.Add("fileIds", fileIds);
                param.Add("contrastIds", contrastIds);

                // 异步同步数据
                await Task.Run(() => VideoApi.SyncLocalDataToServer(param))
                    .ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync($"{ex}", $"同步本地数据到服务器异常")
                    .ConfigureAwait(false);
            }
        }

        /// <summary>
        /// 定时获取用户信息（异步循环+取消支持）
        /// </summary>
        private async Task ScheduledGetUserAsync(CancellationToken cancellationToken)
        {
            while (GetUserFlag && !cancellationToken.IsCancellationRequested)
            {
                try
                {
                    // 异步获取用户信息（若UserApi有异步版本优先用）
                    var userVo = await Task.Run(UserApi.GetUserLoginInfo)
                        .ConfigureAwait(false);

                    if (userVo != null)
                    {
                        GetUserInfoErrorCount = 0;
                        ReplayHttpUtils.UserInfo = userVo;

                        if (userVo.Status == 1)
                        {
                            await FileUtils.LogAsync($"{userVo.NickName}", $"用户被冻结")
                                .ConfigureAwait(false);
                            await UserFrozenAsync(null).ConfigureAwait(false);
                        }

                        if (userVo.activeTenantId != null)
                        {
                            long tenantId = (long)userVo.activeTenantId;
                            if (ReplayHttpUtils.ActiveTenantId != tenantId)
                            {
                                await FileUtils.LogAsync(
                                    $"{userVo.NickName}，oldActiveTenantId= {ReplayHttpUtils.ActiveTenantId}，newtenantId = {tenantId}",
                                    $"用户更换租户")
                                    .ConfigureAwait(false);
                                await UserFrozenAsync("主账号发生变动，请重新登录").ConfigureAwait(false);
                            }
                        }
                    }
                    else
                    {
                        ReplayHttpUtils.UserInfo = null;
                        GetUserInfoErrorCount++;

                        if (GetUserInfoErrorCount >= 10)
                        {
                            // 异步通知前端断网
                            var frontNotice = new FrontNotice();
                            var requestData = new Dictionary<string, object>
                            {
                                ["code"] = 0,
                                ["status"] = 200,
                                ["action"] = "notice",
                                ["data"] = new Dictionary<string, object>
                                {
                                    ["alertType"] = 1,
                                    ["statusType"] = "warning",
                                    ["title"] = "断网提示",
                                    ["msg"] = $"网络已中断超过1分钟，可能会导致录制中断或其他故障，请确保网络的稳定性~提示时间：{ServerTimeUtils.getCurrentTimeStr()}",
                                    ["duration"] = 0
                                }
                            };
                            await Task.Run(() => frontNotice.NoticeJs(JsonConvert.SerializeObject(requestData)))
                                .ConfigureAwait(false);
                        }
                    }
                }
                catch (Exception ex)
                {
                    await FileUtils.LogErrorAsync($"{ex}", $"定时获取用户信息异常")
                        .ConfigureAwait(false);
                }

                // 异步延迟（替代Thread.Sleep，支持取消）
                await Task.Delay(8000, cancellationToken).ConfigureAwait(false);
            }
        }

        /// <summary>
        /// 用户冻结/切换租户（异步）
        /// </summary>
        private async Task UserFrozenAsync(string msg)
        {
            try
            {
                // 异步停止自动分析和录制
                await Task.Run(async () =>
                {
                    AnchorVideoBll.autoAnalysis = false;
                    GetUserFlag = false;
                    await AnchorBll.StopDecectorAll();
                }).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync($"{ex}", $"停止录制异常")
                    .ConfigureAwait(false);
            }

            // 异步退出登录
            try
            {
                await Task.Run(UserApi.logout)
                    .ConfigureAwait(false);
                ReplayHttpUtils.Token = null;
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync($"{ex}", $"退出登录异常")
                    .ConfigureAwait(false);
            }

            // 异步通知前端
            var frontNotice = new FrontNotice();
            var requestData = new Dictionary<string, object>
            {
                ["code"] = 0,
                ["status"] = 200,
                ["action"] = "userFrozen",
                ["data"] = new { msg }
            };
            await Task.Run(() => frontNotice.NoticeJs(JsonConvert.SerializeObject(requestData)))
                .ConfigureAwait(false);
        }

        /// <summary>
        /// 获取用户信息（异步）
        /// </summary>
        [HttpGet("获取用户信息", "/getuserinfo")]
        public async Task<Asr.UserVo> GetUserInfoAsync()
        {
            var userInfo = ReplayHttpUtils.UserInfo;
            if (userInfo == null || userInfo.Status == 1)
            {
                // 异步停止相关操作
                await Task.Run(async () =>
                {
                    AnchorVideoBll.autoAnalysis = false;
                    GetUserFlag = false;
                    await AnchorBll.StopDecectorAll();
                }).ConfigureAwait(false);
            }

            return userInfo;
        }

        /// <summary>
        /// 获取客户端配置（异步）
        /// </summary>
        [HttpGet("获取客户端配置", "/getmodel")]
        public async Task<Config> GetModelAsync()
        {
            var configBll = new ConfigBll();
            return await Task.Run(configBll.GetModel)
                .ConfigureAwait(false);
        }

        /// <summary>
        /// 更新客户端配置（异步）
        /// </summary>
        [HttpPost("更新客户端配置", "/updatemodel")]
        public async Task UpdateModelAsync(Config config)
        {
            if (config == null)
            {
                throw new ArgumentNullException(nameof(config), "配置信息不能为空");
            }

            var configBll = new ConfigBll();
            // 若ConfigBll有异步版本，替换为：await configBll.UpdateModelAsync(config)
            await Task.Run(() => configBll.updateModel(config))
                .ConfigureAwait(false);
        }

        /// <summary>
        /// 打开文件选择器（异步+UI线程安全）
        /// </summary>
        [HttpGet("打开文件选择器", "/checkfilebox")]
        public string CheckFileSavePathAsync()
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
        

        /// <summary>
        /// 关闭文件选择器（线程安全）
        /// </summary>
        [HttpGet("关闭文件选择器", "/closedialog")]
        public static void CloseDialog()
        {
            lock (_dialogLock)
            {
                if (_currentDialog != null)
                {
                    _currentDialog.Dispose();
                    _currentDialog = null;
                }
            }
        }

        /// <summary>
        /// 获取当前磁盘大小（异步）
        /// </summary>
        [HttpGet("获取当前磁盘大小", "/getdisksize")]
        public async Task<Dictionary<string, long>> GetDiskSizeAsync()
        {
            return await Task.Run(() =>
            {
                var configBll = new ConfigBll();
                var config = configBll.GetModel();
                string savePath = config.SavePath;
                string driveLetter = Path.GetPathRoot(savePath).Substring(0, 1);
                return FileUtils.GetDiskSize(driveLetter);
            }).ConfigureAwait(false);
        }

        /// <summary>
        /// 获取剩余空间最大的盘符（异步）
        /// </summary>
        [HttpGet("返回剩余空间容量最大的盘符", "/getmaxdisk")]
        public async Task<string> GetMaxDiskAsync()
        {
            return await Task.Run(() =>
            {
                DriveInfo[] drives = DriveInfo.GetDrives();
                var configBll = new ConfigBll();
                var config = configBll.GetModel();

                if (config.SavePath.StartsWith("C"))
                {
                    if (drives.Length > 1)
                    {
                        var maxDrive = drives
                            .Where(d => d.IsReady && d.Name != "C:\\")
                            .OrderByDescending(d => d.AvailableFreeSpace)
                            .FirstOrDefault();

                        return maxDrive == null
                            ? ""
                            : $"当前视频存储路径为系统盘C盘，是否前往设置更换成剩余容量最大的{maxDrive.Name.Substring(0, 1)}盘？";
                    }
                }
                else
                {
                    if (drives.Length > 2)
                    {
                        var maxDrive = drives
                            .Where(d => d.IsReady && d.Name != "C:\\")
                            .OrderByDescending(d => d.AvailableFreeSpace)
                            .FirstOrDefault();

                        if (maxDrive == null) return "";

                        if (config.SavePath.Substring(0, 1) != maxDrive.Name.Substring(0, 1))
                        {
                            return $"当前视频存储路径所在盘剩余可用空间较小，是否前往设置更换成剩余容量最大的{maxDrive.Name.Substring(0, 1)}盘？";
                        }
                    }
                }

                return "";
            }).ConfigureAwait(false);
        }

        /// <summary>
        /// 更新主程序（异步）
        /// </summary>
        [HttpGet("更新主程序", "/updateProgram")]
        public async Task UpdateProgramAsync()
        {
            if (ReplayHttpUtils.mainUpdateDownload)
            {
                throw new InvalidOperationException("程序正在下载最新的更新包，请稍后再更新");
            }

            await FileUtils.LogAsync("启动更新程序", "更新程序")
                .ConfigureAwait(false);
            //await Task.Run(FileUtils.StartUpdate)
            //    .ConfigureAwait(false);
        }

        [HttpGet("更新客户端版本", "/setClientVersion")]
        public async Task SetClientVersionAsync()
        {
            var configBll = new ConfigBll();
            await Task.Run(configBll.setClientVersion)
                .ConfigureAwait(false);
        }

        [HttpGet("打开开发模式", "/openDevelopmentMode")]
        public async Task<int> OpenDevelopmentModeAsync()
        {
            return await Task.Run(() =>
            {
                ReplayHttpUtils.developmentMode = 0;
                return 0;
            }).ConfigureAwait(false);
        }

        [HttpGet("关闭开发模式", "/closeDevMode")]
        public async Task<int> CloseDevModeAsync()
        {
            return await Task.Run(() =>
            {
                ReplayHttpUtils.developmentMode = 1;
                FormMain.webBrower?.CloseDevTools();
                return 1;
            }).ConfigureAwait(false);
        }

        [HttpGet("获取客户端的模式", "/getClientMode")]
        public async Task<int> GetClientModeAsync()
        {
            return await Task.Run(() => ReplayHttpUtils.developmentMode)
                .ConfigureAwait(false);
        }

        [HttpGet("打开谷歌的F12", "/openDevTools")]
        public async Task OpenDevToolsAsync()
        {
            await Task.Run(() =>
            {
                if (ReplayHttpUtils.developmentMode == 0)
                {
                    FormMain.webBrower?.ShowDevTools();
                }
            }).ConfigureAwait(false);
        }

        [HttpGet("获取账号密码", "/getUserObject")]
        public async Task<AccountPasswordVo> GetUserObjectAsync()
        {
            var configBll = new ConfigBll();
            return await Task.Run(configBll.getUserObject)
                .ConfigureAwait(false);
        }

        /// <summary>
        /// 保存账号密码（异步+参数校验）
        /// </summary>
        [HttpGet("保存账号密码", "/putUserObject")]
        public async Task PutUserObjectAsync(AccountPasswordVo vo)
        {
            // 参数校验
            if (vo == null)
                throw new ArgumentNullException(nameof(vo), "用户账号密码信息不能为空");
            if (string.IsNullOrWhiteSpace(vo.userName))
                throw new ArgumentException("用户名不能为空", nameof(vo.userName));

            try
            {
                var configBll = new ConfigBll();
                await Task.Run(() => configBll.putUserObject(vo))
                    .ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync($"保存用户信息失败：{ex.Message}", "PutUserObjectAsync")
                    .ConfigureAwait(false);
                throw;
            }
        }

        [HttpGet("设置获取websocket地址时是否使用代理", "/setSockectProxy")]
        public async Task SetSocketProxyAsync(int proxy)
        {
            await Task.Run(() => SocketAddressInit.isProxy = proxy == 1)
                .ConfigureAwait(false);
        }
        #endregion

        #region 资源释放
        public void Dispose()
        {
            // 取消定时任务
            _getUserCts?.Cancel();
            _getUserCts?.Dispose();
            // 关闭对话框
            CloseDialog();
        }
        #endregion
    }







    //private static FolderBrowserDialog _currentDialog;
    ///// <summary>
    ///// 是否开启定时获取用户信息
    ///// </summary>
    //public static bool getUserFlag = true;
    ///// <summary>
    ///// 获取用户失败的次数，超过10次通知前端弹窗
    ///// </summary>
    //public static volatile int getUserInfoErrorCount = 0;
    //private readonly AsyncLock _settokenAsyncLock = new AsyncLock();

    //[HttpGet("打印成pdf", "/pin")]
    //public void pin()
    //{
    //    FormMain.webBrower.PrintToPdfAsync("D:\\AiFuPan\\output.pdf", new PdfPrintSettings
    //    {
    //        // 这里可尝试根据实际情况调整ScaleFactor等参数 
    //        PaperWidth = 11.69, // 转换为英寸 
    //        PaperHeight = 8.27 // 转换为英寸 
    //    });
    //}

    ///// <summary>
    ///// 测试设置代理
    ///// </summary>
    ///// <returns></returns>
    //[HttpGet("测试设置代理", "/testSetProxy")]
    //public void testSetProxy()
    //{
    //    new Thread(async () =>
    //    {
    //        DouyinHttpClient webClient = DouyinHttpClient.Instance;
    //        // 使用代理
    //        webClient.UseProxyIP();

    //    }).Start();

    //}

    ///// <summary>
    ///// 测试通知前端抖音警告
    ///// </summary>
    ///// <returns></returns>
    //[HttpGet("测试通知前端抖音警告", "/tesTdouyinWarnStopRecord")]
    //public void tesTdouyinWarnStopRecord()
    //{

    //    // 通知前端弹窗
    //    try
    //    {
    //        ConfigBll configBll = new ConfigBll();
    //        Config config = configBll.GetModel();
    //        if (config.IsRocord == 1)
    //        {
    //            // 抖音已经发起警告，停止录制
    //            AnchorBll.StopDecectorAll();
    //        }

    //        // 通知前端上传
    //        FrontNotice frontNotice = new FrontNotice();
    //        var requestDataObj = new Dictionary<string, object>();
    //        requestDataObj["code"] = 0;
    //        requestDataObj["status"] = 200;
    //        requestDataObj["action"] = "douyinWarnStopRecord";
    //        frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
    //    }
    //    catch (Exception e)
    //    {
    //        FileUtils.LogAnalysis($"{e}", $"自动上传到云空间发生异常");
    //    }
    //}

    ///// <summary>
    ///// 测试通知前端打开更新弹窗
    ///// </summary>
    ///// <returns></returns>
    //[HttpGet("测试通知前端打开更新弹窗", "/openUpdateVersion")]
    //public void openUpdateVersion()
    //{
    //    // 通知前端弹窗
    //    try
    //    {
    //        ConfigBll configBll = new ConfigBll();
    //        Config config = configBll.GetModel();
    //        if (config.IsRocord == 1)
    //        {
    //            // 抖音已经发起警告，停止录制
    //            AnchorBll.StopDecectorAll();
    //        }
    //        // 通知前端上传
    //        FrontNotice frontNotice = new FrontNotice();
    //        Dictionary<string, object> result = new Dictionary<string, object>();
    //        result.Add("code", 0);
    //        result.Add("status", 200);
    //        result.Add("action", "openUpdate");
    //        Dictionary<string, object> data = new Dictionary<string, object>();
    //        data.Add("updateType", 0);
    //        data.Add("currentVersion", Constant.VERSION);
    //        data.Add("updateVersion", "2.4.0.test");
    //        data.Add("updateRemarks", "<p>1、新增弹幕录制功能；</p><p>2、优化录制功能；</p><p>3、xxxxxx；</p>");
    //        result.Add("data", data);
    //        frontNotice.NoticeJs(JsonConvert.SerializeObject(result));
    //    }
    //    catch (Exception e)
    //    {
    //        FileUtils.LogError($"{e}", $"测试通知前端打开更新弹窗发生异常");
    //    }
    //}

    //[HttpGet("打开更新弹窗", "/updateVersion")]
    //public void updateVersion()
    //{
    //    ReplayHttpUtils.openUpdateVersion(false);
    //}

    //[HttpGet("点击更新弹窗后打开更新弹窗", "/handUpdateVersion")]
    //public void handUpdateVersion()
    //{
    //    ReplayHttpUtils.allAreOpenUpdateVersion();
    //}


    ///// <summary>
    ///// 获取当前系统时间是否准确
    ///// </summary>
    ///// <returns></returns>
    //[HttpGet("获取当前系统时间是否准确", "/getTimeAccurate")]
    //public async Task<bool> getTimeAccurate()
    //{
    //    ServerTimeUtils.checkTimeAccurate();
    //    return await Task.Run(() => ServerTimeUtils.timeAccurate);
    //}


    ///// <summary>
    ///// 获取版本更新信息
    ///// </summary>
    ///// <returns></returns>
    //[HttpGet("获取版本更新信息", "/getVersionUpdate")]
    //public VersionUpdateVo getVersionUpdate()
    //{
    //    //FileUtils.log(JsonConvert.SerializeObject(ReplayHttpUtils.VersionVo));
    //    if (ReplayHttpUtils.VersionVo != null && !Constant.VERSION.Equals(ReplayHttpUtils.VersionVo.VersionNum))
    //    {
    //        return ReplayHttpUtils.VersionVo;
    //    }
    //    return null;
    //}

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
    //    return await Task.Run(() => this.getVersionUpdate());
    //}

    ///// <summary>
    ///// getUrl
    ///// </summary>
    ///// <returns></returns>
    //[HttpGet("getUrl", "/getUrl")]
    //public string getUrl()
    //{
    //    return Constant.GetApiBaseUrl() + Constant.GetWebServerUrl() + Constant.GetOnlineUrl();
    //}

    ///// <summary>
    ///// 更新token
    ///// </summary>
    ///// <returns></returns>
    //[HttpGet("更新token", "/settoken")]
    //public async Task SetToken(string token, string userId, long activeTenantId)
    //{
    //    ReplayHttpUtils.Token = token;
    //    ReplayHttpUtils.UserId = userId;
    //    ReplayHttpUtils.ActiveTenantId = activeTenantId;



    //    //线程安全检查：避免重复启动
    //    using (await _settokenAsyncLock.LockAsync())
    //    {
    //        if (FristPageIni._isRunning) return;
    //        FristPageIni._isRunning = true;
    //    }
    //    // 初始化取消令牌源
    //    FristPageIni._cts = new CancellationTokenSource();
    //    CancellationToken cancellationToken = FristPageIni._cts.Token;

    //    try
    //    {
    //        // 启动后台长时间任务（异步等待，不阻塞UI）
    //        await Task.Run(() => FristPageIni.LongRunningTask(cancellationToken), cancellationToken);
    //    }
    //    catch (OperationCanceledException ex)
    //    {
    //        await FileUtils.LogErrorAsync($"{ex}", $"FristPageIni捕获任务取消异常（正常停止）");
    //    }
    //    catch (Exception ex)
    //    {
    //        await FileUtils.LogErrorAsync($"{ex}", $"FristPageIni捕获其他异常");
    //    }
    //    finally
    //    {
    //        // 清理资源+重置状态
    //        using (await _settokenAsyncLock.LockAsync())
    //        {
    //            FristPageIni._isRunning = false;
    //        }
    //        FristPageIni._cts?.Dispose();
    //        FristPageIni._cts = null;

    //    }
    //}

    ///// <summary>
    ///// 同步服务器配置到客户端
    ///// </summary>
    //private void SyncServerConfigToClient()
    //{
    //    try
    //    {

    //    }
    //    catch (Exception ex)
    //    {

    //    }
    //}

    ///// <summary>
    ///// 同步本地视频、文件、对比到服务器
    ///// </summary>
    //private void SyncLocalDataToServer()
    //{
    //    try
    //    {
    //        string dbPath = Path.GetFullPath(@"DbFile/review_analysis.db");
    //        if (!File.Exists(dbPath))
    //        {
    //            return;
    //        }

    //        // 查询当前用户是否已经同步过数据到服务器
    //        bool result = VideoApi.CheckUserSyncLocalData();
    //        if (!result)
    //        {
    //            // 还没同步过数据
    //            Dictionary<string, object> param = new Dictionary<string, object>();
    //            List<string> videoIds = new List<string>();
    //            List<string> fileIds = new List<string>();
    //            List<string> contrastIds = new List<string>();

    //            // 获取本地视频
    //            AnchorVideo anchorVideo = new AnchorVideo();
    //            anchorVideo.UserId = ReplayHttpUtils.UserId;
    //            anchorVideo.TenantId = ReplayHttpUtils.ActiveTenantId;
    //            List<AnchorVideo> anchorVideoList = anchorVideo.GetList();
    //            if (anchorVideoList != null && anchorVideoList.Count > 0)
    //            {
    //                foreach (var video in anchorVideoList)
    //                {
    //                    if (video.DeleteStatus == 0 || video.UploadStatus == 1)
    //                    {
    //                        videoIds.Add(video.VideoId);
    //                    }
    //                }
    //            }

    //            // 获取本地文件
    //            UploadFile uploadFile = new UploadFile();
    //            uploadFile.UserId = ReplayHttpUtils.UserId;
    //            List<UploadFile> uploadFiles = uploadFile.GetList();
    //            if (uploadFiles != null && uploadFiles.Count > 0)
    //            {
    //                foreach (var file in uploadFiles)
    //                {
    //                    fileIds.Add(file.FileId);
    //                }
    //            }

    //            // 获取本地对比数据
    //            VideoContrast videoContrast = new VideoContrast();
    //            videoContrast.UserId = ReplayHttpUtils.UserId;
    //            videoContrast.TenantId = ReplayHttpUtils.ActiveTenantId;
    //            List<VideoContrast> videoContrasts = videoContrast.GetList();
    //            if (videoContrasts != null && videoContrasts.Count > 0)
    //            {
    //                foreach (var item in videoContrasts)
    //                {
    //                    if (videoContrast.DeleteStatus == 0 || videoContrast.IsShard == 1)
    //                    {

    //                        contrastIds.Add(item.ContrastId);
    //                    }
    //                }
    //            }

    //            param.Add("videoIds", videoIds);
    //            param.Add("fileIds", fileIds);
    //            param.Add("contrastIds", contrastIds);

    //            VideoApi.SyncLocalDataToServer(param);

    //        }
    //    }
    //    catch (Exception ex)
    //    {
    //        FileUtils.LogError($"{ex}", $"同步本地视频、文件、对比到服务器发生异常");
    //    }

    //}

    ///// <summary>
    ///// 定时获取用户信息
    ///// </summary>
    //private void ScheduledGetUser()
    //{
    //    while (getUserFlag)
    //    {
    //        try
    //        {
    //            Asr.UserVo userVo = UserApi.GetUserLoginInfo();
    //            if (userVo != null)
    //            {
    //                getUserInfoErrorCount = 0;
    //                ReplayHttpUtils.UserInfo = userVo;

    //                if (userVo.Status == 1)
    //                {
    //                    // 用户已被冻结
    //                    FileUtils.log($"{userVo.NickName}", $"用户被冻结");
    //                    userFrozen(null);

    //                }

    //                // 检查是否更换租户，如果更换了，就弹出登录页
    //                if (userVo.activeTenantId != null)
    //                {
    //                    long tenantId = (long)userVo.activeTenantId;
    //                    if (ReplayHttpUtils.ActiveTenantId != tenantId)
    //                    {
    //                        FileUtils.log($"{userVo.NickName}，oldActiveTenantId= {ReplayHttpUtils.ActiveTenantId}，newtenantId = {tenantId}", $"用户已更换租户");
    //                        userFrozen("主账号发生变动，请重新登录");
    //                    }

    //                }

    //            }
    //            else
    //            {
    //                ReplayHttpUtils.UserInfo = null;

    //                getUserInfoErrorCount++;
    //                if (getUserInfoErrorCount >= 10)
    //                {
    //                    // 通知前端弹出断网提示
    //                    FrontNotice frontNotice = new FrontNotice();
    //                    var requestDataObj = new Dictionary<string, object>();
    //                    requestDataObj["code"] = 0;
    //                    requestDataObj["status"] = 200;
    //                    requestDataObj["action"] = "notice";
    //                    var data = new Dictionary<string, object>();
    //                    data["alertType"] = 1;
    //                    data["statusType"] = "warning";
    //                    data["title"] = "断网提示";
    //                    data["msg"] = $"网络已中断超过1分钟，可能会导致录制中断或其他故障，请确保网络的稳定性~提示时间：{ServerTimeUtils.getCurrentTimeStr()}";
    //                    data["duration"] = 0;
    //                    requestDataObj["data"] = data;
    //                    frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
    //                }
    //            }

    //        }
    //        catch (Exception ex)
    //        {
    //            FileUtils.LogError($"{ex}", $"定时获取当前登录的用户信息请求失败");
    //        }
    //        Thread.Sleep(8000);
    //    }
    //}

    ///// <summary>
    ///// 跳出到登录页
    ///// </summary>
    ///// <param name="msg"></param>
    //public void userFrozen(string msg)
    //{
    //    try
    //    {
    //        // 停止自动分析
    //        AnchorVideoBll.autoAnalysis = false;
    //        // 停止继续获取用户信息
    //        getUserFlag = false;
    //        // 停止录制
    //        AnchorBll.StopDecectorAll();

    //    }
    //    catch (Exception ex)
    //    {
    //        FileUtils.LogError($"{ex}", $"用户已经更换租户id，停止录制时发生异常");
    //    }

    //    // 退出登录
    //    try
    //    {
    //        UserApi.logout();
    //        ReplayHttpUtils.Token = null;
    //    }
    //    catch (Exception ex) { }
    //    // 通知前端弹窗
    //    FrontNotice frontNotice = new FrontNotice();
    //    var requestDataObj = new Dictionary<string, object>();
    //    requestDataObj["code"] = 0;
    //    requestDataObj["status"] = 200;
    //    requestDataObj["action"] = "userFrozen";
    //    requestDataObj["data"] = new { msg };
    //    frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
    //}



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


    ///// <summary>
    ///// 获取配置
    ///// </summary>
    ///// <returns></returns>
    //[HttpGet("获取客户端配置", "/getmodel")]
    //public async Task<Config> GetModel()
    //{
    //    ConfigBll configBll = new ConfigBll();
    //    return await Task.Run(() => configBll.GetModel());
    //}


    ///// <summary>
    ///// 更新客户端配置
    ///// </summary>
    ///// <param name="config"></param>

    //[HttpPost("更新客户端配置", "/updatemodel")]
    //public async Task Update(Config config)
    //{
    //    ConfigBll configBll = new ConfigBll();

    //    await Task.Run(() => configBll.updateModel(config));
    //}

    //[HttpGet("打开文件选择器", "/checkfilebox")]
    //public string CheckFileSavePath()
    //{
    //    _currentDialog = null; // 清除引用
    //    _currentDialog = new FolderBrowserDialog
    //    {
    //        Description = "请选择一个文件夹",
    //        ShowNewFolderButton = true
    //    };
    //    DialogResult result = _currentDialog.ShowDialog();
    //    if (result == DialogResult.OK && !string.IsNullOrWhiteSpace(_currentDialog.SelectedPath))
    //    {
    //        return _currentDialog.SelectedPath;
    //    }
    //    else
    //    {
    //        return null;
    //    }
    //}

    //[HttpGet("关闭文件选择器", "/closedialog")]
    //public static void CloseDialog()
    //{
    //    if (_currentDialog != null)
    //    {
    //        _currentDialog.Dispose(); // 关闭对话框
    //        _currentDialog = null; // 清除引用
    //    }
    //}

    //[HttpGet("获取当前磁盘大小", "/getdisksize")]
    //public Dictionary<string, long> GetDiskSize()
    //{
    //    //获取盘符
    //    // 获取当前执行文件的完整路径
    //    //string exePath = System.Reflection.Assembly.GetExecutingAssembly().Location;

    //    //// 获取当前执行文件所在的盘符，包括冒号
    //    //string driveWithColon = Path.GetPathRoot(exePath);

    //    //// 去除冒号，仅保留盘符字母
    //    //string driveLetter = driveWithColon.Substring(0, 1);

    //    ConfigBll configBll = new ConfigBll();
    //    Config config = configBll.GetModel();
    //    string savePath = config.SavePath;
    //    string driveLetter = Path.GetPathRoot(savePath).Substring(0, 1);
    //    return FileUtils.GetDiskSize(driveLetter);
    //}

    //[HttpGet("返回剩余空间容量最大的盘符", "/getmaxdisk")]
    //public string GetMaxDisk()
    //{
    //    // 获取所有逻辑驱动器
    //    DriveInfo[] drives = DriveInfo.GetDrives();

    //    ConfigBll configBll = new ConfigBll();
    //    Config config = configBll.GetModel();

    //    if (config.SavePath.StartsWith("C"))
    //    {
    //        // 当前存储路径在C盘
    //        if (drives.Length > 1)
    //        {
    //            // 有2个盘或以上，提示放在除C盘外最大的盘
    //            var driveWith = drives
    //                .Where(d => d.IsReady && d.Name != "C:\\") // 只考虑就绪的驱动器且不是C盘
    //                .OrderByDescending(d => d.AvailableFreeSpace) // 根据可用空间降序排列
    //                .FirstOrDefault(); // 获取第一个（也就是可用空间最大的）驱动器
    //            if (driveWith == null) return "";
    //            return "当前视频存储路径为系统盘C盘，是否前往设置更换成剩余容量最大的" + driveWith.Name.Substring(0, 1) + "盘？";
    //        }
    //    }
    //    else
    //    {
    //        // 当前存储在其他盘
    //        if (drives.Length > 2)
    //        {
    //            // 有3个盘或以上，提示放在除C盘外最大的盘
    //            var driveWith = drives
    //                .Where(d => d.IsReady && d.Name != "C:\\") // 只考虑就绪的驱动器且不是C盘
    //                .OrderByDescending(d => d.AvailableFreeSpace) // 根据可用空间降序排列
    //                .FirstOrDefault(); // 获取第一个（也就是可用空间最大的）驱动器

    //            if (driveWith == null) return "";
    //            if (config.SavePath.Substring(0, 1) != driveWith.Name.Substring(0, 1))
    //            {
    //                return "当前视频存储路径所在盘剩余可用空间较小，是否前往设置更换成剩余容量最大的" + driveWith.Name.Substring(0, 1) + "盘？";
    //            }
    //        }
    //    }

    //    return "";

    //}

    //[HttpGet("更新主程序", "/updateProgram")]
    //public void UpdateProgram()
    //{
    //    if (ReplayHttpUtils.mainUpdateDownload)
    //    {
    //        throw new Exception("程序正在下载最新的更新包，请稍后再更新");
    //    }
    //    FileUtils.log("启动更新程序", "启动更新程序");
    //    FileUtils.StartUpdate();
    //}

    //[HttpGet("更新客户端版本", "/setClientVersion")]
    //public void setClientVersion()
    //{
    //    ConfigBll configBll = new ConfigBll();
    //    configBll.setClientVersion();
    //}

    //[HttpGet("打开开发模式", "/openDevelopmentMode")]
    //public int openDevelopmentMode()
    //{
    //    ReplayHttpUtils.developmentMode = 0;
    //    return 0;
    //}

    //[HttpGet("关闭开发模式", "/closeDevMode")]
    //public int closeDevMode()
    //{
    //    ReplayHttpUtils.developmentMode = 1;
    //    FormMain.webBrower?.CloseDevTools();
    //    return 1;
    //}

    //[HttpGet("获取客户端的模式", "/getClientMode")]
    //public async Task<int> getClientMode()
    //{
    //    return await Task.Run(() => ReplayHttpUtils.developmentMode);
    //}

    //[HttpGet("打开谷歌的F12", "/openDevTools")]
    //public void openDevTools()
    //{
    //    if (ReplayHttpUtils.developmentMode == 0)
    //    {
    //        FormMain.webBrower?.ShowDevTools();
    //    }
    //}


    //[HttpGet("获取账号密码", "/getUserObject")]
    //public async Task<AccountPasswordVo> getUserObject()
    //{
    //    ConfigBll configBll = new ConfigBll();
    //    return await Task.Run(() => configBll.getUserObject());
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

    //[HttpGet("设置获取websocket地址时是否使用代理", "/setSockectProxy")]
    //public void setSockectProxy(int proxy)
    //{
    //    SocketAddressInit.isProxy = proxy == 1;
    //}
}
