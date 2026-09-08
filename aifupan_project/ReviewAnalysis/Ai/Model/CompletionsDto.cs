using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.Model
{
    public class CompletionsDto
    {

        /// <summary>
        /// 本次的id
        /// </summary>
        public string id { get; set; }

        /// <summary>
        /// 消息内容
        /// </summary>
        public Choices choices { get; set; }

        /// <summary>
        /// 本次使用的token
        /// </summary>
        public int? totalTokenNum { get; set; }

        /// <summary>
        /// token使用情况
        /// </summary>
        public Usage usage { get; set; }

        /// <summary>
        /// 状态：0成功 1失败
        /// </summary>
        public int status { get; set; } = 0;

        /// <summary>
        /// Responses API 返回的 response.id，用于更新上下文缓存（豆包 Responses API 使用）
        /// </summary>
        public string responseId { get; set; }
    }

    public class Choices
    {
        /// <summary>
        /// 模型输出的消息内容-思考内容和消息
        /// </summary>
        public string message { get; set; }

        /// <summary>
        /// 思考内容
        /// </summary>
        public string thinkMessage { get; set; }

        /// <summary>
        /// 纯消息
        /// </summary>
        public string onlyMessage { get; set; }

        /// <summary>
        /// 由流式模型响应的模型输出增量
        /// </summary>
        public object delta { get; set; }

        /// <summary>
        /// 模型生成结束原因：
        /// stop：正常生成结束
        /// length 触发最大 token 数量而结束。
        /// content_filter ：模型输出被内容审核拦截。
        /// </summary>
        public string finishReason {  get; set; }

        /// <summary>
        /// requestId
        /// </summary>
        public string requestId { get; set; }
    }
}
