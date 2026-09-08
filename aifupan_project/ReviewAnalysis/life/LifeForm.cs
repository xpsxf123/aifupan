using CefSharp;
using CefSharp.WinForms;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.Model;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using ServerTimeUtils = ReviewAnalysis.Utils.ServerTimeUtils;
using FrontNotice = ReviewAnalysis.Utils.FrontNotice;

namespace ReviewAnalysis.life
{
    public partial class LifeForm : Form
    {
        private ChromiumWebBrowser browser;
        public AnchorInfo anchorInfo { get; set; }
        public bool isTemp { get; set; }
        public long addTempTime { get; set; }

        private LifeAuthStatusEnum _loginStatus = LifeAuthStatusEnum.unAuth;
        private bool loginSuccess = false;
        private DateTime _loginStartTime;
        private bool _isFormClosed = false;
        private System.Windows.Forms.Timer _cookieCheckTimer;
        private int _loginSuccessProcessed = 0; // 0=未处理, 1=处理中, 2=已处理完毕
        private Dictionary<string, string> _earlyExtractedPageInfo = null; // 在/p/home瞬间提取的页面信息（防止页面跳回login后丢失）
        private bool _hasPassedLogin = false; // 已经过登录阶段（看到过/p/home），后续回到/p/login是选择公司不是登录失败

        private string LoginUrl = "https://life.douyin.com/p/login";
        private string LoginSuccessUrl = "https://life.douyin.com/p/home";

        public LifeForm()
        {
            InitializeComponent();
        }

        public LifeForm(AnchorInfo anchorInfo)
        {
            this.anchorInfo = anchorInfo;
            InitializeComponent();
            Init();
        }

        /// <summary>
        /// 授权前Cookie有效性双重验证标志：
        /// true = PreCheckCookieAsync 已完成跳过登录流程，后续 FrameLoadEnd 不应重复触发
        /// </summary>
        private bool _preCheckedSkipLogin = false;

        public void Init()
        {
            Init(false);
        }

        public void Init(bool bHide)
        {
            try
            {
                FileUtils.LogRpa($"开始初始化来客窗体, 主播={anchorInfo?.AnchorName}, SecUid={anchorInfo?.SecUid}, bHide={bHide}", "来客登录");

                this.Text = "来客登录";
                this.Size = new Size(1250, 700);
                this.StartPosition = FormStartPosition.CenterScreen;
                this.FormBorderStyle = FormBorderStyle.Sizable;
                this.MaximizeBox = true;
                this.MinimizeBox = true;

                if (bHide)
                {
                    this.Visible = false;
                }

                // ========================================================================
                // 授权前Cookie有效性双重验证（本地检查 + API验证）
                // ------------------------------------------------------------------------
                // 场景：同一来客商家下多个抖音号共享Cookie，第一次授权后生成的Cookie文件
                //       可能对其他抖音号仍然有效。此检查避免重复弹出登录窗体。
                // 流程：
                //   1. 本地检查：Cookie文件存在 + 含 sessionid_ls 字段
                //   2. API 验证：调 GetAccountDetail 验证 Cookie 在服务端是否真正有效
                //   3. 两层都通过 → 标记为已授权，跳过登录流程
                //   4. 任一失败 → 继续打开浏览器登录窗体
                // ========================================================================
                // 使用 Task.Run 确保异步操作在线程池执行，避免UI线程SynchronizationContext死锁
                bool cookieValid = false;
                try
                {
                    var preCheckTask = Task.Run(() => PreCheckCookieAsync());
                    if (preCheckTask.Wait(TimeSpan.FromSeconds(15)))
                    {
                        cookieValid = preCheckTask.Result;
                    }
                    else
                    {
                        FileUtils.LogRpa("[Cookie预检] API验证超时(15秒)，跳过预检直接打开登录窗体", "来客登录");
                    }
                }
                catch (AggregateException aex)
                {
                    FileUtils.LogRpa($"[Cookie预检] 预检异常: {aex.InnerException?.Message ?? aex.Message}", "来客登录");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"[Cookie预检] 预检异常: {ex.Message}", "来客登录");
                }

                if (cookieValid)
                {
                    FileUtils.LogRpa($"[Cookie预检] Cookie有效，跳过登录流程，主播【{anchorInfo?.AnchorName}】", "来客登录");
                    _preCheckedSkipLogin = true;
                    loginSuccess = true;
                    _loginStatus = LifeAuthStatusEnum.auth;
                    LifeUtils.authing = false;

                    // 同步授权状态到服务端
                    try
                    {
                        if (anchorInfo != null)
                        {
                            AnchorBll.UpdateLifeAuthStatus(anchorInfo, (int)LifeAuthStatusEnum.auth);
                            FileUtils.LogRpa($"[Cookie预检] 来客授权状态已同步到服务端，secUid={anchorInfo.SecUid}", "来客登录");

                            // 同步更新 SecUid.json 中当前主播的 secUid/cookiePath（PreCheck 路径不走 LifeAccountResolver，需在这里补充）
                            try
                            {
                                string md5ForPreCheck = LifeUtils.getCachePathMd5(anchorInfo.SecUid);
                                string cookieFileNameForPreCheck = $"life-{md5ForPreCheck}";
                                FileUtils.LogRpa($"[Cookie预检] 更新SecUid.json: awemeUserId=[{anchorInfo.AnchorUserId ?? "null"}], secUid=[{anchorInfo.SecUid}], cookiePath=[{cookieFileNameForPreCheck}]", "来客登录");
                                LifeUtils.UpdateAwemeUserInSecUidJson(anchorInfo.AnchorUserId, anchorInfo.SecUid, cookieFileNameForPreCheck);
                            }
                            catch (Exception secEx)
                            {
                                FileUtils.LogRpa($"[Cookie预检] 更新SecUid.json异常(非致命): {secEx.Message}", "来客登录");
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"[Cookie预检] 同步来客授权状态到服务端异常: {ex.Message}", "来客登录");
                    }

                    // 通知前端授权成功
                    NotifyLoginSuccess();

                    // 延迟关闭窗体
                    Task.Delay(1500).ContinueWith(_ =>
                    {
                        try
                        {
                            this.BeginInvoke(new Action(() =>
                            {
                                FileUtils.LogRpa($"[Cookie预检] Cookie有效流程完成，关闭来客登录窗体，主播【{anchorInfo?.AnchorName}】", "来客登录");
                                this.Close();
                            }));
                        }
                        catch { }
                    });

                    return; // 跳过浏览器初始化和登录窗体
                }

                FileUtils.LogRpa($"[Cookie预检] Cookie无效或不存在，继续打开登录窗体，主播【{anchorInfo?.AnchorName}】", "来客登录");

                InitBrowser();
                FileUtils.LogRpa("浏览器初始化完成", "来客登录");
                OpenLogin();
                FileUtils.LogRpa("来客登录窗体初始化完成，已打开登录页", "来客登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化来客窗体异常: {ex.Message}", "来客登录");
                NotifyLoginFailed($"初始化失败: {ex.Message}");
                this.Close();
            }
        }

        /// <summary>
        /// 授权前Cookie有效性双重验证。
        /// 第一层：本地检查（Cookie文件存在 + 含 sessionid_ls）
        /// 第二层：API 验证（调 GetAccountDetail 确认 Cookie 在服务端有效）
        /// </summary>
        /// <returns>true=Cookie有效可跳过登录；false=需弹出登录窗体</returns>
        private async Task<bool> PreCheckCookieAsync()
        {
            if (anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
            {
                FileUtils.LogRpa("[Cookie预检] anchorInfo或SecUid为空，跳过预检", "来客登录");
                return false;
            }

            // 第一层：本地检查
            int authStatus = LifeUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
            if (authStatus != (int)LifeAuthStatusEnum.auth)
            {
                FileUtils.LogRpa($"[Cookie预检] 本地检查未通过(Cookie文件不存在或无sessionid_ls)，status={authStatus}，secUid={anchorInfo.SecUid}", "来客登录");

                // S2/S3 兄弟 cookie 复用：本地无 cookie 时，尝试从x SecUid.json 里同商户已授权主播复用
                string brotherCookiePath = LifeUtils.FindBrotherCookiePathByAweme(anchorInfo.SecUid);
                if (string.IsNullOrEmpty(brotherCookiePath))
                {
                    FileUtils.LogRpa("[Cookie预检] 未找到同商户兄弟 cookie，回退到弹窗授权", "来客登录");
                    return false;
                }

                // 复制兄弟 cookie 到 targetSecUid 对应的 life-{md5} 路径
                string targetCookiePath = LifeUtils.getCookiePath(anchorInfo.SecUid);
                try
                {
                    string targetDir = System.IO.Path.GetDirectoryName(targetCookiePath);
                    if (!string.IsNullOrEmpty(targetDir) && !System.IO.Directory.Exists(targetDir))
                        System.IO.Directory.CreateDirectory(targetDir);
                    string brotherContent = System.IO.File.ReadAllText(brotherCookiePath);

                    // 改写 AwemeUserId/SecUid 为当前主播（cookie 内容不变）
                    var (brotherFileDto, _) = LifeCookieFileDto.Parse(brotherContent);
                    var rewriteDto = new LifeCookieFileDto
                    {
                        AwemeUserId = anchorInfo.AnchorUserId ?? "",
                        SecUid = anchorInfo.SecUid,
                        Cookies = brotherFileDto?.Cookies ?? new List<LifeCookieDto>()
                    };
                    System.IO.File.WriteAllText(targetCookiePath, rewriteDto.ToJson(), System.Text.Encoding.UTF8);
                    FileUtils.LogRpa($"[Cookie预检] 已从兄弟 cookie 复制生成: {targetCookiePath}，源={brotherCookiePath}", "来客登录");
                }
                catch (Exception copyEx)
                {
                    FileUtils.LogRpa($"[Cookie预检] 复制兄弟 cookie 异常: {copyEx.Message}", "来客登录");
                    return false;
                }

                // 重新校验本地检查
                int reCheck = LifeUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
                if (reCheck != (int)LifeAuthStatusEnum.auth)
                {
                    FileUtils.LogRpa($"[Cookie预检] 兄弟 cookie 复制后校验仍未通过，status={reCheck}，回退到弹窗授权", "来客登录");
                    try { System.IO.File.Delete(targetCookiePath); } catch { }
                    return false;
                }
                FileUtils.LogRpa("[Cookie预检] 兄弟 cookie 复制后本地检查通过，继续 API 验证", "来客登录");
            }
            else
            {
                FileUtils.LogRpa($"[Cookie预检] 本地检查通过(Cookie文件存在且含sessionid_ls)，进行API验证...", "来客登录");
            }

            // 第二层：API 验证
            try
            {
                var cookies = LifeDataHandle.GetCookiesFromLocal(anchorInfo.SecUid);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("[Cookie预检] 从本地读取Cookie字典为空，API验证无法进行", "来客登录");
                    return false;
                }

                var detail = await LifeDataApi.GetAccountDetail(cookies).ConfigureAwait(false);
                if (detail != null && detail.ContainsKey("group_id") && !string.IsNullOrEmpty(detail["group_id"]))
                {
                    string lifeAccountIdStr = detail.ContainsKey("life_account_id") ? detail["life_account_id"] : "无";
                    string awemeUserIdStr = detail.ContainsKey("aweme_user_id") ? detail["aweme_user_id"] : "无";
                    FileUtils.LogRpa($"[Cookie预检] API验证通过: group_id={detail["group_id"]}, life_account_id={lifeAccountIdStr}, aweme_user_id={awemeUserIdStr}", "来客登录");
                    return true;
                }
                else
                {
                    FileUtils.LogRpa($"[Cookie预检] API验证失败: Cookie在服务端已失效（返回null或无group_id），需重新登录", "来客登录");
                    return false;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"[Cookie预检] API验证异常: {ex.Message}，保守起见继续打开登录窗体", "来客登录");
                return false;
            }
        }

        private void InitBrowser()
        {
            try
            {
                string url = LoginUrl;
                FileUtils.LogRpa($"初始化浏览器，URL={url}, 当前线程ID={System.Threading.Thread.CurrentThread.ManagedThreadId}, IsBackground={System.Threading.Thread.CurrentThread.IsBackground}", "来客登录");

                BrowserSettings browserSettings = new BrowserSettings();
                browserSettings.Javascript = CefState.Enabled;
                browserSettings.WebGl = CefState.Enabled;

                RequestContextSettings requestContextSettings = new RequestContextSettings();
                requestContextSettings.PersistSessionCookies = true;
                requestContextSettings.CachePath = GetCachePath();
                FileUtils.LogRpa($"RequestContext设置: PersistSessionCookies=true, CachePath={requestContextSettings.CachePath}", "来客登录");

                browser = new ChromiumWebBrowser(url)
                {
                    BrowserSettings = browserSettings,
                    RequestContext = new RequestContext(requestContextSettings)
                };
                FileUtils.LogRpa($"ChromiumWebBrowser实例已创建", "来客登录");

                browser.LifeSpanHandler = new LifeSpanHandler(this);

                Panel browserPanel = new Panel
                {
                    Dock = DockStyle.Fill,
                    AutoScroll = true
                };

                browser.Dock = DockStyle.Fill;
                browserPanel.Controls.Add(browser);
                this.Controls.Add(browserPanel);
                FileUtils.LogRpa($"浏览器控件已添加到窗体, FormSize={this.Size}, PanelSize={browserPanel.Size}, BrowserSize={browser.Size}", "来客登录");

                browser.IsBrowserInitializedChanged += (sender, e) =>
                {
                    FileUtils.LogRpa($"浏览器初始化状态变更: IsBrowserInitialized={browser.IsBrowserInitialized}", "来客登录");
                };

                browser.LoadingStateChanged += (sender, e) =>
                {
                    FileUtils.LogRpa($"LoadingStateChanged: IsLoading={e.IsLoading}, CanGoBack={e.CanGoBack}, CanGoForward={e.CanGoForward}", "来客登录");
                    if (e.IsLoading == false)
                    {
                        browser.ExecuteScriptAsync(@"document.body.style.overflow = 'auto';");
                        browser.ExecuteScriptAsync(@"document.documentElement.style.overflow = 'auto';");

                        if (this.InvokeRequired)
                        {
                            this.BeginInvoke(new Action(() =>
                            {
                                this.Size = new Size(1250, 700);
                                FileUtils.LogRpa($"页面加载完成，窗体已调整大小: FormSize={this.Size}", "来客登录");
                            }));
                        }
                    }
                };

                browser.FrameLoadEnd += Browser_FrameLoadEnd;
                browser.LoadError += ChromeBrowser_LoadError;

                _cookieCheckTimer = new System.Windows.Forms.Timer
                {
                    Interval = 3000,
                    Enabled = true
                };
                _cookieCheckTimer.Tick += CookieCheckTimer_Tick;

                LoadLifeCookies();
                FileUtils.LogRpa("LoadLifeCookies 执行完毕", "来客登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化浏览器异常: {ex.Message}\n{ex.StackTrace}", "来客登录");
                throw;
            }
        }

        private async void Browser_FrameLoadEnd(object sender, FrameLoadEndEventArgs e)
        {
            try
            {
                if (e == null || string.IsNullOrEmpty(e.Url))
                    return;

                // 仅处理主frame的加载完成事件
                if (e.Frame != null && !e.Frame.IsMain)
                    return;

                // 强制设置浏览器缩放为100%视觉大小
                // 全局Cef.Initialize设了force-device-scale-factor=dpiRatio（如1.5），
                // 导致页面以系统DPI倍率渲染，需按反向比例抵消才能达到100%视觉效果
                float dpiRatio = ReviewAnalysis.Utils.ScreenUtils.GetDpiRatio();
                if (Math.Abs(dpiRatio - 1.0f) > 0.01f)
                {
                    // zoomLevel = ln(1/dpiRatio) / ln(1.2)，CEF zoom每级≈20%
                    double zoomLevel = Math.Log(1.0 / dpiRatio) / Math.Log(1.2);
                    browser.SetZoomLevel(zoomLevel);
                    FileUtils.LogRpa($"高DPI适配: dpiRatio={dpiRatio}, zoomLevel={zoomLevel:F2}", "来客登录");
                }
                else
                {
                    browser.SetZoomLevel(0);
                }

                FileUtils.LogRpa($"主页面加载完成: URL={e.Url}", "来客登录");

                // 检测未入住页面（商家未开通来客）：/p/settle-v2 是折引导页，此时无法获取有效的采集权限
                if (e.Url.Contains("/p/settle-v2") || e.Url.Contains("/p/settle"))
                {
                    if (System.Threading.Interlocked.CompareExchange(ref _loginSuccessProcessed, 1, 0) != 0)
                    {
                        return;
                    }
                    FileUtils.LogRpa($"检测到入住引导页(settle-v2)，商家未开通来客，标记为未授权并关闭窗口: {e.Url}", "来客登录");
                    _loginStatus = LifeAuthStatusEnum.authFailed;
                    LifeUtils.authing = false;
                    if (anchorInfo != null)
                    {
                        AnchorBll.UpdateLifeAuthStatus(anchorInfo, (int)LifeAuthStatusEnum.authFailed);
                    }
                    NotifyLoginFailed("该抖音号未入驻【抖音来客】商家平台，请先到 life.douyin.com 完成商家入驻后再重新授权");
                    if (this.InvokeRequired)
                        this.BeginInvoke(new Action(() => this.Close()));
                    else
                        this.Close();
                    return;
                }

                if (e.Url.Contains(LoginSuccessUrl) && e.Url.Contains("home"))
                {
                    // 检查URL是否包含groupid参数（选择公司后才会带groupid）
                    var groupIdMatch = System.Text.RegularExpressions.Regex.Match(e.Url, @"groupid=([^&]+)");
                    if (!groupIdMatch.Success)
                    {
                        // 登录中间态：已到/p/home但未选择公司，保持窗口打开等待用户操作
                        _hasPassedLogin = true; // 标记已过登录阶段
                        FileUtils.LogRpa($"检测到/p/home但URL中无groupid，等待用户选择公司: {e.Url}", "来客登录");
                        // 调大窗体让用户可以操作选择公司
                        if (this.InvokeRequired)
                        {
                            this.BeginInvoke(new Action(() =>
                            {
                                this.Width = 1000;
                                this.Height = 700;
                                this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
                                this.CenterToScreen();
                                FileUtils.LogRpa($"窗体已调整为选择公司模式: FormSize={{Width={this.Width}, Height={this.Height}}}", "来客登录");
                            }));
                        }

                        // 注入JS：读取页面结构并调整显示以便用户选择公司
                        try
                        {
                            await Task.Delay(1000); // 等待页面渲染
                            var frame = browser?.GetMainFrame();
                            if (frame != null)
                            {
                                // 诊断：输出页面DOM结构
                                var dumpResult = await frame.EvaluateScriptAsync(@"
                                    (function() {
                                        var els = document.querySelectorAll('body > *, #main-app > *, #app > *');
                                        var info = [];
                                        for (var i = 0; i < Math.min(els.length, 30); i++) {
                                            var el = els[i];
                                            var rect = el.getBoundingClientRect();
                                            info.push(el.tagName + '#' + el.id + '.' + el.className.toString().substring(0,60) + ' [' + Math.round(rect.width) + 'x' + Math.round(rect.height) + ' @' + Math.round(rect.top) + ']');
                                        }
                                        return info.join(' | ');
                                    })()
                                ");
                                if (dumpResult.Success && dumpResult.Result != null)
                                {
                                    FileUtils.LogRpa($"选择公司页面DOM结构: {dumpResult.Result}", "来客登录");
                                }

                                // 注入CSS+JS：定位到选择公司卡片，隐藏其他所有内容（仓报位、促销图等）
                                // 参考登录页 ApplyLoginPageCSS 的思路：只保留表单部分，其余全隐藏
                                // 因为不知道卡片确切的选择器，采用「从卡片向上遍历祖先，逐层隐藏兄弟」的通用方案
                                await frame.EvaluateScriptAsync(@"
                                    (function() {
                                        if (window.__lifeAuthIsolateTimer) return;
                                        var attempts = 0;
                                        var maxAttempts = 40; // 40 * 300ms = 12 秒
                                        window.__lifeAuthIsolateTimer = setInterval(function() {
                                            attempts++;
                                            try {
                                                // 1) 查找包含「选择你的公司」文本、且自身直接文本命中的最内层元素
                                                var all = document.querySelectorAll('body *');
                                                var target = null;
                                                for (var i = 0; i < all.length; i++) {
                                                    var el = all[i];
                                                    var txt = (el.textContent || '').trim();
                                                    if (txt.indexOf('选择你的公司') !== -1 && el.children.length <= 3 && txt.length < 30) {
                                                        target = el;
                                                        break;
                                                    }
                                                }
                                                if (!target) {
                                                    if (attempts >= maxAttempts) {
                                                        clearInterval(window.__lifeAuthIsolateTimer);
                                                        window.__lifeAuthIsolateTimer = null;
                                                        console.log('[LifeAuth] 未找到选择你的公司卡片');
                                                    }
                                                    return;
                                                }

                                                // 2) 向上找卡片容器：宽度 260-700，高度 >= 200 的最内层块级祖先
                                                var card = target;
                                                var probe = target;
                                                for (var lvl = 0; lvl < 10 && probe && probe !== document.body; lvl++) {
                                                    var r = probe.getBoundingClientRect();
                                                    if (r.width >= 260 && r.width <= 700 && r.height >= 200) { card = probe; }
                                                    probe = probe.parentElement;
                                                }

                                                // 3) 从卡片向上遍历到 body，每一层把兄弟隐藏，保留自己这一支
                                                var node = card;
                                                while (node && node !== document.body) {
                                                    var parent = node.parentElement;
                                                    if (parent) {
                                                        var children = parent.children;
                                                        for (var j = 0; j < children.length; j++) {
                                                            if (children[j] !== node) {
                                                                children[j].style.setProperty('display', 'none', 'important');
                                                            }
                                                        }
                                                        // 仅重置自己的宽度、定位与边距，避免覆盖卡片内部实际布局样式
                                                        node.style.setProperty('width', '100%', 'important');
                                                        node.style.setProperty('max-width', 'none', 'important');
                                                        node.style.setProperty('min-width', '0', 'important');
                                                        node.style.setProperty('margin', '0', 'important');
                                                        node.style.setProperty('padding', '0', 'important');
                                                        node.style.setProperty('position', 'relative', 'important');
                                                        node.style.setProperty('left', '0', 'important');
                                                        node.style.setProperty('top', '0', 'important');
                                                        node.style.setProperty('transform', 'none', 'important');
                                                        node.style.setProperty('background', 'transparent', 'important');
                                                    }
                                                    node = parent;
                                                }

                                                // 4) 卡片自身居中显示且适当留白
                                                card.style.setProperty('margin', '20px auto', 'important');
                                                card.style.setProperty('max-width', '480px', 'important');
                                                card.style.setProperty('width', 'auto', 'important');

                                                // 5) 全局样式：白底、允许垂直滚动、去掉横向滚动条
                                                document.documentElement.style.setProperty('background', '#fff', 'important');
                                                document.documentElement.style.setProperty('margin', '0', 'important');
                                                document.documentElement.style.setProperty('padding', '0', 'important');
                                                document.documentElement.style.setProperty('overflow-x', 'hidden', 'important');
                                                document.documentElement.style.setProperty('overflow-y', 'auto', 'important');
                                                document.body.style.setProperty('background', '#fff', 'important');
                                                document.body.style.setProperty('margin', '0', 'important');
                                                document.body.style.setProperty('padding', '0', 'important');
                                                document.body.style.setProperty('min-width', '0', 'important');
                                                document.body.style.setProperty('overflow-x', 'hidden', 'important');
                                                document.body.style.setProperty('overflow-y', 'auto', 'important');

                                                clearInterval(window.__lifeAuthIsolateTimer);
                                                window.__lifeAuthIsolateTimer = null;
                                                console.log('[LifeAuth] 已隔离选择公司卡片, 尝试次数=' + attempts);
                                            } catch (e) {
                                                console.log('[LifeAuth] 隔离异常: ' + e.message);
                                                if (attempts >= maxAttempts) {
                                                    clearInterval(window.__lifeAuthIsolateTimer);
                                                    window.__lifeAuthIsolateTimer = null;
                                                }
                                            }
                                        }, 300);
                                    })()
                                ");
                                FileUtils.LogRpa("已注入选择公司页面隔离脚本（只显示卡片）", "来客登录");
                            }
                        }
                        catch (Exception cssEx)
                        {
                            FileUtils.LogRpa($"注入选择公司页面CSS异常: {cssEx.Message}", "来客登录");
                        }

                        return; // 不触发cookie保存，继续等待带groupid的FrameLoadEnd
                    }

                    // 防重复处理：使用 Interlocked.CompareExchange 确保只有第一个进入的线程处理
                    if (System.Threading.Interlocked.CompareExchange(ref _loginSuccessProcessed, 1, 0) != 0)
                    {
                        FileUtils.LogRpa($"登录成功流程已在处理中或已处理完毕，跳过重复触发: URL={e.Url}", "来客登录");
                        return;
                    }

                    // 从URL中直接提取groupId
                    string urlGroupId = groupIdMatch.Groups[1].Value;
                    _earlyExtractedPageInfo = new Dictionary<string, string> { ["group_id"] = urlGroupId };
                    FileUtils.LogRpa($"从URL提取到groupId={urlGroupId}，授权完成", "来客登录");

                    loginSuccess = true;
                    _loginStatus = LifeAuthStatusEnum.auth;
                    LifeUtils.authing = false;

                    // 延迟2秒，确保浏览器内部Cookie完全写入
                    FileUtils.LogRpa("等待2秒确保Cookie完全写入...", "来客登录");
                    await Task.Delay(2000);

                    await SaveCurrentCookiesToLocal();
                    FileUtils.LogRpa("Cookie保存流程完成", "来客登录");

                    // 标记处理完毕
                    _loginSuccessProcessed = 2;

                    NotifyLoginSuccess();
                    try
                    {
                        if (anchorInfo != null)
                        {
                            AnchorBll.UpdateLifeAuthStatus(anchorInfo, (int)LifeAuthStatusEnum.auth);
                            FileUtils.LogRpa("来客授权状态已同步到服务端", "来客登录");

                            await CheckAndStartDataCollectionIfLive();
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"同步来客授权状态到服务端发生异常: {ex.Message}", "来客登录");
                    }

                    if (this.InvokeRequired)
                    {
                        this.BeginInvoke(new Action(() =>
                        {
                            // 登录成功且Cookie已保存，延迟2秒后关闭窗体
                            Task.Delay(2000).ContinueWith(_ =>
                            {
                                this.BeginInvoke(new Action(() =>
                                {
                                    FileUtils.LogRpa("登录成功流程全部完成，关闭来客登录窗体", "来客登录");
                                    this.Close();
                                }));
                            });
                        }));
                    }
                    FileUtils.LogRpa("登录成功，Cookie已保存，窗体将在2秒后关闭", "来客登录");
                }
                else if (e.Url.Contains("/p/login") && !e.Url.Contains("/p/home"))
                {
                    // 如果已经过了登录阶段（看到过/p/home），回到/p/login是选择公司，不做任何处理
                    if (_hasPassedLogin)
                    {
                        FileUtils.LogRpa($"已过登录阶段，当前在选择公司页面，保持窗口打开: {e.Url}", "来客登录");
                        return;
                    }
                    FileUtils.LogRpa($"当前在登录页面: {e.Url}", "来客登录");
                    await Task.Delay(500);
                    ApplyLoginPageCSS();
                    bool isLoginFailed = await CheckLoginFailureAsync();
                    if (isLoginFailed)
                    {
                        FileUtils.LogRpa("检测到登录失败元素，触发失败处理", "来客登录");
                        LoginFailedHandle("登录失败，请检查账号密码");
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"处理页面加载事件异常: {ex.Message}", "来客登录");
            }
        }

        private void ApplyLoginPageCSS()
        {
            try
            {
                if (browser == null || !browser.IsBrowserInitialized || !browser.CanExecuteJavascriptInMainFrame)
                    return;

                // 不再截取/裁剪登录表单，直接加载完整页面，避免不同分辨率下显示不全
                string js = @"
                    // 自动点击“立即登录”链接，从注册（入驻）页面切换到登录页面
                    // 只精确匹配文本为“立即登录”的元素，避免误点登录页里的“登录”按钮/Tab/标题
                    (function() {
                        var attempts = 0;
                        var maxAttempts = 30; // 30 * 500ms = 15秒
                        var clicked = false;
                        var timer = setInterval(function() {
                            attempts++;
                            try {
                                var candidates = document.querySelectorAll('a, span, div, button, p, i');
                                for (var i = 0; i < candidates.length; i++) {
                                    var el = candidates[i];
                                    var text = (el.textContent || '').replace(/\s+/g, '').trim();
                                    // 只匹配元素自身直接文本为“立即登录”的最内层节点，避免匹配到包含“已有账号?立即登录”的父节点
                                    if (text === '立即登录' && el.offsetParent !== null && el.children.length <= 1) {
                                        el.click();
                                        clicked = true;
                                        console.log('[LifeAuth] 已点击立即登录, 尝试次数=' + attempts);
                                        clearInterval(timer);
                                        return;
                                    }
                                }
                            } catch (e) { console.log('[LifeAuth] 查找立即登录异常: ' + e.message); }
                            if (attempts >= maxAttempts) {
                                clearInterval(timer);
                                if (!clicked) console.log('[LifeAuth] 未找到立即登录按钮，可能已在登录页');
                            }
                        }, 500);
                    })();
                ";
                browser.ExecuteScriptAsync(js);
                FileUtils.LogRpa("已注入登录页自动点击脚本（完整页面模式，不裁剪表单）", "来客登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"注入登录页自动点击脚本异常: {ex.Message}", "来客登录");
            }
        }

        private async Task<bool> CheckLoginFailureAsync()
        {
            try
            {
                if (browser == null || !browser.IsBrowserInitialized || !browser.CanExecuteJavascriptInMainFrame)
                {
                    FileUtils.LogRpa("CheckLoginFailureAsync 浏览器未就绪，跳过失败检查", "来客登录");
                    return false;
                }

                var mainFrame = browser.GetMainFrame();
                if (mainFrame == null)
                {
                    FileUtils.LogRpa("CheckLoginFailureAsync 主frame为空", "来客登录");
                    return false;
                }

                var result = await mainFrame.EvaluateScriptAsync(@"
                    const errorElements = document.querySelectorAll('.error-message, .login-error, .alert-error, .tip-error');
                    for (let element of errorElements) {
                        if (element.textContent && element.textContent.trim() !== '') {
                            return true;
                        }
                    }
                    return false;
                ");

                return result.Success && result.Result is bool && (bool)result.Result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检查登录失败状态异常: {ex.Message}", "来客登录");
                return false;
            }
        }

        private async Task CheckAndStartDataCollectionIfLive()
        {
            try
            {
                var cookies = LifeDataHandle.GetCookiesFromLocal(anchorInfo.SecUid);
                if (cookies.Count == 0)
                {
                    FileUtils.LogRpa("无来客Cookie，无法检查直播状态", "来客登录");
                    return;
                }

                var accountDetail = await LifeDataApi.GetAccountDetail(cookies);
                if (accountDetail == null)
                {
                    FileUtils.LogRpa("获取账户详情失败", "来客登录");
                    return;
                }

                string roomId = null;
                if (accountDetail.ContainsKey("current_live_room_id"))
                {
                    roomId = accountDetail["current_live_room_id"];
                }

                if (!string.IsNullOrEmpty(roomId) && roomId != "0")
                {
                    LifeDataCollectionManager.StartPolling(anchorInfo, roomId);
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}登录时正在直播，roomId={roomId}，已开启来客数据轮询", "来客登录");
                }
                else
                {
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}当前未直播，roomId={roomId ?? "null"}", "来客登录");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检查直播状态异常: {ex.Message}\n{ex.StackTrace}", "来客登录");
            }
        }

        private async void CookieCheckTimer_Tick(object sender, EventArgs e)
        {
            try
            {
                if (_isFormClosed)
                {
                    FileUtils.LogRpa("定时器触发，但窗体已关闭，停止执行", "来客登录");
                    return;
                }

                if (DateTime.Now - _loginStartTime > TimeSpan.FromMinutes(5))
                {
                    FileUtils.LogRpa("授权超时(5分钟)，静默关闭窗口并标记为未授权", "来客登录");
                    _cookieCheckTimer.Stop();
                    _loginStatus = LifeAuthStatusEnum.authFailed;
                    LifeUtils.authing = false;
                    if (anchorInfo != null)
                    {
                        AnchorBll.UpdateLifeAuthStatus(anchorInfo, (int)LifeAuthStatusEnum.authFailed);
                        FileUtils.LogRpa("授权超时，未授权状态已同步到服务端", "来客登录");
                    }
                    NotifyLoginFailed("授权超时");
                    // 静默关闭窗口，不弹任何提示
                    if (this.InvokeRequired)
                    {
                        this.BeginInvoke(new Action(() => this.Close()));
                    }
                    else
                    {
                        this.Close();
                    }
                    return;
                }

                if (loginSuccess)
                {
                    FileUtils.LogRpa("登录成功，停止定时器", "来客登录");
                    _cookieCheckTimer.Stop();
                    return;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检查Cookie异常: {ex.Message}", "来客登录");
            }
        }

        private void ChromeBrowser_LoadError(object sender, LoadErrorEventArgs e)
        {
            FileUtils.LogRpa($"浏览器加载错误: ErrorCode={e.ErrorCode}, ErrorText={e.ErrorText}，URL: {e.FailedUrl}, Frame={e.Frame?.Identifier}", "来客登录");
        }

        private void OpenLogin()
        {
            _loginStartTime = DateTime.Now;
            _loginStatus = LifeAuthStatusEnum.authing;
            LifeUtils.authing = true;
            FileUtils.LogRpa($"开始登录流程，URL={LoginUrl}，开始时间={_loginStartTime:HH:mm:ss}，超时=5分钟, IsBrowserInitialized={browser?.IsBrowserInitialized}", "来客登录");
            OpenURL(LoginUrl);
        }

        private void OpenURL(string url_str)
        {
            try
            {
                browser.Load(url_str);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"打开URL异常: {ex.Message}", "来客登录");
            }
        }

        private void LoginFailedHandle(string errorMessage)
        {
            try
            {
                FileUtils.LogRpa($"触发登录失败处理: {errorMessage}", "来客登录");

                _loginStatus = LifeAuthStatusEnum.authFailed;
                LifeUtils.authing = false;
                if (anchorInfo != null)
                {
                    AnchorBll.UpdateLifeAuthStatus(anchorInfo, (int)LifeAuthStatusEnum.authFailed);
                    FileUtils.LogRpa("来客授权失败状态已同步到服务端", "来客登录");
                }
                NotifyLoginFailed(errorMessage);

                // 弹出提示框让用户看到错误信息
                if (this.InvokeRequired)
                {
                    this.Invoke(new Action(() =>
                    {
                        MessageBox.Show(this, errorMessage, "来客登录失败", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                        FileUtils.LogRpa($"已弹出登录失败提示: {errorMessage}", "来客登录");
                        this.Close();
                    }));
                }
                else
                {
                    MessageBox.Show(this, errorMessage, "来客登录失败", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                    FileUtils.LogRpa($"已弹出登录失败提示: {errorMessage}", "来客登录");
                    this.Close();
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"登录失败处理异常: {ex.Message}", "来客登录");
            }
        }

        private void loginExpire(AnchorInfo anchorInfo, int lifeStatus)
        {
            if (anchorInfo.lifeAuthStatus != (int)LifeAuthStatusEnum.auth)
            {
                return;
            }
            AnchorBll.UpdateLifeAuthStatus(anchorInfo, lifeStatus);
            LifeUtils.recountLifeProperty();

            try
            {
                var frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "lifeAuthExpires";
                requestDataObj.Add("data", JsonConvert.SerializeObject(anchorInfo));
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知前端异常: {ex.Message}", "来客登录");
            }
        }

        private void loginExpire(int lifeStatus)
        {
            loginExpire(this.anchorInfo, lifeStatus);
        }

        private void NotifyLoginSuccess()
        {
            try
            {
                var frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "lifeAuthSuccess";
                requestDataObj["message"] = "登录成功";
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                FileUtils.LogRpa("登录成功，已通知前端", "来客登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知登录成功异常: {ex.Message}", "来客登录");
            }
        }

        private void NotifyLoginFailed(string errorMessage = "登录失败，请检查账号密码或网络连接")
        {
            try
            {
                var frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 401;
                requestDataObj["action"] = "lifeAuthFailed";
                requestDataObj["message"] = errorMessage;
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                FileUtils.LogRpa(errorMessage, "来客登录失败");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知登录失败异常: {ex.Message}", "来客登录失败");
            }
        }

        public string GetCachePath()
        {
            string cachePath = $"{Application.LocalUserAppDataPath}\\life\\cache";
            if (!Directory.Exists(cachePath))
            {
                Directory.CreateDirectory(cachePath);
            }
            return cachePath;
        }

        public string GetCookiePath()
        {
            string cookiePath = $"{Application.LocalUserAppDataPath}\\life\\cookies";
            if (!Directory.Exists(cookiePath))
            {
                Directory.CreateDirectory(cookiePath);
            }
            return cookiePath;
        }

        public async Task SaveCurrentCookiesToLocal()
        {
            if (browser == null || anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
            {
                FileUtils.LogRpa($"SaveCurrentCookiesToLocal 跳过: browser={browser != null}, anchorInfo={anchorInfo != null}, secUid={anchorInfo?.SecUid}", "来客登录");
                return;
            }

            try
            {
                var cookieManager = browser.RequestContext.GetCookieManager(null);
                if (cookieManager == null)
                {
                    FileUtils.LogRpa("SaveCurrentCookiesToLocal cookieManager为空", "来客登录");
                    return;
                }

                FileUtils.LogRpa($"开始采集Cookie, SecUid={anchorInfo.SecUid}, AnchorName={anchorInfo.AnchorName}", "来客登录");

                var domains = new List<string>
                {
                    "https://life.douyin.com",
                    "https://eos.douyin.com",
                    "https://jinritemai.com"
                };

                var allCookies = new List<CefSharp.Cookie>();
                foreach (var domain in domains)
                {
                    try
                    {
                        var domainCookies = await cookieManager.VisitUrlCookiesAsync(domain, includeHttpOnly: true);
                        if (domainCookies != null && domainCookies.Count > 0)
                        {
                            allCookies.AddRange(domainCookies);
                            FileUtils.LogRpa($"从{domain}采集到{domainCookies.Count}个Cookie", "来客登录");
                        }
                        else
                        {
                            FileUtils.LogRpa($"从{domain}采集到0个Cookie", "来客登录");
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"从{domain}获取Cookie失败: {ex.Message}", "来客登录");
                    }
                }

                // 检查是否采集到了Cookie
                if (allCookies.Count == 0)
                {
                    FileUtils.LogRpa("所有域名均未采集到Cookie，跳过保存（防止写入空文件覆盖已有数据）", "来客登录");
                    return;
                }

                // 检查关键Cookie是否就绪，未就绪则延迟重试一次
                var hasKeyCookie = allCookies.Any(c => c.Name.Contains("sessionid") || c.Name.Contains("sid_tt"));
                if (!hasKeyCookie)
                {
                    FileUtils.LogRpa("首次采集未发现关键Cookie(sessionid/sid_tt)，延迟3秒后重试", "来客登录");
                    await Task.Delay(3000);
                    allCookies.Clear();
                    foreach (var domain in domains)
                    {
                        try
                        {
                            var domainCookies = await cookieManager.VisitUrlCookiesAsync(domain, includeHttpOnly: true);
                            if (domainCookies != null && domainCookies.Count > 0)
                            {
                                allCookies.AddRange(domainCookies);
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"重试从{domain}获取Cookie失败: {ex.Message}", "来客登录");
                        }
                    }
                    FileUtils.LogRpa($"重试采集到{allCookies.Count}个Cookie", "来客登录");

                    if (allCookies.Count == 0)
                    {
                        FileUtils.LogRpa("重试后仍未采集到Cookie，跳过保存", "来客登录");
                        return;
                    }
                }

                var uniqueCookies = new Dictionary<string, CefSharp.Cookie>();
                foreach (var cookie in allCookies)
                {
                    var key = cookie.Name + cookie.Domain;
                    if (!uniqueCookies.ContainsKey(key))
                    {
                        uniqueCookies.Add(key, cookie);
                    }
                }
                var finalCookies = uniqueCookies.Values.ToList();

                string cookiePath = LifeUtils.getCookiePath(anchorInfo.SecUid);
                if (string.IsNullOrEmpty(cookiePath))
                {
                    FileUtils.LogRpa("SaveCurrentCookiesToLocal cookiePath为空", "来客登录");
                    return;
                }

                FileUtils.LogRpa($"Cookie保存路径: {cookiePath}, 去重后Cookie数量: {finalCookies.Count}", "来客登录");

                // 检查关键Cookie是否存在
                var hasSessionId = finalCookies.Any(c => c.Name.Contains("sessionid"));
                var hasSidTt = finalCookies.Any(c => c.Name.Contains("sid_tt"));
                FileUtils.LogRpa($"关键Cookie检查: sessionid={hasSessionId}, sid_tt={hasSidTt}", "来客登录");

                if (!hasSessionId && !hasSidTt)
                {
                    FileUtils.LogRpa("警告: 未采集到sessionid/sid_tt关键Cookie，来客数据采集可能失败", "来客登录");
                }

                string directory = Path.GetDirectoryName(cookiePath);
                if (!Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                try
                {
                    // 兼容旧格式：读取已有Cookie时兼容新旧格式
                    List<LifeCookieDto> existingDtos = new List<LifeCookieDto>();
                    if (File.Exists(cookiePath))
                    {
                        string existingContent = File.ReadAllText(cookiePath);
                        if (!string.IsNullOrEmpty(existingContent))
                        {
                            var (existingFileDto, _) = LifeCookieFileDto.Parse(existingContent);
                            existingDtos = existingFileDto.Cookies ?? new List<LifeCookieDto>();
                        }
                    }

                    var newDtos = finalCookies.ConvertAll(LifeCookieDto.FromCefCookie);
                    var mergedDtos = MergeCookies(existingDtos, newDtos);

                    // 新格式：带 awemeUserId 元数据
                    var currentAwemeUserId = anchorInfo.AnchorUserId;
                    var cookieFileDto = new LifeCookieFileDto
                    {
                        AwemeUserId = currentAwemeUserId,
                        SecUid = anchorInfo.SecUid,
                        Cookies = mergedDtos
                    };
                    var jsonStr = cookieFileDto.ToJson();
                    File.WriteAllText(cookiePath, jsonStr, System.Text.Encoding.UTF8);
                    FileUtils.LogRpa($"来客登录Cookie已保存到本地：{cookiePath}，合并后共{mergedDtos.Count}个Cookie，awemeUserId={currentAwemeUserId}", "来客登录");

                    // 立即更新 SecUid.json 中当前主播的 secUid/cookiePath（确保即使后续 Resolver 失败也能保持一致）
                    try
                    {
                        string md5Str = LifeUtils.getCachePathMd5(anchorInfo.SecUid);
                        string cookieFileName = $"life-{md5Str}";
                        FileUtils.LogRpa($"准备更新SecUid.json: currentAwemeUserId=[{currentAwemeUserId ?? "null"}], SecUid=[{anchorInfo.SecUid}], cookieFileName=[{cookieFileName}]", "来客登录");
                        LifeUtils.UpdateAwemeUserInSecUidJson(currentAwemeUserId, anchorInfo.SecUid, cookieFileName);
                    }
                    catch (Exception secUidEx)
                    {
                        FileUtils.LogRpa($"更新SecUid.json当前主播信息异常(非致命): {secUidEx.Message}", "来客登录");
                    }

                    // -----------------------------------------------------
                    // 多抖音号Cookie共享逻辑
                    // -----------------------------------------------------
                    await ProcessMultiAccountCookiesAsync(mergedDtos);
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"直接保存Cookie失败：{ex.Message}", "来客登录");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存Cookie异常: {ex.Message}", "来客登录");
            }
        }

        /// <summary>
        /// 通过浏览器JS评估获取来客账户信息（浏览器内自带anti-bot token）
        /// </summary>
        private async Task<Dictionary<string, string>> GetAccountDetailFromBrowserAsync()
        {
            try
            {
                if (browser == null || !browser.IsBrowserInitialized || !browser.CanExecuteJavascriptInMainFrame)
                {
                    FileUtils.LogRpa("GetAccountDetailFromBrowser 浏览器未就绪", "来客登录");
                    return null;
                }

                var mainFrame = browser.GetMainFrame();
                if (mainFrame == null)
                {
                    FileUtils.LogRpa("GetAccountDetailFromBrowser 主frame为空", "来客登录");
                    return null;
                }

                // 使用同步XMLHttpRequest在浏览器内发起API请求（自带Cookie和安全上下文）
                var jsCode = @"
                    (function() {
                        try {
                            var xhr = new XMLHttpRequest();
                            xhr.open('GET', '/life/gate/v1/account/detail?cpa=true', false);
                            xhr.setRequestHeader('Accept', 'application/json');
                            xhr.send();
                            return xhr.responseText;
                        } catch(e) {
                            return JSON.stringify({error: e.message});
                        }
                    })()
                ";

                FileUtils.LogRpa("GetAccountDetailFromBrowser 开始执行JS请求", "来客登录");
                var result = await mainFrame.EvaluateScriptAsync(jsCode);

                if (!result.Success || result.Result == null)
                {
                    FileUtils.LogRpa($"GetAccountDetailFromBrowser JS执行失败: {result.Message}", "来客登录");
                    return null;
                }

                string responseText = result.Result.ToString();
                FileUtils.LogRpa($"GetAccountDetailFromBrowser JS响应: {responseText}", "来客登录");

                if (string.IsNullOrEmpty(responseText))
                    return null;

                var jsonData = Newtonsoft.Json.Linq.JObject.Parse(responseText);
                int statusCode = jsonData["status_code"] != null ? (int)jsonData["status_code"] : -1;
                if (statusCode != 0)
                {
                    FileUtils.LogRpa($"GetAccountDetailFromBrowser API返回错误: status_code={statusCode}, status_msg={jsonData["status_msg"]}", "来客登录");
                    
                    // 尝试从页面DOM/JS状态中提取账户信息
                    return await ExtractAccountInfoFromPageAsync(mainFrame);
                }

                if (jsonData["data"] == null)
                    return null;

                var detail = jsonData["data"]["detail"] as Newtonsoft.Json.Linq.JObject;
                // 参照 laike.py L133/L683：data.account_id 就是 groupid，Python 不读 data.group_id
                string accountId = jsonData["data"]?["account_id"]?.ToString();
                var accountResult = new Dictionary<string, string>
                {
                    ["account_id"] = accountId,
                    ["account_name"] = jsonData["data"]?["account_name"]?.ToString(),
                    ["aweme_user_id"] = detail?["aweme_user_id"]?.ToString(),
                    ["life_account_id"] = detail?["life_account_id"]?.ToString(),
                    ["owner_user_id"] = detail?["owner_user_id"]?.ToString(),
                    ["group_id"] = accountId,
                };

                FileUtils.LogRpa($"GetAccountDetailFromBrowser 成功: group_id={accountResult["group_id"]}, life_account_id={accountResult["life_account_id"]}", "来客登录");
                return accountResult;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GetAccountDetailFromBrowser 异常: {ex.Message}", "来客登录");
                return null;
            }
        }

        /// <summary>
        /// 从页面内嵌的JS状态/script标签中提取账户信息
        /// </summary>
        private async Task<Dictionary<string, string>> ExtractAccountInfoFromPageAsync(CefSharp.IFrame mainFrame)
        {
            try
            {
                var jsCode = @"
                    (function() {
                        var result = {url: window.location.href, source: 'none', debug: ''};

                        // 尝试1: 从 URL 中提取 groupid
                        var url = window.location.href;
                        var groupMatch = url.match(/groupid=([^&]+)/);
                        if (groupMatch) return JSON.stringify({group_id: groupMatch[1], source: 'url'});

                        // 尝试2: 从页面状态中提取
                        var stateVars = ['__INITIAL_STATE__', '__NEXT_DATA__', '__SSR_DATA__', '__APP_DATA__', '__NUXT__', '__MerchantConfig__', '__PartnerPrefetch__', '__INIT_VMOK_DEPLOY_GLOBAL_DATA__', '__life_btm__', '__LS_STATBILITY_PLATFORM_CONTEXT_CONFIG__'];
                        var varDumps = [];
                        for (var s = 0; s < stateVars.length; s++) {
                            try {
                                var state = window[stateVars[s]];
                                if (state) {
                                    var stateStr = JSON.stringify(state);
                                    varDumps.push(stateVars[s] + '=' + stateStr.substring(0, 500));
                                    var gm = stateStr.match(/[""'](?:group_id|groupid|groupId|account_id|accountId|merchant_id|merchantId)[""']\s*:\s*[""']?(\d{10,})[""']?/);
                                    if (gm) return JSON.stringify({group_id: gm[1], source: stateVars[s]});
                                    var gm2 = stateStr.match(/[""'](?:group_id|groupid|groupId|account_id|accountId|merchant_id|merchantId)[""']\s*:\s*(\d{10,})/);
                                    if (gm2) return JSON.stringify({group_id: gm2[1], source: stateVars[s]});
                                }
                            } catch(e) {}
                        }
                        if (varDumps.length > 0) {
                            result.debug += '; varDumps=' + varDumps.join(' | ');
                        }

                        // 尝试3: 从 script 标签中搜索 (group_id/groupid/account_id 为数字值)
                        var scripts = document.querySelectorAll('script');
                        for (var i = 0; i < scripts.length; i++) {
                            var text = scripts[i].textContent || '';
                            if (text.length > 5000000 || text.length < 10) continue;
                            // 匹配 groupid 或 account_id 为数字（10位以上）
                            var gMatch = text.match(/(?:group_id|groupid|groupId|account_id)[""'\s:=]+[""']?(\d{10,})[""']?/);
                            if (gMatch) {
                                return JSON.stringify({group_id: gMatch[1], source: 'script_tag'});
                            }
                        }

                        // 尝试4: 从 localStorage/sessionStorage
                        try {
                            var storages = [sessionStorage, localStorage];
                            for (var si = 0; si < storages.length; si++) {
                                var storage = storages[si];
                                for (var j = 0; j < storage.length; j++) {
                                    var key = storage.key(j);
                                    var val = storage.getItem(key);
                                    if (val && (val.indexOf('group_id') > -1 || val.indexOf('groupid') > -1 || val.indexOf('account_id') > -1)) {
                                        var gm2 = val.match(/(?:group_id|groupid|account_id)[""'\s:=]+[""']?(\d{10,})[""']?/);
                                        if (gm2) return JSON.stringify({group_id: gm2[1], source: si==0?'sessionStorage':'localStorage'});
                                    }
                                }
                            }
                        } catch(e) {}

                        // 尝试5: 从 cookie 中查找 (某些场景下 groupid 存在 cookie 中)
                        try {
                            var cookies = document.cookie;
                            var cgMatch = cookies.match(/(?:group_id|groupid|account_id)=([^;\s]+)/);
                            if (cgMatch && /^\d{10,}$/.test(cgMatch[1])) {
                                return JSON.stringify({group_id: cgMatch[1], source: 'cookie'});
                            }
                        } catch(e) {}

                        // 尝试6: 从页面 a 标签中查找 groupid 参数
                        try {
                            var links = document.querySelectorAll('a[href*=""groupid""]');
                            if (links.length > 0) {
                                var lm = links[0].href.match(/groupid=([^&]+)/);
                                if (lm) return JSON.stringify({group_id: lm[1], source: 'link_tag'});
                            }
                        } catch(e) {}

                        // 诊断信息: 输出当前页面可用的全局变量名和 URL
                        result.debug = 'url=' + url + '; globals=';
                        var gNames = [];
                        for (var k in window) {
                            if (k.startsWith('__') && k.endsWith('__')) gNames.push(k);
                        }
                        result.debug += gNames.join(',');
                        return JSON.stringify(result);
                    })()
                ";

                FileUtils.LogRpa("尝试从页面状态提取账户信息", "来客登录");
                var result = await mainFrame.EvaluateScriptAsync(jsCode);

                if (!result.Success || result.Result == null)
                {
                    FileUtils.LogRpa($"ExtractAccountInfoFromPage JS执行失败或返回null: {result.Message}", "来客登录");
                    return null;
                }

                string json = result.Result.ToString();
                FileUtils.LogRpa($"ExtractAccountInfoFromPage 结果: {json}", "来客登录");

                if (string.IsNullOrEmpty(json) || json == "null")
                    return null;

                var obj = Newtonsoft.Json.Linq.JObject.Parse(json);
                string groupId = obj["group_id"]?.ToString();
                string source = obj["source"]?.ToString();

                if (!string.IsNullOrEmpty(groupId) && source != "none")
                {
                    var dict = new Dictionary<string, string>
                    {
                        ["group_id"] = groupId,
                        ["life_account_id"] = obj["life_account_id"]?.ToString() ?? "",
                        ["aweme_user_id"] = obj["aweme_user_id"]?.ToString() ?? "",
                    };
                    FileUtils.LogRpa($"ExtractAccountInfoFromPage 成功提取: group_id={groupId}, source={source}", "来客登录");
                    return dict;
                }

                // 未找到，输出诊断信息
                FileUtils.LogRpa($"ExtractAccountInfoFromPage 未找到groupid, debug={obj["debug"]}", "来客登录");
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"ExtractAccountInfoFromPage 异常: {ex.Message}", "来客登录");
                return null;
            }
        }

        /// <summary>
        /// 多抖音号Cookie共享处理（新版 v2，交给 LifeAccountResolver）：
        /// 1. 过滤 douyin.com 域 cookie
        /// 2. 一次调用 ResolveAllAccountsAsync，内部会：
        ///    - GroupAccountListShop 拿所有公司
        ///    - 逐家判断线索/普通版 + 菜单文字，选接口拿主播列表
        ///    - 写 SecUid.json v2（accounts + activeMap）
        ///    - 反查本地主播缓存，为已录入主播生成 life-{md5} cookie 文件，同步服务器授权状态
        /// 3. 若所有公司都失败→弹前端 NotifyLoginFailed
        /// </summary>
        private async Task ProcessMultiAccountCookiesAsync(List<LifeCookieDto> mergedCookies)
        {
            try
            {
                // 过滤 douyin.com 域的 Cookie（对齐 laike.py 只用核心Cookie）
                var cookieDict = new Dictionary<string, string>();
                foreach (var c in mergedCookies)
                {
                    if (string.IsNullOrEmpty(c.Domain) || c.Domain.Contains("douyin.com"))
                    {
                        cookieDict[c.Name] = c.Value;
                    }
                }
                FileUtils.LogRpa($"ProcessMultiAccountCookies cookieDict已过滤, 共{cookieDict.Count}个(douyin.com域), sessionid_ls={cookieDict.ContainsKey("sessionid_ls")}, sessionid_ss_ls={cookieDict.ContainsKey("sessionid_ss_ls")}, sid_tt_ls={cookieDict.ContainsKey("sid_tt_ls")}", "来客登录");

                // 交给 LifeAccountResolver 一次处理全部公司
                var resolveResult = await LifeAccountResolver.ResolveAllAccountsAsync(
                    cookieDict, mergedCookies, anchorInfo?.SecUid).ConfigureAwait(false);

                if (!resolveResult.AnySuccess)
                {
                    FileUtils.LogRpa($"ProcessMultiAccountCookies 所有公司处理均失败，总数={resolveResult.TotalCompanyCount}, 失败列表=[{string.Join(",", resolveResult.FailedCompanyNames)}]", "来客登录");
                    NotifyLoginFailed("未获取到任何拖音主播绑定信息，请确认来客账号已开通且含主播");
                    return;
                }

                FileUtils.LogRpa($"ProcessMultiAccountCookies 完成：公司={resolveResult.SuccessCompanyCount}/{resolveResult.TotalCompanyCount}，生成cookie={resolveResult.GeneratedCookieCount}，同步授权={resolveResult.SyncedAnchorCount}", "来客登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"ProcessMultiAccountCookies 异常: {ex.Message}\n{ex.StackTrace}", "来客登录");
            }
        }

        private async void LoadLifeCookies()
        {
            try
            {
                if (browser == null || anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
                {
                    FileUtils.LogRpa("LoadLifeCookies 跳过: browser或anchorInfo或SecUid为空", "来客登录");
                    return;
                }

                string cookiePath = LifeUtils.getCookiePath(anchorInfo.SecUid);
                if (!File.Exists(cookiePath))
                {
                    FileUtils.LogRpa($"LoadLifeCookies 本地Cookie文件不存在: {cookiePath}", "来客登录");
                    return;
                }

                string cookieContent = File.ReadAllText(cookiePath);
                var (loadFileDto, _) = LifeCookieFileDto.Parse(cookieContent);
                var cookies = loadFileDto.Cookies;
                if (cookies == null || cookies.Count == 0)
                    cookies = JsonConvert.DeserializeObject<List<LifeCookieDto>>(cookieContent);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"LoadLifeCookies Cookie文件为空或解析失败: {cookiePath}", "来客登录");
                    return;
                }

                FileUtils.LogRpa($"LoadLifeCookies 从本地加载到{cookies.Count}个Cookie，开始注入浏览器", "来客登录");

                var cookieManager = browser.RequestContext.GetCookieManager(null);
                if (cookieManager == null)
                {
                    return;
                }

                foreach (var cookie in cookies)
                {
                    var cefCookie = new CefSharp.Cookie
                    {
                        Name = cookie.Name,
                        Value = cookie.Value,
                        Domain = cookie.Domain,
                        Path = cookie.Path ?? "/",
                        Expires = cookie.Expires ?? DateTime.MinValue,
                        HttpOnly = cookie.HttpOnly,
                        Secure = cookie.Secure
                    };

                    await cookieManager.SetCookieAsync("https://life.douyin.com", cefCookie);
                    FileUtils.LogRpa($"从Cookie文件更新到来客: {cookie.Name}={cookie.Value}", "来客登录");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从Cookie文件更新到来客异常: {ex.Message}", "来客登录");
            }
        }

        private List<LifeCookieDto> MergeCookies(List<LifeCookieDto> existingDtos, List<LifeCookieDto> newDtos)
        {
            var cookieDict = new Dictionary<string, LifeCookieDto>();

            foreach (var existingDto in existingDtos)
            {
                var key = existingDto.Name + existingDto.Domain;
                cookieDict[key] = existingDto;
            }

            foreach (var newDto in newDtos)
            {
                var key = newDto.Name + newDto.Domain;
                cookieDict[key] = newDto;
            }

            return cookieDict.Values.ToList();
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            base.OnFormClosing(e);

            FileUtils.LogRpa("来客登录窗体开始关闭，准备停止定时器", "来客登录");

            _isFormClosed = true;

            if (_cookieCheckTimer != null)
            {
                try
                {
                    _cookieCheckTimer.Stop();
                    _cookieCheckTimer.Dispose();
                    FileUtils.LogRpa("定时器已成功停止并释放", "来客登录");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"停止定时器时发生异常: {ex.Message}", "来客登录");
                }
            }

            if (browser != null)
            {
                try
                {
                    browser.Dispose();
                    FileUtils.LogRpa("浏览器资源已成功释放", "来客登录");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"释放浏览器资源时发生异常: {ex.Message}", "来客登录");
                }
            }

            if (_loginStatus == LifeAuthStatusEnum.authing)
            {
                _loginStatus = LifeAuthStatusEnum.unAuth;
                LifeUtils.authing = false;
                FileUtils.LogRpa($"授权窗口关闭时状态仍为authing，恢复为未授权。loginSuccess={loginSuccess}, _loginSuccessProcessed={_loginSuccessProcessed}", "来客登录");
            }
            else
            {
                FileUtils.LogRpa($"授权窗口关闭，最终状态: loginSuccess={loginSuccess}, _loginStatus={_loginStatus}, _loginSuccessProcessed={_loginSuccessProcessed}", "来客登录");
            }

            FileUtils.LogRpa("来客登录窗体关闭完成", "来客登录");
        }

        public void closeForm()
        {
            try
            {
                this.Close();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"关闭来客窗体异常: {ex.Message}", "来客登录");
            }
        }

        private void InitializeComponent()
        {
            this.SuspendLayout();
            this.ClientSize = new Size(1000, 700);
            this.Name = "LifeForm";
            this.Text = "来客登录";
            this.ResumeLayout(false);
        }
    }

    public class LifeSpanHandler : ILifeSpanHandler
    {
        private readonly LifeForm _form;

        public LifeSpanHandler(LifeForm form)
        {
            _form = form;
        }

        public bool OnBeforePopup(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, string targetUrl, string targetFrameName, WindowOpenDisposition targetDisposition, bool userGesture, IPopupFeatures popupFeatures, IWindowInfo windowInfo, IBrowserSettings browserSettings, ref bool noJavascriptAccess, out IWebBrowser newBrowser)
        {
            newBrowser = null;
            return true;
        }

        public void OnAfterCreated(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
        }

        public bool DoClose(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
            return false;
        }

        public void OnBeforeClose(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
        }
    }
}