using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Threading.Tasks;
using System.Web;
using System.Windows.Forms;
using CefSharp;
using CefSharp.WinForms;
using douyin.Utils;
using ReviewAnalysis.bo.system;
using ReviewAnalysis.Controller;
using Cookie = CefSharp.Cookie;

namespace ReviewAnalysis.Utils
{


    /// <summary>
    /// 自定义生命周期处理器，用于监听和处理新窗口打开事件
    /// 功能：
    /// 1. 监听前端新打开的标签页（window.open()）
    /// 2. 在新窗口标题中显示网页地址
    /// 3. 支持在主窗口中打开新标签页或在新窗口中打开
    /// 4. 监听新窗口的 URL 变化
    /// </summary>
    public class CustomLifeSpanHandler : ILifeSpanHandler
    {
        private static readonly object customLock = new object();


        /// <summary>
        /// 在新窗口打开前触发（如 window.open()）
        /// 这是拦截和处理新窗口打开的最佳位置
        /// </summary>
        public bool OnBeforePopup(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, string targetUrl, string targetFrameName, WindowOpenDisposition targetDisposition, bool userGesture, IPopupFeatures popupFeatures, IWindowInfo windowInfo, IBrowserSettings browserSettings, ref bool noJavascriptAccess, out IWebBrowser newBrowser)
        {
            try
            {
                string key = "aifupanopenurl:";
                if (targetUrl.StartsWith(key))
                {
                    string url = targetUrl.Replace(key, "");
                    SystemController systemController = new SystemController();
                    OpenUrlBo openUrlBo = new OpenUrlBo();
                    openUrlBo.url = url;
                    systemController.OpenUrl(openUrlBo);
                    //Process.Start(new ProcessStartInfo(url)
                    //{
                    //    UseShellExecute = true
                    //});
                    // 按照 CefSharp 要求：自定义窗口时返回 true，且 newBrowser 设为 null
                    newBrowser = null;
                    return true;
                }

                if (targetUrl.StartsWith("http://localhost") || targetUrl.StartsWith("http://127.0.0.1"))
                {
                    newBrowser = null;
                    return false;
                }

                // ===== 抖音授权处理 =====
                if (IsDouyinUrl(targetUrl))
                {
                    string secUid = ExtractSecUid(targetUrl, frame);
                    if (!string.IsNullOrEmpty(secUid))
                    {
                        return CreateDouyinForm(targetUrl, secUid, out newBrowser);
                    }
                }
                // ===== 结束抖音处理 =====
                
                // ===== 快手授权处理 =====
                if (IsKuaishouUrl(targetUrl))
                {
                    FileUtils.LogRpa($"检测到快手 URL: {targetUrl}", "快手授权");
                    FileUtils.LogRpa($"frame.Url: {(frame != null ? frame.Url : "frame is null")}", "快手授权");
                    
                    // 快手主播的 secUid 存储的是 originUserId（原始用户ID）
                    // 优先从 frame.Url（管理后台）提取 secUid 参数
                    string secUid = ExtractKuaishouSecUid(targetUrl, frame);
                    FileUtils.LogRpa($"提取到的快手 secUid: {secUid ?? "null"}", "快手授权");
                    
                    if (!string.IsNullOrEmpty(secUid))
                    {
                        FileUtils.LogRpa($"创建快手窗体，secUid: {secUid}", "快手授权");
                        return CreateKuaishouForm(targetUrl, secUid, out newBrowser);
                    }
                    else
                    {
                        FileUtils.LogRpa("快手 secUid 为空，窗体未创建，将使用默认浏览器", "快手授权");
                    }
                }
                // ===== 结束快手处理 =====
                // 创建新的 ChromiumWebBrowser 实例
                var newChromiumBrowser = new ChromiumWebBrowser(targetUrl);

                // 设置 LifeSpanHandler（递归处理弹窗）
                newChromiumBrowser.LifeSpanHandler = this;

                // 创建承载浏览器的新窗口
                Form newForm = new Form
                {
                    Text = targetUrl,
                    Size = new Size(1024, 768),
                    StartPosition = FormStartPosition.CenterScreen
                };

                // 关键：注册到全局管理器
                //PopupWindowManager.Instance.RegisterPopup(newForm, newChromiumBrowser);

                // 订阅 TitleChanged 事件，使窗口标题随网页标题变化
                newChromiumBrowser.TitleChanged += (sender, args) =>
                {
                    lock (customLock)
                    {
                        if (newChromiumBrowser != null && newForm.IsHandleCreated && newForm.Visible && !newForm.IsDisposed)
                        {
                            newForm.Invoke((MethodInvoker)delegate
                            {
                                if (!newForm.IsDisposed && !newChromiumBrowser.IsDisposed)
                                {
                                    newForm.Text = $"{args.Title ?? "无标题"} - {newChromiumBrowser.Address}";
                                    exeJs(newChromiumBrowser, newChromiumBrowser.Address);
                                }
                            });
                        }
                    }
                };

                // 可选：监听页面加载完成
                newChromiumBrowser.LoadingStateChanged += (s, e) =>
                {
                    lock (customLock)
                    {
                        if (newChromiumBrowser != null && !e.IsLoading && e.Browser.MainFrame.Url == targetUrl && !newChromiumBrowser.IsDisposed)
                        {
                            FileUtils.log($"--------------------------------新窗口加载完成: {targetUrl}");
                            exeJs(newChromiumBrowser, targetUrl);
                        }
                    }
                };

                // 关闭时释放资源（关键！）
                newForm.FormClosing += (s, e) =>
                {
                    //e.Cancel = true; // 拦截关闭
                    //_ = Task.Run(() => PopupWindowManager.Instance.RequestGracefulCloseAsync(newForm));
                    Task.Run(() =>
                    {
                        lock (customLock)
                        {
                            // 4. 真正释放资源
                            try
                            {
                                if (newChromiumBrowser != null && !newChromiumBrowser.IsDisposed)
                                {
                                    newChromiumBrowser?.Dispose();
                                }
                            }
                            catch (Exception ex)
                            {
                                FileUtils.log($"[PopupManager] Dispose 异常: {ex.Message}");
                            }
                        }
                    });
                };

                // 将浏览器控件添加到窗体
                newChromiumBrowser.Dock = DockStyle.Fill;
                newForm.Controls.Add(newChromiumBrowser);

                // 显示新窗口
                newForm.Show();

                // 按照 CefSharp 要求：自定义窗口时返回 true，且 newBrowser 设为 null
                newBrowser = null;
                return true;
            }
            catch(Exception ex)
            {
                FileUtils.LogError($"msg = {ex.Message}, s = {ex.StackTrace}", "打开新窗体错误");
            }
            newBrowser = null;
            return false;
        }

        public void exeJs(ChromiumWebBrowser newChromiumBrowser, string url)
        {

            string script = $@"
                (function() {{
                    const BUTTON_ID = 'injected-top-right-button';

                    // 1. 如果已存在，先移除旧按钮
                    const existingButton = document.getElementById(BUTTON_ID);
                    if (existingButton) {{
                        existingButton.remove();
                    }}

                    // 2. 创建新按钮
                    const button = document.createElement('button');
                    button.id = BUTTON_ID;
                    button.innerText = '浏览器打开';

                    // 3. 设置基础样式（默认半透明）
                    button.style.cssText = `
                        position: fixed;
                        top: 10px;
                        right: 28%;
                        z-index: 999999;
                        padding: 10px 16px;
                        background-color: #FF5722;
                        color: white;
                        border: none;
                        border-radius: 4px;
                        cursor: pointer;
                        font-size: 14px;
                        box-shadow: 0 2px 6px rgba(0,0,0,0.3);
                        opacity: 0.6;               /* 默认半透明 */
                        transition: opacity 0.2s ease; /* 平滑过渡 */
                    `;

                    // 4. 悬停时变为完全不透明
                    button.onmouseenter = () => button.style.opacity = '1';
                    button.onmouseleave = () => button.style.opacity = '0.6';

                    // 5. 绑定点击事件
                    button.onclick = function() {{
                        // 使用 window.open 打开自定义协议（会调用系统默认应用）
                        window.open('aifupanopenurl:{url}', '_blank', 'noopener,noreferrer');
                    }};

                    // 6. 添加到页面
                    document.body.appendChild(button);
                }})();
            ";
            try
            {
                newChromiumBrowser.EvaluateScriptAsync(script);
            }
            catch(Exception ex)
            {
                FileUtils.LogError(ex.Message, "ChromiumBrowser执行js失败");
            }
           
        }

        /// <summary>
        /// 在新窗口创建后触发
        /// 此时可以获取新窗口的信息并更新标题
        /// 同时为新窗口添加 LoadHandler 以监听 URL 变化
        /// </summary>
        public void OnAfterCreated(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
        }

        /// <summary>
        /// 在窗口关闭前触发
        /// </summary>
        public bool DoClose(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
            // 返回 false 表示允许窗口关闭
            // 返回 true 表示阻止窗口关闭

            return false;
        }

        /// <summary>
        /// 在窗口关闭后触发
        /// </summary>
        public void OnBeforeClose(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {

        }

        #region 抖音授权处理

        /// <summary>
        /// 判断是否是抖音 URL
        /// </summary>
        private bool IsDouyinUrl(string url)
        {
            return url.Contains("douyin.com") || url.Contains("iesdouyin.com");
        }

        /// <summary>
        /// 检查浏览器中是否有 sessionid Cookie（登录成功的标志）
        /// </summary>
        /// <returns>返回 sessionid 值，没有则返回 null</returns>
        private async Task<string> GetSessionIdValue(ChromiumWebBrowser browser)
        {
            try
            {
                var cookieManager = browser.GetCookieManager();
                var cookies = new List<Cookie>();
                var visitor = new DouyinCookieCollector(cookies);
                cookieManager.VisitAllCookies(visitor);
                
                // 等待 cookie 收集完成
                await Task.Delay(500);
                
                // 检查是否有 sessionid
                var sessionCookie = cookies.Find(c => c.Name.Contains("sessionid"));
                if (sessionCookie != null && !string.IsNullOrEmpty(sessionCookie.Value))
                {
                    string sessionId = sessionCookie.Value;
                    FileUtils.LogRpa($"检测到 sessionid: {sessionId.Substring(0, Math.Min(20, sessionId.Length))}...", "抖音授权");
                    return sessionId;
                }
                
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检查 sessionid 异常: {ex.Message}", "抖音授权");
                return null;
            }
        }

        /// <summary>
        /// 获取抖音独立缓存路径（每个主播隔离）
        /// </summary>
        private string GetDouyinCachePath(string secUid)
        {
            try
            {
                string baseCachePath = Path.GetFullPath("googleCache");
                string md5Str = DouyinUtils.getCachePathMd5(secUid);
                string cachePath = Path.Combine(baseCachePath, $"douyin-{md5Str}");
                
                if (!Directory.Exists(cachePath))
                {
                    Directory.CreateDirectory(cachePath);
                }
                
                return cachePath;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取抖音缓存路径异常：{ex.Message}", "抖音授权");
                // 返回默认路径
                return Path.GetFullPath("googleCache");
            }
        }

        /// <summary>
        /// 提取 secUid（优先从 frame.Url 提取当前主播，其次从 targetUrl 提取）
        /// </summary>
        private string ExtractSecUid(string targetUrl, IFrame frame)
        {
            try
            {
                // 方式1：从 frame.Url（当前页面URL）提取主播 secUid
                // 前端页面 URL 可能格式：http://localhost:port/anchor?secUid=xxx 或 /anchor/xxx
                if (frame != null && !string.IsNullOrEmpty(frame.Url))
                {
                    string frameSecUid = ExtractSecUidFromUrl(frame.Url);
                    if (!string.IsNullOrEmpty(frameSecUid))
                    {
                        FileUtils.LogRpa($"从当前页面 URL 提取到 secUid：{frameSecUid}", "抖音授权");
                        return frameSecUid;
                    }
                }

                // 方式2：从 targetUrl 提取
                string targetSecUid = ExtractSecUidFromUrl(targetUrl);
                if (!string.IsNullOrEmpty(targetSecUid))
                {
                    FileUtils.LogRpa($"从目标 URL 提取到 secUid：{targetSecUid}", "抖音授权");
                    return targetSecUid;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"提取 secUid 异常：{ex.Message}", "抖音授权");
            }
            return null;
        }

        /// <summary>
        /// 从 URL 中提取 secUid
        /// </summary>
        private string ExtractSecUidFromUrl(string url)
        {
            try
            {
                // 方式1：从 URL 参数提取 secUid 或 sec_uid
                var uri = new Uri(url);
                var query = HttpUtility.ParseQueryString(uri.Query);
                
                string secUid = query["secUid"];
                if (!string.IsNullOrEmpty(secUid))
                    return secUid;

                secUid = query["sec_uid"];
                if (!string.IsNullOrEmpty(secUid))
                    return secUid;

                // 方式2：从 URL 路径提取 /user/xxx 或 /anchor/xxx
                var match = System.Text.RegularExpressions.Regex.Match(url, @"/(user|anchor)/([^/?#]+)");
                if (match.Success)
                    return match.Groups[2].Value;

                // 方式3：从分享链接中的 sec_uid 提取
                match = System.Text.RegularExpressions.Regex.Match(url, @"sec_uid=([^&]+)");
                if (match.Success)
                    return match.Groups[1].Value;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从 URL 提取 secUid 异常：{ex.Message}", "抖音授权");
            }
            return null;
        }

        /// <summary>
        /// 创建抖音窗体（主播主页直接打开，Cookie有效时加载登录状态）
        /// </summary>
        private bool CreateDouyinForm(string targetUrl, string secUid, out IWebBrowser newBrowser)
        {
            newBrowser = null;

            try
            {
                // 检查 Cookie 是否有效
                bool cookieValid = DouyinUtils.CheckCookieValid(secUid);
                // 主播主页是公开页面，始终直接打开
                string startUrl = targetUrl;

                FileUtils.LogRpa($"创建抖音窗体，Cookie有效：{cookieValid}，起始URL：{startUrl}", "抖音授权");

                // 为每个主播创建独立的缓存路径
                string cachePath = GetDouyinCachePath(secUid);
                var requestContextSettings = new RequestContextSettings
                {
                    CachePath = cachePath,
                    PersistSessionCookies = false  // 不自动持久化 session cookie，由我们手动控制
                };
                var requestContext = new RequestContext(requestContextSettings);

                // 创建浏览器
                var newChromiumBrowser = new ChromiumWebBrowser(startUrl)
                {
                    Dock = DockStyle.Fill,
                    LifeSpanHandler = this,
                    RequestContext = requestContext
                };

                // 创建窗体
                Form newForm = new Form
                {
                    Text = $"抖音页面 - {targetUrl}",
                    Size = new Size(1000, 700),
                    StartPosition = FormStartPosition.CenterScreen,
                    FormBorderStyle = FormBorderStyle.Sizable,
                    MaximizeBox = true,
                    MinimizeBox = true
                };

                // 订阅 TitleChanged 事件，更新窗体标题显示当前URL
                newChromiumBrowser.TitleChanged += (sender, args) =>
                {
                    if (newForm.IsHandleCreated && !newForm.IsDisposed && newChromiumBrowser != null && !newChromiumBrowser.IsDisposed)
                    {
                        newForm.Invoke((MethodInvoker)delegate
                        {
                            if (!newForm.IsDisposed)
                            {
                                newForm.Text = $"抖音页面 - {args.Title ?? "无标题"} - {newChromiumBrowser.Address}";
                            }
                        });
                    }
                };

                // Cookie 有效时加载 Cookie
                if (cookieValid)
                {
                    newChromiumBrowser.IsBrowserInitializedChanged += async (sender, e) =>
                    {
                        if (newChromiumBrowser.IsBrowserInitialized)
                        {
                            await DouyinUtils.LoadCookiesToBrowser(secUid, newChromiumBrowser);
                        }
                    };
                }
                
                // 启动统一的状态检测定时器（同时处理登录和退出）
                bool cookieSaved = cookieValid;  // Cookie 有效时标记为已保存
                string lastSessionId = null;  // 上一次的 sessionid 值
                bool initialized = false;  // 是否已初始化
                int initDelay = 0;  // 初始化延迟计数
                string cookiePath = DouyinUtils.getCookiePath(secUid);
                
                var stateTimer = new System.Windows.Forms.Timer();
                stateTimer.Interval = 2000; // 每2秒检查一次
                stateTimer.Tick += async (sender, e) =>
                {
                    if (newChromiumBrowser.IsDisposed)
                    {
                        stateTimer.Stop();
                        stateTimer.Dispose();
                        return;
                    }
                    
                    try
                    {
                        // 获取当前 sessionid 值
                        string currentSessionId = await GetSessionIdValue(newChromiumBrowser);
                        bool hasSession = !string.IsNullOrEmpty(currentSessionId);
                        
                        FileUtils.LogRpa($"状态检测 - initDelay: {initDelay}, hasSession: {hasSession}, cookieSaved: {cookieSaved}", "抖音授权");
                        
                        // 前3次（6秒）用于初始化，获取页面加载后的真实状态
                        if (!initialized)
                        {
                            initDelay++;
                            if (initDelay >= 3)
                            {
                                initialized = true;
                                lastSessionId = currentSessionId;
                                FileUtils.LogRpa($"状态检测初始化完成 - hasSession: {hasSession}", "抖音授权");
                            }
                            return;
                        }
                        
                        // 检测登录：sessionid 值发生变化（新账号登录）
                        if (hasSession && currentSessionId != lastSessionId && !cookieSaved)
                        {
                            cookieSaved = true;
                            FileUtils.LogRpa($"抖音登录成功（sessionid变化），准备保存 Cookie", "抖音授权");
                            await SaveDouyinCookies(newChromiumBrowser, secUid);
                            newForm.Text = "抖音页面 - Cookie已保存";
                        }
                        // 检测退出：session 从有到无（上一次有，现在无）
                        else if (!hasSession && !string.IsNullOrEmpty(lastSessionId))
                        {
                            FileUtils.LogRpa($"检测到 session 消失 - lastSessionId: {lastSessionId?.Substring(0, Math.Min(10, lastSessionId?.Length ?? 0))}...", "抖音授权");
                            
                            if (File.Exists(cookiePath))
                            {
                                try
                                {
                                    File.Delete(cookiePath);
                                    cookieSaved = false;
                                    FileUtils.LogRpa($"检测到退出登录，已删除 Cookie 文件：{cookiePath}", "抖音授权");
                                    newForm.Text = "抖音页面 - 已退出登录";
                                }
                                catch (Exception ex)
                                {
                                    FileUtils.LogRpa($"删除 Cookie 文件异常：{ex.Message}", "抖音授权");
                                }
                            }
                        }
                        
                        lastSessionId = currentSessionId;
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"状态检测异常：{ex.Message}", "抖音授权");
                    }
                };
                stateTimer.Start();
                FileUtils.LogRpa("已启动登录/退出状态轮询检查（每2秒）", "抖音授权");

                // 关闭时释放资源
                newForm.FormClosing += (s, e) =>
                {
                    Task.Run(() =>
                    {
                        try
                        {
                            newChromiumBrowser?.Dispose();
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"释放抖音浏览器资源异常：{ex.Message}", "抖音授权");
                        }
                    });
                };

                // 显示窗体
                newForm.Controls.Add(newChromiumBrowser);
                newForm.Show();

                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"创建抖音窗体异常：{ex.Message}", "抖音授权");
                return false;
            }
        }

        /// <summary>
        /// 保存抖音 Cookie
        /// </summary>
        private async Task SaveDouyinCookies(ChromiumWebBrowser browser, string secUid)
        {
            try
            {
                var cookieManager = browser.GetCookieManager();
                var cookies = new List<Cookie>();

                // 收集 Cookie
                var visitor = new DouyinCookieCollector(cookies);
                cookieManager.VisitAllCookies(visitor);
                await Task.Delay(1000);

                // 检查是否有有效的 Cookie（sessionid）
                var sessionCookie = cookies.Find(c => c.Name.Contains("sessionid"));
                if (sessionCookie == null || string.IsNullOrEmpty(sessionCookie.Value))
                {
                    FileUtils.LogRpa("未获取到有效的 sessionid Cookie，跳过保存", "抖音授权");
                    return;
                }

                // 保存到文件
                string cookiePath = DouyinUtils.getCookiePath(secUid);
                string json = Newtonsoft.Json.JsonConvert.SerializeObject(cookies, Newtonsoft.Json.Formatting.Indented);
                File.WriteAllText(cookiePath, json);

                FileUtils.LogRpa($"抖音 Cookie 已保存：{cookiePath}，共 {cookies.Count} 个", "抖音授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存抖音 Cookie 异常：{ex.Message}", "抖音授权");
            }
        }

        #endregion

        #region 抖音 Cookie 收集器

        /// <summary>
        /// 抖音 Cookie 收集器
        /// </summary>
        public class DouyinCookieCollector : ICookieVisitor
        {
            private readonly List<Cookie> _cookies;

            public DouyinCookieCollector(List<Cookie> cookies)
            {
                _cookies = cookies;
            }

            public bool Visit(Cookie cookie, int count, int total, ref bool deleteCookie)
            {
                // 空值检查：Domain 可能为 null
                if (cookie != null && !string.IsNullOrEmpty(cookie.Domain) && cookie.Domain.Contains("douyin.com"))
                {
                    _cookies.Add(cookie);
                }
                deleteCookie = false;
                return true;
            }

            public void Dispose()
            {
            }
        }

        #endregion

        #region 快手授权处理

        /// <summary>
        /// 判断是否是快手 URL
        /// </summary>
        private bool IsKuaishouUrl(string url)
        {
            return url.Contains("kuaishou.com");
        }

        /// <summary>
        /// 提取快手主播的 secUid（优先从 frame.Url 管理后台提取）
        /// </summary>
        private string ExtractKuaishouSecUid(string targetUrl, IFrame frame)
        {
            try
            {
                // 方式1：从 frame.Url（管理后台）提取 secUid 参数
                // 管理后台 URL 格式：http://localhost/anchor?secUid=xxx
                if (frame != null && !string.IsNullOrEmpty(frame.Url))
                {
                    try
                    {
                        var uri = new Uri(frame.Url);
                        var query = System.Web.HttpUtility.ParseQueryString(uri.Query);
                        string secUid = query["secUid"];
                        if (!string.IsNullOrEmpty(secUid))
                        {
                            FileUtils.LogRpa($"从管理后台提取到快手 secUid：{secUid}", "快手授权");
                            return secUid;
                        }
                    }
                    catch { }
                }

                // 方式2：从 targetUrl（快手页面）提取 webId
                // 快手 URL 格式：https://live.kuaishou.com/u/xxx 或 https://live.kuaishou.com/profile/xxx
                string webId = KuaishouUtils.ExtractKuaishouIdFromUrl(targetUrl);
                if (!string.IsNullOrEmpty(webId))
                {
                    FileUtils.LogRpa($"从快手 URL 提取到 webId：{webId}，需要异步获取 secUid", "快手授权");
                    // 注意：这里不能异步获取 secUid，因为 OnBeforePopup 需要同步返回
                    // 暂时返回 webId 作为标识（Cookie 文件名）
                    return webId;
                }

                FileUtils.LogRpa($"无法从 URL 提取快手 secUid，targetUrl: {targetUrl}", "快手授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"提取快手 secUid 异常：{ex.Message}", "快手授权");
            }
            return null;
        }

        /// <summary>
        /// 获取快手 Cookie 数量（用于检测登录状态变化）
        /// </summary>
        private async Task<int> GetKuaishouCookieCount(ChromiumWebBrowser browser)
        {
            try
            {
                var cookieManager = browser.GetCookieManager();
                var cookies = new List<Cookie>();
                var visitor = new KuaishouCookieCollector(cookies);
                cookieManager.VisitAllCookies(visitor);

                await Task.Delay(500);

                int count = cookies.Count;
                FileUtils.LogRpa($"快手 Cookie 数量: {count}", "快手授权");
                return count;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取快手 Cookie 数量异常: {ex.Message}", "快手授权");
                return 0;
            }
        }

        /// <summary>
        /// 获取快手独立缓存路径（每个主播隔离）
        /// </summary>
        private string GetKuaishouCachePath(string secUid)
        {
            try
            {
                string baseCachePath = Path.GetFullPath("googleCache");
                string md5Str = KuaishouUtils.getCachePathMd5(secUid);
                string cachePath = Path.Combine(baseCachePath, $"kuaishou-{md5Str}");

                if (!Directory.Exists(cachePath))
                {
                    Directory.CreateDirectory(cachePath);
                }

                return cachePath;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取快手缓存路径异常：{ex.Message}", "快手授权");
                return Path.GetFullPath("googleCache");
            }
        }

        /// <summary>
        /// 创建快手窗体（Cookie有效直接加载，无效则弹出授权）
        /// </summary>
        private bool CreateKuaishouForm(string targetUrl, string secUid, out IWebBrowser newBrowser)
        {
            newBrowser = null;

            try
            {
                // 检查 Cookie 是否有效
                bool cookieValid = KuaishouUtils.CheckCookieValid(secUid);
                string startUrl = cookieValid ? targetUrl : "https://www.kuaishou.com/?isHome=1";

                FileUtils.LogRpa($"创建快手窗体，Cookie有效：{cookieValid}，起始URL：{startUrl}", "快手授权");

                // 为每个主播创建独立的缓存路径
                string cachePath = GetKuaishouCachePath(secUid);
                var requestContextSettings = new RequestContextSettings
                {
                    CachePath = cachePath,
                    PersistSessionCookies = false  // 不自动持久化 session cookie
                };
                var requestContext = new RequestContext(requestContextSettings);

                // 创建浏览器
                var newChromiumBrowser = new ChromiumWebBrowser(startUrl)
                {
                    Dock = DockStyle.Fill,
                    LifeSpanHandler = this,
                    RequestContext = requestContext
                };

                // 创建窗体
                Form newForm = new Form
                {
                    Text = cookieValid ? "快手页面" : "快手授权",
                    Size = new Size(1000, 700),
                    StartPosition = FormStartPosition.CenterScreen,
                    FormBorderStyle = FormBorderStyle.Sizable,
                    MaximizeBox = true,
                    MinimizeBox = true
                };

                // Cookie 有效时加载 Cookie
                if (cookieValid)
                {
                    newChromiumBrowser.IsBrowserInitializedChanged += async (sender, e) =>
                    {
                        if (newChromiumBrowser.IsBrowserInitialized)
                        {
                            await KuaishouUtils.LoadCookiesToBrowser(secUid, newChromiumBrowser);
                        }
                    };
                }

                // 启动统一的状态检测定时器（同时处理登录和退出）
                bool cookieSaved = cookieValid;
                int lastCookieCount = 0;
                bool initialized = false;
                int initDelay = 0;
                string cookiePath = KuaishouUtils.getCookiePath(secUid);

                var stateTimer = new System.Windows.Forms.Timer();
                stateTimer.Interval = 2000;
                stateTimer.Tick += async (sender, e) =>
                {
                    if (newChromiumBrowser.IsDisposed)
                    {
                        stateTimer.Stop();
                        stateTimer.Dispose();
                        return;
                    }

                    try
                    {
                        int currentCookieCount = await GetKuaishouCookieCount(newChromiumBrowser);

                        FileUtils.LogRpa($"快手状态检测 - initDelay: {initDelay}, cookieCount: {currentCookieCount}, lastCount: {lastCookieCount}, cookieSaved: {cookieSaved}", "快手授权");

                        if (!initialized)
                        {
                            initDelay++;
                            if (initDelay >= 3)
                            {
                                initialized = true;
                                lastCookieCount = currentCookieCount;
                                FileUtils.LogRpa($"快手状态检测初始化完成 - cookieCount: {currentCookieCount}", "快手授权");
                                
                                // 如果初始化时 Cookie 数量已较多且 Cookie 无效，说明有旧缓存
                                if (currentCookieCount > 5 && !cookieValid)
                                {
                                    cookieSaved = true;
                                    FileUtils.LogRpa("检测到旧 Cookie 缓存，跳过登录检测", "快手授权");
                                }
                            }
                            return;
                        }

                        // 检测登录：Cookie 数量显著增加（增加 3 个以上说明登录成功）
                        if (currentCookieCount > lastCookieCount + 3 && !cookieSaved)
                        {
                            cookieSaved = true;
                            FileUtils.LogRpa($"快手登录成功（Cookie数量从 {lastCookieCount} 增加到 {currentCookieCount}），准备保存 Cookie", "快手授权");
                            await SaveKuaishouCookies(newChromiumBrowser, secUid);
                            newForm.Text = "快手页面 - Cookie已保存";
                        }
                        // 检测退出：Cookie 数量显著减少
                        else if (currentCookieCount < lastCookieCount - 3 && File.Exists(cookiePath))
                        {
                            FileUtils.LogRpa($"检测到快手 Cookie 数量减少，可能退出登录", "快手授权");
                            try
                            {
                                File.Delete(cookiePath);
                                cookieSaved = false;
                                FileUtils.LogRpa($"已删除 Cookie 文件：{cookiePath}", "快手授权");
                                newForm.Text = "快手页面 - 已退出登录";
                            }
                            catch (Exception ex)
                            {
                                FileUtils.LogRpa($"删除快手 Cookie 文件异常：{ex.Message}", "快手授权");
                            }
                        }

                        lastCookieCount = currentCookieCount;
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"快手状态检测异常：{ex.Message}", "快手授权");
                    }
                };
                stateTimer.Start();
                FileUtils.LogRpa("已启动快手登录/退出状态轮询检查（每2秒）", "快手授权");

                // 关闭时释放资源
                newForm.FormClosing += (s, e) =>
                {
                    Task.Run(() =>
                    {
                        try
                        {
                            newChromiumBrowser?.Dispose();
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"释放快手浏览器资源异常：{ex.Message}", "快手授权");
                        }
                    });
                };

                // 显示窗体
                newForm.Controls.Add(newChromiumBrowser);
                newForm.Show();

                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"创建快手窗体异常：{ex.Message}", "快手授权");
                return false;
            }
        }

        /// <summary>
        /// 保存快手 Cookie
        /// </summary>
        private async Task SaveKuaishouCookies(ChromiumWebBrowser browser, string secUid)
        {
            try
            {
                var cookieManager = browser.GetCookieManager();
                var cookies = new List<Cookie>();

                // 收集 Cookie
                var visitor = new KuaishouCookieCollector(cookies);
                cookieManager.VisitAllCookies(visitor);
                await Task.Delay(1000);

                // 检查是否有足够的 Cookie（至少 5 个才算有效）
                if (cookies.Count < 5)
                {
                    FileUtils.LogRpa($"快手 Cookie 数量不足（{cookies.Count} 个），跳过保存", "快手授权");
                    return;
                }

                // 保存到文件
                string cookiePath = KuaishouUtils.getCookiePath(secUid);
                string json = Newtonsoft.Json.JsonConvert.SerializeObject(cookies, Newtonsoft.Json.Formatting.Indented);
                File.WriteAllText(cookiePath, json);

                FileUtils.LogRpa($"快手 Cookie 已保存：{cookiePath}，共 {cookies.Count} 个", "快手授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存快手 Cookie 异常：{ex.Message}", "快手授权");
            }
        }

        #endregion

        #region 快手 Cookie 收集器

        /// <summary>
        /// 快手 Cookie 收集器
        /// </summary>
        public class KuaishouCookieCollector : ICookieVisitor
        {
            private readonly List<Cookie> _cookies;

            public KuaishouCookieCollector(List<Cookie> cookies)
            {
                _cookies = cookies;
            }

            public bool Visit(Cookie cookie, int count, int total, ref bool deleteCookie)
            {
                if (cookie != null && !string.IsNullOrEmpty(cookie.Domain) && cookie.Domain.Contains("kuaishou.com"))
                {
                    _cookies.Add(cookie);
                }
                deleteCookie = false;
                return true;
            }

            public void Dispose()
            {
            }
        }

        #endregion
    }
}
