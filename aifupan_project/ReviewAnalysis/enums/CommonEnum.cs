using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.enums
{
    public enum CueType
    {
        /// <summary>
        /// 运营提示词
        /// </summary>
        运营 = 0,

        /// <summary>
        /// 违规提示词
        /// </summary>
        违规 = 1,

        /// <summary>
        /// 弹幕提示词
        /// </summary>
        弹幕 = 2,

        /// <summary>
        /// 数据截图助手
        /// </summary>
        数据截图 = 3,

        /// <summary>
        /// 数据看板助手
        /// </summary>
        数据看板 = 4,

        /// <summary>
        /// 重要弹幕提示词
        /// </summary>
        重要弹幕 = 5,

        /// <summary>
        /// Ai话术提示词
        /// </summary>
        话术 = 6,

        /// <summary>
        /// 自然原文提示词
        /// </summary>
        自然原文 = 7,

        /// <summary>
        /// 优化原文提示词
        /// </summary>
        优化原文 = 8,

        /// <summary>
        /// 推荐行业
        /// </summary>
        推荐行业 = 9,


        提取文案优化 = 10,

        主播关键词获取 = 11,

        生成html = 12,

        数据诊断报告 = 13,
        
        AI内容格式判断 = 14,
        
        纠正AI内容 = 15,
    }


    /// <summary>
    /// AI内容校正场景类型
    /// </summary>
    public enum ContentCorrectionSceneType
    {
        /// <summary>
        /// AI问答助手的纠正检查
        /// </summary>
        AI问答助手的纠正检查 = 0,

        /// <summary>
        /// AI问答助手的纠正
        /// </summary>
        AI问答助手的纠正 = 1,

        /// <summary>
        /// 自然、优化原文的纠正检查
        /// </summary>
        自然优化原文的纠正检查 = 2,

        /// <summary>
        /// 自然、优化原文的纠正
        /// </summary>
        自然优化原文的纠正 = 3,
    }


    /// <summary>
    /// tb_ai_token_use_record表的use_source_type类型
    /// </summary>
    public enum AiUseSourceType
    {
        视频 = 0,
        文件 = 1,
        对比分析 = 2,
        数据截图 = 3,
        提取文案视频 = 4,
        主播获取关键词 = 5,
        ai问答 = 6,
        原文 = 7,
    }
}
