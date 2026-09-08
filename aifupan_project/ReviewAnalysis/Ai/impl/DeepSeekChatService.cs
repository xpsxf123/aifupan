using System;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Ai.impl
{
    /// <summary>
    /// DeepSeek AI问答服务实现 — 通过服务端代理 /chatStreamProxy 调用
    /// </summary>
    public class DeepSeekChatService : IAiChatService
    {
        /// <summary>
        /// 流式问答 — 通过 SSE 逐 chunk 推送到前端
        /// </summary>
        public async Task<CompletionsDto> ChatCompletionStream(
            AiTempTokenDto config, AskRequestDto dto,
            string content, HttpListenerRequest request, HttpListenerResponse response,
            CancellationToken cancellationToken = default)
        {
            return await requestDeepSeekProxy(config, dto, content, null, response, cancellationToken);
        }

        /// <summary>
        /// 带上下文的流式问答 — 通过 SSE 逐 chunk 推送到前端
        /// </summary>
        public async Task<CompletionsDto> ContextChatCompletionStream(
            AiTempTokenDto config, AskRequestDto dto,
            string content, string contextId,
            HttpListenerRequest request, HttpListenerResponse response,
            CancellationToken cancellationToken = default)
        {
            return await requestDeepSeekProxy(config, dto, content, contextId, response, cancellationToken);
        }

        /// <summary>
        /// 非流式问答 — 内部调用，不推送浏览器（目前仍走流式代理端点，仅累积结果不推 SSE）
        /// </summary>
        public CompletionsDto ChatCompletion(
            AiTempTokenDto config, AskRequestDto dto, string content)
        {
            return requestDeepSeekProxy(config, dto, content, null, null, CancellationToken.None).Result;
        }

        /// <summary>
        /// 调用服务端 /chatStreamProxy 代理接口，解析 SSE 事件流
        /// </summary>
        /// <param name="response">非 null 时推送消息到前端浏览器</param>
        /// <param name="contextId">上下文缓存ID，非 null 时走上下文模式</param>
        private async Task<CompletionsDto> requestDeepSeekProxy(
            AiTempTokenDto config, AskRequestDto dto, string content,
            string contextId, HttpListenerResponse response,
            CancellationToken cancellationToken = default)
        {
            var useModelWay = string.IsNullOrEmpty(contextId) ? 1 : 0;
            var requestBody = new
            {
                modelId = long.Parse(config.modelId),
                identity = dto.identity,
                realContent = content,
                thinkingType = dto.thinkingType ?? "enabled",
                contextId = contextId,
                useModelWay = useModelWay
            };

            string url = ReplayHttpUtils.BaseUrl + "/aiRelated/chatStreamProxy";
            string json = JsonConvert.SerializeObject(requestBody);
            string result = "";
            string thinkingAll = "";
            string finishReason = "";
            string requestId = "";
            int totalTokens = 0;
            Usage usage = null;

            try
            {
                var httpRequestMessage = new HttpRequestMessage(HttpMethod.Post, url)
                {
                    Content = new StringContent(json, Encoding.UTF8, "application/json")
                };
                httpRequestMessage.Headers.Add("token", ReplayHttpUtils.Token);
                SignatureHeaders.AddSignature(httpRequestMessage);

                using (var timeoutCts = new CancellationTokenSource(TimeSpan.FromMinutes(3)))
                using (var linkedCts = CancellationTokenSource.CreateLinkedTokenSource(
                    cancellationToken, timeoutCts.Token))
                using (HttpClient client = new HttpClient())
                {
                    client.Timeout = TimeSpan.FromMinutes(10);
                    HttpResponseMessage httpResponse = await client.SendAsync(
                        httpRequestMessage, HttpCompletionOption.ResponseHeadersRead, linkedCts.Token);

                    if (!httpResponse.IsSuccessStatusCode)
                    {
                        string errorBody = await httpResponse.Content.ReadAsStringAsync();
                        FileUtils.LogError(
                            $"DeepSeek代理请求失败，状态码:{httpResponse.StatusCode}, 信息：{errorBody}",
                            "DeepSeek流式对话失败");
                        throw new CustomException("DeepSeek请求失败", 7005);
                    }

                    using (var ctr = linkedCts.Token.Register(() => client.CancelPendingRequests()))
                    using (var stream = await httpResponse.Content.ReadAsStreamAsync())
                    using (var reader = new StreamReader(stream))
                    {
                        string eventType = "";
                        string line;
                        while ((line = await reader.ReadLineAsync()) != null)
                        {
                            // 每收到一行数据就重置空闲超时，确保只有真正卡住时才触发
                            timeoutCts.CancelAfter(TimeSpan.FromMinutes(3));

                            if (string.IsNullOrEmpty(line)) continue;

                            if (line.StartsWith("event:"))
                            {
                                eventType = line.Substring(6).Trim();
                                continue;
                            }

                            if (!line.StartsWith("data:")) continue;

                            string data = line.Substring(5).Trim();
                            if (string.IsNullOrEmpty(data)) continue;

                            if (eventType == "error")
                            {
                                var err = JsonConvert.DeserializeObject<dynamic>(data);
                                string msg = err?.msg?.ToString() ?? "DeepSeek请求失败";
                                throw new CustomException(msg, (int)(err?.code ?? 7005));
                            }

                            if (eventType == "message")
                            {
                                try
                                {
                                    var chunk = JsonConvert.DeserializeObject<dynamic>(data);
                                    string chunkContent = chunk?.content?.ToString() ?? "";
                                    string type = chunk?.type?.ToString() ?? "";
                                    string chunkFinishReason = chunk?.finishReason?.ToString() ?? "";

                                    if (!string.IsNullOrEmpty(chunkFinishReason))
                                    {
                                        finishReason = chunkFinishReason;
                                    }

                                    if (!string.IsNullOrEmpty(chunkContent))
                                    {
                                        if (type == "thinking")
                                        {
                                            thinkingAll += chunkContent;
                                        }
                                        else
                                        {
                                            result += chunkContent;
                                        }

                                        if (response != null)
                                        {
                                            await AiUtils.sendData(response, "message",
                                                JsonConvert.SerializeObject(new { content = chunkContent, type }));
                                        }
                                    }
                                }
                                catch (Exception ex)
                                {
                                    FileUtils.LogError($"DeepSeek消息解析异常: {ex.Message}", "DeepSeek流式对话");
                                }
                                continue;
                            }

                            if (eventType == "returnData")
                            {
                                try
                                {
                                    var finalData = JsonConvert.DeserializeObject<dynamic>(data);
                                    int status = (int)(finalData?.status ?? 0);
                                    if (status != 0)
                                    {
                                        throw new CustomException("DeepSeek返回失败", 7005);
                                    }
                                    requestId = finalData?.requestId?.ToString() ?? "";

                                    usage = new Usage
                                    {
                                        promptTokens = (int)(finalData?.promptTokens ?? 0),
                                        completionTokens = (int)(finalData?.completionTokens ?? 0),
                                        totalTokens = (int)(finalData?.totalTokens ?? 0)
                                    };

                                    totalTokens = usage.totalTokens;
                                }
                                catch (CustomException)
                                {
                                    throw;
                                }
                                catch (Exception ex)
                                {
                                    FileUtils.LogError($"DeepSeek返回数据解析异常: {ex.Message}", "DeepSeek流式对话");
                                }
                                continue;
                            }
                        }
                    }
                }
            }
            catch (OperationCanceledException)
            {
                FileUtils.LogError("DeepSeek SSE 流读取超时（3分钟无数据）", "DeepSeek流式对话超时");
                throw new CustomException("DeepSeek请求超时（3分钟无数据）", 7005);
            }
            catch (CustomException ex)
            {
                FileUtils.LogError($"DeepSeek业务异常: {ex.Message}", "DeepSeek流式对话异常");
                throw;
            }
            catch (Exception ex)
            {
                FileUtils.LogError(
                    $"DeepSeek调用异常: {ex.Message}, 堆栈={ex.StackTrace}",
                    "DeepSeek流式对话异常");
                throw new CustomException("DeepSeek请求异常", 7005);
            }

            if (string.IsNullOrEmpty(result) && string.IsNullOrEmpty(thinkingAll))
            {
                throw new CustomException("DeepSeek未返回任何内容", 7005);
            }

            string finalMessage = "";
            if (!string.IsNullOrEmpty(thinkingAll))
            {
                finalMessage += $"<div class='deepThinking'>{thinkingAll}</div>\n\n";
            }
            finalMessage += result;

            return new CompletionsDto
            {
                id = requestId,
                status = 0,
                totalTokenNum = totalTokens,
                usage = usage,
                choices = new Choices
                {
                    message = finalMessage,
                    thinkMessage = thinkingAll,
                    onlyMessage = result,
                    finishReason = finishReason,
                    requestId = requestId
                }
            };
        }
    }
}
