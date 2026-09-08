using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Ai.Model;

namespace ReviewAnalysis.Dto
{
    /// <summary>
    /// ai问答的出参
    /// </summary>
    public class AskResponseDto
    {
        /// <summary>
        /// 资产
        /// </summary>
        public PropertyDto property { get; set; }

        /// <summary>
        /// 问题
        /// </summary>
        public ConversationDto problem { get; set; }

        /// <summary>
        /// 回答
        /// </summary>
        public ConversationDto answer { get; set; }
       
        /// <summary>
        /// 是否检查ai内容
        /// </summary>
        public bool checkAiContent { get; set; }
    }

    /// <summary>
    /// 资产剩余
    /// </summary>
    public class PropertyDto
    {
        /// <summary>
        /// 当前问答使用
        /// </summary>
        public int currerntUseNum { get; set; }

        /// <summary>
        /// 剩余资产
        /// </summary>
        public int surplusNum { get; set; }
    }
}
