using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class AddAnchorDto
    {
        /// <summary>
        /// 主播行业id
        /// </summary>
        public string TradeId { get; set; }

        /// <summary>
        /// 主播url地址集合
        /// </summary>
        public List<string> Urls { get; set; }
    }
}
