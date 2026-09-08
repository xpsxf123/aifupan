using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.system;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.DouyinAuth;

namespace ReviewAnalysis.Utils
{
    /// <summary>
    /// 系统状态轮询定时器
    /// 登录成功后启动，每30秒执行一次：网络状态检测 + 抖音授权状态查询
    /// </summary>
    public static class SystemStatusTimer
    {
        // ── 定时器 ──────────────────────────────────────────────────────────
        private static Timer _timer;
        private static readonly object _timerLock = new object();

        // ── 网络检测配置 ─────────────────────────────────────────────────────
        private const int NETWORK_GOOD_THRESHOLD      = 300;   // ≤ 300ms  → good
        private const int NETWORK_EXHAUSTED_THRESHOLD = 1000;  // ≤ 1000ms → exhausted，否则 poor
        private const int NETWORK_REQUEST_TIMEOUT_MS  = 5000;  // HTTP 请求超时

        // ── 网络检测 URL 池 ──────────────────────────────────────────────────
        private static readonly List<string> _networkUrlPool = new List<string>();
        private static readonly object _poolLock = new object();

        // ── 定时器间隔 ───────────────────────────────────────────────────────
        private static int TIMER_INTERVAL_MS = 30; // 30秒

        /// <summary>
        /// 启动定时器（登录成功后调用，多次调用安全：先停旧再启新）
        /// </summary>
        public static void Start()
        {
            lock (_timerLock)
            {
                // 先停旧定时器
                _timer?.Dispose();
                _timer = null;

                // 清空 URL 池，避免跨账号污染
                lock (_poolLock)
                {
                    _networkUrlPool.Clear();
                }
                
                // 监听抖音授权状态变化
                DouyinAuthUtils.OnAuthStatusChanged += (s, e) => {
                    CheckDouyinAuthStatus();
                };
                TIMER_INTERVAL_MS = KvHelper.GetIntKvByKey("network_and_authorization_detection_time", 30);
                // dueTime=0 表示立即执行一次，period=30s 表示之后每30秒执行
                _timer = new Timer(OnTick, null, 0, TIMER_INTERVAL_MS * 1000);
                FileUtils.log("SystemStatusTimer 已启动");
            }
        }

        /// <summary>
        /// 停止定时器
        /// </summary>
        public static void Stop()
        {
            lock (_timerLock)
            {
                _timer?.Dispose();
                _timer = null;
                FileUtils.log("SystemStatusTimer 已停止");
            }
        }

        // ────────────────────────────────────────────────────────────────────
        // 定时器回调
        // ────────────────────────────────────────────────────────────────────
        private static void OnTick(object state)
        {
            // 网络检测与授权查询互相隔离，任一异常不影响另一个
            CheckNetworkStatus();

            CheckDouyinAuthStatus();
        }

        private static void CheckNetworkStatus()
        {
            try
            {
                CheckNetworkStatusAsync().Wait();
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"网络状态检测异常: {ex.Message}", "SystemStatusTimer");
            }
        }
        
        private static void CheckDouyinAuthStatus()
        {
            try
            {
                CheckDouyinAuthStatusAsync().Wait();
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"抖音授权状态查询异常: {ex.Message}", "SystemStatusTimer");
            }
        }

        // ────────────────────────────────────────────────────────────────────
        // 任务一：网络状态检测
        // ────────────────────────────────────────────────────────────────────
        private static async Task CheckNetworkStatusAsync()
        {
            string url = GetNextNetworkCheckUrl();
            if (string.IsNullOrEmpty(url))
            {
                // URL 池和接口都没有可用地址，跳过本次检测
                FileUtils.log("网络检测URL池为空且接口无返回，跳过本次网络检测", "SystemStatusTimer");
                return;
            }

            string status = await MeasureNetworkStatusAsync(url);
            await NotifyNetworkStatus(status);
        }

        /// <summary>
        /// 从 URL 池获取下一条检测地址，池空时先调用接口补充
        /// </summary>
        private static string GetNextNetworkCheckUrl()
        {
            lock (_poolLock)
            {
                if (_networkUrlPool.Count > 0)
                {
                    string url = _networkUrlPool[0];
                    _networkUrlPool.RemoveAt(0);
                    return url;
                }
            }

            // 池已空，调用接口补充
            try
            {
                List<DictDataListVo> list = SystemApi.listNetworkCheckUrls();
                if (list != null && list.Count > 0)
                {
                    lock (_poolLock)
                    {
                        foreach (var item in list)
                        {
                            if (!string.IsNullOrEmpty(item.value))
                            {
                                _networkUrlPool.Add(item.value);
                            }
                        }

                        if (_networkUrlPool.Count > 0)
                        {
                            string url = _networkUrlPool[0];
                            _networkUrlPool.RemoveAt(0);
                            return url;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"获取网络检测URL列表异常: {ex.Message}", "SystemStatusTimer");
            }

            return null;
        }

        /// <summary>
        /// 对指定 URL 发起 HTTP GET，测量延迟并返回状态字符串
        /// </summary>
        private static async Task<string> MeasureNetworkStatusAsync(string url)
        {
            try
            {
                using (HttpClient client = new HttpClient())
                {
                    client.Timeout = TimeSpan.FromMilliseconds(NETWORK_REQUEST_TIMEOUT_MS);

                    Stopwatch sw = Stopwatch.StartNew();
                    await client.GetAsync(url);
                    sw.Stop();

                    long elapsed = sw.ElapsedMilliseconds;

                    if (elapsed <= NETWORK_GOOD_THRESHOLD)
                        return "good";
                    else if (elapsed <= NETWORK_EXHAUSTED_THRESHOLD)
                        return "exhausted";
                    else
                        return "poor";
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"网络检测请求异常（视为poor）: {ex.Message}", "SystemStatusTimer");
                return "poor";
            }
        }

        /// <summary>
        /// 通知前端网络状态
        /// </summary>
        private static async Task NotifyNetworkStatus(string status)
        {
            await FrontNotice.NoticeFront("networkStatus", new { status = status });
        }

        // ────────────────────────────────────────────────────────────────────
        // 任务二：抖音授权状态查询
        // ────────────────────────────────────────────────────────────────────
        private static async Task CheckDouyinAuthStatusAsync()
        {
            // 替换为同事实际的方法调用，返回值含义：
            //   0 - 未授权   → wait
            //   1 - 已授权   → success
            //   2 - 授权过期 → wait
            //   3 - 授权失败 → fail
            //   4 - 授权中   → 不通知前端
            int authCode = GetDouyinAuthStatus();

            // 授权中（4）不通知前端
            if (authCode == 4)
            {
                FileUtils.log("抖音授权中，不通知前端", "SystemStatusTimer");
                return;
            }

            string status = ConvertDouyinAuthStatus(authCode);
            await NotifyThirdPartyAuthStatus(status, platform: 0);
        }

        /// <summary>
        /// 获取抖音授权状态
        /// </summary>
        private static int GetDouyinAuthStatus()
        {
            return DouyinAuthUtils.GetAuthStatus();
        }

        /// <summary>
        /// 将抖音授权状态码转换为前端 status 字符串
        /// </summary>
        private static string ConvertDouyinAuthStatus(int authCode)
        {
            switch (authCode)
            {
                case 1: return "success"; // 已授权
                case 3: return "fail";    // 授权失败
                case 0: // 未授权
                case 2: // 授权过期
                default:
                    return "wait";        // 待授权
            }
        }

        /// <summary>
        /// 通知前端第三方授权状态
        /// </summary>
        private static async Task NotifyThirdPartyAuthStatus(string status, int platform)
        {
            await FrontNotice.NoticeFront("thirdPartyAuthStatus", new { status = status, platform = platform });
        }
        
        // ────────────────────────────────────────────────────────────────────
        // 打开网络疲劳页面（带频率控制）
        // ────────────────────────────────────────────────────────────────────
        private static long openNetworkFatiguePageLastTime = -1;
        private static string openNetworkFatiguePageTimePath = Path.GetFullPath("dataCollect\\config\\openNetworkFatiguePageTime");

        /// <summary>
        /// 通知前端打开网络疲劳页面（带频率控制）
        /// 通过KV配置 open_network_fatigue_page_time 控制调用间隔（单位：分钟，默认30分钟）
        /// </summary>
        public static async Task openNetworkFatiguePage()
        {
            // 替换为同事实际的方法调用，返回值含义：
            //   0 - 未授权   → wait
            //   1 - 已授权   → success
            //   2 - 授权过期 → wait
            //   3 - 授权失败 → fail
            //   4 - 授权中   → 不通知前端
            int authCode = GetDouyinAuthStatus();
            // 授权中（1, 4）不通知前端
            if (authCode == 1 || authCode == 4)
            {
                return;
            }
            
            // 1、获取频率限制（分钟），默认2分钟
            int interval = KvHelper.GetIntKvByKey("open_network_fatigue_page_time", 30);

            // 2、获取上次触发时间
            long lastTime = getOpenNetworkFatiguePageLastTime();

            // 3、如果有上次触发时间，检查是否超过间隔
            if (lastTime != -1)
            {
                long currentTime = ServerTimeUtils.getCurrentTime();
                long diffMinutes = (currentTime - lastTime) / (1000 * 60);
                if (diffMinutes < interval)
                {
                    return; // 未超过间隔，不触发
                }
            }

            // 4、触发通知
            await FrontNotice.NoticeFront("openNetworkFatiguePage", new { open = 1 });

            // 5、记录本次触发时间
            setOpenNetworkFatiguePageLastTime();
        }

        /// <summary>
        /// 获取上次触发openNetworkFatiguePage的时间戳
        /// 优先从内存获取，内存没有则从文件读取
        /// </summary>
        private static long getOpenNetworkFatiguePageLastTime()
        {
            try
            {
                if (File.Exists(openNetworkFatiguePageTimePath))
                {
                    string content = File.ReadAllText(openNetworkFatiguePageTimePath, Encoding.UTF8);
                    if (!string.IsNullOrWhiteSpace(content) && long.TryParse(content.Trim(), out long time))
                    {
                        openNetworkFatiguePageLastTime = time;
                        return time;
                    }
                }
                return -1;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"获取openNetworkFatiguePage上次触发时间异常: {ex.Message}",
                    "SystemStatusTimer.getOpenNetworkFatiguePageLastTime");
                return -1;
            }
        }

        /// <summary>
        /// 记录本次触发openNetworkFatiguePage的时间戳到文件
        /// </summary>
        private static void setOpenNetworkFatiguePageLastTime()
        {
            try
            {
                long currentTime = ServerTimeUtils.getCurrentTime();
                string dir = Path.GetDirectoryName(openNetworkFatiguePageTimePath);
                if (!Directory.Exists(dir))
                {
                    Directory.CreateDirectory(dir);
                }
                File.WriteAllText(openNetworkFatiguePageTimePath, currentTime.ToString(), Encoding.UTF8);
                openNetworkFatiguePageLastTime = currentTime;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"设置openNetworkFatiguePage触发时间异常: {ex.Message}",
                    "SystemStatusTimer.setOpenNetworkFatiguePageLastTime");
            }
        }
    }
}
