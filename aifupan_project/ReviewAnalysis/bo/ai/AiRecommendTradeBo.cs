using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.ai
{
    /// <summary>
    /// 获取ai推荐的行业入参
    /// </summary>
    public class AiRecommendTradeBo
    {

        /// <summary>
        /// 来源id
        /// </summary>
        public int sourceType {  get; set; }

        /// <summary>
        /// 来源类型
        /// </summary>
        public string sourceId { get; set; }
    }
}
