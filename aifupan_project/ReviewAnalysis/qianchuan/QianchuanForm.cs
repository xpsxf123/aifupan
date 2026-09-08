using CefSharp;
using CefSharp.WinForms;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web;
using System.Windows.Forms;
using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.juliang;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.qianchuan
{
    /// <summary>
    /// 千川登录窗体
    /// </summary>
    public partial class QianchuanForm : Form
    {
        #region 字段
        /// <summary>
        /// 浏览器实例
        /// </summary>
        private ChromiumWebBrowser browser;
        
        /// <summary>
        /// 主播信息
        /// </summary>
        public AnchorInfo anchorInfo { get; set; }
        
        /// <summary>
        /// 是否临时加载
        /// </summary>
        public bool isTemp { get; set; }
        
        /// <summary>
        /// 添加临时时间
        /// </summary>
        public long addTempTime { get; set; }
        
        /// <summary>
        /// 登录状态
        /// </summary>
        private QianchuanAuthStatusEnum _loginStatus = QianchuanAuthStatusEnum.unAuth;
        
        /// <summary>
        /// 登录成功标志
        /// </summary>
        private bool loginSuccess = false;
        
        /// <summary>
        /// 登录开始时间
        /// </summary>
        private DateTime _loginStartTime;
        
        /// <summary>
        /// 窗体是否已关闭
        /// </summary>
        private bool _isFormClosed = false;
        
        /// <summary>
        /// 检查Cookie的定时器
        /// </summary>
        private System.Windows.Forms.Timer _cookieCheckTimer;

        /// <summary>
        /// 登录成功后跳转到千川的账号选择页(只有一个页面就直接进入首页)
        /// </summary>
        private bool JumpHome = false;
        private bool _isPageLoading = false;
        
        /// <summary>
        /// 登录URL
        /// </summary>
        private string LoginUrl = "https://qianchuan.jinritemai.com/login";
        
        /// <summary>
        /// 登录成功后的跳转URL
        /// </summary>
        private string LoginSuccessUrl = "https://e.oceanengine.com/account/page/service/check_login?domain=sso.oceanengine.com&redirectUrl=https%3A%2F%2Fqianchuan.jinritemai.com%2Flogin&source=oe_nav";
        #endregion
        
        #region 构造函数
        /// <summary>
        /// 构造函数
        /// </summary>
        public QianchuanForm()
        {
            InitializeComponent();
        }
        
        /// <summary>
        /// 构造函数
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        public QianchuanForm(AnchorInfo anchorInfo)
        {
            this.anchorInfo = anchorInfo;
            InitializeComponent();
            Init();
        }
        #endregion
        
        #region 初始化方法
        /// <summary>
        /// 初始化
        /// </summary>
        public void Init()
        {
            Init(false);
        }
        
        /// <summary>
        /// 初始化
        /// </summary>
        /// <param name="bHide">是否隐藏窗体</param>
        public void Init(bool bHide)
        {
            try
            {
                // 设置窗体属性
                this.Text = "千川登录";
                this.Size = new Size(1000, 700);
                this.StartPosition = FormStartPosition.CenterScreen;
                
                // 设置置顶和对话框样式
                this.TopMost = true;
                this.FormBorderStyle = FormBorderStyle.Sizable;
                this.MaximizeBox = true;
                this.MinimizeBox = true;
                
                // 如果需要隐藏窗体
                if (bHide)
                {
                    this.WindowState = FormWindowState.Minimized;
                    this.ShowInTaskbar = false;
                }
                else
                {
                    // 确保窗体可见并激活
                    this.Visible = true;
                    this.Activate();
                    this.Focus();
                }
                
                // 初始化浏览器
                InitBrowser();
                
                // 打开登录页
                OpenLogin();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化千川窗体异常: {ex.Message}", "千川登录");
                // 通知前端登录失败
                NotifyLoginFailed($"初始化失败: {ex.Message}");
                // 重新抛出异常，让调用方处理
                throw;
            }
        }
        
        /// <summary>
        /// 初始化浏览器
        /// </summary>
        private void InitBrowser()
        {
            try
            {
                string url = LoginUrl;
                
                BrowserSettings browserSettings = new BrowserSettings();
                // 确保浏览器设置允许滚动条
                browserSettings.Javascript = CefState.Enabled;
                browserSettings.WebGl = CefState.Enabled;
                
                RequestContextSettings requestContextSettings = new RequestContextSettings();
                requestContextSettings.PersistSessionCookies = false;
                string cachePath = GetCachePath();
                requestContextSettings.CachePath = cachePath;

                // 重新授权时，清除浏览器缓存目录，避免旧缓存导致自动跳转
                if (!isTemp && anchorInfo != null && anchorInfo.qianchuanAuthStatus != (int)QianchuanAuthStatusEnum.auth)
                {
                    try
                    {
                        if (System.IO.Directory.Exists(cachePath))
                        {
                            System.IO.Directory.Delete(cachePath, true);
                            FileUtils.LogRpa($"已清除千川浏览器缓存目录：{cachePath}", "千川登录");
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"清除千川浏览器缓存目录异常: {ex.Message}", "千川登录");
                    }
                }
                
                // 异步创建浏览器实例，避免阻塞UI线程
                browser = new ChromiumWebBrowser(url)
                {
                    BrowserSettings = browserSettings,
                    RequestContext = new RequestContext(requestContextSettings),
                    Dock = DockStyle.Fill // 填充整个面板，随面板自动调整大小
                };
                
                // 设置生命周期处理器，用于处理新窗口请求
                browser.LifeSpanHandler = new QianchuanLifeSpanHandler(this);
                
                // 创建一个Panel作为浏览器的容器
                Panel browserPanel = new Panel
                {
                    Dock = DockStyle.Fill, // 填充整个窗体，随窗体自动调整大小
                    AutoScroll = true, // 启用自动滚动
                    Location = new System.Drawing.Point(0, 0)
                };
                
                // 浏览器已设置 Dock = DockStyle.Fill，无需手动设置大小和位置
                
                // 添加浏览器到Panel
                browserPanel.Controls.Add(browser);
                
                // 添加Panel到窗体
                this.Controls.Add(browserPanel);
                
                // 注册浏览器加载完成事件，确保页面内容正确显示
                browser.LoadingStateChanged += (sender, e) =>
                {
                    _isPageLoading = e.IsLoading;
                    if (e.IsLoading == false)
                    {
                        // 获取当前 URL
                        string currentUrl = browser.Address;
                        FileUtils.LogRpa($"页面加载完成: {currentUrl}", "千川登录");
                        
                        // 更新窗体标题显示当前URL
                        if (!string.IsNullOrEmpty(currentUrl))
                        {
                            if (this.InvokeRequired)
                            {
                                this.Invoke(new Action(() => this.Text = $"千川登录 - {currentUrl}"));
                            }
                            else
                            {
                                this.Text = $"千川登录 - {currentUrl}";
                            }
                        }
                        
                        // 只在登录页执行 CSS 注入，扫码成功后不再执行
                        if (currentUrl.Contains("login") && !currentUrl.Contains("is_new_connect") && !currentUrl.Contains("aavid"))
                        {
                            FileUtils.LogRpa("当前是登录页，执行 CSS 注入", "千川登录");
                            
                            // 浏览器加载完成后，执行JavaScript来确保页面内容正确显示和滚动
                            browser.ExecuteScriptAsync(@"document.body.style.overflow = 'auto';");
                            browser.ExecuteScriptAsync(@"document.documentElement.style.overflow = 'auto';");
                            
                            // 执行JavaScript来只显示登录区域，隐藏其他部分
                            browser.ExecuteScriptAsync(@"
                                // 尝试隐藏页面中除登录区域外的其他部分
                                // 隐藏顶部导航栏
                                var header = document.querySelector('header');
                                if (header) header.style.display = 'none';
                                
                                // 隐藏侧边栏
                                var sidebar = document.querySelector('aside, .sidebar, .side-nav');
                                if (sidebar) sidebar.style.display = 'none';
                                
                                // 隐藏页脚
                                var footer = document.querySelector('footer');
                                if (footer) footer.style.display = 'none';
                                
                                // 隐藏其他可能的非登录区域元素
                                var nonLoginElements = document.querySelectorAll('.main-content > *:not(.login-container), .container > *:not(.login-form), .page-content > *:not(.login-section)');
                                nonLoginElements.forEach(function(el) {
                                    el.style.display = 'none';
                                });
                                
                                // 查找登录表单容器并设置样式
                                var loginForm = document.querySelector('.login-form, .login-container, #login-form, .login-section');
                                if (loginForm) {
                                    loginForm.style.margin = '0';
                                    loginForm.style.maxWidth = '400px';
                                    loginForm.style.padding = '20px';
                                    loginForm.style.boxSizing = 'border-box';
                                    loginForm.style.position = 'relative';
                                    loginForm.style.top = '0';
                                    loginForm.style.left = '0';
                                }
                                
                                // 调整body样式
                                document.body.style.margin = '0';
                                document.body.style.padding = '0';
                                document.body.style.backgroundColor = '#f5f5f5';
                                document.body.style.position = 'relative';
                                document.body.style.top = '0';
                                document.body.style.left = '0';
                            ");
                            
                            // 调整浏览器大小以适应登录区域，使用BeginInvoke确保在UI线程中执行
                            if (this.InvokeRequired)
                            {
                                this.BeginInvoke(new Action(() =>
                                {
                                    this.Size = new Size(500, 600);
                                    browserPanel.Size = this.ClientSize;
                                    browser.Size = new System.Drawing.Size(browserPanel.ClientSize.Width, 800);
                                }));
                            }
                            else
                            {
                                this.Size = new Size(500, 600);
                                browserPanel.Size = this.ClientSize;
                                browser.Size = new System.Drawing.Size(browserPanel.ClientSize.Width, 800);
                            }
                        }
                        else
                        {
                            FileUtils.LogRpa("当前不是登录页，跳过 CSS 注入", "千川登录");
                        }
                    }
                };
                
                // 注册事件
                browser.FrameLoadEnd += Browser_FrameLoadEnd;
                browser.LoadError += ChromeBrowser_LoadError;
                
                // 初始化定时器：检查Cookie
                _cookieCheckTimer = new System.Windows.Forms.Timer
                {
                    Interval = 4000, // 4秒检查一次
                    Enabled = true
                };
                _cookieCheckTimer.Tick += CookieCheckTimer_Tick;
                
                // 加载千川本地 Cookie（使用千川独立的 Cookie 文件）
                LoadQianchuanCookies();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"初始化浏览器异常: {ex.Message}", "千川登录");
                throw;
            }
        }
        #endregion
        
        #region 事件处理
        /// <summary>
        /// 浏览器加载完成事件
        /// </summary>
        private async void Browser_FrameLoadEnd(object sender, FrameLoadEndEventArgs e)
        {
            try
            {
                if (e == null || string.IsNullOrEmpty(e.Url))
                    return;
                
                // 检查是否登录成功（跳转到千川登录后的账户选择页面）
                // 使用宽松匹配：URL包含is_new_connect参数即认为登录成功
                FileUtils.LogRpa($"页面加载: {e.Url}", "千川登录");
                if (e.Url.Contains("qianchuan.jinritemai.com") && e.Url.Contains("is_new_connect"))
                {
                    FileUtils.LogRpa("检测到扫码成功URL，进入账户选择页面", "千川登录");
                    // 扫码成功，但尚未选择账户，保持授权中状态
                    _loginStatus = QianchuanAuthStatusEnum.authing;
                    // authing 仍保持 true，等待账户选择完成后才设为 false
                    // 不通知前端授权成功，不同步服务端，等待账户选择完成
                    FileUtils.LogRpa("扫码成功，等待选择账户，状态保持授权中", "千川登录");
                    // 最大化窗体，以便更好地选择账号
                    if (this.InvokeRequired)
                    {
                        this.Invoke(new Action(() =>
                        {
                            this.WindowState = FormWindowState.Maximized;
                            this.PerformLayout(); // 强制刷新布局
                        }));
                    }
                    else
                    {
                        this.WindowState = FormWindowState.Maximized;
                        this.PerformLayout(); // 强制刷新布局
                    }
                    // 注意：这里不要关闭窗体，因为用户还需要选择账户
                    FileUtils.LogRpa("登录成功，等待用户选择账户，窗体已最大化", "千川登录");
                }
                // 检查是否选择了千川账户并跳转到home页面
                // 场景1：手动选择账号后跳转
                // 场景2：自动选择账号后跳转（from_qc_login=1）
                if (e.Url.Contains("qianchuan.jinritemai.com") && e.Url.Contains("aavid"))
                {
                    // 检查是否是自动选择账号（包含from_qc_login=1）或手动选择账号（/home页面）
                    bool isAutoSelect = e.Url.Contains("from_qc_login=1");
                    bool isManualSelect = e.Url.Contains("/home");
                    
                    if (isAutoSelect || isManualSelect)
                    {
                        // 保存Cookie
                        await SaveCurrentCookiesToLocal();
                        // 提取aavid参数
                        string aavid = ExtractAavidFromUrl(e.Url);
                        if (!string.IsNullOrEmpty(aavid))
                        {
                            // 将aavid传递到JuliangReachData
                            PassAavidToJuliangReachData(aavid);
                            string selectType = isAutoSelect ? "自动" : "手动";
                            FileUtils.LogRpa($"千川账户{selectType}选择成功，aavid: {aavid}，Cookie已保存，准备关闭窗体", "千川登录");

                            // 确保登录状态已标记为成功，避免关闭时被误判为取消授权
                            loginSuccess = true;
                            _loginStatus = QianchuanAuthStatusEnum.auth;
                            QianchuanUtils.authing = false;

                            // 关闭窗体
                            if (this.InvokeRequired)
                            {
                                this.Invoke(new Action(() => this.Close()));
                            }
                            else
                            {
                                this.Close();
                            }
                        }
                    }
                }
                // 检查是否仍在登录页（可能登录失败）
                else if (e.Url.Contains(LoginUrl))
                {
                    // 检查是否显示登录失败信息
                    bool isLoginFailed = await CheckLoginFailureAsync();
                    if (isLoginFailed)
                    {
                        LoginFailedHandle("登录失败，请检查账号密码");
                    }
                }
                // 检查手机号登录场景：从登录页跳转到千川主页（非登录页，无is_new_connect/aavid参数）
                // 例如：qianchuan.jinritemai.com/home、qianchuan.jinritemai.com/board-next 等
                else if (e.Url.Contains("qianchuan.jinritemai.com") && 
                         !e.Url.Contains("login") && 
                         !e.Url.Contains("is_new_connect") &&
                         !e.Url.Contains("aavid"))
                {
                    FileUtils.LogRpa($"检测到手机号登录成功，跳转到主页: {e.Url}", "千川登录");
                    // 保存Cookie
                    await SaveCurrentCookiesToLocal();
                    FileUtils.LogRpa("手机号登录成功，Cookie已保存", "千川登录");
                    
                    loginSuccess = true;
                    _loginStatus = QianchuanAuthStatusEnum.authing;
                    QianchuanUtils.authing = true;
                    FileUtils.LogRpa("手机号登录跳转到主页，状态保持授权中，等待确认账户", "千川登录");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"处理页面加载事件异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 检查页面是否显示登录失败信息
        /// </summary>
        private async Task<bool> CheckLoginFailureAsync()
        {
            try
            {
                // 检查浏览器是否准备好执行JavaScript
                if (browser == null || !browser.IsBrowserInitialized || !browser.CanExecuteJavascriptInMainFrame)
                {
                    return false;
                }
                
                // 使用GetMainFrame().EvaluateScriptAsync来执行JavaScript，绕过V8Context检查
                var mainFrame = browser.GetMainFrame();
                if (mainFrame == null)
                {
                    return false;
                }
                
                // 执行JavaScript检查登录失败元素
                var result = await mainFrame.EvaluateScriptAsync(@"
                    // 检查常见的登录失败提示元素
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
                FileUtils.LogRpa($"检查登录失败状态异常: {ex.Message}", "千川登录");
                return false;
            }
        }
        
        /// <summary>
        /// 定时器触发：检查Cookie和登录状态
        /// </summary>
        private async void CookieCheckTimer_Tick(object sender, EventArgs e)
        {
            try
            {
                // 检查窗体是否已关闭
                if (_isFormClosed)
                {
                    FileUtils.LogRpa("定时器触发，但窗体已关闭，停止执行", "千川登录");
                    return;
                }
                
                FileUtils.LogRpa("定时器触发，开始检查Cookie和登录状态", "千川登录");

                // 检查是否在巨量页面，如果是且巨量已登录则强制跳转到千川登录页
                string currentUrl = browser?.Address ?? "";
                // 这个地址是在登录页有的
                List<string> keys = new List<string>(){"/oauth", "/login", "/authorize", "/signin", "/account", "/passport"};
                if (!_isPageLoading && currentUrl.Contains("business.oceanengine.com") && !keys.Any(key => currentUrl.Contains(key)) && !JumpHome)
                {
                    FileUtils.LogRpa("检测到巨量已登录且在巨量页面，强制跳转到千川登录页", "千川登录");
                    OpenURL(LoginSuccessUrl);
                    JumpHome = true;
                }

                // 检查登录是否超时（5分钟）
                if (DateTime.Now - _loginStartTime > TimeSpan.FromMinutes(5))
                {
                    FileUtils.LogRpa("登录超时，处理登录失败", "千川登录");
                    LoginFailedHandle("登录超时，请重新登录");
                    return;
                }
                
                // 检查是否登录成功
                if (loginSuccess)
                {
                    FileUtils.LogRpa("登录成功，停止定时器", "千川登录");
                    // 登录成功后停止定时器
                    _cookieCheckTimer.Stop();
                    return;
                }
                
                // 检查Cookie
                //await CheckCookies();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检查Cookie异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 浏览器加载错误事件
        /// </summary>
        private void ChromeBrowser_LoadError(object sender, LoadErrorEventArgs e)
        {
            FileUtils.LogRpa($"浏览器加载错误: {e.ErrorText}，URL: {e.FailedUrl}", "千川登录");
        }
        #endregion
        
        #region 登录相关方法
        /// <summary>
        /// 打开登录页
        /// </summary>
        private void OpenLogin()
        {
            _loginStartTime = DateTime.Now;
            _loginStatus = QianchuanAuthStatusEnum.authing;
            // 设置全局授权状态为正在授权中
            QianchuanUtils.authing = true;
            OpenURL(LoginUrl);
        }
        
        /// <summary>
        /// 根据url加载页面
        /// </summary>
        /// <param name="url_str"></param>
        private void OpenURL(string url_str)
        {
            try
            {
                browser.Load(url_str);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"打开URL异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 登录失败处理
        /// </summary>
        /// <param name="errorMessage">错误信息</param>
        private void LoginFailedHandle(string errorMessage)
        {
            try
            {
                _loginStatus = QianchuanAuthStatusEnum.authFailed;
                // 设置全局授权状态为未授权
                QianchuanUtils.authing = false;
                // 同步千川授权状态到服务端
                if (anchorInfo != null)
                {
                    // 更新主播的千川授权状态
                    AnchorBll.UpdateQianchuanAuthStatus(anchorInfo, (int)QianchuanAuthStatusEnum.authFailed);
                    FileUtils.LogRpa("千川授权失败状态已同步到服务端", "千川登录");
                }
                // 通知前端登录失败
                NotifyLoginFailed(errorMessage);
                // 关闭窗体
                if (this.InvokeRequired)
                {
                    this.Invoke(new Action(() => this.Close()));
                }
                else
                {
                    this.Close();
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"登录失败处理异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 授权失效的处理
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="qianchuanStatus">授权状态</param>
        private void loginExpire(AnchorInfo anchorInfo, int qianchuanStatus)
        {
            if (anchorInfo.qianchuanAuthStatus != (int)QianchuanAuthStatusEnum.auth)
            {
                return;
            }
            // 修改主播的授权状态
            AnchorBll.UpdateQianchuanAuthStatus(anchorInfo, qianchuanStatus);
            // 重新设置资产
            // QianchuanUtils.recountQianchuanProperty();

            // 通知前端
            try
            {
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "qianchuanAuthExpires";
                requestDataObj.Add("data", JsonConvert.SerializeObject(anchorInfo));
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知前端异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 授权失效的处理
        /// </summary>
        /// <param name="qianchuanStatus">授权状态</param>
        private void loginExpire(int qianchuanStatus)
        {
            loginExpire(this.anchorInfo, qianchuanStatus);
        }
        
        /// <summary>
        /// 通知前端登录成功
        /// </summary>
        private void NotifyLoginSuccess()
        {
            try
            {
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "qianchuanAuthSuccess";
                requestDataObj["message"] = "登录成功";
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                
                FileUtils.LogRpa("登录成功，已通知前端", "千川登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知登录成功异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 通知前端登录失败
        /// </summary>
        /// <param name="errorMessage">错误信息</param>
        private void NotifyLoginFailed(string errorMessage = "登录失败，请检查账号密码或网络连接")
        {
            try
            {
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 401;
                requestDataObj["action"] = "qianchuanAuthFailed";
                requestDataObj["message"] = errorMessage;
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                
                FileUtils.LogRpa(errorMessage, "千川登录失败");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知登录失败异常: {ex.Message}", "千川登录失败");
            }
        }
        #endregion
        
        #region Cookie相关方法
        /// <summary>
        /// 获取缓存的路径地址
        /// </summary>
        /// <returns></returns>
        public string GetCachePath()
        {
            string cachePath = $"{Application.LocalUserAppDataPath}\\qianchuan\\cache";
            if (!System.IO.Directory.Exists(cachePath))
            {
                System.IO.Directory.CreateDirectory(cachePath);
            }
            return cachePath;
        }
        
        /// <summary>
        /// 获取Cookie的路径地址
        /// </summary>
        /// <returns></returns>
        public string GetCookiePath()
        {
            string cookiePath = $"{Application.LocalUserAppDataPath}\\qianchuan\\cookies";
            if (!System.IO.Directory.Exists(cookiePath))
            {
                System.IO.Directory.CreateDirectory(cookiePath);
            }
            return cookiePath;
        }
        
        /// <summary>
        /// 保存当前Cef中的Cookie到本地
        /// </summary>
        public async Task SaveCurrentCookiesToLocal()
        {
            if (browser == null || anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
                return;
            
            try
            {
                var cookieManager = browser.RequestContext.GetCookieManager(null);
                if (cookieManager == null)
                    return;
                
                // 从多个相关域名获取Cookie，确保获取所有必要的Cookie
                var domains = new List<string>
                {
                    "https://qianchuan.jinritemai.com",
                    "https://business.oceanengine.com",
                    "https://jinritemai.com",
                    "https://oceanengine.com"
                };
                
                var allCookies = new List<CefSharp.Cookie>();
                foreach (var domain in domains)
                {
                    try
                    {
                        var domainCookies = await cookieManager.VisitUrlCookiesAsync(
                            url: domain,
                            includeHttpOnly: true
                        );
                        if (domainCookies != null && domainCookies.Count > 0)
                        {
                            allCookies.AddRange(domainCookies);
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"从{domain}获取Cookie失败: {ex.Message}", "千川登录");
                    }
                }
                
                // 去重，避免重复的Cookie
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
                
                // 获取千川独立的 Cookie 文件路径（不再与巨量共用）
                string cookiePath = GetQianchuanCookiePath(anchorInfo.SecUid);
                if (string.IsNullOrEmpty(cookiePath))
                {
                    return;
                }
                
                // 确保目录存在
                string directory = System.IO.Path.GetDirectoryName(cookiePath);
                if (!System.IO.Directory.Exists(directory))
                {
                    System.IO.Directory.CreateDirectory(directory);
                    FileUtils.LogRpa($"创建千川 Cookie 存储目录：{directory}", "千川登录");
                }
                
                // 保存 Cookie 到本地（千川独立文件），使用不加密方式
                try
                {
                    // 读取现有 Cookie
                    List<CookieDto> existingDtos = new List<CookieDto>();
                    if (System.IO.File.Exists(cookiePath))
                    {
                        string existingContent = System.IO.File.ReadAllText(cookiePath);
                        if (!string.IsNullOrEmpty(existingContent))
                        {
                            existingDtos = Newtonsoft.Json.JsonConvert.DeserializeObject<List<CookieDto>>(existingContent) ?? new List<CookieDto>();
                        }
                    }
                    
                    // 转换新 Cookie 为 DTO
                    var newDtos = finalCookies.ConvertAll(CookieDto.FromCefCookie);
                    
                    // 合并 Cookie（更新现有，添加新的）
                    var mergedDtos = MergeCookies(existingDtos, newDtos);
                    
                    // 保存合并后的 Cookie
                    var jsonStr = Newtonsoft.Json.JsonConvert.SerializeObject(mergedDtos, Newtonsoft.Json.Formatting.Indented);
                    System.IO.File.WriteAllText(cookiePath, jsonStr, System.Text.Encoding.UTF8);
                    FileUtils.LogRpa($"千川登录 Cookie 已保存到本地（独立文件）：{cookiePath}", "千川登录");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"直接保存 Cookie 失败：{ex.Message}，使用 CookiePersistenceHelper 保存", "千川登录");
                    // 如果直接保存失败，使用 CookiePersistenceHelper 保存
                    new CookiePersistenceHelper(cookiePath).SaveCefCookiesToLocal(finalCookies, false);
                }
                
                // 注意：千川和巨量 Cookie 已分离，不再更新巨量 Cookie
                // 如需同步，请单独调用巨量授权流程
                // await UpdateJuliangCookies(finalCookies);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存 Cookie 异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 加载千川本地 Cookie 到浏览器（使用千川独立的 Cookie 文件）
        /// </summary>
        private async void LoadQianchuanCookies()
        {
            try
            {
                if (browser == null || anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
                {
                    return;
                }
                
                // 获取千川独立的 Cookie 文件路径
                string cookiePath = GetQianchuanCookiePath(anchorInfo.SecUid);
                if (string.IsNullOrEmpty(cookiePath))
                {
                    return;
                }
                
                // 检查文件是否存在
                if (!System.IO.File.Exists(cookiePath))
                {
                    FileUtils.LogRpa($"千川 Cookie 文件不存在，跳过加载：{cookiePath}", "千川登录");
                    return;
                }
                
                // 读取 Cookie（千川独立文件）
                var cookies = new CookiePersistenceHelper(cookiePath).LoadCefCookiesFromLocal(false);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"千川 Cookie 文件为空，无需加载：{cookiePath}", "千川登录");
                    return;
                }
                
                // 获取千川浏览器的 Cookie 管理器
                var cookieManager = browser.RequestContext.GetCookieManager(null);
                if (cookieManager == null)
                {
                    return;
                }
                
                // 遍历 Cookie，更新到千川浏览器
                int loadedCount = 0;
                foreach (var cookie in cookies)
                {
                    // 添加到千川浏览器的 Cookie 中
                    await cookieManager.SetCookieAsync(
                        url: "https://qianchuan.jinritemai.com",
                        cookie: cookie
                    );
                    loadedCount++;
                }
                
                FileUtils.LogRpa($"已从千川 Cookie 文件加载 {loadedCount} 个 Cookie 到浏览器：{cookiePath}", "千川登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从千川 Cookie 文件加载到浏览器异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 获取千川独立的 Cookie 文件路径（不与巨量共用）
        /// </summary>
        /// <param name="secUid">主播 SecUid</param>
        /// <returns>Cookie 路径</returns>
        private string GetQianchuanCookiePath(string secUid)
        {
            try
            {
                string cachePath = System.IO.Path.GetFullPath("dataCollect/config");
                string md5Str = GetCachePathMd5(secUid);
                return System.IO.Path.Combine(cachePath, $"qianchuan-{md5Str}");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川 Cookie 路径异常: {ex.Message}", "千川登录");
                return null;
            }
        }
        
        /// <summary>
        /// 获取 juliang 文件夹下的 Cookie 路径（巨量使用）
        /// </summary>
        /// <param name="secUid">主播 SecUid</param>
        /// <returns>Cookie 路径</returns>
        private string GetJuliangCookiePath(string secUid)
        {
            try
            {
                string cachePath = System.IO.Path.GetFullPath("dataCollect/config");
                string md5Str = GetCachePathMd5(secUid);
                return System.IO.Path.Combine(cachePath, $"jlby-{md5Str}");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取 juliang Cookie 路径异常: {ex.Message}", "千川登录");
                return null;
            }
        }
        
        /// <summary>
        /// 更新巨量Cookie的COMPASS_LUOPAN_DT和sessionid
        /// </summary>
        /// <param name="qianchuanCookies">千川登录获取的Cookie</param>
        /// <returns></returns>
        private async Task UpdateJuliangCookies(IEnumerable<CefSharp.Cookie> qianchuanCookies)
        {
            try
            {
                if (anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
                {
                    return;
                }
                
                // 获取巨量的Cookie路径（使用与千川相同的文件存储）
                string juliangCookiePath = GetJuliangCookiePath(anchorInfo.SecUid);
                if (string.IsNullOrEmpty(juliangCookiePath))
                {
                    return;
                }
                
                // 确保目录存在
                string directory = System.IO.Path.GetDirectoryName(juliangCookiePath);
                if (!System.IO.Directory.Exists(directory))
                {
                    System.IO.Directory.CreateDirectory(directory);
                    FileUtils.LogRpa($"创建Cookie存储目录：{directory}", "千川登录");
                }
                
                // 直接保存千川的Cookie到巨量的Cookie文件，不使用加密，便于查看
                try
                {
                    var finalCookies = qianchuanCookies.ToList();
                    var dtos = finalCookies.ConvertAll(CookieDto.FromCefCookie);
                    var jsonStr = Newtonsoft.Json.JsonConvert.SerializeObject(dtos, Newtonsoft.Json.Formatting.Indented);
                    System.IO.File.WriteAllText(juliangCookiePath, jsonStr, System.Text.Encoding.UTF8);
                    FileUtils.LogRpa($"巨量Cookie已更新（与千川使用同一文件存储）：{juliangCookiePath}", "千川登录");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"直接保存巨量Cookie失败：{ex.Message}，使用CookiePersistenceHelper保存", "千川登录");
                    // 如果直接保存失败，使用CookiePersistenceHelper保存
                    new CookiePersistenceHelper(juliangCookiePath).SaveCefCookiesToLocal(qianchuanCookies.ToList(), false);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"更新巨量Cookie异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 合并 Cookie 列表（更新现有，添加新的）
        /// </summary>
        /// <param name="existingDtos">现有Cookie</param>
        /// <param name="newDtos">新Cookie</param>
        /// <returns>合并后的Cookie列表</returns>
        private List<CookieDto> MergeCookies(List<CookieDto> existingDtos, List<CookieDto> newDtos)
        {
            // 使用字典来存储Cookie，键为Name+Domain，确保唯一性
            var cookieDict = new Dictionary<string, CookieDto>();
            
            // 添加现有Cookie
            foreach (var existingDto in existingDtos)
            {
                var key = existingDto.Name + existingDto.Domain;
                cookieDict[key] = existingDto;
            }
            
            // 添加或更新新Cookie
            foreach (var newDto in newDtos)
            {
                var key = newDto.Name + newDto.Domain;
                cookieDict[key] = newDto; // 覆盖现有Cookie
            }
            
            return cookieDict.Values.ToList();
        }
        #endregion
        
        #region aavid相关方法
        /// <summary>
        /// 从 URL 中提取 aavid 参数
        /// </summary>
        /// <param name="url">URL字符串</param>
        /// <returns>aavid参数值</returns>
        internal string ExtractAavidFromUrl(string url)
        {
            try
            {
                Uri uri = new Uri(url);
                string query = uri.Query;
                var queryParams = System.Web.HttpUtility.ParseQueryString(query);
                return queryParams["aavid"];
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"提取aavid异常: {ex.Message}", "千川登录");
                return null;
            }
        }
        
        /// <summary>
        /// 将aavid传递到JuliangReachData
        /// </summary>
        /// <param name="aavid">千川账户ID</param>
        internal void PassAavidToJuliangReachData(string aavid)
        {
            try
            {
                if (anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
                {
                    return;
                }
                
                // 获取aavid文件路径（与巨量Cookie保存在同一文件夹中）
                string aavidPath = GetAavidPath(anchorInfo.SecUid);
                if (string.IsNullOrEmpty(aavidPath))
                {
                    return;
                }
                
                // 保存aavid值到文件
                System.IO.File.WriteAllText(aavidPath, aavid);
                
                FileUtils.LogRpa($"aavid已保存: {aavid} 对应主播: {anchorInfo.SecUid} 保存路径: {aavidPath}", "千川登录");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"传递aavid到JuliangReachData异常: {ex.Message}", "千川登录");
            }
        }
        
        /// <summary>
        /// 获取aavid文件路径
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>aavid文件路径</returns>
        private string GetAavidPath(string secUid)
        {
            try
            {
                string cachePath = System.IO.Path.GetFullPath("dataCollect/config");
                string md5Str = GetCachePathMd5(secUid);
                return System.IO.Path.Combine(cachePath, $"qcaavid-{md5Str}");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取aavid文件路径异常: {ex.Message}", "千川登录");
                return null;
            }
        }
        #endregion
        
        #region 辅助方法
        /// <summary>
        /// 获取缓存路径的MD5值（与 QianchuanUtils 保持一致）
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>MD5值</returns>
        private string GetCachePathMd5(string secUid)
        {
            try
            {
                // 使用与 QianchuanUtils 相同的 MD5 计算方式
                return QianchuanUtils.getCachePathMd5(secUid);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取缓存路径MD5异常: {ex.Message}", "千川登录");
                return "default";
            }
        }
        
        /// <summary>
        /// 窗体加载事件
        /// </summary>
        private void QianchuanForm_Load(object sender, EventArgs e)
        {
            // 初始化登录功能
            // 注意：不要在这里调用Init()，因为Init()会调用InitializeComponent()，会导致无限递归
        }
        
        /// <summary>
        /// 窗体关闭时释放资源
        /// </summary>
        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            base.OnFormClosing(e);
            
            FileUtils.LogRpa("千川登录窗体开始关闭，准备停止定时器", "千川登录");
            
            // 设置窗体已关闭标志
            _isFormClosed = true;
            
            // 从缓存中移除窗体
            if (anchorInfo != null && !string.IsNullOrEmpty(anchorInfo.SecUid))
            {
                QianchuanUtils.qianchuanFormList.TryRemove(anchorInfo.SecUid, out _);
                FileUtils.LogRpa($"已从缓存中移除窗体: {anchorInfo.SecUid}", "千川登录");
            }
            
            // 停止定时器
            if (_cookieCheckTimer != null)
            {
                try
                {
                    _cookieCheckTimer.Stop();
                    _cookieCheckTimer.Dispose();
                    FileUtils.LogRpa("定时器已成功停止并释放", "千川登录");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"停止定时器时发生异常: {ex.Message}", "千川登录");
                }
            }
            else
            {
                FileUtils.LogRpa("定时器为null，无需停止", "千川登录");
            }
            
            // 释放浏览器资源
            if (browser != null)
            {
                try
                {
                    browser.Dispose();
                    FileUtils.LogRpa("浏览器资源已成功释放", "千川登录");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"释放浏览器资源时发生异常: {ex.Message}", "千川登录");
                }
            }
            
            // 重置全局授权状态，允许下次授权
            QianchuanUtils.authing = false;
            
            // 如果授权未完成（仍是authing），且不是登录成功状态，才恢复为未授权状态
            if (_loginStatus == QianchuanAuthStatusEnum.authing && !loginSuccess)
            {
                _loginStatus = QianchuanAuthStatusEnum.unAuth;

                if (anchorInfo != null)
                {
                    // 更新主播授权状态
                    AnchorBll.UpdateQianchuanAuthStatus(anchorInfo, (int)QianchuanAuthStatusEnum.unAuth);

                    // 通知前端授权已取消
                    try
                    {
                        FrontNotice frontNotice = new FrontNotice();
                        var requestDataObj = new Dictionary<string, object>();
                        requestDataObj["code"] = 0;
                        requestDataObj["status"] = 200;
                        requestDataObj["action"] = "qianchuanAuthCancelled";
                        requestDataObj["message"] = "授权已取消";
                        frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                        FileUtils.LogRpa("已通知前端授权已取消", "千川登录");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"通知前端授权取消异常: {ex.Message}", "千川登录");
                    }
                }

                FileUtils.LogRpa("授权窗口关闭，恢复授权状态为未授权", "千川登录");
            }
            // 授权成功时，更新状态为已授权，同步服务器，通知前端
            else if (loginSuccess && anchorInfo != null)
            {
                AnchorBll.UpdateQianchuanAuthStatus(anchorInfo, (int)QianchuanAuthStatusEnum.auth);

                // 通知前端授权成功
                try
                {
                    FrontNotice frontNotice = new FrontNotice();
                    var requestDataObj = new Dictionary<string, object>();
                    requestDataObj["code"] = 0;
                    requestDataObj["status"] = 200;
                    requestDataObj["action"] = "qianchuanAuthSuccess";
                    requestDataObj["message"] = "授权成功";
                    requestDataObj.Add("data", JsonConvert.SerializeObject(anchorInfo));
                    frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                    FileUtils.LogRpa("已通知前端千川授权成功", "千川登录");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"通知前端授权成功异常: {ex.Message}", "千川登录");
                }

                // 重新计算千川资产
                // QianchuanUtils.recountQianchuanProperty();

                FileUtils.LogRpa("授权成功，状态已更新为已授权", "千川登录");
            }
            
            FileUtils.LogRpa("千川登录窗体关闭完成", "千川登录");
        }
        
        /// <summary>
        /// 关闭窗体
        /// </summary>
        public void closeForm()
        {
            try
            {
                this.Close();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"关闭千川窗体异常: {ex.Message}", "千川登录");
            }
        }
        #endregion
    }
    
    /// <summary>
    /// 千川授权状态枚举
    /// </summary>
    public enum QianchuanAuthStatusEnum
    {
        unAuth = 0,         // 未授权
        auth = 1,           // 已登录
        authExpires = 2,    // 登录过期
        authFailed = 3,     // 登录失败
        authing = 4,        // 登录中
        accountMismatched = 5, // 授权抖音号不匹配
        subNoPermission = 6     // 子账号无权限
    }
    
    /// <summary>
    /// 生命周期处理器，用于处理新窗口请求
    /// </summary>
    public class QianchuanLifeSpanHandler : ILifeSpanHandler
    {
        private readonly QianchuanForm _form;
        
        public QianchuanLifeSpanHandler(QianchuanForm form)
        {
            _form = form;
        }
        
        /// <summary>
        /// 处理新窗口请求
        /// </summary>
        public bool OnBeforePopup(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, string targetUrl, string targetFrameName, WindowOpenDisposition targetDisposition, bool userGesture, IPopupFeatures popupFeatures, IWindowInfo windowInfo, IBrowserSettings browserSettings, ref bool noJavascriptAccess, out IWebBrowser newBrowser)
        {
            try
            {
                FileUtils.LogRpa($"OnBeforePopup触发，目标URL: {targetUrl}", "千川登录");
                
                // 检查新窗口URL是否包含aavid
                if (!string.IsNullOrEmpty(targetUrl) && targetUrl.Contains("aavid"))
                {
                    // 提取aavid
                    string aavid = _form.ExtractAavidFromUrl(targetUrl);
                    if (!string.IsNullOrEmpty(aavid))
                    {
                        // 保存aavid
                        _form.PassAavidToJuliangReachData(aavid);
                        FileUtils.LogRpa($"从新窗口提取aavid: {aavid}", "千川登录");
                        
                        // 保存Cookie
                        _form.SaveCurrentCookiesToLocal().Wait();
                        
                        // 关闭原窗体
                        if (_form.InvokeRequired)
                        {
                            _form.Invoke(new Action(() => _form.Close()));
                        }
                        else
                        {
                            _form.Close();
                        }
                    }
                    // 取消默认的新窗口创建
                    newBrowser = null;
                    return true;
                }
                
                // 对于其他链接，在当前浏览器中打开
                if (!string.IsNullOrEmpty(targetUrl))
                {
                    // 使用传入的 chromiumWebBrowser 参数加载 URL
                    chromiumWebBrowser.Load(targetUrl);
                    FileUtils.LogRpa($"在当前页面打开链接: {targetUrl}", "千川登录");
                }
                
                // 取消默认的新窗口创建
                newBrowser = null;
                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"处理新窗口请求异常: {ex.Message}", "千川登录");
                newBrowser = null;
                return true;
            }
        }
        
        /// <summary>
        /// 浏览器创建完成
        /// </summary>
        public void OnAfterCreated(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
            // 不需要处理
        }
        
        /// <summary>
        /// 浏览器关闭前
        /// </summary>
        public bool DoClose(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
            return false;
        }
        
        /// <summary>
        /// 浏览器关闭完成
        /// </summary>
        public void OnBeforeClose(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
            // 不需要处理
        }
    }
}