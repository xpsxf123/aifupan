using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Global;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public static class HttpAsyncUtils
    {
        // ========== 核心配置（适配.NET Framework） ==========
        // 1. 静态复用HttpClient（避免端口耗尽，.NET Framework需手动处理Cookie/连接池）
        private static readonly HttpClient _httpClient;
        // 2. 重试常量（可提取到配置文件）
        private const int DefaultRetryCount = 3;
        private const int RetryBaseDelayMs = 3000; // 基础重试延迟3秒

        // 静态构造函数初始化HttpClient（适配.NET Framework）
        static HttpAsyncUtils()
        {
            // 解决.NET Framework HttpClient默认连接数限制
            ServicePointManager.DefaultConnectionLimit = int.MaxValue;
            // 禁用Expect100Continue，提升POST请求效率
            ServicePointManager.Expect100Continue = false;
            // 启用TLS 1.2（解决部分服务端仅支持TLS1.2的问题）
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;

            // 初始化HttpClient（使用默认超时时间 100 秒）
            _httpClient = new HttpClient();
        }

        /// <summary>
        /// 往服务端发送Post请求-异步（优化版）
        /// </summary>
        /// <param name="url">完整的url</param>
        /// <param name="param">参数，建议Dictionary<string, object></param>
        /// <param name="retry">是否重试</param>
        /// <param name="cancellationToken">取消令牌（支持主动取消）</param>
        /// <returns>响应体中的data字段（成功）/null（无data）</returns>
        /// <exception cref="ArgumentNullException">URL为空时抛出</exception>
        /// <exception cref="CustomException">服务端返回非0码且有msg时抛出</exception>
        /// <exception cref="Exception">网络/序列化/其他异常时抛出（含原始异常上下文）</exception>
        public static async Task<string> SendServerPostAsync(
            string url,
            object param,
            bool retry = true,
            CancellationToken cancellationToken = default)
        {
            // 1. 提前参数校验（拦截无效请求，避免无意义的网络调用）
            if (string.IsNullOrWhiteSpace(url))
            {
                throw new ArgumentNullException(nameof(url), "Post请求URL不能为空");
            }

            string responseBody = string.Empty;
            try
            {
                // 2. 调用底层Post方法（加ConfigureAwait(false)避免.NET Framework同步上下文死锁）
                responseBody = await SendPostAsync(url, param, retry, cancellationToken)
                    .ConfigureAwait(false);

                // 3. 空响应处理（直接记录日志并抛出异常）
                if (string.IsNullOrEmpty(responseBody))
                {
                    string errorMsg = $"Post请求响应体为空 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)}";
                    await FileUtils.LogErrorAsync(errorMsg, "SendServerPostAsync-空响应").ConfigureAwait(false);
                    throw new Exception("服务端返回空响应，请检查网络或服务端状态");
                }

                // 4. 反序列化响应（dynamic改为TryParse避免运行时异常，增强稳定性）
                dynamic resultObj = null;
                try
                {
                    resultObj = JsonConvert.DeserializeObject<dynamic>(responseBody);
                }
                catch (Exception ex)
                {
                    string errorMsg = $"响应体反序列化失败 | URL：{url} | 响应体：{responseBody} | 异常：{ex.Message}";
                    await FileUtils.LogErrorAsync(errorMsg, "SendServerPostAsync-反序列化异常").ConfigureAwait(false);
                    throw new Exception("响应数据格式错误，无法解析", ex); // 保留原始异常
                }

                // 5. 服务端返回码判断（简化空值访问，避免NullReferenceException）
                if (resultObj == null)
                {
                    string errorMsg = $"响应体反序列化后为空 | URL：{url} | 响应体：{responseBody}";
                    await FileUtils.LogErrorAsync(errorMsg, "SendServerPostAsync-反序列化结果空").ConfigureAwait(false);
                    throw new Exception("服务端返回数据格式异常，解析后为空");
                }

                // 6. 成功场景（code=0）：返回data字段（null则返回null）
                if (resultObj.code == 0)
                {
                    return resultObj.data?.ToString(); // 简化空值判断，data为null时直接返回null
                }

                // 7. 失败场景（code≠0）：提取错误信息并抛自定义异常
                string errorMessage = resultObj.msg?.ToString() ?? "服务端返回非成功状态码，无具体错误信息";
                await FileUtils.LogErrorAsync(
                    $"服务端返回失败 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 响应体：{responseBody} | 错误信息：{errorMessage}",
                    "SendServerPostAsync-服务端返回失败").ConfigureAwait(false);
                // 防御性解析 code：dynamic 直接 (int) 强转在 code 为字符串/超 int 范围时会运行时抛，导致业务码丢失
                int errorCode;
                if (!int.TryParse(resultObj.code?.ToString(), out errorCode)) errorCode = -1;
                throw new CustomException(errorMessage, errorCode);
            }
            catch (CustomException)
            {
                // 8. 自定义异常直接抛出（无需二次包装，保留原始业务异常）
                throw;
            }
            catch (OperationCanceledException ex)
            {
                // 9. 取消令牌/超时异常（区分主动取消和超时）
                string cancelMsg = cancellationToken.IsCancellationRequested
                    ? "请求被主动取消"
                    : "请求超时";
                string errorMsg = $"{cancelMsg} | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 异常：{ex.Message}";
                await FileUtils.LogErrorAsync(errorMsg, "SendServerPostAsync-请求取消/超时").ConfigureAwait(false);
                throw new Exception($"{cancelMsg}，请稍后重试", ex);
            }
            catch (Exception ex)
            {
                // 10. 其他异常（网络/序列化等）：补充上下文并抛出，保留原始异常
                string errorMsg = $"Post请求异常 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 响应体：{responseBody} | 异常：{ex.Message}";
                await FileUtils.LogErrorAsync(errorMsg, "SendServerPostAsync-全局异常").ConfigureAwait(false);
                throw new Exception("当前网络情况不佳，请稍后重试或联系管理员", ex); // 保留InnerException，方便排查
            }
        }

        // ========== 核心方法 ==========
        /// <summary>
        /// 异步发送POST请求（.NET Framework 优化版）
        /// </summary>
        /// <param name="url">请求地址</param>
        /// <param name="param">请求参数（序列化为JSON）</param>
        /// <param name="retry">是否重试（默认是）</param>
        /// <param name="cancellationToken">取消令牌（支持主动取消）</param>
        /// <returns>响应内容（失败返回空字符串）</returns>
        public static async Task<string> SendPostAsync(
            string url,
            object param,
            bool retry = true,
            CancellationToken cancellationToken = default)
        {
            // 1. 参数校验（提前拦截无效请求）
            if (string.IsNullOrWhiteSpace(url))
            {
                await FileUtils.LogErrorAsync("POST请求URL为空", "SendPostAsync-参数校验").ConfigureAwait(false);
                return string.Empty;
            }

            // 2. 序列化请求参数（单独捕获序列化异常，不重试）
            string jsonContent = null;
            try
            {
                jsonContent = param != null ? JsonConvert.SerializeObject(param) : null;
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync(
                    $"参数序列化失败：{ex.Message}\n参数：{param}",
                    "SendPostAsync-序列化异常").ConfigureAwait(false);
                return string.Empty;
            }

            // 3. 初始化重试配置
            int retryCount = retry ? DefaultRetryCount : 1;
            int currentRetry = 0;

            // 4. 重试循环
            while (currentRetry < retryCount)
            {
                currentRetry++;
                HttpRequestMessage request = null;
                try
                {
                    // 构建请求（.NET Framework 需手动处理null Content）
                    request = new HttpRequestMessage(HttpMethod.Post, url)
                    {
                        // param为null时，使用空Content（避免null Content导致异常）
                        Content = string.IsNullOrEmpty(jsonContent)
                            ? CreateEmptyJsonContent()
                            : new StringContent(jsonContent, Encoding.UTF8, "application/json")
                    };

                    // 添加请求头（.NET Framework 需注意头名称大小写兼容）
                    request.Headers.Add("token", ReplayHttpUtils.Token);
                    request.Headers.Add("webVersion", Constant.VERSION);
                    // 添加签名（适配.NET Framework的签名逻辑）
                    SignatureHeaders.AddSignature(request);

                    // 发送请求：
                    // - ConfigureAwait(false) 避免同步上下文死锁（.NET Framework关键！）
                    // - 支持取消令牌，指定响应读取完成模式
                    using (HttpResponseMessage response = await _httpClient.SendAsync(
                        request,
                        HttpCompletionOption.ResponseContentRead,
                        cancellationToken).ConfigureAwait(false))
                    {
                        // 读取响应内容（异步+ConfigureAwait(false)）
                        string responseContent = await response.Content.ReadAsStringAsync()
                            .ConfigureAwait(false);

                        // 5. 处理成功响应
                        if (response.IsSuccessStatusCode)
                        {
                            return responseContent;
                        }

                        // 6. 记录非成功响应日志
                        await FileUtils.LogErrorAsync(
                            $"POST请求失败（非2xx），URL：{url}，状态码：{response.StatusCode}，响应内容：{responseContent}，重试次数：{currentRetry}/{retryCount}",
                            "SendPostAsync-HTTP错误").ConfigureAwait(false);

                        // 7. 精准重试判定：仅5xx服务器错误重试，4xx客户端错误不重试
                        if ((int)response.StatusCode < 500 || currentRetry >= retryCount)
                        {
                            break; // 4xx/重试耗尽，退出循环
                        }
                    }
                }
                catch (OperationCanceledException ex)
                {
                    // 区分主动取消和超时（.NET Framework 超时也会抛OperationCanceledException）
                    if (cancellationToken.IsCancellationRequested)
                    {
                        await FileUtils.LogErrorAsync(
                            $"POST请求被主动取消，URL：{url}，异常：{ex.Message}",
                            "SendPostAsync-主动取消").ConfigureAwait(false);
                    }
                    else
                    {
                    await FileUtils.LogErrorAsync(
                        $"POST请求超时，URL：{url}，异常：{ex.Message}，重试次数：{currentRetry}/{retryCount}",
                        "SendPostAsync-超时").ConfigureAwait(false);
                    }
                    // 主动取消/重试耗尽则退出
                    if (cancellationToken.IsCancellationRequested || currentRetry >= retryCount)
                    {
                        break;
                    }
                }
                catch (HttpRequestException ex)
                {
                    // 网络异常（连接失败/DNS错误等），可重试
                    await FileUtils.LogErrorAsync(
                        $"POST请求网络异常，URL：{url}，异常：{ex.Message}，重试次数：{currentRetry}/{retryCount}",
                        "SendPostAsync-网络异常").ConfigureAwait(false);
                    if (currentRetry >= retryCount)
                    {
                        break;
                    }
                }
                catch (Exception ex)
                {
                    // 致命异常（签名失败/参数错误），不重试
                    await FileUtils.LogErrorAsync(
                        $"POST请求未知异常，URL：{url}，异常：{ex.Message}\n堆栈：{ex.StackTrace}",
                        "SendPostAsync-未知异常").ConfigureAwait(false);
                    break;
                }
                finally
                {
                    // 确保请求对象释放（.NET Framework 需手动Dispose）
                    request?.Dispose();
                }

                // 8. 指数退避延迟（.NET Framework 支持Task.Delay+取消令牌）
                int delayMs = RetryBaseDelayMs * currentRetry;
                await Task.Delay(delayMs, cancellationToken).ConfigureAwait(false);
            }

            // 所有重试失败，返回空字符串（兼容原逻辑）
            return string.Empty;
        }
        // ========== 辅助方法（适配.NET Framework） ==========
        /// <summary>
        /// 创建空的JSON Content（.NET Framework 无HttpContent.Empty，手动实现）
        /// </summary>
        private static HttpContent CreateEmptyJsonContent()
        {
            return new StringContent(string.Empty, Encoding.UTF8, "application/json");
        }

        #region 核心异步方法：基础GET请求（带重试）
        /// <summary>
        /// 异步发送GET请求（基础版，带重试）
        /// </summary>
        /// <param name="url">完整URL</param>
        /// <param name="param">GET请求参数</param>
        /// <param name="retry">是否重试</param>
        /// <param name="cancellationToken">取消令牌</param>
        /// <returns>响应体字符串（失败返回空）</returns>
        public static async Task<string> SendGetAsync(
            string url,
            Dictionary<string, object> param,
            bool retry = true,
            CancellationToken cancellationToken = default)
        {
            // 1. 参数校验
            if (string.IsNullOrWhiteSpace(url))
            {
                await FileUtils.LogErrorAsync("GET请求URL不能为空", "SendGetAsync-参数校验").ConfigureAwait(false);
                return string.Empty;
            }

            // 2. 拼接URL和参数（拆分方法，提升可读性）
            Uri requestUri = BuildGetRequestUri(url, param);
            int retryCount = retry ? DefaultRetryCount : 1;
            int currentRetry = 0;

            // 3. 重试循环（指数退避）
            while (currentRetry < retryCount)
            {
                currentRetry++;
                HttpRequestMessage request = null;
                try
                {
                    // 4. 构建GET请求
                    request = new HttpRequestMessage(HttpMethod.Get, requestUri)
                    {
                        Headers =
                        {
                            { "token", ReplayHttpUtils.Token },
                            { "webVersion", Constant.VERSION }
                        }
                    };
                    // 添加签名
                    SignatureHeaders.AddSignature(request);

                    // 5. 异步发送请求（核心：替换.Result为await）
                    using (HttpResponseMessage response = await _httpClient.SendAsync(
                        request,
                        HttpCompletionOption.ResponseContentRead,
                        cancellationToken).ConfigureAwait(false))
                    {
                        string responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);

                        // 6. 成功响应直接返回
                        if (response.IsSuccessStatusCode)
                        {
                            return responseContent;
                        }

                        // 7. 记录非成功状态码日志
                        string errorMsg = $"GET请求失败 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 状态码：{response.StatusCode} | 重试次数：{currentRetry}/{retryCount}";
                        await FileUtils.LogErrorAsync(errorMsg, "SendGetAsync-HTTP状态码异常").ConfigureAwait(false);

                        // 8. 精准重试判定：仅5xx服务器错误重试，4xx客户端错误不重试
                        if ((int)response.StatusCode < 500 || currentRetry >= retryCount)
                        {
                            break;
                        }
                    }
                }
                catch (OperationCanceledException ex)
                {
                    // 区分主动取消和超时
                    string cancelMsg = cancellationToken.IsCancellationRequested
                        ? "GET请求被主动取消"
                        : "GET请求超时";
                    string errorMsg = $"{cancelMsg} | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 重试次数：{currentRetry}/{retryCount} | 异常：{ex.Message}";
                    await FileUtils.LogErrorAsync(errorMsg, "SendGetAsync-取消/超时异常").ConfigureAwait(false);

                    // 主动取消或重试耗尽则退出
                    if (cancellationToken.IsCancellationRequested || currentRetry >= retryCount)
                    {
                        break;
                    }
                }
                catch (HttpRequestException ex)
                {
                    // 网络异常，记录日志并继续重试（未耗尽次数时）
                    string errorMsg = $"GET请求网络异常 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 重试次数：{currentRetry}/{retryCount} | 异常：{ex.Message}";
                    await FileUtils.LogErrorAsync(errorMsg, "SendGetAsync-网络异常").ConfigureAwait(false);

                    if (currentRetry >= retryCount)
                    {
                        break;
                    }
                }
                catch (Exception ex)
                {
                    // 致命异常（签名/参数错误），不重试
                    string errorMsg = $"GET请求未知异常 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 异常：{ex.Message}\n堆栈：{ex.StackTrace}";
                    await FileUtils.LogErrorAsync(errorMsg, "SendGetAsync-未知异常").ConfigureAwait(false);
                    break;
                }
                finally
                {
                    // 释放请求资源
                    request?.Dispose();
                }

                // 9. 指数退避延迟（替换Thread.Sleep为await Task.Delay）
                int delayMs = RetryBaseDelayMs * currentRetry;
                await Task.Delay(delayMs, cancellationToken).ConfigureAwait(false);
            }

            return string.Empty;
        }
        #endregion

        #region 业务封装异步方法：SendServerGetAsync
        /// <summary>
        /// 异步发送GET请求（业务版，自动解析响应体）
        /// </summary>
        /// <param name="url">完整URL</param>
        /// <param name="param">GET请求参数</param>
        /// <param name="retry">是否重试</param>
        /// <param name="cancellationToken">取消令牌</param>
        /// <returns>响应体中的data字段（成功）/null（无data）</returns>
        /// <exception cref="ArgumentNullException">URL为空时抛出</exception>
        /// <exception cref="CustomException">服务端返回非0码且有msg时抛出</exception>
        /// <exception cref="HttpRequestException">网络/解析异常时抛出</exception>
        public static async Task<string> SendServerGetAsync(
            string url,
            Dictionary<string, object> param,
            bool retry = true,
            CancellationToken cancellationToken = default)
        {
            // 1. 参数校验
            if (string.IsNullOrWhiteSpace(url))
            {
                throw new ArgumentNullException(nameof(url), "GET请求URL不能为空");
            }

            string responseBody = string.Empty;
            dynamic resultObj;
            try
            {
                // 2. 调用基础异步GET方法
                responseBody = await SendGetAsync(url, param, retry, cancellationToken).ConfigureAwait(false);

                // 3. 空响应处理
                if (string.IsNullOrEmpty(responseBody))
                {
                    string errorMsg = $"GET请求响应体为空 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)}";
                    await FileUtils.LogErrorAsync(errorMsg, "SendServerGetAsync-空响应").ConfigureAwait(false);
                    throw new HttpRequestException("服务端返回空响应，请检查网络或服务端状态");
                }

                // 4. 异步反序列化响应（避免阻塞）
                resultObj = await Task.Run(() => JsonConvert.DeserializeObject<dynamic>(responseBody), cancellationToken).ConfigureAwait(false);

                // 5. 解析响应结果
                if (resultObj == null)
                {
                    string errorMsg = $"GET响应反序列化后为空 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 响应体：{responseBody}";
                    await FileUtils.LogErrorAsync(errorMsg, "SendServerGetAsync-反序列化异常").ConfigureAwait(false);
                    throw new JsonSerializationException("服务端返回数据格式异常，解析后为空");
                }

                // 6. 成功场景（code=0）
                if (resultObj.code == 0)
                {
                    return resultObj.data?.ToString();
                }

                // 7. 业务失败场景（抛自定义异常）
                string errorMessage = resultObj.msg?.ToString() ?? "服务端返回非成功状态码，无具体错误信息";
                await FileUtils.LogErrorAsync(
                    $"GET请求业务失败 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 响应体：{responseBody} | 错误信息：{errorMessage}",
                    "SendServerGetAsync-业务异常").ConfigureAwait(false);
                throw new CustomException(errorMessage);
            }
            catch (CustomException)
            {
                // 自定义业务异常直接抛出
                throw;
            }
            catch (OperationCanceledException ex)
            {
                // 取消/超时异常
                string cancelMsg = cancellationToken.IsCancellationRequested ? "请求被主动取消" : "请求超时";
                string errorMsg = $"{cancelMsg} | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 响应体：{responseBody} | 异常：{ex.Message}";
                await FileUtils.LogErrorAsync(errorMsg, "SendServerGetAsync-取消/超时").ConfigureAwait(false);
                throw new HttpRequestException($"{cancelMsg}，请稍后重试", ex);
            }
            catch (Exception ex)
            {
                // 全局异常处理（保留InnerException）
                string errorMsg = $"GET请求异常 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 响应体：{responseBody} | 异常：{ex.Message}\n堆栈：{ex.StackTrace}";
                await FileUtils.LogErrorAsync(errorMsg, "SendServerGetAsync-全局异常").ConfigureAwait(false);
                throw new HttpRequestException("当前网络情况不佳，请稍后重试或联系管理员", ex);
            }
            finally
            {
                // 记录数据异常日志（仅失败时）
                if (string.IsNullOrEmpty(responseBody))
                {
                    string errorMsg = $"GET请求数据异常 | URL：{url} | 参数：{JsonConvert.SerializeObject(param)} | 响应体：{responseBody}";
                    await FileUtils.LogErrorAsync(errorMsg, "SendServerGetAsync-数据异常").ConfigureAwait(false);
                }
            }
        }
        #endregion

        #region 辅助方法：拼接GET请求URL和参数
        /// <summary>
        /// 拼接GET请求的URL和参数（处理空值、URL编码）
        /// </summary>
        private static Uri BuildGetRequestUri(string url, Dictionary<string, object> param)
        {
            var urlBuilder = new UriBuilder(url);

            if (param != null && param.Count > 0)
            {
                // 处理空值，避免UrlEncode空指针
                var paramList = param
                    .Where(kv => kv.Value != null)
                    .Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value.ToString())}")
                    .ToList();

                urlBuilder.Query = string.Join("&", paramList);
            }

            return urlBuilder.Uri;
        }
        #endregion
    }
}
