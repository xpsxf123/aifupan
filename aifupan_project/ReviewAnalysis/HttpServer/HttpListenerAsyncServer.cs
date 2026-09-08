using douyin.Utils;
using Newtonsoft.Json;
using OpenCvSharp.Aruco;
using ReviewAnalysis.Ai;
using ReviewAnalysis.Asr;
using ReviewAnalysis.BeanCache;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Dto;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.IO;
using System.Linq;
using System.Net;
using System.Reflection;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web;
using System.Windows.Forms;
using ReviewAnalysis.Global;

namespace ReviewAnalysis.HttpServer
{
    public class HttpListenerAsyncServer : IDisposable
    {
        #region 常量配置（抽离硬编码，便于维护）
        private const int DEFAULT_BUFFER_SIZE = 8192; // 8KB 缓冲区（替代8MB超大缓冲区，降低内存占用）
        private const int MAX_RETRIES = 3; // 监听器启动最大重试次数
        private const int RETRY_DELAY_MS = 1000; // 重试延迟（毫秒）
        private const int SSE_ERROR_CODE = 7005; // SSE专属错误码
        private const string CONTENT_TYPE_JSON = "application/json; charset=utf-8";
        private const string CONTENT_TYPE_SSE = "text/event-stream; charset=utf-8";
        private const string CONTENT_TYPE_VIDEO_MP4 = "video/mp4";
        private const int MAX_CONCURRENT_REQUESTS = 100; // 最大并发请求数（防止资源耗尽）
        private const int REQUEST_TIMEOUT_SECONDS = 60; // 请求超时时间（秒）
        #endregion

        #region 字段定义
        private readonly string _requestUrl;
        private readonly Form _mainForm;
        private readonly CancellationTokenSource _cts;
        private HttpListener _listener;
        private bool _disposed = false;
        private readonly SemaphoreSlim _concurrencySemaphore; // 并发控制信号量
        #endregion

        #region 构造函数
        public HttpListenerAsyncServer(Form mainForm, string url, CancellationToken cancellationToken = default)
        {
            if (!HttpListener.IsSupported)
                throw new NotSupportedException("当前系统不支持 HttpListener（需Windows XP SP2+/Server 2003+）");

            _mainForm = mainForm ?? throw new ArgumentNullException(nameof(mainForm), "主窗体实例不能为空");
            _requestUrl = url ?? throw new ArgumentNullException(nameof(url), "监听URL不能为空");
            _cts = new CancellationTokenSource();
            _concurrencySemaphore = new SemaphoreSlim(MAX_CONCURRENT_REQUESTS, MAX_CONCURRENT_REQUESTS);
            Constant.htmlProxy = $"{_requestUrl}api/downloadFile";
        }
        #endregion

        #region 监听器启动（优化重试逻辑，规范字段赋值）
        public void StartHttpListener()
        {
            if (_listener != null && _listener.IsListening)
            {
                FileUtils.log("HTTP监听器已启动，无需重复启动", "端口启动");
                return;
            }

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++)
            {
                try
                {
                    FileUtils.log($"尝试启动HTTP监听器（第 {attempt} 次）...", "端口启动");
                    var listener = new HttpListener();
                    listener.Prefixes.Add(_requestUrl);
                    listener.Start(); // 可能抛出HttpListenerException

                    if (listener.IsListening)
                    {
                        _listener = listener;
                        FileUtils.log($"HTTP监听器启动成功！监听地址：{_requestUrl}", "端口启动");
                        return;
                    }

                    listener.Stop(); // 未成功监听，释放临时实例
                }
                catch (HttpListenerException ex)
                {
                    FileUtils.log($"启动异常（第 {attempt} 次，错误码：{ex.ErrorCode}）: {ex.Message}", "端口启动错误");
                }
                catch (Exception ex)
                {
                    FileUtils.log($"启动异常（第 {attempt} 次）: {ex}", "端口启动错误");
                }

                // 未到最大重试次数则异步等待，避免阻塞调用线程
                if (attempt < MAX_RETRIES)
                {
                    Task.Delay(RETRY_DELAY_MS).GetAwaiter().GetResult();
                }
            }

            FileUtils.log("HTTP监听器启动失败，已放弃重试", "端口启动失败");
            throw new CustomException("端口启动失败", 500);
        }
        #endregion

        #region 监听器停止（规范资源释放，防止重复Dispose）
        public void Stop()
        {
            if (_disposed) return;

            // 取消令牌，终止请求循环
            _cts.Cancel();

            // 停止并释放HttpListener
            if (_listener != null)
            {
                try
                {
                    if (_listener.IsListening)
                        _listener.Stop(); // 停止接受新请求
                    _listener.Close(); // 释放所有资源
                }
                catch (Exception ex)
                {
                    FileUtils.log($"监听器停止异常：{ex}", "端口停止错误");
                }
                finally
                {
                    _listener = null;
                }
            }

            // 释放信号量资源
            try
            {
                _concurrencySemaphore?.Dispose();
            }
            catch { }

            // 释放令牌资源
            _cts.Dispose();
            _disposed = true;
            FileUtils.log("HTTP监听器已停止并释放资源", "端口停止");
        }

        // 实现IDisposable，确保资源释放
        public void Dispose()
        {
            Dispose(true);
            GC.SuppressFinalize(this);
        }

        protected virtual void Dispose(bool disposing)
        {
            if (_disposed) return;
            if (disposing)
            {
                Stop(); // 调用停止逻辑
            }
            _disposed = true;
        }
        #endregion

        #region 主循环（异步监听请求）
        public async Task RunAsync()
        {
            if (_listener == null || !_listener.IsListening)
            {
                await FileUtils.LogErrorAsync("监听器未启动或启动失败", "端口启动失败");
                throw new CustomException("端口启动失败", 500);
            }

            await FileUtils.LogAsync("HTTP监听器开始监听请求...", "端口运行");

            // 使用 ConcurrentBag 替代 List+Lock，提升并发性能
            var activeRequests = new ConcurrentDictionary<Task, DateTime>();

            try
            {
                while (!_cts.Token.IsCancellationRequested)
                {
                    try
                    {
                        // 异步获取请求上下文，响应取消令牌
                        var context = await _listener.GetContextAsync();
                        if (context == null || _cts.Token.IsCancellationRequested)
                            continue;

                        // 【关键修复】使用 ThreadPool.QueueUserWorkItem 处理请求（与 HttpListenerServer 保持一致）
                        ThreadPool.QueueUserWorkItem(async state =>
                        {
                            var ctx = (HttpListenerContext)state;
                            bool semaphoreAcquired = false;

                            try
                            {
                                // 并发控制：等待信号量（限制最大并发数）
                                semaphoreAcquired = await _concurrencySemaphore.WaitAsync(TimeSpan.FromSeconds(5), _cts.Token).ConfigureAwait(false);
                                if (!semaphoreAcquired)
                                {
                                    // 超时未获取信号量，返回 503（服务不可用）
                                    await SendServiceUnavailableResponseAsync(ctx).ConfigureAwait(false);
                                    return;
                                }

                                // 创建并启动请求处理任务
                                var requestTask = ProcessRequestAsync(ctx, _cts.Token);
                                activeRequests.TryAdd(requestTask, DateTime.UtcNow);

                                // 异步清理和监控（不阻塞主循环）
                                _ = requestTask.ContinueWith(t =>
                                {
                                    // 释放信号量（确保总是释放）
                                    try
                                    {
                                        _concurrencySemaphore.Release();
                                    }
                                    catch (Exception ex)
                                    {
                                        FileUtils.LogError($"释放信号量异常：{ex.Message}", "http端口监听");
                                    }

                                    // 移除已完成的任务
                                    activeRequests.TryRemove(t, out _);

                                    // 记录异常
                                    if (t.IsFaulted)
                                    {
                                        FileUtils.LogError($"请求处理异常：{t.Exception?.InnerException?.Message ?? t.Exception?.Message}", "http端口监听");
                                    }
                                }, _cts.Token);
                            }
                            catch
                            {
                                // 如果获取信号量失败，确保释放
                                if (semaphoreAcquired)
                                    _concurrencySemaphore.Release();
                                throw;
                            }

                            // 定期清理超时的请求（每100个请求检查一次）
                            if (activeRequests.Count % 100 == 0)
                            {
                                CleanupExpiredRequests(activeRequests);
                            }
                        }, context);
                    }
                    catch (HttpListenerException ex) when (ex.ErrorCode == 995)
                    {
                        // 监听器停止时的正常中断，忽略
                        await FileUtils.LogAsync($"{ex.Message}（监听器正常停止）", "http端口监听", true);
                    }
                    catch (ObjectDisposedException)
                    {
                        // listener已释放，退出循环
                        await FileUtils.LogAsync("监听器已释放，退出请求监听循环", "http端口监听");
                        break;
                    }
                    catch (OperationCanceledException)
                    {
                        // 令牌取消，优雅退出
                        await FileUtils.LogAsync("请求监听循环已取消，优雅退出", "http端口监听", true);
                        break;
                    }
                    catch (Exception ex)
                    {
                        await FileUtils.LogAsync($"请求监听异常：{ex}", "http端口监听");
                        // 短暂延迟，避免异常循环占用CPU
                        await Task.Delay(100, _cts.Token).ConfigureAwait(false);
                    }
                }
            }
            catch (Exception ex)
            {
                await FileUtils.LogAsync($"监听主循环异常：{ex}", "http端口监听", true);
            }
            finally
            {
                // 等待所有活动请求完成（最多等待10秒）
                await WaitForActiveRequestsAsync(activeRequests, TimeSpan.FromSeconds(10)).ConfigureAwait(false);
                Stop(); // 确保资源释放
            }
        }

        /// <summary>
        /// 发送服务不可用响应
        /// </summary>
        private async Task SendServiceUnavailableResponseAsync(HttpListenerContext context)
        {
            try
            {
                context.Response.StatusCode = (int)HttpStatusCode.ServiceUnavailable;
                context.Response.ContentType = "text/plain; charset=utf-8";
                var errorMsg = Encoding.UTF8.GetBytes("服务器繁忙，请稍后重试");
                context.Response.ContentLength64 = errorMsg.Length;
                await context.Response.OutputStream.WriteAsync(errorMsg, 0, errorMsg.Length, _cts.Token).ConfigureAwait(false);
                context.Response.Close();
            }
            catch { /* 忽略异常 */ }
        }

        /// <summary>
        /// 清理过期的请求（超过5分钟）
        /// </summary>
        private void CleanupExpiredRequests(ConcurrentDictionary<Task, DateTime> activeRequests)
        {
            var timeout = TimeSpan.FromMinutes(5);
            var now = DateTime.UtcNow;
            foreach (var kvp in activeRequests.Where(kvp => now - kvp.Value > timeout).ToList())
            {
                activeRequests.TryRemove(kvp.Key, out _);
            }
        }

        /// <summary>
        /// 等待所有活动请求完成
        /// </summary>
        private async Task WaitForActiveRequestsAsync(ConcurrentDictionary<Task, DateTime> activeRequests, TimeSpan timeout)
        {
            if (activeRequests.IsEmpty)
                return;

            try
            {
                await Task.WhenAny(
                    Task.WhenAll(activeRequests.Keys),
                    Task.Delay(timeout)
                ).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                await FileUtils.LogAsync($"等待请求完成异常：{ex}", "http端口监听");
            }
        }
        #endregion

        #region 核心：异步处理请求（拆分后）
        /// <summary>
        /// 异步处理单个HTTP请求
        /// </summary>
        private async Task ProcessRequestAsync(HttpListenerContext context, CancellationToken token)
        {
            var request = context.Request;
            var response = context.Response;
            bool isResponseHandled = false;

            try
            {
                // 1. 基础配置：CORS + 预检请求处理
                AddCorsHeaders(response);
                if (await HandleOptionsRequestAsync(request, response).ConfigureAwait(false))
                {
                    isResponseHandled = true;
                    return;
                }

                // 2. 解析请求路径（防索引越界、空值）
                var (actionPath, queryString) = ParseActionPath(request.Url);
                if (string.IsNullOrWhiteSpace(actionPath))
                {
                    await SendErrorResponseAsync(response, HttpStatusCode.NotFound, "请求路径不能为空").ConfigureAwait(false);
                    isResponseHandled = true;
                    await FileUtils.LogAsync($"没有找到正确的路径", "http端口监听");
                    return;
                }

                // 3. 路由分发（按业务拆分，使用 if-else 链优化）
                if (actionPath.StartsWith("api/config/getFile", StringComparison.OrdinalIgnoreCase))
                {
                    await HandleFileDownloadRequestAsync(request, response, queryString, token).ConfigureAwait(false);
                    isResponseHandled = true;
                }
                else if (actionPath.StartsWith("api/aiRelated/ask", StringComparison.OrdinalIgnoreCase))
                {
                    await HandleSseAskRequestAsync(request, response, token).ConfigureAwait(false);
                    isResponseHandled = true;
                }
                else if (actionPath.StartsWith("api/upload/frontUpload", StringComparison.OrdinalIgnoreCase))
                {
                    await HandleUploadRequestAsync(request, response).ConfigureAwait(false);
                    isResponseHandled = true;
                }
                else if (actionPath.StartsWith("api/downloadFile", StringComparison.OrdinalIgnoreCase))
                {
                    // 不要使用新线程，直接异步处理
                    DownloadFileUtil downloadFileUtil = new DownloadFileUtil();
                    await downloadFileUtil.DownloadFile(request, response, actionPath).ConfigureAwait(false);
                    isResponseHandled = true;
                }
                else
                {
                    await HandleGeneralRequestAsync(request, response, actionPath, token).ConfigureAwait(false);
                    isResponseHandled = true;
                }
            }
            catch (CustomException ex)
            {
                if (!isResponseHandled)
                    await SendErrorResponseAsync(response, (HttpStatusCode)ex.ErrorCode, ex.Message).ConfigureAwait(false);
                await FileUtils.LogAsync($"业务异常：{ex}", "http端口监听");
            }
            catch (FileNotFoundException ex)
            {
                if (!isResponseHandled)
                    await SendErrorResponseAsync(response, HttpStatusCode.NotFound, $"文件不存在：{ex.Message}").ConfigureAwait(false);
                await FileUtils.LogAsync($"文件异常：{ex}", "http端口监听");
            }
            catch (JsonException ex)
            {
                if (!isResponseHandled)
                    await SendErrorResponseAsync(response, HttpStatusCode.BadRequest, $"JSON解析失败：{ex.Message}").ConfigureAwait(false);
                await FileUtils.LogAsync($"JSON解析异常：{ex}", "http端口监听");
            }
            catch (OperationCanceledException)
            {
                // 取消操作，无需记录错误
                if (!isResponseHandled)
                    await SendErrorResponseAsync(response, HttpStatusCode.ServiceUnavailable, "请求已取消").ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                await FileUtils.LogAsync($"请求处理异常：{ex.Message}\n{ex.StackTrace}");
                if (!isResponseHandled)
                    await SendErrorResponseAsync(response, HttpStatusCode.InternalServerError, $"服务器内部错误：{ex.Message}").ConfigureAwait(false);
                await FileUtils.LogAsync($"请求处理异常：{ex}", "http端口监听");
            }
            finally
            {
                try
                {
                    response.Close();
                }
                catch { /* 忽略关闭异常，避免日志污染 */ }
            }
        }
        #endregion

        #region 子方法：CORS头配置
        private static void AddCorsHeaders(HttpListenerResponse response)
        {
            // 使用AppendHeader避免重复添加
            response.AppendHeader("Access-Control-Allow-Origin", "*");
            response.AppendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.AppendHeader("Access-Control-Allow-Headers", "Content-Type, Accept, token, Origin, X-Requested-With");
            response.AppendHeader("Access-Control-Max-Age", "86400"); // 预检请求缓存1天
        }
        #endregion

        #region 子方法：处理OPTIONS预检请求
        private Task<bool> HandleOptionsRequestAsync(HttpListenerRequest request, HttpListenerResponse response)
        {
            if (request.HttpMethod.Equals("OPTIONS", StringComparison.OrdinalIgnoreCase))
            {
                response.StatusCode = (int)HttpStatusCode.OK;
                response.Close();
                return Task.FromResult(true);
            }
            return Task.FromResult(false);
        }
        #endregion

        #region 子方法：解析请求路径（防索引越界）
        private (string actionPath, string queryString) ParseActionPath(Uri requestUri)
        {
            string uri = requestUri.AbsoluteUri;
            string actionPath = uri.Replace(_requestUrl, string.Empty);
            string queryString = string.Empty;

            if (!string.IsNullOrEmpty(actionPath))
            {
                var uriArray = actionPath.Split('?', (char)StringSplitOptions.RemoveEmptyEntries);
                actionPath = uriArray[0];
                queryString = uriArray.Length > 1 ? uriArray[1] : string.Empty;
            }

            return (actionPath?.Trim(), queryString);
        }
        #endregion

        #region 子方法：发送错误响应（统一格式）
        private async Task SendErrorResponseAsync(HttpListenerResponse response, HttpStatusCode statusCode, string message)
        {
            //response.StatusCode = (int)statusCode;
            //response.ContentType = "text/plain; charset=utf-8";

            //var buffer = Encoding.UTF8.GetBytes(message);
            //response.ContentLength64 = buffer.Length;

            //using (var output = response.OutputStream)
            //{
            //    await output.WriteAsync(buffer, 0, buffer.Length).ConfigureAwait(false);
            //}

            //response.Close();
            try
            {
                // ========== 修复点1：检查响应是否已关闭/提交（核心！） ==========
                if (response == null || response.OutputStream == null || response.OutputStream.CanWrite == false)
                {
                    // 响应已提交，直接返回，避免后续操作抛异常
                    FileUtils.LogError("响应已提交，无法发送错误响应：" + message, "HttpServer");
                    return;
                }

                // 准备错误响应内容
                var errorContent = Encoding.UTF8.GetBytes(message);

                // ========== 修复点2：加锁避免多线程同时操作响应（可选但推荐） ==========
                //lock (response) // 锁定当前响应对象，防止多线程并发修改
                //{
                    // 再次检查（双重校验，避免加锁期间响应被提交）
                    if (response.OutputStream.CanWrite == false) return;

                    // 360行：设置ContentLength64前确保响应未提交
                    response.StatusCode = (int)statusCode;
                    response.ContentType = "text/plain; charset=utf-8";
                    response.ContentLength64 = errorContent.Length; // 原异常行

                    // ========== 修复点3：安全写入响应流 + 确保只提交一次 ==========
                    await response.OutputStream.WriteAsync(errorContent, 0, errorContent.Length).ConfigureAwait(false);
                    await response.OutputStream.FlushAsync().ConfigureAwait(false);
                //}
            }
            catch (InvalidOperationException ex)
            {
                // 捕获“提交后操作”的异常，避免Task异常未被观察
                FileUtils.LogError("发送错误响应失败（响应已提交）：" + ex.Message,"HttpServer");
            }
            catch (Exception ex)
            {
                FileUtils.LogError("发送错误响应异常：" + ex.Message, "HttpServer");
            }
            finally
            {
                // ========== 修复点4：安全关闭响应，避免资源泄漏 ==========
                try
                {
                    if (response != null)
                    {
                        response.OutputStream?.Close();
                        response.Close(); // 关闭响应（仅在未提交时）
                    }
                }
                catch
                {
                    // 关闭失败不抛异常，避免覆盖原异常
                }
            }
        }
        #endregion

        #region 子方法：处理文件下载请求（api/config/getFile）
        private async Task HandleFileDownloadRequestAsync(HttpListenerRequest request, HttpListenerResponse response, string queryString, CancellationToken token)
        {
            // 解析文件路径参数
            if (string.IsNullOrEmpty(queryString))
                throw new CustomException("缺少filePath参数", 400);

            var queryParams = HttpUtility.ParseQueryString(queryString);
            string filePath = queryParams["filePath"];
            if (string.IsNullOrEmpty(filePath))
                throw new CustomException("filePath参数不能为空", 400);

            // URL解码 + 路径合法性校验（防路径遍历）
            filePath = HttpUtility.UrlDecode(filePath, Encoding.UTF8);
            if (!IsValidFilePath(filePath))
                throw new CustomException("文件路径不合法（禁止路径遍历）", 400);

            // 校验文件存在性
            if (!File.Exists(filePath))
                throw new FileNotFoundException("文件不存在", filePath);

            // 获取文件信息，支持 Range 请求（断点续传）
            var fileInfo = new FileInfo(filePath);
            long fileLength = fileInfo.Length;
            long rangeStart = 0;
            long rangeEnd = fileLength - 1;
            bool isRangeRequest = false;

            // 处理 Range 头
            var rangeHeader = request.Headers["Range"];
            if (!string.IsNullOrEmpty(rangeHeader))
            {
                try
                {
                    var rangeValues = rangeHeader.Replace("bytes=", "").Split('-');
                    if (rangeValues.Length >= 1 && long.TryParse(rangeValues[0], out long start))
                    {
                        rangeStart = start;
                        if (rangeValues.Length >= 2 && long.TryParse(rangeValues[1], out long end))
                        {
                            rangeEnd = end;
                        }
                        isRangeRequest = true;
                        response.StatusCode = (int)HttpStatusCode.PartialContent;
                        response.ContentType = CONTENT_TYPE_VIDEO_MP4;
                        response.ContentLength64 = rangeEnd - rangeStart + 1;
                        response.Headers.Add("Accept-Ranges", "bytes");
                        response.Headers.Add("Content-Range", $"bytes {rangeStart}-{rangeEnd}/{fileLength}");
                    }
                }
                catch { }
            }

            if (!isRangeRequest)
            {
                response.ContentType = CONTENT_TYPE_VIDEO_MP4;
                response.ContentLength64 = fileLength;
            }

            // 流式返回文件（支持取消令牌）
            using (var fileStream = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.Read, DEFAULT_BUFFER_SIZE, true))
            {
                // 定位到起始位置
                fileStream.Seek(rangeStart, SeekOrigin.Begin);

                var buffer = new byte[DEFAULT_BUFFER_SIZE];
                long remainingBytes = rangeEnd - rangeStart + 1;

                while (remainingBytes > 0 && !token.IsCancellationRequested)
                {
                    int bytesToRead = (int)Math.Min(buffer.Length, remainingBytes);
                    int bytesRead = await fileStream.ReadAsync(buffer, 0, bytesToRead, token).ConfigureAwait(false);

                    if (bytesRead == 0)
                        break;

                    await response.OutputStream.WriteAsync(buffer, 0, bytesRead, token).ConfigureAwait(false);
                    remainingBytes -= bytesRead;

                    // 每读取 64KB 刷新一次，平衡性能和延迟
                    if (remainingBytes % (64 * 1024) == 0 || remainingBytes == 0)
                    {
                        await response.OutputStream.FlushAsync(token).ConfigureAwait(false);
                    }
                }
            }

            response.Close();
        }

        /// <summary>
        /// 校验文件路径合法性（防路径遍历攻击，如../）
        /// </summary>
        private bool IsValidFilePath(string filePath)
        {
            try
            {
                string fullPath = Path.GetFullPath(filePath);
                // 可选：添加白名单目录限制（如仅允许特定目录）
                // return fullPath.StartsWith("C:\\AllowedDir\\", StringComparison.OrdinalIgnoreCase);
                return !fullPath.Contains("..") && Path.IsPathRooted(fullPath);
            }
            catch
            {
                return false;
            }
        }
        #endregion

        #region 子方法：处理SSE问答请求（api/aiRelated/ask）
        private async Task HandleSseAskRequestAsync(HttpListenerRequest request, HttpListenerResponse response, CancellationToken token)
        {
            // 设置SSE响应头（规范方式）
            response.ContentType = CONTENT_TYPE_SSE;
            response.AppendHeader("Cache-Control", "no-cache");
            response.AppendHeader("Connection", "keep-alive");
            response.AppendHeader("X-Accel-Buffering", "no"); // 禁用Nginx缓冲

            // 异步读取POST请求体
            string requestBody;
            try
            {
                using (var reader = new StreamReader(request.InputStream, Encoding.UTF8))
                {
                    requestBody = await reader.ReadToEndAsync().ConfigureAwait(false);
                }
            }
            catch (Exception ex)
            {
                throw new CustomException($"读取请求体失败：{ex.Message}", 400);
            }

            if (string.IsNullOrEmpty(requestBody))
                throw new CustomException("请求体不能为空", 400);

            // 解析DTO
            AskRequestDto dto;
            try
            {
                dto = JsonConvert.DeserializeObject<AskRequestDto>(requestBody)
                    ?? throw new CustomException("请求体格式错误", 400);
            }
            catch (JsonException ex)
            {
                throw new CustomException($"JSON解析失败：{ex.Message}", 400);
            }

            // 异步处理SSE流
            var aiRelatedBll = new AiRelatedBll();
            try
            {
                await aiRelatedBll.AskStream(dto, request, response).ConfigureAwait(false);
            }
            catch (CustomException tie)
            {
                await AiUtils.sendData(response, "error", JsonConvert.SerializeObject(new { code = tie.ErrorCode, msg = tie.Message })).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                await AiUtils.sendData(response, "error", JsonConvert.SerializeObject(new { code = SSE_ERROR_CODE, msg = ex.Message })).ConfigureAwait(false);
            }
            finally
            {
                // 安全关闭SSE响应流
                try
                {
                    response.OutputStream?.Close();
                    response.Close();
                }
                catch { /* 忽略关闭异常 */ }
            }
        }
        #endregion

        #region 子方法：处理文件上传请求（api/upload/frontUpload）
        private async Task HandleUploadRequestAsync(HttpListenerRequest request, HttpListenerResponse response)
        {
            try
            {
                var up = new ClientUploadFile();
                // 使用 Task.Run 将同步的 frontUpload 方法放到线程池执行
                await Task.Run(() => up.frontUpload(request, response)).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                FileUtils.log($"文件上传异常：{ex}", "文件上传错误");
                await SendErrorResponseAsync(response, HttpStatusCode.InternalServerError, $"文件上传失败：{ex.Message}").ConfigureAwait(false);
            }
        }
        #endregion

        #region 子方法：处理通用请求（其他接口）
        private async Task HandleGeneralRequestAsync(HttpListenerRequest request, HttpListenerResponse response, string actionPath, CancellationToken token)
        {
            // 解析请求参数（GET + POST）
            var parameterList = new List<object>();

            // 解析GET参数（并行处理提升性能）
            var queryMap = FormatQueryString(request.QueryString);
            if (queryMap != null && queryMap.Count > 0)
            {
                await FileUtils.LogAsync($"{JsonConvert.SerializeObject(queryMap)}", "获取全部get的参数", true);
                parameterList.AddRange(queryMap.Values);
            }

            // 解析POST参数
            if (request.HttpMethod.Equals("POST", StringComparison.OrdinalIgnoreCase))
            {
                using (var reader = new StreamReader(request.InputStream, Encoding.UTF8))
                {
                    string requestBody = await reader.ReadToEndAsync().ConfigureAwait(false);
                    if (!string.IsNullOrEmpty(requestBody))
                    {
                        parameterList.Add(requestBody);
                    }
                }
            }

            // 调用业务方法
            object[] parameters = parameterList.Count > 0 ? parameterList.ToArray() : null;
            object httpResult = null;

            // 优化：自动检测方法类型，使用对应的调用方式
            if (!string.IsNullOrEmpty(actionPath))
            {
                try
                {
                    var methodInfo = BeanCache.MethodCache.GetMethod(actionPath);
                    if (methodInfo != null)
                    {
                        // 检查方法是否返回 Task（异步方法）
                        bool isAsyncMethod = typeof(Task).IsAssignableFrom(methodInfo.Method.ReturnType);

                        // 特殊处理：immediatelyLocalVideo 方法使用 Fire-and-Forget 模式
                        // 原因：该方法涉及文件复制、视频信息提取等耗时操作，上传大文件时会阻塞 UI
                        // 解决方案：在后台线程启动任务并立即返回响应，任务完成后通知前端
                        if (actionPath.Contains("/immediatelyLocalVideo"))
                        {
                            // 使用 _ = Task.Run() 的 Fire-and-Forget 模式
                            _ = Task.Run(() =>
                            {
                                try
                                {
                                    // 执行任务
                                    var result = MethodCache.InvokeMethod(actionPath, parameters);

                                    // 检查返回的 HttpReponse 是否包含错误
                                    if (result is HttpReponse httpReponse && httpReponse.code != 0)
                                    {
                                        // 任务执行失败，通知前端
                                        string errorMsg = httpReponse.msg ?? "未知错误";
                                        FileUtils.LogError($"immediatelyLocalVideo 任务执行失败: {errorMsg}", "HttpServer");
                                        FrontNotice frontNotice = new FrontNotice();
                                        var requestDataObj = new Dictionary<string, object>();
                                        requestDataObj["code"] = 0;
                                        requestDataObj["status"] = 200;
                                        requestDataObj["action"] = "notice";
                                        var data = new Dictionary<string, object>();
                                        data["alertType"] = 1;
                                        data["statusType"] = "warning";
                                        data["title"] = "提示";
                                        data["msg"] = $"提取文案任务失败：{errorMsg}";
                                        data["duration"] = 0;
                                        requestDataObj["data"] = data;
                                        frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                                    }
                                    else
                                    {
                                        // 任务成功完成，刷新页面
                                        try
                                        {
                                            // 通过 UI 线程刷新页面
                                            if (_mainForm != null && !_mainForm.IsDisposed && _mainForm.IsHandleCreated)
                                            {
                                                _mainForm.BeginInvoke(new Action(() =>
                                                {
                                                    try
                                                    {
                                                        // 通过反射调用 PictureBox_Click 方法,这会触发页面刷新
                                                        var pictureBoxClickMethod = _mainForm.GetType().GetMethod("PictureBox_Click",
                                                            System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
                                                        pictureBoxClickMethod?.Invoke(_mainForm, new object[] { null, null });
                                                    }
                                                    catch (Exception ex)
                                                    {
                                                        FileUtils.LogError($"刷新页面失败：{ex}", "HttpServer");
                                                    }
                                                }));
                                            }
                                        }
                                        catch (Exception ex)
                                        {
                                            FileUtils.LogError($"调度刷新页面失败：{ex}", "HttpServer");
                                        }
                                    }
                                }
                                catch (Exception ex)
                                {
                                    string errorMessage = ex.Message;
                                    FileUtils.LogError($"immediatelyLocalVideo 后台任务异常：{ex}", "HttpServer");
                                    // 任务失败通知前端,确保错误消息被正确传递
                                    //NotifyFrontComplete("immediatelyLocalVideo", "error", $"提取文案任务失败: {errorMessage}", errorMessage);
                                    FrontNotice frontNotice = new FrontNotice();
                                    var requestDataObj = new Dictionary<string, object>();
                                    requestDataObj["code"] = 0;
                                    requestDataObj["status"] = 200;
                                    requestDataObj["action"] = "notice";
                                    var data = new Dictionary<string, object>();
                                    data["alertType"] = 1;
                                    data["statusType"] = "warning";
                                    data["title"] = "提示";
                                    data["msg"] = $"提取文案任务失败: {errorMessage}";
                                    data["duration"] = 0;
                                    requestDataObj["data"] = data;
                                    frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                                }
                            }).ConfigureAwait(false);

                            // 立即返回成功响应，不等待后台任务完成
                            httpResult = new { code = 0, msg = "提取文案任务已启动" };
                        }
                        else if (isAsyncMethod)
                        {
                            // 异步方法：使用 MethodAsyncCache，传入主窗体用于UI线程操作
                            // 注意：异步方法内部如果需要打开窗体，应通过 _mainForm.Invoke 或 _mainForm.BeginInvoke 切换到UI线程
                            httpResult = await MethodAsyncCache.InvokeMethodAsync(
                                actionPath,
                                parameters,
                                _mainForm,
                                _cts.Token
                            ).ConfigureAwait(false);
                        }
                        else
                        {
                            // 同步方法：根据是否需要UI线程决定调用方式
                            if (_mainForm != null && !_mainForm.IsDisposed && _mainForm.IsHandleCreated)
                            {
                                if (_mainForm.InvokeRequired)
                                {
                                    // 需要UI线程，使用 Invoke 同步调用（与 HttpListenerServer 保持一致）
                                    // 注意：虽然 Invoke 会阻塞当前线程，但当前是后台HTTP处理线程，不会阻塞UI线程
                                    httpResult = _mainForm.Invoke(new Func<object>(() =>
                                    {
                                        return MethodCache.InvokeMethod(actionPath, parameters);
                                    }));
                                }
                                else
                                {
                                    // 已在UI线程，直接调用
                                    httpResult = MethodCache.InvokeMethod(actionPath, parameters);
                                }
                            }
                            else
                            {
                                // 窗体不可用时，使用 Task.Run 在后台线程执行
                                httpResult = await Task.Run(() =>
                                    MethodCache.InvokeMethod(actionPath, parameters)
                                ).ConfigureAwait(false);
                            }
                        }
                    }
                    else
                    {
                        throw new CustomException($"找不到对应的方法: {actionPath}", 404);
                    }
                }
                catch (OperationCanceledException)
                {
                    throw new CustomException("请求已取消", 499);
                }
                catch (CustomException)
                {
                    // 业务异常直接抛出
                    throw;
                }
                catch (Exception ex)
                {
                    await FileUtils.LogErrorAsync(
                        content: $"方法调用异常：{ex.Message}",
                        action: "HandleGeneralRequestAsync",
                        isDebugger: true
                    );
                    throw new CustomException($"请求处理失败：{ex.Message}", 500);
                }
            }

            if (httpResult == null)
                throw new CustomException("业务方法返回空结果", 500);

            // 序列化返回结果（处理long转string）
            var settings = new JsonSerializerSettings
            {
                Converters = { new LongToStringConverter() },
                NullValueHandling = NullValueHandling.Ignore
            };
            string jsonResult = JsonConvert.SerializeObject(httpResult, settings);
            var buffer = Encoding.UTF8.GetBytes(jsonResult);

            // 返回JSON响应
            response.ContentType = CONTENT_TYPE_JSON;
            response.ContentLength64 = buffer.Length;
            response.KeepAlive = true;

            try
            {
                if (response.OutputStream == null || !response.OutputStream.CanWrite)
                    return;
                await response.OutputStream.WriteAsync(buffer, 0, buffer.Length, token).ConfigureAwait(false);
                await response.OutputStream.FlushAsync().ConfigureAwait(false);
            }
            catch (IOException ex) when (ex.Message.Contains("指定的网络名不再可用") ||
                                      ex.Message.Contains("网络名不再可用"))
            {
                // 客户端网络中断，静默处理
                await FileUtils.LogErrorAsync(
                    content: $"客户端网络中断：{ex.Message}",
                    action: "HandleGeneralRequestAsync",
                    isDebugger: false
                );
                // 不抛出异常，静默返回
            }
            catch (HttpListenerException ex) when (ex.ErrorCode == 64 || ex.ErrorCode == 995)
            {
                // 客户端断开连接或监听器停止，静默处理
                await FileUtils.LogErrorAsync(
                    content: $"连接已断开（错误码:{ex.ErrorCode}）",
                    action: "HandleGeneralRequestAsync",
                    isDebugger: false
                );
            }
            catch (ObjectDisposedException)
            {
                // 流已释放，静默处理
                await FileUtils.LogErrorAsync(
                    content: $"响应流已释放",
                    action: "HandleGeneralRequestAsync",
                    isDebugger: false
                );
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync(
                    content: $"客户端已断开连接，跳过响应写入：{ex.Message}",
                    action: "HandleGeneralRequestAsync",
                    isDebugger: false
                );
            }
        }
        // 核心：替代IsClientConnected的检测方法（通用所有.NET Framework版本）
        private bool CheckClientConnected(HttpListenerResponse response)
        {
            if (response == null || response.OutputStream == null)
                return false;

            try
            {
                // 写入1个空字节检测连接（客户端断开会抛64号错误）
                response.OutputStream.Write(new byte[0], 0, 0);
                return true; // 写入成功 → 客户端未断开
            }
            catch (HttpListenerException ex) when (ex.ErrorCode == 64)
            {
                return false; // 64号错误 → 客户端已断开
            }
            catch
            {
                return false; // 其他异常 → 视为客户端断开
            }
        }
        #endregion

        #region 辅助方法：通知前端任务完成
        /// <summary>
        /// 通过 FrontNotice 通知前端任务完成状态
        /// </summary>
        /// <param name="action">操作名称</param>
        /// <param name="status">状态：success/error</param>
        /// <param name="message">提示消息</param>
        /// <param name="detail">详细信息（错误消息等）</param>
        private void NotifyFrontComplete(string action, string status, string message, string detail = null)
        {
            try
            {
                if (_mainForm != null && !_mainForm.IsDisposed && _mainForm.IsHandleCreated)
                {
                    _mainForm.BeginInvoke(new Action(() =>
                    {
                        try
                        {
                            var requestDataObj = new Dictionary<string, object>
                            {
                                ["code"] = 0,
                                ["status"] = 200,
                                ["action"] = "backgroundTaskComplete",
                                ["data"] = new
                                {
                                    action = action,
                                    status = status,
                                    message = message,
                                    detail = detail,
                                    timestamp = DateTime.Now.Ticks
                                }
                            };
                            var frontNotice = new FrontNotice();
                            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogError($"通知前端失败：{ex}", "NotifyFrontComplete");
                        }
                    }));
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"调度通知前端任务失败：{ex}", "NotifyFrontComplete");
            }
        }
        #endregion

        #region 辅助方法：格式化QueryString（优化URL解码）
        private static Dictionary<string, object> FormatQueryString(NameValueCollection queryParams)
        {
            if (queryParams == null || queryParams.Count == 0)
                return null;

            var map = new Dictionary<string, object>(StringComparer.OrdinalIgnoreCase);
            foreach (string key in queryParams)
            {
                if (string.IsNullOrEmpty(key))
                    continue;

                string value = queryParams[key];
                if (!string.IsNullOrEmpty(value))
                    value = HttpUtility.UrlDecode(value, Encoding.UTF8); // 改用标准解码，移除自定义DecodeUrl

                map.Add(key, value);
            }
            return map;
        }
        #endregion

        #region 辅助方法：反射获取方法（原逻辑保留，补充注释）
        private MethodInfo GetMethodInfo(string fullClassName, string methodName, out int ex)
        {
            ex = 0;
            // 从当前程序集获取类型
            Type type = Type.GetType(fullClassName);
            if (type == null)
            {
                FileUtils.log("找不到对应的控制器");
                ex = 1;
                return null;
            }

            // 获取方法（可补充：支持重载、非公共方法等）
            MethodInfo method = type.GetMethod(methodName);
            if (method == null)
            {
                FileUtils.log("找不到对应的方法");
                ex = 2;
                return null;
            }

            return method;
        }
        #endregion
    }

    #region JSON转换器：Long转String
    public class LongToStringConverter : JsonConverter<long>
    {
        public override long ReadJson(JsonReader reader, Type objectType, long existingValue, bool hasExistingValue, JsonSerializer serializer)
        {
            if (reader.TokenType == JsonToken.String && long.TryParse((string)reader.Value, out var result))
                return result;
            if (reader.TokenType == JsonToken.Integer)
                return (long)reader.Value;

            throw new JsonException($"无法将 {reader.Value} 转换为long类型");
        }

        public override void WriteJson(JsonWriter writer, long value, JsonSerializer serializer)
        {
            writer.WriteValue(value.ToString());
        }
    }
    #endregion
}