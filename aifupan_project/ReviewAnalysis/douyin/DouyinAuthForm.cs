using CefSharp;
using CefSharp.Callback;
using CefSharp.WinForms;
using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Asr;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis.DouyinAuth
{
    /// <summary>
    /// 抖音授权登录窗体
    /// 弹出二维码供用户扫码授权，授权成功后保存Cookie和更新授权状态
    /// 抖音授权不绑定特定主播，Cookie使用 MD5(租户ID-用户ID) 标识
    /// </summary>
    public class DouyinAuthForm : Form
    {
        private ChromiumWebBrowser browser;

        private bool loginSuccess = false;
        private bool _isFormClosed = false;
        private System.Windows.Forms.Timer _cookieCheckTimer;

        private Panel _qrPanel;
        private PictureBox _qrPictureBox;
        private Label _qrLabel;

        private string LoginUrl = "https://www.douyin.com/user/self";
        private string LoginSuccessUrl = "https://www.douyin.com";

        /// <summary>
        /// 扫码状态变更事件
        /// </summary>
        public event EventHandler<QrScanStatus> OnQrStatusChanged;

        public DouyinAuthForm()
        {
        }

        public void Init()
        {
            Init(false);
        }

        public void Init(bool bHide)
        {
            try
            {
                this.Text = "抖音授权登录";
                this.StartPosition = FormStartPosition.CenterScreen;
                this.Size = new Size(420, 320);
                this.FormBorderStyle = FormBorderStyle.FixedSingle;
                this.MaximizeBox = false;
                this.MinimizeBox = true;

                if (bHide)
                {
                    this.Visible = false;
                }

                InitBrowser();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化抖音授权窗体异常：{ex.Message}\n{ex.StackTrace}", "抖音授权");
                DouyinAuthUtils.SetAuthFailed();
                this.Close();
            }
        }

        private void InitBrowser()
        {
            try
            {
                string url = LoginUrl;
                FileUtils.LogRpa($"抖音授权窗体开始加载URL：{url}", "抖音授权");

                BrowserSettings browserSettings = new BrowserSettings();
                browserSettings.Javascript = CefState.Enabled;
                browserSettings.WebGl = CefState.Disabled;

                // 异步清除 CEF 缓存目录，避免同步 IO 阻塞 browser 创建影响加载速度
                string cachePath = GetCachePath();
                Task.Run(() =>
                {
                    try
                    {
                        if (Directory.Exists(cachePath))
                            Directory.Delete(cachePath, true);
                        Directory.CreateDirectory(cachePath);
                    }
                    catch { }
                });

                RequestContextSettings requestContextSettings = new RequestContextSettings();
                requestContextSettings.PersistSessionCookies = false;
                requestContextSettings.CachePath = cachePath;

                browser = new ChromiumWebBrowser(url)
                {
                    BrowserSettings = browserSettings,
                    RequestContext = new RequestContext(requestContextSettings),
                    RequestHandler = new DouyinQrRequestHandler(this)
                };

                // 二维码展示区域（直接展开，窗体只显示二维码）
                _qrLabel = new Label
                {
                    Text = "请使用抖音APP扫描下方二维码授权登录",
                    Location = new System.Drawing.Point(0, 5),
                    Height = 28,
                    Width = 400,
                    TextAlign = ContentAlignment.MiddleCenter,
                    Font = new Font("微软雅黑", 10, FontStyle.Bold),
                    ForeColor = Color.FromArgb(30, 30, 30),
                    BackColor = Color.White
                };

                _qrPictureBox = new PictureBox
                {
                    Size = new Size(220, 220),
                    SizeMode = PictureBoxSizeMode.StretchImage,
                    Location = new System.Drawing.Point(90, 35),
                    BackColor = Color.White
                };

                _qrPanel = new Panel
                {
                    Dock = DockStyle.Fill,
                    BackColor = Color.White
                };
                _qrPanel.Controls.Add(_qrPictureBox);
                _qrPanel.Controls.Add(_qrLabel);

                // browser 放入 1x1 的 Panel 并移到窗体可见区域之外（负坐标）
                // 控件句柄存在且 Visible=true，CEF 正常渲染网络请求，用户完全看不到
                browser.Size = new Size(1, 1);
                Panel browserPanel = new Panel
                {
                    Dock = DockStyle.None,
                    Size = new Size(1, 1),
                    Location = new System.Drawing.Point(-10, -10),
                    AutoScroll = false,
                    BackColor = Color.White
                };
                browserPanel.Controls.Add(browser);

                this.Controls.Add(browserPanel);
                this.Controls.Add(_qrPanel);

                browser.LoadingStateChanged += (sender, e) =>
                {
                    if (e.IsLoading == false)
                    {
                        browser.ExecuteScriptAsync(@"document.body.style.overflow = 'auto';");
                        browser.ExecuteScriptAsync(@"document.documentElement.style.overflow = 'auto';");
                    }
                };

                browser.FrameLoadEnd += Browser_FrameLoadEnd;
                browser.LoadError += ChromeBrowser_LoadError;

                // Cookie检测定时器（每3秒检查一次sessionid）
                // 注意：初始不启动，等页面跳转到登录页后再启动，避免误读预加载的旧Cookie
                _cookieCheckTimer = new System.Windows.Forms.Timer
                {
                    Interval = 3000,
                    Enabled = false
                };
                _cookieCheckTimer.Tick += CookieCheckTimer_Tick;

                // 预加载已有Cookie
                LoadExistingCookies();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化抖音浏览器异常：{ex.Message}", "抖音授权");
                throw;
            }
        }

        private async void Browser_FrameLoadEnd(object sender, FrameLoadEndEventArgs e)
        {
            try
            {
                if (e == null || string.IsNullOrEmpty(e.Url))
                    return;

                FileUtils.LogRpa($"页面加载完成：{e.Url}", "抖音授权");

                // 检测登录成功：URL不含/login且在douyin.com域名下，且浏览器已持有sessionid
                if (e.Url.Contains(LoginSuccessUrl) && !e.Url.Contains("/login") && !loginSuccess)
                {
                    // 仅URL满足不够，必须确认 sessionid 已存在，防止入口页重定向误判
                    var cookies = await GetCookiesFromBrowser();
                    var sessionCookie = cookies?.FirstOrDefault(c =>
                        !string.IsNullOrEmpty(c.Name) && c.Name.Contains("sessionid") &&
                        !string.IsNullOrEmpty(c.Value));

                    if (sessionCookie == null)
                    {
                        FileUtils.LogRpa($"URL满足条件但无sessionid，跳过（可能是入口页重定向）：{e.Url}", "抖音授权");
                        return;
                    }

                    loginSuccess = true;
                    await SaveCurrentCookiesToLocal();
                    NotifyLoginSuccess();

                    FileUtils.LogRpa("抖音授权登录成功", "抖音授权");

                    if (this.InvokeRequired)
                    {
                        this.BeginInvoke(new Action(() =>
                        {
                            this.WindowState = FormWindowState.Maximized;
                        }));
                    }
                    FileUtils.LogRpa("抖音登录成功，窗体已最大化", "抖音授权");
                }
                else if (e.Url.Contains("/login") && !loginSuccess)
                {
                    FileUtils.LogRpa("正在加载抖音登录页面...", "抖音授权");
                    // 到达登录页后才开始检测 Cookie（避免误读预加载的旧 Cookie）
                    if (_cookieCheckTimer != null && !_cookieCheckTimer.Enabled)
                    {
                        if (this.InvokeRequired)
                            this.BeginInvoke(new Action(() => _cookieCheckTimer.Start()));
                        else
                            _cookieCheckTimer.Start();
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"FrameLoadEnd异常：{ex.Message}", "抖音授权");
            }
        }

        private void ChromeBrowser_LoadError(object sender, LoadErrorEventArgs e)
        {
            if (!_isFormClosed)
            {
                FileUtils.LogRpa($"加载错误：{e.ErrorText}, URL: {e.FailedUrl}", "抖音授权");
            }
        }

        private async void CookieCheckTimer_Tick(object sender, EventArgs e)
        {
            try
            {
                if (loginSuccess || browser == null || browser.IsDisposed)
                    return;

                var cookies = await GetCookiesFromBrowser();
                if (cookies != null && cookies.Count > 0)
                {
                    var domainList = string.Join(", ", cookies.Select(c => $"{c.Name}@{c.Domain}").Distinct());
                    FileUtils.LogRpa($"Cookie检测：共{cookies.Count}个Cookie，域名分布：{domainList}", "抖音授权");

                    var sessionCookie = cookies.FirstOrDefault(c =>
                        !string.IsNullOrEmpty(c.Name) && c.Name.Contains("sessionid"));

                    if (!string.IsNullOrEmpty(sessionCookie?.Value))
                    {
                        loginSuccess = true;
                        await SaveCurrentCookiesToLocal();
                        NotifyLoginSuccess();

                        FileUtils.LogRpa("抖音授权登录成功（Cookie检测）", "抖音授权");
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检测Cookie异常：{ex.Message}", "抖音授权");
            }
        }

        private async Task<List<DouyinAuthCookieDto>> GetCookiesFromBrowser()
        {
            try
            {
                var cookieManager = browser.GetCookieManager();
                var cefCookies = await cookieManager.VisitAllCookiesAsync();
                var result = new List<DouyinAuthCookieDto>();
                if (cefCookies != null)
                {
                    foreach (var cookie in cefCookies)
                    {
                        // 只保留douyin.com域名的Cookie（防御null）
                        if (!string.IsNullOrEmpty(cookie.Domain) && cookie.Domain.Contains("douyin.com"))
                        {
                            result.Add(new DouyinAuthCookieDto
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
                }
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取Cookie异常：{ex.Message}", "抖音授权");
                return new List<DouyinAuthCookieDto>();
            }
        }

        /// <summary>
        /// 保存Cookie到本地文件（使用 MD5(租户ID-用户ID) 标识路径）
        /// </summary>
        private async Task SaveCurrentCookiesToLocal()
        {
            try
            {
                var cookies = await GetCookiesFromBrowser();
                if (cookies != null && cookies.Count > 0)
                {
                    string cookiePath = DouyinAuthUtils.getCookiePath();

                    // 确保目录存在
                    string dir = Path.GetDirectoryName(cookiePath);
                    if (!Directory.Exists(dir))
                    {
                        Directory.CreateDirectory(dir);
                    }

                    string json = JsonConvert.SerializeObject(cookies, Formatting.Indented);
                    File.WriteAllText(cookiePath, json, System.Text.Encoding.UTF8);

                    FileUtils.LogRpa($"抖音Cookie已保存：{cookiePath}，共{cookies.Count}个", "抖音授权");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存抖音Cookie异常：{ex.Message}", "抖音授权");
            }
        }

        /// <summary>
        /// 预加载已有Cookie到浏览器
        /// </summary>
        private void LoadExistingCookies()
        {
            try
            {
                string cookiePath = DouyinAuthUtils.getCookiePath();
                if (!File.Exists(cookiePath))
                    return;

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<DouyinAuthCookieDto>>(cookieContent);

                if (cookies == null || cookies.Count == 0)
                    return;

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

                    string domain = cookie.Domain.StartsWith(".") ? cookie.Domain.Substring(1) : cookie.Domain;
                    cookieManager.SetCookie($"https://{domain}", cefCookie);
                }

                FileUtils.LogRpa($"已预加载{cookies.Count}个抖音Cookie", "抖音授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"加载本地Cookie异常：{ex.Message}", "抖音授权");
            }
        }

        /// <summary>
        /// 处理QR Connect扫码状态变化
        /// </summary>
        public void HandleQrStatusChanged(QrScanStatus status, string description = "")
        {
            try
            {
                if (loginSuccess) return;

                FileUtils.LogRpa($"【扫码状态】{description}", "抖音授权");
                OnQrStatusChanged?.Invoke(this, status);

                switch (status)
                {
                    case QrScanStatus.New:
                        FileUtils.LogRpa("二维码已加载，请打开抖音APP扫码", "抖音授权");
                        break;
                    case QrScanStatus.Scanned:
                        FileUtils.LogRpa("用户已扫码，请在抖音APP内点击「确认登录」", "抖音授权");
                        break;
                    case QrScanStatus.Confirmed:
                        FileUtils.LogRpa("==================== 抖音扫码登录成功！ ====================", "抖音授权");
                        _ = HandleLoginConfirmed();
                        break;
                    case QrScanStatus.Expired:
                        FileUtils.LogRpa("二维码已过期，请重新启动授权", "抖音授权");
                        break;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"处理扫码状态异常：{ex.Message}", "抖音授权");
            }
        }

        /// <summary>
        /// 扫码确认后的登录处理
        /// </summary>
        private async Task HandleLoginConfirmed()
        {
            try
            {
                FileUtils.LogRpa("[抖音授权] HandleLoginConfirmed 被调用", "抖音授权");
                if (loginSuccess) return;
                loginSuccess = true;

                // 在 UI 线程停止 Timer（System.Windows.Forms.Timer 必须在 UI 线程操作）
                if (_cookieCheckTimer != null)
                {
                    if (this.InvokeRequired)
                        this.BeginInvoke(new Action(() => { _cookieCheckTimer?.Stop(); _cookieCheckTimer?.Dispose(); _cookieCheckTimer = null; }));
                    else
                    { _cookieCheckTimer.Stop(); _cookieCheckTimer.Dispose(); _cookieCheckTimer = null; }
                }

                await SaveCurrentCookiesToLocal();
                NotifyLoginSuccess();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"处理登录确认异常：{ex.Message}", "抖音授权");
            }
        }

        private void NotifyLoginSuccess()
        {
            try
            {
                if (this.InvokeRequired)
                    this.BeginInvoke(new Action(() => { if (!_isFormClosed) this.Close(); }));
                else
                    if (!_isFormClosed) this.Close();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知登录成功异常：{ex.Message}", "抖音授权");
            }
        }

        private string GetCachePath()
        {
            try
            {
                string basePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "googleCache", "douyin-auth");
                if (!Directory.Exists(basePath))
                {
                    Directory.CreateDirectory(basePath);
                }

                string md5Str = MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}");
                string cachePath = Path.Combine(basePath, $"douyin-auth-{md5Str}");
                if (!Directory.Exists(cachePath))
                {
                    Directory.CreateDirectory(cachePath);
                }
                return cachePath;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取缓存路径异常：{ex.Message}", "抖音授权");
                return Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "googleCache");
            }
        }

        /// <summary>
        /// 显示二维码图片（由 get_qrcode 接口响应提供 Base64 图片数据）
        /// </summary>
        /// <param name="qrcodeBase64">data.qrcode 字段的 Base64 字符串</param>
        /// <param name="qrcodeIndexUrl">data.qrcode_index_url（仅用于日志）</param>
        public void ShowQrCode(string qrcodeBase64, string qrcodeIndexUrl = "")
        {
            if (loginSuccess) return;
            FileUtils.LogRpa($"[抖音授权] ShowQrCode 被调用，qrcodeBase64长度={qrcodeBase64?.Length ?? 0}，qrcodeIndexUrl={qrcodeIndexUrl}", "抖音授权");
            try
            {
                if (string.IsNullOrEmpty(qrcodeBase64))
                {
                    FileUtils.LogRpa("get_qrcode 响应中 qrcode 为空，无法显示二维码", "抖音授权");
                    return;
                }

                byte[] imageBytes = Convert.FromBase64String(qrcodeBase64);
                Bitmap qrBitmap;
                using (var ms = new MemoryStream(imageBytes))
                {
                    qrBitmap = new Bitmap(ms);
                }

                if (!string.IsNullOrEmpty(qrcodeIndexUrl))
                    FileUtils.LogRpa($"获取到抖音授权二维码，URL：{qrcodeIndexUrl}", "抖音授权");

                Action updateUi = () =>
                {
                    if (_qrPictureBox != null)
                    {
                        var oldImage = _qrPictureBox.Image;
                        _qrPictureBox.Image = qrBitmap;
                        oldImage?.Dispose();
                        FileUtils.LogRpa("[抖音授权] 二维码图片已设置到 PictureBox", "抖音授权");
                    }
                    else
                    {
                        FileUtils.LogRpa("[抖音授权] _qrPictureBox 为 null，无法显示二维码", "抖音授权");
                    }
                    if (_qrPanel != null && _qrPanel.Height == 0)
                    {
                        _qrPanel.Height = 260;
                    }
                };

                if (this.InvokeRequired)
                    this.BeginInvoke(updateUi);
                else
                    updateUi();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"显示二维码图片异常：{ex.Message}", "抖音授权");
            }
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            _isFormClosed = true;

            if (!loginSuccess)
            {
                DouyinAuthUtils.SetAuthFailed();
            }
            else
            {
                DouyinAuthUtils.authing = false;
            }

            if (_cookieCheckTimer != null)
            {
                _cookieCheckTimer.Stop();
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
    /// 抖音授权Cookie DTO
    /// </summary>
    public class DouyinAuthCookieDto
    {
        public string Name { get; set; }
        public string Value { get; set; }
        public string Domain { get; set; }
        public string Path { get; set; }
        public DateTime? Expires { get; set; }
        public bool Secure { get; set; }
        public bool HttpOnly { get; set; }
    }

    /// <summary>
    /// QR Connect扫码状态枚举
    /// </summary>
    public enum QrScanStatus
    {
        /// <summary>二维码已生成，等待扫码</summary>
        New = 0,
        /// <summary>用户已扫码，等待APP内确认</summary>
        Scanned = 1,
        /// <summary>登录成功</summary>
        Confirmed = 2,
        /// <summary>二维码已过期</summary>
        Expired = 3
    }

    /// <summary>
    /// CefSharp请求拦截器：监听抖音check_qrconnect扫码状态请求
    /// </summary>
    public class DouyinQrRequestHandler : IRequestHandler
    {
        private readonly DouyinAuthForm _authForm;

        public DouyinQrRequestHandler(DouyinAuthForm authForm)
        {
            _authForm = authForm;
        }

        public IResourceRequestHandler GetResourceRequestHandler(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool isNavigation, bool isDownload, string requestInitiator, ref bool disableDefaultHandling)
        {
            // 拦截抖音获取二维码请求（提取 qrcode_index_url 并展示图片）
            if (request.Url.Contains("passport/web/get_qrcode/"))
            {
                FileUtils.LogRpa($"[抖音授权] 拦截 get_qrcode 请求：{request.Url}", "抖音授权");
                return new DouyinGetQrCodeResourceRequestHandler(_authForm);
            }
            // 拦截抖音扫码状态检查请求
            if (request.Url.Contains("login.douyin.com/passport/web/check_qrconnect/"))
            {
                FileUtils.LogRpa($"[抖音授权] 拦截 check_qrconnect 请求：{request.Url}", "抖音授权");
                return new DouyinQrResourceRequestHandler(_authForm);
            }
            return null;
        }

        // 接口默认实现
        public bool OnBeforeBrowse(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool isNavigation, bool isDownload, string requestInitiator) => false;
        public bool OnOpenUrlFromTab(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, string targetUrl, WindowOpenDisposition targetDisposition, bool userGesture) => false;
        public bool OnCertificateError(IWebBrowser chromiumWebBrowser, IBrowser browser, CefErrorCode errorCode, string requestUrl, ISslInfo sslInfo, IRequestCallback callback) => false;
        public void OnPluginCrashed(IWebBrowser chromiumWebBrowser, IBrowser browser, string pluginPath) { }
        public void OnRenderProcessTerminated(IWebBrowser chromiumWebBrowser, IBrowser browser, CefTerminationStatus status) { }
        public bool OnQuotaRequest(IWebBrowser chromiumWebBrowser, IBrowser browser, string originUrl, long newSize, IRequestCallback callback) => false;
        public void OnProtocolExecution(IWebBrowser chromiumWebBrowser, IBrowser browser, string url, ref bool allowExecution) { }
        public bool OnBeforeBrowse(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool userGesture, bool isRedirect) => false;
        public void OnDocumentAvailableInMainFrame(IWebBrowser chromiumWebBrowser, IBrowser browser) { }
        public bool GetAuthCredentials(IWebBrowser chromiumWebBrowser, IBrowser browser, string originUrl, bool isProxy, string host, int port, string realm, string scheme, IAuthCallback callback) => false;
        public bool OnSelectClientCertificate(IWebBrowser chromiumWebBrowser, IBrowser browser, bool isProxy, string host, int port, System.Security.Cryptography.X509Certificates.X509Certificate2Collection certificates, ISelectClientCertificateCallback callback) => false;
        public void OnRenderViewReady(IWebBrowser chromiumWebBrowser, IBrowser browser) { }
        public void OnRenderProcessTerminated(IWebBrowser chromiumWebBrowser, IBrowser browser, CefTerminationStatus status, int errorCode, string errorMessage) { }
    }

    /// <summary>
    /// 资源响应处理器：读取check_qrconnect响应数据（使用IResponseFilter读取真实响应字节）
    /// </summary>
    public class DouyinQrResourceRequestHandler : IResourceRequestHandler
    {
        private readonly DouyinAuthForm _authForm;
        private DouyinResponseBodyFilter _responseFilter;

        public DouyinQrResourceRequestHandler(DouyinAuthForm authForm)
        {
            _authForm = authForm;
        }

        public bool OnResourceResponse(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response) => false;

        public IResourceHandler GetResourceHandler(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request) => null;

        public IResponseFilter GetResourceResponseFilter(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response)
        {
            _responseFilter = new DouyinResponseBodyFilter();
            return _responseFilter;
        }

        public void OnResourceLoadComplete(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response, UrlRequestStatus status, long receivedContentLength)
        {
            if (_responseFilter == null) return;
            try
            {
                byte[] data = _responseFilter.GetData();
                if (data.Length == 0) return;
                string jsonStr = Encoding.UTF8.GetString(data);
                FileUtils.LogRpa($"[抖音授权] check_qrconnect 响应：{jsonStr}", "抖音授权");
                ParseQrStatus(jsonStr);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"解析扫码响应异常：{ex.Message}", "抖音授权");
            }
        }

        /// <summary>
        /// 解析扫码状态JSON响应
        /// </summary>
        private void ParseQrStatus(string jsonStr)
        {
            try
            {
                JObject jsonData = JObject.Parse(jsonStr);
                JObject dataObj = jsonData["data"] as JObject ?? new JObject();
                string status = dataObj["status"]?.ToString() ?? string.Empty;
                int errorCode = dataObj["error_code"]?.ToObject<int>() ?? -1;
                string verifyDesc = dataObj["verify_scene_desc"]?.ToString() ?? string.Empty;

                if (errorCode == 0 && status == "new")
                {
                    _authForm.HandleQrStatusChanged(QrScanStatus.New, "二维码已加载完成，请打开抖音APP扫码");
                }
                else if (errorCode == 0 && status == "scanned")
                {
                    _authForm.HandleQrStatusChanged(QrScanStatus.Scanned, "用户已扫码，等待在抖音APP点击「确认登录」");
                }
                else if (errorCode == 0 && status == "confirmed")
                {
                    _authForm.HandleQrStatusChanged(QrScanStatus.Confirmed, "扫码登录成功！");
                }
                else if (errorCode == 0 && status == "expired")
                {
                    _authForm.HandleQrStatusChanged(QrScanStatus.Expired, "二维码已过期，请重新启动授权");
                }
                else if (errorCode != 0 && !string.IsNullOrEmpty(verifyDesc))
                {
                    FileUtils.LogRpa($"【扫码辅助提示】{verifyDesc}", "抖音授权");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"解析扫码状态JSON失败：{ex.Message}", "抖音授权");
            }
        }

        // 接口默认实现
        public CefReturnValue OnBeforeResourceLoad(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IRequestCallback callback) => CefReturnValue.Continue;
        public bool GetAuthCredentials(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, bool isProxy, string host, int port, string realm, string scheme, IAuthCallback callback) => false;
        public void OnResourceRedirect(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response, ref string newUrl) { }
        public ICookieAccessFilter GetCookieAccessFilter(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request) => null;
        public bool OnProtocolExecution(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request) => false;
        public void Dispose() { }
    }

    /// <summary>
    /// get_qrcode 接口响应处理器：提取 data.qrcode（Base64图片）并通知窗体显示
    /// </summary>
    public class DouyinGetQrCodeResourceRequestHandler : IResourceRequestHandler
    {
        private readonly DouyinAuthForm _authForm;
        private DouyinResponseBodyFilter _responseFilter;

        public DouyinGetQrCodeResourceRequestHandler(DouyinAuthForm authForm)
        {
            _authForm = authForm;
        }

        public bool OnResourceResponse(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response) => false;

        public IResourceHandler GetResourceHandler(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request) => null;

        public IResponseFilter GetResourceResponseFilter(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response)
        {
            _responseFilter = new DouyinResponseBodyFilter();
            return _responseFilter;
        }

        public void OnResourceLoadComplete(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response, UrlRequestStatus status, long receivedContentLength)
        {
            if (_responseFilter == null) return;
            try
            {
                byte[] data = _responseFilter.GetData();
                if (data.Length == 0) return;
                string jsonStr = Encoding.UTF8.GetString(data);
                JObject jo = JObject.Parse(jsonStr);
                JObject dataObj = jo["data"] as JObject;
                if (dataObj != null)
                {
                    string qrcodeBase64 = dataObj["qrcode"]?.ToString();
                    string qrcodeIndexUrl = dataObj["qrcode_index_url"]?.ToString();
                    _authForm.ShowQrCode(qrcodeBase64, qrcodeIndexUrl);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"解析 get_qrcode 响应异常：{ex.Message}", "抖音授权");
            }
        }

        // IResourceRequestHandler 接口默认实现
        public CefReturnValue OnBeforeResourceLoad(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IRequestCallback callback) => CefReturnValue.Continue;
        public bool GetAuthCredentials(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, bool isProxy, string host, int port, string realm, string scheme, IAuthCallback callback) => false;
        public void OnResourceRedirect(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response, ref string newUrl) { }
        public ICookieAccessFilter GetCookieAccessFilter(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request) => null;
        public bool OnProtocolExecution(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request) => false;
        public void Dispose() { }
    }

    /// <summary>
    /// CefSharp响应体过滤器：复制响应字节到内部缓冲区，同时原样透传给浏览器
    /// </summary>
    public class DouyinResponseBodyFilter : IResponseFilter
    {
        private readonly MemoryStream _buffer = new MemoryStream();

        public byte[] GetData() => _buffer.ToArray();

        public bool InitFilter() => true;

        public FilterStatus Filter(Stream dataIn, out long dataInRead, Stream dataOut, out long dataOutWritten)
        {
            if (dataIn == null || dataIn.Length == 0)
            {
                dataInRead = 0;
                dataOutWritten = 0;
                return FilterStatus.Done;
            }

            // 手动逐字节读取，避免 dataIn 不支持 Seek 导致 Position=0 抛异常
            var tempBuffer = new byte[dataIn.Length];
            int bytesRead = 0;
            int offset = 0;
            while (offset < tempBuffer.Length)
            {
                bytesRead = dataIn.Read(tempBuffer, offset, tempBuffer.Length - offset);
                if (bytesRead == 0) break;
                offset += bytesRead;
            }

            // 同时写入 dataOut（透传给浏览器）和内部缓冲区
            dataOut.Write(tempBuffer, 0, offset);
            _buffer.Write(tempBuffer, 0, offset);

            dataInRead = offset;
            dataOutWritten = offset;
            return FilterStatus.Done;
        }

        public void Dispose() => _buffer.Dispose();
    }

    /// <summary>
    /// 内存流资源处理器，用于读取CefSharp响应数据
    /// </summary>
    public class MemoryStreamResourceHandler : IResourceHandler
    {
        public MemoryStream MemoryStream { get; } = new MemoryStream();

        public bool Open(IRequest request, out bool handleRequest, ICallback callback)
        {
            handleRequest = true;
            callback.Continue();
            return true;
        }

        public bool ProcessRequest(IRequest request, ICallback callback)
        {
            callback.Continue();
            return true;
        }

        public void GetResponseHeaders(IResponse response, out long responseLength, out string redirectUrl)
        {
            responseLength = MemoryStream.Length;
            redirectUrl = null;
        }

        public bool Skip(long bytesToSkip, out long bytesSkipped, IResourceSkipCallback callback)
        {
            bytesSkipped = 0;
            return false;
        }

        public bool Read(Stream dataOut, out int bytesRead, IResourceReadCallback callback)
        {
            bytesRead = 0;
            return false;
        }

        public bool ReadResponse(Stream dataOut, out int bytesRead, ICallback callback)
        {
            bytesRead = 0;
            return false;
        }

        public bool CanGetCookie(Cookie cookie) => true;
        public bool CanSetCookie(Cookie cookie) => true;
        public void Cancel() { }
        public void Dispose() => MemoryStream.Dispose();
    }
}
