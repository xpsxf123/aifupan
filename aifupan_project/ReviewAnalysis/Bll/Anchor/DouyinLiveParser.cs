using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using douyin.Utils;
using System.Security.Policy;
using ReviewAnalysis.api;
using ReviewAnalysis.vo.proxyIP;
using Newtonsoft.Json;
using static System.Windows.Forms.VisualStyles.VisualStyleElement.StartPanel;
using CefSharp.DevTools.IO;
using System.Net.Http.Headers;
using System.Net.Sockets;

namespace ReviewAnalysis.Bll.Anchor
{
    public class DouyinLiveParser
    {
        // 字符集，用于 encrypt3 函数
        private static readonly string Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-./";

        private static uint Encrypt(uint hashVal, string str)
        {
            foreach (var ch in str)
            {
                hashVal = ((hashVal ^ ch) * 65599) & 0xFFFFFFFF;
            }
            return hashVal;
        }

        private static uint Encrypt2(uint val, string str)
        {
            foreach (var ch in str)
            {
                val = (val * 65599 + ch) & 0xFFFFFFFF;
            }
            return val;
        }

        private static string Encrypt3(uint num)
        {
            var res = new StringBuilder();
            for (var i = 4; i >= 0; i--)
            {
                res.Append(Chars[(int)((num >> (i * 6)) & 63)]);
            }
            return res.ToString();
        }

        // 获取签名函数 
        public static string GetAcSignature(string ua, string acNonce)
        {
            var timestampInt = (int)(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() / 1000);
            var timestampStr = timestampInt.ToString();
            var timestampNumber = Encrypt(0, timestampStr);
            var urlNumber = Encrypt(timestampNumber, "www.douyin.com");
            var a1 = (timestampInt ^ ((urlNumber % 65521) * 65521)) & 0xFFFFFFFF;
            var bA1 = Convert.ToString(a1, 2);
            var bA2 = "10000000110000" + new string('0', 32 - bA1.Length) + bA1;
            var a2 = Convert.ToUInt64(bA2, 2);
            var a2Str = a2.ToString();
            var a2Number = Encrypt(0, a2Str);
            var str1 = Encrypt3((uint)(a2 >> 2));
            var r1 = (a2 / 4294967296) & 0xFFFFFFFF;
            var r2 = ((a2 << 28) | (r1 >> 4)) & 0xFFFFFFFF;
            var str2 = Encrypt3((uint)r2);
            var r3 = (2010578131 ^ a2) & 0xFFFFFFFF;
            var r4 = ((r1 << 26) | (r3 >> 6)) & 0xFFFFFFFF;
            var str3 = Encrypt3((uint)r4);
            var str3Last = Chars[(int)(r3 & 63)];
            var uaNumber = Encrypt((uint)a2Number, ua);
            var acNonceNumber = Encrypt((uint)a2Number, acNonce);
            var r5 = ((uaNumber % 65521) << 16) & 0xFFFFFFFF;
            var r6 = (r5 | (acNonceNumber % 65521)) & 0xFFFFFFFF;
            var str4 = Encrypt3((uint)(r6 >> 2));
            var r7 = ((r6 << 28) | ((524576 ^ a2) >> 4)) & 0xFFFFFFFF;
            var str5 = Encrypt3((uint)r7);
            var str6 = Encrypt3((uint)(urlNumber % 65521));
            var str7 = "_02B4Z6wo00f01" + str1 + str2 + str3 + str3Last + str4 + str5 + str6;
            var str8 = (Encrypt2(0, str7) & 0xFFFFFFFF).ToString("x8").Substring(6);
            var acSignature = str7 + str8;
            return acSignature;
        }

        /// <summary>
        /// 根据主播主页地址获取主播抖音号
        /// </summary>
        /// <param name="anchorHomeUrl">主播主页地址</param>
        /// <returns></returns>
        public static async Task<string> getAnchorNum(string anchorHomeUrl, bool useProxy = false)
        {
            try
            {

                HttpClientHandler handler = new HttpClientHandler
                {
                    UseCookies = true,
                    CookieContainer = new CookieContainer()
                };

                using (HttpClient client = new HttpClient(handler))
                {
                    // 第一次请求获取__ac_nonce
                    InitializeDefaultHeadersForClient(client);
                    var firstResponse = await client.GetAsync(anchorHomeUrl);
                    var cookies = GetCookies(firstResponse.Headers);

                    if (!cookies.TryGetValue("__ac_nonce", out var acNonce))
                    {
                        FileUtils.LogError($"{firstResponse}", $"采集douyin信息异常--不存在__ac_nonce");
                        return null;
                    }

                    // 生成__ac_signature
                    var ua = client.DefaultRequestHeaders.UserAgent.ToString();
                    var acSignature = GetAcSignature(ua, acNonce);

                    // 创建新的 HttpClient 实例以避免历史 Cookie（或清空当前实例的默认 Cookie）
                    var newHandler = new HttpClientHandler
                    {
                        AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                        UseCookies = false // 关闭自动 Cookie 管理
                    };

                    using (HttpClient newClient = new HttpClient(newHandler))
                    {
                        InitializeDefaultHeadersForClient(newClient);

                        if (useProxy)
                        {
                            ProxyIpVo proxyIpVo = await ProxyApi.GetProxyIp();
                            FileUtils.log($"{JsonConvert.SerializeObject(proxyIpVo)}", $"采集主播主页使用了代理=={anchorHomeUrl}");
                            if (proxyIpVo != null)
                            {
                                WebProxy webProxy = new WebProxy($"{proxyIpVo.ip}:{proxyIpVo.port}");
                                if (!string.IsNullOrEmpty(proxyIpVo.proxyUsername) && !string.IsNullOrEmpty(proxyIpVo.proxyPassword))
                                {
                                    webProxy.Credentials = new NetworkCredential(proxyIpVo.proxyUsername, proxyIpVo.proxyPassword);
                                }

                                newHandler.Proxy = webProxy;
                                newHandler.UseProxy = true;
                            }
                        }

                        // 手动构建 Cookie 字符串，只包含必要的 Cookie
                        var cookieString = $"__ac_nonce={acNonce}; __ac_signature={acSignature}; __ac_referer=__ac_blank";

                        var request = new HttpRequestMessage(HttpMethod.Get, anchorHomeUrl);
                        request.Headers.Add("Cookie", cookieString);

                        // 发送请求
                        var secondResponse = await newClient.SendAsync(request);
                        var htmlContent = await secondResponse.Content.ReadAsStringAsync();

                        // 提取sec_uid
                        var uniqueIdMatch = Regex.Match(htmlContent, @"""uniqueId\\"":\\""([^\\""]+)");
                        if (!uniqueIdMatch.Success)
                        {
                            FileUtils.LogError($"{htmlContent}", $"采集douyin信息异常--不存在uniqueId字符");
                            return null;
                        }

                        var uniqueId = uniqueIdMatch.Groups[1].Value;
                        return uniqueId;
                    }

                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"通过主页获取抖音号失败==={anchorHomeUrl}");
            }
            return null;
            
        }

        /// <summary>
        /// 从响应头中获取cookie
        /// </summary>
        /// <param name="headers"></param>
        /// <returns></returns>
        private static Dictionary<string, string> GetCookies(HttpResponseHeaders headers)
        {
            var cookies = new Dictionary<string, string>();
            if (headers.TryGetValues("Set-Cookie", out var cookieValues))
            {
                foreach (var cookie in cookieValues)
                {
                    var parts = cookie.Split(';');
                    var cookiePart = parts[0].Trim();
                    var equalPos = cookiePart.IndexOf('=');
                    if (equalPos > 0)
                    {
                        var name = cookiePart.Substring(0, equalPos);
                        var value = cookiePart.Substring(equalPos + 1);
                        cookies[name] = value;
                    }
                }
            }
            return cookies;
        }

        /// <summary>
        /// 设置请求头
        /// </summary>
        /// <param name="client"></param>
        private static void InitializeDefaultHeadersForClient(HttpClient client)
        {
            client.DefaultRequestHeaders.Clear();
            client.DefaultRequestHeaders.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
            client.DefaultRequestHeaders.Add("sec-ch-ua", "\"Microsoft Edge\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"");
            client.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
            client.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
            client.DefaultRequestHeaders.Add("upgrade-insecure-requests", "1");
            client.DefaultRequestHeaders.Add("sec-fetch-site", "none");
            client.DefaultRequestHeaders.Add("sec-fetch-mode", "navigate");
            client.DefaultRequestHeaders.Add("sec-fetch-user", "?1");
            client.DefaultRequestHeaders.Add("sec-fetch-dest", "document");
            client.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
            client.DefaultRequestHeaders.Add("priority", "u=0, i");
        }

    }
}
