using CefSharp;
using CefSharp.WinForms;
using douyin.Utils; // 确保这个命名空间被引用
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.BackgroundWork;
using ReviewAnalysis.BeanCache;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Db;
using ReviewAnalysis.Global;
using ReviewAnalysis.HttpServer;
using ReviewAnalysis.juliang;
using ReviewAnalysis.life;
using ReviewAnalysis.enterprise;
using ReviewAnalysis.anchorLive;
using ReviewAnalysis.DouyinAuth;
using ReviewAnalysis.juliangApi;
using ReviewAnalysis.LogConsole;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Websocket;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Security.Principal;
using System.Threading;
using System.Threading.Tasks;
using System.Timers;
using System.Web.UI;
using System.Windows.Forms;
using ReviewAnalysis.plugins.core;
using Message = System.Windows.Forms.Message;

namespace ReviewAnalysis
{
    public partial class FormMain : Form
    {
        private bool isDragging = false;
        private Point dragCursorPoint;
        private Point dragFormPoint;

        private SQLiteHelper sqliteHelper;

        private HttpListenerServer httpListenerServer;

        private bool isReStart = false;

        private bool isClose = false;

        private bool _isShutdownInProgress = false;

        public static ChromiumWebBrowser webBrower;

        // 新架构：平台采集调度器
        private ReviewAnalysis.plugins.core.CollectionScheduler _collectionScheduler;
        public ReviewAnalysis.plugins.core.CollectionScheduler CollectionScheduler => _collectionScheduler;
        private ReviewAnalysis.plugins.upload.PlatformUploadScheduler _uploadScheduler;
        private ReviewAnalysis.plugins.core.SupplementalDataCollectionScheduler _supplementCollectScheduler;
        /// <summary>
        /// 主播下播补采集调度器（供测试接口等外部代码访问，用于手动触发补采集）
        /// </summary>
        public ReviewAnalysis.plugins.core.SupplementalDataCollectionScheduler SupplementCollectScheduler => _supplementCollectScheduler;
        private bool isFrontNoticeRegistered = false;
        private bool isReloadOpenUpdate = false;
        public static Color defaultTitleBarTopColor = SystemColors.GradientActiveCaption;
        public static int minW = 1494, minH = 840;
        public static double scaleRatio = 1.0;
        public static string icoPath = "favilogo1.ico";
        private HttpListenerAsyncServer _httpServer;

        //private CefBackgroundManager _cefManager;

        /// <summary>
        /// 打开巨量百应授权页面或加载数据大屏页面
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="roomId">直播间id</param>
        /// <param name="videoId">视频id</param>
        /// <param name="isTemp">是否临时加载，如果是临时的，同时需要加载专业版，并且加载完后需要关闭</param>
        public void openAuthorizeOrDataScreen(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp)
        {
            //this.Invoke((MethodInvoker)delegate
            //{
            //    JuliangUtils.openAuthorizeOrDataScreen(anchorInfo, roomId, videoId, isTemp, false, 0);
            //});
            //三重校验：控件不为null + 未被销毁 + 句柄已创建
            if (this == null || this.IsDisposed || !this.IsHandleCreated)
            {
                return; // 直接返回，避免抛异常
            }

            try
            {
                if (this.InvokeRequired)
                {
                    this.Invoke((MethodInvoker)delegate
                    {
                        JuliangUtils.openAuthorizeOrDataScreen(anchorInfo, roomId, videoId, isTemp, false, 0);
                    });
                }
                else
                {
                    // 已在 UI 线程，直接调用
                    JuliangUtils.openAuthorizeOrDataScreen(anchorInfo, roomId, videoId, isTemp, false, 0);
                }
            }
            catch (InvalidOperationException ex)
            {
                // 句柄已释放（如窗体关闭），静默忽略（也可记录日志）
                FileUtils.LogRpaAsync($"{ex}", "打开巨量百应");
            }

        }

        private static bool IsRunAsAdministrator()
        {
            WindowsIdentity identity = WindowsIdentity.GetCurrent();
            WindowsPrincipal principal = new WindowsPrincipal(identity);

            return principal.IsInRole(WindowsBuiltInRole.Administrator);
        }
        
        ///// <summary>
        ///// 启动http端口监听
        ///// </summary>
        ///// <returns></returns>
        //private async Task<int> startHttpListenerServer()
        //{
        //    int prot = getLocalhostPort(5001, 20);
        //    string listenrUrl = $"http://127.0.0.1:{prot}/";
        //    //httpListenerServer = new HttpListenerServer(this, listenrUrl);
        //    //httpListenerServer.HttpListenerRequest();
        //    //_ = httpListenerServer.run();
        //    _httpServer = new HttpListenerAsyncServer(this, listenrUrl);
        //    _httpServer.StartHttpListener(); // 启动监听器
        //    _ = _httpServer.RunAsync(); // 异步运行监听循环
        //    return await Task.FromResult(prot);
        //}
        private async Task<int> StartHttpListenerServerAsync(CancellationToken cancellationToken = default)
        {
            const int invalidPort = -1;
            const int basePort = 5001;
            const int portTryCount = 20;

            if (cancellationToken.IsCancellationRequested)
            {
                await FileUtils.LogAsync("HTTP服务器启动请求已取消", "HTTP服务器启动");
                return invalidPort;
            }

            for (int attempt = 0; attempt < portTryCount; attempt++)
            {
                int port = basePort + attempt;
                if (port < 1 || port > 65535) continue;

                string listenerUrl = $"http://127.0.0.1:{port}/";
                await FileUtils.LogAsync($"尝试启动HTTP服务器，监听地址：{listenerUrl}", "HTTP服务器启动");

                var httpServer = new HttpListenerAsyncServer(this, listenerUrl, cancellationToken);
                try
                {
                    httpServer.StartHttpListener();
                }
                catch (CustomException ex)
                {
                    await FileUtils.LogErrorAsync($"端口 {port} 启动失败: {ex.Message}，尝试下一个端口...", "HTTP服务器启动");
                    httpServer.Dispose();
                    continue;
                }
                catch (HttpListenerException ex)
                {
                    await FileUtils.LogErrorAsync($"端口 {port} HttpListener异常（错误码:{ex.ErrorCode}）: {ex.Message}，尝试下一个端口...", "HTTP服务器启动");
                    httpServer.Dispose();
                    continue;
                }
                catch (Exception ex)
                {
                    await FileUtils.LogErrorAsync($"端口 {port} 启动异常: {ex}，尝试下一个端口...", "HTTP服务器启动");
                    httpServer.Dispose();
                    continue;
                }

                _ = Task.Run(async () =>
                {
                    try
                    {
                        await httpServer.RunAsync().ConfigureAwait(false);
                    }
                    catch (OperationCanceledException)
                    {
                    }
                    catch (Exception ex)
                    {
                        await FileUtils.LogErrorAsync($"HTTP监听循环异常：{ex.Message}", "HTTP服务器运行");
                    }
                    finally
                    {
                        httpServer.Dispose();
                    }
                }, cancellationToken);

                _httpServer = httpServer;
                await FileUtils.LogAsync($"HTTP服务器启动成功，监听端口：{port}", "HTTP服务器启动");
                return port;
            }

            await FileUtils.LogErrorAsync("所有端口（5001-5020）全部启动失败", "HTTP服务器启动");
            return invalidPort;
        }
        private CancellationTokenSource _cts = new CancellationTokenSource(); // 取消令牌源
        async Task<int> MainIni()
        {
            //无感知安装依赖组件和运行系统开始的bat文件
            await Task.Run(() => ZuJian());
            Task<bool> taskIsRunAsAdministrator = Task.Run(() => IsRunAsAdministrator());
            if (!await taskIsRunAsAdministrator)
            {
                MessageBox.Show(this, "您不具备有超级管理员权限,这将会导致更新无法操作或者CPU上涨,请您右键,使用超级管理员权限运行", "提示", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            await Task.Run(() => AddFirewall.ManageFirewallRulesAsync());
            //int port = await startHttpListenerServer();
            int port = await StartHttpListenerServerAsync();
            if (port == -1)
            {
                MessageBox.Show("HTTP服务器启动失败！", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
                Application.Exit();
                return 25635;
            }
            //初始化谷歌引擎 - 直接调用，不使用Task.Run，确保在正确的线程上初始化
            await taskInitCef(port);
            await FileUtils.LogAsync("加载谷歌引擎成功");
            //先检测是否有更新软件（ReviewAnalysisUpdate.exe）的文件更新
            await Task.Run(() =>
            {
                FileUtils.UpdateReviewAnalysis();
            });
            // 是否要强制更新
            await Task.Run(() =>
            {
                forceUpdate();
            });
            await Task.Run(() =>
            {
                CheckInit();
            });
            //杀死ffmpeg进程
            await Task.Run(() =>
            {
                KillFFmpegProcesses();
            });
            //杀死websocket进程
            await Task.Run(() =>
            {
                OperationAnchorBll.KillBarrageGrabProcesses();
            });
            //启动的时候将bean存入缓存
            await Task.Run(() =>
            {
                MethodCache.Initialize();
            });

            await MethodAsyncCache.InitializeAsync(this, _cts.Token);
            //FileUtils.log("启动的时候将bean存入缓存");

            //创建数据库
            //if (!CreateSql())
            //{
            //    MessageBox.Show("sql的配置文件没有正确的配置内容");
            //    return;
            //}
            //FileUtils.log("创建数据库");

            //执行需要更新的sql脚本
            //ExecuteSqlUpdate();
            //FileUtils.log("设置保存视频的初始文件目录");

            // 初始化系统配置文件
            //ConfigBll configBll = new ConfigBll();
            //// 将SQLite的配置信息写到配置文件
            //configBll.readSQLiteConfigWriteFile();
            //// 初始化配置信息
            //configBll.startClientInitConfig();
            await Task.Run(() => ClientInit());

            // 初始化视频表和文件表分析状态，将分析中的改成分析失败
            //initVideoAndFileAnalysisStatus();
            //初始化主播和视频表
            //initDataCache();

            //启动一个新线程检测缓存和队列，然后同步数据到数据库
            //Thread threadCache = new Thread(() => SetDataBaseFormCache());
            //threadCache.Start();


            // 开启定时检测本地时间是否准确
            //FileUtils.log("开启定时检测本地时间是否准确");
            System.Timers.Timer aTimer = new System.Timers.Timer(1000 * 60 * 1); // 设置间隔时间为30分钟
            aTimer.Elapsed += OnTimedEvent;
            aTimer.AutoReset = true;
            aTimer.Enabled = true;

            // 定时写入弹幕数据
            //System.Timers.Timer aTimerDanMu = new System.Timers.Timer(1000 *30 * 1); // 设置间隔时间为30分钟
            //aTimerDanMu.Elapsed += DanMuTimedEvent;
            //aTimerDanMu.AutoReset = true;
            //aTimerDanMu.Enabled = true;

            await FileUtils.LogAsync("执行脚本，关闭windows安全检测");
            //执行脚本，关闭windows安全检测
            await Task.Run(() =>
            {
                DefenderExclusionsManager.DisableDefender();
            });

            await FileUtils.LogAsync("杀掉端口");
            // 如果端口被占用，杀掉端口
            await Task.Run(() =>
            {
                CheckAndKillPortProcess(45001);
            });

            return port;
        }
        /// <summary>
        /// 初始化配置信息
        /// </summary>
        private void ClientInit()
        {
            // 初始化系统配置文件
            ConfigBll configBll = new ConfigBll();
            // 将SQLite的配置信息写到配置文件
            configBll.readSQLiteConfigWriteFile();
            // 初始化配置信息
            configBll.startClientInitConfig();
        }
        public FormMain()
        {
            //监听本地资源 - 异步初始化避免阻塞UI线程
            Task.Run(async () =>
            {
                int port = await MainIni();
                FileUtils.log($"HTTP服务器端口: {port}");
            }).GetAwaiter().GetResult(); // 恢复同步等待,确保CEF初始化完成

            // 设置宽高
            float dpiRatio = ScreenUtils.GetDpiRatio();
            minW = (int)(minW * dpiRatio);
            minH = (int)(minH * dpiRatio);
            //FileUtils.log($"{minW}-{minH}", $"计算后的预设窗体宽高"); // 2509*1411

            scaleRatio = ScreenUtils.GetScaleRatio(minW, minH);
            var minSize = ScreenUtils.GetMinimumSize(minW, minH);
            //var clientSize = ScreenUtils.GetClientSize(minW, minH, 0.75);


            // 设置窗体最小尺寸，防止用户调整到过小
            this.MinimumSize = new Size(minSize.Width, minSize.Height);
            this.ClientSize = new Size(minSize.Width, minSize.Height);
            //this.ClientSize = new Size(clientSize.Width, clientSize.Height);


            // 是否进入开发者模式
            confirmMode();

            //杀死更新进程
            var ReviewAnalysisUpdateProcess = Process.GetProcessesByName("ReviewAnalysisUpdate");
            foreach (var process in ReviewAnalysisUpdateProcess)
            {
                process.Kill();
                process.WaitForExit();
            }
            // 创建uuid
            CreateUUID();
            FileUtils.log($"cpuid={ReplayHttpUtils.uuid}");
            // 获取guid-在同一个系统中是不会变的
            GetCpuId();
            FileUtils.log($"cpuid={ReplayHttpUtils.cpuid}");

            // 添加h5到谷歌浏览器打开
            this.CreatePage();
            FileUtils.log("添加h5到谷歌浏览器打开");
            // 启用双缓冲以减少闪烁
            this.DoubleBuffered = true;

            FormUtils.SetFormValue(this);
            //启动的时候将bean存入缓存
            //MethodCache.Initialize();
            ////FileUtils.log("启动的时候将bean存入缓存");

            //创建数据库
            //if (!CreateSql())
            //{
            //    MessageBox.Show("sql的配置文件没有正确的配置内容");
            //    return;
            //}
            //FileUtils.log("创建数据库");

            //执行需要更新的sql脚本
            //ExecuteSqlUpdate();
            //FileUtils.log("设置保存视频的初始文件目录");

            // 初始化系统配置文件
            //ConfigBll configBll = new ConfigBll();
            //// 将SQLite的配置信息写到配置文件
            //configBll.readSQLiteConfigWriteFile();
            //// 初始化配置信息
            //configBll.startClientInitConfig();
            //Task.Run(()=> ClientInit());

            //// 初始化视频表和文件表分析状态，将分析中的改成分析失败
            ////initVideoAndFileAnalysisStatus();
            ////初始化主播和视频表
            ////initDataCache();

            ////启动一个新线程检测缓存和队列，然后同步数据到数据库
            ////Thread threadCache = new Thread(() => SetDataBaseFormCache());
            ////threadCache.Start();


            //// 开启定时检测本地时间是否准确
            //FileUtils.log("开启定时检测本地时间是否准确");
            //System.Timers.Timer aTimer = new System.Timers.Timer(1000 * 60 * 1); // 设置间隔时间为30分钟
            //aTimer.Elapsed += OnTimedEvent;
            //aTimer.AutoReset = true;
            //aTimer.Enabled = true;
            // 初始化服务器时间偏移（一次请求，后续本地时间+偏移）
            ServerTimeUtils.RefreshServerTimeOffset();
            new Thread(() => ServerTimeUtils.UpdateTimeAccurate()).Start();

            //// 定时写入弹幕数据
            ////System.Timers.Timer aTimerDanMu = new System.Timers.Timer(1000 *30 * 1); // 设置间隔时间为30分钟
            ////aTimerDanMu.Elapsed += DanMuTimedEvent;
            ////aTimerDanMu.AutoReset = true;
            ////aTimerDanMu.Enabled = true;

            //FileUtils.log("执行脚本");
            ////执行脚本，关闭windows安全检测
            //DefenderExclusionsManager.DisableDefender();
            //FileUtils.log("杀掉端口");
            //// 如果端口被占用，杀掉端口
            //CheckAndKillPortProcess(45001);

            InitializeComponent();
            this.Text = "爱复盘";

            // 设置窗体居中显示
            //this.StartPosition = FormStartPosition.CenterScreen;
            this.StartPosition = FormStartPosition.Manual;
            this.Left = (Screen.PrimaryScreen.WorkingArea.Width - this.Width) / 2;
            this.Top = (Screen.PrimaryScreen.WorkingArea.Height - this.Height) / 2;

            Icon = new Icon(FormMain.icoPath);
            //FileUtils.log("启动websocket数据采集");

            //FileUtils.log("窗体关闭监听");
            // 窗体关闭监听
            this.FormClosing += new FormClosingEventHandler(MainForm_FormClosing);
            this.FormClosed += new FormClosedEventHandler(MainForm_FormClosed);
            //FileUtils.log("阻止屏幕关闭");

            // 阻止屏幕关闭
            SystemSleep.PreventSleep();

            // 开启消费巨量实时数据队列
            JuliangDataHandle.startConsumeQueue();
            // 开启定时关闭超时巨量临时窗体
            JuliangUtils.closeTimeoutTempForm();

            JuliangUtils.formMain = this;
            ReviewAnalysis.anchorLive.AnchorLiveUtils.formMain = this;
            ReviewAnalysis.life.LifeUtils.formMain = this;

            // 标题栏事件绑定
            this.titleBar.MouseDown += new MouseEventHandler(this.titleBar_MouseDown);
            this.titleBar.MouseMove += new MouseEventHandler(this.titleBar_MouseMove);
            this.titleBar.MouseUp += new MouseEventHandler(this.titleBar_MouseUp);
            this.titleBar.DoubleClick += new EventHandler(this.titleBar_DoubleClick);

            this.btnMin.Click += new EventHandler(this.btnMin_Click);
            this.btnMax.Click += new EventHandler(this.btnMax_Click);
            this.btnClose.Click += new EventHandler(this.btnClose_Click);

            // 窗体大小改变事件
            this.SizeChanged += new EventHandler(this.Form1_SizeChanged);

            // 加载按钮图片
            LoadButtonImages();

            // 初始化动态尺寸
            InitializeDynamicSizes();

            // 初始化按钮位置
            UpdateButtonPositions();

            this.DoubleBuffered = true; // 启用双缓冲减少闪烁

            // 初始化拉伸功能
            InitializeResizeTimer();

            // 每天定时删除超过90天的本地mp4视频
            //Task.Run(() => {
            //    AnchorVideoBll.DeleteAgoLocalVideo();
            //});

            // 初始化CPP录制

            Task.Run(() =>
            {
                CppRecordUtils.Init();
            });

            // 注：主播上线检测已移到登录成功后调用（ConfigController.SetToken）
            // StartAllDetectionOnStartup();

            // 初始化新架构组件
            InitializeNewArchitecture();

            //TencentCosUtils.upload("C:\\Users\\Administrator\\Desktop\\1hour.mp3", Guid.NewGuid().ToString() + ".mp3");
            //AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            //anchorVideoBll.LockCloudAnalysis("fd4a152d-1aef-404c-8c84-46be5d912127");
            //VideoContrastBll videoContrastBll = new VideoContrastBll();
            //videoContrastBll.lockCloudContrast("e87c170b-1cb1-492d-ab81-b1c347f6a100");


            // 启动代理
            //const string regKey = @"HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Internet Settings";
            //Registry.SetValue(regKey, "ProxyServer", $"1.84.189.132:40030");
            //Registry.SetValue(regKey, "ProxyEnable", 1, RegistryValueKind.DWord);

            //// 禁用代理
            //const string regKey = @"HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Internet Settings";
            //Registry.SetValue(regKey, "ProxyServer", $"");
            //Registry.SetValue(regKey, "ProxyEnable", 0, RegistryValueKind.DWord);

            //// 通知系统代理设置已更改 
            //WinInetHelper.InternetSetOption(IntPtr.Zero, 39, IntPtr.Zero, 0);
            //// 强制刷新所有网络进程的配置
            //WinInetHelper.InternetSetOption(IntPtr.Zero, 37, IntPtr.Zero, 0);

            //DouyinHttpClient httClient = DouyinHttpClient.Instance;
            //httClient.SetProxy("112.84.21.97", "40015", "6khwc7", "en1mwp9a");
            //var result = Task.Run(() => httClient.GetHtmlTextAsync("https://ipinfo.io/json")).Result;
            ////result = Task.Run(() => httClient.GetHtmlTextAsync("https://ipinfo.io/json")).Result;
            ////result = Task.Run(() => httClient.GetHtmlTextAsync("https://ipinfo.io/json")).Result;
            ////httClient.SetProxy("", "");
            ////result = Task.Run(() => httClient.GetHtmlTextAsync("https://ipinfo.io/json")).Result;
            ////result = Task.Run(() => httClient.GetHtmlTextAsync("https://ipinfo.io/json")).Result;
            //FileUtils.log(result);

            //string videoPath = @"D:\workSpace\visualStudio\ReviewAnalysis-new\bin\Debug\download\东方甄选\20250415\东方甄选_2025年04月15日10时43分10秒_第1段.ts";
            //string targetPath = @"D:\workSpace\visualStudio\ReviewAnalysis-new\bin\Debug\download\东方甄选\20250415\mp4\东方甄选_2025年04月15日10时43分10秒_第1段.ts.mp4";
            //new Thread(() => VideoUtils.RepairVideo(videoPath, targetPath)).Start();



            //JuliangForm myForm = new JuliangForm();
            //myForm.Tag = "001";//设置标识，用于区分是哪个采集窗口返回的数据。
            //myForm.Location = new Point(0, 0);
            //myForm.Text = "窗体标题";
            //myForm.DataCollectReached += OnDataCollectReached;
            //myForm.MinimizeBox = false;
            //myForm.MaximizeBox = false;
            //myForm.Show();

            //myForm.openLogin();

            //AnchorInfoWebClient instance = AnchorInfoWebClient.Instance;
            //instance.GetLiveInfoByAnchorNumber("dongfangzhenxuan");

            //VideoUtils.SliceVideo(
            //    @"D:\workSpace\visualStudio\ReviewAnalysis-new\bin\Debug\download\东方甄选\20251022\东方甄选_2025年10月22日14时24分45秒_第1段.ts",
            //    @"D:\workSpace\visualStudio\ReviewAnalysis-new\bin\Debug\download\东方甄选\20251022\test_slice.ts",
            //    10000,
            //    60 * 10 * 1000
            //    );

            // 添加调试信息

            Task.Run(() =>
            {
                LogButtonSizes();
            });

            bool flag = SystemUtils.IsVCRedistInstalled();
            if (flag)
            {
                //_cefManager = new CefBackgroundManager(cePort: port, ceflag: flag);
                //// 后台初始化CEF，不阻塞UI
                //_cefManager.InitializeCefInBackground();
            }
            else
            {
                DialogResult result = MessageBox.Show("当前系统缺少必要的运行库，是否现在安装。安装完成后请重新打开爱复盘。", "提示", MessageBoxButtons.YesNo, MessageBoxIcon.None);
                if (result == DialogResult.Yes)
                {
                    FileUtils.log("准备安装VC_redist.x64库");
                    string vcPath = Path.GetFullPath("tools\\VC_redist.x64.exe");
                    if (File.Exists(vcPath))
                    {
                        // 启动安装程序
                        try
                        {
                            Process process = new Process();
                            process.StartInfo.FileName = vcPath;
                            process.StartInfo.Arguments = ""; // 安装参数
                            process.StartInfo.Verb = "runas";  // 提升为管理员权限
                            //process.StartInfo.WindowStyle = ProcessWindowStyle.Hidden; // 隐藏窗口
                            try
                            {
                                // 启动进程
                                process.Start();
                                FileUtils.log("安装程序已启动...");
                                this.Close();
                                Environment.Exit(666);
                            }
                            catch (Exception ex)
                            {
                                FileUtils.log("发生错误: " + ex.Message);
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.log($"启动失败: {ex.Message}");
                        }
                    }
                    else
                    {
                        MessageBox.Show("没有检测到VC_redist.x64库，请您手动下载安装或者联系管理员");
                        this.Close();
                        Environment.Exit(666);
                    }
                }
                else if (result == DialogResult.No)
                {
                    FileUtils.log("用户不安装VC_redist.x64库,程序退出中。。。");
                    this.Close();
                    Environment.Exit(666);
                }
            }

            FileUtils.log("启动爱复盘完成");
        }
        //public FormMain()
        //{
        //    // 检查是否以管理员身份运行
        //    if (!IsRunAsAdministrator())
        //    {
        //        MessageBox.Show("您不具备有超级管理员权限,这将会导致更新无法操作或者CPU上涨,请您右键,使用超级管理员权限运行");
        //    }
        //    AddFirewall.ManageFirewallRulesAsync();

        //    //无感知安装依赖组件和运行系统开始的bat文件
        //    Thread threadZujian = new Thread(() => ZuJian());
        //    threadZujian.Start();

        //    //监听本地资源
        //    int port = startHttpListenerServer();
        //    FileUtils.log("开始加载谷歌引擎");
        //    //初始化谷歌引擎
        //    this.initCef(port);
        //    FileUtils.log("加载谷歌引擎成功");

        //    // 设置宽高
        //    float dpiRatio = ScreenUtils.GetDpiRatio();
        //    minW = (int)(minW * dpiRatio);
        //    minH = (int)(minH * dpiRatio);
        //    FileUtils.log($"{minW}-{minH}", $"计算后的预设窗体宽高"); // 2509*1411

        //    scaleRatio = ScreenUtils.GetScaleRatio(minW, minH);
        //    var minSize = ScreenUtils.GetMinimumSize(minW, minH);
        //    //var clientSize = ScreenUtils.GetClientSize(minW, minH, 0.75);


        //    // 设置窗体最小尺寸，防止用户调整到过小
        //    this.MinimumSize = new Size(minSize.Width, minSize.Height);
        //    this.ClientSize = new Size(minSize.Width, minSize.Height);
        //    //this.ClientSize = new Size(clientSize.Width, clientSize.Height);


        //    // 是否进入开发者模式
        //    confirmMode();

        //    //杀死更新进程
        //    var ReviewAnalysisUpdateProcess = Process.GetProcessesByName("ReviewAnalysisUpdate");
        //    foreach (var process in ReviewAnalysisUpdateProcess)
        //    {
        //        process.Kill();
        //        process.WaitForExit();
        //    }
        //    // 创建uuid
        //    CreateUUID();
        //    FileUtils.log($"cpuid={ReplayHttpUtils.uuid}");
        //    // 获取guid-在同一个系统中是不会变的
        //    GetCpuId();
        //    FileUtils.log($"cpuid={ReplayHttpUtils.cpuid}");

        //    //先检测是否有更新软件（ReviewAnalysisUpdate.exe）的文件更新
        //    FileUtils.UpdateReviewAnalysis();


        //    // 是否要强制更新
        //    this.forceUpdate();

        //    CheckInit();

        //    //杀死ffmpeg进程
        //    KillFFmpegProcesses();
        //    //杀死websocket进程
        //    OperationAnchorBll.KillBarrageGrabProcesses();
        //    // 添加h5到谷歌浏览器打开
        //    this.CreatePage();
        //    FileUtils.log("添加h5到谷歌浏览器打开");
        //    // 启用双缓冲以减少闪烁
        //    this.DoubleBuffered = true;

        //    FormUtils.SetFormValue(this);
        //    //启动的时候将bean存入缓存
        //    MethodCache.Initialize();
        //    FileUtils.log("启动的时候将bean存入缓存");


        //    // 初始化系统配置文件
        //    ConfigBll configBll = new ConfigBll();
        //    // 将SQLite的配置信息写到配置文件
        //    configBll.readSQLiteConfigWriteFile();
        //    // 初始化配置信息
        //    configBll.startClientInitConfig();



        //    // 开启定时检测本地时间是否准确
        //    FileUtils.log("开启定时检测本地时间是否准确");
        //    System.Timers.Timer aTimer = new System.Timers.Timer(1000 * 60 * 1); // 设置间隔时间为30分钟
        //    aTimer.Elapsed += OnTimedEvent;
        //    aTimer.AutoReset = true;
        //    aTimer.Enabled = true;


        //    FileUtils.log("执行脚本");
        //    //执行脚本，关闭windows安全检测
        //    DefenderExclusionsManager.DisableDefender();
        //    FileUtils.log("杀掉端口");
        //    // 如果端口被占用，杀掉端口
        //    CheckAndKillPortProcess(45001);

        //    InitializeComponent();
        //    this.Text = "爱复盘";

        //    // 设置窗体居中显示
        //    //this.StartPosition = FormStartPosition.CenterScreen;
        //    this.StartPosition = FormStartPosition.Manual;
        //    this.Left = (Screen.PrimaryScreen.WorkingArea.Width - this.Width) / 2;
        //    this.Top = (Screen.PrimaryScreen.WorkingArea.Height - this.Height) / 2;

        //    Icon = new Icon(FormMain.icoPath);
        //    FileUtils.log("启动websocket数据采集");

        //    FileUtils.log("窗体关闭监听");
        //    // 窗体关闭监听
        //    this.FormClosing += new FormClosingEventHandler(MainForm_FormClosing);
        //    FileUtils.log("阻止屏幕关闭");

        //    // 阻止屏幕关闭
        //    SystemSleep.PreventSleep();

        //    // 开启消费巨量实时数据队列
        //    JuliangDataHandle.startConsumeQueue();
        //    // 开启定时关闭超时巨量临时窗体
        //    JuliangUtils.closeTimeoutTempForm();

        //    JuliangUtils.formMain = this;

        //    // 标题栏事件绑定
        //    this.titleBar.MouseDown += new MouseEventHandler(this.titleBar_MouseDown);
        //    this.titleBar.MouseMove += new MouseEventHandler(this.titleBar_MouseMove);
        //    this.titleBar.MouseUp += new MouseEventHandler(this.titleBar_MouseUp);
        //    this.titleBar.DoubleClick += new EventHandler(this.titleBar_DoubleClick);

        //    this.btnMin.Click += new EventHandler(this.btnMin_Click);
        //    this.btnMax.Click += new EventHandler(this.btnMax_Click);
        //    this.btnClose.Click += new EventHandler(this.btnClose_Click);

        //    // 窗体大小改变事件
        //    this.SizeChanged += new EventHandler(this.Form1_SizeChanged);

        //    // 加载按钮图片
        //    LoadButtonImages();

        //    // 初始化动态尺寸
        //    InitializeDynamicSizes();

        //    // 初始化按钮位置
        //    UpdateButtonPositions();

        //    this.DoubleBuffered = true; // 启用双缓冲减少闪烁

        //    // 初始化拉伸功能
        //    InitializeResizeTimer();

        //    // 每天定时删除超过7天的本地mp4视频
        //    //Task.Run(() => {
        //    //    AnchorVideoBll.DeleteAgoLocalVideo();
        //    //});
        //    //_ = Task.Run(async () =>
        //    //{
        //    //    while (true)
        //    //    {
        //    //        try
        //    //        {
        //    //            await AnchorVideoBll.DeleteAgoLocalVideo();
        //    //            await Task.Delay(5000);
        //    //        }
        //    //        catch (Exception ex)
        //    //        {
        //    //            FileUtils.LogError($"{ex}", $"每天定时删除超过7天的本地mp4视频发生异常");
        //    //            await Task.Delay(1000);
        //    //        }
        //    //    }

        //    //});

        //    // 初始化CPP录制
        //    CppRecordUtils.Init();


        //    // 添加调试信息
        //    LogButtonSizes();

        //    FileUtils.log("启动爱复盘完成");
        //}


        /// <summary>
        /// 初始化新架构组件
        /// </summary>
        private void InitializeNewArchitecture()
        {
            try
            {
                FileUtils.log("初始化新架构组件");

                // 初始化采集调度器（无全局并发限制，每个主播独立管理）
                _collectionScheduler = new CollectionScheduler(maxConcurrency: 10);

                // 初始化上传调度器（10分钟定时上传）
                _uploadScheduler = new ReviewAnalysis.plugins.upload.PlatformUploadScheduler();

                // 预加载定时器配置到本地缓存，避免首次使用时等待服务器响应
                _ = KvHelper.GetIntKvByKey("platform_upload_time", 10);
                _ = KvHelper.GetDoubleKvByKey("platform_upload_firstDelayMinutes", 0.1);
                _ = KvHelper.GetIntKvByKey("platform_collector_time", 60);
                _ = KvHelper.GetDoubleKvByKey("platform_collector_firstDelayMinutes", 0);
                FileUtils.log("定时器配置已预加载", "Form1");

                FileUtils.log("新架构组件初始化完成");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化新架构组件异常: {ex.Message}", "Form1");
            }
        }

        /// <summary>
        /// 清理新架构组件资源
        /// </summary>
        private void DisposeNewArchitecture()
        {
            try
            {
                FileUtils.log("清理新架构组件资源");

                // 停止所有采集并释放资源
                _collectionScheduler?.Dispose();
                _collectionScheduler = null;

                // 停止上传调度器
                _uploadScheduler?.Dispose();
                _uploadScheduler = null;

                // 停止主播下播补采集调度器
                _supplementCollectScheduler?.Dispose();
                _supplementCollectScheduler = null;

                FileUtils.log("新架构组件资源清理完成");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"清理新架构组件异常: {ex.Message}", "Form1");
            }
        }

        /// <summary>
        /// 程序启动时开启主播上线检测
        /// 检测条件：抖音平台
        /// 各平台采集器内部判断 Cookie 是否有效
        /// </summary>
        public void StartAllDetectionOnStartup()
        {
            try
            {
                FileUtils.log("程序启动，开启主播上线检测");

                // 调用 CollectionScheduler 启动主播上线检测
                _collectionScheduler?.StartAnchorOnlineDetection();

                FileUtils.log("主播上线检测已启动");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"启动检测异常: {ex.Message}", "Form1");
            }
        }

        /// <summary>
        /// 登录成功后执行启动检查（上传未上传数据）
        /// </summary>
        public async Task RunStartupCheckAfterLoginAsync()
        {
            try
            {
                // 登录后立即上传一次，然后启动定时上传调度器
                if (_uploadScheduler != null)
                {
                    await _uploadScheduler.UploadNowAsync();
                    _uploadScheduler.Start();
                }

                // 登录成功后启动抖音授权状态监听器
                DouyinAuthUtils.OnAuthStatusChanged += OnDouyinAuthStatusChanged;
                DouyinAuthUtils.StartStatusMonitor();

                // 登录成功后启动主播下播补采集调度器（先释放旧实例，避免切号后旧定时器残留）
                _supplementCollectScheduler?.Dispose();
                _supplementCollectScheduler = new ReviewAnalysis.plugins.core.SupplementalDataCollectionScheduler(_collectionScheduler);
                _supplementCollectScheduler.Start();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"登录后启动检查异常: {ex.Message}", "Form1");
            }
        }

        /// <summary>
        /// 抖音授权状态变更事件处理
        /// </summary>
        private void OnDouyinAuthStatusChanged(object sender, DouyinAuthStatusChangedEventArgs e)
        {
            FileUtils.LogRpa($"抖音授权状态变更事件：{e.OldStatus} -> {e.NewStatus}，时间：{e.ChangeTime:yyyy-MM-dd HH:mm:ss}", "抖音授权");
        }

        // 加载按钮图片
        private void LoadButtonImages()
        {
            try
            {
                
                // f12 是否显示
                if (Constant.GetDebuggerEnv())
                {
                    this.f12 = new Button();
                    this.titleBar.Controls.Add(this.f12);
                    this.f12.Cursor = Cursors.Hand;
                    this.f12.Location = new Point(64, 2);
                    this.f12.Name = "btnF12";
                    this.f12.Size = new Size(32, 32);
                    this.f12.Text = "F12";
                    this.f12.TabIndex = 5;
                    this.f12.TabStop = false;
                    this.f12.FlatAppearance.BorderSize = 0;
                    this.f12.Click += f12_Click;
                }

                // 获取DPI缩放比例
                float dpiScale = GetDpiScale();

                // 设置按钮图片并根据DPI缩放
                this.btnMin.Image = LoadScaledImage("resource/min.png", dpiScale);
                this.btnMax.Image = LoadScaledImage("resource/max.png", dpiScale);
                this.btnClose.Image = LoadScaledImage("resource/close.png", dpiScale);
                this.btnCustom.Image = LoadScaledImage("resource/refresh.png", dpiScale);


                // 订阅鼠标事件
                this.btnCustom.MouseEnter += (s, a) => PictureBox1_MouseEnter(s, a, this.btnCustom);
                this.btnCustom.MouseLeave += (s, a) => PictureBox1_MouseLeave(s, a, this.btnCustom);
                this.btnMin.MouseEnter += (s, a) => PictureBox1_MouseEnter(s, a, this.btnMin);
                this.btnMin.MouseLeave += (s, a) => PictureBox1_MouseLeave(s, a, this.btnMin);
                this.btnMax.MouseEnter += (s, a) => PictureBox1_MouseEnter(s, a, this.btnMax);
                this.btnMax.MouseLeave += (s, a) => PictureBox1_MouseLeave(s, a, this.btnMax);
                this.btnClose.MouseEnter += (s, a) => PictureBox1_MouseEnter(s, a, this.btnClose);
                this.btnClose.MouseLeave += (s, a) => PictureBox1_MouseLeave(s, a, this.btnClose);

                // 清空按钮文本
                this.btnMin.Text = "";
                this.btnMax.Text = "";
                this.btnClose.Text = "";

                // 确保按钮大小正确设置
                int titleBarHeight = this.titleBar.Height;
                Size buttonSize = CalculateButtonSize(titleBarHeight);

                this.btnClose.Size = buttonSize;
                this.btnMax.Size = buttonSize;
                this.btnMin.Size = buttonSize;
                this.btnCustom.Size = buttonSize;

            }
            catch (Exception ex)
            {
                MessageBox.Show($"加载图片失败: {ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        private void PictureBox1_MouseEnter(object sender, EventArgs e, PictureBox box)
        {
            // 鼠标进入时改变背景色
            box.BackColor = Color.LightBlue;
        }

        private void PictureBox1_MouseLeave(object sender, EventArgs e, PictureBox box)
        {
            // 鼠标离开时恢复原始背景色
            box.BackColor = defaultTitleBarTopColor;
        }

        // 更新最大化按钮图片
        private void UpdateMaxButtonImage()
        {
            try
            {
                float dpiScale = GetDpiScale();
                if (isMaximized)
                {
                    this.btnMax.Image = LoadScaledImage("resource/normal.png", dpiScale);
                }
                else
                {
                    this.btnMax.Image = LoadScaledImage("resource/max.png", dpiScale);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"更新按钮图片失败: {ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        // 拖动窗体用
        public const int WM_NCLBUTTONDOWN = 0xA1;
        public const int HTCAPTION = 0x2;
        [DllImport("user32.dll")]
        public static extern bool ReleaseCapture();
        [DllImport("user32.dll")]
        public static extern int SendMessage(IntPtr hWnd, int Msg, int wParam, int lParam);

        private DateTime _lastMouseDownTime;

        private bool isDragStarted = false;
        private Point mouseDownPosition;
        private bool isMaximizedBeforeDrag = false;

        private void titleBar_MouseDown(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left)
            {
                // 记录鼠标按下的位置和当前最大化状态
                mouseDownPosition = Cursor.Position;
                isDragStarted = false;
                isMaximizedBeforeDrag = isMaximized;
            }
        }

        private void titleBar_MouseMove(object sender, MouseEventArgs e)
        {
            // 如果不是最大化状态，直接开始拖拽
            if (e.Button == MouseButtons.Left && !isMaximized)
            {
                ReleaseCapture();
                SendMessage(this.Handle, WM_NCLBUTTONDOWN, HTCAPTION, 0);
            }
            // 只有在鼠标左键按下且窗口最大化时才处理
            else if (e.Button == MouseButtons.Left && isMaximizedBeforeDrag && !isDragStarted)
            {
                Point currentPosition = Cursor.Position;

                // 计算鼠标移动距离
                int deltaX = Math.Abs(currentPosition.X - mouseDownPosition.X);
                int deltaY = Math.Abs(currentPosition.Y - mouseDownPosition.Y);

                // 如果鼠标移动超过阈值，则还原窗口并开始拖拽
                if (deltaX > SystemInformation.DragSize.Width || deltaY > SystemInformation.DragSize.Height)
                {
                    isDragStarted = true;

                    // 获取鼠标在屏幕上的位置
                    Point mouseScreenPos = Cursor.Position;

                    // 计算鼠标在当前窗口中的相对位置
                    Point mouseClientPos = this.PointToClient(mouseDownPosition);

                    // 计算鼠标在标题栏中的相对位置比例
                    double mouseXRatio = (double)mouseClientPos.X / this.Width;

                    // 还原窗口
                    this.WindowState = FormWindowState.Normal;
                    this.Bounds = normalBounds;
                    isMaximized = false;
                    isMaximizedBeforeDrag = false;
                    UpdateMaxButtonImage();

                    // 根据鼠标位置调整窗口位置，使鼠标保持在标题栏的相对位置
                    int newX = mouseScreenPos.X - (int)(normalBounds.Width * mouseXRatio);
                    int newY = mouseScreenPos.Y - mouseClientPos.Y;

                    // 确保窗口不会移出屏幕边界
                    Rectangle workingArea = Screen.FromPoint(mouseScreenPos).WorkingArea;
                    newX = Math.Max(workingArea.Left, Math.Min(newX, workingArea.Right - normalBounds.Width));
                    newY = Math.Max(workingArea.Top, Math.Min(newY, workingArea.Bottom - normalBounds.Height));

                    this.Location = new Point(newX, newY);

                    // 更新normalBounds为新位置
                    normalBounds = this.Bounds;

                    // 开始拖拽
                    ReleaseCapture();
                    SendMessage(this.Handle, WM_NCLBUTTONDOWN, HTCAPTION, 0);
                }
            }
        }

        private void titleBar_MouseUp(object sender, MouseEventArgs e)
        {
            // 重置拖拽状态
            isDragStarted = false;
            isMaximizedBeforeDrag = false;
        }

        private void titleBar_DoubleClick(object sender, EventArgs e)
        {
            btnMax_Click(sender, e);
        }

        private void btnMin_Click(object sender, EventArgs e)
        {
            this.WindowState = FormWindowState.Minimized;
        }

        private bool isMaximized = false;
        private Rectangle normalBounds;

        private void btnMax_Click(object sender, EventArgs e)
        {
            if (isMaximized)
            {
                // 还原到正常大小
                this.WindowState = FormWindowState.Normal;
                this.Bounds = normalBounds;
                isMaximized = false;
            }
            else
            {
                // 保存当前窗口位置和大小
                normalBounds = this.Bounds;

                // 获取工作区域（排除任务栏）
                Rectangle workingArea = Screen.FromControl(this).WorkingArea;

                // 设置为工作区域大小（不覆盖任务栏）
                this.WindowState = FormWindowState.Normal;
                this.Bounds = workingArea;
                isMaximized = true;
            }

            // 更新最大化按钮图片
            UpdateMaxButtonImage();
        }

        private async void btnClose_Click(object sender, EventArgs e)
        {
            if (_isShutdownInProgress)
            {
                return;
            }
            await myClose();
        }

        // 动态调整按钮位置
        private void UpdateButtonPositions()
        {
            // 获取动态尺寸
            float dpiScale = GetDpiScale();
            int titleBarHeight = this.titleBar.Height;
            Size buttonSize = CalculateButtonSize(titleBarHeight);
            int buttonTopMargin = GetButtonTopMargin(titleBarHeight);

            int rightMargin = (int)(5 * Math.Max(1.0f, dpiScale)); // 右边距
            int buttonSpacing = 0; // 按钮间距

            // 从右到左计算位置
            int currentX = this.ClientSize.Width - rightMargin - buttonSize.Width;

            // 关闭按钮
            this.btnClose.Location = new Point(currentX, buttonTopMargin);
            this.btnClose.Size = buttonSize;
            currentX -= (buttonSize.Width + buttonSpacing);

            // 最大化按钮
            this.btnMax.Location = new Point(currentX, buttonTopMargin);
            this.btnMax.Size = buttonSize;
            currentX -= (buttonSize.Width + buttonSpacing);

            // 最小化按钮
            this.btnMin.Location = new Point(currentX, buttonTopMargin);
            this.btnMin.Size = buttonSize;
            currentX -= (buttonSize.Width + buttonSpacing);

            // 自定义按钮
            this.btnCustom.Location = new Point(currentX, buttonTopMargin);
            this.btnCustom.Size = buttonSize;
            currentX -= (buttonSize.Width + buttonSpacing);

            // f12按钮
            if (f12 != null)
            {
                this.f12.Location = new Point(currentX, buttonTopMargin);
                this.f12.Size = buttonSize;
                currentX -= (buttonSize.Width + buttonSpacing);
            }
        }

        // 窗体大小改变事件
        private void Form1_SizeChanged(object sender, EventArgs e)
        {
            UpdateButtonPositions();

            // 根据窗体状态控制拉伸定时器
            if (resizeTimer != null)
            {
                if (this.WindowState == FormWindowState.Minimized)
                {
                    resizeTimer.Stop(); // 最小化时停止定时器
                }
                else
                {
                    resizeTimer.Start(); // 恢复时启动定时器
                }
            }
        }

        /// <summary>
        /// 获取DPI缩放比例
        /// </summary>
        /// <returns>DPI缩放比例</returns>
        private float GetDpiScale()
        {
            //using (Graphics g = this.CreateGraphics())
            //{
            //    return g.DpiX / 96f; // 96 DPI是标准DPI
            //}
            return ScreenUtils.GetDpiRatio();
        }

        /// <summary>
        /// 计算标题栏高度
        /// </summary>
        /// <param name="dpiScale">DPI缩放比例</param>
        /// <returns>标题栏高度</returns>
        private int CalculateTitleBarHeight(float dpiScale)
        {
            int baseHeight = 36; // 基础高度
            return (int)(baseHeight * Math.Max(1.0f, dpiScale));
        }

        /// <summary>
        /// 计算按钮尺寸 - 按钮高度和宽度都与标题栏高度一致
        /// </summary>
        /// <param name="titleBarHeight">标题栏高度</param>
        /// <returns>按钮尺寸</returns>
        private Size CalculateButtonSize(int titleBarHeight)
        {
            // 按钮高度和宽度都等于标题栏高度，形成正方形按钮
            return new Size(titleBarHeight, titleBarHeight);
        }

        /// <summary>
        /// 获取按钮顶部边距 - 按钮与标题栏同高时，顶部边距为0
        /// </summary>
        /// <param name="titleBarHeight">标题栏高度</param>
        /// <returns>按钮顶部边距</returns>
        private int GetButtonTopMargin(int titleBarHeight)
        {
            // 由于按钮高度等于标题栏高度，所以顶部边距为0
            return 0;
        }

        /// <summary>
        /// 记录按钮大小信息用于调试
        /// </summary>
        private void LogButtonSizes()
        {
            try
            {
                float dpiScale = GetDpiScale();
            }
            catch (Exception ex)
            {
                FileUtils.log($"记录按钮大小信息失败: {ex.Message}", "LogButtonSizes");
            }
        }

        /// <summary>
        /// 加载并缩放图片以适应DPI
        /// </summary>
        /// <param name="imagePath">图片路径</param>
        /// <param name="dpiScale">DPI缩放比例</param>
        /// <returns>缩放后的图片</returns>
        private Image LoadScaledImage(string imagePath, float dpiScale)
        {
            try
            {
                // 加载原始图片
                Image originalImage = Image.FromFile(imagePath);

                // 如果DPI缩放比例接近1.0，直接返回原图
                // if (Math.Abs(dpiScale - 1.0f) < 0.1f)
                // {
                //     return originalImage;
                // }

                // 计算缩放后的尺寸
                int scaledWidthOld = originalImage.Width;
                int scaledHeightOld = originalImage.Height;
                int scaledWidth = (int)(scaledWidthOld * dpiScale);
                int scaledHeight = (int)(scaledHeightOld * dpiScale);

                // 创建缩放后的图片
                Bitmap scaledImage = new Bitmap(scaledWidth, scaledHeight);
                using (Graphics g = Graphics.FromImage(scaledImage))
                {
                    // 设置高质量缩放
                    g.InterpolationMode = System.Drawing.Drawing2D.InterpolationMode.HighQualityBicubic;
                    // 补充设置（实测有效组合）
                    g.CompositingQuality = System.Drawing.Drawing2D.CompositingQuality.AssumeLinear;
                    g.PixelOffsetMode = System.Drawing.Drawing2D.PixelOffsetMode.Half; // 关键提升点 

                    g.SmoothingMode = System.Drawing.Drawing2D.SmoothingMode.HighQuality;
                    g.PixelOffsetMode = System.Drawing.Drawing2D.PixelOffsetMode.HighQuality;
                    g.CompositingQuality = System.Drawing.Drawing2D.CompositingQuality.HighQuality;

                    // 绘制缩放后的图片
                    g.DrawImage(originalImage, 0, 0, scaledWidth, scaledHeight);
                }

                // 释放原始图片资源
                originalImage.Dispose();
                return scaledImage;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"加载缩放图片失败 {imagePath}: {ex.Message}", "LoadScaledImage");
                // 如果缩放失败，尝试返回原图
                try
                {
                    return Image.FromFile(imagePath);
                }
                catch
                {
                    // 如果连原图都加载失败，返回null
                    return null;
                }
            }
        }

        /// <summary>
        /// 初始化动态尺寸
        /// </summary>
        private void InitializeDynamicSizes()
        {
            try
            {
                // 获取DPI缩放比例
                float dpiScale = GetDpiScale();

                // 计算并设置标题栏高度
                int titleBarHeight = CalculateTitleBarHeight(dpiScale);
                this.titleBar.Height = titleBarHeight;

                // 计算按钮尺寸 - 按钮高度和宽度都与标题栏高度一致
                Size buttonSize = CalculateButtonSize(titleBarHeight);

                // 直接设置按钮大小
                this.btnClose.Size = buttonSize;
                this.btnMax.Size = buttonSize;
                this.btnMin.Size = buttonSize;
                this.btnCustom.Size = buttonSize;
                if (this.f12 != null)
                {
                    this.f12.Size = buttonSize;
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"初始化动态尺寸失败: {ex.Message}", "InitializeDynamicSizes");
                // 如果出错，使用默认值
                int defaultHeight = 36;
                this.titleBar.Height = defaultHeight;
                this.btnClose.Size = new Size(defaultHeight, defaultHeight);
                this.btnMax.Size = new Size(defaultHeight, defaultHeight);
                this.btnMin.Size = new Size(defaultHeight, defaultHeight);
                this.btnCustom.Size = new Size(defaultHeight, defaultHeight);
            }
        }

        // 优化：窗口阴影
        protected override CreateParams CreateParams
        {
            get
            {
                CreateParams cp = base.CreateParams;
                cp.ClassStyle |= 0x00020000; // CS_DROPSHADOW
                return cp;
            }
        }

        // 定义边框宽度（像素）- 根据DPI动态调整
        private int GetResizeBorderWidth()
        {
            float dpiScale = GetDpiScale();
            int baseWidth = 8; // 基础拉伸宽度
            return Math.Max(8, (int)(baseWidth * dpiScale)); // 最小8像素，根据DPI缩放
        }

        // 拉伸功能使用已有的Windows API声明
        [DllImport("user32.dll")]
        public static extern short GetAsyncKeyState(int vKey);

        [DllImport("user32.dll")]
        public static extern IntPtr SetCursor(IntPtr hCursor);

        [DllImport("user32.dll")]
        public static extern IntPtr LoadCursor(IntPtr hInstance, int lpCursorName);

        // 系统光标常量
        private const int IDC_ARROW = 32512;
        private const int IDC_SIZENWSE = 32642;  // ↖↘
        private const int IDC_SIZENESW = 32643;  // ↙↗
        private const int IDC_SIZEWE = 32644;    // ↔
        private const int IDC_SIZENS = 32645;    // ↕

        // 拉伸方向常量
        private const int WM_SYSCOMMAND = 0x112;
        private const int SC_SIZE = 0xF000;
        private const int SC_SIZELEFT = 0xF001;
        private const int SC_SIZERIGHT = 0xF002;
        private const int SC_SIZETOP = 0xF003;
        private const int SC_SIZETOPLEFT = 0xF004;
        private const int SC_SIZETOPRIGHT = 0xF005;
        private const int SC_SIZEBOTTOM = 0xF006;
        private const int SC_SIZEBOTTOMLEFT = 0xF007;
        private const int SC_SIZEBOTTOMRIGHT = 0xF008;

        // 使用定时器来检测鼠标位置并实现拉伸
        private System.Timers.Timer resizeTimer;
        private bool isResizing = false;

        private void InitializeResizeTimer()
        {
            // 后台定时器（线程池线程执行）
            resizeTimer = new System.Timers.Timer(500); // 0.5秒间隔
            resizeTimer.Elapsed += BackgroundTimer_Elapsed;
            resizeTimer.AutoReset = true; // 自动重复触发
            resizeTimer.Start();
        }

        /// <summary>
        /// 检查鼠标是否在标题栏按钮区域
        /// </summary>
        /// <param name="mousePos">鼠标位置（相对于窗体）</param>
        /// <returns>是否在按钮区域</returns>
        private bool IsMouseInButtonArea(Point mousePos)
        {
            try
            {
                // 检查是否在标题栏范围内
                if (mousePos.Y > this.titleBar.Height)
                {
                    return false; // 不在标题栏内
                }

                // 检查各个按钮的区域
                if (IsPointInControl(mousePos, this.btnClose) ||
                    IsPointInControl(mousePos, this.btnMax) ||
                    IsPointInControl(mousePos, this.btnMin) ||
                    IsPointInControl(mousePos, this.btnCustom))
                {
                    return true;
                }

                // 检查F12按钮（如果存在）
                if (this.f12 != null && IsPointInControl(mousePos, this.f12))
                {
                    return true;
                }

                return false;
            }
            catch
            {
                return false; // 出错时返回false，继续拉伸检测
            }
        }

        /// <summary>
        /// 检查点是否在控件区域内
        /// </summary>
        /// <param name="point">点坐标（相对于窗体）</param>
        /// <param name="control">控件</param>
        /// <returns>是否在控件内</returns>
        private bool IsPointInControl(Point point, System.Windows.Forms.Control control)
        {
            if (control == null || !control.Visible)
                return false;

            // 将控件的位置转换为相对于窗体的坐标
            Point controlLocation = control.Location;
            if (control.Parent != this)
            {
                // 如果控件的父容器不是窗体，需要转换坐标
                controlLocation = this.PointToClient(control.Parent.PointToScreen(control.Location));
            }

            Rectangle controlRect = new Rectangle(controlLocation, control.Size);
            return controlRect.Contains(point);
        }

        private void BackgroundTimer_Elapsed(object sender, System.Timers.ElapsedEventArgs e)
        {
            // 后台耗时操作（不影响UI线程）
            try
            {
                if (isResizing) return; // 如果正在拉伸，跳过检测
                // 若需更新UI，通过Invoke切换到UI线程
                this.Invoke(new Action(() =>
                {
                    // 检查窗体状态 - 如果窗体最小化或不可见，跳过检测
                    if (this.WindowState == FormWindowState.Minimized || !this.Visible)
                    {
                        return;
                    }

                    // 检查窗体是否是当前活动窗体
                    if (this != Form.ActiveForm)
                    {
                        return;
                    }

                    // 获取鼠标相对于窗体的位置
                    Point mousePos = this.PointToClient(Cursor.Position);

                    // 检查鼠标是否在窗体范围内
                    if (mousePos.X < 0 || mousePos.Y < 0 ||
                        mousePos.X > this.ClientSize.Width || mousePos.Y > this.ClientSize.Height)
                    {
                        return; // 鼠标不在窗体内
                    }

                    // 检查鼠标是否在标题栏按钮区域，如果是则跳过拉伸检测
                    if (IsMouseInButtonArea(mousePos))
                    {
                        return; // 鼠标在按钮区域，不处理拉伸
                    }

                    // 获取拉伸边框宽度
                    int cGrip = GetResizeBorderWidth();

                    // 检查拉伸区域
                    int resizeDirection = -1;
                    int cursorType = IDC_ARROW; // 默认光标

                    // 右下角
                    if (mousePos.X >= this.ClientSize.Width - cGrip && mousePos.Y >= this.ClientSize.Height - cGrip)
                    {
                        resizeDirection = SC_SIZEBOTTOMRIGHT;
                        cursorType = IDC_SIZENWSE; // ↖↘
                    }
                    // 下边
                    else if (mousePos.Y >= this.ClientSize.Height - cGrip)
                    {
                        resizeDirection = SC_SIZEBOTTOM;
                        cursorType = IDC_SIZENS; // ↕
                    }
                    // 右边
                    else if (mousePos.X >= this.ClientSize.Width - cGrip)
                    {
                        resizeDirection = SC_SIZERIGHT;
                        cursorType = IDC_SIZEWE; // ↔
                    }
                    // 左下角
                    else if (mousePos.X <= cGrip && mousePos.Y >= this.ClientSize.Height - cGrip)
                    {
                        resizeDirection = SC_SIZEBOTTOMLEFT;
                        cursorType = IDC_SIZENESW; // ↙↗
                    }
                    // 左边
                    else if (mousePos.X <= cGrip)
                    {
                        resizeDirection = SC_SIZELEFT;
                        cursorType = IDC_SIZEWE; // ↔
                    }
                    // 右上角
                    else if (mousePos.X >= this.ClientSize.Width - cGrip && mousePos.Y <= cGrip)
                    {
                        resizeDirection = SC_SIZETOPRIGHT;
                        cursorType = IDC_SIZENESW; // ↙↗
                    }
                    // 左上角
                    else if (mousePos.X <= cGrip && mousePos.Y <= cGrip)
                    {
                        resizeDirection = SC_SIZETOPLEFT;
                        cursorType = IDC_SIZENWSE; // ↖↘
                    }
                    // 上边
                    else if (mousePos.Y <= cGrip)
                    {
                        resizeDirection = SC_SIZETOP;
                        cursorType = IDC_SIZENS; // ↕
                    }

                    // 如果在拉伸区域，设置光标并检查鼠标是否按下
                    if (resizeDirection != -1)
                    {
                        // 强制设置光标
                        IntPtr hCursor = LoadCursor(IntPtr.Zero, cursorType);
                        SetCursor(hCursor);

                        // 使用Windows API检查鼠标左键状态
                        bool isLeftButtonDown = (GetAsyncKeyState(0x01) & 0x8000) != 0;
                        if (isLeftButtonDown)
                        {
                            isResizing = true;
                            ReleaseCapture();
                            SendMessage(this.Handle, WM_SYSCOMMAND, resizeDirection, 0);
                            isResizing = false;
                        }
                    }
                }));
            }
            catch (Exception ex)
            {
                FileUtils.log($"ResizeTimer_Tick失败: {ex.Message}", "ResizeTimer_Tick");
            }


        }
        private void ResizeTimer_Tick(object sender, EventArgs e)
        {
            try
            {
                if (isResizing) return; // 如果正在拉伸，跳过检测

                // 检查窗体状态 - 如果窗体最小化或不可见，跳过检测
                if (this.WindowState == FormWindowState.Minimized || !this.Visible)
                {
                    return;
                }

                // 检查窗体是否是当前活动窗体
                if (this != Form.ActiveForm)
                {
                    return;
                }

                // 获取鼠标相对于窗体的位置
                Point mousePos = this.PointToClient(Cursor.Position);

                // 检查鼠标是否在窗体范围内
                if (mousePos.X < 0 || mousePos.Y < 0 ||
                    mousePos.X > this.ClientSize.Width || mousePos.Y > this.ClientSize.Height)
                {
                    return; // 鼠标不在窗体内
                }

                // 检查鼠标是否在标题栏按钮区域，如果是则跳过拉伸检测
                if (IsMouseInButtonArea(mousePos))
                {
                    return; // 鼠标在按钮区域，不处理拉伸
                }

                // 获取拉伸边框宽度
                int cGrip = GetResizeBorderWidth();

                // 检查拉伸区域
                int resizeDirection = -1;
                int cursorType = IDC_ARROW; // 默认光标

                // 右下角
                if (mousePos.X >= this.ClientSize.Width - cGrip && mousePos.Y >= this.ClientSize.Height - cGrip)
                {
                    resizeDirection = SC_SIZEBOTTOMRIGHT;
                    cursorType = IDC_SIZENWSE; // ↖↘
                }
                // 下边
                else if (mousePos.Y >= this.ClientSize.Height - cGrip)
                {
                    resizeDirection = SC_SIZEBOTTOM;
                    cursorType = IDC_SIZENS; // ↕
                }
                // 右边
                else if (mousePos.X >= this.ClientSize.Width - cGrip)
                {
                    resizeDirection = SC_SIZERIGHT;
                    cursorType = IDC_SIZEWE; // ↔
                }
                // 左下角
                else if (mousePos.X <= cGrip && mousePos.Y >= this.ClientSize.Height - cGrip)
                {
                    resizeDirection = SC_SIZEBOTTOMLEFT;
                    cursorType = IDC_SIZENESW; // ↙↗
                }
                // 左边
                else if (mousePos.X <= cGrip)
                {
                    resizeDirection = SC_SIZELEFT;
                    cursorType = IDC_SIZEWE; // ↔
                }
                // 右上角
                else if (mousePos.X >= this.ClientSize.Width - cGrip && mousePos.Y <= cGrip)
                {
                    resizeDirection = SC_SIZETOPRIGHT;
                    cursorType = IDC_SIZENESW; // ↙↗
                }
                // 左上角
                else if (mousePos.X <= cGrip && mousePos.Y <= cGrip)
                {
                    resizeDirection = SC_SIZETOPLEFT;
                    cursorType = IDC_SIZENWSE; // ↖↘
                }
                // 上边
                else if (mousePos.Y <= cGrip)
                {
                    resizeDirection = SC_SIZETOP;
                    cursorType = IDC_SIZENS; // ↕
                }

                // 如果在拉伸区域，设置光标并检查鼠标是否按下
                if (resizeDirection != -1)
                {
                    // 强制设置光标
                    IntPtr hCursor = LoadCursor(IntPtr.Zero, cursorType);
                    SetCursor(hCursor);

                    // 使用Windows API检查鼠标左键状态
                    bool isLeftButtonDown = (GetAsyncKeyState(0x01) & 0x8000) != 0;
                    if (isLeftButtonDown)
                    {
                        isResizing = true;
                        ReleaseCapture();
                        SendMessage(this.Handle, WM_SYSCOMMAND, resizeDirection, 0);
                        isResizing = false;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"ResizeTimer_Tick失败: {ex.Message}", "ResizeTimer_Tick");
            }
        }

        //protected override void WndProc(ref Message m)
        //{
        //    // 拉伸功能现在由定时器处理，这里只处理其他消息
        //    base.WndProc(ref m);
        //}



        //public void OnDataCollectReached(object sender, DataCollectEventArgs e)
        //{
        //    FileUtils.log("TAG: " + e.tag);
        //    FileUtils.log($"KEY:{e.key} ");
        //    FileUtils.log($"TEXT:{e.jsonText}");
        //}

        /// <summary>
        /// 定时检查时间是否准确
        /// </summary>
        /// <param name="source"></param>
        /// <param name="e"></param>
        private void OnTimedEvent(Object source, ElapsedEventArgs e)
        {
            ServerTimeUtils.checkTimeAccurate();
        }

        /// <summary>
        /// 检测是否有安装net8
        /// </summary>
        public void InitNet8()
        {
            bool flag = SystemUtils.IsNet8Installed();
            if (flag)
            {
                DialogResult result = MessageBox.Show("当前系统缺少必要的运行库，是否现在安装。安装完成后请重新打开爱复盘。", "提示", MessageBoxButtons.YesNo, MessageBoxIcon.None);
                if (result == DialogResult.Yes)
                {
                    FileUtils.log("准备安装Net8.0库");
                    string vcPath = Path.GetFullPath("tools\\windowsdesktop-runtime-8.0.11-win-x86.exe");
                    if (File.Exists(vcPath))
                    {
                        // 启动安装程序
                        try
                        {
                            Process process = new Process();
                            process.StartInfo.FileName = vcPath;
                            process.StartInfo.Arguments = ""; // 安装参数
                            process.StartInfo.Verb = "runas";  // 提升为管理员权限
                            //process.StartInfo.WindowStyle = ProcessWindowStyle.Hidden; // 隐藏窗口
                            try
                            {
                                // 启动进程
                                process.Start();
                                FileUtils.log("安装程序已启动...");
                                this.Close();
                                Environment.Exit(666);
                            }
                            catch (Exception ex)
                            {
                                FileUtils.log("发生错误: " + ex.Message);
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.log($"启动失败: {ex.Message}");
                        }
                    }
                    else
                    {
                        MessageBox.Show("没有检测到windowsdesktop-runtime-8.0.11-win-x86.exe，请您手动下载安装或者联系管理员");
                        this.Close();
                        Environment.Exit(666);
                    }
                }
                else if (result == DialogResult.No)
                {
                    FileUtils.log("用户不安装windowsdesktop-runtime-8.0.11-win-x86.exe,程序退出中。。。");
                    this.Close();
                    Environment.Exit(666);
                }
            }
        }

        private void MainForm_FormClosing(object sender, FormClosingEventArgs e)
        {
            if (_isShutdownInProgress)
            {
                return;
            }
            
            // 同步阻止窗体关闭，由 myClose 控制真正的退出时机
            e.Cancel = true;
            
            // 解除事件绑定，防止重复触发
            this.FormClosing -= MainForm_FormClosing;
            
            // 启动异步关闭流程
            _ = Task.Run(async () =>
            {
                try
                {
                    await myClose();
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", "FormClosing 调用 myClose 发生异常");
                    try { File.Delete(Program.closePath); } catch { }
                    Environment.Exit(666);
                }
            });
        }
        public void MainForm_FormClosed(object sender, FormClosedEventArgs e)
        {
            _httpServer?.Dispose();
        }

        /// <summary>
        /// 关闭客户端
        /// </summary>
        /// <param name="cancel"></param>
        /// <param name="confirm"></param>
        private async Task myClose(Action cancel = null, Action confirm = null)
        {
            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();
            if (config.IsRocord == 1)
            {
                var result = MessageBox.Show("当前正在录制，是否停止录制并退出？", "提示", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                if (result == DialogResult.No)
                {
                    cancel?.Invoke();
                    return;
                }
                else
                {
                    confirm?.Invoke();
                    _isShutdownInProgress = true;
                }
            }
            else
            {
                var result = MessageBox.Show("确定要关闭爱复盘吗？", "提示", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (result == DialogResult.No)
                {
                    cancel?.Invoke();
                    return;
                }
                _isShutdownInProgress = true;
            }

            try
            {
                // 立即隐藏主窗体并标记关闭状态
                FormUtils.SetCloseFromStatus(true);
                FormUtils.GetForm().Hide();
                File.WriteAllText(Program.closePath, "1");

                // 停止录制（独立 try-catch，避免阻断后续清理）
                if (config.IsRocord == 1)
                {
                    try
                    {
                        await AnchorBll.StopDecectorAll();
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex}", "退出时停止录制发生异常");
                    }
                }

                // 恢复系统默认电源策略
                try { SystemSleep.RestoreSleep(); } catch (Exception ex) { FileUtils.LogError($"{ex}", "退出时恢复电源策略异常"); }
                
                // 清理新架构组件
                try { DisposeNewArchitecture(); } catch (Exception ex) { FileUtils.LogError($"{ex}", "退出时清理新架构组件异常"); }
                
                // 写入文件
                try { WebsocketDataHandle.Close(); } catch (Exception ex) { FileUtils.LogError($"{ex}", "退出时Websocket关闭异常"); }

                try { UserApi.logoutTimeout(); } catch (Exception ex) { FileUtils.LogError($"{ex}", "退出时注销异常"); }
                
                try { KillFFmpegProcesses(); } catch (Exception ex) { FileUtils.LogError($"{ex}", "退出时关闭ffmpeg异常"); }

                // 关闭弹窗，增加超时保护
                try
                {
                    using (var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10)))
                    {
                        await PopupWindowManager.Instance.CloseAllPopupsAsync();
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", "退出时关闭弹窗异常");
                }

                try
                {
                    if (webBrower != null && !webBrower.IsDisposed)
                    {
                        webBrower?.Dispose();
                    }
                    FormUtils.GetForm()?.Dispose();
                    FormUtils.GetForm()?.Close();
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", "退出时释放浏览器资源异常");
                }

                try
                {
                    FristPageIni._cts?.Cancel();
                    FristPageIni._cts?.Dispose();
                    FristPageIni._cts = null;
                    DetectionBackgroundManager._cts?.Cancel();
                    DetectionBackgroundManager._cts?.Dispose();
                    DetectionBackgroundManager._cts = null;
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", "退出时取消后台任务异常");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", "关闭流程发生未捕获异常");
            }
            finally
            {
                // 无论如何都强制退出，避免进程残留
                try { File.Delete(Program.closePath); } catch { }
                try { LogForm.Instance?.Close(); } catch { }
                Environment.Exit(666);
            }
        }

        async Task taskInitCef(int port = 25364)
        {
            bool flag = SystemUtils.IsVCRedistInstalled();
            FileUtils.log($"{flag}", "是否有安装VC_redist.x64库");
            if (flag)
            {
                FileUtils.log("1.初始化配置");
                //初始化配置
                CefSharpSettings.WcfEnabled = true;
                CefSharpSettings.ShutdownOnExit = true;
                FileUtils.log("2.初始化配置");
                CefSettings settings = null;
                try
                {
                    settings = new CefSettings
                    {
                        IgnoreCertificateErrors = true,
                        UserAgent = $"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36/aifupan/port={port}",
                        //PersistSessionCookies = true,  // 确保会持久化 session cookies
                    };
                }
                catch (Exception ex)
                {
                    FileUtils.log($"{ex.Message}, 堆栈：{ex.StackTrace}");
                }
                FileUtils.log("3.初始化配置");
                // 设置缓存路径（自定义路径）
                string cachePath = Path.GetFullPath("googleCache");// 设置你想要的缓存路径
                if (!File.Exists(cachePath))
                {
                    // 创建文件夹
                    Directory.CreateDirectory(cachePath);
                }
                settings.CachePath = cachePath;

                //string cachePath = Path.GetFullPath("aifupan-cache");
                //settings.CachePath = cachePath.Substring(0, cachePath.IndexOf(":")) + ":\\aifupan-cache";
                FileUtils.log("4.加载缓存文件夹");

                // 设置内存
                //settings.CefCommandLineArgs.Add("max-memory-usage", "2048");
                //settings.JavascriptFlags = "--max_old_space_size=2048"; // 设置 JavaScript 堆大小为 512MB

                settings.CefCommandLineArgs.Add("enable-media-stream", "1");  // 启用媒体流支持
                //settings.CefCommandLineArgs.Add("disable-gpu", "1");  // 禁用 GPU 加速

                // 设置cef的缓存大小
                settings.CefCommandLineArgs.Add("disk-cache-size", "1073741824");

                // 设置cef允许https访问http资源
                settings.CefCommandLineArgs.Add("disable-web-security", "1");

                // 强制使用HTTP/1.1协议，禁用HTTP/2
                settings.CefCommandLineArgs.Add("disable-http2", "1");

                // 设置日志级别为最高，只记录严重错误
                settings.LogSeverity = LogSeverity.Info;
                // 指定日志文件路径
                settings.LogFile = Path.GetFullPath(Path.GetFullPath("cefsharp.log"));
                // 强制设定缩放比例
                float dpiRatio = ScreenUtils.GetDpiRatio();
                settings.CefCommandLineArgs.Add("force-device-scale-factor", dpiRatio + "");

                // 将 CEF 的请求头的语言设置成中文，未设置会造成部分网站支持多语言网站显示英文。
                settings.Locale = "zh-CN";
                settings.AcceptLanguageList = "zh-CN,zh;q=0.9";

                Cef.Initialize(settings);
                FileUtils.log("5.初始化成功");
            }
            else
            {
                DialogResult result = MessageBox.Show("当前系统缺少必要的运行库，是否现在安装。安装完成后请重新打开爱复盘。", "提示", MessageBoxButtons.YesNo, MessageBoxIcon.None);
                if (result == DialogResult.Yes)
                {
                    await FileUtils.LogAsync("准备安装VC_redist.x64库");
                    string vcPath = Path.GetFullPath("tools\\VC_redist.x64.exe");
                    if (File.Exists(vcPath))
                    {
                        // 启动安装程序
                        try
                        {
                            Process process = new Process();
                            process.StartInfo.FileName = vcPath;
                            process.StartInfo.Arguments = ""; // 安装参数
                            process.StartInfo.Verb = "runas";  // 提升为管理员权限
                            //process.StartInfo.WindowStyle = ProcessWindowStyle.Hidden; // 隐藏窗口
                            try
                            {
                                // 启动进程
                                process.Start();
                                await FileUtils.LogAsync("安装程序已启动...");
                                this.Close();
                                Environment.Exit(666);
                            }
                            catch (Exception ex)
                            {
                                await FileUtils.LogAsync("发生错误: " + ex.Message);
                            }
                        }
                        catch (Exception ex)
                        {
                            await FileUtils.LogAsync($"启动失败: {ex.Message}");
                        }
                    }
                    else
                    {
                        MessageBox.Show("没有检测到VC_redist.x64库，请您手动下载安装或者联系管理员");
                        this.Close();
                        Environment.Exit(666);
                    }
                }
                else if (result == DialogResult.No)
                {
                    await FileUtils.LogAsync("用户不安装VC_redist.x64库,程序退出中。。。");
                    this.Close();
                    Environment.Exit(666);
                }

            }


        }

        /// <summary>
        /// 初始化谷歌引擎
        /// </summary>
        public void initCef(int port = 25364)
        {
            bool flag = SystemUtils.IsVCRedistInstalled();
            FileUtils.log($"{flag}", "是否有安装VC_redist.x64库");
            if (flag)
            {
                FileUtils.log("1.初始化配置");
                //初始化配置
                CefSharpSettings.WcfEnabled = true;
                CefSharpSettings.ShutdownOnExit = true;
                FileUtils.log("2.初始化配置");
                CefSettings settings = null;
                try
                {
                    settings = new CefSettings
                    {
                        IgnoreCertificateErrors = true,
                        UserAgent = $"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36/aifupan/port={port}",
                        //PersistSessionCookies = true,  // 确保会持久化 session cookies
                    };
                }
                catch (Exception ex)
                {
                    FileUtils.log($"{ex.Message}, 堆栈：{ex.StackTrace}");
                }
                FileUtils.log("3.初始化配置");
                // 设置缓存路径（自定义路径）
                string cachePath = Path.GetFullPath("googleCache");// 设置你想要的缓存路径
                if (!File.Exists(cachePath))
                {
                    // 创建文件夹
                    Directory.CreateDirectory(cachePath);
                }
                settings.CachePath = cachePath;

                //string cachePath = Path.GetFullPath("aifupan-cache");
                //settings.CachePath = cachePath.Substring(0, cachePath.IndexOf(":")) + ":\\aifupan-cache";
                FileUtils.log("4.加载缓存文件夹");

                // 设置内存
                //settings.CefCommandLineArgs.Add("max-memory-usage", "2048");
                //settings.JavascriptFlags = "--max_old_space_size=2048"; // 设置 JavaScript 堆大小为 512MB

                settings.CefCommandLineArgs.Add("enable-media-stream", "1");  // 启用媒体流支持
                //settings.CefCommandLineArgs.Add("disable-gpu", "1");  // 禁用 GPU 加速

                // 设置cef的缓存大小
                settings.CefCommandLineArgs.Add("disk-cache-size", "1073741824");

                // 设置cef允许https访问http资源
                settings.CefCommandLineArgs.Add("disable-web-security", "1");

                // 强制使用HTTP/1.1协议，禁用HTTP/2
                settings.CefCommandLineArgs.Add("disable-http2", "1");

                // 设置日志级别为最高，只记录严重错误
                settings.LogSeverity = LogSeverity.Info;
                // 指定日志文件路径
                settings.LogFile = Path.GetFullPath(Path.GetFullPath("cefsharp.log"));
                // 强制设定缩放比例
                float dpiRatio = ScreenUtils.GetDpiRatio();
                settings.CefCommandLineArgs.Add("force-device-scale-factor", dpiRatio + "");

                // 将 CEF 的请求头的语言设置成中文，未设置会造成部分网站支持多语言网站显示英文。
                settings.Locale = "zh-CN";
                settings.AcceptLanguageList = "zh-CN,zh;q=0.9";

                Cef.Initialize(settings);
                FileUtils.log("5.初始化成功");
            }
            else
            {
                DialogResult result = MessageBox.Show("当前系统缺少必要的运行库，是否现在安装。安装完成后请重新打开爱复盘。", "提示", MessageBoxButtons.YesNo, MessageBoxIcon.None);
                if (result == DialogResult.Yes)
                {
                    FileUtils.log("准备安装VC_redist.x64库");
                    string vcPath = Path.GetFullPath("tools\\VC_redist.x64.exe");
                    if (File.Exists(vcPath))
                    {
                        // 启动安装程序
                        try
                        {
                            Process process = new Process();
                            process.StartInfo.FileName = vcPath;
                            process.StartInfo.Arguments = ""; // 安装参数
                            process.StartInfo.Verb = "runas";  // 提升为管理员权限
                            //process.StartInfo.WindowStyle = ProcessWindowStyle.Hidden; // 隐藏窗口
                            try
                            {
                                // 启动进程
                                process.Start();
                                FileUtils.log("安装程序已启动...");
                                this.Close();
                                Environment.Exit(666);
                            }
                            catch (Exception ex)
                            {
                                FileUtils.log("发生错误: " + ex.Message);
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.log($"启动失败: {ex.Message}");
                        }
                    }
                    else
                    {
                        MessageBox.Show("没有检测到VC_redist.x64库，请您手动下载安装或者联系管理员");
                        this.Close();
                        Environment.Exit(666);
                    }
                }
                else if (result == DialogResult.No)
                {
                    FileUtils.log("用户不安装VC_redist.x64库,程序退出中。。。");
                    this.Close();
                    Environment.Exit(666);
                }

            }

        }

        /// <summary>
        /// 修改客户端模式
        /// </summary>
        public void confirmMode()
        {
            string path = Path.GetFullPath("modeType");
            if (File.Exists(path))
            {
                // 获取文件中的内容
                string type = File.ReadAllText(path);
                FileUtils.log($"更换模式为：{type}");
                int typeInt = 1;
                if (Int32.TryParse(type, out typeInt))
                {
                    ReplayHttpUtils.developmentMode = typeInt;
                }

            }
        }

        /// <summary>
        /// 是否要强制更新
        /// </summary>
        public void forceUpdate()
        {
            string mandatoryUpdateFile = Path.GetFullPath("updateTemp\\updateType.txt");
            string mandatoryUpdateFilePath = Path.GetFullPath(@"updateTemp\servicePack.zip");
            //检测是否有强制更新的文件存在
            if (File.Exists(mandatoryUpdateFile) && File.Exists(mandatoryUpdateFilePath))
            {
                FileUtils.log("有强制更新。", "版本更新");
                //存在强制更新的文件，启动更新程序，然后爱复盘自己关闭
                FileUtils.StartUpdate();
            }

            // 检查是否有补丁包要更新
            string patchUpdateFile = Path.GetFullPath("updateTemp\\updatePatch.txt");
            string patchUpdateFilePath = Path.GetFullPath(@"updateTemp\servicePatchPack.zip");
            //检测是否有强制更新的文件存在
            if (File.Exists(patchUpdateFile) && File.Exists(patchUpdateFilePath))
            {
                FileUtils.log("有补丁包要更新。", "版本更新");
                //存在强制更新的文件，启动更新程序，然后爱复盘自己关闭
                FileUtils.StartUpdate(1);
            }
        }

        /// <summary>
        /// 创建数据库
        /// </summary>
        public bool CreateSql()
        {
            List<Dictionary<string, object>> dictionarys = GetSqlInit();
            if (dictionarys.Count > 0)
            {
                foreach (Dictionary<string, object> item in dictionarys)
                {
                    if (item.TryGetValue("dbName", out object dbNameValue))
                    {
                        if (item.TryGetValue("slqName", out object sqlValue))
                        {
                            sqliteHelper = new SQLiteHelper(dbNameValue.ToString());
                            sqliteHelper.createDbFile(sqlValue.ToString());
                            FileUtils.log("sqlValue" + sqlValue.ToString());
                        }
                    }
                }
                return true;
            }
            else
            {
                return false;
            }
        }

        public void CreatePage()
        {
            string serverWebUrl = Constant.GetWebServerUrl();
            //webView = new WebView2();
            //webView.Dock = DockStyle.Fill;
            ////打开服务器网页 
            //webView.Source = new Uri(serverWebUrl);

            // 初始化谷歌浏览器 
            //string cachePath = Path.GetFullPath("aifupan-cache");
            //RequestContextSettings requestContextSettings = new RequestContextSettings();
            //requestContextSettings.CachePath = cachePath.Substring(0, cachePath.IndexOf(":")) + ":\\aifupan-cache";
            //webBrower = new ChromiumWebBrowser() {
            //    RequestContext = new RequestContext(requestContextSettings)
            //};
            webBrower = new ChromiumWebBrowser();
            webBrower.Dock = DockStyle.Fill;// 填充方式

            // 设置生命周期处理器，监听新窗口打开事件
            webBrower.LifeSpanHandler = new CustomLifeSpanHandler();

            webBrower.Load(serverWebUrl);
            this.Controls.Add(webBrower);

            // 初始化跟前端双向通讯
            FrontNotice.webBrowser = webBrower;
            FrontNotice.formMain = this;
            webBrower.IsBrowserInitializedChanged += (sender, args) =>
            {
                if (webBrower.IsBrowserInitialized)
                {
                    FrontNotice.InitChromiumWebBrowser();

                }
            };

            // 绑定ChromiumWebBrowser 加载完成事件
            // 现在主要是点击刷新按钮后更新弹窗关闭的问题
            webBrower.FrameLoadEnd += (sender, args) =>
            {
                // 通过 BrowserHost 设置缩放级别
                double zoomLevel = 0;
                if (scaleRatio < 1)
                {
                    zoomLevel = GetPercentZoomLevel(scaleRatio);
                }
                webBrower.SetZoomLevel(zoomLevel);
                if (args.Frame.IsMain && this.isReloadOpenUpdate) // 确保是主框架加载完成 
                {
                    this.isReloadOpenUpdate = false;
                    ReplayHttpUtils.openUpdateVersion(false);
                }
            };
        }

        private double GetPercentZoomLevel(double scaleRatio)
        {
            return ((scaleRatio * 100) - 100) / 20.0; // 返回 -1.0
        }


        private void CreateUUID()
        {
            try
            {
                string uuidPath = Path.GetFullPath("uuid");
                if (File.Exists(uuidPath))
                {
                    ReplayHttpUtils.uuid = File.ReadAllText(uuidPath);
                    if (!string.IsNullOrEmpty(ReplayHttpUtils.uuid))
                    {
                        return;
                    }
                }
                Guid guid = Guid.NewGuid();
                ReplayHttpUtils.uuid = guid.ToString();
                File.WriteAllText(uuidPath, ReplayHttpUtils.uuid);
                FileUtils.log($"{ReplayHttpUtils.uuid}", "生成的uuid");
            }
            catch (Exception ex)
            {
                FileUtils.log($"{ex.Message}", "生成uuid报错");
            }
        }

        private void GetCpuId()
        {
            //string temp = string.Empty;
            //ManagementObjectSearcher searcher = new ManagementObjectSearcher("SELECT * FROM Win32_Processor");

            //foreach (ManagementObject obj in searcher.Get())
            //{
            //    temp = obj["ProcessorId"]?.ToString();
            //    if (!string.IsNullOrEmpty(temp))
            //        break;
            //}
            ReplayHttpUtils.cpuid = SystemUtils.GenerateMachineCode();
        }


        /// <summary>
        /// 初始化视频表和文件表，将分析中的改成分析失败
        /// </summary>
        private void initVideoAndFileAnalysisStatus()
        {
            UploadFile uploadFile = new UploadFile();
            uploadFile.AnalysisStatus = 1;
            List<UploadFile> uploadFileList = uploadFile.GetList();
            if (uploadFileList != null && uploadFileList.Count > 0)
            {
                foreach (var item in uploadFileList)
                {
                    item.AnalysisStatus = 3;
                    item.ErrorReason = "系统被退出";
                    item.update();
                }
            }

            AnchorVideo anchorVideo = new AnchorVideo();
            List<AnchorVideo> anchorVideoList = anchorVideo.GetAnalysisStatusIsOne();
            foreach (var item in anchorVideoList)
            {
                item.AnalysisStatus = 3;
                item.ErrorReason = "系统被退出";
                item.update();
            }
        }

        protected override void OnLoad(EventArgs e)
        {
            base.OnLoad(e);
            Icon = new Icon(FormMain.icoPath);

            // 初始化正常窗口边界
            normalBounds = this.Bounds;

            // 在窗体完全加载后重新设置动态尺寸
            InitializeDynamicSizes();
            UpdateButtonPositions();
            LogButtonSizes();
        }

        protected override void OnResizeEnd(EventArgs e)
        {
            base.OnResizeEnd(e);

            // 如果不是最大化状态，更新正常窗口边界
            if (!isMaximized)
            {
                normalBounds = this.Bounds;
            }
        }

        private void panelTop_MouseDown(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left)
            {
                isDragging = true;
                dragCursorPoint = Cursor.Position;
                dragFormPoint = this.Location;
            }
        }

        /// <summary>
        /// 初始化主播和视频的列表数据
        /// </summary>
        private void initDataCache()
        {
            ////初始化主播的直播状态和录制状态
            //AnchorInfo anchor = new AnchorInfo();
            //anchor.InitRecordAndLiveStatus();
            ////从数据库初始化数据到缓存
            //List<AnchorInfo> anchorInfos = anchor.GetList();
            //if (anchorInfos != null && anchorInfos.Count > 0)
            //{
            //    foreach (AnchorInfo anchorInfo in anchorInfos)
            //    {
            //        anchorInfo.RecordStatus = 0;
            //        anchorInfo.LiveStatus = 0;
            //        AnchorCacheManager.SetAnchorCache(anchorInfo);
            //    }
            //    //FileUtils.log($"主播数据同步到缓存:{JsonConvert.SerializeObject(AnchorCacheManager.GetAllAnchors())}");
            //}
            ////初始化所有的视频数据 将录制状态修改为未录制
            //AnchorVideo anchorVideo = new AnchorVideo();
            //anchorVideo.InitVideoRecordStatus();
            ////从数据库同步数据到缓存
            //List<AnchorVideo> videos = anchorVideo.GetList();
            //if (videos != null && videos.Count > 0)
            //{
            //    foreach (AnchorVideo video in videos)
            //    {
            //        AnchorVideoCacheManager.SetAnchorVideoCache(video);
            //    }
            //}


            //// 删除所有主播
            AnchorInfo anchorInfo = new AnchorInfo();
            anchorInfo.DeleteAll();
            //// 删除所有视频
            //AnchorVideo anchorVideo = new AnchorVideo();
            //anchorVideo.DeleteAll();

            //将配置同步到缓存
            //Config config = new Config();
            //config.Id = 1;
            //config.GetModel();
            //ConfigBll.configCahce.TryAdd("config", config);
        }

        /// <summary>
        /// 冲缓存里面同步数据
        /// </summary>
        private void SetDataBaseFormCache()
        {
            while (!isClose)
            {
                //AnchorCacheManager.SetAnchorToDataBaseFromCache();
                //AnchorCacheManager.DelDataBaseFromCache();
                //AnchorVideoCacheManager.SetAnchorVideoToDataBaseFromCache();
                //AnchorVideoCacheManager.DelDataBaseFromVideoCache();
                //OnlineNumCacheManager.SetOnlineNumToDataBaseFromCache();
                //TotalOnlineNumCacheManager.SetOnlineNumToDataBaseFromCache();
                //VideoViewershipNumCacheManager.SetOnlineNumToDataBaseFromCache();
                //ConcurrentDictionary<string, Config> configCahce = ConfigBll.configCahce;
                //if (configCahce.TryGetValue("config", out Config config))
                //{

                //    config.update();
                //}
                //Thread.Sleep(2000);
            }
        }



        private void KillFFmpegProcesses()
        {
            // 获取所有正在运行的进程
            Process[] processes = Process.GetProcessesByName("ffmpeg");

            // 终止所有找到的ffmpeg进程
            foreach (Process process in processes)
            {
                try
                {
                    process.Kill();
                    FileUtils.log($"Killed process {process.Id}");
                }
                catch (Exception ex)
                {
                    FileUtils.log($"Failed to kill process {process.Id}: {ex.Message}");
                }
            }
        }


        /// <summary>
        /// 如果软件已经打开，则将窗体居中到电脑屏幕中间
        /// </summary>
        private void ShowFrom()
        {
            try
            {
                // 获取所有名为 "a.exe" 的进程
                Process[] processes = Process.GetProcessesByName("ReviewAnalysis");
                // 如果找到了其他的 "a.exe" 实例，则询问用户是否终止它们
                if (processes != null && processes.Length > 0)
                {
                    //说明已经有进程存在
                    foreach (Process process in processes)
                    {
                        if (process.Id != Process.GetCurrentProcess().Id)
                        {
                            isReStart = true;
                        }
                    }

                }

            }
            catch (Exception ex)
            {
                MessageBox.Show($"尝试终止现有实例时出现错误: {ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        /// <summary>
        /// 杀死已经启动的ReviewAnalysis进程
        /// </summary>
        private void KillReviewAnalysisProcesses()
        {
            try
            {
                KillGatherProcesses();
                KillFFmpegProcesses();
                // 获取所有名为 "a.exe" 的进程
                var processes = Process.GetProcessesByName("ReviewAnalysis");
                // 如果找到了其他的 "a.exe" 实例，则询问用户是否终止它们
                foreach (var process in processes)
                {
                    if (process.Id != Process.GetCurrentProcess().Id)
                    {
                        process.Kill();
                        process.WaitForExit();
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"尝试终止现有实例时出现错误: {ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        /// <summary>
        /// 杀死已经启动的Gather进程
        /// </summary>
        private void KillGatherProcesses()
        {

            // 获取所有正在运行的进程
            Process[] processes = Process.GetProcessesByName("Gather");
            // 终止所有找到的ffmpeg进程
            foreach (Process process in processes)
            {
                try
                {
                    process.Kill();
                    FileUtils.log($"杀死Gather进程{process.Id}");
                }
                catch (Exception ex)
                {
                    FileUtils.log($"杀死Gather进程失败 {process.Id}: {ex.Message}");
                }
            }
        }


        private void panelTop_MouseMove(object sender, MouseEventArgs e)
        {
            if (isDragging)
            {
                Point diff = Point.Subtract(Cursor.Position, new Size(dragCursorPoint));
                this.Location = Point.Add(dragFormPoint, new Size(diff));
            }
        }

        private void panelTop_MouseUp(object sender, MouseEventArgs e)
        {
            isDragging = false;
        }

        private void panelTop_MouseEnter(object sender, EventArgs e)
        {
            this.Cursor = Cursors.Hand; // 将鼠标指针变为手指形状
        }

        private void panelTop_MouseLeave(object sender, EventArgs e)
        {
            this.Cursor = Cursors.Default; // 将鼠标指针变回默认形状
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            isClose = true;

            // 停止拉伸定时器
            if (resizeTimer != null)
            {
                resizeTimer.Stop();
                resizeTimer.Dispose();
                resizeTimer = null;
            }

            base.OnFormClosing(e);

            // 关闭B软件
            //tcpHandle.killGather();
            // 杀死所有WebView2进程
            KillAllWebView2Processes();
        }

        private void KillAllWebView2Processes()
        {
            var webview2Processes = Process.GetProcessesByName("WebView2");
            foreach (var process in webview2Processes)
            {
                process.Kill();
            }
        }

        /// <summary>
        /// 杀死指定的端口
        /// </summary>
        /// <param name="port"></param>
        private void CheckAndKillPortProcess(int port)
        {
            // 检查端口是否被占用
            if (IsPortAvailable(port))
            {
                // 获取占用该端口的进程ID
                int processId = GetProcessIdByPort(port);
                if (processId != -1)
                {
                    try
                    {
                        // 杀死进程
                        Process process = Process.GetProcessById(processId);
                        process.Kill();
                        process.WaitForExit();
                        //MessageBox.Show($"已终止进程 {process.ProcessName} (PID: {processId})，释放端口 {port}。");
                        FileUtils.log($"已终止进程 {process.ProcessName} (PID: {processId})，释放端口 {port}。", "检测端口是否被暂用");
                    }
                    catch (Exception ex)
                    {
                        //MessageBox.Show($"无法终止进程: {ex.Message}");
                        FileUtils.log($"无法终止进程: {ex.Message}", "检测端口是否被暂用");
                    }
                }
            }
            else
            {
                //MessageBox.Show($"端口 {port} 未被占用。");
                FileUtils.log($"端口 {port} 未被占用。", "检测端口是否被暂用");
            }
        }

        private bool IsPortInUse(int port)
        {
            IPGlobalProperties ipGlobalProperties = IPGlobalProperties.GetIPGlobalProperties();
            var tcpConnections = ipGlobalProperties.GetActiveTcpConnections();
            return tcpConnections.Any(conn => conn.LocalEndPoint.Port == port);
        }

        /// <summary>
        /// 查询当前系统端口是否被占用 
        /// </summary>
        /// <param name="port">要检查的端口号</param>
        /// <returns>true:端口占用，false:端口未占用</returns>
        public static bool IsPortAvailable(int port)
        {
            // 参数验证
            if (port < 1 || port > 65535)
                return true;

            try
            {
                IPGlobalProperties properties = IPGlobalProperties.GetIPGlobalProperties();

                // 只检查指定端口，而不是获取所有端口
                var tcpListeners = properties.GetActiveTcpListeners()
                    .Where(x => x.Port == port);
                var udpListeners = properties.GetActiveUdpListeners()
                    .Where(x => x.Port == port);

                return tcpListeners.Any() && udpListeners.Any();
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"message = {ex.Message}, stackTrace = {ex.StackTrace}", "IsPortAvailable报错");
                // 如果绑定方式失败，使用系统信息查询
                return IsPortAvailableBySystemInfo(port);
            }
        }

        private static bool IsPortAvailableBySystemInfo(int port)
        {
            try
            {
                using (var client = new TcpClient())
                {
                    client.Connect("127.0.0.1", port);
                    return true; // 连接成功，说明端口被占用
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"message = {ex.Message}, stackTrace = {ex.StackTrace}", "IsPortAvailableBySystemInfo报错");
                return IsPortAvailableBySocket(port); // 无法确定时，保守返回端口被占用
            }
        }

        /// <summary>
        /// 通过Socket绑定检查端口是否可用
        /// </summary>
        /// <param name="port">端口号</param>
        /// <returns>true表示端口占用，false表示未占用</returns>
        public static bool IsPortAvailableBySocket(int port)
        {
            Socket socket = null;
            try
            {
                if (IsPortInUse2(port))
                {
                    return true;
                }

                TcpListener listener = new TcpListener(IPAddress.Loopback, port);
                listener.Start();
                listener.Stop();
                return false; // 绑定成功，端口可用
            }
            catch (SocketException)
            {
                return true; // 绑定失败，端口被占用
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"message = {ex.Message}, stackTrace = {ex.StackTrace}", "IsPortAvailableBySocket报错");
                return true; // 无法确定时，保守返回端口被占用
            }
            finally
            {
                socket?.Close();
                socket?.Dispose();
            }
        }

        public static bool IsPortInUse2(int port)
        {
            IPGlobalProperties ipProperties = IPGlobalProperties.GetIPGlobalProperties();

            // ✅ 检查 TCP 监听端口
            var tcpEndPoints = ipProperties.GetActiveTcpListeners();
            foreach (var endPoint in tcpEndPoints)
            {
                if (endPoint.Port == port)
                    return true;
            }

            // ✅ 检查 UDP 监听端口
            var udpEndPoints = ipProperties.GetActiveUdpListeners();
            foreach (var endPoint in udpEndPoints)
            {
                if (endPoint.Port == port)
                    return true;
            }

            // ✅ 检查 TCP 连接中的端口（例如客户端连接）
            var tcpConnections = ipProperties.GetActiveTcpConnections();
            foreach (var connection in tcpConnections)
            {
                if (connection.LocalEndPoint.Port == port)
                    return true;
            }
            return false;
        }

        private int GetProcessIdByPort(int port)
        {
            try
            {
                var tcpListeners = IPGlobalProperties.GetIPGlobalProperties().GetActiveTcpListeners();
                foreach (var listener in tcpListeners)
                {
                    if (listener.Port == port)
                    {
                        var processes = Process.GetProcesses();
                        foreach (var process in processes)
                        {
                            try
                            {
                                var result = process.MainModule.BaseAddress.ToString();
                                // 这里需要更多的逻辑确定哪个进程在监听该端口
                                // 这部分代码可能需要根据具体情况修改
                                // 对于简化起见，此处仅返回第一个找到的进程ID
                                return process.Id;
                            }
                            catch { }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"message = {ex.Message}, stackTrace = {ex.StackTrace}", "GetProcessIdByPort报错");
            }

            return -1; // 端口未被占用
        }


        private List<Dictionary<string, object>> GetSqlInit()
        {
            string configFilePath = "Config/sqlinit.json";
            if (!File.Exists(configFilePath))
            {
                throw new Exception("缺少sql配置文件");
            }
            string json = File.ReadAllText(configFilePath);
            List<Dictionary<string, object>> sqlInit = JsonConvert.DeserializeObject<List<Dictionary<string, object>>>(json);
            return sqlInit;
        }


        /// <summary>
        /// 检测初始化的配置文件
        /// </summary>
        /// <returns></returns>
        private Dictionary<string, string> CheckInitConfig()
        {
            string fileName = "initconfig.json";
            if (!File.Exists(fileName))
            {

                string dataPath = Path.GetFullPath("DbFile") + "\\review_analysis.db";
                if (File.Exists(dataPath))
                {
                    ConfigBll configBll = new ConfigBll();
                    Config config = configBll.GetModel();
                    if (config != null)
                    {
                        if (config.SerialNumber != null)
                        {
                            string version = config.SerialNumber;
                            Dictionary<string, string> initContent2 = new Dictionary<string, string>();
                            initContent2["version"] = version;
                            initContent2["initTime"] = ServerTimeUtils.getCurrentTimeStr();
                            string initJson2 = JsonConvert.SerializeObject(initContent2);
                            // 使用 File.WriteAllText 方法将 JSON 内容写入文件
                            File.WriteAllText(fileName, initJson2);
                            return initContent2;
                        }
                    }
                }
                //先检测初始化文件是否存在，不存在则创建，斌且写入内容
                Dictionary<string, string> initContent = new Dictionary<string, string>();
                initContent["version"] = Constant.VERSION;
                initContent["initTime"] = ServerTimeUtils.getCurrentTimeStr();
                string initJson = JsonConvert.SerializeObject(initContent);
                // 使用 File.WriteAllText 方法将 JSON 内容写入文件
                File.WriteAllText(fileName, initJson);
                return initContent;


            }
            else
            {
                //如果文件存在，直接返回内容
                string json = File.ReadAllText(fileName);
                Dictionary<string, string> dictionary = JsonConvert.DeserializeObject<Dictionary<string, string>>(json);
                return dictionary;
            }
        }

        /// <summary>
        /// 当电脑已经安装有客户端，又重新安装，并且不对原来的程序进行卸载，执行此方法进行检测
        /// </summary>
        private void CheckInit()
        {
            try
            {
                Dictionary<string, string> dictionary = CheckInitConfig();
                dictionary.TryGetValue("version", out var version);
                //如果获取到的原先版本和安装的版本版本号不一致则执行此操作
                if (version.Trim() != Constant.VERSION.Trim())
                {
                    ReplayHttpUtils.CheckAifupanVersionUpdate2(version);
                    //initconfig.json 文件会在解压移动后 由ReviewAnalysisUpdate.exe程序重新改写，并覆盖
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"错误信息:{ex.ToString()}", "初始化升级");
            }
        }


        private void ExecuteSqlUpdate()
        {
            string sqlPath = Path.GetFullPath("script\\updateSql.json");
            if (File.Exists(sqlPath))
            {
                string json = File.ReadAllText(sqlPath);
                if (json != null && !json.Equals(""))
                {
                    try
                    {
                        List<Dictionary<string, object>> dictionarys = JsonConvert.DeserializeObject<List<Dictionary<string, object>>>(json);
                        foreach (Dictionary<string, object> dictionary in dictionarys)
                        {
                            dictionary.TryGetValue("database", out object database);
                            SQLiteHelper sQLiteHelper = new SQLiteHelper(database.ToString());
                            if (dictionary.TryGetValue("sqlList", out object _sqlList))
                            {
                                List<string> sqlList = ((JArray)_sqlList).ToObject<List<string>>();
                                if (sqlList != null && sqlList.Count() > 0)
                                {
                                    foreach (string sql in sqlList)
                                    {
                                        FileUtils.log($"sql:{sql}", "执行sql语句");
                                        sQLiteHelper.Execute(sql);
                                    }
                                }
                            }
                        }
                        File.Delete(sqlPath);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.log($"错误信息:{ex.Message}", "初始化执行sql更新数据库出错");
                    }
                }

            }

        }

        public void ZuJianInstall(string name)
        {
            string installerPath = $".\\tools\\{name}";

#if DEBUG
            FileUtils.log($"Installing {name} Desktop Runtime...");
#endif
            Process process = new Process();
            process.StartInfo.FileName = installerPath;
            process.StartInfo.Arguments = "/install /quiet /norestart";
            process.StartInfo.UseShellExecute = false;
            process.StartInfo.CreateNoWindow = true;
            process.Start();

            process.WaitForExit();

            if (process.ExitCode == 0)
            {
#if DEBUG
                FileUtils.log($".NET {name} installed successfully.");
#endif

            }
            else
            {
#if DEBUG
                FileUtils.log($"Installation failed with exit code {process.ExitCode}.");
#endif
            }
        }
        private void ZuJian()
        {
            ZuJianInstall("windowsdesktop-runtime-8.0.11-win-x86.exe");
            ZuJianInstall("VC_redist.x64.exe");
        }

        /// <summary>
        /// 获取可用的端口，有最多重试次数
        /// </summary>
        /// <param name="port">端口号</param>
        /// <param name="retryNum">重试次数</param>
        /// <returns>端口号</returns>
        private int getLocalhostPort(int port, int retryNum = 20)
        {
            for (int i = 0; i < retryNum; i++)
            {
                int tempPort = port + i;
                if (!IsPortAvailableBySocket(tempPort))
                {
                    return tempPort;
                }
                FileUtils.LogError($"{tempPort}", "端口被占用");
            }

            FileUtils.LogError($"{port} - {port + retryNum}", "端口全部被占用");
            MessageBox.Show(this, "无端口可用，请联系管理员。", "启动失败", MessageBoxButtons.OK, MessageBoxIcon.Error);
            Application.Exit();
            return 25635;
        }
        /// <summary>
        /// 获取可用本地端口（校验有效性）
        /// </summary>
        /// <param name="basePort">起始端口</param>
        /// <param name="tryCount">尝试次数</param>
        /// <returns>可用端口，失败返回-1</returns>
        private int GetAvailableLocalPort(int basePort, int tryCount)
        {
            for (int i = 0; i < tryCount; i++)
            {
                int port = basePort + i;
                if (port < 1 || port > 65535) continue;

                // 校验端口是否被占用（可选，增强可靠性）
                try
                {
                    var listener = new System.Net.Sockets.TcpListener(System.Net.IPAddress.Loopback, port);
                    {
                        listener.Start();
                        listener.Stop();
                        return port;
                    }
                }
                catch
                {
                    continue;
                }
            }
            return -1;
        }

        private void f12_Click(object sender, EventArgs e)
        {
            // 以非模态窗口打开日志界面（不阻塞主程序操作）
            LogForm.Instance.Show();
            // 检查浏览器是否已初始化且未释放
            if (webBrower != null && webBrower.IsBrowserInitialized)
            {
                webBrower.ShowDevTools();
            }
        }

        private void PictureBox_Click(object sender, EventArgs e)
        {
            // 如果当前正在修复视频，不给刷新
            if (VideoUtils.reEncodeProcessId == -1)
            {
                this.isReloadOpenUpdate = true;
                webBrower.Reload();
            }

            // 测试：手动触发按钮大小更新
            FileUtils.log("手动触发按钮大小更新", "PictureBox_Click");
            InitializeDynamicSizes();
            UpdateButtonPositions();
            LogButtonSizes();
        }
    }
}
