using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.Model
{
    public class AiTempTokenDto
    {
        /// <summary>
        /// 模型来源类型 0豆包火山，1通义千问，2DeepSeek
        /// </summary>
        public int resourceType { get; set; }

        /// <summary>
        /// token
        /// </summary>
        public string token { get; set; }

        /// <summary>
        /// 模型角色
        /// </summary>
        public string[] model { get; set; }

        /// <summary>
        /// 模型id
        /// </summary>
        public string modelId { get; set; }

        /// <summary>
        /// 火山引擎模型名称
        /// </summary>
        public string[] modelName { get; set; }

        /// <summary>
        /// 模型code
        /// </summary>
        public string modelCode { get; set; }

        /// <summary>
        /// 模型名称
        /// </summary>
        public string modelDefinition { get; set; }

        /// <summary>
        /// 模型的使用方式 0上下文缓存对话，1对话
        /// </summary>
        public int useModelWay { get; set; } = 1;

        /// <summary>
        /// 上下文缓存ID（从assemblePrompt接口获取，useModelWay=0时传给chatStreamProxy）
        /// </summary>
        public string contextId { get; set; }

        /// <summary>
        /// 上下文缓存的类型
        /// </summary>
        public string mode { get; set; } = "session";

        /// <summary>
        /// 用户截断的策略
        /// last_history_tokens:使用last_history_tokens模式，取决于使用的模型支持哪种Session 缓存；
        /// rolling_tokens：使用rolling_tokens模式，取决于使用的模型支持哪种Session 缓存
        /// </summary>
        public string truncationStrategyType { get; set; } = "last_history_tokens";

        /// <summary>
        /// type设置为last_history_tokens时，进行设置。缓存存储的最大 token 数，触发该上限将根据模型上下文大小对缓存内容进行截断，截断顺序按照时间由远及近
        /// </summary>
        public int lastHistoryTokens { get; set; } = 4096;

        /// <summary>
        /// type设置为rolling_tokens时，进行设置。在context历史消息长度接近模型上下文时，是否自动对历史上下文进行裁剪
        /// </summary>
        public bool rollingTokens { get; set; } = true;


        /// <summary>
        /// 会话的保存时间
        /// </summary>
        public int contextSaveTime { get; set; } = 3600;

        /// <summary>
        /// 临时token的保存时间
        /// </summary>
        public int tempTokenSaveTime { get; set; }

        /// <summary>
        /// 最多Ai发送消息长度
        /// </summary>
        public int? maxSendMessageLength { get; set; }

        /// <summary>
        /// 输出字数
        /// </summary>
        public int? outWordNum { get; set; }

    }



}
