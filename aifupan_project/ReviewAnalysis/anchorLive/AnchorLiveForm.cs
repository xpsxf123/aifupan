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
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis.anchorLive
{
    /// <summary>
    /// 主播后台授权登录窗体
    /// </summary>
    public class AnchorLiveForm : Form
    {
        private ChromiumWebBrowser browser;
        public AnchorInfo anchorInfo { get; set; }
        
        private AnchorLiveAuthStatusEnum _loginStatus = AnchorLiveAuthStatusEnum.unAuth;
        private bool loginSuccess = false;
        private DateTime _loginStartTime;
        private bool _isFormClosed = false;
        private System.Windows.Forms.Timer _cookieCheckTimer;

        private string LoginUrl = "https://anchor.douyin.com/login";
        private string LoginSuccessUrl = "https://anchor.douyin.com";

        public AnchorLiveForm()
        {
        }

        public AnchorLiveForm(AnchorInfo anchorInfo)
        {
            this.anchorInfo = anchorInfo;
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
                this.Text = "主播后台登录";
                this.StartPosition = FormStartPosition.CenterScreen;
                this.FormBorderStyle = FormBorderStyle.Sizable;
                this.WindowState = FormWindowState.Maximized;

                if (bHide)
                {
                    this.Visible = false;
                }

                InitBrowser();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化主播后台窗体异常：{ex.Message}\n{ex.StackTrace}", "主播后台登录");
                AnchorLiveUtils.authing = false;
                NotifyLoginFailed($"初始化失败：{ex.Message}");
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

                browser.LifeSpanHandler = new LifeSpanHandler(this);

                Panel browserPanel = new Panel
                {
                    Size = this.ClientSize,
                    AutoScroll = true,
                    Location = new System.Drawing.Point(0, 0)
                };

                browser.Size = new System.Drawing.Size(browserPanel.ClientSize.Width, 1500);
                browser.Location = new System.Drawing.Point(0, 0);
                browserPanel.Controls.Add(browser);
                this.Controls.Add(browserPanel);

                browser.LoadingStateChanged += (sender, e) =>
                {
                    if (e.IsLoading == false)
                    {
                        browser.ExecuteScriptAsync(@"document.body.style.overflow = 'auto';");
                        browser.ExecuteScriptAsync(@"document.documentElement.style.overflow = 'auto';");

                        if (this.InvokeRequired)
                        {
                            this.BeginInvoke(new Action(() =>
                            {
                                browserPanel.Size = this.ClientSize;
                                browser.Size = new System.Drawing.Size(browserPanel.ClientSize.Width, browserPanel.ClientSize.Height);
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

                LoadAnchorCookies();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化浏览器异常：{ex.Message}", "主播后台登录");
                throw;
            }
        }

        private async void Browser_FrameLoadEnd(object sender, FrameLoadEndEventArgs e)
        {
            try
            {
                if (e == null || string.IsNullOrEmpty(e.Url))
                    return;

                // 检测登录成�?
                if (e.Url.Contains(LoginSuccessUrl) && !e.Url.Contains("/login"))
                {
                    loginSuccess = true;
                    _loginStatus = AnchorLiveAuthStatusEnum.auth;
                    await SaveCurrentCookiesToLocal();
                    NotifyLoginSuccess();
                    
                    try
                    {
                        if (anchorInfo != null)
                        {
                            anchorInfo.anchorLiveAuthStatus = (int)AnchorLiveAuthStatusEnum.auth;
                            AnchorLiveUtils.UpdateAnchorAuthStatus(anchorInfo, AnchorLiveAuthStatusEnum.auth);
                            FileUtils.LogRpa("主播后台授权状态已更新", "主播后台登录");
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"同步授权状态到服务端发生异常：{ex.Message}", "主播后台登录");
                    }

                    if (this.InvokeRequired)
                    {
                        this.BeginInvoke(new Action(() =>
                        {
                            this.WindowState = FormWindowState.Maximized;
                        }));
                    }
                    FileUtils.LogRpa("登录成功，等待用户操作，窗体已最大化", "主播后台登录");
                }
                else if (e.Url.Contains("/login") && !loginSuccess)
                {
                    FileUtils.LogRpa("正在加载主播后台登录页面...", "主播后台登录");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"FrameLoadEnd 异常：{ex.Message}", "主播后台登录");
            }
        }

        private void ChromeBrowser_LoadError(object sender, LoadErrorEventArgs e)
        {
            if (!_isFormClosed)
            {
                FileUtils.LogRpa($"加载错误：{e.ErrorText}, URL: {e.FailedUrl}", "主播后台登录");
                
                if (this.InvokeRequired)
                {
                    this.BeginInvoke(new Action(() =>
                    {
                        var label = new Label
                        {
                            Text = $"加载失败：{e.ErrorText}\n\n请检查网络连接后重试",
                            AutoSize = true,
                            ForeColor = Color.Red,
                            TextAlign = ContentAlignment.MiddleCenter
                        };
                        this.Controls.Add(label);
                        label.BringToFront();
                    }));
                }
            }
        }

        private async void CookieCheckTimer_Tick(object sender, EventArgs e)
        {
            try
            {
                if (loginSuccess)
                    return;

                var cookies = await GetCookiesFromBrowser();
                if (cookies != null && cookies.Count > 0)
                {
                    var sessionCookie = cookies.FirstOrDefault(c => 
                        c.Name.Equals("sessionid", StringComparison.OrdinalIgnoreCase));
                    
                    if (!string.IsNullOrEmpty(sessionCookie?.Value))
                    {
                        loginSuccess = true;
                        _loginStatus = AnchorLiveAuthStatusEnum.auth;
                        await SaveCurrentCookiesToLocal();
                        NotifyLoginSuccess();
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检�?Cookie 异常：{ex.Message}", "主播后台登录");
            }
        }

        private async Task<List<AnchorLiveCookieDto>> GetCookiesFromBrowser()
        {
            try
            {
                var cookieManager = browser.GetCookieManager();
                var cefCookies = await cookieManager.VisitAllCookiesAsync();
                var result = new List<AnchorLiveCookieDto>();
                if (cefCookies != null)
                {
                    foreach (var cookie in cefCookies)
                    {
                        result.Add(new AnchorLiveCookieDto
                        {
                            Name = cookie.Name,
                            Value = cookie.Value,
                            Domain = cookie.Domain,
                            Path = cookie.Path,
                            Expires = cookie.Expires,
                            Secure = cookie.Secure,
                            HttpOnly = cookie.HttpOnly
                        });
                    }
                }
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取 Cookie 异常：{ex.Message}", "主播后台登录");
                return new List<AnchorLiveCookieDto>();
            }
        }

        private async Task SaveCurrentCookiesToLocal()
        {
            try
            {
                var cookies = await GetCookiesFromBrowser();
                if (cookies != null && cookies.Count > 0)
                {
                    // 转换�?Dictionary<string, string>
                    var cookieDict = new Dictionary<string, string>();
                    foreach (var cookie in cookies)
                    {
                        if (!string.IsNullOrEmpty(cookie.Name) && !string.IsNullOrEmpty(cookie.Value))
                        {
                            cookieDict[cookie.Name] = cookie.Value;
                        }
                    }
                    
                    // 保存 Cookie
                    AnchorLiveDataHandle.SaveCookiesToLocal(anchorInfo.SecUid, cookieDict);
                    FileUtils.LogRpa($"主播后台 Cookie 已保存，数量：{cookieDict.Count}", "主播后台登录");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存 Cookie 异常：{ex.Message}", "主播后台登录");
            }
        }

        private void LoadAnchorCookies()
        {
            try
            {
                if (anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
                    return;

                string cookiePath = AnchorLiveUtils.getCookiePath(anchorInfo.SecUid);
                if (!File.Exists(cookiePath))
                    return;

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<AnchorLiveCookieDto>>(cookieContent);
                
                if (cookies == null || cookies.Count == 0)
                    return;

                // 预加�?Cookie 到浏览器
                var cookieManager = browser.GetCookieManager();
                foreach (var cookie in cookies)
                {
                    var cefCookie = new CefSharp.Cookie
                    {
                        Name = cookie.Name,
                        Value = cookie.Value,
                        Domain = cookie.Domain,
                        Path = cookie.Path,
                        Expires = cookie.Expires,
                        Secure = cookie.Secure,
                        HttpOnly = cookie.HttpOnly
                    };
                    
                    cookieManager.SetCookie("https://anchor.douyin.com", cefCookie);
                }

                FileUtils.LogRpa($"已预加载 {cookies.Count} 个主播后�?Cookie", "主播后台登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"加载本地 Cookie 异常：{ex.Message}", "主播后台登录");
            }
        }

        private void NotifyLoginSuccess()
        {
            try
            {
                if (this.InvokeRequired)
                {
                    this.BeginInvoke(new Action(() =>
                    {
                        var lblTip = new Label
                        {
                            Text = "�?登录成功！\n\n窗口将在 3 秒后自动关闭...",
                            AutoSize = true,
                            ForeColor = Color.Green,
                            Font = new Font("微软雅黑", 14, FontStyle.Bold),
                            BackColor = Color.White,
                            Dock = DockStyle.Fill,
                            TextAlign = ContentAlignment.MiddleCenter
                        };
                        this.Controls.Clear();
                        this.Controls.Add(lblTip);
                    }));
                }

                Task.Delay(3000).ContinueWith(t =>
                {
                    if (!_isFormClosed && this.InvokeRequired)
                    {
                        this.BeginInvoke(new Action(() => this.Close()));
                    }
                });
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知登录成功异常：{ex.Message}", "主播后台登录");
            }
        }

        private void NotifyLoginFailed(string message)
        {
            try
            {
                if (this.InvokeRequired)
                {
                    this.BeginInvoke(new Action(() =>
                    {
                        MessageBox.Show(this, message, "登录失败", 
                            MessageBoxButtons.OK, MessageBoxIcon.Error);
                    }));
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知登录失败异常：{ex.Message}", "主播后台登录");
            }
        }

        private string GetCachePath()
        {
            try
            {
                string basePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "cache", "anchor");
                if (!Directory.Exists(basePath))
                {
                    Directory.CreateDirectory(basePath);
                }
                return basePath;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取缓存路径异常：{ex.Message}", "主播后台登录");
                return Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "cache");
            }
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            _isFormClosed = true;
            AnchorLiveUtils.authing = false;
            
            if (_cookieCheckTimer != null)
            {
                _cookieCheckTimer.Dispose();
            }

            if (browser != null)
            {
                browser.Dispose();
            }

            base.OnFormClosing(e);
        }
    }



    /// <summary>
    /// 生命周期处理�?
    /// </summary>
    public class LifeSpanHandler : ILifeSpanHandler
    {
        private AnchorLiveForm _form;

        public LifeSpanHandler(AnchorLiveForm form)
        {
            _form = form;
        }

        public bool OnBeforePopup(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, string targetUrl, string targetFrameName, WindowOpenDisposition targetDisposition, bool userGesture, IPopupFeatures popupFeatures, IWindowInfo windowInfo, IBrowserSettings browserSettings, ref bool noJavascriptAccess, out IWebBrowser newBrowser)
        {
            newBrowser = null;
            return false;
        }

        public void OnAfterCreated(IWebBrowser chromiumBrowser, IBrowser browser)
        {
        }

        public bool DoClose(IWebBrowser chromiumBrowser, IBrowser browser)
        {
            return false;
        }

        public void OnBeforeClose(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
        }
    }
}
