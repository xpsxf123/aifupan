using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using OpenCvSharp.XImgProc;
using ReviewAnalysis.Ai.impl;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Ai
{
    public class AiUtils
    {

        public static string aiContextCompletionsUrl = "https://ark.cn-beijing.volces.com/api/v3/context/chat/completions";
        public static string aiCompletionsUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
        public static string aiFilePath = Path.GetFullPath("aiData");

        /// <summary>
        /// ai会话数据保存文件路径
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <returns></returns>
        public static string GetContextFilePath(int type, string sourceId, int sourceType, string contextId)
        {
            return $"{aiFilePath}/{ReplayHttpUtils.UserId}-{sourceType}-{type}/{sourceId}/{contextId}.txt";
        }

        /// <summary>
        /// ai历史数据的结构文件路径
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <returns></returns>
        public static string GetStructureFilePath(int type, string sourceId, int sourceType)
        {
            return $"{aiFilePath}/{ReplayHttpUtils.UserId}-{sourceType}-{type}/{sourceId}/structure.txt";
        }

        /// <summary>
        /// 获取问题的文件夹地址
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <returns></returns>
        public static string GetAiProblemPath(int type, string sourceId, int sourceType)
        {
            return $"{aiFilePath}/{ReplayHttpUtils.UserId}-{sourceType}-{type}/{sourceId}";
        }

        /// <summary>
        /// 获取ai历史数据的code
        /// </summary>
        /// <param name="contextId"></param>
        /// <param name="code"></param>
        /// <returns></returns>
        public static string GetStruacturePrefix(string contextId, string code)
        {
            return $"ai_{contextId}_{code}";
        }

        /// <summary>
        /// 获取ai历史段落的文件夹
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <returns></returns>
        public static string GetHistoryDirectoryPath(int type, string sourceId, int sourceType)
        {
            return $"{aiFilePath}/{ReplayHttpUtils.UserId}-{sourceType}-{type}/{sourceId}/history/";
        }

        /// <summary>
        /// 获取ai历史段落的文件路径
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="code"></param>
        /// <returns></returns>
        public static string GetHistoryFilePath(int type, string sourceId, int sourceType, string code)
        {
            return $"{GetHistoryDirectoryPath(type, sourceId, sourceType)}{code}.txt";
        }

        /// <summary>
        /// 获取ai的临时token
        /// </summary>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static AiTempTokenDto getTempArkToekn()
        {
            var request = new HttpRequestMessage(HttpMethod.Get, ReplayHttpUtils.BaseUrl + "/openapi/v2200/getArkTempToken")
            {
                Headers =
                {
                    { "token", ReplayHttpUtils.Token }
                }
            };
            using (HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                var responseBody = response.Content.ReadAsStringAsync().Result;
                if (!string.IsNullOrEmpty(responseBody))
                {
                    var res = JsonConvert.DeserializeObject<dynamic>(responseBody);
                    if (res?.code == 0)
                    {
                        return JsonConvert.DeserializeObject<AiTempTokenDto>(res.data.ToString());
                        //AiTempTokenDto result = new AiTempTokenDto();
                        //result.token = result.token;
                        //result.contextSaveTime = result.contextSaveTime;
                        //return result;
                    }
                }
            }
            using (HttpClient Client = new HttpClient())

                throw new CustomException("获取临时临牌失败");
        }

        /// <summary>
        /// 获取用户能用的上下文id
        /// </summary>
        /// <param name="key"></param>
        /// <param name="type"></param>
        /// <returns></returns>
        public static string getContextId(string sourceId, int sourceType, int type, int aiModel = 0)
        {
            if (aiModel == 0)
            {
                var request = new HttpRequestMessage(HttpMethod.Get, ReplayHttpUtils.BaseUrl + $"/openapi/v2200/getContextId?sourceId={sourceId}&sourceType={sourceType}&type={type}")
                {
                    Headers =
                    {
                        { "token", ReplayHttpUtils.Token }
                    }
                };
                using (HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    var responseBody = response.Content.ReadAsStringAsync().Result;
                    if (!string.IsNullOrEmpty(responseBody))
                    {
                        var res = JsonConvert.DeserializeObject<dynamic>(responseBody);
                        if (res?.code == 0)
                        {
                            return res.data;
                        }
                    }
                }

            }
            else if (aiModel == 1 || aiModel == 2)
            {
                return AiRelatedCacheManager.GetFalseContextId(sourceId, sourceType, type);
            }
            return null;
        }

        /// <summary>
        /// 获取用户能用的上下文id
        /// </summary>
        /// <param name="dto"></param>
        /// <returns></returns>
        public static string getContextIdNew(AskRequestDto dto)
        {
            if (dto.resourceType != 0)
            {
                return AiRelatedCacheManager.GetFalseContextId(dto.sourceId, dto.sourceType, dto.type);
            }
            if (dto.useModelType == 0)
            {
                return AiRelatedApi.getContextId(dto);

                //var request = new HttpRequestMessage();
                //request.Method = HttpMethod.Post;
                //request.Headers.Add("token", ReplayHttpUtils.Token);
                //request.RequestUri = new Uri(ReplayHttpUtils.BaseUrl + "/openapi/aiRelated/getContextId");
                //request.Content = new StringContent(JsonConvert.SerializeObject(dto), Encoding.UTF8, "application/json");

                //using (HttpClient Client = new HttpClient())
                //{
                //    var response = Client.SendAsync(request).Result;

                //    if (!response.IsSuccessStatusCode)
                //    {
                //        FileUtils.LogError($"/openapi/aiRelated/getContextId调用失败,response = {response}");
                //    }

                //    var responseBody = response.Content.ReadAsStringAsync().Result;
                //    if (!string.IsNullOrEmpty(responseBody))
                //    {
                //        var res = JsonConvert.DeserializeObject<dynamic>(responseBody);
                //        if (res?.code == 0)
                //        {
                //            return res.data;
                //        }
                //    }
                //}
            }
            else if (dto.useModelType == 1)
            {
                return AiRelatedCacheManager.GetFalseContextId(dto.sourceId, dto.sourceType, dto.type);
            }
            return null;
        }

        /// <summary>
        /// 更新上下文缓存id
        /// </summary>
        /// <param name="dto"></param>
        public static void updateContextId(AskRequestDto dto)
        {
            if (dto.resourceType != 0) return;
            if(dto.useModelType == 0)
            {
                AiRelatedApi.updateContextId(dto);

                //var request = new HttpRequestMessage();
                //request.Method = HttpMethod.Post;
                //request.Headers.Add("token", ReplayHttpUtils.Token);
                //request.RequestUri = new Uri(ReplayHttpUtils.BaseUrl + "/openapi/aiRelated/updateContextId");
                //request.Content = new StringContent(JsonConvert.SerializeObject(dto), Encoding.UTF8, "application/json");

                //using (HttpClient Client = new HttpClient())
                //{
                //    var response = Client.SendAsync(request).Result;

                //    if (!response.IsSuccessStatusCode)
                //    {
                //        FileUtils.LogError($"/openapi/aiRelated/updateContextId调用失败,response = {response}");
                //    }

                //    var responseBody = response.Content.ReadAsStringAsync().Result;
                //}
            }
        }

        /// <summary>
        /// <summary>
        /// 对话(Chat)-文本 API
        /// </summary>
        /// <param name="config"></param>
        /// <param name="dto"></param>
        /// <param name="contextId"></param>
        /// <param name="content"></param>
        /// <param name="request"></param>
        /// <param name="response"></param>
        /// <returns></returns>
        public static async Task<CompletionsDto> chatCompletions(AiTempTokenDto config, AskRequestDto dto, string contextId, string content, HttpListenerRequest request, HttpListenerResponse response)
        {
            CompletionsDto completionsDto = new CompletionsDto();
            try
            {
                var msgList = new List<object>();
                msgList.Add(new { role = "user", content = $"{content}" });
                if (!string.IsNullOrEmpty(dto.identity)) msgList.Add(new { role = "system", content = dto.identity });

                var requestMsg = new HttpRequestMessage();
                requestMsg.Method = HttpMethod.Post;
                requestMsg.Headers.Add("Authorization", $"Bearer {config.token}");
                requestMsg.Headers.Add("Accept", $"text/event-stream");
                requestMsg.Headers.Add("Connection", "keep-alive");
                requestMsg.RequestUri = new Uri(aiCompletionsUrl);
                var obj = new
                {
                    model = config.modelId,
                    messages = msgList,
                    thinking = new
                    {
                        type = dto?.thinkingType ?? "enabled"
                    },
                    stream = true,
                    max_tokens = config.lastHistoryTokens,
                    frequency_penalty = 0.01,
                    stream_options = new
                    {
                        include_usage = true
                    }
                };
                string body = JsonConvert.SerializeObject(obj);
                FileUtils.log($"model={config.modelId}, max_tokens ={config.lastHistoryTokens}", "/api/v3/chat/completions接口参数", true);
                requestMsg.Content = new StringContent(body, Encoding.UTF8, "application/json");
                using (HttpClient client = new HttpClient())
                {
                    // 设置超时时间
                    client.Timeout = TimeSpan.FromSeconds(60 * 5);
                    client.DefaultRequestHeaders.Add("Authorization", $"Bearer {config.token}");

                    try
                    {
                        Choices choices = new Choices();
                        HttpContent httpContent = new StringContent(body, Encoding.UTF8, "application/json");
                        HttpResponseMessage httpRequestMessage = await client.SendAsync(requestMsg, HttpCompletionOption.ResponseHeadersRead);

                        // 从响应头中获取 Request ID
                        if (httpRequestMessage.Headers.TryGetValues("X-Request-ID", out var requestIds))
                        {
                            FileUtils.log(requestIds.FirstOrDefault(), "当前Request ID");
                            choices.requestId = requestIds.FirstOrDefault();
                        }

                        // 确保响应状态码是200 OK（或其他表示成功的状态码）
                        if (!httpRequestMessage.IsSuccessStatusCode)
                        {
                            FileUtils.LogError($"调用对话(Chat)-文本失败，状态码:{httpRequestMessage.StatusCode}, 信息：{await httpRequestMessage.Content.ReadAsStringAsync()}", "调用对话(Chat)-文本失败");
                            LogServerUtils.ErrorLog(
                                $"调用对话(Chat)-文本API失败",
                                $"调用对话(Chat)-文本接口报错，状态码:{httpRequestMessage.StatusCode}",
                                $"requestId={requestIds},信息：{await httpRequestMessage.Content.ReadAsStringAsync()}"
                            );
                            completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                            completionsDto.status = 1;
                            return completionsDto;
                        }



                        completionsDto.totalTokenNum = 0;
                        completionsDto.id = "";
                        string finishReason = "";
                        using (var stream = await httpRequestMessage.Content.ReadAsStreamAsync())
                        using (var reader = new StreamReader(stream))
                        {
                            string result = "";
                            string thinkingAll = "";
                            string line;
                            while ((line = await reader.ReadLineAsync()) != null)
                            {
                                if (!string.IsNullOrEmpty(line))
                                {
                                    if (line.StartsWith("data: "))
                                    {
                                        //File.AppendAllLines("C:\\Users\\JY\\Desktop\\工作\\testTemp.txt", new string[] { line });
                                        string tempStr = line.Replace("data: ", "");
                                        if (string.IsNullOrEmpty(tempStr)) continue;
                                        if ("[DONE]".Equals(tempStr))
                                        {
                                            continue;
                                        }
                                        string message = "";
                                        string thinking = "";
                                        string dataStr = "";
                                        string type = "";
                                        var settings = new JsonSerializerSettings
                                        {
                                            ContractResolver = new CamelCasePropertyNamesContractResolver() // 自动转小驼峰 
                                        };
                                        DouBaoChatStreamDto res = JsonConvert.DeserializeObject<DouBaoChatStreamDto>(tempStr, settings);
                                        completionsDto.id = res.id;

                                        if (res?.choices != null && res.choices?.Count > 0)
                                        {
                                            if (res?.choices != null && res?.choices?.Count > 0)
                                            {
                                                dynamic temp = JsonConvert.DeserializeObject<dynamic>(JsonConvert.SerializeObject(res.choices[0].delta));
                                                if (temp.content != null && !string.IsNullOrEmpty(temp.content.ToString()))
                                                {
                                                    message = temp?.content ?? "";
                                                    dataStr = message;
                                                    type = "text";
                                                }
                                                else if (temp.reasoning_content != null && !string.IsNullOrEmpty(temp.reasoning_content.ToString()))
                                                {
                                                    thinking = temp?.reasoning_content ?? "";
                                                    dataStr = thinking;
                                                    type = "thinking";
                                                }
                                            }
                                            if (!string.IsNullOrEmpty(dataStr) && !string.IsNullOrEmpty(type))
                                            {
                                                await AiUtils.sendData(response, "message", JsonConvert.SerializeObject(new { content = dataStr, type }));
                                            }
                                            result += message;
                                            thinkingAll += thinking;
                                            if (res?.choices?[0]?.finishReason != null)
                                            {
                                                finishReason = res?.choices?[0]?.finishReason;
                                                FileUtils.log(tempStr, "api/v3/chat/completions接口最后返回的数据", true);
                                            }
                                        }
                                        if (res != null && res.usage != null && res.usage.totalTokens != null && res.usage.totalTokens >= 0)
                                        {
                                            completionsDto.totalTokenNum = res.usage.totalTokens;
                                            completionsDto.usage = res.usage;
                                        }
                                    }
                                }
                            }
                            String str = "";
                            if (!string.IsNullOrEmpty(thinkingAll))
                            {
                                str += $"<div class='deepThinking'>{thinkingAll}</div>\n\n";
                            }
                            if (!string.IsNullOrEmpty(result))
                            {
                                str += result;
                            }
                            choices.message = str;
                            choices.thinkMessage = thinkingAll;
                            choices.onlyMessage = result;
                            choices.finishReason = finishReason;
                            completionsDto.choices = choices;
                            return completionsDto;
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"异步调用上下文对话失败: {ex.Message}，，发生参数={body}， 堆栈={ex.StackTrace}", "对话(Chat)-文本 API失败");
                        LogServerUtils.ErrorLog(
                                $"调用chatCompletions方法失败",
                                $"调用对话方法报错",
                                $"Message = {ex.Message}, StackTrace = {ex.StackTrace}"
                            );
                        completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                        completionsDto.status = 1;
                        return completionsDto;
                    }
                }
            }
            catch (Exception ex)
            {
                LogServerUtils.ErrorLog("方法chatCompletions报错", "调用chatCompletions方法报预期之外的错误", $"message = {ex.Message}");
                completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                completionsDto.status = 1;
            }
            return completionsDto;
        }

        public static CompletionsDto chatCompletionsNotAsync(AiTempTokenDto config, AskRequestDto dto, string content)
        {
            CompletionsDto completionsDto = new CompletionsDto();
            try
            {
                var msgList = new List<object>();
                msgList.Add(new { role = "user", content = $"{content}" });
                if (!string.IsNullOrEmpty(dto.identity)) msgList.Add(new { role = "system", content = dto.identity });

                var requestMsg = new HttpRequestMessage();
                requestMsg.Method = HttpMethod.Post;
                requestMsg.Headers.Add("Authorization", $"Bearer {config.token}");
                requestMsg.Headers.Add("Accept", $"text/event-stream");
                requestMsg.Headers.Add("Connection", "keep-alive");
                requestMsg.RequestUri = new Uri(aiCompletionsUrl);
                var obj = new
                {
                    model = config.modelId,
                    messages = msgList,
                    thinking = new 
                    {
                        type = dto?.thinkingType?? "enabled"
                    },
                    stream = true,
                    max_tokens = config.lastHistoryTokens,
                    stream_options = new
                    {
                        include_usage = true
                    }
                };
                string body = JsonConvert.SerializeObject(obj);
                FileUtils.log($"model={config.modelId}, max_tokens ={config.lastHistoryTokens}", "/api/v3/chat/completions接口参数", true);
                requestMsg.Content = new StringContent(body, Encoding.UTF8, "application/json");
                using (HttpClient client = new HttpClient())
                {
                    // 设置超时时间
                    client.Timeout = TimeSpan.FromSeconds(60 * 5);
                    client.DefaultRequestHeaders.Add("Authorization", $"Bearer {config.token}");

                    try
                    {
                        Choices choices = new Choices();
                        HttpContent httpContent = new StringContent(body, Encoding.UTF8, "application/json");
                        HttpResponseMessage httpRequestMessage = client.SendAsync(requestMsg).Result;

                        // 从响应头中获取 Request ID
                        if (httpRequestMessage.Headers.TryGetValues("X-Request-ID", out var requestIds))
                        {
                            FileUtils.log(requestIds.FirstOrDefault(), "当前Request ID");
                            choices.requestId = requestIds.FirstOrDefault();
                        }

                        // 确保响应状态码是200 OK（或其他表示成功的状态码）
                        if (!httpRequestMessage.IsSuccessStatusCode)
                        {
                            var errorStr = httpRequestMessage.Content.ReadAsStreamAsync().Result;
                            FileUtils.LogError($"调用对话(Chat)-文本失败，状态码:{httpRequestMessage.StatusCode}, 信息：{errorStr}", "调用对话(Chat)-文本失败");
                            LogServerUtils.ErrorLog(
                                $"调用对话(Chat)-文本API失败",
                                $"调用对话(Chat)-文本接口报错，状态码:{httpRequestMessage.StatusCode}",
                                $"requestId={requestIds},信息：{errorStr}"
                            );
                            completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                            completionsDto.status = 1;
                            return completionsDto;
                        }



                        completionsDto.totalTokenNum = 0;
                        completionsDto.id = "";
                        string finishReason = "";
                        using (var stream = httpRequestMessage.Content.ReadAsStreamAsync().Result)
                        using (var reader = new StreamReader(stream))
                        {
                            string result = "";
                            string thinkingAll = "";
                            string line;
                            while ((line = reader.ReadLineAsync().Result) != null)
                            {
                                if (!string.IsNullOrEmpty(line))
                                {
                                    if (line.StartsWith("data: "))
                                    {
                                        //File.AppendAllLines("C:\\Users\\JY\\Desktop\\工作\\testTemp.txt", new string[] { line });
                                        string tempStr = line.Replace("data: ", "");
                                        if (string.IsNullOrEmpty(tempStr)) continue;
                                        if ("[DONE]".Equals(tempStr))
                                        {
                                            continue;
                                        }
                                        string message = "";
                                        string thinking = "";
                                        string dataStr = "";
                                        string type = "";
                                        var settings = new JsonSerializerSettings
                                        {
                                            ContractResolver = new CamelCasePropertyNamesContractResolver() // 自动转小驼峰 
                                        };
                                        DouBaoChatStreamDto res = JsonConvert.DeserializeObject<DouBaoChatStreamDto>(tempStr, settings);
                                        completionsDto.id = res.id;

                                        if (res?.choices != null && res.choices?.Count > 0)
                                        {
                                            if (res?.choices != null && res?.choices?.Count > 0)
                                            {
                                                dynamic temp = JsonConvert.DeserializeObject<dynamic>(JsonConvert.SerializeObject(res.choices[0].delta));
                                                if (temp.content != null && !string.IsNullOrEmpty(temp.content.ToString()))
                                                {
                                                    message = temp?.content ?? "";
                                                    dataStr = message;
                                                    type = "text";
                                                }
                                                else if (temp.reasoning_content != null && !string.IsNullOrEmpty(temp.reasoning_content.ToString()))
                                                {
                                                    thinking = temp?.reasoning_content ?? "";
                                                    dataStr = thinking;
                                                    type = "thinking";
                                                }
                                            }
                                            result += message;
                                            thinkingAll += thinking;
                                            if (res?.choices?[0]?.finishReason != null)
                                            {
                                                finishReason = res?.choices?[0]?.finishReason;
                                                FileUtils.log(tempStr, "api/v3/chat/completions接口最后返回的数据", true);
                                            }
                                        }
                                        if (res != null && res.usage != null && res.usage.totalTokens != null && res.usage.totalTokens >= 0)
                                        {
                                            completionsDto.totalTokenNum = res.usage.totalTokens;
                                            completionsDto.usage = res.usage;
                                        }
                                    }
                                }
                            }
                            String str = "";
                            if (!string.IsNullOrEmpty(thinkingAll))
                            {
                                str += $"<div class='deepThinking'>{thinkingAll}</div>\n\n";
                            }
                            if (!string.IsNullOrEmpty(result))
                            {
                                str += result;
                            }
                            choices.message = str;
                            choices.thinkMessage = thinkingAll;
                            choices.onlyMessage = result;
                            choices.finishReason = finishReason;
                            completionsDto.choices = choices;
                            return completionsDto;
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"异步调用上下文对话失败: {ex.Message}，，发生参数={body}， 堆栈={ex.StackTrace}", "对话(Chat)-文本 API失败");
                        LogServerUtils.ErrorLog(
                                $"调用chatCompletions方法失败",
                                $"调用对话方法报错",
                                $"Message = {ex.Message}, StackTrace = {ex.StackTrace}"
                            );
                        completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                        completionsDto.status = 1;
                        return completionsDto;
                    }
                }
            }
            catch (Exception ex)
            {
                LogServerUtils.ErrorLog("方法chatCompletions报错", "调用chatCompletions方法报预期之外的错误", $"message = {ex.Message}");
                completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                completionsDto.status = 1;
            }
            return completionsDto;
        }

        /// <summary>
        /// 调用上下文缓存对话接口-流式接口
        /// </summary>
        /// <param name="config"></param>
        /// <param name="dto"></param>
        /// <param name="contextId"></param>
        /// <param name="content"></param>
        /// <param name="request"></param>
        /// <param name="response"></param>
        /// <returns></returns>
        public static async Task<CompletionsDto> contextChatCompletions(AiTempTokenDto config, AskRequestDto dto, string contextId, string content, HttpListenerRequest request, HttpListenerResponse response, Action action)
        {
            CompletionsDto completionsDto = new CompletionsDto();
            try
            {
                var msgList = new List<object>();
                msgList.Add(new { role = "user", content = $"{content}" });
                if (!string.IsNullOrEmpty(dto.identity)) msgList.Add(new { role = "system", content = dto.identity });

                var requestMsg = new HttpRequestMessage();
                requestMsg.Method = HttpMethod.Post;
                requestMsg.Headers.Add("Authorization", $"Bearer {config.token}");
                requestMsg.Headers.Add("Accept", $"text/event-stream");
                requestMsg.Headers.Add("Connection", "keep-alive");
                requestMsg.RequestUri = new Uri(aiContextCompletionsUrl);
                var obj = new
                {
                    model = config.modelId,
                    context_id = contextId,
                    messages = msgList,
                    stream = true,
                    max_tokens = config.lastHistoryTokens,
                    frequency_penalty = 0.01,
                    stream_options = new
                    {
                        include_usage = true
                    }
                };
                string body = JsonConvert.SerializeObject(obj);
                FileUtils.log($"model={config.modelId}, max_tokens ={config.lastHistoryTokens}", "api/v3/context/chat/completions接口参数", true);
                requestMsg.Content = new StringContent(body, Encoding.UTF8, "application/json");
                using (HttpClient client = new HttpClient())
                {
                    // 设置超时时间
                    client.Timeout = TimeSpan.FromSeconds(60 * 5);
                    client.DefaultRequestHeaders.Add("Authorization", $"Bearer {config.token}");

                    try
                    {
                        Choices choices = new Choices();
                        HttpContent httpContent = new StringContent(body, Encoding.UTF8, "application/json");
                        HttpResponseMessage httpRequestMessage = await client.SendAsync(requestMsg, HttpCompletionOption.ResponseHeadersRead);
                        // 从响应头中获取 Request ID
                        if (httpRequestMessage.Headers.TryGetValues("X-Request-ID", out var requestIds))
                        {
                            FileUtils.log(requestIds.FirstOrDefault(), "当前Request ID");
                            choices.requestId = requestIds.FirstOrDefault();
                        }
                        // 确保响应状态码是200 OK（或其他表示成功的状态码）
                        if (!httpRequestMessage.IsSuccessStatusCode)
                        {
                            FileUtils.LogError($"调用上下文对话失败，状态码:{httpRequestMessage.StatusCode}, 信息：{await httpRequestMessage.Content.ReadAsStringAsync()}", "调用上下文对话失败");
                            LogServerUtils.ErrorLog("豆包上下文对话接口报错", $"调用上下文对话失败，状态码:{httpRequestMessage.StatusCode}", $"requestId={requestIds}, 信息：{await httpRequestMessage.Content.ReadAsStringAsync()}");
                            completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                            completionsDto.status = 1;
                            return completionsDto;
                        }
                        // 调用成功后的回调
                        action?.Invoke();

                        completionsDto.totalTokenNum = 0;
                        completionsDto.id = "";
                        string finishReason = "";
                        using (var stream = await httpRequestMessage.Content.ReadAsStreamAsync())
                        using (var reader = new StreamReader(stream))
                        {
                            string result = "";
                            string thinkingAll = "";
                            string line;
                            while ((line = await reader.ReadLineAsync()) != null)
                            {
                                if (!string.IsNullOrEmpty(line))
                                {
                                    if (line.StartsWith("data: "))
                                    {
                                        //File.AppendAllLines("C:\\Users\\JY\\Desktop\\工作\\testTemp.txt", new string[] { line });
                                        string tempStr = line.Replace("data: ", "");
                                        if (string.IsNullOrEmpty(tempStr)) continue;
                                        if ("[DONE]".Equals(tempStr))
                                        {
                                            continue;
                                        }
                                        string message = "";
                                        string thinking = "";
                                        string dataStr = "";
                                        string type = "";
                                        var settings = new JsonSerializerSettings
                                        {
                                            ContractResolver = new CamelCasePropertyNamesContractResolver() // 自动转小驼峰 
                                        };
                                        DouBaoChatStreamDto res = JsonConvert.DeserializeObject<DouBaoChatStreamDto>(tempStr, settings);
                                        completionsDto.id = res.id;
                                        if (res?.choices != null && res.choices?.Count > 0)
                                        {
                                            if (res?.choices != null && res?.choices?.Count > 0)
                                            {
                                                dynamic temp = JsonConvert.DeserializeObject<dynamic>(JsonConvert.SerializeObject(res.choices[0].delta));
                                                if (temp.content != null && !string.IsNullOrEmpty(temp.content.ToString()))
                                                {
                                                    message = temp?.content ?? "";
                                                    dataStr = message;
                                                    type = "text";
                                                }
                                                else if (temp.reasoningContent != null && !string.IsNullOrEmpty(temp.reasoningContent.ToString()))
                                                {
                                                    thinking = temp?.reasoningContent ?? "";
                                                    dataStr = thinking;
                                                    type = "thinking";
                                                }
                                            }
                                            if (!string.IsNullOrEmpty(dataStr) && !string.IsNullOrEmpty(type))
                                            {
                                                await AiUtils.sendData(response, "message", JsonConvert.SerializeObject(new { content = dataStr, type }));
                                            }
                                            result += message;
                                            thinkingAll += thinking;

                                            if (res?.choices?[0]?.finishReason != null)
                                            {
                                                finishReason = res?.choices?[0]?.finishReason;
                                                FileUtils.log(tempStr, "api/v3/context/chat/completions接口最好返回的数据", true);
                                            }
                                        }
                                        if (res != null && res.usage != null && res.usage.totalTokens != null && res.usage.totalTokens >= 0)
                                        {
                                            completionsDto.totalTokenNum = res.usage.totalTokens;
                                            completionsDto.usage = res.usage;
                                        }
                                    }
                                }
                            }
                            String str = "";
                            if (!string.IsNullOrEmpty(thinkingAll))
                            {
                                str += $"<div class='deepThinking'>{thinkingAll}</div>\n\n";
                            }
                            if (!string.IsNullOrEmpty(result))
                            {
                                str += result;
                            }
                            choices.message = str;
                            choices.finishReason = finishReason;
                            completionsDto.choices = choices;
                            return completionsDto;
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"异步调用上下文对话失败: {ex.Message}，发生参数={body}， 堆栈={ex.StackTrace}", "调用上下文缓存对话接口-流式接口失败");
                        completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                        completionsDto.status = 1;
                        return completionsDto;
                    }
                }
            }
            catch (Exception ex)
            {
                LogServerUtils.ErrorLog("方法contextChatCompletions报错", "调用contextChatCompletions方法报预期之外的错误", $"message = {ex.Message}");
                completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                completionsDto.status = 1;
            }
            return completionsDto;
        }

        
        /// <summary>
        /// 保存会话文件
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="list"></param>
        public static void svaeConversationData(int type, string sourceId, int sourceType, string contextId, List<ConversationDto> list)
        {
            try
            {
                if (list == null || list.Count == 0) return;
                // 获取地址
                string path = GetContextFilePath(type, sourceId, sourceType, contextId);

                // 判断是否有文件夹
                // 提取文件所在的目录路径
                string directoryPath = Path.GetDirectoryName(path);
                FileUtils.createDirectory(directoryPath);

                List<string> listStr = list.Select(v => $"{v.code}>>>{JsonConvert.SerializeObject(v)}").ToList();

                File.AppendAllLines(path, listStr);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"错误：{ex.Message}，堆栈：{ex.StackTrace}", "保存会话文件报错");
            }
        }

        /// <summary>
        /// 添加历史段落文件
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="code"></param>
        /// <param name="content"></param>
        /// <exception cref="CustomException"></exception>
        public static void addHistoryFile(int type, string sourceId, int sourceType, string code, string content)
        {
            try
            {
                // 获取地址
                string path = GetHistoryFilePath(type, sourceId, sourceType, code);

                // 判断是否有文件夹
                // 提取文件所在的目录路径
                string directoryPath = Path.GetDirectoryName(path);
                FileUtils.createDirectory(directoryPath);

                File.WriteAllText(path, content);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"错误：{ex.Message}，堆栈：{ex.StackTrace}", "添加历史段落文件报错");
            }
        }

        /// <summary>
        /// 删除历史段落文件
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="code"></param>
        public static void deleteHistoryFile(int type, string sourceId, int sourceType, string code)
        {
            try
            {
                // 获取地址
                string path = GetHistoryFilePath(type, sourceId, sourceType, code);

                if (File.Exists(path))
                {
                    File.Delete(path);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"错误：{ex.Message}，堆栈：{ex.StackTrace}", "删除历史段落文件报错");
            }
        }

        /// <summary>
        /// 获取所有的历史段落文件
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <returns></returns>
        public static List<string> listHistoryFile(int type, string sourceId, int sourceType)
        {
            List<string> result = new List<string>();
            try
            {
                // 获取地址
                string path = GetHistoryDirectoryPath(type, sourceId, sourceType);

                if (!Directory.Exists(path)) return result;

                // 获取指定目录及其子目录下的所有 .txt 文件
                string[] txtFiles = Directory.GetFiles(path, "*.txt", SearchOption.AllDirectories);

                // 遍历每个 .txt 文件
                foreach (string file in txtFiles)
                {
                    // 读取文件的第一行内容
                    string firstLine = File.ReadLines(file).FirstOrDefault();
                    if (!string.IsNullOrEmpty(firstLine)) result.Add(firstLine);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"错误：{ex.Message}，堆栈：{ex.StackTrace}", "获取所有的历史段落文件报错");
            }
            return result;
        }

        /// <summary>
        /// 保存ai历史数据的结构数据
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="listStr"></param>
        public static void saveStructure(int type, string sourceId, int sourceType, List<string> listStr)
        {
            try
            {
                // 获取地址
                string path = GetStructureFilePath(type, sourceId, sourceType);

                // 判断是否有文件夹
                // 提取文件所在的目录路径
                string directoryPath = Path.GetDirectoryName(path);
                FileUtils.createDirectory(directoryPath);

                File.AppendAllLines(path, listStr);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"错误：{ex.Message}，堆栈：{ex.StackTrace}", "保存会话文件报错");
            }
        }

        /// <summary>
        /// 倒序分页读取文件内容行
        /// </summary>
        /// <param name="filePath"></param>
        /// <param name="pageSize"></param>
        /// <param name="pageIndex"></param>
        /// <returns></returns>
        public static PagedResult ReadFileReversePaged(string filePath, int pageIndex, int pageSize)
        {
            PagedResult result = new PagedResult();
            if (!File.Exists(filePath))
            {
                return result;
            }

            int start = (pageIndex - 1) * pageSize;
            int end = start + pageSize - 1;


            using (FileStream fs = new FileStream(filePath, FileMode.Open, FileAccess.Read))
            {
                long position = fs.Length;
                byte[] buffer = new byte[1];
                StringBuilder lineBuilder = new StringBuilder();

                int current = 0;
                // 从文件末尾逐字节读取
                while (position >= 0)
                {
                    fs.Seek(position, SeekOrigin.Begin);
                    fs.Read(buffer, 0, 1);

                    char currentChar = (char)buffer[0];
                    if (!(currentChar == '\n' || currentChar == '\r' || currentChar.Equals("\0")))
                    {
                        lineBuilder.Insert(0, currentChar);
                    }
                    if (currentChar == '\n' || currentChar == '\r' || position == 0)
                    {
                        string line = lineBuilder.ToString().Trim();
                        // 找到有效的一行
                        if (lineBuilder.Length > 0 && !string.IsNullOrEmpty(line) && !line.Equals("\0"))
                        {
                            if (current >= start && current <= end)
                            {
                                result.Lines.Add(line);
                            }
                            if(current <= end)
                            {
                                result.HasPreviousPage = false;
                            }
                            else if(current == end + 1)
                            {
                                result.HasPreviousPage = true;
                                break;
                            }
                            lineBuilder.Clear();
                            current++;
                        }
                    }
                    position--;
                }

                // 处理文件的第一行
                //if (lineBuilder.Length > 0)
                //{
                //    string line = lineBuilder.ToString().Trim();
                //    if (!string.IsNullOrEmpty(line))
                //    {
                //        string[] temp = line.Split(new string[] { ">>>" }, StringSplitOptions.RemoveEmptyEntries);
                //        if (temp.Length > 1 && int.TryParse(temp[temp.Length - 1], out int result))
                //        {
                //            return line; // 返回符合条件的最后非空行
                //        }
                //    }
                //}
                result.Lines.Reverse();
                return result;
            }

        }

        /// <summary>
        /// 根据code获取ai结构数据
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="map"></param>
        /// <returns></returns>
        public static Dictionary<string, ConversationDto> GetStructureDataByCode(int type, string sourceId, int sourceType, Dictionary<string, List<string>> map)
        {
            Dictionary<string, ConversationDto> result = new Dictionary<string, ConversationDto>();

            for (int i = 0; i < map.Count; i++)
            {
                string key = map.Keys.ElementAt(i);
                List<string> values = map[key];
                if(values.Count > 1)
                {
                    try
                    {
                        string contextId = key;
                        string path = GetContextFilePath(type, sourceId, sourceType, contextId);
                        // 使用 StreamReader 逐行读取文件
                        using (StreamReader reader = new StreamReader(path))
                        {
                            string line;
                            while ((line = reader.ReadLine()) != null)
                            {
                                string[] datas = line.Split(new string[] { ">>>" }, StringSplitOptions.None);
                                string tempCode = datas[0];
                                for (int j = 0; j < values.Count; j++)
                                {
                                    string code = values[j];
                                    if (tempCode.Equals(values[j]))
                                    {
                                        result.Add(GetStruacturePrefix(contextId, code), JsonConvert.DeserializeObject<ConversationDto>(datas[1]));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"错误消息：{ex.Message},堆栈：{ex.StackTrace}", "根据code获取ai结构数据报错");
                    }
                }
            }
            return result;
        }

        /// <summary>
        /// 根据code获取历史段落字符串
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="code"></param>
        /// <returns></returns>
        public static string GetHistoryContentByCode(int type, string sourceId, int sourceType, string code)
        {
            try
            {
                return api.AiRelatedApi.getHistoryContentByCode(type, sourceId, sourceType, code);
            }
            catch(Exception ex)
            {
                FileUtils.LogError($"错误消息：{ex.Message},堆栈：{ex.StackTrace}", "根据code获取历史段落字符串报错");

            }
            return null;
        }

        /// <summary>
        /// 给定一个时间字符串 (HH:mm:ss)，加上指定秒数后返回格式化的时间字符串。
        /// </summary>
        /// <param name="timeString">初始时间字符串，格式为 "HH:mm:ss"</param>
        /// <param name="secondsToAdd">要加上的秒数</param>
        /// <returns>格式化后的时间字符串 (HH:mm:ss)</returns>
        public static string AddSecondsAndFormat(string timeString, int secondsToAdd)
        {
            // 检查输入时间字符串是否为空或格式不正确
            if (string.IsNullOrEmpty(timeString) || !DateTime.TryParseExact(timeString, "HH:mm:ss", null, System.Globalization.DateTimeStyles.None, out DateTime baseTime))
            {
                throw new ArgumentException("时间字符串格式无效，请使用 HH:mm:ss 格式。");
            }

            // 计算新的时间 
            DateTime newTime = baseTime.AddSeconds(secondsToAdd);

            // 返回格式化后的时间字符串
            return newTime.ToString("HH:mm:ss");
        }

        /// <summary>
        /// 修改ai会话数据文件
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="code"></param>
        /// <param name="giveStatuc"></param>
        /// <exception cref="CustomException"></exception>
        /// <exception cref="NotImplementedException"></exception>
        public static void UpdateContextGiveStatuc(int type, string sourceId, int sourceType, string code, int giveStatuc)
        {
            if (!code.StartsWith("ai_"))
            {
                throw new CustomException("有完整结果后再点击", 7005);
            }
            string[] split = code.Split(new string[] { "_" }, StringSplitOptions.None);
            string contextId = split[1];
            string tempCode = split[2];
            string inputFilePath = GetContextFilePath(type, sourceId, sourceType, split[1]);
            string outputFilePath = GetContextFilePath(type, sourceId, sourceType, Guid.NewGuid().ToString("N"));
            try
            {
                using (StreamReader reader = new StreamReader(inputFilePath))
                using (StreamWriter writer = new StreamWriter(outputFilePath))
                {
                    string line;
                    int lineNumber = 0;

                    while ((line = reader.ReadLine()) != null)
                    {
                        lineNumber++;
                        if (!string.IsNullOrEmpty(line))
                        {
                            string[] tempSplit = line.Split(new string[] { ">>>" }, StringSplitOptions.None);
                            // 根据条件修改行数据
                            if (tempSplit.Length > 0 && tempSplit[0].Equals(tempCode))
                            {
                                ConversationDto dto = JsonConvert.DeserializeObject<ConversationDto>(tempSplit[1]);
                                dto.giveStatuc = giveStatuc;
                                line = $"{tempCode}>>>{JsonConvert.SerializeObject(dto)}";
                            }
                        }
                        // 写入修改后的行
                        writer.WriteLine(line);
                    }
                }

                // 如果需要覆盖原文件，可以将输出文件复制回原文件
                File.Delete(inputFilePath); // 删除原文件
                File.Move(outputFilePath, inputFilePath); // 将新文件重命名为原文件名
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"修改ai会话数据文件: {ex.Message}，堆栈：{ex.StackTrace}");
            }
        }

        /// <summary>
        /// 发送sse的数据到前端
        /// </summary>
        /// <param name="response"></param>
        /// <param name="eventStr"></param>
        /// <param name="data"></param>
        /// <returns></returns>
        public static async Task sendData(HttpListenerResponse response, string eventStr, string data)
        {
            try
            {
                if(response == null)
                {
                    return;
                }
                string data1 = $"event: {eventStr}\n" +
                                  $"data: {data}\n\n";
                byte[] buffer1 = Encoding.UTF8.GetBytes(data1);
                await response.OutputStream.WriteAsync(buffer1, 0, buffer1.Length);
                await response.OutputStream.FlushAsync();
            }
            catch (IOException ex)
            {
                FileUtils.LogError(ex.Message, "连接已断开，写入数据时发生异常");
            }
            catch (ObjectDisposedException ex)
            {
                FileUtils.LogError(ex.Message, "连接已断开或流已关闭，发生异常");
            }
            catch(Exception ex)
            {
                FileUtils.LogError(ex.Message, "发生sse数据到前端失败");
            }
        }

    }
}
