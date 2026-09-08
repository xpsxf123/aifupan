using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.Ai.Model
{
    public class DouBaoChatStreamDto
    {
        /// <summary>
        /// id
        /// </summary>
        public string id { get; set; }

        /// <summary>
        /// 结果列表（旧 Chat Completions API 格式）
        /// </summary>
        public List<Choices> choices { get; set; }

        /// <summary>
        /// 本次对话生成时间戳（秒）。
        /// </summary>
        public int created {  get; set; }

        /// <summary>
        /// 实际使用的模型名称和版本。
        /// </summary>
        public string model { get; set; }

        /// <summary>
        /// 本次请求的 token 用量（旧格式）
        /// </summary>
        public Usage usage { get; set; }

        // ========== Responses API 新字段 ==========

        /// <summary>
        /// Responses API 事件类型，如 "response.output_text.delta" / "response.completed"
        /// </summary>
        public string type { get; set; }

        /// <summary>
        /// Responses API 文本增量
        /// </summary>
        public string delta { get; set; }

        /// <summary>
        /// Responses API response.completed 事件中的 response 对象
        /// </summary>
        public ResponseObj response { get; set; }
    }

    public class ResponseObj
    {
        /// <summary>
        /// response id，如 "resp_xxx"
        /// </summary>
        public string id { get; set; }

        /// <summary>
        /// token 用量
        /// </summary>
        public ResponseUsage usage { get; set; }
    }

    public class ResponseUsage
    {
        [JsonProperty("input_tokens")]
        public int? inputTokens { get; set; }

        [JsonProperty("output_tokens")]
        public int? outputTokens { get; set; }

        [JsonProperty("total_tokens")]
        public int? totalTokens { get; set; }

        [JsonProperty("input_tokens_details")]
        public ResponseInputTokensDetails inputTokensDetails { get; set; }

        [JsonProperty("output_tokens_details")]
        public ResponseOutputTokensDetails outputTokensDetails { get; set; }
    }

    public class ResponseInputTokensDetails
    {
        [JsonProperty("cached_tokens")]
        public int? cachedTokens { get; set; }
    }

    public class ResponseOutputTokensDetails
    {
        [JsonProperty("reasoning_tokens")]
        public int? reasoningTokens { get; set; }
    }

}
