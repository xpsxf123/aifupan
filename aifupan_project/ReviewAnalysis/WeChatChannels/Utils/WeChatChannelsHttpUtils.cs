using douyin.Utils;
using Microsoft.AspNetCore.WebUtilities;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.WeChatChannels.Utils
{
    /// <summary>
    /// 微信视频号 Http 工具类
    /// </summary>
    public static class WeChatChannelsHttpUtils
    {
        private static readonly HttpClientHandler clientHandler = new HttpClientHandler();
        private static readonly HttpClient client = new HttpClient(clientHandler, disposeHandler: false);

        static WeChatChannelsHttpUtils()
        {
            // 默认超时时间是 100 秒，时间太长。
            client.Timeout = TimeSpan.FromSeconds(10);
        }

        /// <summary>
        /// 发送 Http 请求
        /// </summary>
        public static async Task<string> SendAsync(HttpRequestMessage requestMessage)
        {
            FileUtils.LogHttp(JsonConvert.SerializeObject(requestMessage), $"微信视频号后端接口 Http 请求参数，{requestMessage.RequestUri}");

            try
            {
                using (HttpResponseMessage response = await client.SendAsync(requestMessage))
                {
                    if (response.IsSuccessStatusCode)
                    {
                        var result = await response.Content.ReadAsStringAsync();
                        FileUtils.LogHttp(result, $"微信视频号后端接口返回数据，{requestMessage.RequestUri}");
                        return result;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogHttp(ex.ToString(), $"微信视频号后端接口 Http 请求失败!{requestMessage.RequestUri}");
                throw;
            }

            return string.Empty;
        }

        /// <summary>
        /// 创建生成带签名的 HttpRequestMessage
        /// </summary>
        public static HttpRequestMessage BuildHttpRequestMessageWithSign(string url, HttpMethod method, Dictionary<string, string> requestParams)
        {
            var requertUrl = url;
            var unixTimeStamp = GetUnixTimeStamp();
            var paramSign = WeChatChannelsServiceSignatureUtil.GenerateSignature(requestParams, method, unixTimeStamp, "HMAC_SHA256");

            // Get 请求方式将请求参数加在查询字符串上
            if (method == HttpMethod.Get)
            {
                requertUrl = QueryHelpers.AddQueryString(url, requestParams);
            }

            // Post 请求方式将请求参数序列化为 Json
            StringContent content = null;
            if (method == HttpMethod.Post)
            {
                if (requestParams != null && requestParams.Count > 0)
                {
                    var json = JsonConvert.SerializeObject(requestParams);
                    content = new StringContent(json, Encoding.UTF8, "application/json");
                }
            }

            return new HttpRequestMessage(method, requertUrl)
            {
                Headers =
                {
                    { "jy-application", WeChatChannelsServiceToken.Token },
                    { "jy-signature", paramSign },
                    { "jy-timestamp",  unixTimeStamp.ToString()},
                    { "jy-sign-model", "HmacSHA256" },
                    { "jy-nonce", Guid.NewGuid().ToString() },
                },

                Content = content
            };
        }

        /// <summary>
        /// 创建生成微信视频号 Token 的 HttpRequestMessage
        /// </summary>
        public static HttpRequestMessage BuildHttpRequestMessageWithGenerateOAuthToken(string url)
        {
            return new HttpRequestMessage(HttpMethod.Get, url)
            {
                Headers =
                {
                    { "Authorization", ReplayHttpUtils.Token }
                }
            };
        }

        /// <summary>
        /// 获得 Unix 时间戳
        /// </summary>
        private static long GetUnixTimeStamp()
        {
            var dateTimeNow = DateTime.UtcNow;
            var unixTimeStamp = (long)(dateTimeNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalMilliseconds;

            return unixTimeStamp;
        }
    }
}
