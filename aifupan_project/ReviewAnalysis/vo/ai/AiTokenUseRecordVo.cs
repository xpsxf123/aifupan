using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    public class AiTokenUseRecordVo
    {

        /// <summary>
        /// id
        ///</summary>
        public string id { get; set; }

        /// <summary>
        /// 租户
        ///</summary>
        public string tenantId { get; set; }

        /// <summary>
        /// 用户id
        ///</summary>
        public string userId { get; set; }

        /// <summary>
        /// 请求来源类型  0：客户端，1：运营端
        ///</summary>
        public int requestSourceType { get; set; }

        /// <summary>
        /// 使用来源类型 0：视频，1：文件，2对比分析，3数据截图, 4：提取文案视频
        ///</summary>
        public int useSourceType { get; set; }

        /// <summary>
        /// 使用来源id
        ///</summary>
        public String useSourceId { get; set; }

        /// <summary>
        /// 使用类型 0: 运营助手，1:违规助手，2:弹幕助手，3：数据截图
        ///</summary>
        public int assistantType { get; set; }

        /// <summary>
        /// 模型名称
        ///</summary>
        public String modelName { get; set; }

        /// <summary>
        /// 本次请求的id
        ///</summary>
        public String requestId { get; set; }

        /// <summary>
        ///模型生成结束原因
        ///通义调用：
        ///stop:因模型输出自然结束，或触发输入参数中的stop条件而结束时为stop
        ///length: 因生成长度过长而结束
        ///tool_calls: 因发生工具调用
        ///豆包调用:
        ///stop表示正常生成结束
        ///length 表示已经到了生成的最大 token 数量
        ///content_filter 表示模型输出命中审核提前终止
        /// </summary>
        public String finishReason { get; set; }

        /// <summary>
        /// 输入token 数量
        ///</summary>
        public int promptTokens { get; set; }

        /// <summary>
        /// 输出 token 数量
        ///</summary>
        public int completionTokens { get; set; }

        /// <summary>
        /// 图片的 token 数量
        ///</summary>
        public int imageTokens { get; set; }

        /// <summary>
        /// 音频的 token 数量
        ///</summary>
        public int audioTokens { get; set; }

        /// <summary>
        /// 视频的 token 数量
        ///</summary>
        public int videoTokens { get; set; }

        /// <summary>
        /// 上下文缓存的tokens数
        ///</summary>
        public int cachedTokens { get; set; }

        /// <summary>
        /// 输出思维链内容花费的 token
        ///</summary>
        public int reasoningTokens { get; set; }

        /// <summary>
        /// 本次请求消耗的总 token 数量
        ///</summary>
        public int totalTokens { get; set; }

        /// <summary>
        /// 更新时间
        ///</summary>
        public string updateDate { get; set; }

        /// <summary>
        /// 创建时间
        ///</summary>
        public string createDate { get; set; }

        /// <summary>
        /// 备注
        ///</summary>
        public String remarks { get; set; }

    }
}
