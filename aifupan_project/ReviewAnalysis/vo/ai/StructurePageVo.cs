using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Ai.Model;

namespace ReviewAnalysis.vo.ai
{
    public class StructurePageVo
    {
        /// <summary>
        /// 是否存在上一页
        /// </summary>
        public bool existPreviousPage { get; set; } = false;

        /// <summary>
        /// 前端的结构
        /// </summary>
        public List<dynamic> list { get; set; }

        /// <summary>
        /// code对应的数据
        /// </summary>
        public Dictionary<string, ConversationDto> rawObj { get; set; }



    }
}
