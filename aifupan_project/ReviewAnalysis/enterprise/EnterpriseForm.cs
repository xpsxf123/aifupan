using CefSharp;
using CefSharp.WinForms;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Model;
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

namespace ReviewAnalysis.enterprise
{
    /// <summary>
    /// 企业号登录窗体
    /// </summary>
    public partial class EnterpriseForm : Form
    {
        private ChromiumWebBrowser browser;
        public AnchorInfo anchorInfo { get; set; }
        public bool isTemp { get; set; }
        public long addTempTime { get; set; }

        private EnterpriseAuthStatusEnum _loginStatus = EnterpriseAuthStatusEnum.unAuth;
        private bool loginSuccess = false;
        private DateTime _loginStartTime;
        private bool _isFormClosed = false;
        private System.Windows.Forms.Timer _cookieCheckTimer;

        private string LoginUrl = "https://leads.cluerich.com/pc/auth/login";
        private string LoginSuccessUrl = "https://leads.cluerich.com/";

        public EnterpriseForm()
        {
            InitializeComponent();
        }

        public EnterpriseForm(AnchorInfo anchorInfo)
        {
            this.anchorInfo = anchorInfo;
            InitializeComponent();
            Init();
        }

        public void Init()
        {
            Init(false);
        }

        public void Init(bool bHide)
        {
            try
            {
                this.Text = "企业号登录";
                this.WindowState = FormWindowState.Maximized;
                this.StartPosition = FormStartPosition.CenterScreen;
                this.FormBorderStyle = FormBorderStyle.FixedSingle;
                this.MaximizeBox = false;
                this.MinimizeBox = false;

                if (bHide)
                {
                    this.Visible = false;
                }

                InitBrowser();
                OpenLogin();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化企业号窗体异常: {ex.Message}", "企业号登录");
                NotifyLoginFailed($"初始化失败: {ex.Message}");
                this.Close();
            }
        }

        private void InitBrowser()
        {
            try
            {
                string url = LoginUrl;

                BrowserSettings browserSettings = new BrowserSettings();
                browserSettings.Javascript = CefState.Enabled;
                browserSettings.WebGl = CefState.Enabled;

                RequestContextSettings requestContextSettings = new RequestContextSettings();
                requestContextSettings.PersistSessionCookies = false;
                requestContextSettings.CachePath = GetCachePath();

                browser = new ChromiumWebBrowser(url)
                {
                    BrowserSettings = browserSettings,
                    RequestContext = new RequestContext(requestContextSettings)
                };

                browser.FrameLoadEnd += Browser_FrameLoadEnd;
                browser.LoadingStateChanged += Browser_LoadingStateChanged;

                this.Controls.Add(browser);
                browser.Dock = DockStyle.Fill;

                // 启动Cookie检查定时器
                StartCookieCheckTimer();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化浏览器异常: {ex.Message}", "企业号登录");
                throw;
            }
        }

        private void Browser_LoadingStateChanged(object sender, LoadingStateChangedEventArgs e)
        {
            // 可以在此处理加载状态变化
        }

        private void Browser_FrameLoadEnd(object sender, FrameLoadEndEventArgs e)
        {
            try
            {
                string url = e.Browser.MainFrame.Url;

                // 登录页面加载完成，注入CSS隐藏非登录区域
                if (url.Contains("leads.cluerich.com/pc/auth/login"))
                {
                    InjectLoginPageCss();
                }

                if (url.Contains("leads.cluerich.com") && !url.Contains("login"))
                {
                    loginSuccess = true;
                    _loginStatus = EnterpriseAuthStatusEnum.auth;
                    
                    // 登录成功，保存Cookie
                    Task.Run(async () => await SaveCurrentCookiesToLocal());
                    
                    NotifyLoginSuccess();
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"FrameLoadEnd异常: {ex.Message}", "企业号登录");
            }
        }

        /// <summary>
        /// 注入CSS隐藏登录页面的非登录区域，只保留登录表单
        /// </summary>
        private void InjectLoginPageCss()
        {
            try
            {
                string css = @"
                    /* 隐藏顶部导航 */
                    .header, .navbar, .nav, .top-bar, [class*='header'], [class*='navbar'] {
                        display: none !important;
                    }
                    /* 隐藏底部 */
                    .footer, .bottom, [class*='footer'] {
                        display: none !important;
                    }
                    /* 隐藏侧边栏 */
                    .sidebar, .aside, [class*='sidebar'] {
                        display: none !important;
                    }
                    /* 隐藏banner/广告区域 */
                    .banner, .carousel, .slider, [class*='banner'] {
                        display: none !important;
                    }
                    /* 登录容器居中 */
                    .login-container, .login-box, .login-form, [class*='login-container'], [class*='login-box'] {
                        margin: 0 auto !important;
                        position: relative !important;
                        top: 0 !important;
                        left: 0 !important;
                        transform: none !important;
                    }
                    /* 页面主体 */
                    body {
                        overflow: hidden !important;
                        background: #fff !important;
                    }
                ";

                string script = $@"
                    (function() {{
                        var style = document.createElement('style');
                        style.id = 'enterprise-login-css';
                        style.textContent = `{css}`;
                        document.head.appendChild(style);
                        
                        // 滚动到登录区域
                        var loginBox = document.querySelector('.login-container, .login-box, .login-form, [class*=""login""]');
                        if (loginBox) {{
                            loginBox.scrollIntoView({{ behavior: 'instant', block: 'start' }});
                        }}
                    }})();
                ";

                browser.ExecuteScriptAsync(script);
                FileUtils.LogRpa("已注入登录页面CSS，隐藏非登录区域", "企业号登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"注入CSS异常: {ex.Message}", "企业号登录");
            }
        }

        private void StartCookieCheckTimer()
        {
            _cookieCheckTimer = new System.Windows.Forms.Timer();
            _cookieCheckTimer.Interval = 2000; // 2秒检查一次
            _cookieCheckTimer.Tick += async (s, e) => await CheckLoginStatus();
            _cookieCheckTimer.Start();
        }

        private async Task CheckLoginStatus()
        {
            try
            {
                if (loginSuccess || _isFormClosed) return;

                // 检查Cookie是否有效
                var cookies = await GetBrowserCookies();
                if (cookies != null && cookies.ContainsKey("sessionid"))
                {
                    loginSuccess = true;
                    _loginStatus = EnterpriseAuthStatusEnum.auth;
                    
                    // 保存Cookie
                    await SaveCookiesToLocal(cookies);
                    
                    NotifyLoginSuccess();
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检查登录状态异常: {ex.Message}", "企业号登录");
            }
        }

        private void OpenLogin()
        {
            try
            {
                if (browser != null && !browser.IsDisposed)
                {
                    browser.Load(LoginUrl);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"打开登录页异常: {ex.Message}", "企业号登录");
            }
        }

        private async Task SaveCurrentCookiesToLocal()
        {
            try
            {
                var cookies = await GetBrowserCookies();
                if (cookies != null && cookies.Count > 0)
                {
                    await SaveCookiesToLocal(cookies);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存Cookie异常: {ex.Message}", "企业号登录");
            }
        }

        private async Task<Dictionary<string, string>> GetBrowserCookies()
        {
            try
            {
                var cookieManager = browser.GetCookieManager();
                var cookies = await cookieManager.VisitAllCookiesAsync();
                
                var result = new Dictionary<string, string>();
                foreach (var cookie in cookies)
                {
                    if (cookie.Domain.Contains("cluerich.com"))
                    {
                        result[cookie.Name] = cookie.Value;
                    }
                }

                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取浏览器Cookie异常: {ex.Message}", "企业号登录");
                return null;
            }
        }

        private async Task SaveCookiesToLocal(Dictionary<string, string> cookies)
        {
            try
            {
                string cookiePath = EnterpriseUtils.getCookiePath(anchorInfo.SecUid);
                string directory = Path.GetDirectoryName(cookiePath);
                if (!Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                var cookieList = cookies.Select(c => new EnterpriseCookieDto
                {
                    name = c.Key,
                    value = c.Value,
                    domain = ".cluerich.com",
                    path = "/",
                    expires = DateTime.Now.AddDays(30),
                    httpOnly = true,
                    secure = true
                }).ToList();

                string json = JsonConvert.SerializeObject(cookieList, Formatting.Indented);
                File.WriteAllText(cookiePath, json);

                FileUtils.LogRpa($"企业号Cookie已保存到: {cookiePath}", "企业号登录");

                // 更新主播授权状态
                if (anchorInfo != null)
                {
                    anchorInfo.enterpriseAuthStatus = (int)EnterpriseAuthStatusEnum.auth;
                    // 同步到服务器
                    // await AnchorApi.UpdateAuthStatus(anchorInfo.SecUid, "enterprise", EnterpriseAuthStatusEnum.auth);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存Cookie到本地异常: {ex.Message}", "企业号登录");
            }
        }

        private void NotifyLoginSuccess()
        {
            try
            {
                if (anchorInfo != null)
                {
                    FrontNotice.sendEnterpriseAuthSuccess(anchorInfo.SecUid);
                    FileUtils.LogRpa($"企业号登录成功: {anchorInfo.AnchorName}", "企业号登录");
                }

                // 关闭窗体
                this.Invoke(new Action(() =>
                {
                    _cookieCheckTimer?.Stop();
                    _cookieCheckTimer?.Dispose();
                    this.Close();
                }));
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知登录成功异常: {ex.Message}", "企业号登录");
            }
        }

        private void NotifyLoginFailed(string message)
        {
            try
            {
                if (anchorInfo != null)
                {
                    FrontNotice.sendEnterpriseAuthFailed(anchorInfo.SecUid, message);
                    FileUtils.LogRpa($"企业号登录失败: {message}", "企业号登录");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知登录失败异常: {ex.Message}", "企业号登录");
            }
        }

        private string GetCachePath()
        {
            string cachePath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "ReviewAnalysis", "EnterpriseCache", anchorInfo?.SecUid ?? "default");
            if (!Directory.Exists(cachePath))
            {
                Directory.CreateDirectory(cachePath);
            }
            return cachePath;
        }

        public void closeForm()
        {
            try
            {
                _isFormClosed = true;
                _cookieCheckTimer?.Stop();
                _cookieCheckTimer?.Dispose();
                
                if (browser != null && !browser.IsDisposed)
                {
                    browser.Dispose();
                }
                
                this.Close();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"关闭企业号窗体异常: {ex.Message}", "企业号登录");
            }
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            _isFormClosed = true;
            _cookieCheckTimer?.Stop();
            _cookieCheckTimer?.Dispose();
            base.OnFormClosing(e);
        }
    }
}
