using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo
{
    public class AnalysisResultCruxTypeVo
    {
        /// <summary>
        /// 关键词分类信息
        /// </summary>
        public CruxTypeInfoVo cruxTypeInfoVo { get; set; }

        /// <summary>
        /// 分类关键词占比比例值，0.21表示21%
        /// </summary>
        public double scale { get; set; }

        /// <summary>
        /// 分类关键词数量
        /// </summary>
        public int num { get; set; }

    }
}
