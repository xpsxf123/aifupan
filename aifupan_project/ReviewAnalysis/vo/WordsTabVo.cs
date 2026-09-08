using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo
{
    public class WordsTabVo
    {
        /// <summary>
        /// 分类ID
        /// </summary>
        public string cruxTypeId { get; set; }
        /// <summary>
        /// 是否统计到总数 0：否 1：是
        /// </summary>
        public int isCount { get; set; }

        /// <summary>
        /// 类型 0：全部 1：敏感词总数 2：关键词总数 3：关键词分类
        /// </summary>
        public int tabType { get; set; }

        /// <summary>
        /// 排序
        /// </summary>
        public int tabSort { get; set; }

        /// <summary>
        /// 分类占比比例值，0.21表示21%
        /// </summary>
        public double scale { get; set; }

        /// <summary>
        /// 词语数量
        /// </summary>
        public int num { get; set; }

        /// <summary>
        /// tab名称
        /// </summary>
        public string tabName { get; set; }
    }
}
