using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    public class AiAnalyzeTradePromptVo
    {
        private static readonly long serialVersionUID = 1L;

        /// <summary>
        /// 提示词
        /// </summary>
        public string cueWord { get; set; }

        /// <summary>
        /// ai的身份
        /// </summary>
        public string aiIdentity { get; set; }

        /// <summary>
        /// 使用的ai模型
        /// </summary>
        public int? aiModel { get; set; }

        /// <summary>
        /// 默认的行业id
        /// </summary>
        public long? defTradeId { get; set; }

        /// <summary>
        /// 默认的行业名称
        /// </summary>
        public string defTradeName { get; set; }

        /// <summary>
        /// 序号对应的行业id
        /// </summary>
        public Dictionary<string, TradeTemp> tradeList { get; set; }
    }

    public class TradeTemp
    {
        public long tradeId { get; set; }
        public String tradeName { get; set; }
    }
}
