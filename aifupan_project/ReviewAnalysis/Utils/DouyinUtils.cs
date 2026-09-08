using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using CefSharp;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.qianchuan;
using douyin.Utils;

namespace ReviewAnalysis.Utils
{
    /// <summary>
    /// 抖音工具类
    /// 提供抖音 Cookie 路径管理、有效性检查和加载功能
    /// </summary>
    public class DouyinUtils
    {
        /// <summary>
        /// 获取抖音 Cookie 文件路径
        /// </summary>
        /// <param name="secUid">抖音用户ID</param>
        /// <returns>Cookie 文件完整路径</returns>
        public static string getCookiePath(string secUid)
        {
            try
            {
                string cachePath = Path.GetFullPath("dataCollect\\config");
                string md5Str = getCachePathMd5(secUid);
                return Path.Combine(cachePath, $"douyin-{md5Str}");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"获取 Cookie 路径异常：{ex.Message}", "抖音工具");
                return null;
            }
        }

        /// <summary>
        /// 获取缓存路径 MD5（与千川、巨量保持一致）
        /// </summary>
        /// <param name="secUid">抖音用户ID</param>
        /// <returns>MD5 字符串</returns>
        public static string getCachePathMd5(string secUid)
        {
            return MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}-{secUid}");
        }

        /// <summary>
        /// 检查 Cookie 是否有效
        /// </summary>
        /// <param name="secUid">抖音用户ID</param>
        /// <returns>true-有效，false-无效或不存在</returns>
        public static bool CheckCookieValid(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                    return false;

                // 读取 JSON 文件并检查关键 Cookie
                string json = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<Cookie>>(json);
                if (cookies == null || cookies.Count == 0)
                    return false;

                // 检查关键 Cookie（sessionid）
                var sessionCookie = cookies.Find(c => c.Name.Contains("sessionid"));
                return sessionCookie != null;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"检查 Cookie 有效性异常：{ex.Message}", "抖音工具");
                return false;
            }
        }

        /// <summary>
        /// 加载 Cookie 到浏览器
        /// </summary>
        /// <param name="secUid">抖音用户ID</param>
        /// <param name="browser">CefSharp 浏览器实例</param>
        public static async Task LoadCookiesToBrowser(string secUid, IWebBrowser browser)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                    return;

                // 读取 JSON 文件
                string json = File.ReadAllText(cookiePath);
                var localCookies = JsonConvert.DeserializeObject<List<Cookie>>(json);
                if (localCookies == null || localCookies.Count == 0)
                    return;

                var cookieManager = browser.GetCookieManager();
                foreach (var cookie in localCookies)
                {
                    string domain = cookie.Domain.StartsWith(".") ? cookie.Domain.Substring(1) : cookie.Domain;
                    await cookieManager.SetCookieAsync($"https://{domain}", cookie);
                }
                FileUtils.log($"Cookie 已加载到浏览器：{localCookies.Count} 个", "抖音工具");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"加载 Cookie 异常：{ex.Message}", "抖音工具");
            }
        }

        /// <summary>
        /// 从本地获取抖音 Cookie 字典
        /// </summary>
        /// <param name="secUid">抖音用户ID</param>
        /// <returns>Cookie 字典</returns>
        public static Dictionary<string, string> GetCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                    return new Dictionary<string, string>();

                var localCookies = new CookiePersistenceHelper(cookiePath).LoadCefCookiesFromLocal(false);
                if (localCookies == null || localCookies.Count == 0)
                    return new Dictionary<string, string>();

                Dictionary<string, string> cookies = new Dictionary<string, string>();
                foreach (var cookie in localCookies)
                {
                    cookies[cookie.Name] = cookie.Value;
                }
                return cookies;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"获取 Cookie 异常：{ex.Message}", "抖音工具");
                return new Dictionary<string, string>();
            }
        }
    }

    /// <summary>
    /// 抖音 Cookie 收集器
    /// 用于遍历和收集 douyin.com 域名的 Cookie
    /// </summary>
    public class DouyinCookieCollector : ICookieVisitor
    {
        private readonly List<Cookie> _cookies;

        /// <summary>
        /// 构造函数
        /// </summary>
        /// <param name="cookies">Cookie 列表</param>
        public DouyinCookieCollector(List<Cookie> cookies)
        {
            _cookies = cookies;
        }

        /// <summary>
        /// 访问每个 Cookie
        /// </summary>
        public bool Visit(Cookie cookie, int count, int total, ref bool deleteCookie)
        {
            // 只保留 douyin.com 域名的 Cookie
            if (cookie.Domain.Contains("douyin.com"))
            {
                _cookies.Add(cookie);
            }
            deleteCookie = false;
            return true; // 继续遍历
        }

        /// <summary>
        /// 释放资源
        /// </summary>
        public void Dispose()
        {
            // 无需额外清理
        }
    }
}
