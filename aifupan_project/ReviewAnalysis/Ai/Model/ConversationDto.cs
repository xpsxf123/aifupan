using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.Model
{
    /// <summary>
    /// Ai答的会话记录
    /// </summary>
    public class ConversationDto
    {
        /// <summary>
        /// id
        /// </summary>
        public string id { get; set; }
        /// <summary>
        /// 来源id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 来源类型 0视频，1文件，2对比分析
        /// </summary>
        public int? sourceType { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public string userId { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public string tenantId { get; set; }

        /// <summary>
        /// 助手类型
        /// </summary>
        public int? askType { get; set; }

        /// <summary>
        /// code
        /// </summary>
        public string code { get; set; }

        /// <summary>
        /// 一次 chat completion 接口调用的唯一标识。
        /// </summary>
        public string completionId { get; set; }

        /// <summary>
        /// 提示词id
        /// </summary>
        public string cueWordsId { get; set; }
        
        /// <summary>
        /// 提示词类型 0系统，1用户
        /// </summary>
        public int? cueWordsType { get; set; }

        /// <summary>
        /// 问答的code，一问一答的code的是一样的
        /// </summary>
        public string qaCode { get; set; }

        /// <summary>
        /// 会话id
        /// </summary>
        public string contextId { get; set; }

        /// <summary>
        /// 数据类型 Q问，A答
        /// </summary>
        public string type { get; set; }

        /// <summary>
        /// 内容
        /// </summary>
        public string content { get; set; }

        /// <summary>
        /// 问AI的真实问题
        /// </summary>
        public string realContent { get; set; }

        /// <summary>
        /// 点赞状态 -1未点赞 0点赞，1踩
        /// </summary>
        public int giveStatuc { get; set; } = -1;

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate { get; set; }
        
        /// <summary>
        /// 上一次对话的id
        /// </summary>
        public string lastConversationId { get; set; }
        
        /// <summary>
        /// 优化文本
        /// </summary>
        public string optimizeText { get; set; }
        
        /// <summary>
        /// 额外要求
        /// </summary>
        public string extraRequire { get; set; }

        /// <summary>
        /// 提问类型：0：正常问题，1：重新提问
        /// </summary>
        public int? questionType { get; set; } = 0;
        
        /// <summary>
        /// ai纠错状态 0：正常，1：纠错中，2：纠错完成
        /// </summary>
        private int? aiCorrectStatus { get; set; }

        /// <summary>
        /// 纠错来源类型 0：服务器生成，1：客户端生成
        /// </summary>
        private int? aiCorrectType { get; set; }

        /// <summary>
        /// ai纠错错误原因
        /// </summary>
        private string aiCorrectError { get; set; }
    }
}
