using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.trade
{
    public class TradeVo
    {

        /// <summary>
        /// ID 
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 行业名称 
        /// </summary>
        public string name { get; set; }

        /// <summary>
        /// 行业描述 
        /// </summary>
        public string remarks { get; set; }

        /// <summary>
        /// 默认的通用模型id 
        /// </summary>
        public long? defaultGeneralModelId { get; set; }

        /// <summary>
        /// 行业模型id，为0表示没有 
        /// </summary>
        public long tradeModelId { get; set; }

        /// <summary>
        /// 父行业ID 
        /// </summary>
        public long? parentId { get; set; }

        /// <summary>
        /// 排序 
        /// </summary>
        public int sort { get; set; }

        /// <summary>
        /// 创建时间 
        /// </summary>
        public DateTime? createDate { get; set; }

        /// <summary>
        /// 最后修改时间 
        /// </summary>
        public DateTime? updateDate { get; set; }

    }
}
