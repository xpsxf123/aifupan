using System.IO;
using System.Net;
using System.Threading;
using System.Threading.Tasks;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.Dto;

namespace ReviewAnalysis.Ai
{
    /// <summary>
    /// AI问答服务接口，屏蔽不同模型厂商（豆包、DeepSeek等）的调用差异
    /// </summary>
    public interface IAiChatService
    {
        /// <summary>
        /// 流式问答 — 逐 chunk 推送到前端浏览器 SSE
        /// </summary>
        /// <param name="config">模型配置（临时 token、模型 ID 等）</param>
        /// <param name="dto">请求参数</param>
        /// <param name="content">组装好的提示词</param>
        /// <param name="request">HTTP 请求（用于获取请求头信息）</param>
        /// <param name="response">HTTP 响应（用于 SSE 推送）</param>
        /// <returns>完整的问答结果</returns>
        Task<CompletionsDto> ChatCompletionStream(
            AiTempTokenDto config, AskRequestDto dto,
            string content, HttpListenerRequest request, HttpListenerResponse response,
            CancellationToken cancellationToken = default);

        /// <summary>
        /// 带上下文的流式问答 — 将上下文缓存ID透传到服务端代理
        /// </summary>
        /// <param name="config">模型配置（含 useModelWay、contextId）</param>
        /// <param name="dto">请求参数</param>
        /// <param name="content">组装好的提示词</param>
        /// <param name="contextId">上下文缓存ID（从 assemblePrompt 接口获取）</param>
        /// <param name="request">HTTP 请求</param>
        /// <param name="response">HTTP 响应（用于 SSE 推送）</param>
        /// <returns>完整的问答结果</returns>
        Task<CompletionsDto> ContextChatCompletionStream(
            AiTempTokenDto config, AskRequestDto dto,
            string content, string contextId,
            HttpListenerRequest request, HttpListenerResponse response,
            CancellationToken cancellationToken = default);

        /// <summary>
        /// 非流式问答 — 内部调用，不推送浏览器，直接返回完整结果
        /// </summary>
        /// <param name="config">模型配置</param>
        /// <param name="dto">请求参数</param>
        /// <param name="content">组装好的提示词</param>
        /// <returns>完整的问答结果</returns>
        CompletionsDto ChatCompletion(
            AiTempTokenDto config, AskRequestDto dto, string content);
    }
}
