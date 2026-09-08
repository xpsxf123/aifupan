using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.IO;
using System.Net;
using System.Text;
using System.Reflection;
using ReviewAnalysis.BeanCache;
using System.Windows.Forms;
using System.Text.RegularExpressions;
using System.Threading;
using douyin.Utils;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Dto;
using System.Threading.Tasks;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Ai;
using System.Diagnostics;
using ReviewAnalysis.upload;

namespace ReviewAnalysis.HttpServer
{
    public class HttpListenerServer
    {
        public static string RequestUrl;
        private Form _mainForm;
        private HttpListener listener = null;
        public HttpListenerServer(Form mainForm, string _url)
        {
            RequestUrl = _url;
            _mainForm = mainForm;
        }

        public void HttpListenerRequest()
        {
            const int maxRetries = 3;
            const int retryDelayMs = 1000; // 3秒

            for (int attempt = 1; attempt <= maxRetries; attempt++)
            {
                try
                {
                    FileUtils.log($"尝试启动 HTTP 监听器（第 {attempt} 次）...", "端口启动");
                    HttpListener listener = new HttpListener();
                    listener.Prefixes.Add(RequestUrl);

                    listener.Start(); // 可能抛出 HttpListenerException

                    bool isStart = listener.IsListening;

                    if (isStart)
                    {
                        FileUtils.log("HTTP 监听器启动成功！", "端口启动");
                        this.listener = listener;
                        return; // 正常退出
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.log($"启动异常（第 {attempt} 次）: {ex}", "端口启动错误");
                }

                // 如果还没到最大重试次数，等待后重试
                if (attempt < maxRetries)
                {
                    Thread.Sleep(retryDelayMs);
                }
            }

            FileUtils.log("HTTP 监听器启动失败，已放弃重试。", "端口启动失败");
            throw new CustomException("端口启动失败");
        }

        public void close()
        {
            if (listener != null)
            {
                if (listener.IsListening)
                {
                    listener.Stop(); // 停止接受新请求
                }
                listener.Close();    // 释放资源（内部会调用 Dispose）
            }
        }

        public async Task run()
        {
            if (listener == null)
            {
                FileUtils.LogError("listener = null", "端口启动失败");
                throw new CustomException("端口启动失败");
            }
            while (true)
            {
                try
                {
                    HttpListenerContext context = await listener?.GetContextAsync();
                    ThreadPool.QueueUserWorkItem(ProcessRequest, context);

                }
                catch (ObjectDisposedException)
                {
                    // listener 已被 Dispose，正常退出
                    break;
                }
                catch (Exception ex)
                {
                    DebugLog.WriteLine($"Exception: {ex.Message}");
                    FileUtils.log($"{ex}", "http端口监听");
                }

            }
        }

        public async void ProcessRequest(object obj)
        {
            try
            {
                var context = (HttpListenerContext)obj;
                HttpListenerRequest request = context.Request;
                HttpListenerResponse response = context.Response;

                // 添加 CORS 头信息
                AddCorsHeaders(response);

                // 处理 OPTIONS 请求（预检请求）
                if (request.HttpMethod == "OPTIONS")
                {
                    response.StatusCode = (int)HttpStatusCode.OK;
                    response.Close();
                    return;
                }
                string uri = request.Url.AbsoluteUri;
                string actionPath = uri.Replace(RequestUrl, string.Empty);
                string[] uriArray = actionPath.Split('?');
                actionPath = uriArray[0];
                if (string.IsNullOrEmpty(actionPath) || actionPath == "")
                {
                    response.StatusCode = 404;
                    string notFoundString = actionPath + " - Not Found";
                    byte[] notFoundBuffer = Encoding.UTF8.GetBytes(notFoundString);
                    response.ContentLength64 = notFoundBuffer.Length;
                    response.OutputStream.Write(notFoundBuffer, 0, notFoundBuffer.Length);
                    response.OutputStream.Close();
                    DebugLog.WriteLine("没有找到正确的路径");
                    return;
                }
                else if (actionPath.StartsWith("api/config/getFile"))
                {
                    // 获取文件流
                    // api/config/getFile?filePath=C:\test\test.mp4
                    string filePath = uriArray[1].Split('=')[1];
                    filePath = System.Web.HttpUtility.UrlDecode(filePath, Encoding.UTF8);

                    //byte[] bytes = File.ReadAllBytes(filePath);
                    //response.ContentType = "video/mp4";
                    //response.ContentLength64 = bytes.Length;
                    //await response.OutputStream.WriteAsync(bytes, 0, bytes.Length);
                    //response.OutputStream.Close();
                    //response.Close();
                    //return;

                    const int bufferSize = 8192000; // 8MB 缓冲区
                    byte[] bytes = new byte[bufferSize];

                    using (var fileStream = new FileStream(filePath, FileMode.Open, FileAccess.Read))
                    {
                        response.ContentType = "video/mp4";
                        response.ContentLength64 = fileStream.Length;

                        int bytesRead;
                        while ((bytesRead = await fileStream.ReadAsync(bytes, 0, bufferSize)) > 0)
                        {
                            await response.OutputStream.WriteAsync(bytes, 0, bytesRead);
                        }
                    }

                    // 关闭响应流
                    response.OutputStream.Close();
                    response.Close();

                    return;

                }
                else if (actionPath.StartsWith("api/aiRelated/ask"))
                {
                    try
                    {
                        response.Headers.Add("Content-Type", "text/event-stream;charset=utf-8");
                        response.Headers.Add("Cache-Control", "no-cache");
                        response.Headers.Add("Connection", "keep-alive");
                        AiRelatedBll aiRelatedBll = new AiRelatedBll();
                        // 处理 POST 请求
                        using (var reader = new StreamReader(request.InputStream, Encoding.UTF8))
                        {
                            string requestBody = await reader.ReadToEndAsync();
                            if (!string.IsNullOrEmpty(requestBody))
                            {
                                AskRequestDto dto = JsonConvert.DeserializeObject<AskRequestDto>(requestBody);
                                try
                                {
                                    // 调用问答接口
                                    await aiRelatedBll.AskStream(dto, request, response);
                                }
                                catch (CustomException tie)
                                {
                                    await AiUtils.sendData(response, "error", JsonConvert.SerializeObject(new { code = tie.ErrorCode, msg = tie.Message }));
                                }
                                catch(Exception ex)
                                {
                                    await AiUtils.sendData(response, "error", JsonConvert.SerializeObject(new { code = 7005, msg = ex.Message }));
                                }
                                finally
                                {
                                    // 关闭响应流
                                    if (response.OutputStream != null)
                                    {
                                        response.OutputStream.Close();
                                    }
                                    if(response != null)
                                    {
                                        response.Close();
                                    }
                                }

                            }
                        }
                    }
                    catch (Exception e)
                    {
                        FileUtils.LogError(e.Message, "api/aiRelated/ask异常");
                    }
                    
                    return;
                }
                else if (actionPath.StartsWith("api/upload/frontUpload"))
                {
                    ClientUploadFile up = new ClientUploadFile();
                    up.frontUpload(request, response);
                    return;
                }
                else if (actionPath.StartsWith("api/downloadFile"))
                {
                    // 不要使用新线程，直接异步处理
                    DownloadFileUtil downloadFileUtil = new DownloadFileUtil();
                    downloadFileUtil.DownloadFile(request, response, actionPath);
                    return;
                }

                Dictionary<string, object> map = FormatQueryString(request.QueryString);
                object[] parameters = null;
                List<object> parameterList = new List<object>();
                if (map != null && map.Count > 0)
                {
                    FileUtils.log($"{JsonConvert.SerializeObject(map)}", $"获取全部get的参数", true);
                    foreach (KeyValuePair<string, object> para in map)
                    {
                        parameterList.Add(para.Value);
                    }
                }
                if (request.HttpMethod == "POST")
                {

                    //// 处理 POST 请求
                    using (var reader = new StreamReader(request.InputStream, Encoding.UTF8))
                    {
                        string requestBody = await reader.ReadToEndAsync();
                        //FileUtils.log($"{requestBody}", $"获取全部post的参数", true);
                        if (!string.IsNullOrEmpty(requestBody))
                        {
                            // object data = JsonConvert.DeserializeObject(requestBody);
                            parameterList.Add(requestBody);
                        }
                    }
                }
                if (parameterList.Count > 0)
                {
                    parameters = parameterList.ToArray();
                }
                object httpResult = null;
                _mainForm.Invoke(new Action(() =>
                {
                    httpResult = MethodCache.InvokeMethod(actionPath, parameters);
                }));
                string a = httpResult.ToString();

                // 创建 JsonSerializerSettings 对象 
                JsonSerializerSettings settings = new JsonSerializerSettings();

                // 添加自定义转换器，将 long 类型转换为 string 类型 
                settings.Converters.Add(new LongToStringConverter());


                string b = JsonConvert.SerializeObject(httpResult, settings);
                byte[] buffer = Encoding.UTF8.GetBytes(b);
                response.ContentType = "application/json";
                response.ContentLength64 = buffer.Length;
                //返回响应
                using (Stream output = response.OutputStream)
                {
                    await output.WriteAsync(buffer, 0, buffer.Length);
                }
            } catch (Exception ex)
            {
                DebugLog.WriteLine($"Exception: {ex.Message}");
                FileUtils.log($"{ex}", "http端口监听");
            }
            
        }

        private static void AddCorsHeaders(HttpListenerResponse response)
        {
            response.AddHeader("Access-Control-Allow-Origin", "*");
            response.AddHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.AddHeader("Access-Control-Allow-Headers", "Content-Type, Accept,token,Origin, X-Requested-With");
        }

        private static Dictionary<string, object> FormatQueryString(NameValueCollection queryParams)
        {
            Dictionary<string, object> map = new Dictionary<string, object>();
            foreach (string key in queryParams)
            {
                //object value = queryParams[key];
                object value = WebUtility.UrlDecode(queryParams[key]);
                //object value = DecodeUrl(queryParams[key]);
                map.Add(key, value);
                
            }
            return map;
        }

        static string DecodeUrl(string encodedString)
        {
            // 将 %xx 编码替换为对应字符
            string decodedString = Regex.Replace(encodedString, @"%[0-9a-fA-F]{2}", m =>
            {
                string hex = m.Value.Substring(1, 2);
                byte[] bytes = new byte[1];
                bytes[0] = Convert.ToByte(hex, 16);
                return Encoding.UTF8.GetString(bytes);
            });

            // 替换 + 为空格
            decodedString = decodedString.Replace("+", " ");

            return decodedString;
        }



        private MethodInfo GetMethodInfo(string fullClassName, string methodName,out int ex) 
        {
          
            // 从当前程序集获取类型
            Type type = Type.GetType(fullClassName);
            if (type == null)
            {
                DebugLog.WriteLine("找不到对应的控制器");
                ex = 1;
                return null;
            }

            // 获取方法
            MethodInfo method = type.GetMethod(methodName);

            if (method == null)
            {
                DebugLog.WriteLine("找不到对应的方法");
                ex = 2;
                return null;
            }
            ex = 0;
            return method;
        }
    }
}
