using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using CefSharp;
using CefSharp.WinForms;
using Newtonsoft.Json;
using douyin.Utils;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils.kuaishou;

namespace ReviewAnalysis.Utils
{
    /// <summary>
    /// 快手工具类
    /// </summary>
    public class KuaishouUtils
    {
        /// <summary>
        /// 获取快手 Cookie 文件路径
        /// </summary>
        /// <param name="secUid">快手原始用户ID</param>
        /// <returns>Cookie 文件完整路径</returns>
        public static string getCookiePath(string secUid)
        {
            try
            {
                string cachePath = Path.GetFullPath("dataCollect\\config");
                string md5Str = getCachePathMd5(secUid);
                return Path.Combine(cachePath, $"kuaishou-{md5Str}");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"获取 Cookie 路径异常：{ex.Message}", "快手工具");
                return null;
            }
        }

        /// <summary>
        /// 获取缓存路径 MD5（与千川、巨量、抖音保持一致）
        /// </summary>
        /// <param name="secUid">快手原始用户ID</param>
        /// <returns>MD5 字符串</returns>
        public static string getCachePathMd5(string secUid)
        {
            return MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}-{secUid}");
        }

        /// <summary>
        /// 检查 Cookie 文件是否有效
        /// </summary>
        /// <param name="secUid">快手原始用户ID</param>
        /// <returns>是否有效</returns>
        public static bool CheckCookieValid(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return false;
                }

                string json = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<Cookie>>(json);
                if (cookies == null || cookies.Count < 5)
                {
                    return false;
                }

                // Cookie 数量大于 5 个算有效
                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检查快手 Cookie 有效性异常：{ex.Message}", "快手授权");
                return false;
            }
        }

        /// <summary>
        /// 加载 Cookie 到浏览器
        /// </summary>
        /// <param name="secUid">快手原始用户ID</param>
        /// <param name="browser">浏览器实例</param>
        public static async Task LoadCookiesToBrowser(string secUid, ChromiumWebBrowser browser)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return;
                }

                string json = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<Cookie>>(json);
                if (cookies == null || cookies.Count == 0)
                {
                    return;
                }

                var cookieManager = browser.GetCookieManager();
                foreach (var cookie in cookies)
                {
                    if (!string.IsNullOrEmpty(cookie.Domain))
                    {
                        await cookieManager.SetCookieAsync($"https://{cookie.Domain}", cookie);
                    }
                }

                FileUtils.LogRpa($"快手 Cookie 已加载：{cookies.Count} 个", "快手授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"加载快手 Cookie 异常：{ex.Message}", "快手授权");
            }
        }

        /// <summary>
        /// 从 URL 中提取快手 webId 或 customId
        /// </summary>
        /// <param name="url">URL</param>
        /// <returns>webId 或 customId</returns>
        public static string ExtractKuaishouIdFromUrl(string url)
        {
            try
            {
                // URL 格式：
                // https://live.kuaishou.com/u/3x123456
                // https://live.kuaishou.com/profile/3x123456
                // https://www.kuaishou.com/profile/3x123456
                
                var match = System.Text.RegularExpressions.Regex.Match(url, @"/(u|profile)/([^/?#]+)");
                if (match.Success)
                {
                    return match.Groups[2].Value;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从 URL 提取快手 ID 异常：{ex.Message}", "快手授权");
            }
            return null;
        }

        /// <summary>
        /// 获取快手原始用户ID（secUid）
        /// </summary>
        /// <param name="webIdOrCustomId">网页ID或自定义ID</param>
        /// <returns>secUid</returns>
        public static async Task<string> GetSecUidByWebId(string webIdOrCustomId)
        {
            try
            {
                var client = KuaiShouRequestClient.Instance;
                string secUid = await client.getOriginUserIdByWebIdOrCustomId(webIdOrCustomId);
                return secUid;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取快手 secUid 异常：{ex.Message}", "快手授权");
                return null;
            }
        }
    }
}
