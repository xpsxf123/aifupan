using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    /// <summary>
    /// 获取ai推荐的行业
    /// </summary>
    public class AiRecommendTradeVo
    {

        /// <summary>
        /// 行业id
        /// </summary>
        public long tradeId {  get; set; }

        /// <summary>
        /// 行业名称
        /// </summary>
        public string tradeName { get; set; }
    }
}
