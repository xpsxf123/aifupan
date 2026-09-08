using COSXML.Network;
using douyin.Utils;
using Microsoft.Playwright.Core;
using Polly;
using Polly.Retry;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    public class DouYinDeveloperOpen
    {
        private string _account;
        private string _password;
        private System.Net.Http.HttpClient _httpClient;
        private HttpClientHandler _handler;
        private AsyncRetryPolicy _retryPolicy;

        public DouYinDeveloperOpen()
        {
            _account = string.Empty;
            _password = string.Empty;
            // 初始化HttpClientHandler并禁用自动重定向
            _handler = new HttpClientHandler
            {
                AllowAutoRedirect = false,  
                UseCookies = true,
                CookieContainer = new CookieContainer(),
                AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate
            };

            _httpClient = new  System.Net.Http.HttpClient(_handler);
            _httpClient.Timeout = TimeSpan.FromSeconds(10);
            // 强制使用 TLS1.2（抖音开放平台可能要求 HTTPS 协议版本）
            System.Net.ServicePointManager.SecurityProtocol = System.Net.SecurityProtocolType.Tls12;
            // 可选：设置 DNS 刷新超时（解决单例模式下 DNS 缓存过期问题）
            ServicePointManager.DnsRefreshTimeout = 60 * 1000; // 1 分钟刷新一次 DNS
            // 配置重试策略（3 次重试，每次间隔 1 秒，仅重试 DNS 解析失败的异常）
            _retryPolicy = Policy
                .Handle<WebException>(ex =>
                    ex.Message.Contains("未能解析此远程名称") ||
                    ex.Status == WebExceptionStatus.NameResolutionFailure)
                .WaitAndRetryAsync(3, retryAttempt => TimeSpan.FromSeconds(1));
        }

        // 加密方法保持不变
        public string Encrypt(string e)
        {
            if (string.IsNullOrEmpty(e))
                return "";

            var t = new List<byte>();

            foreach (char c in e)
            {
                int code = c;

                if (code >= 0 && code <= 127)
                {
                    t.Add((byte)code);
                }
                else if (code >= 128 && code <= 2047)
                {
                    t.Add((byte)(192 | ((code >> 6) & 31)));
                    t.Add((byte)(128 | (code & 63)));
                }
                else if ((code >= 2048 && code <= 55295) || (code >= 57344 && code <= 65535))
                {
                    t.Add((byte)(224 | ((code >> 12) & 15)));
                    t.Add((byte)(128 | ((code >> 6) & 63)));
                    t.Add((byte)(128 | (code & 63)));
                }
            }

            var n = new List<string>();
            foreach (byte b in t)
            {
                byte xorResult = (byte)(5 ^ b);
                n.Add(xorResult.ToString("x2"));
            }

            return string.Join("", n);
        }
        /// <summary>
        /// 通过Email登录第三方平台
        /// </summary>
        /// <param name="email"></param>
        /// <param name="password"></param>
        /// <returns></returns>
        public async Task<(string sessionid, string reason, string message,int failureType)> AccountLoginByEmail(string email, string password)
        {
            _account = email;
            _password = password;
            string sessionid_dy_open = string.Empty;
            string reason_dy_open = string.Empty;
            string message_dy_open = string.Empty;
            int failureType_dy_open = 5;
            // 清除默认请求头，避免冲突
            _httpClient.DefaultRequestHeaders.Clear();

            // 设置登录请求头
            var headers = new Dictionary<string, string>
        {
            {"accept", "application/json, text/javascript"},
            {"accept-language", "zh-CN,zh;q=0.9"},
            {"cache-control", "no-cache"},
            {"origin", "https://developer.open-douyin.com"},
            {"pragma", "no-cache"},
            {"priority", "u=1, i"},
            {"referer", "https://developer.open-douyin.com/login"},
            {"sec-ch-ua", "\"Google Chrome\";v=\"141\", \"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"141\""},
            {"sec-ch-ua-mobile", "?0"},
            {"sec-ch-ua-platform", "\"Windows\""},
            {"sec-fetch-dest", "empty"},
            {"sec-fetch-mode", "cors"},
            {"sec-fetch-site", "same-origin"},
            {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"},
            {"Host", "https://developer.open-douyin.com"}
        };

            foreach (var header in headers)
            {
                _httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
            }

            var formData = new FormUrlEncodedContent(new[]
            {
            new KeyValuePair<string, string>("mix_mode", "1"),
            new KeyValuePair<string, string>("account", Encrypt(_account)),
            new KeyValuePair<string, string>("password", Encrypt(_password)),
            new KeyValuePair<string, string>("account_type", "0"),
            new KeyValuePair<string, string>("service", "https://developer.open-douyin.com"),
            new KeyValuePair<string, string>("biz_param", "{\"source\":\"platform\"}"),
            new KeyValuePair<string, string>("fixed_mix_mode", "1")
        });

            try
            {
                HttpResponseMessage response = null;
                _retryPolicy.ExecuteAsync(async () =>
                {
                    response = await _httpClient.PostAsync("https://developer.open-douyin.com/passport/sso/account_login/v2/", formData);
                    response.EnsureSuccessStatusCode();
                    FileUtils.log("访问成功！状态码：" + response.StatusCode);
                }).Wait();
                if (response == null || !response.IsSuccessStatusCode)
                {
                    reason_dy_open = "网络异常";
                    message_dy_open = "网络异常，请稍后重试";
                    failureType_dy_open = 3;
                    return (sessionid_dy_open, reason_dy_open, message_dy_open, failureType_dy_open);
                }
                string responseBody = await response.Content.ReadAsStringAsync();
                var jsonData = System.Text.Json.JsonSerializer.Deserialize<System.Text.Json.Nodes.JsonObject>(responseBody);

                    if (jsonData != null && jsonData.TryGetPropertyValue("error_code", out var errorCode) &&
                    errorCode.GetValue<int>() == 0)
                {
                    if (jsonData.TryGetPropertyValue("redirect_url", out var redirectUrlNode))
                    {
                        string redirectUrl = redirectUrlNode.ToString();
                        sessionid_dy_open = await LoginCallback(redirectUrl);
                    }
                }
                else
                {
                    if (jsonData == null)
                    {
                        reason_dy_open = "登录异常";
                        message_dy_open = "无任何返回数据";
                        failureType_dy_open = 5;
                    }
                    jsonData.TryGetPropertyValue("error_code", out var errCode);
                    if (errCode.GetValue<int>() == 1009)
                    {
                        reason_dy_open = "账号或密码错误";
                        message_dy_open = "账号或密码错误，请重试";
                        failureType_dy_open = 1;
                    }
                    if (errCode.GetValue<int>() == 1105)
                    {
                        reason_dy_open = "账号异常";
                        message_dy_open = "账号异常，滑动滑块进行验证";
                        failureType_dy_open = 2;
                    }

                }
                
            }
            catch (Exception ex)
            {
                reason_dy_open = "邮箱账号登录错误";
                message_dy_open = ex.Message+ex.InnerException;
                failureType_dy_open = 5;
                FileUtils.LogError($"邮箱账号登录错误,回调请求异常: {ex.Message}");
            }
            return (sessionid_dy_open,reason_dy_open,message_dy_open,failureType_dy_open);
        }

        public async Task<string> LoginCallback(string redirectUrl)
        {
            string sessionid_dy_open = string.Empty;
            // 清除之前的请求头
            _httpClient.DefaultRequestHeaders.Clear();

            // 设置回调请求头
            var headers = new Dictionary<string, string>
        {
            {"accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"},
            {"accept-language", "zh-CN,zh;q=0.9"},
            {"cache-control", "no-cache"},
            {"pragma", "no-cache"},
            {"priority", "u=0, i"},
            {"referer", "https://developer.open-douyin.com/login"},
            {"sec-ch-ua", "\"Google Chrome\";v=\"141\", \"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"141\""},
            {"sec-ch-ua-mobile", "?0"},
            {"sec-ch-ua-platform", "\"Windows\""},
            {"sec-fetch-dest", "document"},
            {"sec-fetch-mode", "navigate"},
            {"sec-fetch-site", "same-origin"},
            {"sec-fetch-user", "?1"},
            {"upgrade-insecure-requests", "1"},
            {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"},
            {"Host", "https://developer.open-douyin.com"}
        };

            foreach (var header in headers)
            {
                _httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
            }

            try
            {
                _retryPolicy.ExecuteAsync(async () =>
                {
                    //response = await _httpClient.GetAsync(redirectUrl);
                    //response.EnsureSuccessStatusCode();
                    //FileUtils.log("访问成功！状态码：" + response.StatusCode);
                using (var response = await _httpClient.GetAsync(redirectUrl))
                {
                    //response.EnsureSuccessStatusCode();
                    // 获取响应中的Cookie
                    var uri = new Uri(redirectUrl);
                    var cookies = _handler.CookieContainer.GetCookies(uri);

                    foreach (Cookie cookie in cookies)
                    {
                        if (cookie.Name == "sessionid_dy_open")
                        {
                            sessionid_dy_open = cookie.Value;
                            break;
                        }
                    }
                }  
                }).Wait(); 
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"回调请求异常: {ex.Message}");
            }
            return sessionid_dy_open;
        }
    }
}
