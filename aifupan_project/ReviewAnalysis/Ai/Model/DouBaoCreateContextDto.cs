using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.Ai.Model
{
    public class DouBaoCreateContextDto
    {
        /// <summary>
        /// 您创建的上下文缓存ID，在后续创建带缓存的上下文缓存对话 API需要使用。
        /// </summary>
        public string id {  get; set; }

        /// <summary>
        /// 您的推理接入点ID。
        /// </summary>
        public string model { get; set; }

        /// <summary>
        /// 不活跃的时间所能达到最大时长，每次使用缓存均重置不活跃时间为0。单位为秒(second)
        /// </summary>
        public string ttl { get; set; }

        /// <summary>
        /// 上下文缓存的类型
        /// session :Session 缓存
        /// common_prefix :前缀缓存
        /// </summary>
        public string mode { get; set; }

        /// <summary>
        /// 控制历史上下文窗口策略。
        /// </summary>
        [JsonProperty("truncation_strategy")]
        public TruncationStrategy truncationStrategy { get; set; }

        /// <summary>
        /// 本次请求的 token 消耗情况。
        /// </summary>
        [JsonProperty("usage")]
        public Usage usage { get; set; }

        /// <summary>
        /// requestId
        /// </summary>
        public string requestId { get; set; }
    }

    public class Usage
    {
        /// <summary>
        /// 本次请求中输入的 token 数量。
        /// </summary>
        [JsonProperty("prompt_tokens")]
        public int promptTokens { get; set; }

        /// <summary>
        /// 模型生成的 token 数量。
        /// </summary>
        [JsonProperty("completion_tokens")]
        public int completionTokens { get; set; }

        /// <summary>
        /// 总的 token 数量。
        /// </summary>
        [JsonProperty("total_tokens")]
        public int totalTokens {  get; set; }

        /// <summary>
        /// prompt_tokens中命中上下文缓存的tokens数。
        /// </summary>
        [JsonProperty("prompt_tokens_details")]
        public PromptTokensDetails promptTokensDetails { get; set; }

        /// <summary>
        /// 完成令牌详细信息
        /// </summary>
        [JsonProperty("completion_tokens_details")]
        public CompletionTokensDetails completionTokensDetails;
    }


    public class CompletionTokensDetails
    {
        /// <summary>
        /// 思维token
        /// </summary>
        [JsonProperty("reasoning_tokens")]
        public int reasoningTokens { get; set; }
    }
    public class PromptTokensDetails
    {
        /// <summary>
        /// 上下文缓存使用的token数量
        /// </summary>
        [JsonProperty("cached_tokens")]
        public int cached_tokens { get; set; }
    }

    public class TruncationStrategy
    {
        /// <summary>
        /// 用户截断的策略，支持last_history_tokens 和rolling_tokens两种，取决于模型版本。
        /// </summary>
        public string type { get; set; }

        /// <summary>
        /// 历史存储的最大token数，默认为4096。根据模型上下文大小对历史存储截断
        /// </summary>
        public int? lastHistoryToken { get; set; }
        /// <summary>
        /// 历史消息长度超过模型上下文时，是否自动对历史上下文进行裁剪
        ///     若设置为true，在历史消息长度超过上下文长度时模型自动清除定量历史消息，并重新计算保持的消息。
        ///     若设置为false，在历史消息长度超过上下文长度时模型会停止输出（finish_reason='length')。
        /// </summary>
        [JsonProperty("rolling_tokens")]
        public bool? rollingTokens { get; set; }
    }

}
