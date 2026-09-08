using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Ai.impl
{
    /// <summary>
    /// 豆包（火山引擎）AI问答服务实现
    /// </summary>
    public class DoubaoChatService : IAiChatService
    {
        private const string aiCompletionsUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
        private const string responsesUrl = "https://ark.cn-beijing.volces.com/api/v3/responses";

        /// <summary>
        /// 流式问答 — 通过 SSE 逐 chunk 推送到前端，适用于实时展示的场景
        /// </summary>
        public async Task<CompletionsDto> ChatCompletionStream(
            AiTempTokenDto config, AskRequestDto dto,
            string content, HttpListenerRequest request, HttpListenerResponse response,
            CancellationToken cancellationToken = default)
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
                FileUtils.log($"【Chat Completions 流式】请求参数: model={config.modelId}, max_tokens={config.lastHistoryTokens}, thinkingType={dto?.thinkingType}", "Chat Completions流式请求");
                FileUtils.log($"【Chat Completions 流式】提示词内容: {body}", "Chat Completions流式-提示词", true);
                requestMsg.Content = new StringContent(body, Encoding.UTF8, "application/json");
                using (HttpClient client = new HttpClient())
                {
                    client.Timeout = TimeSpan.FromSeconds(60 * 5);
                    client.DefaultRequestHeaders.Add("Authorization", $"Bearer {config.token}");

                    try
                    {
                        Choices choices = new Choices();
                        HttpContent httpContent = new StringContent(body, Encoding.UTF8, "application/json");
                        HttpResponseMessage httpRequestMessage = await client.SendAsync(requestMsg, HttpCompletionOption.ResponseHeadersRead, cancellationToken);

                        if (httpRequestMessage.Headers.TryGetValues("X-Request-ID", out var requestIds))
                        {
                            FileUtils.log(requestIds.FirstOrDefault(), "当前Request ID");
                            choices.requestId = requestIds.FirstOrDefault();
                        }

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
                        using (var ctr = cancellationToken.Register(() => client.CancelPendingRequests()))
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
                                            ContractResolver = new CamelCasePropertyNamesContractResolver()
                                        };
                                        DouBaoChatStreamDto res = JsonConvert.DeserializeObject<DouBaoChatStreamDto>(tempStr, settings);
                                        completionsDto.id = res.id;
                                        completionsDto.responseId = res.id;

                                        if (res?.choices != null && res.choices?.Count > 0)
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

        /// <summary>
        /// 带上下文的流式问答 — 调用 Volcengine Responses API
        /// </summary>
        public async Task<CompletionsDto> ContextChatCompletionStream(
            AiTempTokenDto config, AskRequestDto dto,
            string content, string contextId,
            HttpListenerRequest request, HttpListenerResponse response,
            CancellationToken cancellationToken = default)
        {
            CompletionsDto completionsDto = new CompletionsDto();
            try
            {
                bool isFirstTurn = string.IsNullOrEmpty(contextId) || !contextId.StartsWith("resp_");
                FileUtils.log($"【Responses API】开始请求: isFirstTurn={isFirstTurn}, contextId={contextId ?? "null"}, model={config.modelId}, thinkingType={dto?.thinkingType}", "Responses API请求开始");
                FileUtils.log($"【Responses API】用户输入(input): {content ?? ""}", "Responses API-input内容", true);
                if (isFirstTurn && dto.otherObj?.ContainsKey("systemPrompt") == true)
                {
                    string sp = dto.otherObj["systemPrompt"]?.ToString();
                    FileUtils.log($"【Responses API】系统提示词(instructions,长度={sp?.Length ?? 0}): {sp}", "Responses API-instructions内容", true);
                }

                var requestMsg = new HttpRequestMessage();
                requestMsg.Method = HttpMethod.Post;
                requestMsg.Headers.Add("Authorization", $"Bearer {config.token}");
                requestMsg.Headers.Add("Accept", "text/event-stream");
                requestMsg.Headers.Add("Connection", "keep-alive");
                requestMsg.RequestUri = new Uri(responsesUrl);
                var obj = new Dictionary<string, object>
                {
                    { "model", config.modelId },
                    { "stream", true },
                    { "max_output_tokens", config.lastHistoryTokens },
                    { "thinking", new Dictionary<string, string> { { "type", dto?.thinkingType ?? "enabled" } } },
                    // { "input", content ?? "" },
                    { "caching", new Dictionary<string, string> { { "type", dto?.thinkingType ?? "enabled" } }}
                };

                if (isFirstTurn)
                {
                    // 首次：instructions 用服务端返回的 systemPrompt（与服务器 allContent 一致），store=true 缓存对话
                    string systemPrompt = dto.otherObj?.ContainsKey("systemPrompt") == true
                        ? dto.otherObj["systemPrompt"]?.ToString() : null;
                    // if (!string.IsNullOrEmpty(systemPrompt))
                    // {
                    //     obj["instructions"] = systemPrompt;
                    // }

                    obj["input"] = new List<Dictionary<string, string>>
                    {
                        new Dictionary<string, string> { { "role", "system" }, { "content", systemPrompt } },
                        new Dictionary<string, string> { { "role", "user" }, { "content", content ?? "" } }
                    };
                    obj["store"] = true;
                    obj["expire_at"] = DateTimeOffset.UtcNow.ToUnixTimeSeconds() + config.contextSaveTime;
                }
                else
                {
                    // 后续轮次：仅传 previous_response_id，不传 instructions
                    obj["previous_response_id"] = contextId;
                    obj["input"] = content ?? "";
                }

                string body = JsonConvert.SerializeObject(obj);
                string logBody = body;
                if (logBody.Length > 500) logBody = logBody.Substring(0, 500) + "...";
                FileUtils.log($"【Responses API】请求体: {logBody}", "Responses API请求体");
                FileUtils.log($"【Responses API】关键参数: model={config.modelId}, max_output_tokens={config.lastHistoryTokens}, isFirstTurn={isFirstTurn}, hasInstructions={obj.ContainsKey("instructions")}, hasPreviousResponseId={obj.ContainsKey("previous_response_id")}", "Responses API接口参数");
                requestMsg.Content = new StringContent(body, Encoding.UTF8, "application/json");
                using (HttpClient client = new HttpClient())
                {
                    client.Timeout = TimeSpan.FromSeconds(60 * 5);
                    client.DefaultRequestHeaders.Add("Authorization", $"Bearer {config.token}");

                    try
                    {
                        Choices choices = new Choices();
                        HttpResponseMessage httpRequestMessage = await client.SendAsync(requestMsg, HttpCompletionOption.ResponseHeadersRead, cancellationToken);

                        if (httpRequestMessage.Headers.TryGetValues("X-Request-ID", out var requestIds))
                        {
                            FileUtils.log($"【Responses API】Request ID: {requestIds.FirstOrDefault()}", "Responses API Request ID");
                            choices.requestId = requestIds.FirstOrDefault();
                        }

                        FileUtils.log($"【Responses API】HTTP状态码: {httpRequestMessage.StatusCode}", "Responses API HTTP状态");

                        if (!httpRequestMessage.IsSuccessStatusCode)
                        {
                            string errorBody = await httpRequestMessage.Content.ReadAsStringAsync();
                            FileUtils.log($"【Responses API】错误响应体: {errorBody}", "Responses API错误详情");
                            FileUtils.LogError($"调用Responses API失败，状态码:{httpRequestMessage.StatusCode}, 信息：{errorBody}", "调用Responses API失败");
                            LogServerUtils.ErrorLog(
                                $"调用Responses API失败",
                                $"调用Responses API接口报错，状态码:{httpRequestMessage.StatusCode}",
                                $"信息：{await httpRequestMessage.Content.ReadAsStringAsync()}"
                            );
                            completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                            completionsDto.status = 1;
                            return completionsDto;
                        }

                        completionsDto.totalTokenNum = 0;
                        completionsDto.id = "";
                        int chunkCount = 0;
                        int thinkingCount = 0;
                        int textCount = 0;
                        using (var ctr = cancellationToken.Register(() => client.CancelPendingRequests()))
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
                                        string tempStr = line.Replace("data: ", "");
                                        if (string.IsNullOrEmpty(tempStr)) continue;
                                        if ("[DONE]".Equals(tempStr)) continue;

                                        DouBaoChatStreamDto res = JsonConvert.DeserializeObject<DouBaoChatStreamDto>(tempStr);

                                        bool isThinking = "response.reasoning_summary_text.delta".Equals(res.type);
                                        chunkCount++;

                                        if (!string.IsNullOrEmpty(res.delta))
                                        {
                                            if (isThinking)
                                            {
                                                thinkingCount++;
                                                thinkingAll += res.delta;
                                                await AiUtils.sendData(response, "message",
                                                    JsonConvert.SerializeObject(new { content = res.delta, type = "thinking" }));
                                            }
                                            else
                                            {
                                                textCount++;
                                                result += res.delta;
                                                await AiUtils.sendData(response, "message",
                                                    JsonConvert.SerializeObject(new { content = res.delta, type = "text" }));
                                            }
                                        }

                                        if (res.response != null)
                                        {
                                            completionsDto.responseId = res.response.id;
                                            completionsDto.id = res.response.id;
                                            FileUtils.log($"【Responses API】流结束: responseId={res.response.id}, totalTokens={res.response.usage?.totalTokens}, inputTokens={res.response.usage?.inputTokens}, outputTokens={res.response.usage?.outputTokens}, cachedTokens={res.response.usage?.inputTokensDetails?.cachedTokens}, reasoningTokens={res.response.usage?.outputTokensDetails?.reasoningTokens}", "Responses API流结束");
                                            if (res.response.usage != null)
                                            {
                                                completionsDto.totalTokenNum = res.response.usage.totalTokens;
                                                completionsDto.usage = new Usage
                                                {
                                                    promptTokens = res.response.usage.inputTokens ?? 0,
                                                    completionTokens = res.response.usage.outputTokens ?? 0,
                                                    totalTokens = res.response.usage.totalTokens ?? 0,
                                                    promptTokensDetails = new PromptTokensDetails
                                                    {
                                                        cached_tokens = res.response.usage.inputTokensDetails?.cachedTokens ?? 0
                                                    },
                                                    completionTokensDetails = new CompletionTokensDetails
                                                    {
                                                        reasoningTokens = res.response.usage.outputTokensDetails?.reasoningTokens ?? 0
                                                    }
                                                };
                                            }
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
                            completionsDto.choices = choices;
                            FileUtils.log($"【Responses API】流式完成: 总chunk数={chunkCount}, 思考chunk={thinkingCount}(长度={thinkingAll.Length}), 文本chunk={textCount}(长度={result.Length}), responseId={completionsDto.responseId}, totalTokens={completionsDto.totalTokenNum}", "Responses API完成汇总");
                            return completionsDto;
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.log($"【Responses API】异常: model={config.modelId}, contextId={contextId}, isFirstTurn={isFirstTurn}", "Responses API异常上下文");
                        FileUtils.LogError($"异步调用Responses API失败: {ex.Message}，发生参数={body}， 堆栈={ex.StackTrace}", "Responses API失败");
                        LogServerUtils.ErrorLog(
                                $"调用Responses API方法失败",
                                $"调用Responses API方法报错",
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
                LogServerUtils.ErrorLog("方法ContextChatCompletion报错", "调用Responses API方法报预期之外的错误", $"message = {ex.Message}");
                completionsDto.choices = new Choices() { message = AbstractAsk.errorReturnData };
                completionsDto.status = 1;
            }
            return completionsDto;
        }

        /// <summary>
        /// 非流式问答 — 内部调用（如自动纠正、批量处理），不推送浏览器
        /// </summary>
        public CompletionsDto ChatCompletion(
            AiTempTokenDto config, AskRequestDto dto, string content)
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
                    stream_options = new
                    {
                        include_usage = true
                    }
                };
                string body = JsonConvert.SerializeObject(obj);
                FileUtils.log($"【Chat Completions 非流式】请求参数: model={config.modelId}, max_tokens={config.lastHistoryTokens}", "Chat Completions非流式请求");
                FileUtils.log($"【Chat Completions 非流式】提示词内容: {body}", "Chat Completions非流式-提示词", true);
                requestMsg.Content = new StringContent(body, Encoding.UTF8, "application/json");
                using (HttpClient client = new HttpClient())
                {
                    client.Timeout = TimeSpan.FromSeconds(60 * 5);
                    client.DefaultRequestHeaders.Add("Authorization", $"Bearer {config.token}");

                    try
                    {
                        Choices choices = new Choices();
                        HttpContent httpContent = new StringContent(body, Encoding.UTF8, "application/json");
                        HttpResponseMessage httpRequestMessage = client.SendAsync(requestMsg).Result;

                        if (httpRequestMessage.Headers.TryGetValues("X-Request-ID", out var requestIds))
                        {
                            FileUtils.log(requestIds.FirstOrDefault(), "当前Request ID");
                            choices.requestId = requestIds.FirstOrDefault();
                        }

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
                                            ContractResolver = new CamelCasePropertyNamesContractResolver()
                                        };
                                        DouBaoChatStreamDto res = JsonConvert.DeserializeObject<DouBaoChatStreamDto>(tempStr, settings);
                                        completionsDto.id = res.id;

                                        if (res?.choices != null && res.choices?.Count > 0)
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
    }
}
