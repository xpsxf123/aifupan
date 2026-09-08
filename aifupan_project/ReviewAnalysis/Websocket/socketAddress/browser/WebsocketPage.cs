using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using CefSharp;
using CefSharp.DevTools;
using CefSharp.DevTools.Network;
using CefSharp.WinForms;
using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.Websocket.socketAddress;

namespace ReviewAnalysis.socketAddress.browser
{
    public partial class WebsocketPage : Form
    {

        public static volatile ConcurrentDictionary<string, BrowserModels> currentChromium = new ConcurrentDictionary<string, BrowserModels>();

        /// <summary>
        /// websocket地址获取之间的间隔时间
        /// </summary>
        public static int Interval = 1000 * 7;

        private static volatile object lockObject = new object();  // 用于线程同步

        private static volatile object lockTask = new object();  // 用于线程同步

        private static bool isRunning = false;

        public WebsocketPage()
        {
            //this.FormBorderStyle = FormBorderStyle.None;
            this.initDeleteCacheFfile();
            InitializeComponent();
        }

        public void initDeleteCacheFfile()
        {
            string rootPath = Path.GetFullPath($"googleCache");
            try
            {
                // 检查路径是否存在
                if (!Directory.Exists(rootPath))
                {
                    return;
                }
                // 获取所有子文件夹
                string[] subDirectories = Directory.GetDirectories(rootPath);

                // 筛选出以 "socket-" 开头的文件夹
                var socketFolders = subDirectories
                    .Where(dir => Path.GetFileName(dir).StartsWith("socket-", StringComparison.OrdinalIgnoreCase))
                    .ToList();

                if (!socketFolders.Any())
                {
                    return;
                }

                // 删除每个符合条件的文件夹
                foreach (string folder in socketFolders)
                {
                    try
                    {
                        // 删除文件夹及其所有内容
                        Directory.Delete(folder, true);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"path = {folder}, msg = {ex.Message}", "文件夹路径删除失败");
                    }
                }

            }
            catch ( Exception ex )
            {
                FileUtils.LogError($"msg = {ex.Message}", "初始化时删除socket窗体缓存文件夹失败");
            }
        }

        private void web_Load(object sender, EventArgs e)
        {
            //Show();
            isRunning = true;
        }

        private void frmWeb_Paint(object sender, PaintEventArgs e)
        {
            //this.Hide();
            this.Visible = false;
        }

        public void addUrl(string url, string type = "DouYinLive", long maxGetTime = 30 * 1000, bool isPorxy = false)
        {
            if (string.IsNullOrEmpty(url)) return;
            // 同步锁
            lock (lockObject)
            {
                currentChromium.TryGetValue(url, out BrowserModels models);
                if (models == null)
                {
                    string cachePath = Path.GetFullPath($"googleCache\\socket-{Guid.NewGuid().ToString("N")}");// 设置你想要的缓存路径
                    var requestContextSettings = new RequestContextSettings { CachePath = cachePath };
                    var requestContext = new RequestContext(requestContextSettings);

                    BrowserModels browserModels = new BrowserModels
                    {
                        url = url,
                        chromiumWeb = new ChromiumWebBrowser(url, requestContext),
                        cachePath = cachePath,
                        timeoutTimer = new System.Timers.Timer(maxGetTime),
                        status = 0,
                        type = type
                    };

                    if (browserModels.type == "DouYinLive")
                    {
                        // 设置谷歌浏览器 - 使用自定义的静音请求处理器
                        browserModels.chromiumWeb.RequestHandler = new ICustomRequestHandler();
                        browserModels.chromiumWeb.FrameLoadEnd += (a, b) => WebBrower_FrameLoadEnd(a, b, browserModels);
                        browserModels.chromiumWeb.IsBrowserInitializedChanged += async (a, b) => await OnwebInitAsync(a, b, browserModels);
                        // 添加页面加载状态变化事件，在开始加载时就静音
                        browserModels.chromiumWeb.LoadingStateChanged += (a, b) => OnLoadingStateChanged(a, b, browserModels);
                        // 添加帧加载开始事件，更早注入静音脚本
                        browserModels.chromiumWeb.FrameLoadStart += (a, b) => OnFrameLoadStart(a, b, browserModels);
                        browserModels.chromiumWeb.Dock = DockStyle.Top;// 填充方式

                        // 设置代理
                        if (isPorxy)
                        {
                            settingProxy(browserModels.chromiumWeb, requestContext);
                        }

                        browserModels.chromiumWeb.Load(url);
                    }


                    // 设置30秒超时
                    browserModels.timeoutTimer.AutoReset = false; // 只触发一次
                    browserModels.timeoutTimer.Elapsed += (a, b) => timeout(browserModels);

                    // 添加到队列中
                    currentChromium.AddOrUpdate(url, browserModels, (a, b) => browserModels);
                }

                if (isRunning)
                {
                    Task.Run(() =>
                    {
                        if (isRunning)
                        {
                            isRunning = false;
                            startAdd();
                            isRunning = true;
                        }
                    });
                }
            }
        }

        public void settingProxy(ChromiumWebBrowser webBrowser, RequestContext requestContext)
        {
            if (webBrowser == null || requestContext == null) return;
            try
            {
                ProxyIpVo proxy = ProxyApi.GetProxyIpSync();
                if (proxy == null)
                {
                    FileUtils.log("代理商获取，浏览器不设置代理", "获取代理失败");
                    return;
                }
                Task task = Cef.UIThreadTaskFactory.StartNew(() =>
                {
                    try
                    {
                        webBrowser.RequestHandler = new ProxyRequestHandler(proxy.proxyUsername, proxy.proxyPassword);
                        string address = $"{proxy.ip}:{proxy.port}";

                        var proxyDict = new Dictionary<string, object>
                        {
                            ["mode"] = "fixed_servers",
                            ["server"] = address
                        };

                        bool success = requestContext.SetPreference("proxy", proxyDict, out string error);
                        if (!success)
                            FileUtils.LogError($"{address}，错误问题：{error}", "浏览器代理设置失败");
                        else
                            FileUtils.log(address, "浏览器代理设置成功");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError(ex.Message, "浏览器设置代理报错");
                    }
                });
                task.Wait();
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "浏览器设置代理报错");

            }
        }

        public void settingProxy(ChromiumWebBrowser webBrowser)
        {
            if (webBrowser == null) return;
            try
            {
                ProxyIPUtils.SetProxyIpSync();
                // 判断是否要设置代理
                if (!ProxyIPUtils.isUseProxy || ProxyIPUtils.currentProxyIp == null)
                {
                    webBrowser.RequestHandler = new ProxyRequestHandler();
                    return;
                }
                string address = "";
                ProxyIpVo proxy = ProxyIPUtils.currentProxyIp;
                webBrowser.RequestHandler = new ProxyRequestHandler(proxy.proxyUsername, proxy.proxyPassword);
                Task task = Cef.UIThreadTaskFactory.StartNew(delegate
                {
                    try
                    {
                        var rc = webBrowser.GetBrowser().GetHost().RequestContext;
                        rc.GetAllPreferences(true);
                        var dict = new Dictionary<string, object>();
                        if (ProxyIPUtils.isUseProxy && ProxyIPUtils.currentProxyIp != null)
                        {
                            dict.Add("mode", "fixed_servers");
                            address = $"{proxy.ip}:{proxy.port}";
                            dict.Add("server", address); //此处替换成实际 ip地址：端口
                        }
                        else
                        {
                            dict.Add("mode", "direct"); // 设为direct表示取消代理
                        }
                        string error;
                        bool success = rc.SetPreference("proxy", dict, out error);
                        if (!success)
                        {
                            FileUtils.LogError((!string.IsNullOrEmpty(address) ? address : "不设置代理") + $"，错误问题：{error}", "浏览器代理设置失败");
                        }
                        else
                        {
                            FileUtils.log(!string.IsNullOrEmpty(address) ? address : "不设置代理", "浏览器代理设置成功");
                        }
                    }
                    catch (Exception e)
                    {
                        FileUtils.LogError(e.Message, "浏览器代理设置报错");
                    }
                });
                task.Wait();
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "浏览器设置代理报错");
            }
        }
        private void complete(BrowserModels browserModels)
        {
            try
            {
                browserModels.status = 2;
                if(browserModels.timeoutTimer != null)
                browserModels.timeoutTimer.Enabled = false; // 关闭定时器
                browserModels.timeoutTimer?.Dispose();
                browserModels.timeoutTimer = null;
                currentChromium.TryRemove(browserModels.url, out var temp);
                SocketAddressInit.TriggerGetWebsocketAddress(browserModels.url, browserModels.ttwid, browserModels.wssUrl);
                operationControls(browserModels, 1);
                browserModels = null;
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "浏览器设置-完成方法报错");
            }

        }

        private void timeout(BrowserModels browserModels)
        {
            if (!string.IsNullOrEmpty(browserModels.ttwid) && !string.IsNullOrEmpty(browserModels.wssUrl))
            {
                complete(browserModels);
            }
            else
            {
                browserModels.timeoutTimer.Enabled = false;
                browserModels.timeoutTimer?.Dispose();
                browserModels.timeoutTimer = null;
                SocketAddressInit.TriggerGetWebsocketTimeout(browserModels.url);
                currentChromium.TryRemove(browserModels.url, out var temp);
                operationControls(browserModels, 1);
                browserModels = null;
            }
        }

        private void startAdd()
        {
            Debug.WriteLine("执行------------等待锁------------------------");
            lock (lockTask)
            {
                var keys = currentChromium.Keys.ToList();
                Debug.WriteLine($"keys={keys}");
                if (keys.Count > 0)
                {
                    foreach (var key in keys)
                    {
                        if (currentChromium.TryGetValue(key, out BrowserModels temp) && temp.status == 0)
                        {
                            temp.status = 1;
                            operationControls(temp, 0);
                            temp.timeoutTimer.Enabled = true;
                            Debug.WriteLine($"------------------------------------添加完成key={key}------------------------------------------------");
                            // 两个url加载的间隔 
                            Thread.Sleep(Interval);
                        }
                    }
                    // 二次判断数组是否还有
                    var keys1 = currentChromium.Keys.ToList();
                    if (keys1.Count > 0)
                    {
                        foreach (var key in keys1)
                        {
                            if (currentChromium.TryGetValue(key, out BrowserModels temp) && temp.status == 0)
                            {
                                startAdd();
                                break;
                            }
                        }
                    }
                }
                else
                {
                    return;
                }
            }
        }

        /// <summary>
        /// 操作Controls
        /// </summary>
        /// <param name="temp"></param>
        /// <param name="type">0添加控件，1删除控件</param>
        private void operationControls(BrowserModels temp, int type)
        {
            if (this.InvokeRequired)
            {
                this.BeginInvoke(new Action(() =>
                {
                    switch (type)
                    {
                        case 0:
                            // 强制创建控件
                            temp.chromiumWeb.CreateControl();
                            this.Controls.Add(temp?.chromiumWeb);
                            break;
                        case 1:
                            this.Controls.Remove(temp?.chromiumWeb);
                            // 释放控件资源
                            temp?.chromiumWeb?.Dispose();
                            // 置空引用
                            temp.chromiumWeb = null;
                            break;
                        default:
                            break;
                    }
                    //ControlsAddOrRemove(this, temp, type);
                }));
            }
            else
            {
                //ControlsAddOrRemove(this, temp, type);
                switch (type)
                {
                    case 0:
                        this.Controls.Add(temp?.chromiumWeb);
                        break;
                    case 1:
                        this.Controls.Remove(temp?.chromiumWeb);
                        // 释放控件资源
                        temp?.chromiumWeb.Dispose();
                        // 置空引用
                        temp.chromiumWeb = null;
                        break;
                    default:
                        break;
                }
            }
        }

        private void WebBrower_FrameLoadEnd(object sender, FrameLoadEndEventArgs b, BrowserModels browserModels)
        {
            //注册获取cookie回调事件
            ICookieManager cookieManager = browserModels.chromiumWeb.GetCookieManager();
            CookieVisitor visitor = new CookieVisitor();
            visitor.SendCookie += (c) => visitor_SendCookie(c, browserModels);
            cookieManager.VisitAllCookies(visitor);
            
            // 方案2：多层静音设置 - 第三层：页面加载完成后再次设置静音
            try
            {
                browserModels.chromiumWeb.GetBrowser().GetHost().SetAudioMuted(true);
            }
            catch (Exception e)
            {
                FileUtils.LogError(e.Message, "设置静音错误");
            }

            // 方案3：注入JavaScript静音所有媒体元素（包括动态添加的）
            try
            {
                string muteScript = @"
                    (function() {
                        // 静音现有的video和audio元素
                        var videos = document.querySelectorAll('video');
                        var audios = document.querySelectorAll('audio');
                        videos.forEach(function(v) { v.muted = true; v.pause(); });
                        audios.forEach(function(a) { a.muted = true; a.pause(); });
                        
                        // 监听新添加的媒体元素并自动静音
                        var observer = new MutationObserver(function(mutations) {
                            mutations.forEach(function(mutation) {
                                mutation.addedNodes.forEach(function(node) {
                                    if (node.nodeType === 1) { // 元素节点
                                        if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO') {
                                            node.muted = true;
                                            node.pause();
                                        }
                                        if (node.querySelectorAll) {
                                            var mediaElements = node.querySelectorAll('video, audio');
                                            mediaElements.forEach(function(el) {
                                                el.muted = true;
                                                el.pause();
                                            });
                                        }
                                    }
                                });
                            });
                        });
                        observer.observe(document.body || document.documentElement, { 
                            childList: true, 
                            subtree: true 
                        });
                    })();
                ";
                browserModels.chromiumWeb.ExecuteScriptAsync(muteScript);
                FileUtils.log(browserModels.url, "JavaScript注入静音脚本成功");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"JavaScript静音脚本注入失败: {ex.Message}", "WebsocketPage.WebBrower_FrameLoadEnd");
            }
        }

        /// 回调事件
        public void visitor_SendCookie(CefSharp.Cookie obj, BrowserModels browserModels)
        {
            string Domain = obj?.Domain?.TrimStart('.')?.Trim();

            if (Domain?.ToLower() == "douyin.com" && obj?.Name == "ttwid")
            {
                browserModels.ttwid = obj.Value;
                if (!string.IsNullOrEmpty(browserModels.ttwid) && !string.IsNullOrEmpty(browserModels.wssUrl))
                {
                    complete(browserModels);
                }
            }
        }


        private async Task OnwebInitAsync(object a, EventArgs b, BrowserModels browserModels)
        {
            if (browserModels.chromiumWeb.IsBrowserInitialized)
            {
                // 方案2：多层静音设置 - 第一层：浏览器级别静音
                try
                {
                    browserModels.chromiumWeb.GetBrowser().GetHost().SetAudioMuted(true);
                    FileUtils.log(browserModels.url, "浏览器初始化完成 -> 已静音(SetAudioMuted)");
                }
                catch (Exception ex)
                {
                    FileUtils.LogError(ex.Message, "浏览器静音设置失败");
                }

                DevToolsClient DTC1 = browserModels.chromiumWeb.GetDevToolsClient();

                // 方案2：多层静音设置 - 第二层：通过DevTools浏览器主机再次静音
                try
                {
                    // 禁用音频输出
                    var browser = browserModels.chromiumWeb.GetBrowser();
                    if (browser != null && browser.GetHost() != null)
                    {
                        browser.GetHost().SetAudioMuted(true);
                        FileUtils.log(browserModels.url, "DevTools -> 音频已禁用");
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"DevTools媒体禁用失败: {ex.Message}", "WebsocketPage.OnwebInitAsync");
                }

                await DTC1.Network.EnableAsync();

                DTC1.Network.WebSocketCreated += (a1, b1) => OnWebSocketCreated(a1, b1, browserModels);
            }
        }

        private void OnWebSocketCreated(object o, WebSocketCreatedEventArgs e, BrowserModels browserModels)
        {
            browserModels.wssUrl = e.Url;
            if (!string.IsNullOrEmpty(browserModels.ttwid) && !string.IsNullOrEmpty(browserModels.wssUrl))
            {
                complete(browserModels);
            }
        }

        /// <summary>
        /// 页面加载状态变化事件 - 在页面开始加载时立即静音
        /// </summary>
        private void OnLoadingStateChanged(object sender, LoadingStateChangedEventArgs e, BrowserModels browserModels)
        {
            // 当页面开始加载时（IsLoading=true），立即设置静音
            if (e.IsLoading)
            {
                try
                {
                    var browser = browserModels.chromiumWeb.GetBrowser();
                    if (browser != null && !browser.IsDisposed)
                    {
                        browser.GetHost().SetAudioMuted(true);
                    }
                }
                catch (Exception ex)
                {
                    // 忽略错误，因为此时浏览器可能还未完全初始化
                }
            }
        }

        /// <summary>
        /// 帧加载开始时立即注入静音脚本（比FrameLoadEnd更早）
        /// </summary>
        private void OnFrameLoadStart(object sender, FrameLoadStartEventArgs e, BrowserModels browserModels)
        {
            try
            {
                // 立即设置静音
                var browser = browserModels.chromiumWeb.GetBrowser();
                if (browser != null && !browser.IsDisposed)
                {
                    browser.GetHost().SetAudioMuted(true);
                }

                // 注入早期静音脚本
                string earlyMuteScript = @"
                    (function() {
                        // 静音所有现有的媒体元素
                        var videos = document.querySelectorAll('video');
                        var audios = document.querySelectorAll('audio');
                        videos.forEach(function(v) { v.muted = true; });
                        audios.forEach(function(a) { a.muted = true; });
                        
                        // 重写HTMLMediaElement.prototype.play，强制静音
                        var originalPlay = HTMLMediaElement.prototype.play;
                        HTMLMediaElement.prototype.play = function() {
                            this.muted = true;
                            return originalPlay.apply(this, arguments);
                        };
                    })();
                ";
                e.Frame.ExecuteJavaScriptAsync(earlyMuteScript);
            }
            catch (Exception ex)
            {
                // 忽略错误
            }
        }
    }
}
