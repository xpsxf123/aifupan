using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Net.Http;
using Swan.Parsers;
using CefSharp.DevTools.IO;
using System.Threading;
using Microsoft.Extensions.DependencyInjection;
using douyin.Utils;
using Newtonsoft.Json;
using Swan.Formatters;
using System.Web.UI.WebControls;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Global;
using System.Net;
using Newtonsoft.Json.Serialization;

namespace ReviewAnalysis.Utils
{
    public class HttpUtils
    {
        /// <summary>
        /// HttpClient工厂
        /// </summary>
        //private static IHttpClientFactory httpClientFactory = Program.ServiceProvider.GetService<IHttpClientFactory>();

        private static readonly HttpClientHandler clientHandler = new HttpClientHandler();
        private static readonly HttpClient client = new HttpClient(clientHandler, disposeHandler: false)
        {
            // 设置超时时间为 30 秒，避免网络断开时长时间卡死
            Timeout = TimeSpan.FromSeconds(30)
        };

        /// <summary>
        /// 获取HttpClient
        /// </summary>
        /// <returns></returns>
        public static HttpClient getClient()
        {
            return client;
        }

        /// <summary>
        /// 发送GET请求-异步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数</param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static async Task<string> SendGetAsync(string url, Dictionary<string, object> param, bool retry = true)
        {

            // 拼接请求参数
            string paramStr = "";
            if (param != null && param.Count > 0)
            {
                paramStr = string.Join("&", param.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value?.ToString())}"));
            }
            var urlBuilder = new UriBuilder(url)
            {
                Query = paramStr
            };

            // 重试次数
            int i = retry ? 3 : 1;
            while (i > 0)
            {
                try
                {
                    // 构建请求头
                    var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
                    {
                        Headers =
                        {
                            { "token", ReplayHttpUtils.Token },
                            { "webVersion", Constant.VERSION }
                        }
                    };
                    SignatureHeaders.AddSignature(httpRequestMessage);
                    using (HttpResponseMessage response = await client.SendAsync(httpRequestMessage))
                    {
                        if (response.IsSuccessStatusCode)
                        {
                            // 通讯成功
                            return await response.Content.ReadAsStringAsync();
                        }
                        else
                        {
                            // 记录非成功状态码的日志 
                            FileUtils.LogError($"请求失败，url={url}，状态码: {response.StatusCode}", $"异步发送GET请求发生异常");
                        }
                    }

                }
                catch (Exception e)
                {
                    FileUtils.LogError($"{e}，url={url}", $"异步发送GET请求发生异常");
                }

                i--;
                // 异步等待3秒
                await Task.Delay(3000);
            }


            return "";
        }

        /// <summary>
        /// 往服务端发送Get请求-同步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数</param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static string SendServerGet(string url, Dictionary<string, object> param, bool retry = true)
        {
            string respnseBody = "";
            try
            {
                respnseBody = SendGet(url, param, retry);
                if (!string.IsNullOrEmpty(respnseBody))
                {
                    var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                    string strMsg = resultObj?.msg?.ToString();
                    if (resultObj != null && resultObj.code == 0)
                    {
                        if (resultObj.data != null)
                        {
                            return resultObj.data.ToString();
                        }
                        else
                        {
                            return null;
                        }

                    }
                    if (!string.IsNullOrEmpty(strMsg) && strMsg.Contains("IP代理"))
                    {
                        FileUtils.LogError(strMsg, $"往服务端发送Get请求-同步，发送异常");
                        return null;
                    }
                }
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"往服务端发送Get请求-同步，发送异常");
            }

            FileUtils.LogError($"地址：{url}=====请求参数：{JsonConvert.SerializeObject(param)}=====响应体：{respnseBody}", $"往服务端发送Get请求-同步，数据异常");
            return null; //throw new Exception("当前网络情况不佳，请稍后重试或联系管理员");
        }

        /// <summary>
        /// 往服务端发送Post请求-同步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数，建议Dictionary<string, object></param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static R SendServerGetR(string url, Dictionary<string, object> param, bool retry = true)
        {
            R r = new R();
            r.code = -1;
            string respnseBody = "";
            try
            {
                respnseBody = SendGet(url, param, retry);
                if (!string.IsNullOrEmpty(respnseBody))
                {
                    var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                    r.code = resultObj?.code ?? -1;
                    r.data = resultObj?.data?.ToString() ?? "";
                    r.msg = resultObj?.msg?.ToString() ?? "";
                }
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"往服务端发送Post请求-同步，发生异常");
                FileUtils.LogError($"地址：{url}=====请求参数：{JsonConvert.SerializeObject(param)}=====响应体：{respnseBody},== msg:{e.Message}", $"往服务端发送Get请求-同步，数据异常");
            }
            return r;
        }

        /// <summary>
        /// 往服务端发送Get请求-异步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数</param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static async Task<string> SendServerGetAsync(string url, Dictionary<string, object> param, bool retry = true)
        {

            string respnseBody = "";
            try
            {
                respnseBody = await SendGetAsync(url, param, retry);
                if (!string.IsNullOrEmpty(respnseBody))
                {
                    var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                    if (resultObj != null && resultObj.code == 0)
                    {
                        if (resultObj.data != null)
                        {
                            return resultObj.data.ToString();
                        }
                        else
                        {
                            return null;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                await FileUtils.LogErrorAsync($"{e}", $"SendServerGetAsync往服务端发送Get请求-异步，发生异常");

            }

            await FileUtils.LogErrorAsync($"地址：{url}=====请求参数：{JsonConvert.SerializeObject(param)}=====响应体：{respnseBody}", $"往服务端发送Get请求-异步，数据异常");
            return null; //throw new Exception("当前网络情况不佳，请稍后重试或联系管理员");
        }

        /// <summary>
        /// 发送GET请求-同步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数</param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static string SendGet(string url, Dictionary<string, object> param, bool retry = true)
        {

            // 拼接请求参数
            string paramStr = "";
            if (param != null && param.Count > 0)
            {
                paramStr = string.Join("&", param.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value?.ToString())}"));
            }
            var urlBuilder = new UriBuilder(url)
            {
                Query = paramStr
            };

            // 重试次数
            int i = retry ? 3 : 1;
            while (i > 0)
            {
                try
                {
                    // 构建请求头
                    var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
                    {
                        Headers =
                        {
                            { "token", ReplayHttpUtils.Token },
                            { "webVersion", Constant.VERSION },
                        }
                    };
                    SignatureHeaders.AddSignature(httpRequestMessage);
                    using (HttpResponseMessage response = client.SendAsync(httpRequestMessage).Result)
                    {
                        if (response.IsSuccessStatusCode)
                        {
                            // 通讯成功
                            return response.Content.ReadAsStringAsync().Result;
                        }
                        else
                        {
                            // 记录非成功状态码的日志 
                            FileUtils.LogError($"{url}，参数：{(param != null ? JsonConvert.SerializeObject(param) : "")}", $"异步发送GET请求发生异常，状态码: {response.StatusCode}");
                        }
                    }

                }
                catch (Exception e)
                {
                    FileUtils.LogError($"{e}", $"异步发送GET请求发生异常");
                }

                i--;
                // 等待3秒
                Thread.Sleep(3000);
            }


            return "";
        }

        /// <summary>
        /// 往服务端发送Post请求-异步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数，建议Dictionary<string, object></param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static async Task<string> SendServerPostAsync(string url, object param, bool retry = true)
        {
            string respnseBody = "";
            try
            {
                respnseBody = await SendPostAsync(url, param, retry);
                if (!string.IsNullOrEmpty(respnseBody))
                {
                    var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                    if (resultObj != null && resultObj.code == 0)
                    {
                        if (resultObj.data != null)
                        {
                            return resultObj.data.ToString();
                        }
                        else
                        {
                            return null;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"往服务端发送Post请求-异步，发生异常");

            }

            FileUtils.LogError($"地址：{url}=====请求参数：{JsonConvert.SerializeObject(param)}=====响应体：{respnseBody}", $"往服务端发送Post请求-异步，数据异常");
            return null; //throw new Exception("当前网络情况不佳，请稍后重试或联系管理员");
        }

        /// <summary>
        /// 往服务端发送Post请求-同步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数，建议Dictionary<string, object></param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static string SendServerPost(string url, object param, bool retry = true)
        {
            string respnseBody = "";
            try
            {
                respnseBody = SendPost(url, param, retry);
                if (!string.IsNullOrEmpty(respnseBody))
                {
                    var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                    if (resultObj != null && resultObj.code == 0)
                    {
                        if (resultObj.data != null)
                        {
                            return resultObj.data.ToString();
                        }
                        else
                        {
                            return null;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"往服务端发送Post请求-同步，发生异常");

            }

            FileUtils.LogError($"地址：{url}=====请求参数：{JsonConvert.SerializeObject(param)}=====响应体：{respnseBody}", $"往服务端发送Post请求-同步，数据异常");
            return null; //throw new Exception("当前网络情况不佳，请稍后重试或联系管理员");
        }

        /// <summary>
        /// 往服务端发送Post请求-同步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数，建议Dictionary<string, object></param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static R SendServerPostR(string url, object param, bool retry = true)
        {
            R r = new R();
            string respnseBody = "";
            try
            {
                respnseBody = SendPost(url, param, retry);
                if (!string.IsNullOrEmpty(respnseBody))
                {
                    var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                    r.code = resultObj?.code ?? -1;
                    r.data = resultObj?.data?.ToString() ?? "";
                    r.msg = resultObj?.msg?.ToString() ?? "";
                }
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"往服务端发送Post请求-同步，发生异常");
                FileUtils.LogError($"地址：{url}=====请求参数：{JsonConvert.SerializeObject(param)}=====响应体：{respnseBody}", $"往服务端发送Post请求-同步，数据异常");
            }
            return r;
        }


        /// <summary>
        /// 往服务端发送Put请求-同步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数，建议Dictionary<string, object></param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static string SendServerPut(string url, object param, bool retry = true)
        {
            string respnseBody = "";
            try
            {
                respnseBody = SendPut(url, param, retry);
                if (!string.IsNullOrEmpty(respnseBody))
                {
                    var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                    if (resultObj != null && resultObj.code == 0)
                    {
                        if (resultObj.data != null)
                        {
                            return resultObj.data.ToString();
                        }
                        else
                        {
                            return null;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"往服务端发送Put请求-同步，发生异常");

            }

            FileUtils.LogError($"地址：{url}=====请求参数：{JsonConvert.SerializeObject(param)}=====响应体：{respnseBody}", $"往服务端发送Post请求-同步，数据异常");
            return null; //throw new Exception("当前网络情况不佳，请稍后重试或联系管理员");
        }

        /// <summary>
        /// 发送POST请求-同步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数，建议Dictionary<string, object></param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static string SendPost(string url, object param, bool retry = true)
        {

            // 请求体内容
            string json = param != null ? JsonConvert.SerializeObject(param) : null;
            // 重试次数
            int i = retry ? 3 : 1;
            while (i > 0)
            {
                using (var content = json != null ? new StringContent(json, Encoding.UTF8, "application/json") : null)
                {
                    try
                    {
                        // 构建请求体和请求头
                        var httpRequestMessage = new HttpRequestMessage(HttpMethod.Post, url)
                        {
                            Content = content
                        };
                        httpRequestMessage.Headers.Add("token", ReplayHttpUtils.Token);
                        httpRequestMessage.Headers.Add("webVersion", Constant.VERSION);

                        SignatureHeaders.AddSignature(httpRequestMessage);
                        using (HttpResponseMessage response = client.SendAsync(httpRequestMessage).Result)
                        {
                            if (response.IsSuccessStatusCode)
                            {
                                // 通讯成功
                                string result = response.Content.ReadAsStringAsync().Result;
                                return result;
                            }
                            else
                            {
                                string errorContent = response.Content.ReadAsStringAsync().Result;
                                FileUtils.LogError($"HTTP POST请求失败，状态码: {response.StatusCode}, 响应内容: {errorContent}", "异步发送POST请求发生异常");
                            }
                        }

                    }
                    catch (Exception e)
                    {
                        FileUtils.LogError($"{e}", $"异步发送POST请求发生异常");
                    }

                    i--;
                    // 等待3秒
                    Thread.Sleep(3000);
                }
            }

            return "";
        }

        /// <summary>
        /// 发送PUT请求-同步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数，建议Dictionary<string, object></param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static string SendPut(string url, object param, bool retry = true)
        {

            // 请求体内容
            string json = param != null ? JsonConvert.SerializeObject(param) : null;
            // 重试次数
            int i = retry ? 3 : 1;
            while (i > 0)
            {
                using (var content = json != null ? new StringContent(json, Encoding.UTF8, "application/json") : null)
                {
                    try
                    {
                        // 构建请求体和请求头
                        var httpRequestMessage = new HttpRequestMessage(HttpMethod.Put, url)
                        {
                            Content = content
                        };
                        httpRequestMessage.Headers.Add("token", ReplayHttpUtils.Token);
                        httpRequestMessage.Headers.Add("webVersion", Constant.VERSION);

                        SignatureHeaders.AddSignature(httpRequestMessage);
                        using (HttpResponseMessage response = client.SendAsync(httpRequestMessage).Result)
                        {
                            if (response.IsSuccessStatusCode)
                            {
                                // 通讯成功
                                string result = response.Content.ReadAsStringAsync().Result;
                                return result;
                            }
                            else
                            {
                                string errorContent = response.Content.ReadAsStringAsync().Result;
                                FileUtils.LogError($"HTTP POST请求失败，状态码: {response.StatusCode}, 响应内容: {errorContent}", "异步发送POST请求发生异常");
                            }
                        }

                    }
                    catch (Exception e)
                    {
                        FileUtils.LogError($"{e}", $"异步发送POST请求发生异常");
                    }

                    i--;
                    // 等待3秒
                    Thread.Sleep(3000);
                }
            }

            return "";
        }

        /// <summary>
        /// 发送POST请求-异步
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数，建议Dictionary<string, object></param>
        /// <param name="retry">是否重试</param>
        /// <returns></returns>
        public static async Task<string> SendPostAsync(string url, object param, bool retry = true)
        {
            // 请求体内容
            string json = param != null ? JsonConvert.SerializeObject(param) : null;
            // 重试次数
            int i = retry ? 3 : 1;
            while (i > 0)
            {
                using (var content = json != null ? new StringContent(json, Encoding.UTF8, "application/json") : null)
                {
                    try
                    {
                        // 构建请求体和请求头
                        var httpRequestMessage = new HttpRequestMessage(HttpMethod.Post, url)
                        {
                            Content = content
                        };
                        httpRequestMessage.Headers.Add("token", ReplayHttpUtils.Token);
                        httpRequestMessage.Headers.Add("webVersion", Constant.VERSION);

                        SignatureHeaders.AddSignature(httpRequestMessage);
                        using (HttpResponseMessage response = await client.SendAsync(httpRequestMessage))
                        {
                            if (response.IsSuccessStatusCode)
                            {
                                // 通讯成功
                                string result = await response.Content.ReadAsStringAsync();
                                return result;
                            }
                            else
                            {
                                string errorContent = await response.Content.ReadAsStringAsync();
                                await FileUtils.LogErrorAsync($"HTTP POST请求失败，状态码: {response.StatusCode}, 响应内容: {errorContent}", "异步发送POST请求发生异常");
                            }
                        }

                    }
                    catch (Exception e)
                    {
                        await FileUtils.LogErrorAsync($"{e}", $"异步发送POST请求发生异常");
                    }

                    i--;
                    // 异步等待3秒
                    await Task.Delay(3000);
                }
            }

            return "";
        }

        /// <summary>
        /// 发送POST请求
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="content">请求内容</param>
        /// <param name="retryNum">重试次数</param>
        /// <param name="timeout">超时时间(秒)</param>
        /// <returns></returns>
        public static string SendPost(string url, Action<MultipartFormDataContent> fillFormAction, int retryNum = 3, int timeout = 5)
        {
            // 不要在这里释放content，因为可能需要重试
            using (CancellationTokenSource cts = new CancellationTokenSource(TimeSpan.FromSeconds(timeout)))
            {
                int retryCount = 0;
                while (retryCount < retryNum)
                {
                    try
                    {
                        // 构建请求体和请求头
                        HttpRequestMessage request = null;
                        HttpResponseMessage response = null;
                        MultipartFormDataContent formData = null;
                        try
                        {
                            formData = new MultipartFormDataContent();

                            // 要把formData传到content外
                            fillFormAction?.Invoke(formData);

                            request = new HttpRequestMessage(HttpMethod.Post, url)
                            {
                                Content = formData,
                                Headers =
                                {
                                    { "token", ReplayHttpUtils.Token },
                                    { "webVersion", Constant.VERSION },
                                }
                            };

                            SignatureHeaders.AddSignature(request);
                            response = client.SendAsync(request, cts.Token).Result;

                            if (response.IsSuccessStatusCode)
                            {
                                // 通讯成功
                                string result = response.Content.ReadAsStringAsync().Result;
                                return result;
                            }
                        }
                        catch (Exception e)
                        {
                            FileUtils.LogError($"{e}", $"同步发送POST请求发生异常");
                        }
                        finally
                        {
                            formData?.Dispose();
                            request?.Dispose();
                            response?.Dispose();
                        }
                    }
                    catch (Exception e)
                    {
                        FileUtils.LogError($"{e}", $"异步发送POST请求发生异常");
                    }

                    retryCount++;
                    if (retryCount < retryNum)
                    {
                        Thread.Sleep(3000);
                    }
                }
            }
            return "";
        }

        public class R
        {
            public int code { get; set; } = -1;

            public string msg { get; set; }

            public string data { get; set; }

            public bool success()
            {
                return code == 0;
            }
        }
    }
}
