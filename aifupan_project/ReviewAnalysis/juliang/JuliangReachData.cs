﻿using douyin.Utils;
using Jint;
using juliang;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using RestSharp;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.enumeration.juliang;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.Model;
using ReviewAnalysis.qianchuan;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Reflection;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using System.Timers;
using System.Web;

namespace ReviewAnalysis.juliang
{
    public class JuliangReachData : IDisposable // 实现 IDisposable 释放 Timer 资源
    {
        // 配置项（外部传入）
        private readonly JuliangReachDataConfig _config;
        private readonly string _roomId = "";
        private readonly string _videoId = "";
        private readonly AnchorInfo _anchorInfo;
        private readonly CookieContainer _cookieContainer = new CookieContainer();
        // 暴露 CookieContainer 供外部添加 Cookie
        public CookieContainer CookieContainer => _cookieContainer;
        private readonly HttpClient _httpClient;

        // 定时轮询核心成员
        private System.Timers.Timer _pollingTimer; // 定时器
        private bool _isTaskRunning; // 任务执行标志（避免并发冲突）
        private readonly object _lockObj = new object(); // 锁对象（保证线程安全）
        private int _pollingInterval = 30; // 轮询间隔（默认 30 秒，可自定义）
        private DataCollectEventArgs _dataCollectArgs;
        private JuliangForm _juliangForm;
        private string _luopanDtCookie;
        public string LuopanDtCookie => _luopanDtCookie;
        private string _sessionIdCookie;
        public string SessionIdCookie => _sessionIdCookie;
        
        // Cookie文件缓存，避免频繁读取文件
        private static Dictionary<string, string> _cookieFileCache = new Dictionary<string, string>();
        private static object _cacheLock = new object();
        private static DateTime _lastCacheUpdateTime = DateTime.MinValue;
        private static readonly TimeSpan _cacheExpiry = TimeSpan.FromMinutes(5); // 缓存5分钟
        //线程安全计数器：记录待完成的核心逻辑数量（初始为核心逻辑总数）
        private int _pendingCoreTasks;
        // 定义委托变量，保存匿名Lambda的引用
        private ElapsedEventHandler _pollingHandler;
        #region 对外事件（外部可订阅）
        /// <summary>
        /// 轮询任务执行完成（无论成功失败）
        /// </summary>
        public event EventHandler<PollingResultEventArgs> PollingCompleted;

        /// <summary>
        /// 轮询状态变更（启动/停止）
        /// </summary>
        public event EventHandler<StatusChangedEventArgs> StatusChanged;

        /// <summary>
        /// 内部异常（非轮询业务异常，如配置错误、资源释放异常）
        /// </summary>
        public event EventHandler<Exception> InternalError;
        #endregion

        
        #region 构造函数（外部调用入口）
        /// <summary>
        /// 构造函数（传入配置，推荐外部使用）
        /// </summary>
        /// <param name="config">爬虫配置</param>
        public JuliangReachData(JuliangReachDataConfig config)
        {
            _config = config ?? throw new ArgumentNullException(nameof(config));
            _config.Validate(); // 验证配置有效性

            _roomId = _config.RoomId;
            _videoId = _config.VideoId;
            _anchorInfo = _config.AnchorInfo;
            _juliangForm = _config.juliangForm;
            
            // 从文件中获取LUOPAN_DT（巨量百应）
            string luopanDtFromFile = GetCookieFromFile("LUOPAN_DT");
            if (!string.IsNullOrEmpty(luopanDtFromFile))
            {
                _luopanDtCookie = luopanDtFromFile;
                FileUtils.LogRpa("从文件中获取LUOPAN_DT成功", "千川数据");
            }
            else
            {
                // 如果文件中没有，使用配置中的值
                _luopanDtCookie = _config.LuopanDtCookie;
                if (string.IsNullOrEmpty(_luopanDtCookie))
                {
                    FileUtils.LogRpa("未找到LUOPAN_DT配置", "千川数据");
                }
            }
            
            // 从千川模块获取sessionid（优先）
            string sessionIdFromQianchuan = GetQianchuanCookie("sessionid");
            if (!string.IsNullOrEmpty(sessionIdFromQianchuan))
            {
                _sessionIdCookie = sessionIdFromQianchuan;
                FileUtils.LogRpa("从千川模块获取sessionid成功", "千川数据");
            }
            else
            {
                // 备用：从巨量百应文件获取
                string sessionIdFromFile = GetCookieFromFile("sessionid");
                if (!string.IsNullOrEmpty(sessionIdFromFile))
                {
                    _sessionIdCookie = sessionIdFromFile;
                    FileUtils.LogRpa("从巨量百应文件获取sessionid成功", "千川数据");
                }
                else
                {
                    // 如果文件中没有，使用配置中的值
                    _sessionIdCookie = _config.SessionIdCookie;
                    if (string.IsNullOrEmpty(_sessionIdCookie))
                    {
                        FileUtils.LogRpa("未找到sessionid配置", "千川数据");
                    }
                }
            }
            
            ClearAllCookies();
            if (_luopanDtCookie != null)
            {
                _cookieContainer?.Add(new Uri("https://compass.jinritemai.com"),
    new Cookie("LUOPAN_DT", _luopanDtCookie));
//                _cookieContainer?.Add(new Uri("https://jinritemai.com"),
//new Cookie("COMPASS_LUOPAN_DT", _luopanDtCookie));
            }
            if (_sessionIdCookie != null)
            {
                _cookieContainer?.Add(new Uri("https://qianchuan.jinritemai.com"),
    new Cookie("sessionid", _sessionIdCookie));
            }
            var handler = new HttpClientHandler
            {
                AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                CookieContainer = _cookieContainer,
                UseCookies = true,
                AllowAutoRedirect = true
            };
            _httpClient = new HttpClient(handler);

            // 初始化定时器（默认不启动，需手动调用 StartPolling）
            InitTimer();
        }

        /// <summary>
        /// 无参构造函数（使用默认配置，简化外部调用）
        /// </summary>
        public JuliangReachData() : this(new JuliangReachDataConfig())
        {
            // 初始化 HttpClient（保持原逻辑）
            var handler = new HttpClientHandler
            {
                CookieContainer = _cookieContainer,
                UseCookies = true,
                AllowAutoRedirect = true
            };
            _httpClient = new HttpClient(handler);

            // 初始化定时器（默认不启动，需手动调用 StartPolling）
            InitTimer();
        }
        #endregion
        /// <summary>
        /// 初始化定时器配置
        /// </summary>
        private void InitTimer()
        {
            _pollingTimer = new System.Timers.Timer();
            _pollingTimer.Interval = _pollingInterval * 1000; // 间隔单位：毫秒（30秒 = 30*1000ms）
            _pollingTimer.AutoReset = true; // 自动重复触发（true=循环执行，false=只执行一次）
            _pollingTimer.Enabled = false; // 初始禁用，调用 StartPolling 后启用
            _pollingHandler = async (sender, e) => await PollingElapsedHandler();
            // 定时器触发事件（绑定异步任务处理方法）
            _pollingTimer.Elapsed += _pollingHandler;
        }

        /// <summary>
        /// 定时器触发后的异步处理方法（核心轮询逻辑）
        /// </summary>
        public async Task PollingElapsedHandler()
        {
            // 锁 + 执行标志：避免上一次请求未完成时，下一次轮询触发（防止并发）
            lock (_lockObj)
            {
                if (_isTaskRunning)
                {
                    FileUtils.log($"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] 上一次轮询任务尚未完成，跳过本次触发");
                    return;
                }
                _isTaskRunning = true;
            }
            _pendingCoreTasks = 5;
            try
            {
                FileUtils.LogRpa($"\n[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] 开始执行轮询任务（LiveTalent）", "巨量接口");
                Task taskLiveTalent =  LiveTalent();
                //Task  taskFormulaModeCoreData =  FormulaModeCoreData();
                Task  taskFormPayUserPortrait =  FormPayUserPortrait();
                Task  taskFormFlowOrderSource = FormFlowOrderSource();
                Task  taskFormWatchUserPortrait = FormWatchUserPortrait();
                Task  taskFormBasicLiveScreen = FormBasicLiveScreen();
                Task  taskGetProductPaymentData = GetProductPaymentData();
                Task [] allTasks = new[] { taskLiveTalent, taskFormPayUserPortrait, taskFormFlowOrderSource, taskFormWatchUserPortrait, taskFormBasicLiveScreen, taskGetProductPaymentData };
                await Task.WhenAll(allTasks);
                _juliangForm.browser_FrameLoadEnd(null, null);
                //if (_pendingCoreTasks == 0)
                //{
                    _juliangForm.tempLoadDataFlagSet?.Add(JuliangDataKeyTypeEnum.bigScreenDataBase);
                    _juliangForm.tempLoadDataFlagSet?.Add(JuliangDataKeyTypeEnum.bigScreenDataPro);
                    _juliangForm.tempLoadDataFlagSet?.Add(JuliangDataKeyTypeEnum.flowOrderSource);
                    _juliangForm.tempLoadDataFlagSet?.Add(JuliangDataKeyTypeEnum.payUserPortrait);
                    _juliangForm.tempLoadDataFlagSet?.Add(JuliangDataKeyTypeEnum.watchUserPortrait);
                //}
                if (_juliangForm.isTemp)
                {
                    _juliangForm.closeForm();
                    StopPolling();
                }

                await FileUtils.LogAsync($"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] 轮询任务执行完成");
            }
            catch (Exception ex)
            {
                // 捕获异常，避免单个轮询失败导致定时器停止
                FileUtils.LogRpa($"接口轮询任务执行失败${ex.Message + ex.StackTrace}=={_anchorInfo?.AnchorName}", "巨量接口");
                if (_juliangForm.isTemp)
                {
                    _juliangForm.closeForm();
                    StopPolling();
                }
            }
            finally
            {
                // 无论成功失败，都重置执行标志
                lock (_lockObj)
                {
                    _isTaskRunning = false;
                }
            }
        }

        /// <summary>
        /// 启动定时轮询
        /// </summary>
        /// <param name="intervalSeconds">轮询间隔（秒），默认 30 秒</param>
        public void StartPolling(int intervalSeconds = 30)
        {
            if (intervalSeconds < 5) // 最小间隔限制（避免过于频繁请求被反爬）
                throw new ArgumentOutOfRangeException(nameof(intervalSeconds), "轮询间隔不能小于 5 秒");
//            var luopanDtCookie = GetLuopanDtCookie("https://compass.jinritemai.com")?.Value; //https://jinritemai.com
//            ClearAllCookies();
//            if (luopanDtCookie != null)
//            {
//                _cookieContainer?.Add(new Uri("https://compass.jinritemai.com"),
//    new Cookie("LUOPAN_DT", luopanDtCookie));
//                _cookieContainer?.Add(new Uri("https://jinritemai.com"),
//new Cookie("COMPASS_LUOPAN_DT", luopanDtCookie));
//            }

//            else
//                return;
                //_dataCollectArgs = args;
            _pollingInterval = intervalSeconds;
            _pollingTimer.Interval = _pollingInterval * 1000; // 更新间隔
            _pollingTimer.Enabled = true; // 启用定时器
            _pollingTimer.Start(); // 启动定时器

            FileUtils.log($"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] 定时轮询已启动，间隔：{_pollingInterval} 秒");
        }
        /// <summary>
        /// 全量清除 CookieContainer 中所有 Cookie（彻底清空）
        /// </summary>
        public void ClearAllCookies()
        {
            lock (_lockObj) // 线程安全：避免与添加/读取 Cookie 冲突
            {
                if (_cookieContainer == null)
                {
                    //OnStatusChanged("CookieContainer 未初始化，无需清除");
                    return;
                }

                try
                {
                    // 方案1：反射操作 CookieContainer 私有字段（最彻底，推荐）
                    ClearCookiesByReflection();

                    // 方案2：遍历删除（备选，无需反射，兼容所有环境）
                    ClearCookiesByIteration();

                    //OnStatusChanged("已清除 CookieContainer 中所有 Cookie");
                }
                catch (Exception ex)
                {
                    //OnInternalError(new Exception("清除所有 Cookie 失败", ex));
                    //OnStatusChanged($"清除 Cookie 失败：{ex.Message}");
                }
            }
        }
        #region 内部辅助方法（反射/遍历实现）
        /// <summary>
        /// 反射清除所有 Cookie（最彻底，直接操作 CookieContainer 内部存储）
        /// </summary>
        private void ClearCookiesByReflection()
        {
            // CookieContainer 内部私有字段 _cookies（存储所有 Cookie）
            var cookiesField = typeof(CookieContainer).GetField("_cookies", BindingFlags.NonPublic | BindingFlags.Instance);
            if (cookiesField == null)
                throw new InvalidOperationException("未找到 CookieContainer 的 _cookies 私有字段（可能 .NET 版本不兼容）");

            // 获取 _cookies 的值（类型为 Hashtable）
            var cookiesHashtable = cookiesField.GetValue(_cookieContainer) as System.Collections.Hashtable;
            cookiesHashtable?.Clear(); // 清空 Hashtable，彻底删除所有 Cookie

            // 可选：清除私有字段 _domainTable（存储域名与 Cookie 的映射）
            var domainTableField = typeof(CookieContainer).GetField("_domainTable", BindingFlags.NonPublic | BindingFlags.Instance);
            var domainTableHashtable = domainTableField?.GetValue(_cookieContainer) as System.Collections.Hashtable;
            domainTableHashtable?.Clear();
        }

        /// <summary>
        /// 遍历删除所有 Cookie（备选方案，无需反射）
        /// 注意：需提前知道所有已存储的域名，否则可能删除不彻底
        /// </summary>
        private void ClearCookiesByIteration()
        {
            // 已知的爬虫目标域名（根据实际场景补充）
            var knownDomains = new[] { "jinritemai.com", "buyin.jinritemai.com", "compass.jinritemai.com" };

            foreach (var domain in knownDomains)
            {
                var targetUri = new Uri($"https://{domain}");
                CookieCollection domainCookies = _cookieContainer.GetCookies(targetUri);

                foreach (HttpCookie cookie in domainCookies.Cast<HttpCookie>())
                {
                    _cookieContainer.Add(targetUri, new Cookie(cookie.Name, "")
                    {
                        Domain = cookie.Domain,
                        Path = cookie.Path
                    });
                }
            }
        }

        /// <summary>
        /// 反射按名称清除 Cookie（彻底，支持所有域名）
        /// </summary>
        private int ClearCookieByNameByReflection(string cookieName)
        {
            var cookiesField = typeof(CookieContainer).GetField("_cookies", BindingFlags.NonPublic | BindingFlags.Instance);
            if (cookiesField == null)
                throw new InvalidOperationException("未找到 CookieContainer 的 _cookies 私有字段");

            var cookiesHashtable = cookiesField.GetValue(_cookieContainer) as System.Collections.Hashtable;
            if (cookiesHashtable == null || cookiesHashtable.Count == 0)
                return 0;

            int deletedCount = 0;

            // 遍历所有 Cookie 条目（key 为 Uri，value 为 CookieCollection）
            var entries = cookiesHashtable.Cast<System.Collections.DictionaryEntry>().ToList();
            foreach (var entry in entries)
            {
                if (entry.Value is CookieCollection cookieCollection)
                {
                    // 筛选名称匹配的 Cookie
                    var cookiesToRemove = cookieCollection.Cast<Cookie>()
                        .Where(c => c.Name.Equals(cookieName, StringComparison.Ordinal))
                        .ToList();

                    foreach (var cookie in cookiesToRemove)
                    {
                        //cookieCollection.Remove(cookie.Name);
                        deletedCount++;
                    }

                    // 若该 Uri 下无 Cookie，移除整个条目
                    if (cookieCollection.Count == 0)
                        cookiesHashtable.Remove(entry.Key);
                }
            }

            return deletedCount;
        }
        #endregion
        /// <summary>
        /// 按域名清除 Cookie（仅删除目标域名下的所有 Cookie）
        /// </summary>
        /// <param name="targetDomain">目标域名（如 "buyin.jinritemai.com"，无需带 https://）</param>
        public void ClearCookiesByDomain(string targetDomain)
        {
            if (string.IsNullOrWhiteSpace(targetDomain))
                throw new ArgumentNullException(nameof(targetDomain), "目标域名不能为空");

            lock (_lockObj)
            {
                if (_cookieContainer == null)
                {
                    //OnStatusChanged("CookieContainer 未初始化，无需清除");
                    return;
                }

                try
                {
                    var targetUri = new Uri($"https://{targetDomain}");
                    CookieCollection domainCookies = _cookieContainer.GetCookies(targetUri);

                    // 遍历删除目标域名下的所有 Cookie
                    foreach (HttpCookie cookie in domainCookies.Cast<HttpCookie>())
                    {
                        // 移除 Cookie：添加一个同名、同域名但空值的 Cookie 覆盖原有的
                        _cookieContainer.Add(targetUri, new Cookie(cookie.Name, "")
                        {
                            Domain = cookie.Domain,
                            Path = cookie.Path
                        });
                    }

                    //OnStatusChanged($"已清除域名 [{targetDomain}] 下的 {domainCookies.Count} 个 Cookie");
                }
                catch (Exception ex)
                {
                    //OnInternalError(new Exception($"清除域名 [{targetDomain}] 的 Cookie 失败", ex));
                    //OnStatusChanged($"清除域名 Cookie 失败：{ex.Message}");
                }
            }
        }

        /// <summary>
        /// 停止定时轮询
        /// </summary>
        public void StopPolling()
        {
            _pollingTimer?.Stop();
            if (_pollingTimer != null)
                _pollingTimer.Enabled = false;
            FileUtils.log($"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] 定时轮询已停止");
        }

        /// <summary>
        /// 释放资源（Timer + HttpClient）
        /// </summary>
        public void Dispose()
        {
            StopPolling();
            if (_pollingTimer != null && _pollingHandler != null)
            {
                _pollingTimer.Elapsed -= _pollingHandler;
            
            }
            _pollingTimer?.Dispose(); // 释放定时器
            _pollingTimer = null;
            _httpClient?.Dispose(); // 释放 HttpClient
            FileUtils.log($"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] 资源已释放");
        }

        /// <summary>
        /// 从 CookieContainer 中获取 LUOPAN_DT Cookie
        /// </summary>
        /// <param name="targetDomain">目标域名（如 "buyin.jinritemai.com"，无需带 https://）</param>
        /// <returns>LUOPAN_DT 的 HttpCookie 对象，不存在则返回 null</returns>
        public Cookie GetLuopanDtCookie(string targetDomain)
        {
            if (string.IsNullOrWhiteSpace(targetDomain))
                throw new ArgumentNullException(nameof(targetDomain), "目标域名不能为空");

            if (_cookieContainer == null)
                throw new InvalidOperationException("CookieContainer 未初始化");

            try
            {
                // 关键：CookieContainer.GetCookies 需传入带协议的 Uri（如 https://）
                var targetUri = new Uri(targetDomain);

                // 1. 获取目标域名下的所有 Cookie（返回 CookieCollection 集合）
                CookieCollection allCookies = _cookieContainer.GetCookies(targetUri);

                // 2. 筛选名称为 LUOPAN_DT 的 Cookie（大小写敏感，需与网站一致）
                System.Net.Cookie luopanDtCookie = allCookies.Cast<System.Net.Cookie>()
                    .FirstOrDefault(cookie => cookie.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal));

                return luopanDtCookie;
            }
            catch (Exception ex)
            {
                //OnInternalError(new Exception("获取 LUOPAN_DT Cookie 失败", ex));
                return null;
            }
        }

        /// <summary>
        /// 简化版：获取默认域名（buyin.jinritemai.com）的 LUOPAN_DT Cookie
        /// </summary>
        public Cookie GetLuopanDtCookie()
        {
            // 默认目标域名（可根据你的实际场景修改，如 "compass.jinritemai.com"）
            return GetLuopanDtCookie("buyin.jinritemai.com");
        }

        /// <summary>
        /// 快捷获取 LUOPAN_DT 的 Cookie 值（直接返回字符串，方便外部使用）
        /// </summary>
        /// <returns>LUOPAN_DT 的值，不存在/过期则返回 null</returns>
        public string GetLuopanDtCookieValue()
        {
            var cookie = GetLuopanDtCookie();
            if (cookie == null)
                return null;

            // 检查 Cookie 是否过期
            if (cookie.Expires != DateTime.MinValue && cookie.Expires < DateTime.Now)
            {
                //OnStatusChanged("LUOPAN_DT Cookie 已过期");
                return null;
            }

            return cookie.Value;
        }

        // ---------------------- 以下是原有的方法（保持不变）----------------------
        private string GetAB(string ua, Dictionary<string, string> parameters, string dataStr = "")
        {
            try
            {
                var paramStr = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
                string jsFilePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "script\\juliang.js");

                if (!File.Exists(jsFilePath))
                    throw new FileNotFoundException("未找到juliang.js文件", jsFilePath);

                var jsCode = File.ReadAllText(jsFilePath, Encoding.UTF8);

                // 替换为 Jint 引擎（支持 ES6+）
                var engine = new Jint.Engine();
                engine.Execute(jsCode);

                // 调用全局函数 get_a_bogus（参数传递方式与 Jurassic 一致）
                return engine.Invoke("get_a_bogus", ua, paramStr, dataStr).AsString();
            }
            catch (Exception ex)
            {
                FileUtils.log($"生成a_bogus失败：{ex.Message}");
                throw;
            }
        }

        private string GetFp()
        {
            const string chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            var random = new Random();

            var timestamp = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalSeconds;
            var base36Time = ToBase36(timestamp).ToLower();

            var r = new char[36];
            r[8] = r[13] = r[18] = r[23] = '_';
            r[14] = '4';

            for (int o = 0; o < 36; o++)
            {
                if (r[o] == '\0')
                {
                    var i = random.Next(chars.Length);
                    if (o == 19)
                    {
                        r[o] = chars[(3 & i) | 8];
                    }
                    else
                    {
                        r[o] = chars[i];
                    }
                }
            }

            return $"verify_{base36Time}_{new string(r)}";
        }

        private string ToBase36(long number)
        {
            const string base36Chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            if (number == 0) return "0";

            var sb = new StringBuilder();
            while (number > 0)
            {
                sb.Insert(0, base36Chars[(int)(number % 36)]);
                number /= 36;
            }
            return sb.ToString();
        }

        private string GetMsToken(int randomLength = 184)
        {
            const string baseStr = "ABCDEFGHIGKLMNOPQRSTUVwXYZabcdefghigklmnopqrstuVwxyz0123456789=";
            var random = new Random();
            var sb = new StringBuilder();

            for (int i = 0; i < randomLength; i++)
            {
                sb.Append(baseStr[random.Next(baseStr.Length)]);
            }

            return sb.ToString();
        }

        private string GetLid()
        {
            var timestamp = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalMilliseconds;
            var randomPart = new Random().Next(1000, 9999);
            return $"{timestamp.ToString().Substring(0, 5)}{randomPart}";
        }

        public async Task CheckAnchorAuthStatusByCookie()
        {
            _juliangForm.GetCookie();
        }

        /// <summary>
        /// 专业版-直播大屏数据
        /// </summary>
        public async Task LiveTalent()
        {
            var headers = new Dictionary<string, string>
            {
                {"accept", "application/json, text/plain, */*"},
                {"accept-language", "zh-CN,zh;q=0.9"},
                {"cache-control", "no-cache"},
                {"pragma", "no-cache"},
                {"priority", "u=1, i"},
                {"referer", $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_roomId}"},
                {"sec-ch-ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\""},
                {"sec-ch-ua-mobile", "?0"},
                {"sec-ch-ua-platform", "\"Windows\""},
                {"sec-fetch-dest", "empty"},
                {"sec-fetch-mode", "cors"},
                {"sec-fetch-site", "same-origin"},
                {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"}
            };
            _httpClient.DefaultRequestHeaders.Clear();
            foreach (var header in headers)
            {
                if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                {
                    FileUtils.log($"无法添加头信息: {header.Key}");
                }
            }

            var fp = GetFp();
            var msToken = GetMsToken();
            var parameters = new Dictionary<string, string>
            {
                {"room_id", _roomId},
                {"index_selected", "gpm,pay_ucnt,pay_combo_cnt,watch_pay_ucnt_ratio,product_click_pay_ucnt_ratio,online_user_cnt,live_show_watch_cnt_ratio,avg_watch_duration,watch_interact_ucnt_ratio,follow_anchor_ucnt,live_show_cnt,stat_cost,real_refund_amt"},
                {"_lid", GetLid()},
                {"verifyFp", fp},
                {"fp", fp},
                {"msToken", msToken}
            };

            var ua = headers["user-agent"];
            var aBogus = GetAB(ua, parameters);
            parameters.Add("a_bogus", aBogus);

            var url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/core_data";
            var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
            var requestUrl = $"{url}?{queryString}";

            var response = await _httpClient.GetAsync(requestUrl);
            var responseText = await response.Content.ReadAsStringAsync();
            FileUtils.log($"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] 响应状态：{response?.StatusCode}");
            FileUtils.log($"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] 响应内容：{responseText}");
            //_dataCollectArgs.data = responseText;
            if (string.IsNullOrWhiteSpace(responseText) || responseText.Contains("\"msg\":\"当前网络不稳定，请稍后再试\"")
|| responseText.Contains("\"msg\":\"当前视角与页面不匹配\"")
|| responseText.Contains("\"msg\":\"当前环境存在风险，请稍后重试\""))
            {
                FileUtils.LogRpa($"{responseText}=={_anchorInfo?.AnchorName}", "专业版-直播大屏数据");
                return;
            }
            else if (responseText.Contains("\"msg\":\"服务器错误\"")) //授权失效，可能是罗盘Token改变
            {

                _juliangForm.ModifyLuopanCookieExpiry();
                _juliangForm.loginExpire(JuliangAuthStatusEnum.authExpires);
                StopPolling();
                FileUtils.LogRpa($"接口检测到，授权登录已过期${responseText}=={_anchorInfo?.AnchorName}", "专业版-直播大屏数据");
                return;
            }
            JuliangDataHandle.writeGatherDataPro(fillDataCollectArgs(responseText));
            FileUtils.LogRpa($"接口检测到，数据存储本地${responseText}=={_anchorInfo?.AnchorName}", "专业版-直播大屏数据");
            // 逻辑完成：计数器减1（线程安全）
            Interlocked.Decrement(ref _pendingCoreTasks);
        }

        /// <summary>
        /// 填充写入的数据
        /// </summary>
        /// <param name="dataCollectArgs">写入数据对象</param>
        /// <param name="data">接口数据</param>
        private DataCollectEventArgs fillDataCollectArgs(string data)
        {
            return new DataCollectEventArgs
            {
                data = data,
                videoId = _juliangForm.videoId,// _videoId,
                secUid = _anchorInfo.SecUid,
                batchNumber = _juliangForm.roomId,// _roomId,
                anchorName = _anchorInfo.AnchorName,
                juliangForm = _juliangForm
            };
        }

        /// <summary>
        /// 专业版-直播大屏公式模式核心数据
        /// </summary>
        public async Task FormulaModeCoreData()
        {
            var headers = new Dictionary<string, string>
            {
                {"accept", "application/json, text/plain, */*"},
                {"accept-language", "zh-CN,zh;q=0.9"},
                {"cache-control", "no-cache"},
                {"pragma", "no-cache"},
                {"priority", "u=1, i"},
                {"referer", $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_roomId}"},
                {"sec-ch-ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\""},
                {"sec-ch-ua-mobile", "?0"},
                {"sec-ch-ua-platform", "\"Windows\""},
                {"sec-fetch-dest", "empty"},
                {"sec-fetch-mode", "cors"},
                {"sec-fetch-site", "same-origin"},
                {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"}
            };
            _httpClient.DefaultRequestHeaders.Clear();
            foreach (var header in headers)
            {
                if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                {
                    FileUtils.log($"无法添加头信息: {header.Key}");
                }
            }

            var parameters = new Dictionary<string, string>
            {
                {"room_id", _roomId},
                {"_lid", GetLid()}
            };

            var url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/formula_mode_core_data";
            var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
            var requestUrl = $"{url}?{queryString}";

            var response = await _httpClient.GetAsync(requestUrl);
            var responseText = await response.Content.ReadAsStringAsync();
            var jsonData = JObject.Parse(responseText);

            // 解析并输出数据
            var liveShowCnt = jsonData["data"]["live_show_cnt"];
            var medianLiveShowCnt = jsonData["data"]["median_live_show_cnt"];
            var medianGpm = jsonData["data"]["median_gpm"];
            var medianWatchRatio = jsonData["data"]["median_watch_live_show_cnt_ratio"];

            var items = new[]
            {
                new { index_name = "live_show_cnt", index_display = "曝光次数", unit = liveShowCnt["unit"], value = liveShowCnt["value"] },
                new { index_name = "median_live_show_cnt", index_display = "曝光次数-同行同层中位数", unit = medianLiveShowCnt["unit"], value = medianLiveShowCnt["value"] },
                new { index_name = "median_watch_live_show_cnt_ratio", index_display = "曝光-观看率 (次数)-同行同层中位数", unit = medianWatchRatio["unit"], value = medianWatchRatio["value"] },
                new { index_name = "median_gpm", index_display = "千次观看成交金额-同行同层中位数", unit = medianGpm["unit"], value = medianGpm["value"] }
            };

            foreach (var item in items)
            {
                FileUtils.log(JsonConvert.SerializeObject(item));
            }
            // 逻辑完成：计数器减1（线程安全）
            Interlocked.Decrement(ref _pendingCoreTasks);
        }

        /// <summary>
        ///基础版-成交用户画像
        /// </summary>
        public async Task FormPayUserPortrait()
        {
            var headers = new Dictionary<string, string>
            {
                {"accept", "application/json, text/plain, */*"},
                {"accept-language", "zh-CN,zh;q=0.9"},
                {"cache-control", "no-cache"},
                {"pragma", "no-cache"},
                {"priority", "u=1, i"},
                {"referer", $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_roomId}"},
                {"sec-ch-ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\""},
                {"sec-ch-ua-mobile", "?0"},
                {"sec-ch-ua-platform", "\"Windows\""},
                {"sec-fetch-dest", "empty"},
                {"sec-fetch-mode", "cors"},
                {"sec-fetch-site", "same-origin"},
                {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"}
            };
            _httpClient.DefaultRequestHeaders.Clear();
            foreach (var header in headers)
            {
                if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                {
                    FileUtils.log($"无法添加头信息: {header.Key}");
                }
            }

            var parameters = new Dictionary<string, string>
            {
                {"room_id", _roomId},
                {"source", "pay_user"},
                {"_lid", GetLid()}
            };

            var url = "https://compass.jinritemai.com/compass_api/author/live/basic_live_screen/user_portrait";
            var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
            var requestUrl = $"{url}?{queryString}";

            var response = await _httpClient.GetAsync(requestUrl);
            var responseText = await response.Content.ReadAsStringAsync();
            if (string.IsNullOrWhiteSpace(responseText) || responseText.Contains("\"msg\":\"当前网络不稳定，请稍后再试\"")
|| responseText.Contains("\"msg\":\"当前视角与页面不匹配\"")
|| responseText.Contains("\"msg\":\"当前环境存在风险，请稍后重试\""))
            {
                FileUtils.LogRpa($"{responseText}=={_anchorInfo?.AnchorName}", "基础版-成交用户画像");
                return;
            }
            else if (responseText.Contains("\"msg\":\"服务器错误\"")) //授权失效，可能是罗盘Token改变
            {
                //_juliangForm.ModifyLuopanCookieExpiry();
                //_juliangForm.loginExpire(JuliangAuthStatusEnum.authExpires);
                //StopPolling();
                FileUtils.LogRpa($"接口检测到，授权登录已过期${responseText}=={_anchorInfo?.AnchorName}", "基础版-成交用户画像");
                return;
            }
            
            JuliangDataHandle.writeUserPortrait(fillDataCollectArgs(responseText), 0);
            FileUtils.LogRpa($"接口检测到，数据存储本地${responseText}=={_anchorInfo?.AnchorName}", "基础版-成交用户画像");
            // 逻辑完成：计数器减1（线程安全）
            Interlocked.Decrement(ref _pendingCoreTasks);
        }

        /// <summary>
        ///基础版-看播用户画像
        /// </summary>
        public async Task FormWatchUserPortrait()
        {
            var headers = new Dictionary<string, string>
            {
                {"accept", "application/json, text/plain, */*"},
                {"accept-language", "zh-CN,zh;q=0.9"},
                {"cache-control", "no-cache"},
                {"pragma", "no-cache"},
                {"priority", "u=1, i"},
                {"referer", $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_roomId}"},
                {"sec-ch-ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\""},
                {"sec-ch-ua-mobile", "?0"},
                {"sec-ch-ua-platform", "\"Windows\""},
                {"sec-fetch-dest", "empty"},
                {"sec-fetch-mode", "cors"},
                {"sec-fetch-site", "same-origin"},
                {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"}
            };
            _httpClient.DefaultRequestHeaders.Clear();
            foreach (var header in headers)
            {
                if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                {
                    FileUtils.log($"无法添加头信息: {header.Key}");
                }
            }

            var parameters = new Dictionary<string, string>
            {
                {"room_id", _roomId},
                {"source", "watch_user"},
                {"_lid", GetLid()}
            };

            var url = "https://compass.jinritemai.com/compass_api/author/live/basic_live_screen/user_portrait";
            var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
            var requestUrl = $"{url}?{queryString}";

            var response = await _httpClient.GetAsync(requestUrl);
            var responseText = await response.Content.ReadAsStringAsync();
            if (string.IsNullOrWhiteSpace(responseText) || responseText.Contains("\"msg\":\"当前网络不稳定，请稍后再试\"")
|| responseText.Contains("\"msg\":\"当前视角与页面不匹配\"")
|| responseText.Contains("\"msg\":\"当前环境存在风险，请稍后重试\""))
            {
                FileUtils.LogRpa($"{responseText}=={_anchorInfo?.AnchorName}", "基础版-看播用户画像");
                return;
            }
            else if (responseText.Contains("\"msg\":\"服务器错误\"")) //授权失效，可能是罗盘Token改变
            {
                //_juliangForm.ModifyLuopanCookieExpiry();
                //_juliangForm.loginExpire(JuliangAuthStatusEnum.authExpires);
                //StopPolling();
                FileUtils.LogRpa($"接口检测到，授权登录已过期${responseText}=={_anchorInfo?.AnchorName}", "基础版-看播用户画像");
                return;
            }
            
            JuliangDataHandle.writeUserPortrait(fillDataCollectArgs(responseText), 1);
            FileUtils.LogRpa($"接口检测到，数据存储本地${responseText}=={_anchorInfo?.AnchorName}", "基础版-看播用户画像");
            // 逻辑完成：计数器减1（线程安全）
            Interlocked.Decrement(ref _pendingCoreTasks);
        }

        /// <summary>
        /// 基础版-流量分析
        /// </summary>
        /// <returns></returns>
        public async Task FormFlowOrderSource()
        {
            var headers = new Dictionary<string, string>
            {
                {"accept", "application/json, text/plain, */*"},
                {"accept-language", "zh-CN,zh;q=0.9"},
                {"cache-control", "no-cache"},
                {"pragma", "no-cache"},
                {"priority", "u=1, i"},
                {"referer", $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_roomId}"},
                {"sec-ch-ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\""},
                {"sec-ch-ua-mobile", "?0"},
                {"sec-ch-ua-platform", "\"Windows\""},
                {"sec-fetch-dest", "empty"},
                {"sec-fetch-mode", "cors"},
                {"sec-fetch-site", "same-origin"},
                {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"}
            };
            _httpClient.DefaultRequestHeaders.Clear();
            foreach (var header in headers)
            {
                if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                {
                    FileUtils.log($"无法添加头信息: {header.Key}");
                }
            }

            var parameters = new Dictionary<string, string>
            {
                {"room_id", _roomId},
                {"data_range", "1"},
                {"_lid", GetLid()}
            };

            var url = "https://compass.jinritemai.com/compass_api/content_live/author/basic_live_screen/flow_order_source";
            var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
            var requestUrl = $"{url}?{queryString}";

            var response = await _httpClient.GetAsync(requestUrl);
            var responseText = await response.Content.ReadAsStringAsync();
            if (string.IsNullOrWhiteSpace(responseText) || responseText.Contains("\"msg\":\"当前网络不稳定，请稍后再试\"")
|| responseText.Contains("\"msg\":\"当前视角与页面不匹配\"")
|| responseText.Contains("\"msg\":\"当前环境存在风险，请稍后重试\""))
            {
                FileUtils.LogRpa($"{responseText}=={_anchorInfo?.AnchorName}", "基础版-流量分析");
                return;
            }
            else if (responseText.Contains("\"msg\":\"服务器错误\"")) //授权失效，可能是罗盘Token改变
            {
                //_juliangForm.ModifyLuopanCookieExpiry();
                //_juliangForm.loginExpire(JuliangAuthStatusEnum.authExpires);
                //StopPolling();
                FileUtils.LogRpa($"接口检测到，授权登录已过期${responseText}=={_anchorInfo?.AnchorName}", "基础版-流量分析");
                return;
            }
            JuliangDataHandle.writeFlowSourceData(fillDataCollectArgs(responseText));
            
            FileUtils.LogRpa($"接口检测到，数据存储本地${responseText}=={_anchorInfo?.AnchorName}", "基础版-流量分析");
            // 逻辑完成：计数器减1（线程安全）
            Interlocked.Decrement(ref _pendingCoreTasks);
        }

        private RestClient _restClient;

        /// <summary>
        ///基础版-直播大屏
        /// </summary>
        public async Task FormBasicLiveScreen()
        {
            var headers = new Dictionary<string, string>
            {
                {"accept", "application/json, text/plain, */*"},
                {"accept-language", "zh-CN,zh;q=0.9"},
                {"cache-control", "no-cache"},
                {"pragma", "no-cache"},
                //{"priority", "u=1, i"},
                {"referer", $"https://compass.jinritemai.com/screen/talent/main?live_room_id={_roomId}"},
                //{"sec-ch-ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\""},
                //{"sec-ch-ua-mobile", "?0"},
                //{"sec-ch-ua-platform", "\"Windows\""},
                //{"sec-fetch-dest", "empty"},
                //{"sec-fetch-mode", "cors"},
                //{"sec-fetch-site", "same-origin"},
                {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"}
            };
            _httpClient.DefaultRequestHeaders.Clear();
            foreach (var header in headers)
            {
                if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                {
                    FileUtils.log($"无法添加头信息: {header.Key}");
                }
            }

            var fp = GetFp();
            var msToken = GetMsToken();

            var parameters = new Dictionary<string, string>
            {
                {"room_id", _roomId},
                {"_lid", GetLid()},
                {"verifyFp", fp},
                {"fp", fp},
                {"msToken", msToken}
            };
            var ua = headers["user-agent"];
            var aBogus = GetAB(ua, parameters);
            parameters.Add("a_bogus", aBogus);
            var url = "https://compass.jinritemai.com/compass_api/author/live/basic_live_screen/base_info";
            var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
            var requestUrl = $"{url}?{queryString}";

            var response = await _httpClient.GetAsync(requestUrl);
            var responseText = await response.Content.ReadAsStringAsync();
            if (string.IsNullOrWhiteSpace(responseText) || responseText.Contains("\"msg\":\"当前网络不稳定，请稍后再试\"")
    || responseText.Contains("\"msg\":\"当前视角与页面不匹配\"")
    || responseText.Contains("\"msg\":\"当前环境存在风险，请稍后重试\""))
            {
                FileUtils.LogRpa($"{responseText}=={_anchorInfo?.AnchorName}", "基础版-直播大屏");
                return;
            }
            else if (responseText.Contains("\"msg\":\"服务器错误\"")) //授权失效，可能是罗盘Token改变
            {
                //_juliangForm.ModifyLuopanCookieExpiry();
                //_juliangForm.loginExpire(JuliangAuthStatusEnum.authExpires);
                //StopPolling();
                FileUtils.LogRpa($"接口检测到，授权登录已过期${responseText}=={_anchorInfo?.AnchorName}", "基础版-直播大屏");
                return;
            }
            
            // 先尝试获取千川数据并合并到基础版数据中
            string mergedResponseText = responseText;
            bool qianchuanDataMerged = false;
            
            // 检查是否可以获取千川数据
            if (CanFetchQianchuanData())
            {
                // 尝试获取千川的"整体支付ROI"数据并合并到基础版数据中
                try
                {
                // 从千川模块获取aavid
                var aavid = GetAavidFromQianchuan();
                if (!string.IsNullOrEmpty(aavid))
                {
                    // 使用_anchorInfo.AnchorUserId作为anchorId
                    var anchorId = _anchorInfo?.AnchorUserId;
                    if (string.IsNullOrEmpty(anchorId))
                    {
                        // anchorId为空，跳过千川数据获取
                        FileUtils.LogRpa($"主播{_anchorInfo?.AnchorName} anchorId为空，跳过千川数据获取", "基础版-直播大屏");
                    }
                    else
                    {
                        var qianchuanHeaders = new Dictionary<string, string>
                    {
                        { "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36" },
                        { "Accept", "application/json, text/plain, */*" },
                        { "Content-Type", "application/json" },
                        { "sec-ch-ua-platform", "\"Windows\"" },
                        { "sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"" },
                        { "sec-ch-ua-mobile", "?0" },
                        { "x-secsdk-csrf-token", "DOWNGRADE" },
                        { "Origin", "https://qianchuan.jinritemai.com" },
                        { "Sec-Fetch-Site", "same-origin" },
                        { "Sec-Fetch-Mode", "cors" },
                        { "Sec-Fetch-Dest", "empty" },
                        { "Referer", $"https://qianchuan.jinritemai.com/board-next?live_room_id={_roomId}&aavid={aavid}&ad_origin=1&fromScene=luopan" },
                        { "Accept-Language", "zh-CN,zh;q=0.9" },
                        { "Priority", "u=1, i" }
                    };

                    _httpClient.DefaultRequestHeaders.Clear();
                    foreach (var header in qianchuanHeaders)
                    {
                        if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                        {
                            FileUtils.log($"无法添加头信息: {header.Key}");
                        }
                    }

                    var qianchuanUrl = "https://qianchuan.jinritemai.com/ad/api/data/v1/common/statQuery";
                    var queryParams = new Dictionary<string, string>
                    {
                        { "reqFrom", "commonMetricCard" },
                        { "aavid", aavid },
                        { "gfversion", "1.0.0.294" }
                    };

                    var requestBody = new
                    {
                        DataSetKey = "board_roi2_overview_conf_next",
                        Metrics = new[]
                        {
                            "total_prepay_and_pay_order_realtime_roi2", // 整体支付ROI
                            "stat_cost_for_roi2", // 千川消耗（投放金额）
                            "total_refund_order_gmv_for_roi2_1h_all", // 退款金额
                            //"stat_cost_for_roi2", // 整体消耗
                            "total_prepay_and_pay_settle_realtime_roi2_1h" // 净成交ROI
                        },
                        Dimensions = Array.Empty<object>(),
                        PageParams = new
                        {
                            Offset = 0,
                            Limit = -1
                        },
                        Filters = new
                        {
                            ConditionRelationshipType = 1,
                            Conditions = new[]
                            {
                                new { Field = "advertiser_id", Operator = 7, Values = new[] { aavid } },
                                new { Field = "room_id", Operator = 7, Values = new[] { "7622133390529334026" } },//_roomId
                                new { Field = "anchor_id", Operator = 7, Values = new[] { anchorId } }
                            }
                        },
                        refer = "ecp,7532804468681916467,7542810029855047726,board_roi2_overview_conf_next",
                        Base = new
                        {
                            Extra = new
                            {
                                refer = "ecp,7532804468681916467,7542810029855047726,board_roi2_overview_conf_next"
                            }
                        }
                    };

                    string jsonData = JsonConvert.SerializeObject(requestBody, Formatting.None);
                    var content = new StringContent(jsonData, Encoding.UTF8, "application/json");

                    var qianchuanQueryString = string.Join("&", queryParams.Select(kvp => $"{Uri.EscapeDataString(kvp.Key)}={Uri.EscapeDataString(kvp.Value)}"));
                    var fullUrl = $"{qianchuanUrl}?{qianchuanQueryString}";

                    var qianchuanResponse = await _httpClient.PostAsync(fullUrl, content);
                    var qianchuanResponseText = await qianchuanResponse.Content.ReadAsStringAsync();

                    if (qianchuanResponse.IsSuccessStatusCode)
                    {
                        var jsonResponse = JObject.Parse(qianchuanResponseText);
                        if (jsonResponse["status_code"].ToString() == "0" && jsonResponse["message"].ToString() == "success" && jsonResponse["data"] != null)
                        {
                            var totals = jsonResponse["data"]["StatsData"]["Totals"] as JObject;
                            if (totals != null)
                            {
                                // 解析基础版数据
                                var basicData = JObject.Parse(responseText);
                                bool hasQianchuanData = false;
                                                        
                                // 处理整体支付ROI
                                var roiKey = "total_prepay_and_pay_order_realtime_roi2";
                                if (totals[roiKey] != null)
                                {
                                    var roiValue = totals[roiKey]["ValueStr"].ToString();
                                    basicData["整体支付ROI"] = roiValue;
                                    hasQianchuanData = true;
                                }

                                        // 处理千川消耗（投放金额）
                                        var costKey = "stat_cost_for_roi2";
                                        if (totals[costKey] != null)
                                        {
                                            var costValue = totals[costKey]["ValueStr"].ToString();
                                            basicData["千川消耗"] = costValue;
                                            hasQianchuanData = true;
                                        }

                                        // 处理退款金额
                                        var refundKey = "total_refund_order_gmv_for_roi2_1h_all";
                                if (totals[refundKey] != null)
                                {
                                    var refundValue = totals[refundKey]["ValueStr"].ToString();
                                    basicData["退款金额"] = refundValue;
                                    hasQianchuanData = true;
                                }
                                                        
                                // 处理整体消耗
                                var statCostKey = "stat_cost_for_roi2";
                                if (totals[statCostKey] != null)
                                {
                                    var statCostValue = totals[statCostKey]["ValueStr"].ToString();
                                    basicData["整体消耗"] = statCostValue;
                                    hasQianchuanData = true;
                                }
                                                        
                                // 处理净成交ROI
                                var settleRoiKey = "total_prepay_and_pay_settle_realtime_roi2_1h";
                                if (totals[settleRoiKey] != null)
                                {
                                    var settleRoiValue = totals[settleRoiKey]["ValueStr"].ToString();
                                    basicData["净成交ROI"] = settleRoiValue;
                                    hasQianchuanData = true;
                                }
                                                        
                                // 如果有千川数据，更新合并后的数据
                                if (hasQianchuanData)
                                {
                                    mergedResponseText = basicData.ToString();
                                    qianchuanDataMerged = true;
                                    FileUtils.LogRpa("千川数据（整体支付ROI、千川消耗、退款金额、整体消耗、净成交ROI）已合并到基础版数据中", "基础版-直播大屏");
                                }
                            }
                        }
                    }
                    } // end of else (anchorId不为空)
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川整体支付ROI数据异常: {ex.Message}", "基础版-直播大屏");
            }
            }
            else
            {
                FileUtils.LogRpa("千川数据获取条件不满足，跳过千川数据合并", "基础版-直播大屏");
            }
            
            // 写入实时数据（包含合并后的千川数据）
            JuliangDataHandle.writeRealTimeData(fillDataCollectArgs(mergedResponseText));
            
            // 保存汇总数据
            JuliangDataHandle.writeGatherDataBase(fillDataCollectArgs(mergedResponseText));
            
            FileUtils.LogRpa($"接口检测到，数据存储本地${(qianchuanDataMerged ? "已合并千川数据" : "仅基础版数据")}=={_anchorInfo?.AnchorName}", "基础版-直播大屏");
            // 逻辑完成：计数器减1（线程安全）
            Interlocked.Decrement(ref _pendingCoreTasks);
        }

        /// <summary>
        /// 获取商品支付金额数据
        /// </summary>
        public async Task<List<vo.ProductInfoVo>> GetProductPaymentData()
        {
            var productList = new List<vo.ProductInfoVo>();
            //try
            //{
            //    // 获取Cookie
            //    var cookie = GetCookieAsync().Result;
            //    if (string.IsNullOrEmpty(cookie))
            //    {
            //        FileUtils.LogRpa("无法获取Cookie，跳过商品支付金额获取", "商品支付金额");
            //        return productList;
            //    }

            //    // 创建帮助类并获取数据
            //    var helper = new ProductPaymentHelper(_httpClient, _roomId, cookie);
            //    productList = await helper.GetProductPaymentAmountAsync();
            //    var product = await helper.GetLiveScreenProductListAfterLiveAsync();


            //    FileUtils.LogRpa($"成功获取 {productList.Count} 个商品数据", "商品支付金额");
                return productList;
            //}
            //catch (Exception ex)
            //{
            //    FileUtils.LogRpa($"获取商品支付金额失败: {ex.Message}", "商品支付金额");
            //    return productList;
            //}
        }

        /// <summary>
        /// 获取浏览器Cookie
        /// </summary>
        private Task<string> GetCookieAsync()
        {
            // 直接使用已有的 LUOPAN_DT Cookie
            var cookieParts = new List<string>();
            if (!string.IsNullOrEmpty(_luopanDtCookie))
            {
                cookieParts.Add($"LUOPAN_DT={_luopanDtCookie}");
            }
            if (!string.IsNullOrEmpty(_sessionIdCookie))
            {
                cookieParts.Add($"sessionid={_sessionIdCookie}");
            }
            return Task.FromResult(string.Join("; ", cookieParts));
        }

        /// <summary>
        /// 千川全域大屏-核心概览看板
        /// </summary>
        public async Task QianchuanOverviewBoard()
        {
            try
            {
                // 检查是否能获取到aavid
                var aavid = GetAavidFromConfig();
                if (string.IsNullOrEmpty(aavid))
                {
                    FileUtils.LogRpa("配置文件中没有获取到aavid，不执行千川数据获取", "千川数据");
                    return;
                }
                
                // 首先获取千川账户列表
                await GetQianchuanAccountList();

                // 然后获取全域大屏数据
                await GetQianchuanStatData();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"千川数据获取异常: {ex.Message}", "千川数据");
            }
            finally
            {
                // 逻辑完成：计数器减1（线程安全）
                Interlocked.Decrement(ref _pendingCoreTasks);
            }
        }

        /// <summary>
        /// 获取千川账户列表
        /// </summary>
        private async Task GetQianchuanAccountList()
        {
            // sessionid已经在构造函数中添加到cookie容器中，无需重复添加
            
            var headers = new Dictionary<string, string>
            {
                { "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36" },
                { "Accept", "application/json, text/plain, */*" },
                { "sec-ch-ua-platform", "\"Windows\"" },
                { "sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"" },
                { "sec-ch-ua-mobile", "?0" },
                { "sec-fetch-site", "same-origin" },
                { "sec-fetch-mode", "cors" },
                { "sec-fetch-dest", "empty" },
                { "Referer", $"https://compass.jinritemai.com/qianchuan/check-login?roomId={_roomId}&source=luopan" },
                { "Accept-Language", "zh-CN,zh;q=0.9" },
                { "Priority", "u=1, i" }
            };

            _httpClient.DefaultRequestHeaders.Clear();
            foreach (var header in headers)
            {
                if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                {
                    FileUtils.log($"无法添加头信息: {header.Key}");
                }
            }

            var url = $"https://compass.jinritemai.com/ad/api/data/compass/get-account-list?roomId={_roomId}&gfversion=1.0.1.7967";

            try
            {
                var response = await _httpClient.GetAsync(url);
                var responseText = await response.Content.ReadAsStringAsync();

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.LogRpa($"HTTP 错误: {response.StatusCode}", "千川账户列表");
                    // 检查是否是授权失效
                    if (response.StatusCode == HttpStatusCode.Unauthorized || response.StatusCode == HttpStatusCode.Forbidden)
                    {
                        FileUtils.LogRpa("千川授权已失效", "千川账户列表");
                        // 发送短信提醒
                        if (_anchorInfo != null && !string.IsNullOrEmpty(_anchorInfo.AnchorName))
                        {
                            // _ = AnchorApi.SendQianchuanAuthExpireMsg(_anchorInfo.AnchorName); // TODO: 需要添加此方法
                        }
                    }
                    return;
                }

                var jsonResponse = JObject.Parse(responseText);
                if (jsonResponse["status_code"].ToString() == "0" && jsonResponse["message"].ToString() == "success" && jsonResponse["data"] != null)
                {
                    var userAccountInfos = jsonResponse["data"]["ecpAdvs"];
                    if (userAccountInfos != null)
                    {
                        foreach (var item in userAccountInfos)
                        {
                            var accountId = item["id"].ToString();
                            var accountName = item["name"].ToString();
                            FileUtils.LogRpa($"账户ID: {accountId}, 账户名称: {accountName}", "千川账户列表");
                        }
                    }
                }
                else
                {
                    FileUtils.LogRpa($"业务逻辑失败: {responseText}", "千川账户列表");
                    // 检查是否是授权失效
                    string message = jsonResponse["message"].ToString();
                    if (message.Contains("授权") || message.Contains("登录") || message.Contains("token"))
                    {
                        FileUtils.LogRpa("千川授权已失效", "千川账户列表");
                        // 发送短信提醒
                        if (_anchorInfo != null && !string.IsNullOrEmpty(_anchorInfo.AnchorName))
                        {
                            // _ = AnchorApi.SendQianchuanAuthExpireMsg(_anchorInfo.AnchorName); // TODO: 需要添加此方法
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川账户列表异常: {ex.Message}", "千川账户列表");
            }
        }

        /// <summary>
        /// 从文件中获取千川账户ID（aavid）
        /// </summary>
        /// <returns>aavid值</returns>
        private string GetAavidFromConfig()
        {
            try
            {
                if (_anchorInfo == null || string.IsNullOrEmpty(_anchorInfo.SecUid))
                {
                    return null;
                }
                
                // 生成缓存键
                string cacheKey = $"{_anchorInfo.SecUid}_aavid";
                
                // 检查缓存是否有效
                lock (_cacheLock)
                {
                    if (DateTime.Now - _lastCacheUpdateTime < _cacheExpiry && _cookieFileCache.ContainsKey(cacheKey))
                    {
                        return _cookieFileCache[cacheKey];
                    }
                }
                
                // 获取aavid文件路径（与巨量Cookie保存在同一文件夹中）
                string aavidPath = GetAavidPath(_anchorInfo.SecUid);
                if (File.Exists(aavidPath))
                {
                    string aavid = File.ReadAllText(aavidPath).Trim();
                    if (!string.IsNullOrEmpty(aavid))
                    {
                        // 更新缓存
                        lock (_cacheLock)
                        {
                            _cookieFileCache[cacheKey] = aavid;
                            _lastCacheUpdateTime = DateTime.Now;
                        }
                        return aavid;
                    }
                }
                
                // 更新缓存为null
                lock (_cacheLock)
                {
                    _cookieFileCache[cacheKey] = null;
                }
                
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取aavid配置异常: {ex.Message}", "千川数据");
                return null;
            }
        }
        
        /// <summary>
        /// 从文件中获取Cookie值
        /// </summary>
        /// <param name="cookieName">Cookie名称</param>
        /// <returns>Cookie值</returns>
        private string GetCookieFromFile(string cookieName)
        {
            try
            {
                if (_anchorInfo == null || string.IsNullOrEmpty(_anchorInfo.SecUid))
                {
                    return null;
                }
                
                // 生成缓存键
                string cacheKey = $"{_anchorInfo.SecUid}_{cookieName}";
                
                // 检查缓存是否有效
                lock (_cacheLock)
                {
                    if (DateTime.Now - _lastCacheUpdateTime < _cacheExpiry && _cookieFileCache.ContainsKey(cacheKey))
                    {
                        return _cookieFileCache[cacheKey];
                    }
                }
                
                // 获取Cookie文件路径
                string cookiePath = GetCookiePath(_anchorInfo.SecUid);
                if (File.Exists(cookiePath))
                {
                    // 读取Cookie文件内容
                    string cookieContent = File.ReadAllText(cookiePath);
                    
                    // 解析JSON格式的Cookie数据
                    try
                    {
                        // 使用juliang命名空间的CookieDto（与保存时保持一致）
                        var cookieDtos = Newtonsoft.Json.JsonConvert.DeserializeObject<List<ReviewAnalysis.juliang.CookieDto>>(cookieContent);
                        if (cookieDtos != null)
                        {
                            // 查找指定名称的Cookie
                            var cookieDto = cookieDtos.FirstOrDefault(c => c.Name == cookieName);
                            if (cookieDto != null)
                            {
                                string cookieValue = cookieDto.Value;
                                
                                // 更新缓存
                                lock (_cacheLock)
                                {
                                    _cookieFileCache[cacheKey] = cookieValue;
                                    _lastCacheUpdateTime = DateTime.Now;
                                }
                                
                                return cookieValue;
                            }
                        }
                    }
                    catch (Exception jsonEx)
                    {
                        FileUtils.LogRpa($"解析Cookie JSON异常: {jsonEx.Message}", "千川数据");
                        // 如果JSON解析失败，尝试使用旧的字符串搜索方法
                        if (cookieContent.Contains(cookieName))
                        {
                            // 简单的解析逻辑，作为后备
                            int startIndex = cookieContent.IndexOf(cookieName);
                            if (startIndex >= 0)
                            {
                                int valueStartIndex = cookieContent.IndexOf(':', startIndex) + 1;
                                int valueEndIndex = cookieContent.IndexOf(',', valueStartIndex);
                                if (valueEndIndex < 0)
                                {
                                    valueEndIndex = cookieContent.IndexOf('}', valueStartIndex);
                                }
                                if (valueStartIndex < valueEndIndex)
                                {
                                    string cookieValue = cookieContent.Substring(valueStartIndex, valueEndIndex - valueStartIndex).Trim();
                                    // 移除引号
                                    cookieValue = cookieValue.Trim('"');
                                    
                                    // 更新缓存
                                    lock (_cacheLock)
                                    {
                                        _cookieFileCache[cacheKey] = cookieValue;
                                        _lastCacheUpdateTime = DateTime.Now;
                                    }
                                    
                                    return cookieValue;
                                }
                            }
                        }
                    }
                }
                
                // 更新缓存为null
                lock (_cacheLock)
                {
                    _cookieFileCache[cacheKey] = null;
                }
                
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取Cookie异常: {ex.Message}", "千川数据");
                return null;
            }
        }
        
        /// <summary>
        /// 获取Cookie文件路径
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>Cookie文件路径</returns>
        private string GetCookiePath(string secUid)
        {
            try
            {
                string cachePath = Path.GetFullPath("dataCollect/config");
                string md5Str = GetCachePathMd5(secUid);
                return Path.Combine(cachePath, $"jlby-{md5Str}");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取Cookie文件路径异常: {ex.Message}", "千川数据");
                return null;
            }
        }

        #region 违规数据采集

        /// <summary>
        /// 采集违规数据（直播结束后调用）
        /// </summary>
        public async Task CollectViolationData()
        {
            try
            {
                // 1. 从Cookie文件读取COMPASS_LUOPAN_DT（罗盘Cookie）
                string luopanDt = GetCookieFromFile("COMPASS_LUOPAN_DT");
                if (string.IsNullOrEmpty(luopanDt))
                {
                    FileUtils.LogRpa("COMPASS_LUOPAN_DT为空，跳过违规数据采集", "违规采集");
                    return;
                }

                // 2. 获取违规警告列表
                string warningJson = api.ViolationApi.GetViolationWarnings(_config.RoomId, luopanDt);
                if (string.IsNullOrEmpty(warningJson))
                {
                    FileUtils.LogRpa("获取违规警告返回空，跳过违规数据采集", "违规采集");
                    return;
                }

                var warningObj = JObject.Parse(warningJson);
                var warnings = warningObj["data"]?["warning_infos"] as JArray;

                if (warnings == null || warnings.Count == 0)
                {
                    FileUtils.LogRpa("该直播间无违规记录", "违规采集");
                    return;
                }

                var violationList = new List<entity.JuliangViolationEntity>();
                var detailList = new List<entity.JuliangViolationDetailEntity>();

                // 3. 预读BUYIN_SASID（巨量百应Cookie），避免循环内重复读取
                string sasid = GetCookieFromFile("BUYIN_SASID");
                bool hasSasid = !string.IsNullOrEmpty(sasid);

                // 4. 遍历处理每条违规记录
                foreach (var warning in warnings)
                {
                    string penalizeId = warning["penalize_id"]?.ToString();
                    
                    var entity = new entity.JuliangViolationEntity
                    {
                        penalizeId = penalizeId,
                        punishLevel = warning["punish_level"]?.ToString(),
                        punishReason = warning["punish_reason"]?.ToString(),
                        punishResult = warning["punish_result"]?.ToString(),
                        punishSuggestion = warning["punish_suggestion"]?.ToString(),
                        punishTime = warning["punish_time"]?.Value<long?>(),
                        punishVideoCover = warning["punish_video"]?["punish_video_cover"]?.ToString(),
                        roomId = warning["punish_video"]?["room_id"]?.ToString()
                    };

                    // 获取视频链接
                    if (!string.IsNullOrEmpty(penalizeId))
                    {
                        entity.videoLink = api.ViolationApi.GetViolationVideoLink(_config.RoomId, penalizeId, luopanDt);
                    }

                    violationList.Add(entity);

                    // 5. 获取违规详情（需要巨量百应Cookie SASID）
                    if (hasSasid && !string.IsNullOrEmpty(penalizeId))
                    {
                        var details = CollectViolationDetail(penalizeId, sasid);
                        if (details != null && details.Count > 0)
                        {
                            detailList.AddRange(details);
                        }
                    }
                }

                // 5. 保存到汇总数据
                SaveViolationData(violationList, detailList);

                FileUtils.LogRpa($"违规数据采集完成：警告{violationList.Count}条，详情{detailList.Count}条", "违规采集");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"采集违规数据异常==={_config.RoomId}");
            }
            finally
            {
                // 无论违规采集成功失败，都尝试采集商品数据
                await CollectAndUploadProductDataAfterLive();
            }
        }

        #region 直播后商品数据采集

        /// <summary>
        /// 采集直播后商品数据并上传
        /// </summary>
        private async Task CollectAndUploadProductDataAfterLive()
        {
            try
            {
                FileUtils.LogRpa($"开始采集直播后商品数据，RoomId: {_config.RoomId}", "直播后商品采集");

                // 等待30秒确保数据已同步
                //await Task.Delay(30000);

                // 1. 获取罗盘Cookie
                string luopanDt = GetCookieFromFile("COMPASS_LUOPAN_DT");
                if (string.IsNullOrEmpty(luopanDt))
                {
                    FileUtils.LogRpa("COMPASS_LUOPAN_DT为空，跳过商品数据采集", "直播后商品采集");
                    return;
                }

                // 2. 构建cookie字符串（注意：API要求使用 LUOPAN_DT，不是 COMPASS_LUOPAN_DT）
                var cookieStr = $"LUOPAN_DT={luopanDt}";

                // 3. 调用API获取商品数据（使用完全独立的LiveScreenProductHelper，避免与其他方法耦合）
                var productHelper = new LiveScreenProductHelper(_config.RoomId, cookieStr);
                var products = await productHelper.GetLiveScreenProductListAfterLiveAsync();

                if (products == null || products.Count == 0)
                {
                    FileUtils.LogRpa("未获取到商品数据", "直播后商品采集");
                    return;
                }

                FileUtils.LogRpa($"获取到 {products.Count} 条商品数据，准备上传", "直播后商品采集");

                // 4. 转换为ProductDetailsBo并逐个上传
                await UploadProductDetailsBatch(products);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"采集直播后商品数据异常: {ex.Message}", "直播后商品采集");
            }
        }

        /// <summary>
        /// 批量上传商品详情数据（含趋势数据）
        /// </summary>
        private async Task UploadProductDetailsBatch(List<Dictionary<string, object>> products)
        {
            try
            {
                // cookie 只获取一次，放到循环外
                string luopanDt = GetCookieFromFile("COMPASS_LUOPAN_DT");
                string cookieStr = !string.IsNullOrEmpty(luopanDt) ? $"LUOPAN_DT={luopanDt}" : null;
                if (string.IsNullOrEmpty(cookieStr))
                {
                    FileUtils.LogRpa("COMPASS_LUOPAN_DT为空，将跳过趋势数据获取，仅提交商品基础数据", "直播后商品采集");
                }

                var productBoList = new List<ProductDetailsBo>();

                foreach (var product in products)
                {
                    try
                    {
                        string productId = GetValueFromProduct(product, "product_id")?.ToString();
                        if (string.IsNullOrEmpty(productId))
                        {
                            FileUtils.LogRpa("商品ID为空，跳过", "直播后商品采集");
                            continue;
                        }

                        // 尽力获取趋势数据，失败不影响提交
                        string statisticsCurve = null;
                        if (!string.IsNullOrEmpty(cookieStr))
                        {
                            try
                            {
                                var trendHelper = new ProductOverallTrendHelper(_config.RoomId, cookieStr);
                                var trendResult = await trendHelper.GetProductOverallTrendAsync(productId);
                                statisticsCurve = trendResult != null ? JsonConvert.SerializeObject(trendResult) : null;
                            }
                            catch (Exception trendEx)
                            {
                                FileUtils.LogRpa($"获取商品 {productId} 趋势数据失败: {trendEx.Message}，继续提交基础数据", "直播后商品采集");
                            }
                        }

                        // 无论趋势数据是否获取成功，都加入列表
                        var productBo = ConvertToProductDetailsBo(product, statisticsCurve);
                        if (productBo != null)
                        {
                            productBoList.Add(productBo);
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"转换单条商品数据异常: {ex.Message}", "直播后商品采集");
                    }
                }

                // 一次性批量上传
                if (productBoList.Count > 0)
                {
                    bool success = await Task.Run(() =>
                        ReplayHttpUtils.SaveProductDetailsBatch(productBoList)
                    );
                    FileUtils.LogRpa($"商品数据批量上传{(success ? "成功" : "失败")}，共 {productBoList.Count}/{products.Count} 条", "直播后商品采集");
                }
                else
                {
                    FileUtils.LogRpa("无有效商品数据可上传", "直播后商品采集");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"批量上传商品数据异常: {ex.Message}", "直播后商品采集");
            }
        }

        /// <summary>
        /// 获取商品趋势数据并序列化为JSON字符串
        /// </summary>
        private async Task<string> GetProductTrendData(string productId)
        {
            try
            {
                var trendData = await FetchProductTrendData(productId);

                if (trendData != null && trendData.Count > 0)
                {
                    return JsonConvert.SerializeObject(trendData);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取商品趋势数据异常: {ex.Message}", "直播后商品采集");
            }

            return null;
        }

        /// <summary>
        /// 调用API获取商品趋势数据
        /// </summary>
        private async Task<List<Dictionary<string, object>>> FetchProductTrendData(string productId)
        {
            var result = new List<Dictionary<string, object>>();

            try
            {
                var lid = GenLid();
                var fp = SignatureHelper.GetFp();
                var msToken = SignatureHelper.GetMsToken(172);
                var ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";

                var queryParams = new Dictionary<string, string>
                {
                    ["room_id"] = _config.RoomId,
                    ["product_id"] = productId,
                    ["_lid"] = lid,
                    ["verifyFp"] = fp,
                    ["fp"] = fp,
                    ["msToken"] = msToken
                };

                var paramsStr = BuildQueryString(queryParams);
                var aBogus = SignatureHelper.GetABogus(ua, paramsStr);
                if (!string.IsNullOrEmpty(aBogus))
                {
                    queryParams["a_bogus"] = aBogus;
                }

                // 使用HttpRequestMessage避免修改共享HttpClient
                var url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/product_overall_trend";
                var queryString = BuildQueryString(queryParams);
                var fullUrl = $"{url}?{queryString}";

                // 获取罗盘Cookie
                string luopanDt = GetCookieFromFile("COMPASS_LUOPAN_DT");
                
                // 使用独立HttpClient避免共享实例阻塞
                using (var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(10) })
                {
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("accept", "application/json, text/plain, */*");
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("cache-control", "no-cache");
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("pragma", "no-cache");
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("user-agent", ua);
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("referer", $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_config.RoomId}&live_app_id=1128");

                    if (!string.IsNullOrEmpty(luopanDt))
                    {
                        httpClient.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", $"LUOPAN_DT={luopanDt}");
                    }

                    var response = await httpClient.GetAsync(fullUrl);
                    var responseText = await response.Content.ReadAsStringAsync();

                    if (string.IsNullOrEmpty(responseText))
                    {
                        return result;
                    }

                    var jsonData = JObject.Parse(responseText);

                    // 解析左侧趋势数据
                    var leftTrends = jsonData["data"]?["left_trend"]?["trends"] as JArray;
                    if (leftTrends != null && leftTrends.Count > 0)
                    {
                        foreach (var trend in leftTrends)
                        {
                            var trendItem = new Dictionary<string, object>();
                            foreach (var prop in trend.Children<JProperty>())
                            {
                                trendItem[prop.Name] = prop.Value.ToObject<object>();
                            }
                            trendItem["trend_type"] = "left";
                            result.Add(trendItem);
                        }
                    }

                    // 解析右侧趋势数据
                    var rightTrends = jsonData["data"]?["right_trend"]?["trends"] as JArray;
                    if (rightTrends != null && rightTrends.Count > 0)
                    {
                        foreach (var trend in rightTrends)
                        {
                            var trendItem = new Dictionary<string, object>();
                            foreach (var prop in trend.Children<JProperty>())
                            {
                                trendItem[prop.Name] = prop.Value.ToObject<object>();
                            }
                            trendItem["trend_type"] = "right";
                            result.Add(trendItem);
                        }
                    }

                    // 解析讲解时间段列表
                    var explainList = jsonData["data"]?["explain_list"] as JArray;
                    if (explainList != null && explainList.Count > 0)
                    {
                        var explainItem = new Dictionary<string, object>();
                        explainItem["explain_list"] = explainList.ToObject<object>();
                        result.Add(explainItem);
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取商品趋势数据异常: {ex.Message}", "直播后商品采集");
            }

            return result;
        }

        /// <summary>
        /// 生成lid参数
        /// </summary>
        private string GenLid()
        {
            var timestamp = DateTimeOffset.Now.ToUnixTimeMilliseconds().ToString();
            var random = new Random();
            var randomPart = random.Next(10000).ToString("D4");
            return timestamp.Substring(0, 5) + randomPart;
        }

        /// <summary>
        /// 构建查询字符串
        /// </summary>
        private string BuildQueryString(Dictionary<string, string> parameters)
        {
            var sb = new StringBuilder();
            foreach (var param in parameters)
            {
                if (sb.Length > 0) sb.Append('&');
                sb.Append(Uri.EscapeDataString(param.Key));
                sb.Append('=');
                sb.Append(Uri.EscapeDataString(param.Value));
            }
            return sb.ToString();
        }

        /// <summary>
        /// 将API返回的数据转换为ProductDetailsBo（含趋势数据）
        /// </summary>
        private ProductDetailsBo ConvertToProductDetailsBo(Dictionary<string, object> product, string statisticsCurve)
        {
            try
            {
                var bo = new ProductDetailsBo
                {
                    batchNumber = _config.RoomId,
                    videoId = _juliangForm.videoId,
                    productId = GetValueFromProduct(product, "product_id")?.ToString(),
                    title = GetValueFromProduct(product, "title")?.ToString(),
                    imageUri = GetValueFromProduct(product, "image_uri")?.ToString(),
                    marketPrice = ConvertToDecimal(GetValueFromProduct(product, "market_price")),
                    productBindTime = ConvertTimestampToDateTime(GetValueFromProduct(product, "product_bind_time")),
                    explainCnt = ConvertToLong(GetValueFromProduct(product, "explain_cnt")),
                    productShowUcnt = ConvertToLong(GetValueFromProduct(product, "product_show_ucnt")),
                    productClickUcnt = ConvertToLong(GetValueFromProduct(product, "product_click_ucnt")),
                    productShowClickUcntRatio = ConvertToDecimal(GetValueFromProduct(product, "product_show_click_ucnt_ratio")),
                    productShowPayUcntRatio = ConvertToDecimal(GetValueFromProduct(product, "product_show_pay_ucnt_ratio")),
                    productClickPayUcntRatio = ConvertToDecimal(GetValueFromProduct(product, "product_click_pay_ucnt_ratio")),
                    gpm = ConvertToDecimal(GetValueFromProduct(product, "gpm")),
                    payAmt = ConvertToDecimal(GetValueFromProduct(product, "pay_amt")),
                    avgMaxPayAmtMin = ConvertToDecimal(GetValueFromProduct(product, "avg_max_pay_amt_min")),
                    payComboCnt = ConvertToLong(GetValueFromProduct(product, "pay_combo_cnt")),
                    payCnt = ConvertToLong(GetValueFromProduct(product, "pay_cnt")),
                    createCnt = ConvertToLong(GetValueFromProduct(product, "create_cnt")),
                    createPayUcntRatio = ConvertToDecimal(GetValueFromProduct(product, "create_pay_ucnt_ratio")),
                    payDepositPreOrderCnt = ConvertToLong(GetValueFromProduct(product, "pay_deposit_pre_order_cnt")),
                    presaleDepayDeamt = ConvertToDecimal(GetValueFromProduct(product, "presale_depay_deamt")),
                    payDepositPreOrderAmt = ConvertToDecimal(GetValueFromProduct(product, "pay_deposit_pre_order_amt")),
                    refundCnt = ConvertToLong(GetValueFromProduct(product, "refund_cnt")),
                    realRefundAmt = ConvertToDecimal(GetValueFromProduct(product, "real_refund_amt")),
                    refundRate = ConvertToDecimal(GetValueFromProduct(product, "refund_rate")),
                    statisticsCurve = statisticsCurve,
                    createDate = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss")
                };

                // 校验必需字段
                if (string.IsNullOrEmpty(bo.productId) || string.IsNullOrEmpty(bo.title))
                {
                    FileUtils.LogRpa($"商品数据缺少必需字段: productId={bo.productId}, title={bo.title}", "直播后商品采集");
                    return null;
                }

                return bo;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"转换ProductDetailsBo异常: {ex.Message}", "直播后商品采集");
                return null;
            }
        }

        /// <summary>
        /// 从商品数据中获取值（处理 {'value': xxx, 'unit': '...'} 格式）
        /// </summary>
        private object GetValueFromProduct(Dictionary<string, object> product, string key)
        {
            if (!product.ContainsKey(key))
                return null;

            var value = product[key];
            if (value == null)
                return null;

            // 处理字典格式 {'value': xxx, 'unit': '...'}
            if (value is Dictionary<string, object> dict && dict.ContainsKey("value"))
            {
                return dict["value"];
            }

            return value;
        }

        /// <summary>
        /// 转换为decimal
        /// </summary>
        private decimal? ConvertToDecimal(object value)
        {
            if (value == null) return null;
            if (decimal.TryParse(value.ToString(), out decimal result))
            {
                return result;
            }
            return null;
        }

        /// <summary>
        /// 转换为long
        /// </summary>
        private long? ConvertToLong(object value)
        {
            if (value == null) return null;
            if (long.TryParse(value.ToString(), out long result))
            {
                return result;
            }
            return null;
        }

        /// <summary>
        /// 转换为string
        /// </summary>
        private string ConvertToString(object value)
        {
            return value?.ToString();
        }

        /// <summary>
        /// 将时间戳（秒或毫秒）转换为 yyyy-MM-dd HH:mm:ss 格式
        /// </summary>
        private string ConvertTimestampToDateTime(object value)
        {
            if (value == null) return null;
            try
            {
                string str = value.ToString();
                if (string.IsNullOrEmpty(str) || str == "0") return null;
                long ts = long.Parse(str);
                if (ts <= 0) return null;
                // 毫秒级时间戳（13位）转秒级
                if (str.Length >= 13) ts = ts / 1000;
                var dt = DateTimeOffset.FromUnixTimeSeconds(ts).LocalDateTime;
                return dt.ToString("yyyy-MM-dd HH:mm:ss");
            }
            catch
            {
                return value.ToString();
            }
        }

        #endregion

        /// <summary>
        /// 采集违规详情
        /// </summary>
        private List<entity.JuliangViolationDetailEntity> CollectViolationDetail(string penalizeId, string sasid)
        {
            try
            {
                string detailJson = api.ViolationApi.GetViolationDetails(penalizeId, sasid);
                if (string.IsNullOrEmpty(detailJson))
                {
                    return null;
                }

                var detailObj = JObject.Parse(detailJson);
                var violations = detailObj["data"]?["violation_list"] as JArray;

                if (violations == null || violations.Count == 0)
                {
                    return null;
                }

                var list = new List<entity.JuliangViolationDetailEntity>();
                foreach (var v in violations)
                {
                    list.Add(new entity.JuliangViolationDetailEntity
                    {
                        taskId = v["task_id"]?.ToString(),
                        objectType = v["object_type"]?.Value<int?>(),
                        objectId = v["object_id"]?.ToString(),
                        violationStatus = v["violation_status"]?.Value<int?>(),
                        violationReason = v["violation_reason"]?.ToString(),
                        penalizeResult = v["penalize_result"]?.ToString(),
                        appealStatus = v["appeal_detail"]?["appeal_status"]?.Value<int?>(),
                        penalizeTime = v["penalize_time"]?.Value<long?>(),
                        violationDesc = v["violation_desc"]?.ToString(),
                        proofPoint = v["violation_content"]?["proof_point"]?.ToString(),
                        proofSentence = v["violation_content"]?["proof_sentence"]?.ToString(),
                        videoUrl = v["violation_content"]?["video_list"]?[0]?["video_url"]?.ToString(),
                        coverImg = v["violation_content"]?["video_list"]?[0]?["cover_img"]?.ToString(),
                        isLivingCut = v["violation_content"]?["video_list"]?[0]?["is_living_cut"]?.Value<bool?>(),
                        liveVideoStartTime = v["violation_content"]?["video_list"]?[0]?["live_video_start_time"]?.Value<long?>(),
                        liveVideoEndTime = v["violation_content"]?["video_list"]?[0]?["live_video_end_time"]?.Value<long?>(),
                        suggestion = v["revise_suggestion"]?["suggestion"]?.ToString()
                    });
                }
                return list;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"采集违规详情异常==={penalizeId}");
                return null;
            }
        }

        /// <summary>
        /// 保存违规数据到汇总实体
        /// </summary>
        private void SaveViolationData(List<entity.JuliangViolationEntity> violations, List<entity.JuliangViolationDetailEntity> details)
        {
            try
            {
                // 获取汇总数据文件路径
                string filePath = Path.Combine(Path.GetFullPath("dataCollect/juliang/finish"), $"{_config.VideoId}.txt");
                
                // 读取现有的汇总数据
                JuliangGatherDataEntity gatherData;
                if (File.Exists(filePath))
                {
                    string objJsonStr = File.ReadAllText(filePath, Encoding.UTF8);
                    if (!string.IsNullOrEmpty(objJsonStr))
                    {
                        gatherData = JsonConvert.DeserializeObject<JuliangGatherDataEntity>(objJsonStr);
                    }
                    else
                    {
                        gatherData = new JuliangGatherDataEntity();
                        gatherData.isTakeProduct = 1;
                    }
                }
                else
                {
                    gatherData = new JuliangGatherDataEntity();
                    gatherData.isTakeProduct = 1;
                }

                // 设置违规数据（追加而非覆盖）
                gatherData.videoId = _config.VideoId;
                gatherData.secUid = _config.AnchorInfo?.SecUid;
                gatherData.batchNumber = _config.RoomId;
                
                // 如果已有违规数据，追加新数据
                if (gatherData.violationList == null)
                {
                    gatherData.violationList = violations;
                }
                else
                {
                    // 去重：根据penalizeId去重
                    var existingIds = new HashSet<string>(gatherData.violationList.Select(v => v.penalizeId));
                    foreach (var v in violations)
                    {
                        if (!existingIds.Contains(v.penalizeId))
                        {
                            gatherData.violationList.Add(v);
                        }
                    }
                }
                
                if (gatherData.violationDetailList == null)
                {
                    gatherData.violationDetailList = details;
                }
                else
                {
                    // 去重：根据taskId去重
                    var existingIds = new HashSet<string>(gatherData.violationDetailList.Select(v => v.taskId));
                    foreach (var d in details)
                    {
                        if (!existingIds.Contains(d.taskId))
                        {
                            gatherData.violationDetailList.Add(d);
                        }
                    }
                }
                
                gatherData.hasViolation = gatherData.violationList?.Count > 0 ? 1 : 0;

                // 创建目录（如果不存在）
                string directory = Path.GetDirectoryName(filePath);
                if (!Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                // 保存到汇总数据文件
                string json = JsonConvert.SerializeObject(gatherData);
                File.WriteAllText(filePath, json, Encoding.UTF8);

                FileUtils.LogRpa($"违规数据已合并到汇总数据: {filePath}", "违规采集");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"保存违规数据异常==={_config.VideoId}");
            }
        }

        #endregion
        
        /// <summary>
        /// 从千川模块获取Cookie
        /// </summary>
        /// <param name="cookieName">Cookie名称</param>
        /// <returns>Cookie值</returns>
        private string GetQianchuanCookie(string cookieName)
        {
            try
            {
                if (_anchorInfo == null || string.IsNullOrEmpty(_anchorInfo.SecUid))
                {
                    return null;
                }

                string cookiePath = QianchuanUtils.getCookiePath(_anchorInfo.SecUid);
                if (!File.Exists(cookiePath))
                {
                    FileUtils.LogRpa($"千川Cookie文件不存在: {cookiePath}", "千川数据");
                    return null;
                }

                var localCookies = new CookiePersistenceHelper(cookiePath).LoadCefCookiesFromLocal(false);
                if (localCookies == null || localCookies.Count == 0)
                {
                    return null;
                }

                var cookie = localCookies.FirstOrDefault(c => c.Name == cookieName);
                return cookie?.Value;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从千川模块获取Cookie异常: {ex.Message}", "千川数据");
                return null;
            }
        }

        /// <summary>
        /// 从千川模块获取aavid
        /// </summary>
        /// <returns>aavid值</returns>
        private string GetAavidFromQianchuan()
        {
            try
            {
                if (_anchorInfo == null || string.IsNullOrEmpty(_anchorInfo.SecUid))
                {
                    return null;
                }

                string cachePath = Path.GetFullPath("dataCollect\\config");
                string md5Str = QianchuanUtils.getCachePathMd5(_anchorInfo.SecUid);
                string aavidPath = Path.Combine(cachePath, $"qcaavid-{md5Str}");

                if (File.Exists(aavidPath))
                {
                    string aavid = File.ReadAllText(aavidPath).Trim();
                    if (!string.IsNullOrEmpty(aavid))
                    {
                        FileUtils.LogRpa($"从千川模块获取aavid成功: {aavid}", "千川数据");
                        return aavid;
                    }
                }

                FileUtils.LogRpa("千川aavid文件不存在或为空", "千川数据");
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从千川模块获取aavid异常: {ex.Message}", "千川数据");
                return null;
            }
        }

        /// <summary>
        /// 检查是否可以获取千川数据
        /// </summary>
        /// <returns>true-可以获取，false-不可以</returns>
        private bool CanFetchQianchuanData()
        {
            // 1. 检查千川Cookie文件是否存在
            string qianchuanCookiePath = QianchuanUtils.getCookiePath(_anchorInfo.SecUid);
            if (!File.Exists(qianchuanCookiePath))
            {
                FileUtils.LogRpa("千川Cookie文件不存在，跳过千川数据获取", "千川数据");
                return false;
            }

            // 2. 检查sessionid是否存在且有效
            var cookies = new CookiePersistenceHelper(qianchuanCookiePath).LoadCefCookiesFromLocal(false);
            if (cookies == null || cookies.Count == 0)
            {
                FileUtils.LogRpa("千川Cookie文件为空，跳过千川数据获取", "千川数据");
                return false;
            }

            var sessionCookie = cookies.FirstOrDefault(c => c.Name == "sessionid");
            if (sessionCookie == null || string.IsNullOrWhiteSpace(sessionCookie.Value))
            {
                FileUtils.LogRpa("千川sessionid不存在，跳过千川数据获取", "千川数据");
                return false;
            }
            if (sessionCookie.Expires < DateTime.Now)
            {
                FileUtils.LogRpa("千川sessionid已过期，跳过千川数据获取", "千川数据");
                return false;
            }

            // 3. 检查aavid是否存在
            string aavid = GetAavidFromQianchuan();
            if (string.IsNullOrEmpty(aavid))
            {
                FileUtils.LogRpa("千川aavid不存在，跳过千川数据获取", "千川数据");
                return false;
            }

            FileUtils.LogRpa("千川数据获取条件满足，准备获取数据", "千川数据");
            return true;
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
                string cachePath = Path.GetFullPath("dataCollect/config");
                string md5Str = GetCachePathMd5(secUid);
                return Path.Combine(cachePath, $"qcaavid-{md5Str}");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取aavid文件路径异常: {ex.Message}", "千川数据");
                return null;
            }
        }
        
        /// <summary>
        /// 获取SecUid的MD5值，用于生成缓存路径
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>MD5值</returns>
        private string GetCachePathMd5(string secUid)
        {
            try
            {
                // 与JuliangUtils中的实现保持一致，使用租户ID、用户ID和SecUid生成MD5
                return MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}-{secUid}");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"生成MD5异常: {ex.Message}", "千川数据");
                return secUid.GetHashCode().ToString();
            }
        }
        
        /// <summary>
        /// 获取千川全域大屏数据
        /// </summary>
        private async Task GetQianchuanStatData()
        {
            // sessionid已经在构造函数中添加到cookie容器中，无需重复添加
            
            // 从配置文件中获取aavid
            var aavid = GetAavidFromConfig();
            // 这里不需要检查aavid是否为空，因为QianchuanOverviewBoard方法已经检查过了
            // 使用_anchorInfo.AnchorUserId作为anchorId
            var anchorId = _anchorInfo?.AnchorUserId;
            if (string.IsNullOrEmpty(anchorId))
            {
                // 如果AnchorUserId为空，使用默认值
                anchorId = "3160467642781443"; // 主播uid
                FileUtils.LogRpa("未找到anchorId配置，使用默认值", "千川数据");
            }

            var indexNameData = new Dictionary<string, string>
            {
                { "board_roi2_overview_conf_next", "全域大屏-核心概览看板" },
                { "total_order_settle_count_realtime_for_roi2_1h", "净成交订单数" },
                { "total_cost_per_pay_order_settle_realtime_for_roi2_1h", "净成交订单成本" },
                { "total_order_real_settle_amount_realtime_for_roi2_1h", "用户实际支付净成交金额" },
                { "total_order_settle_amount_rate_realtime_for_roi2_1h", "净成交金额结算率" },
                { "total_order_settle_count_rate_realtime_for_roi2_1h", "净成交订单结算率" },
                { "total_refund_order_count_for_roi2_1h", "1小时内退款订单数" },
                { "total_refund_order_gmv_for_roi2_1h_all", "1小时内退款金额" },
                { "total_refund_order_gmv_for_roi2_1h_rate", "1小时内退款率" },
                { "total_pay_order_count_realtime_for_roi2", "整体成交订单数" },
                { "total_live_pay_order_gpm_realtime_for_roi2", "GPM" },
                { "live_watch_to_pay_rate_for_roi2", "观看成交转化率" },
                { "total_cost_per_pay_order_realtime_for_roi2", "整体成交订单成本" },
                { "total_pay_order_gmv_realtime_for_roi2", "用户实际支付金额" },
                { "total_pay_order_coupon_amount_realtime_for_roi2", "整体成交智能优惠券金额" },
                { "total_ecom_platform_subsidy_amount_realtime_for_roi2", "电商平台补贴金额" },
                { "total_unfinished_estimate_order_gmv_realtime_for_roi2", "整体未完结预售订单预估金额" },
                { "total_refund_order_amount_for_roi2_90d_all", "直播间退款金额" },
                { "total_refund_order_amount_rate_for_roi2_90d_all", "直播间退款率" },
                { "live_online_user_count", "实时在线人数" },
                { "live_show_count_for_roi2", "直播间整体曝光次数" },
                { "total_show_to_watch_rate_for_roi2", "曝光观看率(次数)" },
                { "live_watch_ucount_for_roi2", "直播间整体观看人数" },
                { "live_duration_avg_ecom_for_roi2", "直播间平均停留时长(整场)" },
                { "live_follow_count_for_roi2", "直播间整体新增粉丝数" },
                { "live_comment_count_for_roi2", "直播间评论次数" },
                { "live_product_show_count_for_roi2", "直播间商品曝光次数" },
                { "live_product_click_count_for_roi2", "直播间商品点击次数" },
                { "live_share_count_for_roi2", "分享次数" },
                { "total_live_share_rate_for_roi2", "分享率" },
                { "live_gift_count_for_roi2", "打赏次数" },
                { "total_live_like_rate_for_roi2", "点赞率" },
                { "stat_cost_for_roi2", "整体消耗" },
                { "total_prepay_and_pay_order_realtime_roi2", "整体支付ROI" },
                { "total_pay_order_gmv_include_coupon_realtime_for_roi2", "整体成交金额" },
                { "total_prepay_and_pay_settle_realtime_roi2_1h", "净成交ROI" },
                { "total_order_settle_amount_realtime_for_roi2_1h", "净成交金额" }
            };

            var headers = new Dictionary<string, string>
            {
                { "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36" },
                { "Accept", "application/json, text/plain, */*" },
                { "Content-Type", "application/json" },
                { "sec-ch-ua-platform", "\"Windows\"" },
                { "sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"" },
                { "sec-ch-ua-mobile", "?0" },
                { "x-secsdk-csrf-token", "DOWNGRADE" },
                { "Origin", "https://qianchuan.jinritemai.com" },
                { "Sec-Fetch-Site", "same-origin" },
                { "Sec-Fetch-Mode", "cors" },
                { "Sec-Fetch-Dest", "empty" },
                { "Referer", $"https://qianchuan.jinritemai.com/board-next?live_room_id={_roomId}&aavid={aavid}&ad_origin=1&fromScene=luopan" },
                { "Accept-Language", "zh-CN,zh;q=0.9" },
                { "Priority", "u=1, i" }
            };

            _httpClient.DefaultRequestHeaders.Clear();
            foreach (var header in headers)
            {
                if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                {
                    FileUtils.log($"无法添加头信息: {header.Key}");
                }
            }

            var url = "https://qianchuan.jinritemai.com/ad/api/data/v1/common/statQuery";
            var queryParams = new Dictionary<string, string>
            {
                { "reqFrom", "commonMetricCard" },
                { "aavid", aavid },
                { "gfversion", "1.0.0.294" }
            };

            var requestBody = new
            {
                DataSetKey = "board_roi2_overview_conf_next",
                Metrics = new[]
                {
                    "total_pay_order_count_realtime_for_roi2",
                    "total_live_pay_order_gpm_realtime_for_roi2",
                    "live_watch_to_pay_rate_for_roi2",
                    "total_cost_per_pay_order_realtime_for_roi2",
                    "live_online_user_count",
                    "total_show_to_watch_rate_for_roi2",
                    "live_watch_ucount_for_roi2",
                    "stat_cost_for_roi2",
                    "total_prepay_and_pay_order_realtime_roi2",
                    "total_pay_order_gmv_include_coupon_realtime_for_roi2"
                },
                Dimensions = Array.Empty<object>(),
                PageParams = new
                {
                    Offset = 0,
                    Limit = -1
                },
                Filters = new
                {
                    ConditionRelationshipType = 1,
                    Conditions = new[]
                    {
                        new { Field = "advertiser_id", Operator = 7, Values = new[] { aavid } },
                        new { Field = "room_id", Operator = 7, Values = new[] { _roomId } },
                        new { Field = "anchor_id", Operator = 7, Values = new[] { anchorId } }
                    }
                },
                refer = "ecp,7532804468681916467,7542810029855047726,board_roi2_overview_conf_next",
                Base = new
                {
                    Extra = new
                    {
                        refer = "ecp,7532804468681916467,7542810029855047726,board_roi2_overview_conf_next"
                    }
                }
            };

            string jsonData = JsonConvert.SerializeObject(requestBody, Formatting.None);
            var content = new StringContent(jsonData, Encoding.UTF8, "application/json");

            var queryString = string.Join("&", queryParams.Select(kvp => $"{Uri.EscapeDataString(kvp.Key)}={Uri.EscapeDataString(kvp.Value)}"));
            var fullUrl = $"{url}?{queryString}";

            try
            {
                var response = await _httpClient.PostAsync(fullUrl, content);
                var responseText = await response.Content.ReadAsStringAsync();

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.LogRpa($"HTTP 错误: {response.StatusCode}", "千川全域大屏");
                    // 检查是否是授权失效
                    if (response.StatusCode == HttpStatusCode.Unauthorized || response.StatusCode == HttpStatusCode.Forbidden)
                    {
                        FileUtils.LogRpa("千川授权已失效", "千川全域大屏");
                        // 发送短信提醒
                        if (_anchorInfo != null && !string.IsNullOrEmpty(_anchorInfo.AnchorName))
                        {
                            // _ = AnchorApi.SendQianchuanAuthExpireMsg(_anchorInfo.AnchorName); // TODO: 需要添加此方法
                        }
                    }
                    return;
                }

                var jsonResponse = JObject.Parse(responseText);
                if (jsonResponse["status_code"].ToString() == "0" && jsonResponse["message"].ToString() == "success" && jsonResponse["data"] != null)
                {
                    var totals = jsonResponse["data"]["StatsData"]["Totals"] as JObject;
                    if (totals != null)
                    {
                        var result = new Dictionary<string, string>();
                        foreach (var item in totals.Properties())
                        {
                            var key = item.Name;
                            var value = item.Value["ValueStr"].ToString();
                            if (indexNameData.ContainsKey(key))
                            {
                                result[indexNameData[key]] = value;
                            }
                            else
                            {
                                result[key] = value;
                            }
                        }

                        // 存储数据
                        var dataArgs = fillDataCollectArgs(JsonConvert.SerializeObject(result));
                        //JuliangDataHandle.writeRealTimeData(dataArgs);
                        JuliangDataHandle.writeGatherDataBase(dataArgs);

                        FileUtils.LogRpa($"千川全域大屏数据获取成功", "千川全域大屏");
                    }
                }
                else
                {
                    FileUtils.LogRpa($"业务逻辑失败: {responseText}", "千川全域大屏");
                    // 检查是否是授权失效
                    string message = jsonResponse["message"].ToString();
                    if (message.Contains("授权") || message.Contains("登录") || message.Contains("token"))
                    {
                        FileUtils.LogRpa("千川授权已失效", "千川全域大屏");
                        // 发送短信提醒
                        if (_anchorInfo != null && !string.IsNullOrEmpty(_anchorInfo.AnchorName))
                        {
                            // _ = AnchorApi.SendQianchuanAuthExpireMsg(_anchorInfo.AnchorName); // TODO: 需要添加此方法
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川全域大屏数据异常: {ex.Message}", "千川全域大屏");
            }
        }
    }
}
