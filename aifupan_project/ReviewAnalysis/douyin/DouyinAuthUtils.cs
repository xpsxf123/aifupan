using douyin.Utils;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using System;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis.DouyinAuth
{
    /// <summary>
    /// 抖音授权工具类
    /// 抖音授权不绑定特定主播，Cookie路径使用 MD5(租户ID-用户ID) 标识
    /// </summary>
    public class DouyinAuthUtils
    {
        public static volatile bool authing;
        private static volatile bool _lastAuthFailed;

        /// <summary>
        /// 抖音授权状态变更事件
        /// </summary>
        public static event EventHandler<DouyinAuthStatusChangedEventArgs> OnAuthStatusChanged;

        private static int _lastAuthStatus = -1;
        private static System.Threading.Timer _statusCheckTimer;
        private static readonly object _monitorLock = new object();

        /// <summary>
        /// 打开抖音授权窗体
        /// </summary>
        public static void openAuthorizeOrDataScreen(bool bHide = false)
        {
            _lastAuthFailed = false;
            _ = Task.Run(() =>
            {
                try
                {
                    Form mainForm = Form.ActiveForm;
                    if (mainForm != null && mainForm.InvokeRequired)
                    {
                        mainForm.BeginInvoke(new Action(() =>
                        {
                            CreateAndInitDouyinAuthForm(bHide);
                        }));
                    }
                    else
                    {
                        Thread uiThread = new Thread(() =>
                        {
                            try
                            {
                                DouyinAuthForm authForm = new DouyinAuthForm();
                                authForm.Init(bHide);
                                Application.Run(authForm);
                            }
                            catch (Exception ex)
                            {
                                FileUtils.LogRpa($"抖音授权STA线程异常: {ex.Message}\n{ex.StackTrace}", "抖音授权");
                                SetAuthFailed();
                            }
                        });
                        uiThread.SetApartmentState(ApartmentState.STA);
                        uiThread.Start();
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"打开抖音授权窗体异常: {ex.Message}\n{ex.StackTrace}", "抖音授权");
                    SetAuthFailed();
                }
            }).ConfigureAwait(false);
        }

        /// <summary>
        /// 创建并初始化抖音授权窗体
        /// </summary>
        private static void CreateAndInitDouyinAuthForm(bool bHide)
        {
            try
            {
                DouyinAuthForm authForm = new DouyinAuthForm();
                authForm.Init(bHide);
                authForm.Show();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"创建抖音授权窗体异常: {ex.Message}", "抖音授权");
                SetAuthFailed();
            }
        }

        /// <summary>
        /// 获取Cookie文件路径（使用 MD5(租户ID-用户ID) 标识，不绑定secUid）
        /// </summary>
        public static string getCookiePath()
        {
            string cachePath = Path.GetFullPath("dataCollect\\config");
            string md5Str = MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}");
            return Path.Combine(cachePath, $"douyin-{md5Str}");
        }

        /// <summary>
        /// 检查Cookie是否有效
        /// </summary>
        public static bool CheckCookieValid()
        {
            return GetAuthStatus() == 1;
        }

        /// <summary>
        /// 获取抖音授权状态（综合：含授权过程状态）
        /// </summary>
        /// <returns>0=未授权, 1=已授权, 2=授权过期, 3=授权失败, 4=授权中</returns>
        public static int GetAuthStatus()
        {
            if (authing)
                return 4; // 授权中

            if (_lastAuthFailed)
                return 3; // 授权失败

            try
            {
                string cookiePath = getCookiePath();

                if (!File.Exists(cookiePath))
                    return 0; // 未授权

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = Newtonsoft.Json.JsonConvert.DeserializeObject<System.Collections.Generic.List<DouyinAuthCookieDto>>(cookieContent);

                if (cookies == null || cookies.Count == 0)
                    return 0;

                var sessionCookie = cookies.FirstOrDefault(c =>
                    c.Name.Contains("sessionid"));

                if (sessionCookie == null || string.IsNullOrWhiteSpace(sessionCookie.Value))
                    return 0;

                if (sessionCookie.Expires.HasValue && sessionCookie.Expires.Value < DateTime.Now)
                    return 2; // 授权过期

                return 1; // 已授权
            }
            catch
            {
                return 0; // 异常按未授权处理
            }
        }

        /// <summary>
        /// 设置授权失败标记
        /// </summary>
        public static void SetAuthFailed()
        {
            _lastAuthFailed = true;
            authing = false;
            FileUtils.LogRpa("抖音授权已标记为失败", "抖音授权");
        }

        /// <summary>
        /// 启动授权状态监听器（30秒间隔检测Cookie状态变化）
        /// </summary>
        public static void StartStatusMonitor()
        {
            lock (_monitorLock)
            {
                if (_statusCheckTimer != null) return;

                _lastAuthStatus = GetAuthStatus();
                FileUtils.LogRpa($"抖音授权状态监听器已启动，当前状态：{_lastAuthStatus}", "抖音授权");

                _statusCheckTimer = new System.Threading.Timer(StatusCheckCallback, null, TimeSpan.FromSeconds(30), TimeSpan.FromSeconds(30));
            }
        }

        /// <summary>
        /// 停止授权状态监听器
        /// </summary>
        public static void StopStatusMonitor()
        {
            lock (_monitorLock)
            {
                if (_statusCheckTimer != null)
                {
                    _statusCheckTimer.Dispose();
                    _statusCheckTimer = null;
                    FileUtils.LogRpa("抖音授权状态监听器已停止", "抖音授权");
                }
            }
        }

        /// <summary>
        /// 状态检测回调
        /// </summary>
        private static void StatusCheckCallback(object state)
        {
            try
            {
                int currentStatus = GetAuthStatus();
                if (currentStatus != _lastAuthStatus)
                {
                    int oldStatus = _lastAuthStatus;
                    _lastAuthStatus = currentStatus;

                    FileUtils.LogRpa($"抖音授权状态变更：{oldStatus} -> {currentStatus}", "抖音授权");

                    OnAuthStatusChanged?.Invoke(null, new DouyinAuthStatusChangedEventArgs
                    {
                        OldStatus = oldStatus,
                        NewStatus = currentStatus,
                        ChangeTime = DateTime.Now
                    });
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"抖音授权状态检测异常：{ex.Message}", "抖音授权");
            }
        }
    }

    /// <summary>
    /// 抖音授权状态变更事件参数
    /// </summary>
    public class DouyinAuthStatusChangedEventArgs : EventArgs
    {
        /// <summary>
        /// 变更前状态 0=未授权 1=已授权 2=授权过期
        /// </summary>
        public int OldStatus { get; set; }

        /// <summary>
        /// 变更后状态 0=未授权 1=已授权 2=授权过期
        /// </summary>
        public int NewStatus { get; set; }

        /// <summary>
        /// 变更时间
        /// </summary>
        public DateTime ChangeTime { get; set; }
    }
}
