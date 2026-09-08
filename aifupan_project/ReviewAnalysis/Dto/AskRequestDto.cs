using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Ai.Model;

namespace ReviewAnalysis.Dto
{
    /// <summary>
    /// 提问的入参
    /// </summary>
    public class AskRequestDto
    {
        /// <summary>
        /// 提问助手类型 0运营助手 1违规助手
        /// </summary>
        public int type { get; set; }
        
        /// <summary>
        /// 上一次对话的id
        /// </summary>
        public string lastConversationId { get; set; }

        /// <summary>
        /// 提问的问题
        /// </summary>
        public string content { get; set; }

        /// <summary>
        /// 问AI的真实问题
        /// </summary>
        public string realContent { get; set; }

        /// <summary>
        /// 提示词id
        /// </summary>
        public string cueWordsId { get; set; }

        /// <summary>
        /// 提示词类型 0系统，1用户
        /// </summary>
        public int? cueWordsType { get; set; }

        /// <summary>
        /// ai的身份
        /// </summary>
        public string identity { get; set; }

        /// <summary>
        /// 段落code(全文为0)(运营助手使用)
        /// </summary>
        public string paragraphCode { get; set; }

        /// <summary>
        /// 段落内容(违规助手使用)
        /// </summary>
        public string paragraphContent { get; set; }

        /// <summary>
        /// 违规原因(违规助手使用)
        /// </summary>
        public string reasonViolation { get; set; }

        /// <summary>
        /// 来源的id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 数据类型 0视频，1文件，2对比分析
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// 额外要求
        /// </summary>
        public List<string> additionalList { get; set; }

        /// <summary>
        /// 文本内的参数说明
        /// </summary>
        public string paramsDescribe { get; set; }

        /// <summary>
        /// 使用的ai模型 0:doubai1.5-pro-32K， 1:deepseek-r1
        /// </summary>
        public int aiModel { get; set; } = 0;

        /// <summary>
        /// 模型来源类型 0豆包火山，1通义千问，2DeepSeek
        /// </summary>
        public int resourceType { get; set; }

        /// <summary>
        /// 模型的使用方式 0上下文缓存对话，1对话
        /// </summary>
        public int useModelType { get; set; } = 1;

        /// <summary>
        /// 视频一的时间，第一个为开始时间，第二个为结束时间
        /// </summary>
        public List<long> videoTimeOneList { get; set; } = null;

        /// <summary>
        /// 视频二的时间，第一个为开始时间，第二个为结束时间
        /// </summary>
        public List<long> videoTimeTwoList { get; set; } = null;
        /// <summary>
        /// 是否上传数据截图(默认为1)：0不传，1传
        /// </summary>
        public int? uploadScreenshot { get; set; } = 1;

        /// <summary>
        /// 是否上传数据截图(默认为1)：0不传，1传
        /// </summary>
        public int? uploadBoard { get; set; } = 1;

        /// <summary>
        /// 其他参数
        /// </summary>
        public Dictionary<string, Object> otherObj { get; set; }

        /// <summary>
        /// 提示词限制在多少字
        /// </summary>
        public int singleMaxNum { get; set; } = -1;

        /// <summary>
        /// 模型是否开启深度思考模式。默认开启深度思考模式
        /// </summary>
        public string thinkingType { get; set; }

        /// <summary>
        /// 临时token
        /// </summary>
        public AiTempTokenDto token { get; set; }

        public string propertyDeductRemarks {  get; set; }
    }
}
